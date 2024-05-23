package arpeggiatorum.supplementary;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import java.io.IOException;
import java.math.BigInteger;

public class EventMaker {
    public static final short NOTE_OFF = 128;
    public static final short NOTE_ON = 144;
    public static final short POLY_AFTERTOUCH = 160;
    public static final short CONTROL_CHANGE = 176;
    public static final short PROGRAM_CHANGE = 192;
    public static final short CHANNEL_AFTERTOUCH = 208;
    public static final short PITCH_BEND = 224;
    public static final short SYSEX_START = 240;
    public static final short MIDI_TIME_CODE = 241;
    public static final short SONG_POSITION_POINTER = 242;
    public static final short SONG_SELECT = 243;
    public static final short UNDEF1 = 244;
    public static final short UNDEF2 = 245;
    public static final short TUNE_REQUEST = 246;
    public static final short SYSEX_END = 247;
    public static final short TIMING_CLOCK = 248;
    public static final short UNDEF3 = 249;
    public static final short START = 250;
    public static final short CONTINUE = 251;
    public static final short STOP = 252;
    public static final short UNDEF4 = 253;
    public static final short ACTIVE_SENSING = 254;
    public static final short SYSTEM_RESET = 255;
    public static final short META_EVENT = 255;
    public static final short CC_Bank_Select = 0;
    public static final short CC_Modulation_Wheel = 1;
    public static final short CC_Breath_Ctrl = 2;
    public static final short CC_Undefined_Ctrl_1 = 3;
    public static final short CC_Foot_Ctrl = 4;
    public static final short CC_Portamento_Time = 5;
    public static final short CC_Data_Entry = 6;
    public static final short CC_Channel_Volume = 7;
    public static final short CC_Balance = 8;
    public static final short CC_Undefined_Ctrl_2 = 9;
    public static final short CC_Pan = 10;
    public static final short CC_Expression_Ctrl = 11;
    public static final short CC_Effect_Ctrl_1 = 12;
    public static final short CC_Effect_Ctrl_2 = 13;
    public static final short CC_Undefined_Ctrl_3 = 14;
    public static final short CC_Undefined_Ctrl_4 = 15;
    public static final short CC_General_Purpose_Ctrl_1 = 16;
    public static final short CC_General_Purpose_Ctrl_2 = 17;
    public static final short CC_General_Purpose_Ctrl_3 = 18;
    public static final short CC_General_Purpose_Ctrl_4 = 19;
    public static final short CC_Undefined_Ctrl_5 = 20;
    public static final short CC_Undefined_Ctrl_6 = 21;
    public static final short CC_Undefined_Ctrl_7 = 22;
    public static final short CC_Undefined_Ctrl_8 = 23;
    public static final short CC_Undefined_Ctrl_9 = 24;
    public static final short CC_Undefined_Ctrl_10 = 25;
    public static final short CC_Undefined_Ctrl_11 = 26;
    public static final short CC_Undefined_Ctrl_12 = 27;
    public static final short CC_Undefined_Ctrl_13 = 28;
    public static final short CC_Undefined_Ctrl_14 = 29;
    public static final short CC_Undefined_Ctrl_15 = 30;
    public static final short CC_Undefined_Ctrl_16 = 31;
    public static final short CC_Bank_Select_14b = 32;
    public static final short CC_Modulation_Wheel_14b = 33;
    public static final short CC_Breath_Ctrl_14b = 34;
    public static final short CC_Undefined_Ctrl_1_14b = 35;
    public static final short CC_Foot_Ctrl_14b = 36;
    public static final short CC_Portamento_Time_14b = 37;
    public static final short CC_Data_Entry_14b = 38;
    public static final short CC_Channel_Volume_14b = 39;
    public static final short CC_Balance_14b = 40;
    public static final short CC_Undefined_Ctrl_2_14b = 41;
    public static final short CC_Pan_14b = 42;
    public static final short CC_Expression_Ctrl_14b = 43;
    public static final short CC_Effect_Ctrl_1_14b = 44;
    public static final short CC_Effect_Ctrl_2_14b = 45;
    public static final short CC_Undefined_Ctrl_3_14b = 46;
    public static final short CC_Undefined_Ctrl_4_14b = 47;
    public static final short CC_General_Purpose_Ctrl_1_14b = 48;
    public static final short CC_General_Purpose_Ctrl_2_14b = 49;
    public static final short CC_General_Purpose_Ctrl_3_14b = 50;
    public static final short CC_General_Purpose_Ctrl_4_14b = 51;
    public static final short CC_Undefined_Ctrl_5_14b = 52;
    public static final short CC_Undefined_Ctrl_6_14b = 53;
    public static final short CC_Undefined_Ctrl_7_14b = 54;
    public static final short CC_Undefined_Ctrl_8_14b = 55;
    public static final short CC_Undefined_Ctrl_9_14b = 56;
    public static final short CC_Undefined_Ctrl_10_14b = 57;
    public static final short CC_Undefined_Ctrl_11_14b = 58;
    public static final short CC_Undefined_Ctrl_12_14b = 59;
    public static final short CC_Undefined_Ctrl_13_14b = 60;
    public static final short CC_Undefined_Ctrl_14_14b = 61;
    public static final short CC_Undefined_Ctrl_15_14b = 62;
    public static final short CC_Undefined_Ctrl_16_14b = 63;
    public static final short CC_Damper_Pedal = 64;
    public static final short CC_Portamento_OnOff = 65;
    public static final short CC_Sustenuto = 66;
    public static final short CC_Soft_Pedal = 67;
    public static final short CC_Legato_Footswitch = 68;
    public static final short CC_Hold_2 = 69;
    public static final short CC_Sound_Ctrl_1 = 70;
    public static final short CC_Sound_Ctrl_2 = 71;
    public static final short CC_Sound_Ctrl_3 = 72;
    public static final short CC_Sound_Ctrl_4 = 73;
    public static final short CC_Sound_Ctrl_5 = 74;
    public static final short CC_Sound_Ctrl_6 = 75;
    public static final short CC_Sound_Ctrl_7 = 76;
    public static final short CC_Sound_Ctrl_8 = 77;
    public static final short CC_Sound_Ctrl_9 = 78;
    public static final short CC_Sound_Ctrl_10 = 79;
    public static final short CC_General_Purpose_Ctrl_5 = 80;
    public static final short CC_General_Purpose_Ctrl_6 = 81;
    public static final short CC_General_Purpose_Ctrl_7 = 82;
    public static final short CC_General_Purpose_Ctrl_8 = 83;
    public static final short CC_Portamento_Ctrl = 84;
    public static final short CC_Undefined_Ctrl_17 = 85;
    public static final short CC_Undefined_Ctrl_18 = 86;
    public static final short CC_Undefined_Ctrl_19 = 87;
    public static final short CC_Undefined_Ctrl_20 = 88;
    public static final short CC_Undefined_Ctrl_21 = 89;
    public static final short CC_Undefined_Ctrl_22 = 90;
    public static final short CC_Reverb_Send_Level = 91;
    public static final short CC_Effects_2_Depth = 92;
    public static final short CC_Chorus_Send_Level = 93;
    public static final short CC_Effects_4_Depth = 94;
    public static final short CC_Effects_5_Depth = 95;
    public static final short CC_Data_Entry_plus_1 = 96;
    public static final short CC_Data_Entry_minus_1 = 97;
    public static final short CC_Nonregistered_Param_Num_LSB = 98;
    public static final short CC_Nonregistered_Param_Num_MSB = 99;
    public static final short CC_Registered_Param_Num_LSB = 100;
    public static final short CC_Registered_Param_Num_MSB = 101;
    public static final short CC_Undefined_Ctrl_23 = 102;
    public static final short CC_Undefined_Ctrl_24 = 103;
    public static final short CC_Undefined_Ctrl_25 = 104;
    public static final short CC_Undefined_Ctrl_26 = 105;
    public static final short CC_Undefined_Ctrl_27 = 106;
    public static final short CC_Undefined_Ctrl_28 = 107;
    public static final short CC_Undefined_Ctrl_29 = 108;
    public static final short CC_Undefined_Ctrl_30 = 109;
    public static final short CC_Undefined_Ctrl_31 = 110;
    public static final short CC_Undefined_Ctrl_32 = 111;
    public static final short CC_Undefined_Ctrl_33 = 112;
    public static final short CC_Undefined_Ctrl_34 = 113;
    public static final short CC_Undefined_Ctrl_35 = 114;
    public static final short CC_Undefined_Ctrl_36 = 115;
    public static final short CC_Undefined_Ctrl_37 = 116;
    public static final short CC_Undefined_Ctrl_38 = 117;
    public static final short CC_Undefined_Ctrl_39 = 118;
    public static final short CC_Undefined_Ctrl_40 = 119;
    public static final short CC_All_Sound_Off = 120;
    public static final short CC_Reset_All_Controllers = 121;
    public static final short CC_Local_Control_OnOff = 122;
    public static final short CC_All_Notes_Off = 123;
    public static final short CC_Omni_Mode_Off = 124;
    public static final short CC_Omni_Mode_On = 125;
    public static final short CC_Poly_Mode_OnOff = 126;
    public static final short CC_Poly_Mode_On = 127;
    public static final short META_Sequence_Number = 0;
    public static final short META_Text_Event = 1;
    public static final short META_Copyright_Notice = 2;
    public static final short META_Track_Name = 3;
    public static final short META_Sequence_Name = 3;
    public static final short META_Instrument_Name = 4;
    public static final short META_Lyric = 5;
    public static final short META_Marker = 6;
    public static final short META_Cue_Point = 7;
    public static final short META_Program_Name = 8;
    public static final short META_Device_Name = 9;
    public static final short META_Midi_Channel_Prefix = 32;
    public static final short META_Midi_Port = 33;
    public static final short META_End_of_Track = 47;
    public static final short META_Set_Tempo = 81;
    public static final short META_SMTPE_Offset = 84;
    public static final short META_Time_Signature = 88;
    public static final short META_Key_Signature = 89;
    public static final short META_Sequence_specific_Meta_event = 127;
    public static final short PC_Acoustic_Grand_Piano = 0;
    public static final short PC_Bright_Acoustic_Piano = 1;
    public static final short PC_Electric_Grand_Piano = 2;
    public static final short PC_Honkytonk_Piano = 3;
    public static final short PC_Electric_Piano_1 = 4;
    public static final short PC_Electric_Piano_2 = 5;
    public static final short PC_Harpsichord = 6;
    public static final short PC_Clavinet = 7;
    public static final short PC_Celesta = 8;
    public static final short PC_Glockenspiel = 9;
    public static final short PC_Music_Box = 10;
    public static final short PC_Vibraphone = 11;
    public static final short PC_Marimba = 12;
    public static final short PC_Xylophone = 13;
    public static final short PC_Tubular_Bells = 14;
    public static final short PC_Dulcimer = 15;
    public static final short PC_Drawbar_Organ = 16;
    public static final short PC_Percussive_Organ = 17;
    public static final short PC_Rock_Organ = 18;
    public static final short PC_Church_Organ = 19;
    public static final short PC_Reed_Organ = 20;
    public static final short PC_Accordion = 21;
    public static final short PC_Harmonica = 22;
    public static final short PC_Tango_Accordion = 23;
    public static final short PC_Acoustic_Guitar_nylon = 24;
    public static final short PC_Acoustic_Guitar_steel = 25;
    public static final short PC_Electric_Guitar_jazz = 26;
    public static final short PC_Electric_Guitar_clean = 27;
    public static final short PC_Electric_Guitar_muted = 28;
    public static final short PC_Overdriven_Guitar = 29;
    public static final short PC_Distortion_Guitar = 30;
    public static final short PC_Guitar_Harmonics = 31;
    public static final short PC_Acoustic_Bass = 32;
    public static final short PC_Electric_Bass_finger = 33;
    public static final short PC_Electric_Bass_pick = 34;
    public static final short PC_Fretless_Bass = 35;
    public static final short PC_Slap_Bass_1 = 36;
    public static final short PC_Slap_Bass_2 = 37;
    public static final short PC_Synth_Bass_1 = 38;
    public static final short PC_Synth_Bass_2 = 39;
    public static final short PC_Violin = 40;
    public static final short PC_Viola = 41;
    public static final short PC_Cello = 42;
    public static final short PC_Contrabass = 43;
    public static final short PC_Tremolo_Strings = 44;
    public static final short PC_Pizzicato_Strings = 45;
    public static final short PC_Orchestral_Harp = 46;
    public static final short PC_Timpani = 47;
    public static final short PC_String_Ensemble_1 = 48;
    public static final short PC_String_Ensemble_2 = 49;
    public static final short PC_Synth_Strings_1 = 50;
    public static final short PC_Synth_Strings_2 = 51;
    public static final short PC_Choir_Aahs = 52;
    public static final short PC_Voice_Oohs = 53;
    public static final short PC_Synth_Choir = 54;
    public static final short PC_Orchestra_Hit = 55;
    public static final short PC_Trumpet = 56;
    public static final short PC_Trombone = 57;
    public static final short PC_Tuba = 58;
    public static final short PC_Muted_Trumpet = 59;
    public static final short PC_French_Horn = 60;
    public static final short PC_Brass_Section = 61;
    public static final short PC_Synth_Brass_1 = 62;
    public static final short PC_Synth_Brass_2 = 63;
    public static final short PC_Soprano_Sax = 64;
    public static final short PC_Alto_Sax = 65;
    public static final short PC_Tenor_Sax = 66;
    public static final short PC_Baritone_Sax = 67;
    public static final short PC_Oboe = 68;
    public static final short PC_English_Horn = 69;
    public static final short PC_Bassoon = 70;
    public static final short PC_Clarinet = 71;
    public static final short PC_Piccolo = 72;
    public static final short PC_Flute = 73;
    public static final short PC_Recorder = 74;
    public static final short PC_Pan_Flute = 75;
    public static final short PC_Blown_bottle = 76;
    public static final short PC_Shakuhachi = 77;
    public static final short PC_Whistle = 78;
    public static final short PC_Ocarina = 79;
    public static final short PC_Lead_1_square = 80;
    public static final short PC_Lead_2_sawtooth = 81;
    public static final short PC_Lead_3_calliope = 82;
    public static final short PC_Lead_4_chiff = 83;
    public static final short PC_Lead_5_charang = 84;
    public static final short PC_Lead_6_voice = 85;
    public static final short PC_Lead_7_fifths = 86;
    public static final short PC_Lead_8_bass_plus_lead = 87;
    public static final short PC_Pad_1_new_age = 88;
    public static final short PC_Pad_2_warm = 89;
    public static final short PC_Pad_3_polysynth = 90;
    public static final short PC_Pad_4_choir = 91;
    public static final short PC_Pad_5_bowed = 92;
    public static final short PC_Pad_6_metallic = 93;
    public static final short PC_Pad_7_halo = 94;
    public static final short PC_Pad_8_sweep = 95;
    public static final short PC_FX_1_rain = 96;
    public static final short PC_FX_2_soundtrack = 97;
    public static final short PC_FX_3_crystal = 98;
    public static final short PC_FX_4_atmosphere = 99;
    public static final short PC_FX_5_brightness = 100;
    public static final short PC_FX_6_goblins = 101;
    public static final short PC_FX_7_echoes = 102;
    public static final short PC_FX_8_scifi = 103;
    public static final short PC_Sitar = 104;
    public static final short PC_Banjo = 105;
    public static final short PC_Shamisen = 106;
    public static final short PC_Koto = 107;
    public static final short PC_Kalimba = 108;
    public static final short PC_Bagpipe = 109;
    public static final short PC_Fiddle = 110;
    public static final short PC_Shanai = 111;
    public static final short PC_Tinkle_Bell = 112;
    public static final short PC_Agogo = 113;
    public static final short PC_Steel_Drums = 114;
    public static final short PC_Woodblock = 115;
    public static final short PC_Taiko_Drum = 116;
    public static final short PC_Melodic_Tom = 117;
    public static final short PC_Synth_Drum = 118;
    public static final short PC_Reverse_Cymbal = 119;
    public static final short PC_Guitar_Fret_Noise = 120;
    public static final short PC_Breath_Noise = 121;
    public static final short PC_Seashore = 122;
    public static final short PC_Bird_Tweet = 123;
    public static final short PC_Telephone_Ring = 124;
    public static final short PC_Helicopter = 125;
    public static final short PC_Applause = 126;
    public static final short PC_Gunshot = 127;
    private static final byte TICKS_PER_METER_CLICK = 24;
    private static final byte THIRTY_SECOND_NOTES_PER_QUARTER = 8;

