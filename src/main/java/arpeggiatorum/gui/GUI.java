package arpeggiatorum.gui;

import arpeggiatorum.Arpeggiator;
import arpeggiatorum.notePool.NotePool;
import arpeggiatorum.supplementary.NumberTextField;
import arpeggiatorum.supplementary.Tools;
import arpeggiatorum.supplementary.rangeSlider.RangeSlider;

import arpeggiatorum.microphoneControl.*;

import com.jsyn.JSyn;
import com.jsyn.devices.AudioDeviceFactory;
import com.jsyn.devices.AudioDeviceManager;
import com.jsyn.Synthesizer;

import meico.midi.EventMaker;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * A graphical user interface for Arpeggiatorum.
 *
 * @author Axel Berndt
 */
public class GUI extends JFrame implements Receiver {

    private final Synthesizer synth = JSyn.createSynthesizer(); // this Synthesizer instance is used for scheduling and audio processing
    private final int padding = 10;
    private static JComboBox<Integer> inputChannelChooser;
    private static JComboBox<Integer> arpeggioChannelChooser;
    private static JComboBox<Integer> heldNotesChannelChooser;
    private static JComboBox<Integer> bassChannelChooser;
    private static JSlider tempoSlider;
    private static JSlider articulationSlider;
    private static JSlider tonalEnrichmentSlider;
    private static JComboBox<NotePool.Pattern> patternChooser;
    private static JComboBox<TonalEnrichmentChooserItem> tonalEnrichmentPresetChooser;
    private static JSlider signal2noiseThreshold;
    private static RangeSlider rangeSlider;

    private final Arpeggiator arpeggiator;
    private final ArrayList<Mic2MIDI> mic2Midi;
    //Helper Windows
    public static JPanel cqtBinsPanel;
    private static final JFrame logFrame = new JFrame("Arpeggiatorum Log");
    private static final JTextArea logMessages = new JTextArea();

    Properties configProp;
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
    private static int sampleRate;
    private static boolean cqtAutoTune;
    private static double cqtMin;
    private static double cqtMax;
    private static float cqtThreshold;
    private static float cqtSpread;
    private static int cqtMinVel;
    private static int cqtMaxVel;
    private static int tarsosBuffer;
    private static double tarsosConfidence;
    private static int fftBinSize;
    private static double fftMaxFreq;

    /**
     * constructor
     */
    public GUI() {
        super("Arpeggiatorum");
        timeLast = Instant.now();

        Tools.printAudioDevices(); // print a list of all available audio devices

        this.synth.setRealTime(true);
        this.synth.start();
        //Passing a value greater than 0 for input channels will cause an error. why?
        //this.synth.start(44100,AudioDeviceManager.USE_DEFAULT_DEVICE,2,AudioDeviceManager.USE_DEFAULT_DEVICE,0);
        this.arpeggiator = new Arpeggiator(this.synth, this); // instantiate the Arpeggiator and specify this GUI as
        // receiver of outgoing MIDI messages (to monitor
        // controller movements as slider movements in the GUI)

        try (FileInputStream inputConfig = new FileInputStream("config.properties")) {
            configProp = new Properties();
            //load a properties file from class path, inside static method
            configProp.load(inputConfig);
        } catch (IOException ex) {
            GUI.updateLogGUI(ex.getMessage());
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
        cqtAutoTune = Boolean.parseBoolean(configProp.getProperty("CQT Auto-Tune", "false"));
        cqtMinVel = Integer.parseInt(configProp.getProperty("CQT Min Velocity", "60"));
        cqtMaxVel = Integer.parseInt(configProp.getProperty("CQT Max Velocity", "127"));
        tarsosBuffer = Integer.parseInt(configProp.getProperty("Tarsos Buffer", "1024"));
        tarsosConfidence = Double.parseDouble(configProp.getProperty("Tarsos Confidence", "0.98"));
        fftBinSize = Integer.parseInt(configProp.getProperty("FFT Bin Size", "9"));
        fftMaxFreq = Double.parseDouble(configProp.getProperty("FFT Max Freq", "1567.98"));

        //Pitch processors
        this.mic2Midi = new ArrayList<>();
        this.mic2Midi.add(new Mic2MIDI_JSyn(this.arpeggiator, sampleRate));
        this.mic2Midi.add(new Mic2MIDI_FFT(this.arpeggiator, sampleRate, fftBinSize, fftMaxFreq));
        this.mic2Midi.add(new Mic2MIDI_Tarsos(this.arpeggiator, sampleRate, tarsosBuffer, tarsosConfidence));
        this.mic2Midi.add(new Mic2MIDI_CQT(this.arpeggiator, sampleRate, cqtMin, cqtMax, cqtThreshold, cqtSpread, false, cqtAutoTune, cqtMinVel,cqtMaxVel));
        for (Mic2MIDI processor : mic2Midi) {
            this.synth.add(processor);
        }

        GUI.exitOnEsc(this); // close window on ESC
        // this.setResizable(false); // don't allow resizing

        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // what happens when the X is clicked
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook)); // do what has to be done on shutdown

