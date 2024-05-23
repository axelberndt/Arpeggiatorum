package arpeggiatorum.gui.regulators;


import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;


/**
 * Created by hansolo on 02.03.16.
 */
public class RegulatorEvent extends Event {
    public static final EventType<RegulatorEvent> TARGET_SET = new EventType(ANY, "targetSet");
    public static final EventType<RegulatorEvent> ADJUSTING  = new EventType(ANY, "adjusting");
    public static final EventType<RegulatorEvent> ADJUSTED   = new EventType(ANY, "adjusted");


    // ******************** Constructors **********************************
    public RegulatorEvent(final EventType<RegulatorEvent> TYPE) { super(TYPE); }
    public RegulatorEvent(final Object SRC, final EventTarget TARGET, final EventType<RegulatorEvent> TYPE) { super(SRC, TARGET, TYPE); }
}
