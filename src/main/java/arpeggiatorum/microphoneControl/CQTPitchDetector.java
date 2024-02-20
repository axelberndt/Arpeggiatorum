package arpeggiatorum.microphoneControl;


import arpeggiatorum.gui.GUI;
import be.tarsos.dsp.ConstantQ;

import com.jsyn.ports.UnitInputPort;
import com.jsyn.unitgen.UnitGenerator;

import arpeggiatorum.supplementary.UnitVariableOutputPort;
import static arpeggiatorum.microphoneControl.Mic2MIDI_CQT.cqtHist;

public class CQTPitchDetector extends UnitGenerator{
	public UnitInputPort input;
	/**
	 * Provides arbitrary sized output.
	 */
	public UnitVariableOutputPort output;

	private final double[] buffer;
	private int cursor;
	private final int offset = 0;
	private boolean running;

	private final ConstantQ CQT;
	public double[] frequencies;
	float sampleRate;
	double[] pushData;
	int lowIndex;

	public CQTPitchDetector(){
		this(44100.0f, 41.20f, 2000.0f, 12);
	}

	public CQTPitchDetector(float sampleRate, float minFreq, float maxFreq, int binsPerOctave){
		this.addPort(this.input = new UnitInputPort("Input"));
		this.sampleRate = sampleRate;
		CQT = new ConstantQ(sampleRate, minFreq, maxFreq, binsPerOctave,0.01f,0.55f);
		frequencies = Mic2MIDI_Tarsos.toDoubleArray(CQT.getFreqencies());
		this.addPort(this.output = new UnitVariableOutputPort("CQT Bins", frequencies.length));
		buffer = new double[CQT.getFFTlength()];
		pushData = output.getData();
		lowIndex = ((int) (Math.log((minFreq / Mic2MIDI_CQT.minFreq)) / Math.log(2))) * (binsPerOctave);
		String message=String.format("CQT Pitch Detection: Min Frequency (%.2fHz) Max Frequency (%.2fHz)  Delay (%.03fs) FFT: %d samples  \r\n",minFreq, maxFreq, buffer.length/this.sampleRate, buffer.length);
		//System.out.printf(message);
		GUI.updateLogGUI(message);
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
					CQT.calculateMagintudes(Mic2MIDI_Tarsos.toFloatArray(buffer));
					float[] CQTBins = CQT.getMagnitudes();
					//Visualize CQT Bins
					cqtHist.updateBins(Mic2MIDI_Tarsos.toDoubleArray(CQTBins), lowIndex);
					GUI.cqtBinsPanel.revalidate();
					GUI.cqtBinsPanel.repaint();
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