    public EventMaker() {
    }

    public static byte[] intToByteArray(int value, boolean isBigEndian) {
        byte[] byteArray;
        if (isBigEndian) {
            byteArray = new byte[]{(byte)value, (byte)(value >>> 8), (byte)(value >>> 16), (byte)(value >>> 24)};
        } else {
            byteArray = new byte[]{(byte)(value >>> 24), (byte)(value >>> 16), (byte)(value >>> 8), (byte)value};
        }

        return byteArray;
    }

    public static int byteArrayToInt(byte[] bytes) {
        return (new BigInteger(bytes)).intValue();
    }

    public static byte[] shortToByteArray(short value) {
        return new byte[]{(byte)value};
    }

    public static short byteToShort(byte b) {
        return (short)b;
    }

    public static MidiEvent createNoteOff(int chan, long date, int pitch, int vel) {
        if (vel > 127) {
            vel = 127;
        } else if (vel < 0) {
            vel = 0;
        }

        try {
            MidiEvent e = new MidiEvent(new ShortMessage(128, chan, pitch, vel), date);
            return e;
        } catch (InvalidMidiDataException var7) {
            InvalidMidiDataException e1 = var7;
            e1.printStackTrace();
            return null;
        }
    }

    public static MidiEvent createNoteOn(int chan, long date, int pitch, int vel) {
        if (vel > 127) {
            vel = 127;
        } else if (vel < 0) {
            vel = 0;
        }

        try {
            MidiEvent e = new MidiEvent(new ShortMessage(144, chan, pitch, vel), date);
            return e;
        } catch (InvalidMidiDataException var7) {
            InvalidMidiDataException e1 = var7;
            e1.printStackTrace();
            return null;
        }
    }

