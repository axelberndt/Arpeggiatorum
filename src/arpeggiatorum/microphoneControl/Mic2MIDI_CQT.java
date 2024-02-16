package arpeggiatorum.microphoneControl;

import arpeggiatorum.supplementary.UnitVariableInputPort;

import be.tarsos.dsp.AudioEvent;
import com.jsyn.unitgen.*;

import com.softsynth.math.AudioMath;
import meico.midi.EventMaker;

import javax.naming.Name;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.JFrame;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class reads the microphone signal, performs a basic pitch detection and outputs corresponding MIDI messages to the specified Receiver.
 *
 * @author Davide Mauro
 */
public class Mic2MIDI_CQT extends Mic2MIDI {

    //Lower and upper boundaries for Pitch detection
    //E1-G6 gives us 64 CQT-bins (power of 2)
    public static final double minFreq = 41.205; //E1
    public static final double maxFreq = 2637.02; //E7


    //CQT
    public static final int binsPerOctave = 12;
    public static final int binsToCompute = 16;
    private final CQTPitchDetector[] cqtPitchDetectors;
    public final UnitVariableInputPort[] CQTPorts;
    double[] CQTBins;
    double[] CQTFrequencies;
    int[] CQTBinsSortedIndexes;
    //Histogram
    public static final JFrame cqtBinsFrame = new JFrame("CQT Bins");
    public static CQTHistogram cqtHist;
    private double PITCH_THRESHOLD;

    public Mic2MIDI_CQT(Receiver receiver, boolean isPoly) {
        NAME = "CQT-Based Pitch Detector";
        POLY = isPoly;
        // Instantiate ports

        // Build DSP patch
        this.add(this.channelIn);// add channelIn to the synth

        //Build a CQT-Factory of 1-Octave-band CQTs
        double bandSplitter = minFreq;
        ArrayList<Double> bands = new ArrayList<>();
        while (bandSplitter < maxFreq) {
            bands.add(bandSplitter);
            bandSplitter *= 2.0f;
        }

        int bandNum = bands.size();
        bands.add(maxFreq);
        cqtPitchDetectors = new CQTPitchDetector[bandNum];
        CQTPorts = new UnitVariableInputPort[bandNum];
        for (int i = 0; i < bandNum; i++) {
            cqtPitchDetectors[i] = new CQTPitchDetector((float) sampleRate, bands.get(i).floatValue(), bands.get(i + 1).floatValue(), binsPerOctave);
            cqtPitchDetectors[i].input.connect(0, this.channelIn.output, 0);
            this.add(cqtPitchDetectors[i]);
            addPort(this.CQTPorts[i] = new UnitVariableInputPort("CQTBins"));
            this.CQTPorts[i].connect(cqtPitchDetectors[i].output);
        }
        int sumBins = 0;
        for (CQTPitchDetector bins : cqtPitchDetectors) {
            sumBins += bins.frequencies.length;
        }
        CQTBins = new double[sumBins];
        CQTFrequencies = new double[sumBins];
        for (int i = 0; i < sumBins; i++) {
            CQTFrequencies[i] = (float) (minFreq * Math.pow(2, i / (float) binsPerOctave));

        }

        //CQT Histogram
        double[] initializer = new double[CQTFrequencies.length];
        cqtHist = new CQTHistogram(initializer, CQTFrequencies);

        cqtBinsFrame.add(cqtHist);
        cqtBinsFrame.pack();
        cqtBinsFrame.setLocationRelativeTo(null);
        cqtBinsFrame.setVisible(true);

        this.setReceiver(receiver);
    }

    @Override
    public void generate(int start, int limit) {
        super.generate(start, limit);


        //Pull CQT Data from individual Bands
        for (int i = 0; i < CQTPorts.length; i++) {
            if (CQTPorts[i].isAvailable()) {
                double[] tempData = CQTPorts[i].getData();
                System.arraycopy(tempData, 0, CQTBins, (i * binsPerOctave), tempData.length);
            } else {
                for (int j = 0; j < CQTPorts[i].getData().length; j++) {
                    CQTBins[j + (i * binsPerOctave)] = 0.0;
                }
            }
        }

        CQTBinsSortedIndexes = getMaxBins(CQTBins, binsToCompute);
        if (!isPoly()) {
            if (CQTBins[CQTBinsSortedIndexes[0]] >= (PITCH_THRESHOLD)) {
                int newPitch = (int) Math.round(AudioMath.frequencyToPitch(CQTFrequencies[CQTBinsSortedIndexes[0]]));
                if (newPitch != currentPitch) {
                    if (currentPitch != -1) {
                        this.sendNoteOff(this.currentPitch);
                    }
                    this.sendNoteOn(newPitch);
                    currentPitch = newPitch;
                    String message = String.format("[%d] %.0fHz", newPitch, CQTFrequencies[CQTBinsSortedIndexes[0]]);
                    System.out.println(message);
                }
            } else {
                if (currentPitch != -1) {
                    this.sendNoteOff(currentPitch);
                    currentPitch = -1;
                }
            }
        } else {
            System.out.print("- Pitches using CQT: ");
            for (int i = 0; i < CQTBinsSortedIndexes.length; i++) {
                System.out.printf("[%d] %.0fHz", i, CQTFrequencies[CQTBinsSortedIndexes[i]]);
            }
            System.out.print("\r\n");
        }


    }

    @Override
    public void setSignalToNoiseThreshold(double value) {
        double modValue=value/20.0f;
        PITCH_THRESHOLD = modValue / 2;
        cqtHist.max = modValue;
    }

    /**
     * Return the indexes correspond to the top-k largest in an array.
     */
    public static int[] getMaxBins(double[] array, int top_k) {
        double[] max = new double[top_k];
        int[] maxIndex = new int[top_k];
        Arrays.fill(max, Double.NEGATIVE_INFINITY);
        Arrays.fill(maxIndex, -1);

        top:
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < top_k; j++) {
                if (array[i] > max[j]) {
                    for (int x = top_k - 1; x > j; x--) {
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
