package arpeggiatorum;

import arpeggiatorum.microphoneControl.*;
import arpeggiatorum.supplementary.Tools;
import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.devices.AudioDeviceFactory;
import com.jsyn.devices.AudioDeviceManager;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;
import meico.midi.EventMaker;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Arpeggiatorum implements Receiver {
    private static Arpeggiatorum instance;
    private final Synthesizer synth = JSyn.createSynthesizer(); // this Synthesizer instance is used for scheduling and audio processing
    private final Arpeggiator arpeggiator;
    private final ArrayList<Mic2MIDI> mic2Midi;

    //GUI
    public ChoiceBox choiceAudioChannel;
    //Properties
    private Properties configProp;
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
    //Sample Rate
    private static int sampleRate;
    // CQT Properties
    private static boolean cqtAutoTune;
    private static boolean cqtisPoly;
    private static double cqtMin;
    private static double cqtMax;
    private static float cqtThreshold;
    private static float cqtSpread;
    private static int cqtMinVel;
    private static int cqtMaxVel;
    private static double histScale;
    //Tarsos Properties
    private static int tarsosBuffer;
    private static double tarsosConfidence;
    private static int fftBinSize;
    private static double fftMaxFreq;

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

    public Arpeggiator getArpeggiator() {
        return arpeggiator;
    }

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

        this.synth.setRealTime(true);
        //this.synth.start();
        //Passing a value greater than 0 for input channels will cause an error. why?
        //this.synth.start(44100,AudioDeviceManager.USE_DEFAULT_DEVICE,2,AudioDeviceManager.USE_DEFAULT_DEVICE,0);
        this.arpeggiator = new Arpeggiator(this.synth, this); // instantiate the Arpeggiator and specify this GUI as
        // receiver of outgoing MIDI messages (to monitor
        // controller movements as slider movements in the GUI)
        configProp = new Properties();
        try (FileInputStream inputConfig = new FileInputStream("config.properties")) {
            //load a properties file from class path, inside static method
            configProp.load(inputConfig);
        } catch (IOException ex) {
            // GUI.updateLogGUI(ex.getMessage());
        }
        //Get the properties for internal values
        timeOut = Double.parseDouble(configProp.getProperty("Tap Timeout", "5000"));
        maxCount = Integer.parseInt(configProp.getProperty("Tap Count", "8"));
        times = new Instant[maxCount];
        sampleRate = Integer.parseInt(configProp.getProperty("Sample Rate", "44100"));
        cqtMin = Double.parseDouble(configProp.getProperty("CQT Min Freq", "41.205"));
        cqtMax = Double.parseDouble(configProp.getProperty("CQT Max Freq", "2637.02"));
        cqtThreshold = Float.parseFloat(configProp.getProperty("CQT Threshold", "0.01"));
        cqtSpread = Float.parseFloat(configProp.getProperty("CQT Spread", "0.55"));
        cqtisPoly = Boolean.parseBoolean(configProp.getProperty("CQT Poly", "true"));
        cqtAutoTune = Boolean.parseBoolean(configProp.getProperty("CQT Auto-Tune", "false"));
        Mic2MIDI_CQT.clusterSize = Integer.parseInt(configProp.getProperty("CQT Auto-Tune Cluster Size", "3"));

        cqtMinVel = Integer.parseInt(configProp.getProperty("CQT Min Velocity", "60"));
        cqtMaxVel = Integer.parseInt(configProp.getProperty("CQT Max Velocity", "127"));
        Mic2MIDI_CQT.scalingFactor = Double.parseDouble(configProp.getProperty("CQT Scaling Factor", "1.0"));
        tarsosBuffer = Integer.parseInt(configProp.getProperty("Tarsos Buffer Size", "1024"));
        tarsosConfidence = Double.parseDouble(configProp.getProperty("Tarsos Confidence", "0.98"));
        fftBinSize = Integer.parseInt(configProp.getProperty("FFT Bin Size", "9"));
        fftMaxFreq = Double.parseDouble(configProp.getProperty("FFT Max Freq", "1567.98"));
        histScale = Double.parseDouble(configProp.getProperty("Histogram Scale", "10.00"));

        //Pitch processors
        this.mic2Midi = new ArrayList<>();
        this.mic2Midi.add(new Mic2MIDI_JSyn(this.arpeggiator, sampleRate));
        this.mic2Midi.add(new Mic2MIDI_FFT(this.arpeggiator, sampleRate, fftBinSize, fftMaxFreq));
        this.mic2Midi.add(new Mic2MIDI_Tarsos(this.arpeggiator, sampleRate, tarsosBuffer, tarsosConfidence));
        this.mic2Midi.add(new Mic2MIDI_CQT(this.arpeggiator, sampleRate, cqtMin, cqtMax, cqtThreshold, cqtSpread, cqtisPoly, cqtAutoTune, cqtMinVel, cqtMaxVel));
        for (Mic2MIDI processor : mic2Midi) {
            this.synth.add(processor);
        }

    }

    /**
     * process incoming MIDI messages from the Arpeggiator
     *
     * @param message   the MIDI message to process
     * @param timeStamp the time-stamp for the message, in microseconds.
     */
    @Override
    public void send(MidiMessage message, long timeStamp) {
//        if (!(message instanceof ShortMessage sMsg))
//            return;
//
//        if ((inputChannelChooser.getSelectedItem() == null)
//                || (((ShortMessage) message).getChannel() != (int) inputChannelChooser.getSelectedItem()))
//            return;
//
//        switch (message.getStatus() & 0xF0) {
//            case EventMaker.CONTROL_CHANGE:
//                switch (sMsg.getData1()) {
//                    case EventMaker.CC_General_Purpose_Ctrl_1: // tonal enrichment slider
//                        SwingUtilities.invokeLater(() -> {
//                            float tonalEnrichmentAmount = ((float) sMsg.getData2()) / 127f;
//                            tonalEnrichmentSlider.setValue((int) (tonalEnrichmentAmount * 100));
//                        });
//                        break;
//                    case EventMaker.CC_General_Purpose_Ctrl_2: // tempo slider
//                        SwingUtilities.invokeLater(() -> {
//                            double tempo = ((900.0 * sMsg.getData2()) / 127.0) + 100.0;
//                            tempoSlider.setValue((int) tempo);
//                        });
//                        break;
//                    case EventMaker.CC_General_Purpose_Ctrl_3: // articulation slider
//                        SwingUtilities.invokeLater(() -> {
//                            double articulation = ((double) sMsg.getData2() / 127.0) - 0.5;
//                            articulationSlider.setValue((int) ((articulation * 100.0) + 100));
//                        });
//                        break;
//                    case EventMaker.CC_Undefined_Ctrl_8: // switch tonal enrichment set/tonality
//                        SwingUtilities.invokeLater(() -> {
//                            int numberOfOptions = tonalEnrichmentPresetChooser.getItemCount();
//                            int sliderValue = sMsg.getData2();
//                            int choice = (sliderValue * (--numberOfOptions)) / 127;
//                            tonalEnrichmentPresetChooser.setSelectedIndex(choice);
//                        });
//                        break;
//                    case EventMaker.CC_Undefined_Ctrl_7: // arpeggio pattern
//                        SwingUtilities.invokeLater(() -> {
//                            int numberOfOptions = patternChooser.getItemCount();
//                            int sliderValue = sMsg.getData2();
//                            int choice = (sliderValue * (--numberOfOptions) / 127);
//                            patternChooser.setSelectedIndex(choice);
//                        });
//                        break;
//                    case EventMaker.CC_Effect_Ctrl_2_14b: // trigger arpeggio channel
//                        SwingUtilities.invokeLater(() -> {
//                            int choice = (sMsg.getData2() >= 64) ? Arpeggiator.ARPEGGIO_CHANNEL_PRESET : -1;
//                            arpeggioChannelChooser.setSelectedItem(choice);
//                        });
//                        break;
//                    case EventMaker.CC_Undefined_Ctrl_3_14b: // trigger held notes channel
//                        SwingUtilities.invokeLater(() -> {
//                            int choice = (sMsg.getData2() >= 64) ? Arpeggiator.HELD_NOTES_CHANNEL_PRESET : -1;
//                            heldNotesChannelChooser.setSelectedItem(choice);
//                        });
//                        break;
//                    case EventMaker.CC_Undefined_Ctrl_4_14b: // trigger bass channel
//                        SwingUtilities.invokeLater(() -> {
//                            int choice = (sMsg.getData2() >= 64) ? Arpeggiator.BASS_CHANNEL_PRESET : -1;
//                            bassChannelChooser.setSelectedItem(choice);
//                        });
//                        break;
//                    default:
//                        break;
//                }
//                break;
//            default:
//                break;
//        }
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
        if (this.synth.isRunning()) {
            this.synth.stop();
        }
    }


    public static void SaveNClose() {
        //Save Config Properties
        try (OutputStream output = new FileOutputStream("config.properties")) {
            Properties prop = new Properties();
            // Set the properties values
            prop.setProperty("Name", "ArpeggiatorumGUI");
            prop.setProperty("Version", "0.1.2");

//            prop.setProperty("Channel", inputChannelChooser.getSelectedItem().toString());
//            prop.setProperty("Arpeggio", arpeggioChannelChooser.getSelectedItem().toString());
//            prop.setProperty("Bass", bassChannelChooser.getSelectedItem().toString());
//            prop.setProperty("Held", heldNotesChannelChooser.getSelectedItem().toString());
//            prop.setProperty("Threshold", String.valueOf(signal2noiseThreshold.getValue()));
//            prop.setProperty("Tempo", String.valueOf(tempoSlider.getValue()));
//            prop.setProperty("Articulation", String.valueOf(articulationSlider.getValue()));
//            prop.setProperty("RangeMin", String.valueOf(rangeSlider.getValue()));
//            prop.setProperty("RangeMax", String.valueOf(rangeSlider.getUpperValue()));
//            prop.setProperty("Density", String.valueOf(tonalEnrichmentSlider.getValue()));
//            prop.setProperty("Enrichment Preset", String.valueOf(tonalEnrichmentPresetChooser.getSelectedIndex()));
//            prop.setProperty("Enrichment Pattern", String.valueOf(patternChooser.getSelectedIndex()));
//            prop.setProperty("Pitch Detector", String.valueOf(mic2MIDIChooser.getSelectedIndex()));

            prop.setProperty("Tap Timeout", String.valueOf(timeOut));
            prop.setProperty("Tap Count", String.valueOf(maxCount));

            prop.setProperty("Sample Rate", String.valueOf(sampleRate));

            prop.setProperty("CQT Min Freq", String.valueOf(cqtMin));
            prop.setProperty("CQT Max Freq", String.valueOf(cqtMax));
            prop.setProperty("CQT Threshold", String.valueOf(cqtThreshold));
            prop.setProperty("CQT Spread", String.valueOf(cqtSpread));
            prop.setProperty("CQT Auto-Tune", String.valueOf(cqtAutoTune));
            prop.setProperty("CQT Auto-Tune Cluster Size", String.valueOf(Mic2MIDI_CQT.clusterSize));
            prop.setProperty("CQT Poly", String.valueOf(cqtisPoly));
            prop.setProperty("CQT Min Velocity", String.valueOf(cqtMinVel));
            prop.setProperty("CQT Max Velocity", String.valueOf(cqtMaxVel));
            prop.setProperty("CQT Scaling Factor", String.valueOf(Mic2MIDI_CQT.scalingFactor));


            prop.setProperty("Tarsos Buffer Size", String.valueOf(tarsosBuffer));
            prop.setProperty("Tarsos Confidence Threshold", String.valueOf(tarsosConfidence));

            prop.setProperty("FFT Bin Size", String.valueOf(fftBinSize));
            prop.setProperty("FFT Max Freq", String.valueOf(fftMaxFreq));
            prop.setProperty("Histogram Scale", String.valueOf(histScale));

//            prop.setProperty("MIDI Input", String.valueOf(midiInChooser.getSelectedItem().toString()));
//            prop.setProperty("MIDI Output", String.valueOf(midiOutChooser.getSelectedItem().toString()));
//            prop.setProperty("Audio Input", String.valueOf(audioInputChooser.getSelectedItem().toString()));
//            prop.setProperty("Audio Output", String.valueOf(audioOutputChooser.getSelectedItem().toString()));


            // Save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            //GUI.updateLogGUI(io.getMessage());
        } finally {
            System.exit(0); // The program may still run, enforce exit
        }
    }
}
