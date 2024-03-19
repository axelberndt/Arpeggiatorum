package arpeggiatorum;

import com.jsyn.devices.AudioDeviceFactory;
import com.jsyn.devices.AudioDeviceManager;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArpeggiatorumController {

    public ChoiceBox choiceAudioChannel;

    @FXML
    public void buttonPanicClick(Event e) {
        Arpeggiatorum.getInstance().getArpeggiator().panic();
    }

    @FXML
    public void buttonLogClick(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("LogGUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = new Stage();
            stage.setTitle("Log Messages");
            stage.setScene(scene);
            stage.show();
            stage.setMaximized(true);
        } catch (IOException e) {
            Logger logger = Logger.getLogger(getClass().getName());
            logger.log(Level.SEVERE, "Failed to create new Window.", e);
        }
    }
    public static ChoiceBox<Integer> createAudioChannelChooser() {
        ChoiceBox<Integer> audioChannelChooser = new ChoiceBox<>();

        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager(false);
        int numDevices = audioManager.getMaxInputChannels(audioManager.getDefaultInputDeviceID());
        for (int i = 1; i <= numDevices; ++i) {
            audioChannelChooser.getItems().add(i);
        }
        return audioChannelChooser;
    }
}
