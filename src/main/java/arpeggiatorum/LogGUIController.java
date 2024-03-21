package arpeggiatorum;

import arpeggiatorum.supplementary.ObservableStringBuffer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class LogGUIController {
    @FXML
    TextArea logTextArea;
    public final static ObservableStringBuffer logBuffer = new ObservableStringBuffer();

    @FXML
    public void initialize() {
        logTextArea.textProperty().bind(logBuffer);
        
        logBuffer.addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue,
                                Object newValue) {
                // from stackoverflow.com/a/30264399/1032167
                // for some reason setScrollTop will not scroll properly
                //consoleTextArea.setScrollTop(Double.MAX_VALUE);
                logTextArea.selectPositionCaret(logTextArea.getLength());
                logTextArea.deselect();
            }
        });
    }
}
