package arpeggiatorum.notePool;

import arpeggiatorum.gui.ArpeggiatorumGUI;
import arpeggiatorum.supplementary.EventMaker;
import arpeggiatorum.supplementary.SortedList;

import javax.sound.midi.ShortMessage;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The note pool is more than a list of notes. It also implements logic to
 * automatically add and remove notes from the pool over time and prove
 * notes according to a specified pattern.
 *
 * @author Axel Berndt
 */
public class NotePool {
    protected final SortedList<NoteItem> triggeredNotes = new SortedList<>(128);  // stores the notes that are currently active
    protected final SortedList<NoteItem> notePool = new SortedList<>(128);        // the pool of notes to be played; some might be added that are not directly triggered by the user
    private final Random random = new Random();
    private Pattern pattern = Pattern.random_no_repetitions;
    private NoteItem lastNoteProvided = null;
    private int[] tonalEnrichment = new int[]{0};     // no tonal enrichment, just the fundamental pitch
    private float tonalEnrichmentAmount = 0.0f;
    private int upperTonalEnrichmentIndex = 0;
    private int lowestPitch = 0;
    private int highestPitch = 127;

    /**
     * default constructor
     */
    public NotePool() {
        super();
    }

    /**
     * add a note to the pool;
     * if the note is already in the pool, its noteOn counter will be increased
     *
     * @param noteOn
     * @return
     */
    public boolean add(ShortMessage noteOn) {
        if ((noteOn.getStatus() & 0xF0) != EventMaker.NOTE_ON)
            return false;

        NoteItem note = new NoteItem(noteOn.getData1(), noteOn.getData2());
        return this.add(note);
    }

    /**
     * add a note to the pool;
     * if the note is already in the pool, its noteOn counter will be increased
     *
     * @param note
     * @return
     */
    public boolean add(NoteItem note) {
        if (this.notePool.isEmpty())            // if the note pool is empty, i.e., the arpeggio starts anew
            this.lastNoteProvided = null;       // make sure the pattern starts at its beginning

        // add fundamental note
        if (!this.triggeredNotes.add(note)) {   // if it is already present in the list
            int index = this.triggeredNotes.indexOf(note);
            this.triggeredNotes.get(index).increaseNoteOnCounter();
            return false;
        }

        // triggered notes will not go automatically in the note pool, except they are part of their enrichment
//        if ((note.getPitch() >= this.lowestPitch) && (note.getPitch() <= this.highestPitch))    // ensure pitch range restrictions
//            this.notePool.add(note);

        // add enrichment notes
        for (int i = 0; i <= this.upperTonalEnrichmentIndex; ++i) {
            int interval = this.tonalEnrichment[i];
            int pitch = note.getPitch() + interval;

            if ((pitch >= this.lowestPitch) && (pitch <= this.highestPitch)) {      // ensure pitch range restrictions
                NoteItem enrichmentNote = new NoteItem(pitch, note.getVelocity());
                this.notePool.add(enrichmentNote);
            }
        }

        if (this.lastNoteProvided == null)
            this.lastNoteProvided = note;

        return true;
    }

    /**
     * get a copy of the list of triggered notes, sorted by increasing pitch
     *
     * @return
     */
    public ArrayList<NoteItem> getTriggeredNotes() {
        return (ArrayList<NoteItem>) this.triggeredNotes.toArrayList().clone();
    }

    /**
     * get the lowest note in the list of triggered notes, lowered by one octave
     *
     * @return the note or null, iff no notes are triggered
     */
    public NoteItem getBassNote() {
        if (this.triggeredNotes.isEmpty())
            return null;

        NoteItem note = this.triggeredNotes.get(0);
        int pitch = (note.getPitch() > 10) ? note.getPitch() - 12 : note.getPitch();
//        if ((pitch < this.lowestPitch) || (pitch > this.highestPitch))
//            return null;
        return new NoteItem(pitch, note.getVelocity());
    }

