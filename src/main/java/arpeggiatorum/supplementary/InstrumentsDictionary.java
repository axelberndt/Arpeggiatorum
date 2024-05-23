package arpeggiatorum.supplementary;

//import info.debatty.java.stringsimilarity.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class InstrumentsDictionary {
    public static final byte Levenshtein = 0;
    public static final byte NormalizedLevenshtein = 1;
    public static final byte Damerau = 2;
    public static final byte JaroWinkler = 3;
    public static final byte LongestCommonSubsequence = 4;
    public static final byte MetricLCS = 5;
    public static final byte NGram = 6;
    public static final byte QGram = 7;
    public static final byte Cosine = 8;
    public static final byte Jaccard = 9;
    public static final byte SorensenDice = 10;
    public static final String[] DefaultNames = new String[]{"Acoustic Grand Piano", "Bright Acoustic Piano", "Electric Grand Piano", "Honkytonk Piano", "Electric Piano 1", "Electric Piano 2", "Harpsichord", "Clavinet", "Celesta", "Glockenspiel", "Music Box", "Vibraphone", "Marimba", "Xylophone", "Tubular Bells", "Dulcimer", "Drawbar Organ", "Percussive Organ", "Rock Organ", "Church Organ", "Reed Organ", "Accordion", "Harmonica", "Tango Accordion", "Acoustic Nylon Guitar", "Acoustic Steel Guitar", "Electric Jazz Guitar", "Electric Clean Guitar", "Electric Muted Guitar", "Overdriven Guitar", "Distorted Guitar", "Harmonic Guitar", "Acoustic Bass", "Fingered Electric Bass", "Picked Electric Bass", "Fretless Bass", "Slap Bass 1", "Slap Bass 2", "Synth Bass 1", "Synth Bass 2", "Violin", "Viola", "Cello", "Contrabass", "Tremolo Strings", "Pizzicato Strings", "Orchestral Harp", "Timpani", "String Ensemble 1", "String Ensemble 2", "Synth Strings 1", "Synth Strings 2", "Choir Aahs", "Voice Oohs", "Synth Choir", "Orchestra Hit", "Trumpet", "Trombone", "Tuba", "Muted Trumpet", "French Horn", "Brass Section", "Synth Brass 1", "Synth Brass 2", "Soprano Sax", "Alto Sax", "Tenor Sax", "Baritone Sax", "Oboe", "English Horn", "Bassoon", "Clarinet", "Piccolo", "Flute", "Recorder", "Pan Flute", "Blown Bottle", "Shakuhachi", "Whistle", "Ocarina", "Lead 1 Square", "Lead 2 Sawtooth", "Lead 3 Calliope", "Lead 4 Chiff", "Lead 5 Charang", "Lead 6 Voice", "Lead 7 Fifths", "Lead 8 (Bass + Lead)", "Pad 1 New Age", "Pad 2 Warm", "Pad 3 Polysynth", "Pad 4 Choir", "Pad 5 Bowed", "Pad 6 Metallic", "Pad 7 Halo", "Pad 8 Sweep", "FX 1 Rain", "FX 2 Soundtrack", "FX 3 Crystal", "FX 4 Atmosphere", "FX 5 Brightness", "FX 6 Goblins", "FX 7 Echoes", "FX 8 Scifi", "Sitar", "Banjo", "Shamisen", "Koto", "Kalimba", "Bagpipe", "Fiddle", "Shanai", "Tinkle Bell", "Agogo", "Steel Drums", "Woodblock", "Taiko Drum", "Melodic Tom", "Synth Drum", "Reverse Cymbal", "Guitar Fret Noise", "Breath Noise", "Seashore", "Bird Tweet", "Telephone Ring", "Helicopter", "Applause", "Gunshot"};
    private Map<String, Short> dict = new HashMap();

    public InstrumentsDictionary() throws IOException, NullPointerException {
        InputStream is = this.getClass().getResourceAsStream("/resources/instuments.dict");
        InputStreamReader ir = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(ir);
        Short pc = Short.valueOf((short)0);

        for(String line = br.readLine(); line != null; line = br.readLine()) {
            if (!line.isEmpty() && line.charAt(0) != '%') {
                if (line.charAt(0) == '#') {
                    pc = Short.parseShort(line.substring(1).replaceAll("\\s+", ""));
                    if (pc > 127) {
                        pc = Short.valueOf((short)127);
                    }

                    if (pc < 0) {
                        pc = Short.valueOf((short)0);
                    }
                } else {
                    this.dict.put(line.toLowerCase(), pc);
                }
            }
        }

        br.close();
        ir.close();
        is.close();
    }

    public short getProgramChange(String name) {
        return this.getProgramChange(name, (byte)42);
    }

    public short getProgramChange(String name, byte distanceMethod) {
        if (name.isEmpty()) {
            return 0;
        } else {
            String n = name.toLowerCase();
            short pc = 0;
            double distance = Double.MAX_VALUE;
            String foo = "";
            Iterator var8 = this.dict.entrySet().iterator();

            while(var8.hasNext()) {
                Map.Entry<String, Short> entry = (Map.Entry)var8.next();
                double cur_distance;
                switch (distanceMethod) {
//                    case 0:
//                        cur_distance = (new Levenshtein()).distance((String)entry.getKey(), n);
//                        break;
//                    case 1:
//                        cur_distance = (new NormalizedLevenshtein()).distance((String)entry.getKey(), n);
//                        break;
//                    case 2:
//                        cur_distance = (new Damerau()).distance((String)entry.getKey(), n);
//                        break;
//                    case 3:
//                        cur_distance = (new JaroWinkler()).distance((String)entry.getKey(), n);
//                        break;
//                    case 4:
//                        cur_distance = (new LongestCommonSubsequence()).distance((String)entry.getKey(), n);
//                        break;
//                    case 5:
//                        cur_distance = (new MetricLCS()).distance((String)entry.getKey(), n);
//                        break;
//                    case 6:
//                        cur_distance = (new NGram(2)).distance((String)entry.getKey(), n);
//                        break;
//                    case 7:
//                        cur_distance = (new QGram(2)).distance((String)entry.getKey(), n);
//                        break;
//                    case 8:
//                        cur_distance = (new Cosine()).distance((String)entry.getKey(), n);
//                        break;
//                    case 9:
//                        cur_distance = (new Jaccard()).distance((String)entry.getKey(), n);
//                        break;
//                    case 10:
//                        cur_distance = (new SorensenDice()).distance((String)entry.getKey(), n);
//                        break;
                    default:
                        cur_distance = levenshtein((String)entry.getKey(), n);
                }

                if (cur_distance == 0.0) {
                    System.out.println(name + " is mapped to " + (String)entry.getKey() + " with " + cur_distance);
                    return (Short)entry.getValue();
                }

                if (cur_distance < distance) {
                    distance = cur_distance;
                    pc = (Short)entry.getValue();
                    foo = (String)entry.getKey();
                }
            }

            System.out.println(name + " is mapped to " + foo + " with " + distance);
            return pc;
        }
    }

    private int levenshtein(String str1, String str2) {
        int[][] matrix = new int[str1.length() + 1][str2.length() + 1];

        int a;
        for(a = 0; a < str1.length() + 1; matrix[a][0] = a++) {
        }

        for(a = 0; a < str2.length() + 1; matrix[0][a] = a++) {
        }

        for(a = 1; a < str1.length() + 1; ++a) {
            for(int b = 1; b < str2.length() + 1; ++b) {
                int right = 0;
                if (str1.charAt(a - 1) != str2.charAt(b - 1)) {
                    right = 1;
                }

                int mini = matrix[a - 1][b] + 1;
                if (matrix[a][b - 1] + 1 < mini) {
                    mini = matrix[a][b - 1] + 1;
                }

                if (matrix[a - 1][b - 1] + right < mini) {
                    mini = matrix[a - 1][b - 1] + right;
                }

                matrix[a][b] = mini;
            }
        }

        return matrix[str1.length()][str2.length()];
    }

    public static String getInstrumentName(short programChangeNumber) {
        return getInstrumentName(programChangeNumber, false);
    }

    public static String getInstrumentName(short programChangeNumber, boolean useGmDefaultNames) {
        if (useGmDefaultNames) {
            return DefaultNames[programChangeNumber];
        } else {
            InstrumentsDictionary dict;
            try {
                dict = new InstrumentsDictionary();
            } catch (IOException var5) {
                IOException e = var5;
                e.printStackTrace();
                return DefaultNames[programChangeNumber];
            }

            Iterator var6 = dict.dict.entrySet().iterator();

            Map.Entry entry;
            do {
                if (!var6.hasNext()) {
                    return "";
                }

                entry = (Map.Entry)var6.next();
            } while(!((Short)entry.getValue()).equals(programChangeNumber));

            return (String)entry.getKey();
        }
    }
}
