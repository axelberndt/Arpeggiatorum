package arpeggiatorum.supplementary;

import javafx.application.Platform;
import javafx.beans.binding.StringBinding;

public class ObservableStringBuffer extends StringBinding {
    private final StringBuffer buffer = new StringBuffer();

    @Override
    protected String computeValue() {
        return buffer.toString();
    }

    public void set(String content) {
        buffer.replace(0, buffer.length(), content);
        invalidate();
    }

    public void append(String text) {
        Platform.runLater(() -> {
            buffer.append(text);
            invalidate();
        });

    }
    // wrap other StringBuffer methods as needed...
}
