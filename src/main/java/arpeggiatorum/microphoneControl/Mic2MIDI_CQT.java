package arpeggiatorum.microphoneControl;

import arpeggiatorum.gui.GUI;
import arpeggiatorum.supplementary.UnitVariableInputPort;

import com.softsynth.math.AudioMath;

import javax.sound.midi.Receiver;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class reads the microphone signal, performs a basic pitch detection and outputs corresponding MIDI messages to the specified Receiver.
 *
 * @author Davide Mauro
 */
public class Mic2MIDI_CQT extends Mic2MIDI {
    //CQT
    private final int binsPerOctave = 12;
    private final int binsToCompute = 16;
    private final CQTPitchDetector[] cqtPitchDetectors;
    private Integer newPitch;
    public final UnitVariableInputPort[] CQTPorts;
    private final double[] CQTBins;
    private final double[] CQTFrequencies;
    private int[] CQTBinsSortedIndexes;
    public static double minFreq;
    //Histogram
    public static CQTHistogram cqtHist;
    private double PITCH_THRESHOLD;
    private boolean[] currentPitches;
    private double[] currentMag;
    public static boolean autoTune;
    private final int minVelocity;
    private final int maxVelocity;
    private final int diffVelocity;
    private int currentVelocity;

    @Override
    public void stop() {
        super.stop();
        currentPitches = new boolean[CQTBins.length];
        currentMag = new double[CQTBins.length];
        cqtHist.updateBins(new double[CQTHistogram.binSize]);
        GUI.cqtBinsPanel.revalidate();
        GUI.cqtBinsPanel.repaint();
    }

    public Mic2MIDI_CQT(Receiver receiver, double sampleRate, double minFreq, double maxFreq, float threshold, float spread, boolean isPoly, boolean autoTune, int cqtMinVel, int cqtMaxVel) {
        super(sampleRate);
        NAME = "CQT-Based Pitch Detector";
        POLY = isPoly;
        Mic2MIDI_CQT.minFreq = minFreq;
        Mic2MIDI_CQT.autoTune = autoTune;
        minVelocity = cqtMinVel;
        maxVelocity = cqtMaxVel;
        diffVelocity = maxVelocity - minVelocity;
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
            cqtPitchDetectors[i] = new CQTPitchDetector((float) sampleRate, bands.get(i).floatValue(), bands.get(i + 1).floatValue(), binsPerOctave, threshold, spread);
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
        currentPitches = new boolean[CQTBins.length];
        currentMag = new double[currentPitches.length];
        //CQT Histogram
        double[] initializer = new double[CQTFrequencies.length];
        cqtHist = new CQTHistogram(initializer, CQTFrequencies);
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
        if (!isPoly()) {
            playMono();
        } else {
            if (autoTune) {
                playPolyTune();
            } else {
                playPoly();
            }
        }


    }

