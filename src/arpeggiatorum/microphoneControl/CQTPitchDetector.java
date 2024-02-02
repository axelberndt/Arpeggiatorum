package arpeggiatorum.microphoneControl;

import be.tarsos.dsp.ConstantQ;
import com.jsyn.ports.UnitInputPort;
import com.jsyn.ports.UnitOutputPort;
import com.jsyn.unitgen.UnitGenerator;

import java.util.Arrays;
import java.util.Collections;

public class CQTPitchDetector extends UnitGenerator{
	public UnitInputPort input;
	public UnitOutputPort output;
	private boolean running;
	private int offset = 0;
	private double[] buffer;
	private int cursor;
	private ConstantQ CQT;

	public double[] frequencies;
	float sampleRate;

	public CQTPitchDetector(){
		this(44100.0f, 40.0f, 2000.0f, 12);
	}

	public CQTPitchDetector(float sampleRate, float minFreq, float maxFreq, int binsPerOctave){
		this.addPort(this.input = new UnitInputPort("Input"));
		this.addPort(this.output = new UnitOutputPort("CQT Bins"));

		this.sampleRate = sampleRate;
		CQT = new ConstantQ(sampleRate, minFreq, maxFreq, binsPerOctave);
		frequencies = Mic2Midi.toDoubleArray(CQT.getFreqencies());
		buffer = new double[CQT.getFFTlength()];


	}

	/**
	 * process the input signal
	 *
	 * @param start offset into port buffers
	 * @param limit limit offset into port buffers for loop
	 */
	@Override
	public void generate(int start, int limit){
		double[] outputValues = this.output.getValues();

		if (!running){
			int mask = (CQT.getFFTlength()) - 1;
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
					//CQT
					CQT.calculateMagintudes(Mic2Midi.toFloatArray(buffer));
					float[] CQTBins = CQT.getMagnitudes();
					//outputValues[0]= Mic2Midi.getMaxBin(CQTBins, 0, CQTBins.length);
					double[] values = java.util.Arrays.stream(getMaxBins(CQTBins, limit)).asDoubleStream().toArray();
					for (int j = 0; j < limit; j++){
						outputValues[j] = values[j];
					}
					cursor = 0;
				}
			}
		}
	}

	/**
	 * Return the indexes correspond to the top-k largest in an array.
	 */
	public static int[] getMaxBins(float[] array, int top_k){
		double[] max = new double[top_k];
		int[] maxIndex = new int[top_k];
		Arrays.fill(max, Double.NEGATIVE_INFINITY);
		Arrays.fill(maxIndex, -1);

		top:
		for (int i = 0; i < array.length; i++){
			for (int j = 0; j < top_k; j++){
				if (array[i] > max[j]){
					for (int x = top_k - 1; x > j; x--){
						maxIndex[x] = maxIndex[x - 1];
						max[x] = max[x - 1];
					}
					maxIndex[j] = i;
					max[j] = array[i];
					continue top;
				}
			}
		}
		return maxIndex;
	}
}
