package arpeggiatorum.supplementary;

import arpeggiatorum.supplementary.SortedList;
import com.jsyn.ports.UnitInputPort;
import com.jsyn.unitgen.UnitGenerator;
import com.jsyn.unitgen.UnitSink;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

/**
 * This unit generator reads an input signal (usually an amplitude signal from a PeakFollower)
 * and gets triggered once the signal exceeds the specified positiveCrossingThresholds. It then sends the
 * corresponding MIDI messages to the specified receiver. If the signal goes below the specified
 * negativeCrossingThresholds their corresponding MIDI messages are sent.
 * Usage:   connect the input with a ramped peak follower (ChannelIn - PeakFollower - LinearRamp - LevelTrigger);
 *          set the MIDI message receiver;
 *          levelTrigger.addPositiveCrossingTrigger(0.1, new ShortMessage(EventMaker.NOTE_ON, 60, 100));
 *          levelTrigger.addNegativeCrossingTrigger(0.1, new ShortMessage(EventMaker.NOTE_OFF, 60, 0));
 *          levelTrigger.addPositiveCrossingTrigger(0.2, new ShortMessage(EventMaker.NOTE_ON, 67, 100));
 *          levelTrigger.addNegativeCrossingTrigger(0.2, new ShortMessage(EventMaker.NOTE_OFF, 67, 0));
 * @author Axel Berndt
 */
public class LevelTrigger extends UnitGenerator implements UnitSink, Transmitter {
    public UnitInputPort input;         // the signal input port
    public Receiver receiver;           // the MIDI receiver
    public SortedList<TriggerData> positiveCrossingTriggers = new SortedList<>();
    public SortedList<TriggerData> negativeCrossingTriggers = new SortedList<>();
    private double previousValue = 0.0;

    /**
     * constructor
     * @param receiver the MIDI receiver
     */
    public LevelTrigger(Receiver receiver) {
        this.receiver = receiver;
        addPort(input = new UnitInputPort("Input"));
    }

    /**
     * add a trigger
     * @param triggerData
     * @return
     */
    public boolean addPositiveCrossingTrigger(TriggerData triggerData) {
        return this.positiveCrossingTriggers.add(triggerData);
    }

    /**
     * add a trigger
     * @param threshold
     * @param message
     * @return
     */
    public boolean addPositiveCrossingTrigger(double threshold, MidiMessage message) {
        return this.addPositiveCrossingTrigger(new TriggerData(threshold, message));
    }

    /**
     * add a trigger
     * @param triggerData
     * @return
     */
    public boolean addNegativeCrossingTrigger(TriggerData triggerData) {
        return this.negativeCrossingTriggers.add(triggerData);
    }

    /**
     * add a trigger
     * @param threshold
     * @param message
     * @return
     */
    public boolean addNegativeCrossingTrigger(double threshold, MidiMessage message) {
        return this.addNegativeCrossingTrigger(new TriggerData(threshold, message));
    }

    /**
     * remove a trigger
     * @param triggerData the message part of the triggerData is irrelevant, the threshold is used for removal
     * @return
     */
    public TriggerData removePositiveCrossingTrigger(TriggerData triggerData) {
        return this.positiveCrossingTriggers.remove(triggerData);
    }

    /**
     * remove a trigger
     * @param triggerData the message part of the triggerData is irrelevant, the threshold is used for removal
     * @return
     */
    public TriggerData removeNegativeCrossingTrigger(TriggerData triggerData) {
        return this.negativeCrossingTriggers.remove(triggerData);
    }

    /**
     * process the input signal
     * @param start offset into port buffers
     * @param limit limit offset into port buffers for loop
     */
    @Override
    public void generate(int start, int limit) {
        double[] inputs = input.getValues();
//        for (int i = start; i < limit; i++) {
//            System.out.println(inputs[i]);
//        }

        if (this.getReceiver() == null) {
            this.previousValue = inputs[0];
            return;
        }

        if (this.previousValue == inputs[0])
            return;

        double value1 = this.previousValue;
        double value2 = inputs[0];
        this.previousValue = value2;

        if (value1 < value2) {
            for (TriggerData trigger : this.positiveCrossingTriggers) {
                if (value2 < trigger.threshold)
                    break;
                if (trigger.isTriggered(value1, value2) > 0)
                    this.getReceiver().send((MidiMessage) trigger.message.clone(), -1);
            }
        } else {    // if (value1 > value2)
            for (TriggerData trigger : this.negativeCrossingTriggers) {
                if (value1 < trigger.threshold)
                    break;
                if (trigger.isTriggered(value1, value2) < 0)
                    this.getReceiver().send((MidiMessage) trigger.message.clone(), -1);
            }
        }
    }

    /**
     * get the signal input port
     * @return
     */
    @Override
    public UnitInputPort getInput() {
        return this.input;
    }

    /**
     * set the MIDI receiver
     * @param receiver the desired receiver.
     */
    @Override
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    /**
     * get the MIDI receiver
     * @return
     */
    @Override
    public Receiver getReceiver() {
        return this.receiver;
    }

    /**
     * close procedure
     */
    @Override
    public void close() {
    }

    /**
     * The data objects for the LevelTrigger unit generator.
     * @author Axel Berndt
     */
    public static class TriggerData implements Comparable<TriggerData> {
        public final Double threshold;
        public final MidiMessage message;

        /**
         * constructor
         * @param threshold
         * @param message
         */
        public TriggerData(Double threshold, MidiMessage message) {
            this.threshold = threshold;
            this.message = message;
        }

        /**
         * make this object type comparable
         * @param object the object to be compared.
         * @return
         */
        @Override
        public int compareTo(TriggerData object) {
            return this.threshold.compareTo(object.threshold);
        }

        /**
         * check if the two values cross the threshold
         * @param value1
         * @param value2
         * @return 0=no, -1=negative crossing, 1=positive crossing
         */
        public int isTriggered(double value1, double value2) {
            if (value1 < this.threshold) {
                if (value2 >= this.threshold) {
                    return 1;
                }
                return 0;
            }
            if (value2 < this.threshold) {
                return -1;
            }
            return 0;
        }
    }
}