    /**
     * set the series of additional intervals over the fundamental pitch
     * to be added to the notePool list
     *
     * @param intervalSeries
     */
    public void setTonalEnrichment(int[] intervalSeries) {
        if (intervalSeries.length > 0)
            this.tonalEnrichment = intervalSeries;
        else
            this.tonalEnrichment = new int[]{0};    // there has to be at least one element, the fundamental pitch

        this.setTonalEnrichmentAmount(this.tonalEnrichmentAmount);
    }

    /**
     * Specify the percentage how many enrichment intervals are actually added to the note pool.
     * This updates the note pool accordingly
     *
     * @param amount in [0.0f, 1.0f]
     */
    public void setTonalEnrichmentAmount(float amount) {
        this.tonalEnrichmentAmount = Math.min(Math.max(amount, 0.0f), 1.0f);
        this.upperTonalEnrichmentIndex = (int) ((this.tonalEnrichment.length - 1) * this.tonalEnrichmentAmount);

        ArrayList<NoteItem> newNotePool = new ArrayList<>(128);

        for (NoteItem note : this.triggeredNotes) {
            // triggered notes will not go automatically in the note pool, except they are part of their enrichment
//            if ((note.getPitch() >= this.lowestPitch) && (note.getPitch() <= this.highestPitch))    // ensure pitch range restrictions
//                newNotePool.add(note);

            for (int i = 0; i <= this.upperTonalEnrichmentIndex; ++i) {
                int interval = this.tonalEnrichment[i];
                int pitch = note.getPitch() + interval;

                if ((pitch >= this.lowestPitch) && (pitch <= this.highestPitch)) {      // ensure pitch range restrictions
                    NoteItem enrichmentNote = new NoteItem(pitch, note.getVelocity());
                    newNotePool.add(enrichmentNote);
                }
            }
        }

        synchronized (this.notePool) {
            this.notePool.clear();
            this.notePool.addAll(newNotePool);
        }
    }

    /**
     * specify the pitch range of notes output by this note pool
     *
     * @param lowest
     * @param highest
     */
    public void setPitchRange(int lowest, int highest) {
        this.highestPitch = Math.min(Math.max(highest, 0), 127);
        this.lowestPitch = Math.min(Math.max(lowest, 0), 127);
        this.setTonalEnrichmentAmount(this.tonalEnrichmentAmount);
    }

    /**
     * remove the note from the pool that corresponds with the given ShortMessage;
     * if it was triggered more than once, only the note's noteOn counter will be decreased
     *
     * @param shortMessage can be a noteOn or noteOff
     * @return
     */
    public NoteItem remove(ShortMessage shortMessage) {
        NoteItem note = new NoteItem(shortMessage);
        return this.remove(note);
    }

    /**
     * remove a note from the pool; if it was triggered more than once,
     * only the note's noteOn counter will be decreased
     *
     * @param note
     * @return
     */
    public NoteItem remove(NoteItem note) {
        // remove the fundamental note from the list of triggered notes
        int index = this.triggeredNotes.indexOf(note);
        if (index < 0)      // if the note does not exist in the pool
            return null;    // done

        NoteItem toBeRemoved = this.triggeredNotes.get(index);              // get the note from the pool

        // we first decrease the note's noteOn counter
        if (toBeRemoved.decreaseNoteOnCounter() > 0)                        // if there are more noteOns left
            return null;                                                    // we keep the note in the pool

        // otherwise the noteOn counter reached 0 all triggered noteOns have ended
        toBeRemoved = this.triggeredNotes.remove(toBeRemoved);              // remove the note completely from the pool
        this.setTonalEnrichmentAmount(this.tonalEnrichmentAmount);          // recompute the pool's note material incl. the enrichment notes

        return toBeRemoved;
    }

    /**
     * Removes all the notes from the pool.
     */
    public void clear() {
        this.notePool.clear();
        this.triggeredNotes.clear();
    }

    /**
     * directly access an item
     *
     * @param index
     * @return
     */
    public NoteItem get(int index) {
        return this.notePool.get(index);
    }