    public static MidiEvent createProgramChange(int chan, long date, String name) {
        InstrumentsDictionary dict;
        try {
            dict = new InstrumentsDictionary();
        } catch (NullPointerException | IOException var6) {
            return createProgramChange(chan, date, (short)0);
        }

        return createProgramChange(chan, date, dict.getProgramChange(name));
    }

    public static MidiEvent createProgramChange(int chan, long date, short programNumber) {
        try {
            return new MidiEvent(new ShortMessage(192, chan, programNumber, 0), date);
        } catch (InvalidMidiDataException var5) {
            InvalidMidiDataException e = var5;
            e.printStackTrace();
            return null;
        }
    }

    public static MidiEvent createControlChange(int chan, long date, int controllerNumber, int controllerValue) {
        if (controllerValue > 127) {
            controllerValue = 127;
        } else if (controllerValue < 0) {
            controllerValue = 0;
        }

        try {
            return new MidiEvent(new ShortMessage(176, chan, controllerNumber, controllerValue), date);
        } catch (InvalidMidiDataException var6) {
            InvalidMidiDataException e = var6;
            e.printStackTrace();
            return null;
        }
    }

    public static MidiEvent createKeySignature(long date, int accids) {
        try {
            return new MidiEvent(new MetaMessage(89, new byte[]{(byte)accids, 0}, 2), date);
        } catch (InvalidMidiDataException var4) {
            InvalidMidiDataException e = var4;
            e.printStackTrace();
            return null;
        }
    }

