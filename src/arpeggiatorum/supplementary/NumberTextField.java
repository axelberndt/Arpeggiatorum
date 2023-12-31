package arpeggiatorum.supplementary;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;

/**
 * A text field for numeric input.
 * @author Axel Berndt
 */
public class NumberTextField extends JFormattedTextField {
    /**
     * default constructor
     */
    public NumberTextField(){
        this(NumberFormat.getInstance());
    }

    /**
     * constructor
     * @param format
     */
    public NumberTextField(NumberFormat format) {
        super(new NumberFormatter(format));

        NumberFormatter formatter = (NumberFormatter) this.getFormatter();
        format.setGroupingUsed(false);
        formatter.setAllowsInvalid(true);
        this.setHorizontalAlignment(JFormattedTextField.RIGHT);
        this.setValue(0L);
    }

    /**
     * constructor
     * @param format
     * @param initialValue
     */
    public NumberTextField(NumberFormat format, Object initialValue) {
        this(format);
        this.setValue(initialValue);
    }
}