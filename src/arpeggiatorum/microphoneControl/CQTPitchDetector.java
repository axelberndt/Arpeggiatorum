package arpeggiatorum.microphoneControl;

import be.tarsos.dsp.ConstantQ;

import com.jsyn.ports.UnitInputPort;
import com.jsyn.ports.UnitOutputPort;
import com.jsyn.unitgen.UnitGenerator;

import javax.swing.JFrame;
import java.util.Arrays;

public class CQTPitchDetector extends UnitGenerator{
	public UnitInputPort input;
//	public UnitOutputPort output;
	public UnitVariableOutputPort output;

	private boolean running;
	private int offset = 0;
	private double[] buffer;
	private int cursor;
	private ConstantQ CQT;

	public double[] frequencies;
	float sampleRate;

	//Histogram
	JFrame cqtBinsFrame= new JFrame("CQT Bins");
	CQTHistogram cqtHist;
	private final int WIDTH=1000;
	private final int HEIGHT=500;
	public CQTPitchDetector(){
		this(44100.0f, 40.0f, 2000.0f, 12);
	}

	public CQTPitchDetector(float sampleRate, float minFreq, float maxFreq, int binsPerOctave){
		this.addPort(this.input = new UnitInputPort("Input"));
//		this.addPort(this.output = new UnitOutputPort("CQT Bins"));
		this.sampleRate = sampleRate;
		CQT = new ConstantQ(sampleRate, minFreq, maxFreq, binsPerOctave);
		frequencies = Mic2Midi.toDoubleArray(CQT.getFreqencies());
		this.addPort(this.output = new UnitVariableOutputPort("CQT Bins",frequencies.length));

		buffer = new double[CQT.getFFTlength()];
		double[] initializer={0.0};
		cqtHist=new CQTHistogram(initializer, frequencies);

		cqtBinsFrame.add(cqtHist);
		cqtBinsFrame.pack();
		cqtBinsFrame.setLocationRelativeTo(null);
		cqtBinsFrame.setVisible(true);
		cqtBinsFrame.show();
	}

	/**
	 * process the input signal
	 *
	 * @param start offset into port buffers
	 * @param limit limit offset into port buffers for loop
	 */
	@Override
	public void generate(int start, int limit){
		double[] outputValues = this.output.getData();

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
//					//Visualize CQT Bins
//					cqtHist.updateBins(Mic2Midi.toDoubleArray(CQTBins));
//					cqtBinsFrame.revalidate();
//					cqtBinsFrame.repaint();
//					//outputValues[0]= Mic2Midi.getMaxBin(CQTBins, 0, CQTBins.length);
//					double[] values = Arrays.stream(getMaxBins(CQTBins, limit)).asDoubleStream().toArray();
//					for (int j = 0; j < limit; j++){
//						outputValues[j] = values[j];
//					}
					outputValues=Mic2Midi.toDoubleArray(CQTBins);
					output.advance();
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
