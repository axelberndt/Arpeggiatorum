package arpeggiatorum.microphoneControl;

import arpeggiatorum.gui.GUI;
import com.jsyn.data.*;
import com.jsyn.ports.*;
import com.jsyn.unitgen.*;

import com.softsynth.math.AudioMath;

import javax.sound.midi.Receiver;

/**
 * This class reads the microphone signal, performs a basic pitch detection and outputs corresponding MIDI messages to the specified Receiver.
 *
 * @author Axel Berndt
 */
public class Mic2MIDI_FFT extends Mic2MIDI {

    public UnitInputPort trigger;// this port gets a 1.0 to trigger and a 0.0 to do nothing
    private double previousTriggerValue = 0.0;
    public UnitInputPort frequency;
    public UnitSpectralInputPort spectrum;


    //FFT to Pitch
    private static final int binSize = 9;
    private static final int numberBins = (int) Math.pow(2, binSize);
    private static final int windowLength = numberBins * 2;
    //LowPass filter cutoff frequency
    private static final double lowCut = 8000.0f;
    //Highpass filter cutoff frequency
    private static final double hiCut = 40.0f;
    private int lowCutoff = 0;
    private int hiCutoff = 0;
    private int size = 0;
    private int nyquist = 0;
    //HPS
    private static final int decimationSize = 3;

    //Lower and upper boundaries for Pitch detection
    //E1-G6 gives us 64 CQT bins (power of 2)
    public static final double minFreq = sampleRate/numberBins;
    public static final double maxFreq = 1567.98; //G6

    public Mic2MIDI_FFT(Receiver receiver) {
        NAME = "FFT-Based Pitch Detectors";
        // Instantiate ports
        addPort(this.trigger = new UnitInputPort("Trigger"));
        addPort(this.frequency = new UnitInputPort("Frequency"));

        //Extra ports for FFT
        addPort(this.spectrum = new UnitSpectralInputPort("Spectrum"));

        // Build DSP patch
        this.add(this.channelIn);// add channelIn to the synth

        //FFT
        SpectralFFT spectralFFT = new SpectralFFT(binSize);           // number of bins 2^x
        spectralFFT.setWindow(new HammingWindow(windowLength));         // window type and window length (should suffice for 21.53 Hz minimum frequency)
        spectralFFT.input.connect(0, this.channelIn.output, 0);
        this.add(spectralFFT);
        this.spectrum.connect(spectralFFT.output);

        //Takes the input --> Peak Follower --> Linear Ramp --> Schmidt Trigger --> Multiply by confidence of pitch detection
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
        this.trigger.connect(0,this.schmidtTrigger.output, 0);

        String message= String.format("FFT Pitch Detection: Minimum Frequency (%.2fHz) Delay (%.03fs) \r\n",sampleRate/numberBins, numberBins/sampleRate);
       // System.out.printf(message);
        GUI.updateLogGUI(message);

        this.setReceiver(receiver);
    }

