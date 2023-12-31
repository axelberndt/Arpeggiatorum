package arpeggiatorum.gui;


import meico.supplementary.KeyValue;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

/**
 * This class is used to create items in a ComboBox.
 * @author Axel Berndt
 */
class MidiDeviceChooserItem extends KeyValue<String, MidiDevice> {
    /**
     * constructor
     * @param info
     * @throws MidiUnavailableException
     */
    public MidiDeviceChooserItem(MidiDevice.Info info) throws MidiUnavailableException {
        super(info.getName(), MidiSystem.getMidiDevice(info));
    }

    /**
     * constructor
     * @param key
     * @param value
     */
    public MidiDeviceChooserItem(String key, MidiDevice value) {
        super(key, value);
    }

    /**
     * All ComboBox items require this method. The overwrite here makes sure that the string being returned
     * is the device name instead of some Java Object ID.
     * @return
     */
    @Override
    public String toString() {
        return this.getKey();
    }
}
