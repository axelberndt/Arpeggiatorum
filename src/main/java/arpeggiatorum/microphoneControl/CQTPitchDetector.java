package arpeggiatorum.microphoneControl;


import arpeggiatorum.gui.ArpeggiatorumGUI;
import arpeggiatorum.gui.LogGUIController;
import arpeggiatorum.supplementary.Tools;
import arpeggiatorum.supplementary.UnitVariableOutputPort;
import be.tarsos.dsp.ConstantQ;
import com.jsyn.ports.UnitInputPort;
import com.jsyn.unitgen.UnitGenerator;

public class CQTPitchDetector extends UnitGenerator {
    private final double[] buffer;
    private final int offset = 0;
    private final ConstantQ CQT;
    private final int lowIndex;
    public UnitInputPort input;
    /**
     * Provides arbitrary sized output.
     */
    public UnitVariableOutputPort output;
    public double[] frequencies;
    private int cursor;
    private boolean running;
    private final double[] pushData;

    public CQTPitchDetector() {
        this(44100.0f, 41.20f, 2000.0f, 12, 0.01f, 0.55f);
    }

    public CQTPitchDetector(float sampleRate, float minFreq, float maxFreq, int binsPerOctave, float threshold, float spread) {
        this.addPort(this.input = new UnitInputPort("Input"));
        CQT = new ConstantQ(sampleRate, minFreq, maxFreq, binsPerOctave, threshold, spread);
        frequencies = Tools.toDoubleArray(CQT.getFreqencies());
        this.addPort(this.output = new UnitVariableOutputPort("CQT Bins", frequencies.length));
        buffer = new double[CQT.getFFTlength()];
        pushData = output.getData();
        lowIndex = ((int) (Math.log((minFreq / Mic2MIDI_CQT.minFreq)) / Math.log(2))) * (binsPerOctave);
        String message = String.format("CQT Pitch Detection: Min Frequency (%.2fHz) Max Frequency (%.2fHz)  Delay (%.03fs) FFT: %d samples  \r\n", minFreq, maxFreq, buffer.length / sampleRate, buffer.length);
        LogGUIController.logBuffer.append(message);
    }

    /**
     * process the input signal
     *
     * @param start offset into port buffers
     * @param limit limit offset into port buffers for loop
     */
    @Override
    public void generate(int start, int limit) {
        if (!running) {
            int mask = (CQT.getFFTlength()) - 1;
            if (((getSynthesisEngine().getFrameCount() - offset) & mask) == 0) {
                running = true;
                cursor = 0;
            }
        }
        // Don't use "else" because "running" may have changed in above block.
        if (running) {
            double[] inputs = input.getValues();
            for (int i = start; i < limit; i++) {
                buffer[cursor] = inputs[i];
                ++cursor;
                // When it is full, do something.
                if (cursor == buffer.length) {
                    //CQT
                    CQT.calculateMagintudes(Tools.toFloatArray(buffer));
                    float[] CQTBins = CQT.getMagnitudes();
                    //Visualize CQT Bins
                    ArpeggiatorumGUI.controllerHandle.updateHist(Tools.toDoubleArray(CQTBins), lowIndex);
                    for (int j = 0; j < pushData.length; j++) {
                        pushData[j] = CQTBins[j];
                    }
                    output.advance();
                    cursor = 0;
                }
            }
        }
    }
}
