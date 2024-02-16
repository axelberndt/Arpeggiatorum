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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.*;

/**
 * A graphical user interface for Arpeggiatorum.
 *
 * @author Axel Berndt
 */
public class GUI extends JFrame implements Receiver {
    private final Synthesizer synth = JSyn.createSynthesizer(); // this Synthesizer instance is used for scheduling
    // and audio processing
    private final int padding = 20;
    private JComboBox<Integer> inputChannelChooser;
    private JComboBox<Integer> arpeggioChannelChooser;
    private JComboBox<Integer> heldNotesChannelChooser;
    private JComboBox<Integer> bassChannelChooser;
    private JSlider tempoSlider;
    private JSlider articulationSlider;
    private JSlider tonalEnrichmentSlider;
    private JComboBox<NotePool.Pattern> patternChooser;
    private JComboBox<TonalEnrichmentChooserItem> tonalEnrichmentPresetChooser;

    private final Arpeggiator arpeggiator;
    private final ArrayList<Mic2MIDI> mic2Midi;

    /**
     * constructor
     */
    public GUI() {
        super("Arpeggiatorum");

        // Tools.printAudioDevices(); // print a list of all available audio devices
        // int audioInputDeviceID = Tools.getDeviceID("Primärer Soundaufnahmetreiber");
        // //Problem with system language
        //	 int audioInputDeviceID = Tools.getDeviceID("Primary Sound Capture Driver");
        //	 int numAudioInputChannels = this.synth.getAudioDeviceManager().getMaxInputChannels(audioInputDeviceID);

        this.synth.setRealTime(true);
        this.synth.start();
//		 this.synth.start(44100, // frame rate
//		 audioInputDeviceID, // input device ID
//		 numAudioInputChannels, // num input channels
//		 AudioDeviceManager.USE_DEFAULT_DEVICE, // output device ID
//		 0); // num output channels (here we need no output channels as we have no
        // audio output)
        //Passing a value greater than 0 for input channels will cause an error. why?
        //this.synth.start(44100,AudioDeviceManager.USE_DEFAULT_DEVICE,2,AudioDeviceManager.USE_DEFAULT_DEVICE,0);
        this.arpeggiator = new Arpeggiator(this.synth, this); // instantiate the Arpeggiator and specify this GUI as
        // receiver of outgoing MIDI messages (to monitor
        // controller movements as slider movements in the GUI)

        this.mic2Midi = new ArrayList<Mic2MIDI>();
        this.mic2Midi.add(new Mic2MIDI_JSyn(this.arpeggiator));
        this.mic2Midi.add(new Mic2MIDI_FFT(this.arpeggiator));
        this.mic2Midi.add(new Mic2MIDI_Tarsos(this.arpeggiator));
        this.mic2Midi.add(new Mic2MIDI_CQT(this.arpeggiator, false));
        // this.mic2Midi.add(new Mic2MIDI_CQT(this.arpeggiator, true));
        for (Mic2MIDI processor : mic2Midi) {
            this.synth.add(processor);
        }

        GUI.exitOnEsc(this); // close window on ESC
        // this.setResizable(false); // don't allow resizing
        this.setLocationRelativeTo(null); // set window position
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // what happens when the X is clicked
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook)); // do what has to be done on shutdown

        // execute the GUI building in the EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            // set look and feel
            try {
                //UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");\
                //Should set the correct system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                     | UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }

            // the container panel
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
                        e.printStackTrace();
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
                    e.printStackTrace();
                }
            });
            addComponentToGridBagLayout(mainPanel, layout, midiInChooser, 1, 0, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            this.inputChannelChooser = new JComboBox<>(
                    new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
            ;
            this.inputChannelChooser.addActionListener(actionEvent -> {
                if (this.inputChannelChooser.getSelectedItem() != null)
                    this.arpeggiator.setInputChannel((int) this.inputChannelChooser.getSelectedItem());
            });
            addComponentToGridBagLayout(mainPanel, layout, this.inputChannelChooser, 2, 0, 1, 1, 1.0, 1.0, this.padding,
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
                        e.printStackTrace();
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
                    e.printStackTrace();
                }
            });
            addComponentToGridBagLayout(mainPanel, layout, midiOutChooser, 1, 1, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            this.arpeggioChannelChooser = new JComboBox<>(
                    new Integer[]{-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
            this.arpeggioChannelChooser.setSelectedItem(this.arpeggiator.getArpeggioChannel());
            this.arpeggioChannelChooser.addActionListener(actionEvent -> {
                if (this.arpeggioChannelChooser.getSelectedItem() != null)
                    this.arpeggiator.setArpeggioChannel((int) this.arpeggioChannelChooser.getSelectedItem());
            });
            addComponentToGridBagLayout(mainPanel, layout, this.arpeggioChannelChooser, 2, 1, 1, 1, 1.0, 1.0,
                    this.padding, this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            JLabel arpeggioChannelLabel = new JLabel("   Arpeggio Channel");
            arpeggioChannelLabel.setHorizontalAlignment(JLabel.LEFT);
            addComponentToGridBagLayout(mainPanel, layout, arpeggioChannelLabel, 3, 1, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            this.heldNotesChannelChooser = new JComboBox<>(
                    new Integer[]{-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
            this.heldNotesChannelChooser.setSelectedItem(this.arpeggiator.getHeldNotesChannel());
            this.heldNotesChannelChooser.addActionListener(actionEvent -> {
                if (this.heldNotesChannelChooser.getSelectedItem() != null)
                    this.arpeggiator.setHeldNotesChannel((int) this.heldNotesChannelChooser.getSelectedItem());
            });
            addComponentToGridBagLayout(mainPanel, layout, this.heldNotesChannelChooser, 2, 2, 1, 1, 1.0, 1.0,
                    this.padding, this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            JLabel heldNotesChannelLabel = new JLabel("   Held Notes Channel");
            heldNotesChannelLabel.setHorizontalAlignment(JLabel.LEFT);
            addComponentToGridBagLayout(mainPanel, layout, heldNotesChannelLabel, 3, 2, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            this.bassChannelChooser = new JComboBox<>(
                    new Integer[]{-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
            this.bassChannelChooser.setSelectedItem(this.arpeggiator.getBassChannel());
            this.bassChannelChooser.addActionListener(actionEvent -> {
                if (this.bassChannelChooser.getSelectedItem() != null)
                    this.arpeggiator.setBassChannel((int) this.bassChannelChooser.getSelectedItem());
            });
            addComponentToGridBagLayout(mainPanel, layout, this.bassChannelChooser, 2, 3, 1, 1, 1.0, 1.0, this.padding,
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

            addComponentToGridBagLayout(mainPanel, layout, activateAudioInput, 3, 4, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            JComboBox<String> audioInputChooser = createAudioInChooser();
            audioInputChooser.addActionListener(actionEvent -> {
                // TODO: the following lines do not work!
                //Windows appear to be the problem, works on Mac
                this.synth.stop();

                int deviceID = Tools.getDeviceID((String) audioInputChooser.getSelectedItem());
                int deviceInputChannels = this.synth.getAudioDeviceManager().getMaxInputChannels(deviceID);
                this.synth.start(44100,
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
            JComboBox<Mic2MIDI> mic2MIDIChooser = new JComboBox<Mic2MIDI>();
            for (Mic2MIDI processor : mic2Midi) {
                mic2MIDIChooser.addItem(processor);
            }
            mic2MIDIChooser.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    if (activateAudioInput.isSelected()) {
                        if (event.getStateChange() == ItemEvent.SELECTED) {
                            ((Mic2MIDI) event.getItem()).start();
                        }
                        if (event.getStateChange() == ItemEvent.DESELECTED) {
                            ((Mic2MIDI) event.getItem()).stop();
                        }
                    }
                }
            });
            mic2MIDIChooser.setEnabled(true);
            mic2MIDIChooser.setSelectedIndex(0);
            addComponentToGridBagLayout(mainPanel, layout, mic2MIDIChooser, 2, 4, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);


            JSlider signal2noiseThreshold = new JSlider(0, 1000);
            signal2noiseThreshold.setValue(500);
            signal2noiseThreshold.setOrientation(JSlider.HORIZONTAL);
            signal2noiseThreshold.setMajorTickSpacing(500);
            signal2noiseThreshold.setMinorTickSpacing(100);
            signal2noiseThreshold.setPaintTicks(true);
            signal2noiseThreshold.setToolTipText("Signal to Noise Threashold");
            Hashtable<Integer, JLabel> signal2noiseThresholdLabel = new Hashtable<>();
            signal2noiseThresholdLabel.put(0, new JLabel("0.0"));
            signal2noiseThresholdLabel.put(500, new JLabel("Noise Threshold"));
            signal2noiseThresholdLabel.put(1000, new JLabel("1.0"));
            signal2noiseThreshold.setLabelTable(signal2noiseThresholdLabel);
            signal2noiseThreshold.setPaintLabels(true);
            signal2noiseThreshold.addChangeListener(changeEvent -> {
                double value = ((double) signal2noiseThreshold.getValue()) / signal2noiseThreshold.getMaximum();
                for (Mic2MIDI processor : mic2Midi) {
                    processor.setSignalToNoiseThreshold(value);
                }
            });
            addComponentToGridBagLayout(mainPanel, layout, signal2noiseThreshold, 4, 4, 1, 1, 1.0, 1.0, this.padding, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            activateAudioInput.addActionListener(actionEvent -> {
                if (activateAudioInput.isSelected()) {
                    ((Mic2MIDI) mic2MIDIChooser.getSelectedItem()).start();
                    ((Mic2MIDI) mic2MIDIChooser.getSelectedItem()).setSignalToNoiseThreshold(((double) signal2noiseThreshold.getValue()) / signal2noiseThreshold.getMaximum());
                } else {
                    for (Mic2MIDI processor : mic2Midi) {
                        processor.stop();
                    }
                    //Try to avoid calling panic
                    this.arpeggiator.panic();
                }
            });

            ////////////////////

            JLabel patternLabel = new JLabel("Arpeggiation Pattern   ");
            patternLabel.setHorizontalAlignment(JLabel.RIGHT);
            addComponentToGridBagLayout(mainPanel, layout, patternLabel, 0, 5, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            this.patternChooser = new JComboBox<>();
            this.patternChooser.addItem(NotePool.Pattern.up);
            this.patternChooser.addItem(NotePool.Pattern.down);
            this.patternChooser.addItem(NotePool.Pattern.up_down);
            this.patternChooser.addItem(NotePool.Pattern.random_no_repetitions);
            this.patternChooser.addItem(NotePool.Pattern.random_with_repetitions);
            this.patternChooser.setSelectedItem(this.arpeggiator.getPattern());
            this.patternChooser.addActionListener(actionEvent -> {
                this.arpeggiator.setPattern((NotePool.Pattern) this.patternChooser.getSelectedItem());
            });
            addComponentToGridBagLayout(mainPanel, layout, this.patternChooser, 1, 5, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            ////////////////////

            JLabel tempoLabel = new JLabel("Tempo in keystrokes/min   ");
            tempoLabel.setHorizontalAlignment(JLabel.RIGHT);
            addComponentToGridBagLayout(mainPanel, layout, tempoLabel, 0, 6, 1, 1, 1.0, 1.0, this.padding, this.padding,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            JLabel tempoValue = new JLabel("   500");
            tempoValue.setHorizontalAlignment(JLabel.LEFT);
            addComponentToGridBagLayout(mainPanel, layout, tempoValue, 3, 6, 1, 1, 1.0, 1.0, this.padding, this.padding,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            this.tempoSlider = new JSlider(100, 1000);
            this.tempoSlider.setValue(500);
            this.tempoSlider.setOrientation(JSlider.HORIZONTAL);
            this.tempoSlider.setMajorTickSpacing(200);
            this.tempoSlider.setMinorTickSpacing(100);
            this.tempoSlider.setPaintTicks(true);
            Hashtable<Integer, JLabel> tempoSliderLabel = new Hashtable<>();
            tempoSliderLabel.put(100, new JLabel("100"));
            tempoSliderLabel.put(1000, new JLabel("1000"));
            this.tempoSlider.setLabelTable(tempoSliderLabel);
            this.tempoSlider.setPaintLabels(true);
            this.tempoSlider.addChangeListener(changeEvent -> {
                int tempo = this.tempoSlider.getValue();
                this.arpeggiator.setTempo(tempo);
                tempoValue.setText("   " + tempo);
            });
            addComponentToGridBagLayout(mainPanel, layout, this.tempoSlider, 1, 6, 2, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            ////////////////////

            JLabel articulationLabel = new JLabel("Articulation   ");
            articulationLabel.setHorizontalAlignment(JLabel.RIGHT);
            addComponentToGridBagLayout(mainPanel, layout, articulationLabel, 0, 7, 1, 1, 1.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            this.articulationSlider = new JSlider(50, 150);
            this.articulationSlider.setValue(100);
            this.articulationSlider.setOrientation(JSlider.HORIZONTAL);
            this.articulationSlider.setMajorTickSpacing(50);
            this.articulationSlider.setMinorTickSpacing(10);
            this.articulationSlider.setPaintTicks(true);
            Hashtable<Integer, JLabel> articulationSliderLabel = new Hashtable<>();
            articulationSliderLabel.put(50, new JLabel("staccato"));
            articulationSliderLabel.put(100, new JLabel("tenuto"));
            articulationSliderLabel.put(150, new JLabel("legato"));
            this.articulationSlider.setLabelTable(articulationSliderLabel);
            this.articulationSlider.setPaintLabels(true);
            this.articulationSlider.addChangeListener(changeEvent -> {
                double articulation = ((double) (this.articulationSlider.getValue() - 100)) / 100.0;
                this.arpeggiator.setArticulation(articulation);
            });
            addComponentToGridBagLayout(mainPanel, layout, this.articulationSlider, 1, 7, 2, 1, 1.0, 1.0, this.padding,
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

            RangeSlider rangeSlider = new RangeSlider(0, 127); // the pitch range slider
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
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e2, 1, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e3, 2, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e4, 3, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e5, 4, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e6, 5, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e7, 6, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e8, 7, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e9, 8, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e10, 9, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e11, 10, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e12, 11, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e13, 12, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e14, 13, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e15, 14, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(tonalEnrichmentPanel, tonalEnrichmentLayout, e16, 15, 0, 1, 1, 1.0, 1.0, 15, 0,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);
            addComponentToGridBagLayout(mainPanel, layout, tonalEnrichmentPanel, 1, 10, 2, 1, 100.0, 1.0, this.padding,
                    this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            tonalEnrichmentButton.addActionListener(actionEvent -> {
                int[] intervals = new int[]{
                        (e1.getValue() instanceof Long) ? ((Long) e1.getValue()).intValue() : (Integer) e1.getValue(), // it
                        // can
                        // be
                        // of
                        // type
                        // Long
                        // or
                        // Integer
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

            this.tonalEnrichmentPresetChooser = new JComboBox<>();
            this.tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Empty",
                    new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
            this.tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Octaves",
                    new int[]{0, 12, 24, 36, 48, 60, 72, 84, 96, 108, 120, 0, 0, 0, 0, 0}));
            this.tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Octaves and Fifths",
                    new int[]{0, 7, 12, 19, 24, 31, 36, 43, 48, 55, 60, 67, 72, 79, 84, 91}));
            this.tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Major Triad",
                    new int[]{0, 4, 7, 12, 16, 19, 24, 28, 31, 36, 40, 43, 48, 52, 55, 60}));
            this.tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Minor Triad",
                    new int[]{0, 3, 7, 12, 15, 19, 24, 27, 31, 36, 39, 43, 48, 51, 55, 60}));
            this.tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Major Thirds",
                    new int[]{0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52, 56, 60}));
            this.tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Forths",
                    new int[]{0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75}));
            this.tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Series of Overtones",
                    new int[]{0, 12, 19, 24, 28, 31, 34, 36, 38, 40, 42, 43, 44, 46, 47, 48}));
            this.tonalEnrichmentPresetChooser.addItem(new TonalEnrichmentChooserItem("Overtones Transposed Down",
                    new int[]{0, 12, 24, 7, 19, 4, 16, 10, 22, 2, 14, 6, 18, 8, 20, 11}));
            this.tonalEnrichmentPresetChooser.addActionListener(actionEvent -> {
                if (this.tonalEnrichmentPresetChooser.getSelectedItem() == null)
                    return;
                int[] intervals = ((TonalEnrichmentChooserItem) this.tonalEnrichmentPresetChooser.getSelectedItem())
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
            addComponentToGridBagLayout(mainPanel, layout, this.tonalEnrichmentPresetChooser, 1, 9, 1, 1, 1.0, 1.0,
                    this.padding, this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            this.tonalEnrichmentSlider = new JSlider(0, 100);
            this.tonalEnrichmentSlider.setValue(0);
            this.tonalEnrichmentSlider.setOrientation(JSlider.HORIZONTAL);
            this.tonalEnrichmentSlider.setMajorTickSpacing(50);
            this.tonalEnrichmentSlider.setMinorTickSpacing(10);
            this.tonalEnrichmentSlider.setPaintTicks(true);
            Hashtable<Integer, JLabel> tonalEnrichmentSliderLabel = new Hashtable<>();
            tonalEnrichmentSliderLabel.put(0, new JLabel("0%"));
            tonalEnrichmentSliderLabel.put(50, new JLabel("50%"));
            tonalEnrichmentSliderLabel.put(100, new JLabel("100%"));
            this.tonalEnrichmentSlider.setLabelTable(tonalEnrichmentSliderLabel);
            this.tonalEnrichmentSlider.setPaintLabels(true);
            this.tonalEnrichmentSlider.addChangeListener(changeEvent -> {
                float tonalEnrichmentAmount = (float) (this.tonalEnrichmentSlider.getValue()) / 100.0f;
                this.arpeggiator.setTonalEnrichmentAmount(tonalEnrichmentAmount);
            });
            addComponentToGridBagLayout(mainPanel, layout, this.tonalEnrichmentSlider, 1, 11, 2, 1, 1.0, 1.0,
                    this.padding, this.padding, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            ////////////////////

            JButton panic = new JButton("Panic");
            panic.addActionListener(actionEvent -> {
                this.arpeggiator.panic();
            });
            addComponentToGridBagLayout(mainPanel, layout, panic, 0, 12, 4, 1, 1.0, 1.0, this.padding, this.padding,
                    GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

            ////////////////////

            // pack it all together and show it
            this.pack();
            this.setVisible(true);
        });
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
                frame.dispose(); // close the window (if this is the only window, this will terminate the JVM)
                System.exit(0); // the program may still run, enforce exit
            }
        });
    }

    /**
     * Do all that must be done before shutdown.
     */
    private void shutdownHook() {
        // AllNotesOff? Anything?
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
     */
    public static void addComponentToGridBagLayout(Container container, GridBagLayout gridBagLayout,
                                                   Component component, int x, int y, int width, int height, double weightx, double weighty, int ipadx,
                                                   int ipady, int fill, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = fill;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
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
            // get the corresponding device
            MidiDevice device;
            try {
                device = MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException e) {
                continue;
            }

            // the device should be a MIDI port with receiver or a synthesizer (Gervill)
            if (!(device instanceof Synthesizer) && (device.getMaxTransmitters() != 0)) {
                try {
                    midiPortChooser.addItem(new MidiDeviceChooserItem(info));
                } catch (MidiUnavailableException e) {
                    e.printStackTrace();
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
            // get the corresponding device
            MidiDevice device;
            try {
                device = MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException e) {
                continue;
            }

            // the device should be a MIDI port with receiver or a synthesizer (Gervill)
            if (!(device instanceof Sequencer) && (device.getMaxReceivers() != 0)) {
                try {
                    midiPortChooser.addItem(new MidiDeviceChooserItem(info));
                } catch (MidiUnavailableException e) {
                    e.printStackTrace();
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
        if (!(message instanceof ShortMessage))
            return;

        if ((this.inputChannelChooser.getSelectedItem() == null)
                || (((ShortMessage) message).getChannel() != (int) this.inputChannelChooser.getSelectedItem()))
            return;

        ShortMessage sMsg = (ShortMessage) message;
        switch (message.getStatus() & 0xF0) {
            case EventMaker.CONTROL_CHANGE:
                switch (sMsg.getData1()) {
                    case EventMaker.CC_General_Purpose_Ctrl_1: // tonal enrichment slider
                        SwingUtilities.invokeLater(() -> {
                            float tonalEnrichmentAmount = ((float) sMsg.getData2()) / 127f;
                            this.tonalEnrichmentSlider.setValue((int) (tonalEnrichmentAmount * 100));
                        });
                        break;
                    case EventMaker.CC_General_Purpose_Ctrl_2: // tempo slider
                        SwingUtilities.invokeLater(() -> {
                            double tempo = ((900.0 * sMsg.getData2()) / 127.0) + 100.0;
                            this.tempoSlider.setValue((int) tempo);
                        });
                        break;
                    case EventMaker.CC_General_Purpose_Ctrl_3: // articulation slider
                        SwingUtilities.invokeLater(() -> {
                            double articulation = ((double) sMsg.getData2() / 127.0) - 0.5;
                            this.articulationSlider.setValue((int) ((articulation * 100.0) + 100));
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_8: // switch tonal enrichment set/tonality
                        SwingUtilities.invokeLater(() -> {
                            int numberOfOptions = this.tonalEnrichmentPresetChooser.getItemCount();
                            int sliderValue = sMsg.getData2();
                            int choice = (sliderValue * (--numberOfOptions)) / 127;
                            this.tonalEnrichmentPresetChooser.setSelectedIndex(choice);
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_7: // arpeggio pattern
                        SwingUtilities.invokeLater(() -> {
                            int numberOfOptions = this.patternChooser.getItemCount();
                            int sliderValue = sMsg.getData2();
                            int choice = (sliderValue * (--numberOfOptions) / 127);
                            this.patternChooser.setSelectedIndex(choice);
                        });
                        break;
                    case EventMaker.CC_Effect_Ctrl_2_14b: // trigger arpeggio channel
                        SwingUtilities.invokeLater(() -> {
                            int choice = (sMsg.getData2() >= 64) ? Arpeggiator.ARPEGGIO_CHANNEL_PRESET : -1;
                            this.arpeggioChannelChooser.setSelectedItem(choice);
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_3_14b: // trigger held notes channel
                        SwingUtilities.invokeLater(() -> {
                            int choice = (sMsg.getData2() >= 64) ? Arpeggiator.HELD_NOTES_CHANNEL_PRESET : -1;
                            this.heldNotesChannelChooser.setSelectedItem(choice);
                        });
                        break;
                    case EventMaker.CC_Undefined_Ctrl_4_14b: // trigger bass channel
                        SwingUtilities.invokeLater(() -> {
                            int choice = (sMsg.getData2() >= 64) ? Arpeggiator.BASS_CHANNEL_PRESET : -1;
                            this.bassChannelChooser.setSelectedItem(choice);
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
}
