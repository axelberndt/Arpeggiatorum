package arpeggiatorum.microphoneControl;


import arpeggiatorum.gui.GUI;
import arpeggiatorum.supplementary.UnitVariableOutputPort;
import be.tarsos.dsp.pitch.*;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.PitchProcessor;

import be.tarsos.dsp.pitch.Yin;
import com.jsyn.unitgen.*;
import com.jsyn.ports.*;

import java.util.Arrays;

public class TarsosPitchDetector extends UnitGenerator {
    public UnitInputPort input;
    public UnitVariableOutputPort frequency;
    public UnitVariableOutputPort confidence;

    private boolean running;
    private int offset = 0;
    private double[] buffer;
    private int cursor;

    float sampleRate;
    int bufferSize;
    private PitchDetector detector;

    private double[] pushPitch;
    private double[] pushConf;

    public TarsosPitchDetector() {
        this(44100, 2048, PitchProcessor.PitchEstimationAlgorithm.FFT_PITCH);
    }

    public TarsosPitchDetector(float sampleRate, int bufferSize, PitchProcessor.PitchEstimationAlgorithm algo) {
        this.addPort(this.input = new UnitInputPort("Input"));
        this.addPort(this.frequency = new UnitVariableOutputPort("Frequency", 1));
        this.addPort(this.confidence = new UnitVariableOutputPort("Confidence", 1));
        pushPitch = frequency.getData();
        pushConf = confidence.getData();

        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        buffer = new double[this.bufferSize];

        String message = String.format("Tarsos Pitch Detection: Minimum Frequency (%.2fHz) Delay (%.03fs) \r\n", (sampleRate / bufferSize) * 2, (bufferSize / sampleRate) / 2);

        //System.out.printf(message);
        GUI.updateLogGUI(message);
        if (algo == PitchProcessor.PitchEstimationAlgorithm.MPM) {
            detector = new McLeodPitchMethod(sampleRate, this.bufferSize);
        } else if (algo == PitchProcessor.PitchEstimationAlgorithm.DYNAMIC_WAVELET) {
            detector = new DynamicWavelet(sampleRate, this.bufferSize);
        } else if (algo == PitchProcessor.PitchEstimationAlgorithm.FFT_YIN) {
            detector = new FastYin(sampleRate, this.bufferSize);
        } else if (algo == PitchProcessor.PitchEstimationAlgorithm.AMDF) {
            detector = new AMDF(sampleRate, this.bufferSize);
        } else {
            detector = new Yin(sampleRate, this.bufferSize);
        }
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
            int mask = (bufferSize) - 1;
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
                    float[] fBuffer = Mic2MIDI_Tarsos.toFloatArray(buffer);
                    PitchDetectionResult pitchDetectionResult = detector.getPitch(fBuffer);
                    Arrays.fill(pushPitch, pitchDetectionResult.getPitch());
                    Arrays.fill(pushConf, pitchDetectionResult.getProbability());
                    frequency.advance();
                    confidence.advance();
                    cursor = 0;
                }
            }
        }
    }
}
