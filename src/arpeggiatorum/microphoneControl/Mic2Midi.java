package arpeggiatorum.microphoneControl;


import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import com.jsyn.data.*;
import com.jsyn.ports.*;
//import com.jsyn.scope.AudioScope;
import com.jsyn.unitgen.*;

import com.softsynth.math.AudioMath;

import meico.midi.EventMaker;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.JFrame;
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
	public static final double SET_LEVEL = 0.5;
	public static final double RESET_LEVEL = 0.45;

	public Receiver receiver;                                   // the MIDI receiver
	public UnitInputPort trigger;                               // this port gets a 1.0 to trigger and a 0.0 to do nothing
	private double previousTriggerValue = 0.0;
	public UnitInputPort frequency;
	public UnitSpectralInputPort spectrum;
	private int currentPitch = -1;
	private final ChannelIn channelIn = new ChannelIn();              // microphone input
	private final SchmidtTrigger schmidtTrigger = new SchmidtTrigger();

	private final double sampleRate = 44100.00;

	//FFT to Pitch
	private final int binSize = 11;
	private final int numberBins = (int) Math.pow(2, binSize);
	private final int windowLength = numberBins * 2;
	//LowPass filter cutoff frequency
	private final double lowCut = 8000.0f;
	//Highpass filter cutoff frequency
	private final double hiCut = 40.0f;
	private int lowCutoff = 0;
	private int hiCutoff = 0;
	private int size = 0;
	private int nyquist = 0;


	//HPS
	private final int decimationSize = 3;

	//Lower and upper boundaries for Pitch detection
	//E1-G6 gives us 64 bins (power of 2)
	private final double lowFreq = 41.20; //E1
	private final double hiFreq = 1567.98; //G6

	//CQT
	private final CQTPitchDetector cqtPitchDetector;
	private final int binsPerOctave = 12;
	//	public UnitInputPort CQTBins;
	public UnitVariableInputPort CQTBins;
	//Histogram
	JFrame cqtBinsFrame = new JFrame("CQT Bins");
	CQTHistogram cqtHist;


//	private AudioScope scope;
//	Boolean flag=false;

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
		// Instantiate ports
		addPort(this.trigger = new UnitInputPort("Trigger"));
		addPort(this.frequency = new UnitInputPort("Frequency"));

		//Extra ports for FFT and CQT
		addPort(this.spectrum = new UnitSpectralInputPort("Spectrum"));
		//addPort(this.CQTBins = new UnitInputPort("CQTBins"));
		addPort(this.CQTBins = new UnitVariableInputPort("CQTBins"));

		// Build DSP patch
		this.add(this.channelIn);                               // add channelIn to the synth
		//        this.channelIn.setChannelIndex(0);                      // set its channel index, this call is redundant

