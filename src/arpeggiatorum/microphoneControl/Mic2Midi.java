package arpeggiatorum.microphoneControl;

import com.jsyn.data.*;
import com.jsyn.ports.*;
import com.jsyn.unitgen.*;
import com.softsynth.math.AudioMath;
import meico.midi.EventMaker;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import java.util.stream.DoubleStream;

/**
 * This class reads the microphone signal, performs a basic pitch detection and outputs corresponding MIDI messages to the specified Receiver.
 *
 * @author Axel Berndt
 */
public class Mic2Midi extends Circuit implements Transmitter{
	private static final double CONFIDENCE_THRESHOLD = 0.3;
	private static final double FREQUENCY_RAMP_TIME = 0.01;
	private static final double PEAK_FOLLOWER_RAMP_TIME = 0.25;
	public Receiver receiver;                                   // the MIDI receiver
	public UnitInputPort trigger;                               // this port gets a 1.0 to trigger and a 0.0 to do nothing
	private double previousTriggerValue = 0.0;
	public UnitInputPort frequency;
	public com.jsyn.ports.UnitSpectralInputPort spectrum;
	private int currentPitch = -1;
	private ChannelIn channelIn = new ChannelIn();              // microphone input
	private SchmidtTrigger schmidtTrigger = new SchmidtTrigger();

	/**
	 * constructor
	 * channelIn
	 * ____________________|___________________
	 * |                                       |
	 * pitchDetector                            peakFollower
	 * frequency     confidence                            |
	 * |              |                         peakFollowerRamp
	 * frequencyRamp        |                                |
	 * |              |                          schmidtTrigger
	 * |              |_____________   ________________|
	 * |                           |  |
	 * |                         multiply
	 * |                            |
	 * frequencyInputPort            triggerInputPort
	 */
	public Mic2Midi(Receiver receiver){
		// instantiate ports
		addPort(this.trigger = new UnitInputPort("Trigger"));
		addPort(this.frequency = new UnitInputPort("Frequency"));
		//Extra port for FFT/CQT Spectrum
		addPort(this.spectrum = new UnitSpectralInputPort("Spectrum"));

		// built dsp patch
		this.add(this.channelIn);                               // add channelIn to the synth
		//        this.channelIn.setChannelIndex(0);                      // set its channel index, this call is redundant

		//This pitch detector returns a value that is one octave lower, compensate by adding +12
		PitchDetector pitchDetector = new PitchDetector();
		pitchDetector.input.connect(0, this.channelIn.output, 0);
		this.add(pitchDetector);

		LinearRamp frequencyRamp = new LinearRamp();
		frequencyRamp.time.set(FREQUENCY_RAMP_TIME);
		frequencyRamp.input.connect(0, pitchDetector.frequency, 0);
		this.add(frequencyRamp);
		this.frequency.connect(0, frequencyRamp.output, 0);

		PeakFollower peakFollower = new PeakFollower();
		peakFollower.input.connect(0, this.channelIn.output, 0);
		this.add(peakFollower);

		LinearRamp peakFollowerRamp = new LinearRamp();
		peakFollowerRamp.time.set(PEAK_FOLLOWER_RAMP_TIME);       // ramp time, smaller=more sensitive
		peakFollowerRamp.input.connect(0, peakFollower.output, 0);
		this.add(peakFollowerRamp);

		this.schmidtTrigger.input.connect(0, peakFollowerRamp.output, 0);
		this.schmidtTrigger.setLevel.set(0.5);
		this.schmidtTrigger.resetLevel.set(0.45);
		this.add(this.schmidtTrigger);

		Multiply multiply = new Multiply();
		multiply.inputA.connect(0, this.schmidtTrigger.output, 0);
		multiply.inputB.connect(0, pitchDetector.confidence, 0);
		this.trigger.connect(0, multiply.output, 0);
		this.add(multiply);

		int binSize = 11;
		int numberBins = (int) Math.pow(2, binSize);
		int windowLength = numberBins;

		SpectralFFT spectralFFT = new SpectralFFT(binSize);           // number of bins 2^x
//        int numberBins = (int) Math.pow(2, spectralFFT.getSizeLog2());
//        double lowestFrequency = ((double) spectralFFT.getFrameRate()) / numberBins;
//        double lowestFrequency = ((double) SynthesisEngine.DEFAULT_FRAME_RATE) / numberBins;
//        System.out.println("lowest freq: " + lowestFrequency);
		spectralFFT.setWindow(new HammingWindow(windowLength));         // window type and window length (should suffice for 21.53 Hz minimum frequency)
		spectralFFT.input.connect(0, this.channelIn.output, 0);
		this.add(spectralFFT);
		this.spectrum.connect(spectralFFT.output);

//        //Setting up things for a CQT implementation
//        CQT cqt = new CQT(binSize); // number of bins 2^x
//        cqt.setWindow(new HammingWindow(windowLength)); // window type and window length (should suffice for 21.53 Hz minimum
//        // frequency)
//        cqt.input.connect(0, this.channelIn.output, 0);
//        this.add(cqt);
//		this.spectrum.connect(cqt.output);

		this.setReceiver(receiver);
		// it is not necessary to start any of the unit generators individually, as the Circuit should be started by its creator
	}