        this.addWindowListener(new WindowAdapter() {
            /**
             * Invoked when a window has been closed.
             *
             * @param e
             */
            @Override
            public void windowClosing(WindowEvent e) {
                SaveNClose();
            }
        });
        // Execute the GUI building in the EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            // Set look and feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                     | UnsupportedLookAndFeelException e) {
                GUI.updateLogGUI(e.getMessage());
            }

            // The container panel
            GridBagLayout layout = new GridBagLayout();
            JPanel mainPanel = new JPanel(layout);
            this.add(mainPanel);

            ////////////////////

            JLabel midiInLabel = new JLabel("MIDI Input Port   ");
            midiInLabel.setHorizontalAlignment(JLabel.RIGHT);
            addComponentToGridBagLayout(mainPanel, layout, midiInLabel, 0, 0, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            JComboBox<MidiDeviceChooserItem> midiInChooser = createMidiInChooser();
            if (midiInChooser.getSelectedItem() != null) {
                MidiDeviceChooserItem item = (MidiDeviceChooserItem) midiInChooser.getSelectedItem();
                if (item.getValue() != null) {
                    try {
                        this.arpeggiator.setMidiIn(item.getValue());
                    } catch (MidiUnavailableException e) {
                        //e.printStackTrace();
                        GUI.updateLogGUI(e.getMessage());
                    }
                }
            }
            midiInChooser.addActionListener(actionEvent -> {
                if (midiInChooser.getSelectedItem() == null)
                    return;

                MidiDeviceChooserItem item = (MidiDeviceChooserItem) midiInChooser.getSelectedItem();
                if (item.getValue() == null)
                    return;

                try {
                    this.arpeggiator.setMidiIn(item.getValue());
                } catch (MidiUnavailableException e) {
                    //e.printStackTrace();
                    GUI.updateLogGUI(e.getMessage());
                }
            });
            addComponentToGridBagLayout(mainPanel, layout, midiInChooser, 1, 0, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            inputChannelChooser = new JComboBox<>(
                    new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
            inputChannelChooser.addActionListener(actionEvent -> {
                if (inputChannelChooser.getSelectedItem() != null)
                    this.arpeggiator.setInputChannel((int) inputChannelChooser.getSelectedItem());
            });
            addComponentToGridBagLayout(mainPanel, layout, inputChannelChooser, 2, 0, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            JLabel inputChannelLabel = new JLabel("   Channel");
            inputChannelLabel.setHorizontalAlignment(JLabel.LEFT);
            addComponentToGridBagLayout(mainPanel, layout, inputChannelLabel, 3, 0, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            ////////////////////

            JLabel midiOutLabel = new JLabel("MIDI Output Port   ");
            midiOutLabel.setHorizontalAlignment(JLabel.RIGHT);
            addComponentToGridBagLayout(mainPanel, layout, midiOutLabel, 0, 1, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            JComboBox<MidiDeviceChooserItem> midiOutChooser = createMidiOutChooser();
            if (midiOutChooser.getSelectedItem() != null) {
                MidiDeviceChooserItem item = (MidiDeviceChooserItem) midiOutChooser.getSelectedItem();
                if (item.getValue() != null) {
                    try {
                        this.arpeggiator.setMidiOut(item.getValue());
                    } catch (MidiUnavailableException e) {
                        //e.printStackTrace();
                        GUI.updateLogGUI(e.getMessage());
                    }
                }
            }
            midiOutChooser.addActionListener(actionEvent -> {
                if (midiOutChooser.getSelectedItem() == null)
                    return;

                MidiDeviceChooserItem item = (MidiDeviceChooserItem) midiOutChooser.getSelectedItem();
                if (item.getValue() == null)
                    return;

                try {
                    this.arpeggiator.setMidiOut(item.getValue());
                } catch (MidiUnavailableException e) {
                    //e.printStackTrace();
                    GUI.updateLogGUI(e.getMessage());
                }
            });
            addComponentToGridBagLayout(mainPanel, layout, midiOutChooser, 1, 1, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            arpeggioChannelChooser = new JComboBox<>(
                    new Integer[]{-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
            arpeggioChannelChooser.setSelectedItem(this.arpeggiator.getArpeggioChannel());
            arpeggioChannelChooser.addActionListener(actionEvent -> {
                if (arpeggioChannelChooser.getSelectedItem() != null)
                    this.arpeggiator.setArpeggioChannel((int) arpeggioChannelChooser.getSelectedItem());
            });
            addComponentToGridBagLayout(mainPanel, layout, arpeggioChannelChooser, 2, 1, 1, 1, 1.0, 1.0,
                    this.padding, this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            JLabel arpeggioChannelLabel = new JLabel("   Arpeggio Channel");
            arpeggioChannelLabel.setHorizontalAlignment(JLabel.LEFT);
            addComponentToGridBagLayout(mainPanel, layout, arpeggioChannelLabel, 3, 1, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            heldNotesChannelChooser = new JComboBox<>(
                    new Integer[]{-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
            heldNotesChannelChooser.setSelectedItem(this.arpeggiator.getHeldNotesChannel());
            heldNotesChannelChooser.addActionListener(actionEvent -> {
                if (heldNotesChannelChooser.getSelectedItem() != null)
                    this.arpeggiator.setHeldNotesChannel((int) heldNotesChannelChooser.getSelectedItem());
            });
            addComponentToGridBagLayout(mainPanel, layout, heldNotesChannelChooser, 2, 2, 1, 1, 1.0, 1.0,
                    this.padding, this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            JLabel heldNotesChannelLabel = new JLabel("   Held Notes Channel");
            heldNotesChannelLabel.setHorizontalAlignment(JLabel.LEFT);
            addComponentToGridBagLayout(mainPanel, layout, heldNotesChannelLabel, 3, 2, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            bassChannelChooser = new JComboBox<>(
                    new Integer[]{-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
            bassChannelChooser.setSelectedItem(this.arpeggiator.getBassChannel());
            bassChannelChooser.addActionListener(actionEvent -> {
                if (bassChannelChooser.getSelectedItem() != null)
                    this.arpeggiator.setBassChannel((int) bassChannelChooser.getSelectedItem());
            });
            addComponentToGridBagLayout(mainPanel, layout, bassChannelChooser, 2, 3, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            JLabel bassChannelLabel = new JLabel("   Bass Channel");
            bassChannelLabel.setHorizontalAlignment(JLabel.LEFT);
            addComponentToGridBagLayout(mainPanel, layout, bassChannelLabel, 3, 3, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            ////////////////////

            JLabel audioInputLabel = new JLabel("Audio Input   ");
            audioInputLabel.setHorizontalAlignment(JLabel.RIGHT);
            addComponentToGridBagLayout(mainPanel, layout, audioInputLabel, 0, 4, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            JToggleButton activateAudioInput = new JToggleButton("Activate", false);
            activateAudioInput.setOpaque(true);

            addComponentToGridBagLayout(mainPanel, layout, activateAudioInput, 3, 4, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            JComboBox<String> audioInputChooser = createAudioInChooser();
            audioInputChooser.addActionListener(actionEvent -> {
                // TODO: the following lines do not work!
                //Windows appear to be the problem, works on Mac
                this.synth.stop();

                int deviceID = Tools.getDeviceID((String) audioInputChooser.getSelectedItem());
                int deviceInputChannels = this.synth.getAudioDeviceManager().getMaxInputChannels(deviceID);
                this.synth.start(sampleRate,
                        deviceID,
                        deviceInputChannels,
                        AudioDeviceManager.USE_DEFAULT_DEVICE,
                        0);
                if (activateAudioInput.isSelected()) {
                    activateAudioInput.setSelected(false);
                }
            });
            audioInputChooser.setEnabled(true);
            //Selecting the first interface for startup
            audioInputChooser.setSelectedIndex(0);
            addComponentToGridBagLayout(mainPanel, layout, audioInputChooser, 1, 4, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            // Select the pitch processor
            JComboBox<Mic2MIDI> mic2MIDIChooser = new JComboBox<>();
            for (Mic2MIDI processor : mic2Midi) {
                mic2MIDIChooser.addItem(processor);
            }
            mic2MIDIChooser.addItemListener(event -> {
                if (activateAudioInput.isSelected()) {
                    if (event.getStateChange() == ItemEvent.SELECTED) {
                        ((Mic2MIDI) event.getItem()).start();
                    }
                    if (event.getStateChange() == ItemEvent.DESELECTED) {
                        ((Mic2MIDI) event.getItem()).stop();
                    }
                }
            });
            mic2MIDIChooser.setEnabled(true);
            mic2MIDIChooser.setSelectedIndex(0);
            addComponentToGridBagLayout(mainPanel, layout, mic2MIDIChooser, 2, 4, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            JLabel thresholdValue = new JLabel("   0.5");
            thresholdValue.setHorizontalAlignment(JLabel.CENTER);
            addComponentToGridBagLayout(mainPanel, layout, thresholdValue, 0, 5, 1, 1, 1.0, 1.0, this.padding, this.padding,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            signal2noiseThreshold = new JSlider(1, 1000);
            signal2noiseThreshold.setValue(500);
            signal2noiseThreshold.setOrientation(JSlider.HORIZONTAL);
            signal2noiseThreshold.setMajorTickSpacing(500);
            signal2noiseThreshold.setMinorTickSpacing(100);
            signal2noiseThreshold.setPaintTicks(true);
            signal2noiseThreshold.setToolTipText("Signal to Noise Threshold");
            Hashtable<Integer, JLabel> signal2noiseThresholdLabel = new Hashtable<>();
            signal2noiseThresholdLabel.put(0, new JLabel("0.0"));
            signal2noiseThresholdLabel.put(500, new JLabel("Threshold"));
            signal2noiseThresholdLabel.put(1000, new JLabel("1.0"));
            signal2noiseThreshold.setLabelTable(signal2noiseThresholdLabel);
            signal2noiseThreshold.setPaintLabels(true);
            signal2noiseThreshold.addChangeListener(changeEvent -> {
                double value = ((double) signal2noiseThreshold.getValue()) / signal2noiseThreshold.getMaximum();
                thresholdValue.setText("   " + value);
                for (Mic2MIDI processor : mic2Midi) {
                    processor.setSignalToNoiseThreshold(value);
                }
            });
            addComponentToGridBagLayout(mainPanel, layout, signal2noiseThreshold, 1, 5, 2, 1, 1.0, 1.0, this.padding, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            activateAudioInput.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent ev) {
                    if (ev.getStateChange() == ItemEvent.SELECTED) {
                        activateAudioInput.setForeground(Color.green);
                        activateAudioInput.setBackground(Color.green);
                        activateAudioInput.setText("Active");
                        ((Mic2MIDI) mic2MIDIChooser.getSelectedItem()).start();
                        ((Mic2MIDI) mic2MIDIChooser.getSelectedItem()).setSignalToNoiseThreshold(((double) signal2noiseThreshold.getValue()) / signal2noiseThreshold.getMaximum());
                    } else {
                        activateAudioInput.setForeground(Color.DARK_GRAY);
                        activateAudioInput.setBackground(Color.DARK_GRAY);
                        activateAudioInput.setText("Inactive");
                        for (Mic2MIDI processor : mic2Midi) {
                            processor.stop();
                        }
                        //Try to avoid calling panic
                        arpeggiator.panic();
                    }
                }
            });

            ////////////////////

            JLabel patternLabel = new JLabel("Arpeggiation Pattern   ");
            patternLabel.setHorizontalAlignment(JLabel.RIGHT);
            addComponentToGridBagLayout(mainPanel, layout, patternLabel, 3, 9, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            patternChooser = new JComboBox<>();
            patternChooser.addItem(NotePool.Pattern.up);
            patternChooser.addItem(NotePool.Pattern.down);
            patternChooser.addItem(NotePool.Pattern.up_down);
            patternChooser.addItem(NotePool.Pattern.random_no_repetitions);
            patternChooser.addItem(NotePool.Pattern.random_with_repetitions);
            patternChooser.setSelectedItem(this.arpeggiator.getPattern());
            patternChooser.addActionListener(actionEvent -> this.arpeggiator.setPattern((NotePool.Pattern) patternChooser.getSelectedItem()));
            addComponentToGridBagLayout(mainPanel, layout, patternChooser, 2, 9, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            ////////////////////

//            JLabel tempoLabel = new JLabel("Tempo in keystrokes/min   ");
//            tempoLabel.setHorizontalAlignment(JLabel.RIGHT);
//            addComponentToGridBagLayout(mainPanel, layout, tempoLabel, 0, 6, 1, 1, 1.0, 1.0, this.padding, this.padding,
//                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            JLabel tempoValue = new JLabel("   500");
            tempoValue.setHorizontalAlignment(JLabel.CENTER);
            addComponentToGridBagLayout(mainPanel, layout, tempoValue, 0, 6, 1, 1, 1.0, 1.0, this.padding, this.padding,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            tempoSlider = new JSlider(100, 1000);
            tempoSlider.setValue(500);
            tempoSlider.setOrientation(JSlider.HORIZONTAL);
            tempoSlider.setMajorTickSpacing(200);
            tempoSlider.setMinorTickSpacing(100);
            tempoSlider.setPaintTicks(true);
            Hashtable<Integer, JLabel> tempoSliderLabel = new Hashtable<>();
            tempoSliderLabel.put(100, new JLabel("100"));
            tempoSliderLabel.put(500, new JLabel("Tempo in keystrokes/min"));
            tempoSliderLabel.put(1000, new JLabel("1000"));
            tempoSlider.setLabelTable(tempoSliderLabel);
            tempoSlider.setPaintLabels(true);
            tempoSlider.addChangeListener(changeEvent -> {
                int tempo = tempoSlider.getValue();
                if (tempo < 100)
                    tempoSlider.setValue(100);
                if (tempo > 1000)
                    tempoSlider.setValue(1000);
                this.arpeggiator.setTempo(tempo);
                tempoValue.setText("   " + tempo);
            });
            addComponentToGridBagLayout(mainPanel, layout, tempoSlider, 1, 6, 2, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            JButton tapTempo = getTempo();
            addComponentToGridBagLayout(mainPanel, layout, tapTempo, 3, 6, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            ////////////////////

            JLabel articulationLabel = new JLabel("Articulation   ");
            articulationLabel.setHorizontalAlignment(JLabel.RIGHT);
            addComponentToGridBagLayout(mainPanel, layout, articulationLabel, 0, 7, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            articulationSlider = new JSlider(50, 150);
            articulationSlider.setValue(100);
            articulationSlider.setOrientation(JSlider.HORIZONTAL);
            articulationSlider.setMajorTickSpacing(50);
            articulationSlider.setMinorTickSpacing(10);
            articulationSlider.setPaintTicks(true);
            Hashtable<Integer, JLabel> articulationSliderLabel = new Hashtable<>();
            articulationSliderLabel.put(50, new JLabel("staccato"));
            articulationSliderLabel.put(100, new JLabel("tenuto"));
            articulationSliderLabel.put(150, new JLabel("legato"));
            articulationSlider.setLabelTable(articulationSliderLabel);
            articulationSlider.setPaintLabels(true);
            articulationSlider.addChangeListener(changeEvent -> {
                double articulation = ((double) (articulationSlider.getValue() - 100)) / 100.0;
                this.arpeggiator.setArticulation(articulation);
            });
            addComponentToGridBagLayout(mainPanel, layout, articulationSlider, 1, 7, 2, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            ////////////////////

            JLabel pitchRangeLabel = new JLabel("Pitch Range   ");
            pitchRangeLabel.setHorizontalAlignment(JLabel.RIGHT);
            addComponentToGridBagLayout(mainPanel, layout, pitchRangeLabel, 0, 8, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            JLabel pitchRangeNumbers = new JLabel("   [0, 127]");
            pitchRangeNumbers.setHorizontalAlignment(JLabel.LEFT);
            addComponentToGridBagLayout(mainPanel, layout, pitchRangeNumbers, 3, 8, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            rangeSlider = new RangeSlider(0, 127); // the pitch range slider
            rangeSlider.setValue(0);
            rangeSlider.setUpperValue(127);
            rangeSlider.setOrientation(JSlider.HORIZONTAL);
            Color thumbColor = new Color(0, 119, 214);
            Color trackColor = thumbColor.brighter();
            rangeSlider.setColors(trackColor, thumbColor, thumbColor);
            rangeSlider.setMajorTickSpacing(20);
            rangeSlider.setMinorTickSpacing(10);
            rangeSlider.setPaintTicks(true);
            Hashtable<Integer, JLabel> rangeSliderLabels = new Hashtable<>();
            rangeSliderLabels.put(0, new JLabel("0"));
            rangeSliderLabels.put(60, new JLabel("60"));
            rangeSliderLabels.put(127, new JLabel("127"));
            rangeSlider.setLabelTable(rangeSliderLabels);
            rangeSlider.setPaintLabels(true);
            rangeSlider.addChangeListener(e -> {
                RangeSlider slider = (RangeSlider) e.getSource();
                int lowestPitch = slider.getValue();
                int highestPitch = slider.getUpperValue();
                this.arpeggiator.setPitchRange(lowestPitch, highestPitch);
                pitchRangeNumbers.setText("   [" + lowestPitch + ", " + highestPitch + "]");
            });
            addComponentToGridBagLayout(mainPanel, layout, rangeSlider, 1, 8, 2, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            ////////////////////

            JLabel tonalEnrichmentLabel = new JLabel("Tonal Enrichment   ");
            tonalEnrichmentLabel.setHorizontalAlignment(JLabel.RIGHT);
            addComponentToGridBagLayout(mainPanel, layout, tonalEnrichmentLabel, 0, 9, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            JButton tonalEnrichmentButton = new JButton("Upload");

            NumberTextField e1 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e2 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e3 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e4 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e5 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e6 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e7 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e8 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e9 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e10 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e11 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e12 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e13 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e14 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e15 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            NumberTextField e16 = new NumberTextField(NumberFormat.getIntegerInstance(), 0);
            e1.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e2.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e3.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e4.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e5.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e6.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e7.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e8.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e9.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e10.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e11.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e12.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e13.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e14.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e15.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            e16.addActionListener(actionEvent -> tonalEnrichmentButton.doClick());
            GridBagLayout tonalEnrichmentLayout = new GridBagLayout();
            JPanel tonalEnrichmentPanel = new JPanel(tonalEnrichmentLayout);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e1, 0, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e2, 1, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e3, 2, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e4, 3, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e5, 4, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e6, 5, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e7, 6, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e8, 7, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e9, 8, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e10, 9, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e11, 10, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e12, 11, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e13, 12, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e14, 13, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e15, 14, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e16, 15, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);
            addComponentToGridBagLayout(mainPanel, layout, tonalEnrichmentPanel, 1, 10, 2, 1, 100.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END, 0);

            tonalEnrichmentButton.addActionListener(actionEvent -> {
                int[] intervals = new int[]{
                        (e1.getValue() instanceof Long) ? ((Long) e1.getValue()).intValue() : (Integer) e1.getValue(), // it
                        // can be of type Long or Integer
                        (e2.getValue() instanceof Long) ? ((Long) e2.getValue()).intValue() : (Integer) e2.getValue(),
                        (e3.getValue() instanceof Long) ? ((Long) e3.getValue()).intValue() : (Integer) e3.getValue(),
                        (e4.getValue() instanceof Long) ? ((Long) e4.getValue()).intValue() : (Integer) e4.getValue(),
                        (e5.getValue() instanceof Long) ? ((Long) e5.getValue()).intValue() : (Integer) e5.getValue(),
                        (e6.getValue() instanceof Long) ? ((Long) e6.getValue()).intValue() : (Integer) e6.getValue(),
                        (e7.getValue() instanceof Long) ? ((Long) e7.getValue()).intValue() : (Integer) e7.getValue(),
                        (e8.getValue() instanceof Long) ? ((Long) e8.getValue()).intValue() : (Integer) e8.getValue(),
                        (e9.getValue() instanceof Long) ? ((Long) e9.getValue()).intValue() : (Integer) e9.getValue(),
                        (e10.getValue() instanceof Long) ? ((Long) e10.getValue()).intValue()
                                : (Integer) e10.getValue(),
                        (e11.getValue() instanceof Long) ? ((Long) e11.getValue()).intValue()
                                : (Integer) e11.getValue(),
                        (e12.getValue() instanceof Long) ? ((Long) e12.getValue()).intValue()
                                : (Integer) e12.getValue(),
                        (e13.getValue() instanceof Long) ? ((Long) e13.getValue()).intValue()
                                : (Integer) e13.getValue(),
                        (e14.getValue() instanceof Long) ? ((Long) e14.getValue()).intValue()
                                : (Integer) e14.getValue(),
                        (e15.getValue() instanceof Long) ? ((Long) e15.getValue()).intValue()
                                : (Integer) e15.getValue(),
                        (e16.getValue() instanceof Long) ? ((Long) e16.getValue()).intValue()
                                : (Integer) e16.getValue(),
                };
                this.arpeggiator.setTonalEnrichment(intervals);
            });
            addComponentToGridBagLayout(mainPanel, layout, tonalEnrichmentButton, 3, 10, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            tonalEnrichmentPresetChooser = new JComboBox<>();
            tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Empty",
                    new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
            tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Octaves",
                    new int[]{0, 12, 24, 36, 48, 60, 72, 84, 96, 108, 120, 0, 0, 0, 0, 0}));
            tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Octaves and Fifths",
                    new int[]{0, 7, 12, 19, 24, 31, 36, 43, 48, 55, 60, 67, 72, 79, 84, 91}));
            tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Major Triad",
                    new int[]{0, 4, 7, 12, 16, 19, 24, 28, 31, 36, 40, 43, 48, 52, 55, 60}));
            tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Minor Triad",
                    new int[]{0, 3, 7, 12, 15, 19, 24, 27, 31, 36, 39, 43, 48, 51, 55, 60}));
            tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Major Thirds",
                    new int[]{0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52, 56, 60}));
            tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Forths",
                    new int[]{0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75}));
            tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Series of Overtones",
                    new int[]{0, 12, 19, 24, 28, 31, 34, 36, 38, 40, 42, 43, 44, 46, 47, 48}));
            tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Overtones Transposed Down",
                    new int[]{0, 12, 24, 7, 19, 4, 16, 10, 22, 2, 14, 6, 18, 8, 20, 11}));
            tonalEnrichmentPresetChooser.addActionListener(actionEvent -> {
                if (tonalEnrichmentPresetChooser.getSelectedItem() == null)
                    return;
                int[] intervals = ((TonalEnrichmentChooserItem) tonalEnrichmentPresetChooser.getSelectedItem())
                        .getValue();
                e1.setValue((intervals.length >= 1) ? intervals[0] : 0);
                e2.setValue((intervals.length >= 2) ? intervals[1] : 0);
                e3.setValue((intervals.length >= 3) ? intervals[2] : 0);
                e4.setValue((intervals.length >= 4) ? intervals[3] : 0);
                e5.setValue((intervals.length >= 5) ? intervals[4] : 0);
                e6.setValue((intervals.length >= 6) ? intervals[5] : 0);
                e7.setValue((intervals.length >= 7) ? intervals[6] : 0);
                e8.setValue((intervals.length >= 8) ? intervals[7] : 0);
                e9.setValue((intervals.length >= 9) ? intervals[8] : 0);
                e10.setValue((intervals.length >= 10) ? intervals[9] : 0);
                e11.setValue((intervals.length >= 11) ? intervals[10] : 0);
                e12.setValue((intervals.length >= 12) ? intervals[11] : 0);
                e13.setValue((intervals.length >= 13) ? intervals[12] : 0);
                e14.setValue((intervals.length >= 14) ? intervals[13] : 0);
                e15.setValue((intervals.length >= 15) ? intervals[14] : 0);
                e16.setValue((intervals.length >= 16) ? intervals[15] : 0);
                this.arpeggiator.setTonalEnrichment(intervals);
            });
            addComponentToGridBagLayout(mainPanel, layout, tonalEnrichmentPresetChooser, 1, 9, 1, 1, 1.0, 1.0,
                    this.padding, this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            JLabel tonalEnrichmentDensityLabel = new JLabel("");
            tonalEnrichmentDensityLabel.setHorizontalAlignment(JLabel.RIGHT);
            addComponentToGridBagLayout(mainPanel, layout, tonalEnrichmentDensityLabel, 0, 11, 1, 1, 1.0, 1.0,
                    this.padding, this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            tonalEnrichmentSlider = new JSlider(0, 100);
            tonalEnrichmentSlider.setValue(0);
            tonalEnrichmentSlider.setOrientation(JSlider.HORIZONTAL);
            tonalEnrichmentSlider.setMajorTickSpacing(50);
            tonalEnrichmentSlider.setMinorTickSpacing(10);
            tonalEnrichmentSlider.setPaintTicks(true);
            Hashtable<Integer, JLabel> tonalEnrichmentSliderLabel = new Hashtable<>();
            tonalEnrichmentSliderLabel.put(0, new JLabel("0%"));
            tonalEnrichmentSliderLabel.put(50, new JLabel("50%"));
            tonalEnrichmentSliderLabel.put(100, new JLabel("100%"));
            tonalEnrichmentSlider.setLabelTable(tonalEnrichmentSliderLabel);
            tonalEnrichmentSlider.setPaintLabels(true);
            tonalEnrichmentSlider.addChangeListener(changeEvent -> {
                float tonalEnrichmentAmount = (float) (tonalEnrichmentSlider.getValue()) / 100.0f;
                tonalEnrichmentDensityLabel.setText(tonalEnrichmentSlider.getValue() + "%");
                this.arpeggiator.setTonalEnrichmentAmount(tonalEnrichmentAmount);
            });


            addComponentToGridBagLayout(mainPanel, layout, tonalEnrichmentSlider, 1, 11, 2, 1, 1.0, 1.0,
                    this.padding, this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            ////////////////////

            JButton panic = new JButton("PANIC!");
            panic.setBackground(Color.red);
            panic.setForeground(Color.red);
            panic.addActionListener(actionEvent -> this.arpeggiator.panic());
            addComponentToGridBagLayout(mainPanel, layout, panic, 1, 12, 2, 1, 1.0, 1.0, this.padding, this.padding,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            ////////////////////
            cqtBinsPanel = Mic2MIDI_CQT.cqtHist;
            addComponentToGridBagLayout(mainPanel, layout, cqtBinsPanel, 0, 13, 4, 1, 1, 1000, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            // pack it all together and show it
            this.pack();
            this.setVisible(true);
            //Start fullscreen
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            this.setMaximizedBounds(env.getMaximumWindowBounds());
            this.setExtendedState(this.getExtendedState() | MAXIMIZED_BOTH);
            //Menu Bar for Log
            JMenuBar menuBar = new JMenuBar();
            JMenu menu = new JMenu("Utilities");
            JMenuItem menuLog = new JMenuItem("Log");
            GridBagLayout logLayout = new GridBagLayout();
            JPanel logPanel = new JPanel(logLayout);
            JScrollPane scrollPanel = new JScrollPane(logMessages, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            logFrame.add(logPanel);
            logMessages.setEditable(false);
            logMessages.setAutoscrolls(true);
            logMessages.setBackground(Color.white);
            DefaultCaret caret = (DefaultCaret) logMessages.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            logMessages.setCaretPosition(logMessages.getDocument().getLength());
            addComponentToGridBagLayout(logPanel, logLayout, scrollPanel, 0, 0, 1, 1, 1.0, 1.0, this.padding, this.padding,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            logFrame.pack();
            logFrame.setLocationRelativeTo(null);
            menuLog.addActionListener(actionEvent -> logFrame.setVisible(true));
            menu.add(menuLog);
            menuBar.add(menu);
            this.setJMenuBar(menuBar);

            //Get the properties values for GUI
            inputChannelChooser.setSelectedItem(Integer.parseInt(configProp.getProperty("Channel", "0")));
            arpeggioChannelChooser.setSelectedItem(Integer.parseInt(configProp.getProperty("Arpeggio", "1")));
            bassChannelChooser.setSelectedItem(Integer.parseInt(configProp.getProperty("Bass", "0")));
            heldNotesChannelChooser.setSelectedItem(Integer.parseInt(configProp.getProperty("Held", "2")));
            signal2noiseThreshold.setValue(Integer.parseInt(configProp.getProperty("Threshold", "500")));
            tempoSlider.setValue(Integer.parseInt(configProp.getProperty("Tempo", "500")));
            articulationSlider.setValue(Integer.parseInt(configProp.getProperty("Articulation", "100")));
            rangeSlider.setValue(Integer.parseInt(configProp.getProperty("RangeMin", "0")));
            rangeSlider.setUpperValue(Integer.parseInt(configProp.getProperty("RangeMax", "127")));
            tonalEnrichmentSlider.setValue(Integer.parseInt(configProp.getProperty("Density", "0")));
            tonalEnrichmentPresetChooser.setSelectedIndex(Integer.parseInt(configProp.getProperty("Enrichment Preset", "0")));
            patternChooser.setSelectedIndex(Integer.parseInt(configProp.getProperty("Enrichment Pattern", "0")));

        });
    }

    private JButton getTempo() {
        JButton tapTempo = new JButton("Tap Tempo");
//              Modified from:
//              <!-- Original:  Derek Chilcote-Batto (dac-b@usa.net) -->
//              <!-- Web Site:  http://www.mixed.net -->
//              <!-- Rewritten by: Rich Reel all8.com -->
        tapTempo.addActionListener(actionEvent -> {
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
                    tempoSlider.setValue(bpmAvg);
                } else {
                    bpmNow = (int) ((2 * 60.0 / timeChange) * 1000); // instantaneous measurement
                    tempoSlider.setValue(bpmNow);
                }
            }

            timeLast = timeNow; // for instantaneous measurement and for timeout
            times[tapNext] = timeNow;
            tapNext++;
            if (tapNext >= maxCount) {
                tapNext = 0;
            }
        });
        return tapTempo;
    }

    private static void SaveNClose() {
        try (OutputStream output = new FileOutputStream("config.properties")) {
            Properties prop = new Properties();
            // Set the properties values
            prop.setProperty("Name", "Arpeggiatorum");
            prop.setProperty("Version", "2.0");

            prop.setProperty("Channel", inputChannelChooser.getSelectedItem().toString());
            prop.setProperty("Arpeggio", arpeggioChannelChooser.getSelectedItem().toString());
            prop.setProperty("Bass", bassChannelChooser.getSelectedItem().toString());
            prop.setProperty("Held", heldNotesChannelChooser.getSelectedItem().toString());
            prop.setProperty("Threshold", String.valueOf(signal2noiseThreshold.getValue()));
            prop.setProperty("Tempo", String.valueOf(tempoSlider.getValue()));
            prop.setProperty("Articulation", String.valueOf(articulationSlider.getValue()));
            prop.setProperty("RangeMin", String.valueOf(rangeSlider.getValue()));
            prop.setProperty("RangeMax", String.valueOf(rangeSlider.getUpperValue()));
            prop.setProperty("Density", String.valueOf(tonalEnrichmentSlider.getValue()));
            prop.setProperty("Enrichment Preset", String.valueOf(tonalEnrichmentPresetChooser.getSelectedIndex()));
            prop.setProperty("Enrichment Pattern", String.valueOf(patternChooser.getSelectedIndex()));

            prop.setProperty("Tap Timeout", String.valueOf(timeOut));
            prop.setProperty("Tap Count", String.valueOf(maxCount));

            prop.setProperty("Sample Rate", String.valueOf(sampleRate));

            prop.setProperty("CQT Min Freq", String.valueOf(cqtMin));
            prop.setProperty("CQT Max Freq", String.valueOf(cqtMax));
            prop.setProperty("CQT Threshold", String.valueOf(cqtThreshold));
            prop.setProperty("CQT Spread", String.valueOf(cqtSpread));
            prop.setProperty("CQT Auto-Tune", String.valueOf(cqtAutoTune));
            prop.setProperty("CQT Min Velocity", String.valueOf(cqtMinVel));
            prop.setProperty("CQT Max Velocity", String.valueOf(cqtMaxVel));


            prop.setProperty("Tarsos Buffer Size", String.valueOf(tarsosBuffer));
            prop.setProperty("Tarsos Confidence Threshold", String.valueOf(tarsosConfidence));

            prop.setProperty("FFT Bin Size", String.valueOf(fftBinSize));
            prop.setProperty("FFT Max Freq", String.valueOf(fftMaxFreq));

            // Save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            GUI.updateLogGUI(io.getMessage());
        } finally {
            System.exit(0); // The program may still run, enforce exit
        }
    }

    /**
     * adds window close on ESC
     *
     * @param frame
     */
    protected static void exitOnEsc(JFrame frame) {
        // keyboard input via key binding
        InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Exit"); // close the window when ESC pressed
        frame.getRootPane().getActionMap().put("Exit", new AbstractAction() { // define the "Exit" action
            @Override
            public void actionPerformed(ActionEvent e) {
                SaveNClose();

            }
        });
    }

    /**
     * Do all that must be done before shutdown.
     */
    private void shutdownHook() {
        // AllNotesOff? Anything?
        // exitOnEsc(this);
    }

    public static void addComponentToGridBagLayout(Container container, GridBagLayout gridBagLayout,
                                                   Component component, int x, int y, int width, int height, double weightx, double weighty, int ipadx,
                                                   int ipady, int fill, int anchor) {
        addComponentToGridBagLayout(container, gridBagLayout, component, x, y, width, height, weightx, weighty, ipadx, ipady, fill, anchor, 2);
    }

    /**
     * a helper method to add components to a gridbag layouted container
     *
     * @param container
     * @param gridBagLayout
     * @param component
     * @param x
     * @param y
     * @param width
     * @param height
     * @param weightx
     * @param weighty
     * @param ipadx
     * @param ipady
     * @param fill
     * @param insetSize
     */
    public static void addComponentToGridBagLayout(Container container, GridBagLayout gridBagLayout,
                                                   Component component, int x, int y, int width, int height, double weightx, double weighty, int ipadx,
                                                   int ipady, int fill, int anchor, int insetSize) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = fill;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.insets = new Insets(insetSize, insetSize, insetSize, insetSize);
        gbc.ipadx = ipadx;
        gbc.ipady = ipady;
        gbc.anchor = anchor;
        gridBagLayout.setConstraints(component, gbc);
        container.add(component);
    }

    /**
     * create a MIDI port chooser, ready to be added to the GUI
     *
     * @return
     */
    public static JComboBox<MidiDeviceChooserItem> createMidiInChooser() {
        JComboBox<MidiDeviceChooserItem> midiPortChooser = new JComboBox<>();

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
                    midiPortChooser.addItem(new MidiDeviceChooserItem(info));
                } catch (MidiUnavailableException e) {
                    GUI.updateLogGUI(e.getMessage());
                }
            }
        }

        return midiPortChooser;
    }

    /**
     * create a MIDI port chooser, ready to be added to the GUI
     *
     * @return
     */
    public static JComboBox<MidiDeviceChooserItem> createMidiOutChooser() {
        JComboBox<MidiDeviceChooserItem> midiPortChooser = new JComboBox<>();

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
                    midiPortChooser.addItem(new MidiDeviceChooserItem(info));
                } catch (MidiUnavailableException e) {
                    GUI.updateLogGUI(e.getMessage());
                }
            }
        }

        return midiPortChooser;
    }

    /**
     * create a combobox with audio input devices
     *
     * @return
     */
    public static JComboBox<String> createAudioInChooser() {
        JComboBox<String> audioInChooser = new JComboBox<>();

        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager();
        int numDevices = audioManager.getDeviceCount();

        for (int i = 0; i < numDevices; ++i) {
            if (audioManager.getMaxInputChannels(i) <= 0)
                continue;
            audioInChooser.addItem(audioManager.getDeviceName(i));
        }

        return audioInChooser;
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

        if ((inputChannelChooser.getSelectedItem() == null)
                || (((ShortMessage) message).getChannel() != (int) inputChannelChooser.getSelectedItem()))
            return;

        switch (message.getStatus() & 0xF0) {
            case EventMaker.CONTROL_CHANGE:
                switch (sMsg.getData1()) {
                    case EventMaker.CC_General_Purpose_Ctrl_1: // tonal enrichment slider
                        SwingUtilities.invokeLater(() -> {
                            float tonalEnrichmentAmount = ((float) sMsg.getData2()) / 127f;
                            tonalEnrichmentSlider.setValue((int) (tonalEnrichmentAmount * 100));
                        });
                        break;
                    case EventMaker.CC_General_Purpose_Ctrl_2: // tempo slider
                        SwingUtilities.invokeLater(() -> {
                            double tempo = ((900.0 * sMsg.getData2()) / 127.0) + 100.0;
                            tempoSlider.setValue((int) tempo);
                        });
                        break;
                    case EventMaker.CC_General_Purpose_Ctrl_3: // articulation slider
                        SwingUtilities.invokeLater(() -> {
                            double articulation = ((double) sMsg.getData2() / 127.0) - 0.5;
                            articulationSlider.setValue((int) ((articulation * 100.0) + 100));
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_8: // switch tonal enrichment set/tonality
                        SwingUtilities.invokeLater(() -> {
                            int numberOfOptions = tonalEnrichmentPresetChooser.getItemCount();
                            int sliderValue = sMsg.getData2();
                            int choice = (sliderValue * (--numberOfOptions)) / 127;
                            tonalEnrichmentPresetChooser.setSelectedIndex(choice);
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_7: // arpeggio pattern
                        SwingUtilities.invokeLater(() -> {
                            int numberOfOptions = patternChooser.getItemCount();
                            int sliderValue = sMsg.getData2();
                            int choice = (sliderValue * (--numberOfOptions) / 127);
                            patternChooser.setSelectedIndex(choice);
                        });
                        break;
                    case EventMaker.CC_Effect_Ctrl_2_14b: // trigger arpeggio channel
                        SwingUtilities.invokeLater(() -> {
                            int choice = (sMsg.getData2() >= 64) ? Arpeggiator.ARPEGGIO_CHANNEL_PRESET : -1;
                            arpeggioChannelChooser.setSelectedItem(choice);
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_3_14b: // trigger held notes channel
                        SwingUtilities.invokeLater(() -> {
                            int choice = (sMsg.getData2() >= 64) ? Arpeggiator.HELD_NOTES_CHANNEL_PRESET : -1;
                            heldNotesChannelChooser.setSelectedItem(choice);
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_4_14b: // trigger bass channel
                        SwingUtilities.invokeLater(() -> {
                            int choice = (sMsg.getData2() >= 64) ? Arpeggiator.BASS_CHANNEL_PRESET : -1;
                            bassChannelChooser.setSelectedItem(choice);
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
     * close this receiver
     */
    @Override
    public void close() {
        this.synth.stop();
    }

    public static void updateLogGUI(final String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateLogGUI(message));
            return;
        }
        //Now edit your GUI objects
        GUI.logMessages.append(message);
    }
}