//		scope = new AudioScope(arpeggiatorum.gui.Gui.synth);
//		scope.addProbe(this.channelIn.output);
//		scope.setTriggerMode(AudioScope.TriggerMode.NORMAL);

		//FFT
		SpectralFFT spectralFFT = new SpectralFFT(binSize);           // number of bins 2^x
		spectralFFT.setWindow(new HammingWindow(windowLength));         // window type and window length (should suffice for 21.53 Hz minimum frequency)
		spectralFFT.input.connect(0, this.channelIn.output, 0);
		this.add(spectralFFT);
		this.spectrum.connect(spectralFFT.output);

		//CQT Pitch Detector
		cqtPitchDetector = new CQTPitchDetector((float) sampleRate, (float) lowFreq, (float) hiFreq, binsPerOctave);
		cqtPitchDetector.input.connect(0, this.channelIn.output, 0);
		this.CQTBins.connect(cqtPitchDetector.output);
		this.add(cqtPitchDetector);

		//CQT Histogram
		double[] initializer = {0.0};
		cqtHist = new CQTHistogram(initializer, cqtPitchDetector.frequencies);

		cqtBinsFrame.add(cqtHist);
		cqtBinsFrame.pack();
		cqtBinsFrame.setLocationRelativeTo(null);
		cqtBinsFrame.setVisible(true);

		// Tarsos Pitch Detector
		TarsosPitchDetector tarsosPitchDetector = new TarsosPitchDetector((float) sampleRate, windowLength, PitchEstimationAlgorithm.FFT_YIN);
		tarsosPitchDetector.input.connect(0, this.channelIn.output, 0);
		this.add(tarsosPitchDetector);

		//This pitch detector returns a value that is one octave lower, compensate by adding +12
		PitchDetector pitchDetector = new PitchDetector();
		pitchDetector.input.connect(0, this.channelIn.output, 0);
		this.add(pitchDetector);

		LinearRamp frequencyRamp = new LinearRamp();
		frequencyRamp.time.set(FREQUENCY_RAMP_TIME);
		//frequencyRamp.input.connect(0, pitchDetector.frequency, 0);
		//Use Tarsos instead of built in pitch detector
		frequencyRamp.input.connect(0, tarsosPitchDetector.frequency, 0);
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
		this.schmidtTrigger.setLevel.set(SET_LEVEL);
		this.schmidtTrigger.resetLevel.set(RESET_LEVEL);
		this.add(this.schmidtTrigger);

		Multiply multiply = new Multiply();
		multiply.inputA.connect(0, this.schmidtTrigger.output, 0);
		//multiply.inputB.connect(0, pitchDetector.confidence, 0);
		//Using Tarsos
		multiply.inputB.connect(0, tarsosPitchDetector.confidence, 0);
		this.trigger.connect(0, multiply.output, 0);
		this.add(multiply);

		this.setReceiver(receiver);
		// it is not necessary to start any of the unit generators individually, as the Circuit should be started by its creator
	}

	@Override
	public void generate(int start, int limit){
		super.generate(start, limit);
//if (!flag){
//	scope.start();
//	flag = true;
//}
		double[] triggerInputs = this.trigger.getValues();
		double[] frequencyInputs = this.frequency.getValues();

		Spectrum inputSpectrum = this.spectrum.getSpectrum();
		double[] spectrumReal = inputSpectrum.getReal();
		double[] spectrumImg = inputSpectrum.getImaginary();

//		double[] CQTBins = this.CQTBins.getValues();
		double[] CQTBins = this.CQTBins.getData();
		//Visualize CQT Bins
		cqtHist.updateBins(CQTBins);
		cqtBinsFrame.revalidate();
		cqtBinsFrame.repaint();


		size = spectrumReal.length;
		nyquist = size / 2;

		lowCutoff = (int) (lowCut * size / sampleRate);
		hiCutoff = (int) (hiCut * size / sampleRate);
		// Brickwall Low-Pass Filter
//		for (int i = lowCutoff; i < nyquist; i++){
//			// Bins above nyquist are mirror of ones below.
//			spectrumReal[i] = spectrumReal[size - i] = 0.0;
//			spectrumImg[i] = spectrumImg[size - i] = 0.0;
//		}
		// Brickwall Hi-Pass Filter
//		for (int i = hiCutoff; i > 0; i--){
//			// Bins above nyquist are mirror of ones below.
//			spectrumReal[i] = spectrumReal[size - i] = 0.0;
//			spectrumImg[i] = spectrumImg[size - i] = 0.0;
//		}
		int maxBin;
		int maxBinCep;
		int outputLength;

		int lowBin = (int) (lowFreq / ((double) sampleRate / nyquist));
		int hiBin = (int) (hiFreq / ((double) sampleRate / nyquist));

		//Extract magnitude
		double[] magnitude = getMagnitude(nyquist, spectrumReal, spectrumImg);
		//HPS
		double[] arrayOutput = HPS(decimationSize, magnitude);
		//Get max value's bin number restricting the output between ...
		maxBin = getMaxBin(arrayOutput, lowBin, hiBin);
		//Cepstrum FFT(log(mag(FFT)^2))
		double[] logMag = new double[size];
		for (int i = 0; i < magnitude.length; i++){
			logMag[i] = Math.log(magnitude[i]);
		}
		double[] logMagR = logMag;
		double[] logMagI = new double[size];
		com.softsynth.math.FourierMath.fft(logMag.length, logMagR, logMagI);
		double[] magnitudeFFT = getMagnitude(logMagR.length, logMagR, logMagI);
		double[] logMagFFT = new double[magnitudeFFT.length];
		outputLength = logMagFFT.length;
		for (int i = 0; i < magnitudeFFT.length; i++){
			logMagFFT[i] = Math.log(magnitudeFFT[i]);
		}
		maxBinCep = getMaxBin(logMagFFT, logMagFFT.length - hiBin, logMagFFT.length - lowBin);

//		//	 Print buffer content
//		System.out.println("confidence " + Arrays.toString(triggerInputs) + "\n" +
//				"frequency  " + Arrays.toString(frequencyInputs) + "\n");
//
//		//	 Process the buffered frames
//		for (int i = start; i < limit; i++){
//			;
//		}

		// check if at the end of the buffer we have to play or stop a note
		//int newPitch = (int) Math.round(AudioMath.frequencyToPitch(DoubleStream.of(frequencyInputs).average().getAsDouble())) + 12;
		int newPitch = (int) Math.round(AudioMath.frequencyToPitch(frequencyInputs[0])) + 12;
		if (newPitch < 0){
			return;
		}
		if (this.previousTriggerValue > CONFIDENCE_THRESHOLD){         // we are currently playing a tone
			if (triggerInputs[limit - limit] <= CONFIDENCE_THRESHOLD){     // [limit -1] if we have to stop the note
				System.out.println("> " + this.currentPitch);
				System.out.println("> Autocorrelation Pitch: " + DoubleStream.of(frequencyInputs).average().getAsDouble());
				System.out.println("> Tarsos Pitch: " + frequencyInputs[0]);
				System.out.println("> FFT to Pitch using HPS: " + getFrequencyForIndex(maxBin, nyquist, sampleRate));
				System.out.println("> FFT to Pitch using Cepstrum: " + ((sampleRate) - (maxBinCep * ((double) sampleRate / outputLength))));
//				System.out.println("> Pitch using CQT: " + cqtPitchDetector.frequencies[(int) CQTBins[0]]);
				System.out.print("> Pitches using CQT: ");
				for (int i = 0; i < CQTBins.length; i++){
					System.out.print("[" + i + "] " + cqtPitchDetector.frequencies[(int) CQTBins[i]] + " ");
				}
				System.out.print("\r\n");
				this.sendNoteOff(this.currentPitch);
			} else{                                                    // we may have to update the pitch
//				System.out.println("- " + currentPitch);
				if (newPitch != this.currentPitch){
					System.out.println("- " + this.currentPitch + " ->" + newPitch);
					System.out.println("- Autocorrelation Pitch: " + DoubleStream.of(frequencyInputs).average().getAsDouble());
					System.out.println("- Tarsos Pitch: " + frequencyInputs[0]);
					System.out.println("- FFT to Pitch using HPS: " + getFrequencyForIndex(maxBin, nyquist, sampleRate));
					System.out.println("- FFT to Pitch using Cepstrum: " + ((sampleRate) - (maxBinCep * ((double) sampleRate / outputLength))));
					//				System.out.println("- Pitch using CQT: " + cqtPitchDetector.frequencies[(int) CQTBins[0]]);
					System.out.print("- Pitches using CQT: ");
					for (int i = 0; i < CQTBins.length; i++){
						System.out.print("[" + i + "] " + cqtPitchDetector.frequencies[(int) CQTBins[i]] + " ");
					}
					System.out.print("\r\n");

					this.sendNoteOff(this.currentPitch);
					this.sendNoteOn(newPitch);
				}
			}
		} else if (triggerInputs[limit - limit] > CONFIDENCE_THRESHOLD){   //[limit -1] we have to start a note
			System.out.println("< " + this.currentPitch);
			System.out.println("< Autocorrelation Pitch: " + DoubleStream.of(frequencyInputs).average().getAsDouble());
			System.out.println("< Tarsos Pitch: " + frequencyInputs[0]);
			System.out.println("< FFT to Pitch using HPS: " + getFrequencyForIndex(maxBin, nyquist, sampleRate));
			System.out.println("< FFT to Pitch using Cepstrum: " + ((sampleRate) - (maxBinCep * ((double) sampleRate / outputLength))));
			//System.out.println("< Pitch using CQT: " + cqtPitchDetector.frequencies[(int) CQTBins[0]]);
			System.out.print("< Pitches using CQT: ");
			for (int i = 0; i < CQTBins.length; i++){
				System.out.print("[" + i + "] " + cqtPitchDetector.frequencies[(int) CQTBins[i]] + " ");
			}
			System.out.print("\r\n");
			this.sendNoteOn(newPitch);
		}
		this.previousTriggerValue = triggerInputs[limit - limit]; //[limit - 1]
	}

	public static int getMaxBin(double[] arrayInput, int start, int end){
		//Look for the maximum value's index
		int maxBin = start;
		double maxVal = arrayInput[start];
		for (int i = start; i < end; i++){
			double val = arrayInput[i];
			if (val > maxVal){
				maxVal = val;
				maxBin = i;
			}
		}
		return maxBin;
	}

	public static int getMaxBin(float[] arrayInput, int start, int end){
		//Look for the maximum value's index
		int maxBin = start;
		float maxVal = arrayInput[start];
		for (int i = start; i < end; i++){
			float val = arrayInput[i];
			if (val > maxVal){
				maxVal = val;
				maxBin = i;
			}
		}
		return maxBin;
	}

	// Extract magnitude from spectrum
	private double[] getMagnitude(int nyquist, double[] spectrumReal, double[] spectrumImg){
		double[] magnitude = new double[nyquist];
		for (int i = 0; i < nyquist; i++){
			magnitude[i] = Math.sqrt(Math.pow(2, spectrumReal[i]) + Math.pow(2, spectrumImg[i]));
		}
		return magnitude;
	}

	//HPS Helper methods
	public double[] HPS(int decimationSize, double[] arrayData){
		int size = decimationSize - 1;
		double[][] HPSDownsampled = new double[size][arrayData.length];
		for (int i = 0; i < size; i++){
			HPSDownsampled[i] = Downsample(arrayData, i + 2);
		}

		double[] arrayOutput = new double[HPSDownsampled[size - 1].length];

		for (int i = 0; i < arrayOutput.length; i++){
			arrayOutput[i] = arrayData[i];
			for (int j = 0; j < size - 1; j++){
				arrayOutput[i] = arrayOutput[i] * HPSDownsampled[j][i];
			}
		}
		return arrayOutput;
	}

	public double[] Downsample(double[] data, int n){
		double[] array = new double[(int) (Math.ceil(data.length * 1.0 / n))];
		for (int i = 0; i < array.length; i++){
			array[i] = data[i * n];
		}
		return array;
	}

	private double getFrequencyForIndex(int index, int size, double sampleRate){
		return (double) index * sampleRate / (double) size;
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

	public static float[] toFloatArray(double[] arr){
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++){
			ret[i] = (float) arr[i];
		}
		return ret;
	}

	public static double[] toDoubleArray(float[] arr){
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++){
			ret[i] = arr[i];
		}
		return ret;
	}
}
