package arpeggiatorum;

import arpeggiatorum.gui.ArpeggiatorumGUI;
import arpeggiatorum.gui.ArpeggiatorumGUIController;
import arpeggiatorum.gui.LogGUIController;
import arpeggiatorum.microphoneControl.*;
import arpeggiatorum.supplementary.EventMaker;
import arpeggiatorum.supplementary.Tools;
import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.sound.midi.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Arpeggiatorum implements Receiver {

    public static Synthesizer synth = JSyn.createSynthesizer(); // this Synthesizer instance is used for scheduling and audio processing
    //Sample Rate
    public static int sampleRate;
    private static volatile Arpeggiatorum instance;
    //Properties
    private static Properties configProp;
    //Tap Tempo
    private static int tapCount = 0;
    private static int tapNext = 0;
    private static int maxCount;
    private static double timeOut;
    private static Instant timeLast;
    private static Instant timeNow;
    private static Instant[] times;
    private static long timeChange;
    private static int bpmNow;
    private static int bpmAvg;
    // CQT Properties
    private static boolean cqtAutoTune;
    private static boolean cqtIsPoly;
    private static double cqtMin;
    private static double cqtMax;
    private static float cqtThreshold;
    private static float cqtSpread;
    private static int cqtMinVel;
    private static int cqtMaxVel;
    //Tarsos Properties
    private static int tarsosBuffer;
    private static double tarsosConfidence;
    private static int fftBinSize;
    private static double fftMaxFreq;
    private final Arpeggiator arpeggiator;
    private final ArrayList<Mic2MIDI> mic2Midi;


    public static Scene sceneLog;
    public static Stage stageLog;
    private static int instanceLog = 0;

    public static Scene scenePerformance;
    public static Stage stagePerformance;
    private static int instancePerformance = 0;

    public Arpeggiatorum() {
        super();
        synchronized (Arpeggiatorum.class) {
            if (instance != null) throw new UnsupportedOperationException(
                    getClass() + " is singleton but constructor called more than once");
            instance = this;
        }
        //Logic
        timeLast = Instant.now();

        Tools.printAudioDevices(); // print a list of all available audio devices

        synth.setRealTime(true);
        //this.synth.start();
        //Passing a value greater than 0 for input channels will cause an error. why?
        //this.synth.start(44100,AudioDeviceManager.USE_DEFAULT_DEVICE,2,AudioDeviceManager.USE_DEFAULT_DEVICE,0);
        this.arpeggiator = new Arpeggiator(synth, this); // instantiate the Arpeggiator and specify this GUI as
        // receiver of outgoing MIDI messages (to monitor
        // controller movements as slider movements in the GUI)
        configProp = new Properties();
        try (FileInputStream inputConfig = new FileInputStream("config.properties")) {
            //load a properties file from class path, inside static method
            configProp.load(inputConfig);
        } catch (IOException ex) {
            Logger logger = Logger.getLogger(this.getClass().getName());
            logger.log(Level.SEVERE, "Failed to load properties. Defaults will be used.", ex);
            LogGUIController.logBuffer.append(ex.getMessage());
        }
        //Get the properties for internal values
        timeOut = Double.parseDouble(configProp.getProperty("Tap Timeout", "5000"));
        maxCount = Integer.parseInt(configProp.getProperty("Tap Count", "8"));
        times = new Instant[maxCount];
        sampleRate = Integer.parseInt(configProp.getProperty("Sample Rate", "44100"));
        cqtMin = Double.parseDouble(configProp.getProperty("CQT Min Freq", "41.205"));
        cqtMax = Double.parseDouble(configProp.getProperty("CQT Max Freq", "2637.22"));
        cqtThreshold = Float.parseFloat(configProp.getProperty("CQT Threshold", "0.01"));
        cqtSpread = Float.parseFloat(configProp.getProperty("CQT Spread", "0.55"));
        cqtIsPoly = Boolean.parseBoolean(configProp.getProperty("CQT Poly", "true"));
        cqtAutoTune = Boolean.parseBoolean(configProp.getProperty("CQT Auto-Tune", "false"));
        Mic2MIDI_CQT.clusterSize = Integer.parseInt(configProp.getProperty("CQT Auto-Tune Cluster Size", "3"));
        cqtMinVel = Integer.parseInt(configProp.getProperty("CQT Min Velocity", "30"));
        cqtMaxVel = Integer.parseInt(configProp.getProperty("CQT Max Velocity", "127"));
        Mic2MIDI_CQT.scalingFactor = Double.parseDouble(configProp.getProperty("CQT Scaling Factor", "0.0"));
        Mic2MIDI_CQT.sharpness = Float.parseFloat(configProp.getProperty("CQT Sharpness", "1.0"));
        tarsosBuffer = Integer.parseInt(configProp.getProperty("Tarsos Buffer Size", "1024"));
        tarsosConfidence = Double.parseDouble(configProp.getProperty("Tarsos Confidence", "0.98"));
        fftBinSize = Integer.parseInt(configProp.getProperty("FFT Bin Size", "9"));
        fftMaxFreq = Double.parseDouble(configProp.getProperty("FFT Max Freq", "1567.98"));

        //Pitch processors
        this.mic2Midi = new ArrayList<>();
        this.mic2Midi.add(new Mic2MIDI_JSyn(this.arpeggiator, sampleRate));
        this.mic2Midi.add(new Mic2MIDI_FFT(this.arpeggiator, sampleRate, fftBinSize, fftMaxFreq));
        this.mic2Midi.add(new Mic2MIDI_Tarsos(this.arpeggiator, sampleRate, tarsosBuffer, tarsosConfidence));
        this.mic2Midi.add(new Mic2MIDI_CQT(this.arpeggiator, sampleRate, cqtMin, cqtMax, cqtThreshold, cqtSpread, cqtIsPoly, cqtAutoTune, cqtMinVel, cqtMaxVel));
        for (Mic2MIDI processor : mic2Midi) {
            synth.add(processor);
        }


    }

    //Singleton pattern
    public static synchronized Arpeggiatorum getInstance() {
        if (instance == null) {
            synchronized (Arpeggiatorum.class) {
                if (instance == null) {
                    instance = new Arpeggiatorum();
                }
            }
        }
        return instance;
    }

    public static void LoadConfig(ArpeggiatorumGUI arpeggiatorumGUI) {
        //Get the properties values for GUI

        String midiInProp = configProp.getProperty("MIDI Input", "0");
        String midiOutProp = configProp.getProperty("MIDI Output", "0");
        String audioInProp = configProp.getProperty("Audio Input", "0");
        //String audioOutProp = configProp.getProperty("Audio Output", "0");

        //Select first value on startup (useful for first execution without properties)
        ArpeggiatorumGUI.controllerHandle.comboMIDIOut.getSelectionModel().select(0);
        ArpeggiatorumGUI.controllerHandle.comboMIDIIn.getSelectionModel().select(0);
        //ArpeggiatorumGUI.controllerHandle.comboAudioOut.getSelectionModel().select(0);
        ArpeggiatorumGUI.controllerHandle.comboAudioIn.getSelectionModel().select(0);
        ArpeggiatorumGUI.controllerHandle.comboAudioChannel.getSelectionModel().select(0);

        for (int i = 0; i < (long) ArpeggiatorumGUI.controllerHandle.comboMIDIIn.getItems().size(); i++) {
            if (ArpeggiatorumGUI.controllerHandle.comboMIDIIn.getItems().get(i).toString().equals(midiInProp)) {
                ArpeggiatorumGUI.controllerHandle.comboMIDIIn.getSelectionModel().select(i);
            }
        }
        for (int i = 0; i < (long) ArpeggiatorumGUI.controllerHandle.comboMIDIOut.getItems().size(); i++) {
            if (ArpeggiatorumGUI.controllerHandle.comboMIDIOut.getItems().get(i).toString().equals(midiOutProp)) {
                ArpeggiatorumGUI.controllerHandle.comboMIDIOut.getSelectionModel().select(i);
            }
        }

        for (int i = 0; i < (long) ArpeggiatorumGUI.controllerHandle.comboAudioIn.getItems().size(); i++) {
            if (ArpeggiatorumGUI.controllerHandle.comboAudioIn.getItems().get(i).equals(audioInProp)) {
                ArpeggiatorumGUI.controllerHandle.comboAudioIn.getSelectionModel().select(i);
            }
        }

        ArpeggiatorumGUI.controllerHandle.comboMIDIChannel.setValue(Integer.parseInt(configProp.getProperty("Channel", "0")));

        ArpeggiatorumGUI.controllerHandle.comboArpeggioChannel.setValue(Integer.parseInt(configProp.getProperty("Arpeggio", "1")));
        ArpeggiatorumGUI.controllerHandle.comboSustainedChannel.setValue(Integer.parseInt(configProp.getProperty("Sustained", "2")));
        ArpeggiatorumGUI.controllerHandle.comboBassChannel.setValue(Integer.parseInt(configProp.getProperty("Bass", "0")));

        if (Integer.parseInt(configProp.getProperty("Toggle Arpeggio", "-1")) == -1) {
            ArpeggiatorumGUI.controllerHandle.comboArpeggioChannel.setValue(-1);
        }

        if (Integer.parseInt(configProp.getProperty("Toggle Sustained", "-1")) == -1) {
            ArpeggiatorumGUI.controllerHandle.comboSustainedChannel.setValue(-1);
        }

        if (Integer.parseInt(configProp.getProperty("Toggle Bass", "-1")) == -1) {
            ArpeggiatorumGUI.controllerHandle.comboBassChannel.setValue(-1);
        }

        //ArpeggiatorumGUI.controllerHandle.sliderThreshold.setValue(Double.parseDouble(configProp.getProperty("Threshold", "500")));

        ArpeggiatorumGUI.controllerHandle.sliderScale.setValue(Float.parseFloat(configProp.getProperty("CQT Scaling Factor", "0.0")));
        ArpeggiatorumGUI.controllerHandle.sliderSharpness.setValue(Float.parseFloat(configProp.getProperty("CQT Sharpness", "1.0")));
        ArpeggiatorumGUI.controllerHandle.sliderTempo.setValue(Double.parseDouble(configProp.getProperty("Tempo", "500")));
        ArpeggiatorumGUI.controllerHandle.sliderArticulation.setValue(Double.parseDouble(configProp.getProperty("Articulation", "100")));
        ArpeggiatorumGUI.controllerHandle.sliderRange.setLowValue(Double.parseDouble(configProp.getProperty("RangeMin", "0")));
        ArpeggiatorumGUI.controllerHandle.sliderRange.setHighValue(Double.parseDouble(configProp.getProperty("RangeMax", "127")));
        ArpeggiatorumGUI.controllerHandle.sliderEnrichment.setValue(Double.parseDouble(configProp.getProperty("Density", "0")));
        ArpeggiatorumGUI.controllerHandle.comboEnrichment.getSelectionModel().select(Integer.parseInt(configProp.getProperty("Enrichment Preset", "0")));
        ArpeggiatorumGUI.controllerHandle.comboPattern.getSelectionModel().select(Integer.parseInt(configProp.getProperty("Enrichment Pattern", "0")));
        ArpeggiatorumGUI.controllerHandle.comboMic2MIDI.getSelectionModel().select(Integer.parseInt(configProp.getProperty("Pitch Detector", "0")));
        if (Boolean.parseBoolean(configProp.getProperty("CQT Auto-Tune", "false"))) {
            ArpeggiatorumGUI.controllerHandle.toggleButtonAutoTune.fire();

        }
    }

    public static void SaveNClose(ArpeggiatorumGUI arpeggiatorumGUI) {
        //Save Config Properties
        try (OutputStream output = new FileOutputStream("config.properties")) {
            Properties prop = new Properties();
            // Set the properties values
            prop.setProperty("Name", "ArpeggiatorumGUI");
            prop.setProperty("Version", Arpeggiator.version);

            prop.setProperty("Channel", ArpeggiatorumGUI.controllerHandle.comboMIDIChannel.getValue().toString());

            prop.setProperty("Toggle Arpeggio", ArpeggiatorumGUI.controllerHandle.comboArpeggioChannel.getValue().toString());
            prop.setProperty("Toggle Bass", ArpeggiatorumGUI.controllerHandle.comboBassChannel.getValue().toString());
            prop.setProperty("Toggle Sustained", ArpeggiatorumGUI.controllerHandle.comboSustainedChannel.getValue().toString());


            prop.setProperty("Arpeggio", ArpeggiatorumGUI.sessionArpeggioChannel.toString());
            prop.setProperty("Bass", ArpeggiatorumGUI.sessionBassChannel.toString());
            prop.setProperty("Sustained", ArpeggiatorumGUI.sessionSustainedChannel.toString());

           // prop.setProperty("Threshold", String.valueOf(ArpeggiatorumGUI.controllerHandle.sliderThreshold.getValue()));

            prop.setProperty("Tempo", String.valueOf(ArpeggiatorumGUI.controllerHandle.sliderTempo.getValue()));
            prop.setProperty("Articulation", String.valueOf(ArpeggiatorumGUI.controllerHandle.sliderArticulation.getValue()));

            prop.setProperty("RangeMin", String.valueOf(ArpeggiatorumGUI.controllerHandle.sliderRange.getLowValue()));
            prop.setProperty("RangeMax", String.valueOf(ArpeggiatorumGUI.controllerHandle.sliderRange.getHighValue()));

            prop.setProperty("Density", String.valueOf(ArpeggiatorumGUI.controllerHandle.sliderEnrichment.getValue()));
            prop.setProperty("Enrichment Preset", String.valueOf(ArpeggiatorumGUI.controllerHandle.comboEnrichment.getSelectionModel().getSelectedIndex()));
            prop.setProperty("Enrichment Pattern", String.valueOf(ArpeggiatorumGUI.controllerHandle.comboPattern.getSelectionModel().getSelectedIndex()));

            prop.setProperty("Pitch Detector", String.valueOf(ArpeggiatorumGUI.controllerHandle.comboMic2MIDI.getSelectionModel().getSelectedIndex()));

            prop.setProperty("Tap Timeout", String.valueOf(timeOut));
            prop.setProperty("Tap Count", String.valueOf(maxCount));

            prop.setProperty("Sample Rate", String.valueOf(sampleRate));

            prop.setProperty("CQT Min Freq", String.valueOf(cqtMin));
            prop.setProperty("CQT Max Freq", String.valueOf(cqtMax));
            prop.setProperty("CQT Threshold", String.valueOf(cqtThreshold));
            prop.setProperty("CQT Spread", String.valueOf(cqtSpread));
            prop.setProperty("CQT Auto-Tune", String.valueOf(cqtAutoTune));
            prop.setProperty("CQT Auto-Tune Cluster Size", String.valueOf(Mic2MIDI_CQT.clusterSize));
            prop.setProperty("CQT Poly", String.valueOf(cqtIsPoly));
            prop.setProperty("CQT Min Velocity", String.valueOf(cqtMinVel));
            prop.setProperty("CQT Max Velocity", String.valueOf(cqtMaxVel));
            prop.setProperty("CQT Scaling Factor", String.valueOf(Mic2MIDI_CQT.scalingFactor));
            prop.setProperty("CQT Sharpness", String.valueOf(Mic2MIDI_CQT.sharpness));

            prop.setProperty("Tarsos Buffer Size", String.valueOf(tarsosBuffer));
            prop.setProperty("Tarsos Confidence Threshold", String.valueOf(tarsosConfidence));

            prop.setProperty("FFT Bin Size", String.valueOf(fftBinSize));
            prop.setProperty("FFT Max Freq", String.valueOf(fftMaxFreq));

            prop.setProperty("MIDI Input", String.valueOf(ArpeggiatorumGUI.controllerHandle.comboMIDIIn.getValue().toString()));
            prop.setProperty("MIDI Output", String.valueOf(ArpeggiatorumGUI.controllerHandle.comboMIDIOut.getValue().toString()));
            prop.setProperty("Audio Input", ArpeggiatorumGUI.controllerHandle.comboAudioIn.getValue());

            // Save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            Logger logger = Logger.getLogger(arpeggiatorumGUI.getClass().getName());
            logger.log(Level.SEVERE, "Failed to save properties.", io);
            LogGUIController.logBuffer.append(io.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            System.exit(0); // The program may still run, enforce exit
        }
    }

    public static void LoadLog(ArpeggiatorumGUI arpeggiatorumGUI) {
        try {
            if (instanceLog >= 1) {
                stageLog.setAlwaysOnTop(true);
                stageLog.setAlwaysOnTop(false);
                stageLog.requestFocus();
                return;
            }
            instanceLog++;
            FXMLLoader fxmlLoader;
            fxmlLoader = new FXMLLoader(ArpeggiatorumGUI.class.getResource("LogGUI.fxml"));
            sceneLog = new Scene(fxmlLoader.load());
            stageLog = new Stage();
            ArpeggiatorumGUI.controllerHandleLog = fxmlLoader.getController();

            stageLog.setTitle("Log Messages");
            stageLog.setScene(sceneLog);
            stageLog.show();
            stageLog.setMaximized(true);
            stageLog.setOnCloseRequest((event) -> CloseLog());

            stageLog.addEventHandler(KeyEvent.KEY_RELEASED, (event) -> {
                switch (event.getCode()) {
                    case KeyCode.ESCAPE:
                    case KeyCode.Q: {
                        // Close current window
                        CloseLog();
                        break;
                    }
                    case KeyCode.L: {
                        //Close and reopen
                        //CloseLog(stageLog);
                        Arpeggiatorum.LoadLog(ArpeggiatorumGUI.getInstance());
                        break;
                    }
                    case KeyCode.P: {
                        Arpeggiatorum.LoadPerformance(ArpeggiatorumGUI.getInstance());
                        break;
                    }
                    default: {
                        LogGUIController.logBuffer.append("Unrecognized key.");
                    }
                }
            });
        } catch (IOException e) {
            Logger logger = Logger.getLogger(arpeggiatorumGUI.getClass().getName());
            logger.log(Level.SEVERE, "Failed to create new Window.", e);
        }
    }


    public static void LoadPerformance(ArpeggiatorumGUI arpeggiatorumGUI) {
        try {

            if (instancePerformance >= 1) {
                stagePerformance.setAlwaysOnTop(true);
                stagePerformance.setAlwaysOnTop(false);
                stagePerformance.requestFocus();
                return;
            }
            instancePerformance++;
            FXMLLoader fxmlLoader;
            fxmlLoader = new FXMLLoader(ArpeggiatorumGUI.class.getResource("PerformanceGUI.fxml"));

            //fxmlLoader.setLocation(ArpeggiatorumGUI.class.getClassLoader().getResource("arpeggiatorum/gui/LogGUI.fxml"));
            scenePerformance = new Scene(fxmlLoader.load());
            scenePerformance.getStylesheets().add(Objects.requireNonNull(ArpeggiatorumGUI.class.getResource("application.css")).toExternalForm());

            stagePerformance = new Stage();
            ArpeggiatorumGUI.controllerHandlePerformance = fxmlLoader.getController();

            stagePerformance.setTitle("Performance Mode");
            scenePerformance.setFill(Color.BLACK);
            stagePerformance.setScene(scenePerformance);
            stagePerformance.show();
            stagePerformance.setMaximized(true);
            stagePerformance.setFullScreen(true);
            stagePerformance.setOnCloseRequest((event) -> ClosePerformance());

            stagePerformance.addEventHandler(KeyEvent.KEY_RELEASED, (event) -> {
                switch (event.getCode()) {
                    case KeyCode.ESCAPE:
                    case KeyCode.Q: {
                        // Close current window
                        ClosePerformance();
                        break;
                    }
                    case KeyCode.L: {
                        Arpeggiatorum.LoadLog(ArpeggiatorumGUI.getInstance());
                        break;
                    }
                    case KeyCode.P: {
                        // Close current window
                        // ClosePerformance(stagePerformance);
                        //and reopen
                        Arpeggiatorum.LoadPerformance(ArpeggiatorumGUI.getInstance());
                        break;
                    }
                    default: {
                        LogGUIController.logBuffer.append("Unrecognized key.");
                    }
                }
            });
        } catch (IOException e) {
            System.out.println(e.getMessage());
            Logger logger = Logger.getLogger(arpeggiatorumGUI.getClass().getName());
            logger.log(Level.SEVERE, "Failed to create new Window.", e);
        }
    }

    public static void ClosePerformance() {
        //Fix for mac closing when full screen
        stagePerformance.setFullScreen(false);
        instancePerformance--;
        stagePerformance.close();
    }

    private static void CloseLog() {
        instanceLog--;
        stageLog.close();
    }

    public Arpeggiator getArpeggiator() {
        return arpeggiator;
    }

    public ArrayList<Mic2MIDI> getMic2Midi() {
        return mic2Midi;
    }

    /**
     * process incoming MIDI messages from the Arpeggiator
     *
     * @param message   the MIDI message to process
     * @param timeStamp the time-stamp for the message, in microseconds.
     */
    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (!(message instanceof ShortMessage sMsg))
            return;

        if ((ArpeggiatorumGUI.controllerHandle.comboMIDIChannel.getValue() == null)
                || (((ShortMessage) message).getChannel() != ArpeggiatorumGUI.controllerHandle.comboMIDIChannel.getValue()))
            return;

        switch (message.getStatus() & 0xF0) {
            case EventMaker.CONTROL_CHANGE:
                switch (sMsg.getData1()) {
                    case EventMaker.CC_General_Purpose_Ctrl_1: // tonal enrichment slider
                        Platform.runLater(() -> {
                            float tonalEnrichmentAmount = ((float) sMsg.getData2()) / 127f;
                            ArpeggiatorumGUI.controllerHandle.sliderEnrichment.setValue((int) (tonalEnrichmentAmount * 100));
                        });
                        break;
                    case EventMaker.CC_General_Purpose_Ctrl_2: // tempo slider
                        Platform.runLater(() -> {
                            double tempo = ((900.0 * sMsg.getData2()) / 127.0) + 100.0;
                            ArpeggiatorumGUI.controllerHandle.sliderTempo.setValue((int) tempo);
                        });
                        break;
                    case EventMaker.CC_General_Purpose_Ctrl_3: // articulation slider
                        Platform.runLater(() -> {
                            double articulation = ((double) sMsg.getData2() / 127.0) - 0.5;
                            ArpeggiatorumGUI.controllerHandle.sliderArticulation.setValue((int) ((articulation * 100.0) + 100));
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_8: // switch tonal enrichment set/tonality
                        Platform.runLater(() -> {
                            int numberOfOptions = ArpeggiatorumGUI.controllerHandle.comboEnrichment.getItems().size();
                            int sliderValue = sMsg.getData2();
                            int choice = (sliderValue * (--numberOfOptions)) / 127;
                            ArpeggiatorumGUI.controllerHandle.comboEnrichment.getSelectionModel().select(choice);
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_7: // arpeggio pattern
                        Platform.runLater(() -> {
                            int numberOfOptions = ArpeggiatorumGUI.controllerHandle.comboPattern.getItems().size();
                            int sliderValue = sMsg.getData2();
                            int choice = (sliderValue * (--numberOfOptions) / 127);
                            ArpeggiatorumGUI.controllerHandle.comboPattern.getSelectionModel().select(choice);

                        });
                        break;
                    case EventMaker.CC_Effect_Ctrl_2_14b: // trigger arpeggio channel
                        Platform.runLater(() -> {
                            Integer choice = (sMsg.getData2() >= 64) ? ArpeggiatorumGUI.sessionArpeggioChannel : -1;
                            boolean bChoice = sMsg.getData2() >= 64;
                            ArpeggiatorumGUI.controllerHandle.comboArpeggioChannel.getSelectionModel().select(choice);
                            if (ArpeggiatorumGUI.controllerHandlePerformance != null) {
                                ArpeggiatorumGUI.controllerHandlePerformance.toggleArpeggio.setSelected(bChoice);
                            }
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_3_14b: // trigger held notes channel
                        Platform.runLater(() -> {
                            Integer choice = (sMsg.getData2() >= 64) ? ArpeggiatorumGUI.sessionSustainedChannel : -1;
                            boolean bChoice = sMsg.getData2() >= 64;
                            ArpeggiatorumGUI.controllerHandle.comboSustainedChannel.getSelectionModel().select(choice);
                            if (ArpeggiatorumGUI.controllerHandlePerformance != null) {
                                ArpeggiatorumGUI.controllerHandlePerformance.toggleSustained.setSelected(bChoice);
                            }
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_4_14b: // trigger bass channel
                        Platform.runLater(() -> {
                            Integer choice = (sMsg.getData2() >= 64) ? ArpeggiatorumGUI.sessionBassChannel : -1;
                            boolean bChoice = sMsg.getData2() >= 64;
                            ArpeggiatorumGUI.controllerHandle.comboBassChannel.getSelectionModel().select(choice);
                            if (ArpeggiatorumGUI.controllerHandlePerformance != null) {
                                ArpeggiatorumGUI.controllerHandlePerformance.toggleBass.setSelected(bChoice);
                            }
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_5_14b: // Toggle Audio In
                        Platform.runLater(() -> {
                            boolean choice = sMsg.getData2() >= 64;
                            Arpeggiatorum.getInstance().Activate(choice);
                            if (ArpeggiatorumGUI.controllerHandlePerformance != null) {
                                ArpeggiatorumGUI.controllerHandlePerformance.toggleAudio.setSelected(choice);
                            }
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_6_14b: // Toggle AutoTune
                        Platform.runLater(() -> {
                            boolean choice = sMsg.getData2() >= 64;
                            Arpeggiatorum.getInstance().AutoTune(choice);
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_7_14b: // Set Scaling Factor
                        Platform.runLater(() -> {
                            double sliderValue = sMsg.getData2() / 127.00;
                            ArpeggiatorumGUI.controllerHandle.sliderScale.setValue(sliderValue * ArpeggiatorumGUI.controllerHandle.sliderScale.getMax());
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_8_14b: // Set Sharpness
                        Platform.runLater(() -> {
                            double sliderValue = sMsg.getData2() / 127.00;
                            ArpeggiatorumGUI.controllerHandle.sliderSharpness.setValue(sliderValue * ArpeggiatorumGUI.controllerHandle.sliderSharpness.getMax());
                        });
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }

    /**
     * Indicates that the application has finished using the receiver, and that
     * limited resources it requires may be released or made available.
     * <p>
     * If the creation of this {@code Receiver} resulted in implicitly opening
     * the underlying device, the device is implicitly closed by this method.
     * This is true unless the device is kept open by other {@code Receiver} or
     * {@code Transmitter} instances that opened the device implicitly, and
     * unless the device has been opened explicitly. If the device this
     * {@code Receiver} is retrieved from is closed explicitly by calling
     * {@link MidiDevice#close MidiDevice.close}, the {@code Receiver} is
     * closed, too. For a detailed description of open/close behaviour see the
     * class description of {@link MidiDevice MidiDevice}.
     *
     * @see MidiSystem#getReceiver
     */
    @Override
    public void close() {
        if (synth.isRunning()) {
            synth.stop();
        }
    }

    public void tapTempo() {
        //              Modified from:
//              <!-- Original:  Derek Chilcote-Batto (dac-b@usa.net) -->
//              <!-- Web Site:  http://www.mixed.net -->
//              <!-- Rewritten by: Rich Reel all8.com -->

        timeNow = Instant.now();
        timeChange = Duration.between(timeLast, timeNow).toMillis();
        if (timeChange > timeOut) {
            tapCount = 0;
            tapNext = 0;
        }
        tapCount++;
        // Enough beats to make a measurement (2 or more)?
        if (tapCount > 1) {
            // Enough to make an average measurement?
            if (tapCount > maxCount) {// average over maxCount
                bpmAvg = (int) ((2 * 60.0 * maxCount / Duration.between(times[tapNext], timeNow).toMillis()) * 1000);
                ArpeggiatorumGUI.controllerHandle.sliderTempo.setValue(bpmAvg);
            } else {
                bpmNow = (int) ((2 * 60.0 / timeChange) * 1000); // instantaneous measurement
                ArpeggiatorumGUI.controllerHandle.sliderTempo.setValue(bpmNow);
            }
        }

        timeLast = timeNow; // for instantaneous measurement and for timeout
        times[tapNext] = timeNow;
        tapNext++;
        if (tapNext >= maxCount) {
            tapNext = 0;
        }
    }

    public void AutoTune(boolean selected) {
        if (selected) {
            ArpeggiatorumGUI.controllerHandle.toggleButtonAutoTune.setStyle("-fx-background-color: Chartreuse;");
            ArpeggiatorumGUI.controllerHandle.toggleButtonAutoTune.setSelected(true);
            cqtAutoTune = true;
        } else {
            ArpeggiatorumGUI.controllerHandle.toggleButtonAutoTune.setStyle("");
            ArpeggiatorumGUI.controllerHandle.toggleButtonAutoTune.setSelected(false);
            cqtAutoTune = false;
        }
        if (ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.isSelected()) {
            ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.fire();
        }
        Arpeggiatorum.getInstance().Activate(false);
        Mic2MIDI_CQT.autoTune = cqtAutoTune;
    }

    public void Activate(boolean selected) {
        if (selected) {
            ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.setStyle("-fx-background-color: Chartreuse;");
            ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.setSelected(true);
            // ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.setText("Active");
            ArpeggiatorumGUI.controllerHandle.comboMic2MIDI.getValue().start();
            ArpeggiatorumGUI.controllerHandle.comboMic2MIDI.getValue().setSignalToNoiseThreshold(1.0); //The value does not matter
            //ArpeggiatorumGUI.controllerHandle.comboMic2MIDI.getValue().setSignalToNoiseThreshold(ArpeggiatorumGUI.controllerHandle.sliderThreshold.getValue() / ArpeggiatorumGUI.controllerHandle.sliderThreshold.getMax());
        } else {
            ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.setStyle("");
            ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.setSelected(false);

            // ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.setText("Activate");
            for (Mic2MIDI processor : mic2Midi) {
                processor.stop();
            }
            //TODO Try to avoid calling panic
            arpeggiator.panic();
        }
    }

    public void Upload() {
        int[] intervals = new int[]{
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e1.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e2.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e3.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e4.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e5.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e6.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e7.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e8.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e9.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e10.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e11.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e12.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e13.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e14.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e15.getText()),
                Integer.parseInt(ArpeggiatorumGUI.controllerHandle.e16.getText()),
        };
        this.arpeggiator.setTonalEnrichment(intervals);
    }

//    public void ThresholdChange(Number value) {
//        double scaledValue = value.doubleValue() / ArpeggiatorumGUI.controllerHandle.sliderThreshold.getMax();
//        for (Mic2MIDI processor : mic2Midi) {
//            processor.setSignalToNoiseThreshold(scaledValue);
//        }
//    }

    public void TempoChange(Number value) {
        this.arpeggiator.setTempo(value.doubleValue());
        if (ArpeggiatorumGUI.controllerHandlePerformance != null) {
            ArpeggiatorumGUI.controllerHandlePerformance.regulatorTempo.setTargetValue(value.doubleValue());
        }
    }

    public void RangeChange(Number lowValue, Number hiValue) {
        this.arpeggiator.setPitchRange(lowValue.intValue(), hiValue.intValue());
    }

    public void EnrichmentChange(Number value) {
        float tonalEnrichmentAmount = value.floatValue() / 100.0f;
        this.arpeggiator.setTonalEnrichmentAmount(tonalEnrichmentAmount);

    }

    public void ArticulationChange(Number value) {
        double articulation = (value.doubleValue() - 100) / 100.0;
        this.arpeggiator.setArticulation(articulation);
    }

    public void ScaleChange(Number value) {
        for (Mic2MIDI processor : mic2Midi) {
            if (processor instanceof Mic2MIDI_CQT) {
                Mic2MIDI_CQT.scalingFactor = (Math.pow(value.floatValue(),4.0f)*9999999.0f)+1;
               // System.out.println(Mic2MIDI_CQT.scalingFactor);
                //processor.setSignalToNoiseThreshold(ArpeggiatorumGUI.controllerHandle.sliderThreshold.getValue() / ArpeggiatorumGUI.controllerHandle.sliderThreshold.getMax());
            }
        }
    }

    public void SharpnessChange(Number value) {
        for (Mic2MIDI processor : mic2Midi) {
            if (processor instanceof Mic2MIDI_CQT) {
                ((Mic2MIDI_CQT) processor).updateDetectorsSharpness(value.doubleValue());
            }
        }
    }
}
