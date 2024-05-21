package arpeggiatorum.gui.regulators;

import arpeggiatorum.Arpeggiatorum;
import arpeggiatorum.gui.ArpeggiatorumGUI;
import javafx.scene.text.Font;


/**
 * Created by hansolo on 01.03.16.
 */
public class Fonts {
    private static final String ROBOTO_LIGHT_NAME;
    private static final String ROBOTO_MEDIUM_NAME;
    private static final String ROBOTO_CONDENSED_NAME;

    private static String robotoLightName;
    private static String robotoMediumName;
    private static String robotoCondensedName;

    static {
        try {
            robotoLightName = Font.loadFont(Fonts.class.getResourceAsStream("regulators/Roboto-Light.ttf"), 10).getName();
            robotoMediumName = Font.loadFont(Fonts.class.getResourceAsStream("regulators/Roboto-Medium.ttf"), 10).getName();
            robotoCondensedName = Font.loadFont(ArpeggiatorumGUI.class.getResourceAsStream("RobotoCondensed-Regular.ttf"), 10).getName();

        } catch (Exception exception) {
            System.out.println(exception.getMessage());

        }
        ROBOTO_LIGHT_NAME = robotoLightName;
        ROBOTO_MEDIUM_NAME = robotoMediumName;
        ROBOTO_CONDENSED_NAME = robotoCondensedName;

    }


    // ******************** Methods *******************************************
    public static Font robotoLight(final double SIZE) {
        return new Font(ROBOTO_LIGHT_NAME, SIZE);
    }

    public static Font robotoMedium(final double SIZE) {
        return new Font(ROBOTO_MEDIUM_NAME, SIZE);
    }

    public static Font robotoCondensed(final double SIZE) {
        return new Font(ROBOTO_CONDENSED_NAME, SIZE);
    }

}
