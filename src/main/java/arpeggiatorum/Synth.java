//package arpeggiatorum;
//
//import arpeggiatorum.gui.GUI;
//import com.jsyn.JSyn;
//import com.jsyn.Synthesizer;
//import com.jsyn.instruments.DualOscillatorSynthVoice;
//import com.jsyn.midi.MidiSynthesizer;
//import com.jsyn.unitgen.LineOut;
//import com.jsyn.util.MultiChannelSynthesizer;
//import com.jsyn.util.VoiceDescription;
//
//import javax.sound.midi.*;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Scanner;
//
///**
// * A little test synthesizer.
// */
//public class Synth {
//    private static final int NUM_CHANNELS = 16;
//    private static final int VOICES_PER_CHANNEL = 3;
//    private Synthesizer synth;
//    private LineOut lineOut;
//    private MidiSynthesizer midiSynthesizer;
//    private VoiceDescription voiceDescription;
//    private MultiChannelSynthesizer multiSynth;
//
//    public static void run() {
//        Synth app = new Synth();
//        try {
//            app.test();
//        } catch (MidiUnavailableException e) {
//            //e.printStackTrace();
//LogGUIController.logBuffer.append(e.getMessage());
//        } catch (IOException e) {
//            //e.printStackTrace();
//LogGUIController.logBuffer.append(e.getMessage());
//        } catch (InterruptedException e) {
//            //e.printStackTrace();
//LogGUIController.logBuffer.append(e.getMessage());
//        }
//    }
//
//
//    private int test() throws MidiUnavailableException, IOException, InterruptedException {
//        setupSynth();
//
//        int result = 2;
//
//        ArrayList<MidiDevice> deviceMap = new ArrayList<>();
//        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
//            MidiDevice device = MidiSystem.getMidiDevice(info);
//            if (device.getMaxReceivers() != 0)
//                continue;
//           // System.out.println(deviceMap.size() + " " + info.getName());
//            LogGUIController.logBuffer.append(deviceMap.size() + " " + info.getName()+"\r\n");
//            deviceMap.add(device);
//        }
////        System.out.println("\nChoose your input device: ");
//        LogGUIController.logBuffer.append("\nChoose your input device: \r\n");
//        Scanner in = new Scanner(System.in);
//        MidiDevice keyboard = deviceMap.get(in.nextInt());
//
////        MidiDevice keyboard = MidiDeviceTools.findKeyboard();
//        Receiver receiver = new CustomReceiver();
//        // Just use default synthesizer.
//        if (keyboard != null) {
//            // If you forget to open them you will hear no sound.
//            keyboard.open();
//            // Put the receiver in the transmitter.
//            // This gives fairly low latency playing.
//            keyboard.getTransmitter().setReceiver(receiver);
////            System.out.println("Play MIDI keyboard: " + keyboard.getDeviceInfo().getName());
//            LogGUIController.logBuffer.append("Play MIDI keyboard: " + keyboard.getDeviceInfo().getName()+"\r\n");
//            result = 0;
//        } else {
////            System.out.println("Could not find a keyboard.");
//            LogGUIController.logBuffer.append("Could not find a keyboard.\r\n");
//        }
//        return result;
//    }
//
//
//    private void setupSynth() {
//        synth = JSyn.createSynthesizer();
//
//        voiceDescription = DualOscillatorSynthVoice.getVoiceDescription();
////        voiceDescription = SubtractiveSynthVoice.getVoiceDescription();
//
//        multiSynth = new MultiChannelSynthesizer();
//        final int startChannel = 0;
//        multiSynth.setup(synth, startChannel, NUM_CHANNELS, VOICES_PER_CHANNEL, voiceDescription);
//        midiSynthesizer = new MidiSynthesizer(multiSynth);
//
//        // Create a LineOut for the entire synthesizer.
//        synth.add(lineOut = new LineOut());
//        multiSynth.getOutput().connect(0,lineOut.input, 0);
//        multiSynth.getOutput().connect(1,lineOut.input, 1);
//
//        // Start synthesizer using default stereo output at 44100 Hz.
//        synth.start();
//        lineOut.start();
//    }
//
//    // Write a Receiver to get the messages from a Transmitter.
//    class CustomReceiver implements Receiver {
//        @Override
//        public void close() {
////            System.out.print("Closed.");
//            LogGUIController.logBuffer.append("Closed.");
//        }
//
//        @Override
//        public void send(MidiMessage message, long timeStamp) {
//            byte[] bytes = message.getMessage();
//            midiSynthesizer.onReceive(bytes, 0, bytes.length);
//        }
//    }
//}
