package arpeggiatorum.microphoneControl;

import arpeggiatorum.gui.ArpeggiatorumGUI;
import arpeggiatorum.gui.LogGUIController;
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
    public static double minFreq;
    public static double scalingFactor;
    public static boolean autoTune;
    public static int clusterSize;
    public final UnitVariableInputPort[] CQTPorts;
    //CQT
    private final int binsPerOctave = 12;
    private final int binsToCompute = 16;
    private final CQTPitchDetector[] cqtPitchDetectors;
    private final double[] CQTBins;
    public final double[] CQTFrequencies;
    private final int minVelocity;
    private final int maxVelocity;
    private final int diffVelocity;
    int currentAboveThresholdCount = 0;
    private Integer newPitch;
    private int[] CQTBinsSortedIndexes;
    private double PITCH_THRESHOLD;
    private boolean[] currentPitches = new boolean[128];
    private boolean[] currentActive = new boolean[128];
    private boolean[] currentAboveThreshold = new boolean[128];
    private int[] currentVelocities = new int[128];
    private double[] currentMag = new double[128];
    private int currentVelocity;

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

        this.setReceiver(receiver);
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

    @Override
    public void stop() {
        super.stop();
        //Reset everything

        //Mono Version
        currentVelocity = 0;
        currentPitch = -1;

        //Poly Version
        currentPitches = new boolean[128];
        currentActive = new boolean[128];
        currentAboveThreshold = new boolean[128];
        currentVelocities = new int[128];
        currentMag = new double[128];

        //Histogram
        ArpeggiatorumGUI.controllerHandle.updateHist(new double[CQTFrequencies.length]);
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
        currentAboveThresholdCount = 0;
        //Auto-Tune
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < CQTBins.length; i++) {
            newPitch = (int) Math.round(AudioMath.frequencyToPitch(CQTFrequencies[i]));
            //Add all the pitches above the threshold, send a noteoff for everything that falls below
            if (CQTBins[i] >= PITCH_THRESHOLD) {
                currentAboveThreshold[newPitch] = true;
                currentMag[newPitch] = CQTBins[i];
                currentAboveThresholdCount++;
            } else {
                this.sendNoteOff(newPitch);
                currentAboveThreshold[newPitch] = false;
                currentActive[newPitch] = false;
                currentMag[newPitch] = 0.0;
                currentVelocities[newPitch] = 0;
            }
        }
        if (currentAboveThresholdCount > 0) {
            //Go through the 128 MIDI pitches
            int i = 0;
            while (i < 128) {
                //Process further
                if (currentAboveThreshold[i]) {
                    //Start a temporary cluster, a cluster is over when you retrieve the first False.
                    for (int j = i + 1; j < 128; j++) {
                        if (!currentAboveThreshold[j]) {
                            //Then you can compute the length
                            int clusterLen = j - i;
                            //If the cluster has size 1 or greater than 3 you play all the notes
                            if (clusterLen == 1 || clusterLen > clusterSize) {
                                for (int k = i; k < j; k++) {
                                    if (!currentActive[k]) {
                                        //We have to play a new note
                                        //Velocity
                                        double ratioVelocity = Math.clamp(currentMag[k] / PITCH_THRESHOLD, 1.0, 2.0) - 1;
                                        int newVelocity = Math.clamp((int) (minVelocity + (ratioVelocity * diffVelocity)), minVelocity, maxVelocity);
                                        this.sendNoteOn(k, newVelocity);
                                        currentVelocities[k] = newVelocity;
                                        currentActive[k] = true;
                                        message.append(String.format("[%d] %.0fHz: %d ", k, AudioMath.pitchToFrequency(k), newVelocity));
                                    } else {
                                        //Aftertouch
                                        double ratioVelocity = Math.clamp(currentMag[k] / PITCH_THRESHOLD, 1.0, 2.0) - 1;
                                        int newVelocity = Math.clamp((int) (minVelocity + (ratioVelocity * diffVelocity)), minVelocity, maxVelocity);
                                        if (newVelocity != currentVelocities[k]) {
                                            this.sendAftertouch(k, newVelocity);
                                            currentVelocities[k] = newVelocity;
                                        }
                                    }
                                }
                            } else {
                                //I have a cluster of size X, I have to determine the maximum
                                double localMax = currentMag[i];
                                int localPitch = i;
                                for (int k = i; k < j; k++) {
                                    if (currentMag[k] > localMax) {
                                        int copyPitch = localPitch;
                                        localMax = currentMag[k];
                                        localPitch = k;
                                        //Reset the others to 0
                                        currentVelocities[copyPitch] = 0;
                                        currentPitches[copyPitch] = false;
                                        currentMag[copyPitch] = 0.0;
                                    } else {
                                        currentVelocities[k] = 0;
                                        currentPitches[k] = false;
                                        currentMag[k] = 0.0;
                                    }
                                }
                                if (!currentActive[localPitch]) {
                                    //We have to play a new note
                                    double ratioVelocity = Math.clamp(localMax / PITCH_THRESHOLD, 1.0, 2.0) - 1;
                                    int newVelocity = Math.clamp((int) (minVelocity + (ratioVelocity * diffVelocity)), minVelocity, maxVelocity);
                                    this.sendNoteOn(localPitch, newVelocity);
                                    currentActive[localPitch] = true;
                                    currentVelocities[localPitch] = newVelocity;
                                    message.append(String.format("[%d] %.0fHz: %d ", localPitch, AudioMath.pitchToFrequency(localPitch), newVelocity));
                                }
                            }
                            i = j + 1;
                            break;
                        }
                    }
                }
                //Or advance to the next pitch
                else {
                    i++;
                }
            }
            //Print what's playing
            if (!message.isEmpty()) {
                LogGUIController.logBuffer.append(message + "\r\n");
            }
        }
    }

    private void playPoly() {
        //Polyphonic version
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < CQTBins.length; i++) {
            newPitch = (int) Math.round(AudioMath.frequencyToPitch(CQTFrequencies[i]));
            if (CQTBins[i] >= PITCH_THRESHOLD) {
                if (!currentPitches[newPitch]) {
                    //We have to play a new note
                    //Velocity
                    currentPitches[newPitch] = true;
                    double ratioVelocity = Math.clamp(CQTBins[i] / PITCH_THRESHOLD, 1.0, 2.0) - 1;
                    int newVelocity = Math.clamp((int) (minVelocity + (ratioVelocity * diffVelocity)), minVelocity, maxVelocity);
                    currentVelocities[newPitch] = newVelocity;
                    this.sendNoteOn(newPitch, newVelocity);

                    message.append(String.format("[%d] %.0fHz: %d ", newPitch, CQTFrequencies[i], newVelocity));
                } else {
                    //Aftertouch
                    double ratioVelocity = Math.clamp(CQTBins[i] / PITCH_THRESHOLD, 1.0, 2.0) - 1;
                    int newVelocity = Math.clamp((int) (minVelocity + (ratioVelocity * diffVelocity)), minVelocity, maxVelocity);
                    if (newVelocity != currentVelocities[newPitch]) {
                        this.sendAftertouch((int) Math.round(AudioMath.frequencyToPitch(CQTFrequencies[i])), newVelocity);
                        currentVelocities[newPitch] = newVelocity;
                    }
                }
            } else {
                if (currentPitches[newPitch]) {
                    this.sendNoteOff(newPitch);
                    currentPitches[newPitch] = false;
                    currentVelocities[newPitch] = 0;
                }
            }
        }
        if (!message.isEmpty()) {
            LogGUIController.logBuffer.append(message + "\r\n");
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
                LogGUIController.logBuffer.append(message);
            } else {
                //Aftertouch
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
        double modValue = value / scalingFactor;
        PITCH_THRESHOLD = modValue / 2;
        ArpeggiatorumGUI.controllerHandle.yAxis.setUpperBound(modValue);
        ArpeggiatorumGUI.controllerHandle.yAxis.setTickUnit(modValue/10);
    }
}
