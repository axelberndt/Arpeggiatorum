package arpeggiatorum.microphoneControl;


import arpeggiatorum.LogGUIController;
import arpeggiatorum.supplementary.UnitVariableOutputPort;
import be.tarsos.dsp.pitch.*;
import com.jsyn.ports.UnitInputPort;
import com.jsyn.unitgen.UnitGenerator;

import java.util.Arrays;

public class TarsosPitchDetector extends UnitGenerator {
    private final int offset = 0;
    private final double[] buffer;
    private final PitchDetector detector;
    private final int bufferSize;
    private final double[] pushPitch;
    private final double[] pushConf;
    public UnitInputPort input;
    public UnitVariableOutputPort frequency;
    public UnitVariableOutputPort confidence;
    private boolean running;
    private int cursor;

    public TarsosPitchDetector() {
        this(44100, 2048, PitchProcessor.PitchEstimationAlgorithm.FFT_PITCH);
    }

    public TarsosPitchDetector(float sampleRate, int bufferSize, PitchProcessor.PitchEstimationAlgorithm algo) {
        this.addPort(this.input = new UnitInputPort("Input"));
        this.addPort(this.frequency = new UnitVariableOutputPort("Frequency", 1));
        this.addPort(this.confidence = new UnitVariableOutputPort("Confidence", 1));
        pushPitch = frequency.getData();
        pushConf = confidence.getData();
        this.bufferSize = bufferSize;
        buffer = new double[bufferSize];

        String message = String.format("Tarsos Pitch Detection: Minimum Frequency (%.2fHz) Delay (%.03fs) \r\n", (sampleRate / bufferSize) * 2, (bufferSize / sampleRate) / 2);
        LogGUIController.logBuffer.append(message);

        if (algo == PitchProcessor.PitchEstimationAlgorithm.MPM) {
            detector = new McLeodPitchMethod(sampleRate, bufferSize);
        } else if (algo == PitchProcessor.PitchEstimationAlgorithm.DYNAMIC_WAVELET) {
            detector = new DynamicWavelet(sampleRate, bufferSize);
        } else if (algo == PitchProcessor.PitchEstimationAlgorithm.FFT_YIN) {
            detector = new FastYin(sampleRate, bufferSize);
        } else if (algo == PitchProcessor.PitchEstimationAlgorithm.AMDF) {
            detector = new AMDF(sampleRate, bufferSize);
        } else {
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
