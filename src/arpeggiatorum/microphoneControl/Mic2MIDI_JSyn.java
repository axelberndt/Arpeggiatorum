package arpeggiatorum.microphoneControl;

import arpeggiatorum.gui.GUI;
import com.jsyn.ports.*;
import com.jsyn.unitgen.*;

import com.softsynth.math.AudioMath;

import meico.midi.EventMaker;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

import java.util.stream.DoubleStream;

/**
 * This class reads the microphone signal, performs a basic pitch detection and outputs corresponding MIDI messages to the specified Receiver.
 *
 * @author Axel Berndt
 */
public class Mic2MIDI_JSyn extends Mic2MIDI {
    public UnitInputPort trigger;// this port gets a 1.0 to trigger and a 0.0 to do nothing
    private double previousTriggerValue = 0.0;
    public UnitInputPort frequency;


    /**
     * constructor
     * channelIn
     * ____________________|___________________
     * |                                       |
     * pitchDetector                            peakFollower
     * frequency     confidence                            |
     * |              |                         peakFollowerRamp
     * frequencyRamp        |                                |
     * |              |                          schmidtTrigger
     * |              |_____________   ________________|
     * |                           |  |
     * |                         multiply
     * |                            |
     * frequencyInputPort            triggerInputPort
     */
    public Mic2MIDI_JSyn(Receiver receiver) {
        NAME = "JSyn AutoCorrelation";

        // Instantiate ports
        addPort(this.trigger = new UnitInputPort("Trigger"));
        addPort(this.frequency = new UnitInputPort("Frequency"));

        // Build DSP patch
        this.add(this.channelIn);// add channelIn to the synth
        //this.channelIn.setChannelIndex(0);// set its channel index, this call is redundant

        //This pitch detector returns a value that is one octave lower, compensate by adding +12
        PitchDetector pitchDetector = new PitchDetector();
        pitchDetector.input.connect(0, this.channelIn.output, 0);
        this.add(pitchDetector);

        //Frequency of the pitch to be determined
        LinearRamp frequencyRamp = new LinearRamp();
        frequencyRamp.time.set(FREQUENCY_RAMP_TIME);
        frequencyRamp.input.connect(0, pitchDetector.frequency, 0);
        this.add(frequencyRamp);
        this.frequency.connect(0, frequencyRamp.output, 0);

        //Takes the input --> Peak Follower --> Linear Ramp --> Schmidt Trigger --> Multiply by confidence of pitch detection
        PeakFollower peakFollower = new PeakFollower();
        peakFollower.input.connect(0, this.channelIn.output, 0);
        this.add(peakFollower);

        LinearRamp peakFollowerRamp = new LinearRamp();
        peakFollowerRamp.time.set(PEAK_FOLLOWER_RAMP_TIME);       // ramp time, smaller=more sensitive
        peakFollowerRamp.input.connect(0, peakFollower.output, 0);
        this.add(peakFollowerRamp);

        this.schmidtTrigger.input.connect(0, peakFollowerRamp.output, 0);
        this.schmidtTrigger.setLevel.set(SET_LEVEL);
        this.schmidtTrigger.resetLevel.set(RESET_LEVEL);
        this.add(this.schmidtTrigger);

        Multiply multiply = new Multiply();
        multiply.inputA.connect(0, this.schmidtTrigger.output, 0);
        multiply.inputB.connect(0, pitchDetector.confidence, 0);
        this.trigger.connect(0, multiply.output, 0);
        this.add(multiply);

        //System.out.printf("Jsyn Pitch Detection: Variable delay \r\n");
        GUI.logMessages.append("Jsyn Pitch Detection: Variable delay \r\n");

        this.setReceiver(receiver);
        // it is not necessary to start any of the unit generators individually, as the Circuit should be started by its creator
    }

    @Override
    public void generate(int start, int limit) {
        super.generate(start, limit);

        double[] triggerInputs = this.trigger.getValues();
        double[] frequencyInputs = this.frequency.getValues();

        int newPitch = (int) Math.round(AudioMath.frequencyToPitch(DoubleStream.of(frequencyInputs).average().getAsDouble())) + 12;

        //   Check if at the end of the buffer we have to play or stop a note
        if (this.previousTriggerValue > CONFIDENCE_THRESHOLD) {         // we are currently playing a tone
            if (triggerInputs[0] <= CONFIDENCE_THRESHOLD) {     // [limit -1] if we have to stop the note
//                System.out.println("> " + this.currentPitch);
//                System.out.println("> Auto-correlation Pitch: " + DoubleStream.of(frequencyInputs).average().getAsDouble());
                GUI.logMessages.append("> " + this.currentPitch + "\r\n");
                GUI.logMessages.append("> Auto-correlation Pitch: " + DoubleStream.of(frequencyInputs).average().getAsDouble() + "\r\n");
                GUI.logMessages.append("\r\n");
                this.sendNoteOff(this.currentPitch);
            } else {                                                    // we may have to update the pitch
                // System.out.println("- " + currentPitch);
                if (newPitch != this.currentPitch) {
//                    System.out.println("- " + this.currentPitch + " ->" + newPitch);
//                    System.out.println("- Auto-correlation Pitch: " + DoubleStream.of(frequencyInputs).average().getAsDouble());
                    GUI.logMessages.append("- " + this.currentPitch + " ->" + newPitch+ "\r\n");
                    GUI.logMessages.append("- Auto-correlation Pitch: " + DoubleStream.of(frequencyInputs).average().getAsDouble()+ "\r\n");
                    this.sendNoteOff(this.currentPitch);
                    this.sendNoteOn(newPitch);
                }
            }
        } else if (triggerInputs[0] > CONFIDENCE_THRESHOLD) {   //[limit -1] we have to start a note
//            System.out.println("< " + this.currentPitch);
//            System.out.println("< Auto-correlation Pitch: " + DoubleStream.of(frequencyInputs).average().getAsDouble());
//            System.out.print("\r\n");
            GUI.logMessages.append("< " + this.currentPitch + "\r\n");
            GUI.logMessages.append("< Auto-correlation Pitch: " + DoubleStream.of(frequencyInputs).average().getAsDouble() + "\r\n");
            GUI.logMessages.append("\r\n");
            this.sendNoteOn(newPitch);
        }

        this.previousTriggerValue = triggerInputs[0]; //[limit - 1]
    }
}
