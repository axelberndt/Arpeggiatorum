package arpeggiatorum;

import arpeggiatorum.gui.ArpeggiatorumGUI;
import arpeggiatorum.gui.LogGUIController;
import arpeggiatorum.notePool.NoteItem;
import arpeggiatorum.notePool.NotePool;
import arpeggiatorum.supplementary.EventMaker;
import com.jsyn.Synthesizer;
import com.softsynth.shared.time.ScheduledCommand;
import com.softsynth.shared.time.TimeStamp;

import javax.sound.midi.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the actual arpeggiator.
 *
 * @author Axel Berndt
 */
public class Arpeggiator implements Receiver, Transmitter {
    public static final String version = "1.0.1";
    public static final int ARPEGGIO_CHANNEL_PRESET = 1;
    public static final int HELD_NOTES_CHANNEL_PRESET = 2;
    public static final int BASS_CHANNEL_PRESET = 0;
    private final Synthesizer synth;
    private final NotePool notePool = new NotePool();
    private MidiDevice inDevice = null;
    private int inputChannel = 0;
    private MidiDevice outDevice = null;
    private Receiver outReceiver = null;
    private Receiver monitorReceiver = null;
    private int arpeggioChannel = ARPEGGIO_CHANNEL_PRESET;
    private int heldNotesChannel = HELD_NOTES_CHANNEL_PRESET;
    private int bassChannel = BASS_CHANNEL_PRESET;
    private ScheduledCommand scheduledCommand = null;
    private double tempo = 500.0;
    private double beatLengthInSeconds = 60.0 / this.tempo;
    private double articulation = 0.0;  // should be in +/- 0.5 to increase/decrease the note length by its half

    /**
     * default constructor
     *
     * @param synth   synthesizer used for scheduling
     * @param monitor a receiver where incoming events can be forwarded for monitoring
     */
    public Arpeggiator(Synthesizer synth, Receiver monitor) {
        // the synth is used for precise scheduling (more precise than Java's standard Threads)
        this.synth = synth;
        this.setReceiver(monitor);
    }

    /**
     * switch MIDI input device
     *
     * @param inDevice
     * @throws MidiUnavailableException
     */
    public void setMIDIIn(MidiDevice inDevice) throws MidiUnavailableException {
        if (this.inDevice != null)
            this.inDevice.close();

        this.inDevice = inDevice;
        this.inDevice.getTransmitter().setReceiver(this);   // connect the transmitter of the input device to this arpeggiator; this will effectively invoke the send() method whenever a MIDI message comes in and passes that message to send()
        this.inDevice.open();
    }

    /**
     * switch MIDI output device
     *
     * @param outDevice
     * @throws MidiUnavailableException
     */
    public void setMIDIOut(MidiDevice outDevice) throws MidiUnavailableException {
        if (this.outDevice != null)
            this.outDevice.close();

        this.outDevice = outDevice;
        //this.outDevice.getTransmitter().setReceiver(this);
        this.outReceiver = outDevice.getReceiver();
        this.outDevice.open();
    }

    /**
     * set the input MIDI channel
     *
     * @param channel
     */
    public void setInputChannel(int channel) {
        this.inputChannel = channel;
    }

    /**
     * get the channel number where arpeggios are played
     *
     * @return
     */
    public int getArpeggioChannel() {
        return this.arpeggioChannel;
    }

    /**
     * set the output MIDI channel for arpeggios; stop all notes on the previous channel before switching to the new one
     *
     * @param channel
     */
    public void setArpeggioChannel(int channel) {
        if (this.arpeggioChannel >= 0) {
            try {
                this.outReceiver.send(new ShortMessage(EventMaker.CONTROL_CHANGE, this.arpeggioChannel, EventMaker.CC_All_Notes_Off, 0), -1);
            } catch (Exception e) {
                Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
                logger.log(Level.SEVERE, "MIDI Exception.", e);
                LogGUIController.logBuffer.append(e.getMessage());

            }
        }
        this.arpeggioChannel = channel;
    }

    /**
     * get the channel number for playing held notes
     *
     * @return
     */
    public int getHeldNotesChannel() {
        return this.heldNotesChannel;
    }