    public static MidiEvent createTimeSignature(long date, int numerator, int denominator) {
        int p;
        for(p = 1; Math.pow(2.0, (double)p) < (double)denominator; ++p) {
        }

        byte denom = (byte)p;

        try {
            return new MidiEvent(new MetaMessage(88, new byte[]{(byte)numerator, denom, 24, 8}, 4), date);
        } catch (InvalidMidiDataException var7) {
            InvalidMidiDataException e = var7;
            e.printStackTrace();
            return null;
        }
    }

    public static MidiEvent createTempo(long date, double bpm, double beatlength) {
        int mpq = (int)(6.0E7 / (bpm * beatlength * 4.0));
        byte[] tempo = intToByteArray(mpq, false);

        try {
            return new MidiEvent(new MetaMessage(81, new byte[]{tempo[1], tempo[2], tempo[3]}, 3), date);
        } catch (InvalidMidiDataException var9) {
            InvalidMidiDataException e1 = var9;
            e1.printStackTrace();
            return null;
        }
    }

    public static MidiEvent createTrackName(long date, String name) {
        byte[] text = name.getBytes();

        try {
            return new MidiEvent(new MetaMessage(3, text, text.length), date);
        } catch (InvalidMidiDataException var5) {
            InvalidMidiDataException e = var5;
            e.printStackTrace();
            return null;
        }
    }

