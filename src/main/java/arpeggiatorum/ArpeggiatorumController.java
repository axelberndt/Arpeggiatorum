package arpeggiatorum;

import arpeggiatorum.gui.GUI;
import arpeggiatorum.gui.MidiDeviceChooserItem;
import arpeggiatorum.gui.TonalEnrichmentChooserItem;
import arpeggiatorum.microphoneControl.Mic2MIDI;
import arpeggiatorum.notePool.NotePool;
import com.jsyn.Synthesizer;
import com.jsyn.devices.AudioDeviceFactory;
import com.jsyn.devices.AudioDeviceManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.RangeSlider;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArpeggiatorumController {

    //GUI Controls
    //Menu Bar
    @FXML
    public MenuItem menuLog;

    //First Column
    @FXML
    public Text textThreshold;
    @FXML
    public Text textRange;
    @FXML
    public Text textTempo;
    @FXML
    public Text textEnrichment;

    //Second Column
    @FXML
    public ComboBox<MidiDeviceChooserItem> comboMIDIIn;
    @FXML
    public ComboBox<MidiDeviceChooserItem> comboMIDIOut;
    @FXML
    public ComboBox<String> comboAudioOut;
    @FXML
    public ComboBox<String> comboAudioIn;



    @FXML
    private ComboBox<Integer> comboAudioChannel;
    @FXML
    public Slider sliderThreshold;
    @FXML
    public Slider sliderTempo;
    @FXML
    public Slider sliderArticulation;
    @FXML
    public RangeSlider sliderRange;
    @FXML
    public ComboBox<TonalEnrichmentChooserItem> comboEnrichment;
    @FXML
    public Slider sliderEnrichment;
    @FXML
    public Button buttonPanic;

    // Third Column
    @FXML
    public ComboBox<Integer> comboMIDIChannel;
    @FXML
    public ComboBox<Integer> comboArpeggioChannel;
    @FXML
    public ComboBox<Integer> comboHeldChannel;
    @FXML
    public ComboBox<Integer> comboBassChannel;
    @FXML
    public ComboBox<Mic2MIDI> comboMic2MIDI;
    @FXML
    public ComboBox<NotePool.Pattern> comboPattern;


    //Fourth Column
    @FXML
    public Button buttonActivate;
    @FXML
    public Button buttonAutoTune;
    @FXML
    public Button buttonTapTempo;
    @FXML
    public Button buttonUpload;

    @FXML
    public void initialize() {
        //Initialize GUI Elements
        createMidiInChooser();
        createMidiOutChooser();
        createAudioInChooser();
        createAudioOutChooser();
        createAudioChannelChooser();

        comboMIDIChannel.getItems().addAll(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        comboArpeggioChannel.getItems().addAll(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        comboHeldChannel.getItems().addAll(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        comboBassChannel.getItems().addAll(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);

        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Empty",
                new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Octaves",
                new int[]{0, 12, 24, 36, 48, 60, 72, 84, 96, 108, 120, 0, 0, 0, 0, 0}));
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Octaves and Fifths",
                new int[]{0, 7, 12, 19, 24, 31, 36, 43, 48, 55, 60, 67, 72, 79, 84, 91}));
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Major Triad",
                new int[]{0, 4, 7, 12, 16, 19, 24, 28, 31, 36, 40, 43, 48, 52, 55, 60}));
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Minor Triad",
                new int[]{0, 3, 7, 12, 15, 19, 24, 27, 31, 36, 39, 43, 48, 51, 55, 60}));
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Major Thirds",
                new int[]{0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52, 56, 60}));
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Fourths",
                new int[]{0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75}));
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Series of Overtones",
                new int[]{0, 12, 19, 24, 28, 31, 34, 36, 38, 40, 42, 43, 44, 46, 47, 48}));
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Overtones Transposed Down",
                new int[]{0, 12, 24, 7, 19, 4, 16, 10, 22, 2, 14, 6, 18, 8, 20, 11}));

        for (Mic2MIDI processor : Arpeggiatorum.getInstance().getMic2Midi()) {
            comboMic2MIDI.getItems().add(processor);
        }

        comboPattern.getItems().add(NotePool.Pattern.up);
        comboPattern.getItems().add(NotePool.Pattern.down);
        comboPattern.getItems().add(NotePool.Pattern.up_down);
        comboPattern.getItems().add(NotePool.Pattern.random_no_repetitions);
        comboPattern.getItems().add(NotePool.Pattern.random_with_repetitions);

        //Initialize internal components
        if (comboMIDIIn.getValue() != null) {
            MidiDeviceChooserItem item = comboMIDIIn.getValue();
            if (item.getValue() != null) {
                try {
                    Arpeggiatorum.getInstance().getArpeggiator().setMidiIn(item.getValue());
                } catch (MidiUnavailableException e) {
                    updateLogGUI(e.getMessage());
                }
            }
        }


        //Event Handlers
        //For whatever reason Sliders don’t have ActionEvents...
        //Instead, they have a Number called valueProperty that contains the current value of the slider.
        sliderThreshold.valueProperty().addListener((observable, oldValue, newValue) -> {

            textThreshold.setText(String.format("Current Threshold: %d", ((int) sliderThreshold.getValue())));


        });
        sliderTempo.valueProperty().addListener((observable, oldValue, newValue) -> {

            textTempo.setText(String.format("Current Tempo: %d", ((int) sliderTempo.getValue())));


        });

        sliderRange.highValueProperty().addListener((observable, oldValue, newValue) -> {

            textRange.setText(String.format("[%d-%d]", ((int) sliderRange.getLowValue()), ((int) sliderRange.getHighValue())));

        });
        sliderRange.lowValueProperty().addListener((observable, oldValue, newValue) -> {

            textRange.setText(String.format("[%d-%d]", ((int) sliderRange.getLowValue()), ((int) sliderRange.getHighValue())));

        });
        sliderEnrichment.valueProperty().addListener((observable, oldValue, newValue) -> {

            textEnrichment.setText(String.format("%d %%", ((int) sliderEnrichment.getValue())));


        });
    }

    @FXML
    public void buttonPanicClick(ActionEvent actionEvent) {
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

    /**
     * create a MIDI port chooser, ready to be added to the GUI
     */
    @FXML
    public void createMidiInChooser() {
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) { // iterate the info of each device
            // Get the corresponding device
            MidiDevice device;
            try {
                device = MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException e) {
                continue;
            }

            // The device should be a MIDI port with receiver or a synthesizer (Gervill)
            if (!(device instanceof Synthesizer) && (device.getMaxTransmitters() != 0)) {
                try {
                    comboMIDIIn.getItems().add(new MidiDeviceChooserItem(info));
                } catch (MidiUnavailableException e) {
                    GUI.updateLogGUI(e.getMessage());
                }
            }
        }
    }

    /**
     * create a MIDI port chooser, ready to be added to the GUI
     */
    @FXML
    public void createMidiOutChooser() {
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) { // iterate the info of each device
            // Get the corresponding device
            MidiDevice device;
            try {
                device = MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException e) {
                continue;
            }

            // The device should be a MIDI port with receiver or a synthesizer (Gervill)
            if (!(device instanceof Sequencer) && (device.getMaxReceivers() != 0)) {
                try {
                    comboMIDIOut.getItems().add(new MidiDeviceChooserItem(info));
                } catch (MidiUnavailableException e) {
                    GUI.updateLogGUI(e.getMessage());
                }
            }
        }
    }

    /**
     * create a combobox with audio input devices
     */
    @FXML
    public void createAudioInChooser() {
        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager(false);
        int numDevices = audioManager.getDeviceCount();

        for (int i = 0; i < numDevices; ++i) {
            if (audioManager.getMaxInputChannels(i) <= 0)
                continue;
            comboAudioIn.getItems().add(audioManager.getDeviceName(i));
        }
    }

    /**
     * create a combobox with audio output devices
     */
    @FXML
    public void createAudioOutChooser() {
        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager(false);
        int numDevices = audioManager.getDeviceCount();

        for (int i = 0; i < numDevices; ++i) {
            if (audioManager.getMaxOutputChannels(i) <= 0)
                continue;
            comboAudioOut.getItems().add(audioManager.getDeviceName(i));
        }
    }

    @FXML
    public void createAudioChannelChooser() {
        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager(false);
        int numDevices = audioManager.getMaxInputChannels(audioManager.getDefaultInputDeviceID());
        for (int i = 1; i <= numDevices; ++i) {
            comboAudioChannel.getItems().add(i);
        }

    }

    @FXML
    public void updateAudioChannelChooser(int devID) {
        comboAudioChannel.getItems().removeAll();
        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager(false);
        int numDevices = audioManager.getMaxInputChannels(devID);
        for (int i = 0; i < numDevices; ++i) {
            comboAudioChannel.getItems().add(i + 1);
        }
    }

    @FXML
    public void buttonUploadClick(ActionEvent actionEvent) {
        //Do something
    }

    @FXML
    public void updateLogGUI(final String message) {
        //Execute on correct thread
        // Then edit your GUI objects
    }

    @FXML
    public void comboMIDIInChanged(ActionEvent actionEvent) {
        if (comboMIDIIn.getValue() == null)
            return;

        MidiDeviceChooserItem item = comboMIDIIn.getValue();
        if (item.getValue() == null)
            return;

        try {
            Arpeggiatorum.getInstance().getArpeggiator().setMidiIn(item.getValue());
        } catch (MidiUnavailableException e) {
            updateLogGUI(e.getMessage());
        }
    }

    public void buttonActivateClick(ActionEvent actionEvent) {
    }

    public void buttonAutoTuneClick(ActionEvent actionEvent) {
    }

    public void buttonTapTempoClick(ActionEvent actionEvent) {
    }
}