    /**
     * set the output MIDI channel for held notes; stop all notes on the previous channel before switching to the new one
     *
     * @param channel
     */
    public void setHeldNotesChannel(int channel) {
        // stop the triggered notes individually if they are currently played
        if (this.heldNotesChannel >= 0) {
            for (NoteItem note : this.notePool.getTriggeredNotes())
                this.outReceiver.send(note.toShortMessage(this.heldNotesChannel, false), -1);
        }
        this.heldNotesChannel = channel;

        // start the notes on the new channel
        if (this.heldNotesChannel >= 0) {
            for (NoteItem note : this.notePool.getTriggeredNotes())
                this.outReceiver.send(note.toShortMessage(this.heldNotesChannel, true), -1);
        }
    }

    /**
     * get the bass channel number
     *
     * @return
     */
    public int getBassChannel() {
        return this.bassChannel;
    }

    /**
     * set the output MIDI channel for bass notes; stop all notes on the previous channel before switching to the new one
     *
     * @param channel
     */
    public void setBassChannel(int channel) {
        NoteItem note = this.notePool.getBassNote();

        // stop the bass note if any is currently played
        if (this.bassChannel >= 0)
            if (note != null)
                this.outReceiver.send(note.toShortMessage(this.bassChannel, false), -1);

        this.bassChannel = channel;

        // start the bass note on the new channel
        if (this.bassChannel >= 0)
            if (note != null)
                this.outReceiver.send(note.toShortMessage(this.bassChannel, true), -1);
    }

    /**
     * set arpeggiation tempo
     *
     * @param tempo
     */
    public void setTempo(double tempo) {
        this.tempo = tempo;
        this.beatLengthInSeconds = 60.0 / this.tempo;
    }

    /**
     * set the articulation of the notes
     *
     * @param articulation value in [-0.5, 0.5]
     */
    public void setArticulation(double articulation) {
        this.articulation = Math.min(Math.max(articulation, -0.5), 0.5);
    }

    /**
     * get the current pattern
     *
     * @return
     */
    public NotePool.Pattern getPattern() {
        return this.notePool.getPattern();
    }

    /**
     * switch the note provider pattern
     *
     * @param pattern
     */
    public void setPattern(NotePool.Pattern pattern) {
        this.notePool.setPattern(pattern);
    }

    /**
     * specify the additional intervals that the arpeggiator plays to each struck note
     *
     * @param intervals
     */
    public void setTonalEnrichment(int[] intervals) {
        this.notePool.setTonalEnrichment(intervals);
    }

    /**
     * Specify the percentage how many enrichment intervals are actually added to the note pool.
     * This updates the note pool accordingly
     *
     * @param amount in [0.0f, 1.0f]
     */
    public void setTonalEnrichmentAmount(float amount) {
        this.notePool.setTonalEnrichmentAmount(amount);
    }

    /**
     * specify the pitch range of notes output by this note pool
     *
     * @param lowest
     * @param highest
     */
    public void setPitchRange(int lowest, int highest) {
        this.notePool.setPitchRange(lowest, highest);
    }

    /**
     * the classic MIDI Panic functionality
     */
    public void panic() {
        this.scheduledCommand = null;
        this.notePool.clear();

        // send an allNotesOff to all channels
        for (int chan = 0; chan < 16; ++chan) {
            try {
                this.outReceiver.send(new ShortMessage(EventMaker.CONTROL_CHANGE, chan, EventMaker.CC_All_Notes_Off, 0), -1);
            } catch (Exception e) {
                Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
                logger.log(Level.SEVERE, "MIDI Exception.", e);
                LogGUIController.logBuffer.append(e.getMessage());

            }
        }
    }

    /**
     * schedule a message to be sent at a specific time
     *
     * @param message
     * @param at
     */
    private synchronized void sendMessage(MidiMessage message, TimeStamp at) {
        ScheduledCommand sendCommand = () -> this.outReceiver.send(message, -1);
        this.synth.scheduleCommand(at, sendCommand);
    }

    /**
     * process incoming message
     *
     * @param message   the MIDI message to send
     * @param timeStamp the time-stamp for the message, in microseconds.
     */
    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (!(message instanceof ShortMessage sMsg))
            return;

        if (((ShortMessage) message).getChannel() != this.inputChannel)
            return;

