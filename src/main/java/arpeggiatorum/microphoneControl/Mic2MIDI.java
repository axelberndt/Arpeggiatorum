package arpeggiatorum.microphoneControl;

import arpeggiatorum.gui.GUI;
import com.jsyn.unitgen.ChannelIn;
import com.jsyn.unitgen.Circuit;
import com.jsyn.unitgen.SchmidtTrigger;
import meico.midi.EventMaker;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

public abstract class Mic2MIDI extends Circuit implements Transmitter, IMic2MIDI {
    public String NAME = "Abstract";
    public boolean POLY = false;
    public Receiver receiver;// the MIDI receiver
    public final ChannelIn channelIn = new ChannelIn();// microphone input
    public static final double sampleRate = 44100.00;
    protected int currentPitch = -1;
    protected final SchmidtTrigger schmidtTrigger = new SchmidtTrigger();
    public static final double SET_LEVEL = 0.5;
    public static final double DIFFERENCE_LEVEL = 0.05;
    public static final double RESET_LEVEL = SET_LEVEL - DIFFERENCE_LEVEL;
    public static final double CONFIDENCE_THRESHOLD = 0.3;
    public static final double FREQUENCY_RAMP_TIME = 0.01;
    public static final double PEAK_FOLLOWER_RAMP_TIME = 0.25;

    /**
     * set the receiver of outgoing MIDI messages
     *
     * @param receiver the desired receiver.
     */
    @Override
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    /**
     * get the receiver of outgoing MIDI messages
     *
     * @return The MIDI Receiver
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
     * @return
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * @return
     */
    @Override
    public Boolean isPoly() {
        return POLY;
    }


    @Override
    public String toString() {
        return String.format(this.getName() + (isPoly() ? " (Polyphonic)" : " (Monophonic)"));
    }

    /**
     * set the amplitude level above which output will be triggered
     *
     * @param value Threshold for input
     */
    public void setSignalToNoiseThreshold(double value) {
        this.schmidtTrigger.setLevel.set(value);
        this.schmidtTrigger.resetLevel.set(Math.max(0.0, value - DIFFERENCE_LEVEL));
    }

    public void sendNoteOn(int pitch) {
        ShortMessage noteOn;
        try {
            noteOn = new ShortMessage(EventMaker.NOTE_ON, pitch, 100);
        } catch (InvalidMidiDataException e) {
            //e.printStackTrace();
            GUI.updateLogGUI(e.getMessage());
            return;
        }
        this.getReceiver().send(noteOn, -1);
        this.currentPitch = pitch;
    }

    public void sendNoteOff(int pitch) {
        ShortMessage noteOff;
        try {
            noteOff = new ShortMessage(EventMaker.NOTE_OFF, pitch, 0);
        } catch (InvalidMidiDataException e) {
            //e.printStackTrace();
            GUI.updateLogGUI(e.getMessage());
            return;
        }
        this.getReceiver().send(noteOff, -1);
        this.currentPitch = -1;
    }

    @Override
    public void start() {
        super.start();
        currentPitch = -1;
    }
}