    @Override
    public void generate(int start, int limit) {
        super.generate(start, limit);

        double[] triggerInputs = this.trigger.getValues();

        //FFT
        Spectrum inputSpectrum = this.spectrum.getSpectrum();
        double[] spectrumReal = inputSpectrum.getReal();
        double[] spectrumImg = inputSpectrum.getImaginary();

        //FFT-based Methods
        size = spectrumReal.length;
        nyquist = size / 2;

        lowCutoff = (int) (lowCut * size / sampleRate);
        hiCutoff = (int) (hiCut * size / sampleRate);
        // Brick-wall Low-Pass Filter
//		for (int i = lowCutoff; i < nyquist; i++){
//			// Bins above nyquist are mirror of ones below.
//			spectrumReal[i] = spectrumReal[size - i] = 0.0;
//			spectrumImg[i] = spectrumImg[size - i] = 0.0;
//		}
        // Brick-wall Hi-Pass Filter
//		for (int i = hiCutoff; i > 0; i--){
//			// Bins above nyquist are mirror of ones below.
//			spectrumReal[i] = spectrumReal[size - i] = 0.0;
//			spectrumImg[i] = spectrumImg[size - i] = 0.0;
//		}
        int maxBinHPS;
        int maxBinCep;
        int outputLength;

        int lowBin = (int) (minFreq / (sampleRate / nyquist));
        int hiBin = (int) (maxFreq / (sampleRate / nyquist));

        //Extract magnitude
        double[] magnitude = getMagnitude(nyquist, spectrumReal, spectrumImg);
        //HPS
        double[] arrayOutput = HPS(decimationSize, magnitude);
        //Get max value's bin number restricting the output between ...
        maxBinHPS = getMaxBin(arrayOutput, lowBin, hiBin);
        //Cepstrum FFT(log(mag(FFT)^2))
        double[] logMag = new double[size];
        for (int i = 0; i < magnitude.length; i++) {
            logMag[i] = Math.log(magnitude[i]);
        }
        double[] logMagR = logMag;
        double[] logMagI = new double[size];
        com.softsynth.math.FourierMath.fft(logMag.length, logMagR, logMagI);
        double[] magnitudeFFT = getMagnitude(logMagR.length, logMagR, logMagI);
        double[] logMagFFT = new double[magnitudeFFT.length];
        outputLength = logMagFFT.length;
        for (int i = 0; i < magnitudeFFT.length; i++) {
            logMagFFT[i] = Math.log(magnitudeFFT[i]);
        }
        maxBinCep = getMaxBin(logMagFFT, outputLength - hiBin, outputLength - lowBin);

        //System.out.println("- FFT to Pitch using HPS: " + getFrequencyForIndex(maxBinHPS, nyquist));
        //System.out.println("- FFT to Pitch using Cepstrum: " + ((sampleRate) - (maxBinCep * (sampleRate / outputLength))));

        // Check if at the end of the buffer we have to play or stop a note
		int newPitch = (int) Math.round(AudioMath.frequencyToPitch(getFrequencyForIndex(maxBinHPS, nyquist)));
		if (newPitch<= 0|| currentPitch<-1 ||newPitch>= 128|| currentPitch>=128){
			//this.previousTriggerValue = triggerInputs[0]; //[limit - 1]
			//((Arpeggiator)receiver).panic();
			return;
		}

		if (this.previousTriggerValue > CONFIDENCE_THRESHOLD){         // we are currently playing a tone
			if (triggerInputs[0] <= CONFIDENCE_THRESHOLD){     // [limit -1] if we have to stop the note
//				System.out.println("> " + this.currentPitch);
//				System.out.println("> FFT to Pitch using HPS: " + getFrequencyForIndex(maxBinHPS, nyquist));
//				System.out.println("> FFT to Pitch using Cepstrum: " + ((sampleRate) - (maxBinCep * (sampleRate / outputLength))));
//
//				System.out.print("\r\n");
                GUI.updateLogGUI("> " + this.currentPitch+"\r\n");
                GUI.updateLogGUI("> FFT to Pitch using HPS: " + getFrequencyForIndex(maxBinHPS, nyquist)+"\r\n");
                GUI.updateLogGUI("> FFT to Pitch using Cepstrum: " + ((sampleRate) - (maxBinCep * (sampleRate / outputLength)))+"\r\n");

                GUI.updateLogGUI("\r\n");
				this.sendNoteOff(this.currentPitch);
			} else{                                                    // we may have to update the pitch
//				System.out.println("- " + currentPitch);
                GUI.updateLogGUI("- " + currentPitch);
				if (newPitch != this.currentPitch){
//					System.out.println("- " + this.currentPitch + " ->" + newPitch);
//					System.out.println("- FFT to Pitch using HPS: " + getFrequencyForIndex(maxBinHPS, nyquist));
//					System.out.println("- FFT to Pitch using Cepstrum: " + ((sampleRate) - (maxBinCep * (sampleRate / outputLength))));
//					System.out.print("\r\n");
                    GUI.updateLogGUI("- " + this.currentPitch + " ->" + newPitch+"\r\n");
                    GUI.updateLogGUI("- FFT to Pitch using HPS: " + getFrequencyForIndex(maxBinHPS, nyquist)+"\r\n");
                    GUI.updateLogGUI("- FFT to Pitch using Cepstrum: " + ((sampleRate) - (maxBinCep * (sampleRate / outputLength)))+"\r\n");
                    GUI.updateLogGUI("\r\n");
					this.sendNoteOff(this.currentPitch);
					this.sendNoteOn(newPitch);
				}
			}
		} else if (triggerInputs[0] > CONFIDENCE_THRESHOLD){   //[limit -1] we have to start a note
//			System.out.println("< " + this.currentPitch);
//			System.out.println("< FFT to Pitch using HPS: " + getFrequencyForIndex(maxBinHPS, nyquist));
//			System.out.println("< FFT to Pitch using Cepstrum: " + ((sampleRate) - (maxBinCep * (sampleRate / outputLength))));
//			System.out.print("\r\n");
            GUI.updateLogGUI("< " + this.currentPitch+"\r\n");
            GUI.updateLogGUI("< FFT to Pitch using HPS: " + getFrequencyForIndex(maxBinHPS, nyquist)+"\r\n");
            GUI.updateLogGUI("< FFT to Pitch using Cepstrum: " + ((sampleRate) - (maxBinCep * (sampleRate / outputLength)))+"\r\n");

            GUI.updateLogGUI("\r\n");
			this.sendNoteOn(newPitch);
		}

		this.previousTriggerValue = triggerInputs[0]; //[limit - 1]
    }

    public static int getMaxBin(double[] arrayInput, int start, int end) {
        //Look for the maximum value's index
        int maxBin = start;
        double maxVal = arrayInput[start];
        for (int i = start; i < end; i++) {
            double val = arrayInput[i];
            if (val > maxVal) {
                maxVal = val;
                maxBin = i;
            }
        }
        return maxBin;
    }


    // Extract magnitude from spectrum
    private double[] getMagnitude(int nyquist, double[] spectrumReal, double[] spectrumImg) {
        double[] magnitude = new double[nyquist];
        for (int i = 0; i < nyquist; i++) {
            magnitude[i] = Math.sqrt(Math.pow(2, spectrumReal[i]) + Math.pow(2, spectrumImg[i]));
        }
        return magnitude;
    }

    //HPS Helper methods
    public double[] HPS(int decimationSize, double[] arrayData) {
        int size = decimationSize - 1;
        double[][] HPSDownsampled = new double[size][arrayData.length];
        for (int i = 0; i < size; i++) {
            HPSDownsampled[i] = Downsample(arrayData, i + 2);
        }
        double[] arrayOutput = new double[HPSDownsampled[size - 1].length];
        for (int i = 0; i < arrayOutput.length; i++) {
            arrayOutput[i] = arrayData[i];
            for (int j = 0; j < size - 1; j++) {
                arrayOutput[i] = arrayOutput[i] * HPSDownsampled[j][i];
            }
        }
        return arrayOutput;
    }

    public double[] Downsample(double[] data, int n) {
        double[] array = new double[(int) (Math.ceil(data.length * 1.0 / n))];
        for (int i = 0; i < array.length; i++) {
            array[i] = data[i * n];
        }
        return array;
    }

    private double getFrequencyForIndex(int index, int size) {
        return (double) index * Mic2MIDI_FFT.sampleRate / (double) size;
    }

}