        switch (message.getStatus() & 0xF0) {
            case EventMaker.NOTE_OFF: {                             // remove the note from the pool
                NoteItem bassNote = this.notePool.getBassNote();
                NoteItem removedNote = this.notePool.remove(sMsg);
                if (removedNote != null) {
                    if (this.heldNotesChannel >= 0)                 // stop the note on the held notes channel
                        this.outReceiver.send(removedNote.toShortMessage(this.heldNotesChannel, false), -1);

                    // handle bass change
                    if (this.bassChannel >= 0) {
                        NoteItem newBassNote = this.notePool.getBassNote();
                        if (!bassNote.equals(newBassNote)) {
                            this.outReceiver.send(bassNote.toShortMessage(this.bassChannel, false), -1);    // stop the old bass note
                            if (newBassNote != null) {
                                this.outReceiver.send(newBassNote.toShortMessage(this.bassChannel, true), -1);  // start the new bass note
                            }
                        }
                    }
                }
                break;
            }
            case EventMaker.NOTE_ON: {                              // add the note to the note pool and start arpeggiating through the pool (if not already running)
                if (sMsg.getData2() == 0) {                         // noteOn with velocity 0 is effectively a noteOff
                    try {
                        this.send(new ShortMessage(EventMaker.NOTE_OFF, sMsg.getData1(), sMsg.getData2()), -1); // make real noteOff of it and process it accordingly
                    } catch (Exception e) {
                        Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
                        logger.log(Level.SEVERE, "MIDI Exception.", e);
                        LogGUIController.logBuffer.append(e.getMessage());
                    }
                    return;
                }
                NoteItem bassNote = this.notePool.getBassNote();
                NoteItem note = new NoteItem(sMsg);
                if (!this.notePool.add(note))                       // if not successful
                    break;                                          // cancel operation

                if (this.scheduledCommand == null)                  // if arpeggiator is not yet running
                    this.run(this.synth.createTimeStamp(), note);   // start arpeggiator

                if (heldNotesChannel >= 0)                          // if held notes are activated
                    this.outReceiver.send(note.toShortMessage(this.heldNotesChannel, true), -1);    // send the noteOn to the heldNotesChannel

                // handle bass change
                if (this.bassChannel >= 0) {
                    NoteItem newBassNote = this.notePool.getBassNote();
                    if ((newBassNote != null) && !newBassNote.equals(bassNote)) {
                        if (bassNote != null)
                            this.outReceiver.send(bassNote.toShortMessage(this.bassChannel, false), -1);    // stop the old bass note
                        this.outReceiver.send(newBassNote.toShortMessage(this.bassChannel, true), -1);  // start the new bass note
                    }
                }
                break;
            }
            case EventMaker.CONTROL_CHANGE:
                switch (sMsg.getData1()) {
                    case EventMaker.CC_General_Purpose_Ctrl_1:      // tonal enrichment slider
//                        float tonalEnrichmentAmount = ((float) sMsg.getData2()) / 127f;
//                        this.setTonalEnrichmentAmount(tonalEnrichmentAmount);
                        this.sendToReceiver(message, timeStamp);
                        break;
                    case EventMaker.CC_General_Purpose_Ctrl_2:      // tempo slider
//                        double tempo = ((900.0 * sMsg.getData2()) / 127.0) + 100.0;
//                        this.setTempo(tempo);
                        this.sendToReceiver(message, timeStamp);
                        break;
                    case EventMaker.CC_General_Purpose_Ctrl_3:      // articulation slider
//                        double articulation = ((double) sMsg.getData2() / 127.0) - 0.5;
//                        this.setArticulation(articulation);
                        this.sendToReceiver(message, timeStamp);
                        break;
                    case EventMaker.CC_Undefined_Ctrl_8:            // switch tonal enrichment set/tonality
                        this.sendToReceiver(message, timeStamp);
                        break;
                    case EventMaker.CC_Undefined_Ctrl_7:            // arpeggio pattern
                        this.sendToReceiver(message, timeStamp);
                        break;
                    case EventMaker.CC_Effect_Ctrl_2_14b:           // trigger arpeggio channel
                        this.sendToReceiver(message, timeStamp);
                        break;
                    case EventMaker.CC_Undefined_Ctrl_3_14b:        // trigger held notes channel
                        this.sendToReceiver(message, timeStamp);
                        break;
                    case EventMaker.CC_Undefined_Ctrl_4_14b:        // trigger bass channel
                        this.sendToReceiver(message, timeStamp);
                        break;

                    case EventMaker.CC_Undefined_Ctrl_5_14b:        // Trigger Audio In
                        this.sendToReceiver(message, timeStamp);
                        break;
                    case EventMaker.CC_Undefined_Ctrl_6_14b:        // Trigger autotune
                        this.sendToReceiver(message, timeStamp);
                        break;
                    case EventMaker.CC_Undefined_Ctrl_7_14b:        // Set Scaling Factor
                        this.sendToReceiver(message, timeStamp);
                        break;
                    case EventMaker.CC_Undefined_Ctrl_8_14b:        // Set Sharpness
                        this.sendToReceiver(message, timeStamp);
                        break;
                    default:
                        break;
                }
                break;
            default:
                // not of our business
        }
    }

    /**
     * a getter for the receiver
     *
     * @return
     */
    @Override
    public Receiver getReceiver() {
        return this.monitorReceiver;
    }

    /**
     * set the receiver of outgoing MIDI messages
     *
     * @param receiver the desired receiver.
     */
    @Override
    public void setReceiver(Receiver receiver) {
        this.monitorReceiver = receiver;
    }

    /**
     * send a specified message and timestamp to the monitorReceiver
     *
     * @param message
     * @param timeStamp
     * @return success
     */
    private boolean sendToReceiver(MidiMessage message, long timeStamp) {
        if (this.monitorReceiver == null)
            return false;

        try {
            this.monitorReceiver.send(message, timeStamp);
        } catch (Exception e) {     // if the receiver is closed
            Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
            logger.log(Level.SEVERE, "MIDI Exception.", e);
            LogGUIController.logBuffer.append(e.getMessage());
            this.monitorReceiver = null;
            return false;
        }

        return true;
    }

    /**
     * close this receiver, transmitter and the devices it is connected to
     */
    @Override
    public void close() {
        if (this.inDevice != null)
            this.inDevice.close();

        if (this.outDevice != null)
            this.outDevice.close();
    }

    /**
     * this method does the arpeggiation
     *
     * @param timeStamp the date when this method should be called
     * @param note      the note to be played on the next call
     */
    private synchronized void run(TimeStamp timeStamp, NoteItem note) {
        // as long as a note is held, intensity grows, otherwise it decreases until it is vanished and the arpeggiator stops
        if (this.notePool.isEmpty()) {                          // no more notes to play
            if (this.notePool.isTriggered()) {                  // we are not finished as long as there are triggered notes which later might come back into pitch range; so we do a busy waiting here
                TimeStamp nextCallTime = timeStamp.makeRelative(this.beatLengthInSeconds);
                this.scheduledCommand = () -> this.run(nextCallTime, null);
                this.synth.scheduleCommand(nextCallTime, this.scheduledCommand);
            } else {                                            // done, stop the arpeggiator
                this.scheduledCommand = null;
                // stop all sounding notes
                for (int chan = 0; chan < 16; ++chan) {
                    try {
                        this.sendMessage(new ShortMessage(EventMaker.CONTROL_CHANGE, chan, EventMaker.CC_All_Notes_Off, 0), timeStamp);
                    } catch (Exception e) {
                        Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
                        logger.log(Level.SEVERE, "MIDI Exception.", e);
                        LogGUIController.logBuffer.append(e.getMessage());
                    }
                }
            }
            return;
        }

        // compute the time of the next call
        TimeStamp nextCallTime = timeStamp.makeRelative(this.beatLengthInSeconds);
        TimeStamp noteOffTime = nextCallTime.makeRelative(this.beatLengthInSeconds * this.articulation);

        // compute the index of the note to be played
        synchronized (this.notePool) {
            int index = this.notePool.indexOf(note);
            if (index < 0) {
                index = Math.min(this.notePool.size() - 1, Math.max(0, -(++index)));
            }
            note = this.notePool.get(index);
        }

        // play the note
        if (this.arpeggioChannel >= 0) {
            this.sendMessage(note.toShortMessage(this.arpeggioChannel, true), timeStamp);
            this.sendMessage(note.toShortMessage(this.arpeggioChannel, false), noteOffTime);
        }

        NoteItem nextNote = this.notePool.getNext();

        // schedule the next call of generate()
        this.scheduledCommand = () -> this.run(nextCallTime, nextNote);
        this.synth.scheduleCommand(nextCallTime, this.scheduledCommand);
    }
}
