package arpeggiatorum.gui;

import arpeggiatorum.supplementary.ObservableStringBuffer;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.ResourceBundle;

public class LogGUIController implements Initializable {
    public final static ObservableStringBuffer logBuffer = new ObservableStringBuffer();
    @FXML
    TextArea logTextArea;

    /**
     * @param url
     * @param resourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logTextArea.textProperty().bind(logBuffer);

        logBuffer.addListener((ChangeListener<Object>) (observable, oldValue, newValue) -> {
            // from stackoverflow.com/a/30264399/1032167
            // for some reason setScrollTop will not scroll properly
            //consoleTextArea.setScrollTop(Double.MAX_VALUE);
            logTextArea.selectPositionCaret(logTextArea.getLength());
            logTextArea.deselect();
        });
    }
}
