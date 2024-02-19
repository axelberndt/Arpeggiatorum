package arpeggiatorum.notePool;

import arpeggiatorum.gui.GUI;
import meico.midi.EventMaker;
import meico.supplementary.KeyValue;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

/**
 * This class represents a note item in a SortedList.
 *
 * @author Axel Berndt
 */
public class NoteItem extends KeyValue<Integer, Integer> implements Comparable<NoteItem> {
    private int noteOnCounter = 1;  // if the same note is triggered several times it can be represented by this counter

    /**
     * constructor
     *
     * @param pitch
     * @param velocity
     */
    public NoteItem(Integer pitch, Integer velocity) {
        super(Math.min(Math.max(pitch, 0), 127), Math.min(Math.max(velocity, 0), 127));
    }

    /**
     * constructor
     *
     * @param shortMessage
     */
    public NoteItem(ShortMessage shortMessage) {
        this(shortMessage.getData1(), shortMessage.getData2());
    }

    /**
     * get the pitch value
     *
     * @return
     */
    public int getPitch() {
        return this.getKey();
    }

    /**
     * get the velocity value
     *
     * @return
     */
    public int getVelocity() {
        return this.getValue();
    }

    /**
     * increase the noteOn counter
     *
     * @return counter value after increase
     */
    public int increaseNoteOnCounter() {
        return ++this.noteOnCounter;
    }

    /**
     * decrease the noteOn counter
     *
     * @return counter value after decrease
     */
    public int decreaseNoteOnCounter() {
        return --this.noteOnCounter;
    }

    /**
     * get the value of the noteOn counter
     *
     * @return
     */
    public int getNoteOnCounter() {
        return this.noteOnCounter;
    }

    /**
     * make the items comparable
     *
     * @param noteItem the object to be compared.
     * @return
     */
    @Override
    public int compareTo(NoteItem noteItem) {
        return Integer.compare(this.getKey(), noteItem.getKey());
    }

    /**
     * compare this object with the provided one for equality
     *
     * @param noteItem object to be compared for equality with this map entry
     * @return
     */
    @Override
    public boolean equals(Object noteItem) {
        if (!(noteItem instanceof NoteItem))
            return false;
        return this.getKey().equals(((NoteItem) noteItem).getKey());
    }

    /**
     * get clone of this object
     *
     * @return
     */
    @Override
    public NoteItem clone() {
        return new NoteItem(this.getPitch(), this.getVelocity());
    }

    /**
     * convert the note item to a MIDI ShortMessage
     *
     * @param channel   the MIDI channel in [0, 15]
     * @param noteOnOff true=noteOn, false=noteOff
     * @return the ShortMessage or null if failed
     */
    public ShortMessage toShortMessage(int channel, boolean noteOnOff) {
        try {
            if (noteOnOff)
                return new ShortMessage(EventMaker.NOTE_ON, channel, this.getPitch(), this.getVelocity());
            return new ShortMessage(EventMaker.NOTE_OFF, channel, this.getPitch(), 0);
        } catch (InvalidMidiDataException e) {
            //e.printStackTrace();
            GUI.logMessages.append(e.getMessage());
        }
        return null;
    }
}
