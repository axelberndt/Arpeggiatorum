package arpeggiatorum.microphoneControl;

import arpeggiatorum.gui.GUI;
import arpeggiatorum.supplementary.UnitVariableInputPort;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import com.softsynth.math.AudioMath;


import javax.sound.midi.Receiver;

/**
 * This class reads the microphone signal, performs a basic pitch detection and outputs corresponding MIDI messages to the specified Receiver.
 *
 * @author Davide Mauro
 */
public class Mic2MIDI_Tarsos extends Mic2MIDI {

    // Tarsos
    private final double pitchThreshold;
    private final int bufferSize;
    private static final int INTERVAL = 0;
    public UnitVariableInputPort confidence;
    public UnitVariableInputPort pitch;

    public Mic2MIDI_Tarsos(Receiver receiver, double sampleRate, int bufferSize, double pitchThreshold) {
        super(sampleRate);
        NAME = "Tarsos Pitch Detector";
        this.bufferSize=bufferSize;
        this.pitchThreshold =pitchThreshold;
        // Build DSP patch
        this.add(this.channelIn);// add channelIn to the synth

        // Tarsos Pitch Detector
        addPort(this.confidence = new UnitVariableInputPort("Confidence"));
        addPort(this.pitch = new UnitVariableInputPort("Pitch"));

        //Problems with internal representation?
        TarsosPitchDetector tarsosPitchDetector = new TarsosPitchDetector((float) sampleRate * 2, bufferSize * 2, PitchEstimationAlgorithm.FFT_YIN);
        tarsosPitchDetector.input.connect(0, this.channelIn.output, 0);
        tarsosPitchDetector.frequency.connect(this.pitch);
        tarsosPitchDetector.confidence.connect(this.confidence);

        this.add(tarsosPitchDetector);
        this.setReceiver(receiver);
    }

    @Override
    public void generate(int start, int limit) {
        super.generate(start, limit);

        //Tarsos
        double[] confidenceInputs = this.confidence.getData();
        double[] pitchInputs = this.pitch.getData();
        if (pitchInputs[0] > 0) {
            if (confidenceInputs[0] >= pitchThreshold) {
                int newPitch = (int) Math.round(AudioMath.frequencyToPitch(pitchInputs[0]));
                if ((newPitch > currentPitch + INTERVAL) || (newPitch < currentPitch - INTERVAL)) {
                    if (currentPitch != -1) {
                        this.sendNoteOff(this.currentPitch);
                    }
                    this.sendNoteOn(newPitch);
                    currentPitch = newPitch;
                    String message = String.format("Pitch detected : %.2fHz MIDI %d ( %.2f probability)\n", pitchInputs[0], newPitch, confidenceInputs[0]);
                    GUI.updateLogGUI(message);
                }
            }
        } else {
            if (currentPitch != -1) {
                this.sendNoteOff(currentPitch);
                currentPitch = -1;
            }
        }
    }

    public static float[] toFloatArray(double[] arr) {
        if (arr == null) return null;
        int n = arr.length;
        float[] ret = new float[n];
        for (int i = 0; i < n; i++) {
            ret[i] = (float) arr[i];
        }
        return ret;
    }

    public static double[] toDoubleArray(float[] arr) {
        if (arr == null) return null;
        int n = arr.length;
        double[] ret = new double[n];
        for (int i = 0; i < n; i++) {
            ret[i] = arr[i];
        }
        return ret;
    }
}
