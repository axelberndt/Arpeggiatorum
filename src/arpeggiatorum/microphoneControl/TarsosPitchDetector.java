package arpeggiatorum.microphoneControl;


import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.*;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.*;

import be.tarsos.dsp.pitch.Yin;
import com.jsyn.unitgen.*;
import com.jsyn.ports.*;

import java.util.Arrays;

public class TarsosPitchDetector extends UnitGenerator{ // implements PitchDetectionHandler{
	public UnitInputPort input;
	public UnitOutputPort frequency;
	public UnitOutputPort confidence;

	private boolean running;
	private int offset = 0;
	private double[] buffer;
	private int cursor;

	float sampleRate;
	int bufferSize;
	private PitchDetector detector;

	public TarsosPitchDetector(){
		this(44100, 2048, PitchProcessor.PitchEstimationAlgorithm.FFT_PITCH);
	}

	public TarsosPitchDetector(float sampleRate, int bufferSize, PitchProcessor.PitchEstimationAlgorithm algo){
		this.addPort(this.input = new UnitInputPort("Input"));
		this.addPort(this.frequency = new UnitOutputPort("Frequency"));
		this.addPort(this.confidence = new UnitOutputPort("Confidence"));

		this.sampleRate = sampleRate;
		this.bufferSize = bufferSize;
		buffer = new double[bufferSize];

		if (algo == PitchProcessor.PitchEstimationAlgorithm.MPM){
			detector = new McLeodPitchMethod(sampleRate, bufferSize);
		} else if (algo == PitchProcessor.PitchEstimationAlgorithm.DYNAMIC_WAVELET){
			detector = new DynamicWavelet(sampleRate, bufferSize);
		} else if (algo == PitchProcessor.PitchEstimationAlgorithm.FFT_YIN){
			detector = new FastYin(sampleRate, bufferSize);
		} else if (algo == PitchProcessor.PitchEstimationAlgorithm.AMDF){
			detector = new AMDF(sampleRate, bufferSize);
		} else{
			detector = new Yin(sampleRate, bufferSize);
		}
	}

	/**
	 * process the input signal
	 *
	 * @param start offset into port buffers
	 * @param limit limit offset into port buffers for loop
	 */
	@Override
	public void generate(int start, int limit){
		double[] frequencyOutput = this.frequency.getValues();
		double[] confidenceOutput = this.confidence.getValues();

		if (!running){
			int mask = (bufferSize) - 1;
			if (((getSynthesisEngine().getFrameCount() - offset) & mask) == 0){
				running = true;
				cursor = 0;
			}
		}
		// Don't use "else" because "running" may have changed in above block.
		if (running){
			double[] inputs = input.getValues();

			for (int i = start; i < limit; i++){
				buffer[cursor] = inputs[i];
				++cursor;
				// When it is full, do something.
				if (cursor == buffer.length){
					float[] fBuffer = Mic2Midi.toFloatArray(buffer);
					PitchDetectionResult pitchDetectionResult = detector.getPitch(fBuffer);
					Arrays.fill(frequencyOutput,pitchDetectionResult.getPitch());
					Arrays.fill(confidenceOutput,pitchDetectionResult.getProbability());
					if (pitchDetectionResult.getPitch() != -1){
						//frequencyOutput[0] = pitchDetectionResult.getPitch();
						//confidenceOutput[0] = pitchDetectionResult.getProbability();
					Arrays.fill(frequencyOutput,pitchDetectionResult.getPitch());
					Arrays.fill(confidenceOutput,pitchDetectionResult.getProbability());
					} else{
						Arrays.fill(frequencyOutput,0.0);
						Arrays.fill(confidenceOutput,0.0);

					}
					//Display the detected pitch
//					if (pitchDetectionResult.getPitch() != -1){
//						float pitch = pitchDetectionResult.getPitch();
//						float probability = pitchDetectionResult.getProbability();
//						String message = String.format("Pitch detected: %.2fHz ( %.2f probability)\n", pitch, probability);
//						System.out.println(message);
//					}
					cursor = 0;
				}
			}
		}
	}
}
