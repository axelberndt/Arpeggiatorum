package arpeggiatorum.gui.regulators;

import javafx.scene.text.Font;


/**
 * Created by hansolo on 01.03.16.
 */
public class Fonts {
    private static final String ROBOTO_LIGHT_NAME;
    private static final String ROBOTO_MEDIUM_NAME;

    private static String robotoLightName;
    private static String robotoMediumName;

    static {
        try {
            robotoLightName  = Font.loadFont(Fonts.class.getResourceAsStream("regulators/Roboto-Light.ttf"), 10).getName();
            robotoMediumName = Font.loadFont(Fonts.class.getResourceAsStream("regulators/Roboto-Medium.ttf"), 10).getName();
        } catch (Exception exception) { }
        ROBOTO_LIGHT_NAME  = robotoLightName;
        ROBOTO_MEDIUM_NAME = robotoMediumName;
    }


    // ******************** Methods *******************************************
    public static Font robotoLight(final double SIZE) { return new Font(ROBOTO_LIGHT_NAME, SIZE); }
    public static Font robotoMedium(final double SIZE) { return new Font(ROBOTO_MEDIUM_NAME, SIZE); }
}