    public static MidiEvent createInstrumentName(long date, String name) {
        byte[] text = name.getBytes();

        try {
            return new MidiEvent(new MetaMessage(4, text, text.length), date);
        } catch (InvalidMidiDataException var5) {
            InvalidMidiDataException e = var5;
            e.printStackTrace();
            return null;
        }
    }

    public static MidiEvent createMarker(long date, String markerText) {
        byte[] text = markerText.getBytes();

        try {
            return new MidiEvent(new MetaMessage(6, text, text.length), date);
        } catch (InvalidMidiDataException var5) {
            InvalidMidiDataException e = var5;
            e.printStackTrace();
            return null;
        }
    }

    public static MidiEvent createChannelPrefix(long date, short channel) {
        try {
            return new MidiEvent(new MetaMessage(32, shortToByteArray(channel), 1), date);
        } catch (InvalidMidiDataException var4) {
            InvalidMidiDataException e = var4;
            e.printStackTrace();
            return null;
        }
    }

    public static MidiEvent createMidiPortEvent(long date, short port) {
        try {
            return new MidiEvent(new MetaMessage(33, shortToByteArray(port), 1), date);
        } catch (InvalidMidiDataException var4) {
            InvalidMidiDataException e = var4;
            e.printStackTrace();
            return null;
        }
    }
}
