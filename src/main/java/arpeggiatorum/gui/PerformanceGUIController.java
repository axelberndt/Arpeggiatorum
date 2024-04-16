/**
 * Sample Skeleton for 'PerformanceGUI.fxml' Controller Class
 */

package arpeggiatorum.gui;

import arpeggiatorum.gui.cornerRadialMenu.RadialMenu;
import arpeggiatorum.gui.cornerRadialMenu.RadialMenuItem;
import arpeggiatorum.gui.rotaryKnob.RotaryKnob;
import javafx.animation.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class PerformanceGUIController implements Initializable {
    @FXML
    public AnchorPane anchorPane;

    public RotaryKnob rotaryTempo;
    public RadialMenu radialMenuPattern;
    public RadialMenu radialMenuEnrichment;

    protected Label actionPerformedLabel = new Label();

    protected boolean show;
    protected double lastOffsetValue;
    protected double lastInitialAngleValue;

    //Menu Properties
    double ITEM_SIZE = 20.0;
    double INNER_RADIUS = 70.0;
    double ITEM_FIT_WIDTH = 70.0;
    double MENU_SIZE = 250.0;
    double OFFSET = 1.0;
    double INITIAL_ANGLE = 0.0;
    double STROKE_WIDTH = 1.5;
    double CORNER_ITEM_SIZE = 38.0;
    double CORNER_INNER_RADIUS = 30;
    double CORNER_ITEM_FIT_WIDTH = 30.0;
    double CORNER_MENU_SIZE = 80.0;
    double CORNER_OFFSET = 4.0;
    double CORNER_INITIAL_ANGLE = 240.0;
    double CORNER_STROKE_WIDTH = 0.5;


    //Colors
    Color bgLg1Color = Color.CHARTREUSE.deriveColor(1, 1, 1, 0.2);
    Color bgLg2Color = Color.CHARTREUSE.deriveColor(1, 1, 1, 0.5);
    Color bgMoLg1Color = Color.CHARTREUSE.deriveColor(1, 1, 1, 0.3);
    Color bgMoLg2Color = Color.CHARTREUSE.deriveColor(1, 1, 1, 0.6);
    Color strokeColor = Color.CHARTREUSE;
    Color strokeMouseOnColor = Color.CHARTREUSE;
    Color outlineColor = Color.CHARTREUSE;
    Color outlineMouseOnColor = Color.CHARTREUSE;

    //Observable Properties
    private SimpleLongProperty timeDelayProp = new SimpleLongProperty(2000);
    private SimpleBooleanProperty centeredMenu = new SimpleBooleanProperty(true);

    //Transitions
    TranslateTransition tt;
    ParallelTransition pt;
    FadeTransition textFadeTransition;
    Timeline animation;

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // This method is called by the FXMLLoader when initialization is complete
    /**
     * @param url
     * @param resourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        rotaryTempo = new RotaryKnob();
        rotaryTempo.setTranslateX(1150);
        rotaryTempo.setTranslateY(200);
        rotaryTempo.setValue(1000);

        final EventHandler<ActionEvent> patternHandler = new EventHandler<>() {
            @Override
            public synchronized void handle(final ActionEvent paramT) {
                final RadialMenuItem item = (RadialMenuItem) paramT.getSource();
                if (textFadeTransition != null
                        && textFadeTransition.getStatus() != Animation.Status.STOPPED) {
                    textFadeTransition.stop();
                    actionPerformedLabel.setOpacity(1.0);
                }
                actionPerformedLabel.setText(item.getText());
                actionPerformedLabel.setVisible(true);

                FadeTransition textFadeTransition = new FadeTransition(Duration.millis(400), actionPerformedLabel);
                textFadeTransition.setDelay(Duration.seconds(1));
                textFadeTransition.setFromValue(1);
                textFadeTransition.setToValue(0);
                textFadeTransition.setOnFinished(e -> {
                    actionPerformedLabel.setVisible(false);
                    actionPerformedLabel.setOpacity(1.0);
                });
                textFadeTransition.play();
            }
        };
        radialMenuPattern = createCenterRadialMenu("Pattern", ArpeggiatorumGUI.controllerHandle.comboPattern.getItems().stream().toList(), patternHandler);
        radialMenuPattern.setTranslateX(500);
        radialMenuPattern.setTranslateY(400);
        radialMenuPattern.hideRadialMenu();

        radialMenuEnrichment = createCenterRadialMenu("Enrichment", ArpeggiatorumGUI.controllerHandle.comboEnrichment.getItems().stream().toList(), patternHandler);
        radialMenuEnrichment.setTranslateX(1000);
        radialMenuEnrichment.setTranslateY(400);
        radialMenuEnrichment.hideRadialMenu();

        //Debug Label
        actionPerformedLabel.setTranslateX(750);
        actionPerformedLabel.setTranslateY(100);

        anchorPane.getChildren().addAll(radialMenuPattern, radialMenuEnrichment, actionPerformedLabel, rotaryTempo);

    }

    public RadialMenu createCenterRadialMenu(String menuName, List menuItems, EventHandler<ActionEvent> eventHandler) {
        LinearGradient background = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, bgLg1Color), new Stop(0.8, bgLg2Color));
        LinearGradient backgroundMouseOn = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, bgMoLg1Color), new Stop(0.8, bgMoLg2Color));

        RadialMenu radialMenu = new RadialMenu(INITIAL_ANGLE, ITEM_SIZE, MENU_SIZE, OFFSET,
                background, backgroundMouseOn, strokeColor, strokeMouseOnColor,
                false, RadialMenu.CenterVisibility.ALWAYS, new Label(menuName));
        radialMenu.setStrokeWidth(STROKE_WIDTH);
        radialMenu.setOutlineStrokeWidth(STROKE_WIDTH);
        radialMenu.setOutlineStrokeFill(outlineColor);
        radialMenu.setOutlineStrokeMouseOnFill(outlineMouseOnColor);
        //Populate with items
        for (Object element : menuItems) {
            radialMenu.addMenuItem(new RadialMenuItem(ITEM_SIZE, element.toString(), new Label(element.toString()), eventHandler));
        }
        //Settings for RadialMenu:
        radialMenu.setMenuItemSize(360.0 / menuItems.size());
        radialMenu.setInnerRadius(50.0);
        radialMenu.setGraphicsFitWidth(0.0);
        radialMenu.setRadius(200.0);
        radialMenu.setOffset(1.0);
        radialMenu.setInitialAngle(0.0);
        radialMenu.setStrokeWidth(1.5);

        return radialMenu;
    }
}
