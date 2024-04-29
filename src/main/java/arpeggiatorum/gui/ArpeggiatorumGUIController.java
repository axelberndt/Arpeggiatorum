package arpeggiatorum.gui;

import arpeggiatorum.Arpeggiatorum;
import arpeggiatorum.microphoneControl.Mic2MIDI;
import arpeggiatorum.microphoneControl.Mic2MIDI_CQT;
import arpeggiatorum.notePool.NotePool;
import arpeggiatorum.supplementary.MidiDeviceChooserItem;
import arpeggiatorum.supplementary.TonalEnrichmentChooserItem;
import arpeggiatorum.supplementary.Tools;
import com.jsyn.Synthesizer;
import com.jsyn.devices.AudioDeviceFactory;
import com.jsyn.devices.AudioDeviceManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.controlsfx.control.RangeSlider;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArpeggiatorumGUIController implements Initializable {

    //GUI Controls

    public final CategoryAxis xAxis = new CategoryAxis();
    public final NumberAxis yAxis = new NumberAxis(0, 1, 0.1);
    public final XYChart.Series<String, Number> chartSeries = new XYChart.Series<>();
    public XYChart.Data<String, Number>[] chartData;

    public final XYChart.Series<String, Number> middleSeries = new XYChart.Series<>();
    public XYChart.Data<String, Number>[] middleData;
    public LineChart<String, Number> lineChart;
    @FXML
    public TabPane tabPaneControls;
    //First Column
    @FXML
    public Text textThreshold;
    @FXML
    public Text textRange;
    @FXML
    public Text textTempo;
//    @FXML
//    public Text textEnrichment;
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
    public ComboBox<Integer> comboAudioChannel;
    @FXML
    public Slider sliderThreshold;
    @FXML
    public Slider sliderSharpness;
    @FXML
    public Slider sliderScale;
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
    public ToggleButton toggleButtonActivate;
    @FXML
    public ToggleButton toggleButtonAutoTune;
    @FXML
    public Button buttonTapTempo;
    @FXML
    public Button buttonUpload;
    //Histogram
    @FXML
    public BarChart<String, Number> chartCQTHistogram;
    @FXML
    public BorderPane borderPane;
    @FXML
    public TextField e1;
    @FXML
    public TextField e2;
    @FXML
    public TextField e3;
    @FXML
    public TextField e4;
    @FXML
    public TextField e5;
    @FXML
    public TextField e6;
    @FXML
    public TextField e7;
    @FXML
    public TextField e8;
    @FXML
    public TextField e9;
    @FXML
    public TextField e10;
    @FXML
    public TextField e11;
    @FXML
    public TextField e12;
    @FXML
    public TextField e13;
    @FXML
    public TextField e14;
    @FXML
    public TextField e15;
    @FXML
    public TextField e16;


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
                Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
                logger.log(Level.SEVERE, "MIDI Exception.", e);
                LogGUIController.logBuffer.append(e.getMessage());
                continue;
            }

            // The device should be a MIDI port with receiver or a synthesizer (Gervill)
            if (!(device instanceof Synthesizer) && (device.getMaxTransmitters() != 0)) {
                try {
                    comboMIDIIn.getItems().add(new MidiDeviceChooserItem(info));
                } catch (MidiUnavailableException e) {
                    Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
                    logger.log(Level.SEVERE, "MIDI Exception.", e);
                    LogGUIController.logBuffer.append(e.getMessage());
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
                Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
                logger.log(Level.SEVERE, "MIDI Exception.", e);
                LogGUIController.logBuffer.append(e.getMessage());
                continue;
            }

            // The device should be a MIDI port with receiver or a synthesizer (Gervill)
            if (!(device instanceof Sequencer) && (device.getMaxReceivers() != 0)) {
                try {
                    comboMIDIOut.getItems().add(new MidiDeviceChooserItem(info));
                } catch (MidiUnavailableException e) {
                    Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
                    logger.log(Level.SEVERE, "MIDI Exception.", e);
                    LogGUIController.logBuffer.append(e.getMessage());
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
        comboAudioChannel.getItems().clear();
        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager(false);
        int numDevices = audioManager.getMaxInputChannels(devID);
        for (int i = 0; i < numDevices; ++i) {
            comboAudioChannel.getItems().add(i + 1);
        }
        comboAudioChannel.getSelectionModel().select(0);
    }


    //Toggles
    @FXML
    public void toggleButtonActivateClick(ActionEvent actionEvent) {
        Arpeggiatorum.getInstance().Activate(toggleButtonActivate.isSelected());
    }

    @FXML
    public void toggleButtonAutoTuneClick(ActionEvent actionEvent) {
        Arpeggiatorum.getInstance().AutoTune(toggleButtonAutoTune.isSelected());
    }

    //Buttons
    @FXML
    public void buttonTapTempoClick(ActionEvent actionEvent) {
        Arpeggiatorum.getInstance().tapTempo();
    }

    @FXML
    public void buttonUploadClick(ActionEvent actionEvent) {
        Arpeggiatorum.getInstance().Upload();
    }

    @FXML
    public void buttonPanicClick(ActionEvent actionEvent) {
        if (toggleButtonActivate.isSelected()) {
            toggleButtonActivate.fire();
        }
        Arpeggiatorum.getInstance().getArpeggiator().panic();
    }

    //Combo Boxes
    @FXML
    public void comboMIDIInChanged(ActionEvent actionEvent) {
        if (comboMIDIIn.getValue() == null)
            return;

        MidiDeviceChooserItem item = comboMIDIIn.getValue();
        if (item.getValue() == null)
            return;

        try {
            Arpeggiatorum.getInstance().getArpeggiator().setMIDIIn(item.getValue());
        } catch (MidiUnavailableException e) {
            Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
            logger.log(Level.SEVERE, "MIDI Exception.", e);
            LogGUIController.logBuffer.append(e.getMessage());
        }
    }

    @FXML
    public void comboMIDIOutChanged(ActionEvent actionEvent) {
        if (comboMIDIOut.getValue() == null)
            return;

        MidiDeviceChooserItem item = comboMIDIOut.getValue();
        if (item.getValue() == null)
            return;

        try {
            Arpeggiatorum.getInstance().getArpeggiator().setMIDIOut(item.getValue());
        } catch (MidiUnavailableException e) {
            Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
            logger.log(Level.SEVERE, "MIDI Exception.", e);
            LogGUIController.logBuffer.append(e.getMessage());
        }
    }

    @FXML
    public void comboMIDIChannelChanged(ActionEvent actionEvent) {
        if (comboMIDIChannel.getValue() != null)
            Arpeggiatorum.getInstance().getArpeggiator().setInputChannel(comboMIDIChannel.getValue());
    }

    @FXML
    public void comboArpeggioChannelChanged(ActionEvent actionEvent) {
        if (comboArpeggioChannel.getValue() != null) {
            Arpeggiatorum.getInstance().getArpeggiator().setArpeggioChannel(comboArpeggioChannel.getValue());
        }
    }

    @FXML
    public void comboHeldChannelChanged(ActionEvent actionEvent) {
        if (comboHeldChannel.getValue() != null)
            Arpeggiatorum.getInstance().getArpeggiator().setHeldNotesChannel(comboHeldChannel.getValue());
    }

    @FXML
    public void comboBassChannelChanged(ActionEvent actionEvent) {
        if (comboBassChannel.getValue() != null)
            Arpeggiatorum.getInstance().getArpeggiator().setBassChannel(comboBassChannel.getValue());
    }

    @FXML
    public void comboMic2MIDIChanged(ActionEvent actionEvent) {
//        if (toggleButtonActivate.isSelected()) {
//            for (Mic2MIDI processor : Arpeggiatorum.getInstance().getMic2Midi()) {
//                processor.stop();
//            }
        //comboMic2MIDI.getValue().start();
        if (toggleButtonActivate.isSelected()) {
            toggleButtonActivate.fire();
        }
    }
//    }

    @FXML
    public void comboAudioOutChanged(ActionEvent actionEvent) {
        //Nothing to do here for now
    }

    @FXML
    public void comboAudioInChanged(ActionEvent actionEvent) {
        // TODO: the following lines do not work!
        //Windows appear to be the problem, works on Mac
        if (Arpeggiatorum.synth.isRunning()) {
            Arpeggiatorum.synth.stop();
        }
        int deviceInputID = Tools.getDeviceID(comboAudioIn.getValue());
        int deviceInputChannels = Arpeggiatorum.synth.getAudioDeviceManager().getMaxInputChannels(deviceInputID);
        updateAudioChannelChooser(deviceInputID);

        try {
            Arpeggiatorum.synth.start(Arpeggiatorum.sampleRate,
                    deviceInputID,
                    deviceInputChannels,
                    Arpeggiatorum.synth.getAudioDeviceManager().getDefaultOutputDeviceID(), 0);
        }
        catch (Exception e) {
            Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
            logger.log(Level.SEVERE, "Audio Exception.", e);
            LogGUIController.logBuffer.append(e.getMessage());
        }

        if (toggleButtonActivate.isSelected()) {
            toggleButtonActivate.fire();
        }
    }

    @FXML
    public void comboAudioChannelChanged(ActionEvent actionEvent) {
        if (ArpeggiatorumGUI.controllerHandle.comboAudioChannel.getValue() != null) {
            for (Mic2MIDI processor : Arpeggiatorum.getInstance().getMic2Midi()) {
                processor.setChannel(ArpeggiatorumGUI.controllerHandle.comboAudioChannel.getValue() - 1);
            }

        }
    }

    @FXML
    public void comboPatternChanged(ActionEvent actionEvent) {
        Arpeggiatorum.getInstance().getArpeggiator().setPattern(comboPattern.getValue());
    }

    @FXML
    public void comboEnrichmentChanged(ActionEvent actionEvent) {
        if (comboEnrichment.getValue() == null) {
            return;
        }
        int[] intervals = comboEnrichment.getValue().getValue();
        e1.setText(String.valueOf((intervals.length >= 1) ? intervals[0] : 0));
        e2.setText(String.valueOf((intervals.length >= 2) ? intervals[1] : 0));
        e3.setText(String.valueOf((intervals.length >= 3) ? intervals[2] : 0));
        e4.setText(String.valueOf((intervals.length >= 4) ? intervals[3] : 0));
        e5.setText(String.valueOf((intervals.length >= 5) ? intervals[4] : 0));
        e6.setText(String.valueOf((intervals.length >= 6) ? intervals[5] : 0));
        e7.setText(String.valueOf((intervals.length >= 7) ? intervals[6] : 0));
        e8.setText(String.valueOf((intervals.length >= 8) ? intervals[7] : 0));
        e9.setText(String.valueOf((intervals.length >= 9) ? intervals[8] : 0));
        e10.setText(String.valueOf((intervals.length >= 10) ? intervals[9] : 0));
        e11.setText(String.valueOf((intervals.length >= 11) ? intervals[10] : 0));
        e12.setText(String.valueOf((intervals.length >= 12) ? intervals[11] : 0));
        e13.setText(String.valueOf((intervals.length >= 13) ? intervals[12] : 0));
        e14.setText(String.valueOf((intervals.length >= 14) ? intervals[13] : 0));
        e15.setText(String.valueOf((intervals.length >= 15) ? intervals[14] : 0));
        e16.setText(String.valueOf((intervals.length >= 16) ? intervals[15] : 0));
        Arpeggiatorum.getInstance().getArpeggiator().setTonalEnrichment(intervals);
    }

    public void updateHist(double[] newData) {
        Platform.runLater(() -> {
            chartSeries.getData().clear();
            for (int i = 0; i < chartData.length; i++) {
                chartData[i] = new XYChart.Data<>(chartData[i].getXValue(),
                        newData[i]);
                chartSeries.getData().add(chartData[i]);

            }
        });
    }

    public void updateHist(double[] newData, int lowIndex) {
        Platform.runLater(() -> {
            double colorThreshold = ArpeggiatorumGUI.controllerHandle.yAxis.getUpperBound();
            for (int i = 0; i < newData.length; i++) {
                XYChart.Data dataPoint = chartSeries.getData().get(i + lowIndex);
                dataPoint.setYValue(newData[i]);
                //Determine colour
                if (newData[i] < (colorThreshold / 2.0)) {
                    dataPoint.getNode().setStyle("-fx-bar-fill: Gainsboro;");
                }
                if (newData[i] >= (colorThreshold / 2.0)) {
                    dataPoint.getNode().setStyle("-fx-bar-fill: Chartreuse;");
                }
                if (newData[i] >= colorThreshold) {
                    dataPoint.getNode().setStyle("-fx-bar-fill: Red;");
                }
            }
        });
    }

    /**
     * @param url
     * @param resourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //Initialize GUI Elements
        createMidiInChooser();
        createMidiOutChooser();
        createAudioInChooser();
        createAudioChannelChooser();

        tabPaneControls.getStylesheets().addAll(ArpeggiatorumGUI.class.getResource("tabpane.css").toExternalForm());

        comboMIDIChannel.getItems().addAll(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        comboArpeggioChannel.getItems().addAll(-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        comboHeldChannel.getItems().addAll(-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        comboBassChannel.getItems().addAll(-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);

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
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Minor Thirds",
                new int[]{0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45}));
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Fourths",
                new int[]{0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75}));
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Series of Overtones",
                new int[]{0, 12, 19, 24, 28, 31, 34, 36, 38, 40, 42, 43, 44, 46, 47, 48}));
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("Overtones Transposed Down",
                new int[]{0, 12, 24, 7, 19, 4, 16, 10, 22, 2, 14, 6, 18, 8, 20, 11}));
        comboEnrichment.getItems().add(new TonalEnrichmentChooserItem("User Preset",
                new int[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

        for (Mic2MIDI processor : Arpeggiatorum.getInstance().getMic2Midi()) {
            comboMic2MIDI.getItems().add(processor);
        }

        comboPattern.getItems().add(NotePool.Pattern.up);
        comboPattern.getItems().add(NotePool.Pattern.down);
        comboPattern.getItems().add(NotePool.Pattern.up_down);
        comboPattern.getItems().add(NotePool.Pattern.random_no_repetitions);
        comboPattern.getItems().add(NotePool.Pattern.random_with_repetitions);

        //Histogram
        chartCQTHistogram = new BarChart<>(xAxis, yAxis);
        chartCQTHistogram.setLegendVisible(false);
        chartCQTHistogram.setAnimated(false);
        chartCQTHistogram.setBarGap(0);
        chartCQTHistogram.setCategoryGap(1);
        chartCQTHistogram.setVerticalGridLinesVisible(false);

        //Setup Chart:
        //chartCQTHistogram.setTitle("CQT Bins"); //Save space avoiding title
        //xAxis.setLabel("Frequency (Hz)");
        //yAxis.setLabel("Magnitude");
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, null, ""));

        // Add starting data
        chartSeries.setName("Data Series");
        double[] cqtFreqs = new double[0];
        for (Mic2MIDI processor : Arpeggiatorum.getInstance().getMic2Midi()) {
            if (processor instanceof Mic2MIDI_CQT) {
                cqtFreqs = ((Mic2MIDI_CQT) processor).CQTFrequencies;
            }
        }
        chartData = new XYChart.Data[cqtFreqs.length];
        middleData = new XYChart.Data[cqtFreqs.length];
        for (int i = 0; i < chartData.length; i++) {

            chartData[i] = new XYChart.Data<>(String.format("%.2f", cqtFreqs[i]),
                    1);
            chartSeries.getData().add(chartData[i]);

            middleData[i] = new XYChart.Data<>(String.format("%.2f", cqtFreqs[i]),
                    0.1);
            middleSeries.getData().add(middleData[i]);

        }
        chartCQTHistogram.getData().add(chartSeries);
        chartCQTHistogram.lookupAll(".default-color0.chart-bar").forEach(n -> n.setStyle("-fx-bar-fill: Gainsboro;"));
        //  chartCQTHistogram.setStyle("-fx-background-color: transparent");

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(true);
        lineChart.setAlternativeRowFillVisible(false);
        lineChart.setAlternativeColumnFillVisible(false);
        lineChart.setHorizontalGridLinesVisible(false);
        lineChart.setVerticalGridLinesVisible(false);
        lineChart.getXAxis().setVisible(false);
        lineChart.getYAxis().setVisible(false);
        lineChart.getStylesheets().addAll(ArpeggiatorumGUI.class.getResource("chart.css").toExternalForm());

        lineChart.getData().add(middleSeries);


        StackPane rootStack = new StackPane();
        rootStack.getChildren().addAll(chartCQTHistogram, lineChart);

        borderPane.centerProperty().setValue(rootStack);


        //Initialize internal components
        if (comboMIDIIn.getValue() != null) {
            MidiDeviceChooserItem item = comboMIDIIn.getValue();
            if (item.getValue() != null) {
                try {
                    Arpeggiatorum.getInstance().getArpeggiator().setMIDIIn(item.getValue());
                } catch (MidiUnavailableException e) {
                    Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
                    logger.log(Level.SEVERE, "MIDI Exception.", e);
                    LogGUIController.logBuffer.append(e.getMessage());
                }
            }
        }


        //Event Handlers
        //For whatever reason Sliders don’t have ActionEvents...
        //Instead, they have a Number called valueProperty that contains the current value of the slider.
        sliderThreshold.valueProperty().addListener((observable, oldValue, newValue) -> {
            textThreshold.setText(String.format("Current Threshold: %d", newValue.intValue()));
            Arpeggiatorum.getInstance().ThresholdChange(newValue);
        });

        sliderScale.valueProperty().addListener((observable, oldValue, newValue) -> Arpeggiatorum.getInstance().ScaleChange(newValue));

        sliderSharpness.valueProperty().addListener((observable, oldValue, newValue) -> Arpeggiatorum.getInstance().SharpnessChange(newValue));

        sliderTempo.valueProperty().addListener((observable, oldValue, newValue) -> {
            textTempo.setText(String.format("Current Tempo: %d", newValue.intValue()));
            Arpeggiatorum.getInstance().TempoChange(newValue);
        });

        sliderArticulation.valueProperty().addListener((observable, oldValue, newValue) -> Arpeggiatorum.getInstance().ArticulationChange(newValue));

        sliderRange.highValueProperty().addListener((observable, oldValue, newValue) -> {
            textRange.setText(String.format("[%d-%d]", ((int) sliderRange.getLowValue()), newValue.intValue()));
            Arpeggiatorum.getInstance().RangeChange(sliderRange.getLowValue(), newValue);
        });

        sliderRange.lowValueProperty().addListener((observable, oldValue, newValue) -> {
            textRange.setText(String.format("[%d-%d]", newValue.intValue(), ((int) sliderRange.getHighValue())));
            Arpeggiatorum.getInstance().RangeChange(newValue, sliderRange.getHighValue());
        });

        sliderEnrichment.valueProperty().addListener((observable, oldValue, newValue) -> {
            //textEnrichment.setText(String.format("%d %%", newValue.intValue()));
            Arpeggiatorum.getInstance().EnrichmentChange(newValue);
        });
    }
    //Sliders
    //They don't have event handlers...

    //Text
}