    /**
     * provide a note sequence according to the specified pattern
     *
     * @return
     */
    public NoteItem getNext() {
        return this.getNext(this.lastNoteProvided);
    }

    /**
     * provide a note sequence according to the specified pattern
     *
     * @param previous
     * @return
     */
    public NoteItem getNext(NoteItem previous) {
        // if no previous note given, return the lowest triggered note
        if (previous == null) {
            this.lastNoteProvided = this.triggeredNotes.get(0);
            return this.lastNoteProvided;
        }

        // compute the index of the previous note
        int index = this.indexOf(previous);
        if (index < 0)
            index = Math.min(this.size() - 1, Math.max(0, -(++index)));

        // get next note from note pool
        switch (this.pattern) {
            case up:
                index = ++index % this.notePool.size();
                break;
            case down:
                index = Math.floorMod(--index, this.notePool.size());
                break;
            case up_down:
                if (++index >= this.notePool.size()) {
                    this.pattern = Pattern.down_up;
                    index = Math.max(0, this.notePool.size() - 2);
                }
                break;
            case down_up:
                if (--index < 0) {
                    this.pattern = Pattern.up_down;
                    index = (this.notePool.size() == 1) ? 0 : 1;
                }
                break;
            case random_no_repetitions:
                if (this.notePool.size() > 1)    // only possible if we have more than one note in the pool
                    index = (++index + this.random.nextInt(this.notePool.size() - 1)) % this.notePool.size();
                break;
            case random_with_repetitions:
            default:
                index = this.random.nextInt(this.notePool.size());
                break;
        }

        try {   // the note pool could have become empty in the meantime, so we have to try
            this.lastNoteProvided = this.notePool.get(index);
        } catch (IndexOutOfBoundsException e) {
            Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
            logger.log(Level.SEVERE, "Index out of bounds.", e);
            return null;
        }

        return this.lastNoteProvided;
    }

    /**
     * get the current pattern
     *
     * @return
     */
    public NotePool.Pattern getPattern() {
        return this.pattern;
    }

    /**
     * switch the note provider pattern
     *
     * @param pattern
     */
    public void setPattern(NotePool.Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * compute the index of the given note
     *
     * @param note
     * @return the index of the search key, if it is contained in the list; otherwise, (-(insertion point) - 1). The insertion point is defined as the point at which the key would be inserted into the list: the index of the first element greater than the key, or list.size() if all elements in the list are less than the specified key. Note that this guarantees that the return value will be >= 0 if and only if the key is found.
     */
    public int indexOf(NoteItem note) {
        if (this.notePool.isEmpty() || (note == null))
            return 0;

        return this.notePool.indexOf(note);
//        int index = this.notePool.indexOf(note);
//        if (index < 0) {                                        // if the note is no longer in the list
//            index = -index - 1;
////            index = (-(index + 1)) % this.notePool.size();      // choose the one that would follow next
//        }
//        return index;
    }

    /**
     * Is the pool empty?
     *
     * @return
     */
    public boolean isEmpty() {
        return this.notePool.isEmpty();
    }

    /**
     * Is the note pool holding triggered notes.
     * This can be true even if the notes to be played are empty because of pitch range restrictions.
     *
     * @return
     */
    public boolean isTriggered() {
        return !this.triggeredNotes.isEmpty();
    }

    /**
     * How many notes in the pool are left? The triggered AND generated notes!
     *
     * @return
     */
    public int size() {
        return this.notePool.size();
    }

    /**
     * indicates the pattern according to which notes are provided
     */
    public enum Pattern {
        up("Up"),
        down("Down"),
        up_down("Up-Down"),
        down_up("Down-Up"),
        random_no_repetitions("Random \r\n no reps"),
        random_with_repetitions("Random \r\n with reps");

        private final String name;

        Pattern(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {
            // (otherName == null) check is not needed because name.equals(null) returns false
            return name.equals(otherName);
        }

        public String toString() {
            return this.name;
        }

        public static Pattern fromString(String text) {
            for (Pattern b : Pattern.values()) {
                if (b.name.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }
}