    private void playPolyTune() {
        //Auto-Tune
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < CQTBins.length; i++) {
            newPitch = (int) Math.round(AudioMath.frequencyToPitch(CQTFrequencies[i]));
            //Add all the pitches above the threshold, send a noteoff for everything that falls below
            if (CQTBins[i] >= PITCH_THRESHOLD) {
                if (currentPitches[i] == false) {
                    //This pitch is new
                    currentPitches[i] = true;
                    currentMag[i] = CQTBins[i];
                }
            } else {
                    this.sendNoteOff(newPitch);
                    currentPitches[i] = false;
                    currentMag[i] = 0.0;
            }
        }
        //Auto-Tune post-processing, sends correct NoteOn
        for (int i = 0; i < currentPitches.length; i++) {
            if (currentPitches[i] == true) {
                //Add to a temporary cluster, a cluster is over when you retrieve the first False. Then you can compute the length
                for (int j = i + 1; j < currentPitches.length; j++) {
                    if (currentPitches[j] == false) {
                        int clusterLen = j - i;
                        if (clusterLen == 1 || clusterLen > 3) {
                            for (int k = i; k <= j; k++) {
                                //We have to play a new note
                                //Velocity
                                double ratioVelocity = Math.clamp(CQTBins[k] / PITCH_THRESHOLD, 1.0, 2.0) - 1;
                                int newVelocity = Math.clamp((int) (minVelocity + (ratioVelocity * diffVelocity)), minVelocity, maxVelocity);
                                int pitchNow = (int) Math.round(AudioMath.frequencyToPitch(CQTFrequencies[k]));
                                this.sendNoteOn(pitchNow, newVelocity);

                                message.append(String.format("[%d] %.0fHz: %d ", pitchNow, CQTFrequencies[k], newVelocity));
                            }
                        } else {
                            double localMax = currentMag[i];
                            int localPitch = i;
                            for (int k = i; k <= j; k++) {
                                if (currentMag[k] > localMax) {
                                    localMax = currentMag[k];
                                    localPitch = k;
                                }
                            }
                            double ratioVelocity = Math.clamp(localMax / PITCH_THRESHOLD, 1.0, 2.0) - 1;
                            int newVelocity = Math.clamp((int) (minVelocity + (ratioVelocity * diffVelocity)), minVelocity, maxVelocity);
                            int pitchNow = (int) Math.round(AudioMath.frequencyToPitch(CQTFrequencies[localPitch]));
                            this.sendNoteOn(pitchNow, newVelocity);

                            message.append(String.format("[%d] %.0fHz: %d ", pitchNow, CQTFrequencies[i], newVelocity));
                        }
                        i = j;
                    } else {
                        //Aftertouch?
                    }
                }
            }
        }
        if (!message.isEmpty()) {
            GUI.updateLogGUI(message + "\r\n");
        }
    }

    private void playPoly() {
        //Polyphonic version
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < CQTBins.length; i++) {
            newPitch = (int) Math.round(AudioMath.frequencyToPitch(CQTFrequencies[i]));
            if (CQTBins[i] >= PITCH_THRESHOLD) {
                if (currentPitches[i] == false) {
                    //We have to play a new note
                    //Velocity
                    currentPitches[i] = true;
                    double ratioVelocity = Math.clamp(CQTBins[i] / PITCH_THRESHOLD, 1.0, 2.0) - 1;
                    int newVelocity = Math.clamp((int) (minVelocity + (ratioVelocity * diffVelocity)), minVelocity, maxVelocity);

                    this.sendNoteOn(newPitch, newVelocity);

                    message.append(String.format("[%d] %.0fHz: %d ", newPitch, CQTFrequencies[i], newVelocity));
                } else {
                    double ratioVelocity = Math.clamp(CQTBins[i] / PITCH_THRESHOLD, 1.0, 2.0) - 1;
                    int newVelocity = Math.clamp((int) (minVelocity + (ratioVelocity * diffVelocity)), minVelocity, maxVelocity);
                    if (newVelocity != currentVelocity) {
                        this.sendAftertouch(newPitch, newVelocity);
                        currentVelocity = newVelocity;
                    }
                }
            } else {
                if (currentPitches[i] == true) {
                    this.sendNoteOff(newPitch);
                    currentPitches[i] = false;
                }
            }
        }
        if (!message.isEmpty()) {
            GUI.updateLogGUI(message + "\r\n");
        }
    }

    private void playMono() {
        CQTBinsSortedIndexes = getMaxBins(CQTBins, binsToCompute);
        if (CQTBins[CQTBinsSortedIndexes[0]] >= (PITCH_THRESHOLD)) {
            int newPitch = (int) Math.round(AudioMath.frequencyToPitch(CQTFrequencies[CQTBinsSortedIndexes[0]]));
            if (newPitch != currentPitch) {
                if (currentPitch != -1) {
                    this.sendNoteOff(this.currentPitch);
                }
                //Velocity
                double ratioVelocity = Math.clamp(CQTBins[CQTBinsSortedIndexes[0]] / PITCH_THRESHOLD, 1.0, 2.0) - 1;
                int newVelocity = Math.clamp((int) (minVelocity + (ratioVelocity * diffVelocity)), minVelocity, maxVelocity);
                currentVelocity = newVelocity;
                this.sendNoteOn(newPitch, newVelocity);

                currentPitch = newPitch;
                String message = String.format("[%d] %.0fHz: %d\r\n", newPitch, CQTFrequencies[CQTBinsSortedIndexes[0]], newVelocity);
                GUI.updateLogGUI(message);
            } else {
                double ratioVelocity = Math.clamp(CQTBins[CQTBinsSortedIndexes[0]] / PITCH_THRESHOLD, 1.0, 2.0) - 1;
                int newVelocity = Math.clamp((int) (minVelocity + (ratioVelocity * diffVelocity)), minVelocity, maxVelocity);
                if (newVelocity != currentVelocity) {
                    this.sendAftertouch(currentPitch, newVelocity);
                    currentVelocity = newVelocity;
                }
            }
        } else {
            if (currentPitch != -1) {
                this.sendNoteOff(currentPitch);
                currentPitch = -1;
            }
        }
    }

    @Override
    public void setSignalToNoiseThreshold(double value) {
        float scalingFactor = 5.0f; //This is quite arbitrary, different interfaces get considerable different ranges, can we do better?
        double modValue = value / scalingFactor;
        PITCH_THRESHOLD = modValue / 2;
        cqtHist.max = modValue;
    }

    /**
     * Return the indexes corresponding to the top-k largest in an array.
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
