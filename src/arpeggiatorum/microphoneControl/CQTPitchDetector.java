package arpeggiatorum.microphoneControl;

import be.tarsos.dsp.ConstantQ;

import com.jsyn.ports.UnitInputPort;
import com.jsyn.unitgen.UnitGenerator;

import java.util.Arrays;

public class CQTPitchDetector extends UnitGenerator{
	public UnitInputPort input;
	/**
	 * Provides arbitrary sized output.
	 */
	public UnitVariableOutputPort output;

	private double[] buffer;
	private int cursor;
	private int offset = 0;
	private boolean running;


	private ConstantQ CQT;
	public double[] frequencies;
	float sampleRate;
	double[] pushData;

	//Histogram
	javax.swing.JFrame cqtBinsFrame = new javax.swing.JFrame("CQT Bins");
	CQTHistogram cqtHist;

	public CQTPitchDetector(){
		this(44100.0f, 40.0f, 2000.0f, 12);
	}

	public CQTPitchDetector(float sampleRate, float minFreq, float maxFreq, int binsPerOctave){
		this.addPort(this.input = new UnitInputPort("Input"));
		this.sampleRate = sampleRate;
		CQT = new ConstantQ(sampleRate, minFreq, maxFreq, binsPerOctave);
		frequencies = Mic2Midi.toDoubleArray(CQT.getFreqencies());
		this.addPort(this.output = new UnitVariableOutputPort("CQT Bins", frequencies.length));
		buffer = new double[CQT.getFFTlength()];
		pushData = output.getData();
		//CQT Histogram
		double[] initializer = {0.0};
		cqtHist = new CQTHistogram(initializer, frequencies);

		cqtBinsFrame.add(cqtHist);
		cqtBinsFrame.pack();
		cqtBinsFrame.setLocationRelativeTo(null);
		cqtBinsFrame.setVisible(true);
	}

	/**
	 * process the input signal
	 *
	 * @param start offset into port buffers
	 * @param limit limit offset into port buffers for loop
	 */
	@Override
	public void generate(int start, int limit){
		if (!running){
			int mask = (CQT.getFFTlength()) - 1;
			//int mask = (frequencies.length) - 1;
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
					//Visualize CQT Bins
					cqtHist.updateBins(Mic2Midi.toDoubleArray(CQTBins));
					cqtBinsFrame.revalidate();
					cqtBinsFrame.repaint();
					for (int j = 0; j < pushData.length; j++){
						pushData[j] = CQTBins[j];
					}
					output.advance();
					cursor = 0;
				}
			}
		}
	}


}
