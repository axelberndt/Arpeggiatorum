package arpeggiatorum.gui;

import meico.supplementary.KeyValue;

/**
 * This class represents a preset item in the tonal enrichment ComboBox.
 * @author Axel Berndt
 */
public class TonalEnrichmentChooserItem extends KeyValue<String, int[]> {
    /**
     * constructor
     * @param key
     * @param value
     */
    public TonalEnrichmentChooserItem(String key, int[] value) {
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