	@Override
	public void generate(int start, int limit){
		super.generate(start, limit);

		double[] triggerInputs = this.trigger.getValues();
		double[] frequencyInputs = this.frequency.getValues();
		Spectrum inputSpectrum = this.spectrum.getSpectrum();
		double[] spectrumReal = inputSpectrum.getReal();
		double[] spectrumImg = inputSpectrum.getImaginary();

		int size = spectrumReal.length;
		double lowCut = 8000;
		double hiCut = 40;
		int sampleRate = 44100;
		int lowCutoff = (int) (lowCut * size / sampleRate);
		int hiCutoff = (int) (hiCut * size / sampleRate);

		int nyquist = size / 2;
		// Brickwall Low-Pass Filter
//		for (int i = lowCutoff; i < nyquist; i++){
//			// Bins above nyquist are mirror of ones below.
//			spectrumReal[i] = spectrumReal[size - i] = 0.0;
//			spectrumImg[i] = spectrumImg[size - i] = 0.0;
//		}
		// Brickwall Hi-Pass Filter
		for (int i = hiCutoff; i > 0; i--){
			// Bins above nyquist are mirror of ones below.
			spectrumReal[i] = spectrumReal[size - i] = 0.0;
			spectrumImg[i] = spectrumImg[size - i] = 0.0;
		}
// Extract magnitude
		double[] magnitude = new double[nyquist];
		for (int i = 0; i < nyquist; i++){
			magnitude[i] = Math.sqrt(Math.pow(2, spectrumReal[i]) + Math.pow(2, spectrumImg[i]));
		}

// HPS
		double[] hps2 = Downsample(magnitude, 2);
		double[] hps3 = Downsample(magnitude, 3);
		double[] hps4 = Downsample(magnitude, 4);
		double[] hps5 = Downsample(magnitude, 5);

		double[] arrayOutput = new double[hps5.length];

		for (int i = 0; i < arrayOutput.length; i++){
			arrayOutput[i] = magnitude[i] * hps2[i] * hps3[i] * hps4[i]* hps5[i];
		}

		//Look for the maximum  value
		int maxBin = 0;
		double maxVal = arrayOutput[0];
		for (int i = 0; i < arrayOutput.length; i++){
			double val = arrayOutput[i];
			if (val > maxVal){
				maxVal = val;
				maxBin = i;
			}
		}
//Other methods
// MLE?
//		Cepstrum? FFT(log(mag(FFT)))

		// print buffer content
//        System.out.println("confidence " + Arrays.toString(triggerInputs) + "\n" +
//                           "frequency  " + Arrays.toString(frequencyInputs) + "\n");

		// process the buffered frames
//        for (int i = start; i < limit; i++) {
//            ;
//        }

		// check if at the end of the buffer we have to play or stop a note
		int newPitch = (int) Math.round(AudioMath.frequencyToPitch(DoubleStream.of(frequencyInputs).average().getAsDouble())) + 12;
		if (this.previousTriggerValue > CONFIDENCE_THRESHOLD){         // we are currently playing a tone
			if (triggerInputs[limit - 1] <= CONFIDENCE_THRESHOLD){     // if we have to stop the note
				System.out.println("> " + currentPitch);
				this.sendNoteOff(this.currentPitch);
			} else{                                                    // we may have to update the pitch
				System.out.println("- " + currentPitch);
				if (newPitch != this.currentPitch){
					this.sendNoteOff(this.currentPitch);
					this.sendNoteOn(newPitch);
					System.out.println("FFT to Pitch using HPS: " + getFrequencyForIndex(maxBin, nyquist, sampleRate));
				}
			}
		} else if (triggerInputs[limit - 1] > CONFIDENCE_THRESHOLD){   // we have to start a note
			System.out.println("< " + currentPitch);
			this.sendNoteOn(newPitch);
			System.out.println("FFT to Pitch using HPS: " + getFrequencyForIndex(maxBin, nyquist, sampleRate));
		}
		this.previousTriggerValue = triggerInputs[limit - 1];
	}

	//HPS Helper methods

	public double[] Downsample(double[] data, int n){
		double[] array = new double[(int) (Math.ceil(data.length * 1.0 / n))];
		for (int i = 0; i < array.length; i++){
			array[i] = data[i * n];
		}
		return array;
	}

	private double getFrequencyForIndex(int index, int size, int sampleRate){
		double freq = (double) index * (double) sampleRate / (double) size;
		return freq;
	}

	private void sendNoteOn(int pitch){
		ShortMessage noteOn;
		try{
			noteOn = new ShortMessage(EventMaker.NOTE_ON, pitch, 100);
		} catch (InvalidMidiDataException e){
			e.printStackTrace();
			return;
		}
		this.getReceiver().send(noteOn, -1);
		this.currentPitch = pitch;
	}

	private void sendNoteOff(int pitch){
		ShortMessage noteOff;
		try{
			noteOff = new ShortMessage(EventMaker.NOTE_OFF, pitch, 0);
		} catch (InvalidMidiDataException e){
			e.printStackTrace();
			return;
		}
		this.getReceiver().send(noteOff, -1);
		this.currentPitch = -1;
	}

	/**
	 * set the amplitude level above which output will be triggered
	 *
	 * @param value
	 */
	public void setSignalToNoiseThreshold(double value){
		this.schmidtTrigger.setLevel.set(value);
		this.schmidtTrigger.resetLevel.set(Math.max(0.0, value - 0.05));
	}

	/**
	 * set the receiver of outgoing MIDI messages
	 *
	 * @param receiver the desired receiver.
	 */
	@Override
	public void setReceiver(Receiver receiver){
		this.receiver = receiver;
	}

	/**
	 * get the receiver of outgoing MIDI messages
	 *
	 * @return
	 */
	@Override
	public Receiver getReceiver(){
		return this.receiver;
	}

	/**
	 * close procedure
	 */
	@Override
	public void close(){

	}
}
