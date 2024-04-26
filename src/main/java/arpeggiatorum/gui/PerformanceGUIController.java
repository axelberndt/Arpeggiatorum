/**
 * Sample Skeleton for 'PerformanceGUI.fxml' Controller Class
 */

package arpeggiatorum.gui;

import arpeggiatorum.Arpeggiatorum;
import arpeggiatorum.gui.cornerRadialMenu.RadialMenu;
import arpeggiatorum.gui.cornerRadialMenu.RadialMenuItem;
import arpeggiatorum.gui.touchSlider.TouchSlider;
import arpeggiatorum.gui.touchSlider.TouchSliderBuilder;
import arpeggiatorum.notePool.NotePool;
import eu.hansolo.regulators.Regulator;
import eu.hansolo.regulators.RegulatorBuilder;
import javafx.animation.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Screen;
import javafx.util.Duration;
import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.fontawesome.FontAwesome;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class PerformanceGUIController implements Initializable {
    @FXML
    public AnchorPane anchorPane;

    public Regulator regulatorTempo;

    public RadialMenu radialMenuPattern;
    public RadialMenu radialMenuEnrichment;

    public ToggleSwitch toggleAudio;
    public ToggleSwitch toggleHeld;
    public ToggleSwitch toggleArpeggio;
    public ToggleSwitch toggleBass;

    public TouchSlider sliderArticulation;

    public Button buttonPanic;
    public Button buttonConfirmPanic;
    public Button buttonTap;

    @FXML
    public Button[] buttonEnrichmentArray;

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

    int buttonSizeLarge = 200;
    int buttonSizeMedium = (int) (buttonSizeLarge * 0.5);
    int buttonSizeSmall = (int) (buttonSizeMedium * 0.75);
    String buttonPanicStyle = "-fx-text-fill: RED;-fx-font-size: 28;-fx-pref-width: " + buttonSizeLarge + ";-fx-pref-height: " + buttonSizeLarge + ";";
    String buttonTempoStyle = "-fx-text-fill: BLACK;-fx-font-size: 14;-fx-pref-width: " + buttonSizeMedium + ";-fx-pref-height: " + buttonSizeMedium + ";";
    String buttonEnrichmentStyleUnchecked = "-fx-text-fill: WHITE;-fx-font-size: 14;-fx-pref-width: " + buttonSizeSmall + ";-fx-pref-height: " + buttonSizeSmall + ";-fx-background-color: DarkGray;";
    String buttonEnrichmentStyleChecked = "-fx-text-fill: WHITE;-fx-font-size: 14;-fx-pref-width: " + buttonSizeSmall + ";-fx-pref-height: " + buttonSizeSmall + ";-fx-background-color: DARKGREEN;";

    int pixelHeight;
    int pixelWidth;
    int visualVerticalBuffer = 24;
    int visualHorizontalBuffer = 5;


    @FXML // This method is called by the FXMLLoader when initialization is complete
    /*
      @param url
     * @param resourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //This gets us the usable size of the window
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        pixelHeight = (int) screenBounds.getHeight();
        pixelWidth = (int) screenBounds.getWidth();
        //How to get window title height instead of using a magic number? (visualVertricalBuffer)

        anchorPane.setOnTouchPressed(touchEvent -> {
            System.out.println(touchEvent);
            touchEvent.consume();
        });
        anchorPane.setOnTouchMoved(touchEvent -> {
            if (touchEvent.getTarget() instanceof Button) {
                //call that button action
            }
            touchEvent.consume();
        });

        buttonPanic = new Button("PANIC");
        buttonPanic.setStyle(buttonPanicStyle);
        buttonPanic.setTranslateX(pixelWidth - (buttonSizeLarge));
        buttonPanic.setTranslateY(pixelHeight - (buttonSizeLarge + visualVerticalBuffer));
        buttonPanic.setOnAction(this::buttonPanicHandle);


        buttonConfirmPanic = new Button("Confirm Panic");
        buttonConfirmPanic.setStyle(buttonPanicStyle + "-fx-wrap-text: TRUE;-fx-text-alignment: CENTER;");
        buttonConfirmPanic.setTranslateX(buttonPanic.getTranslateX() - (buttonSizeLarge));
        buttonConfirmPanic.setTranslateY(buttonPanic.getTranslateY());
        buttonConfirmPanic.setVisible(false);
        buttonConfirmPanic.setOnAction(event -> {
            if (ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.isSelected()) {
                ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.fire();
            }
            Arpeggiatorum.getInstance().getArpeggiator().panic();
        });

        buttonEnrichmentArray = new Button[16];
        for (int i = 0; i < buttonEnrichmentArray.length; i++) {
            buttonEnrichmentArray[i] = new Button();
            buttonEnrichmentArray[i].setText(String.valueOf(ArpeggiatorumGUI.controllerHandle.comboEnrichment.getValue().getValue()[i]));
            buttonEnrichmentArray[i].setStyle(buttonEnrichmentStyleUnchecked);
            buttonEnrichmentArray[i].setTranslateX((i * buttonSizeSmall));
            buttonEnrichmentArray[i].setTranslateY(pixelHeight - (buttonSizeSmall + visualVerticalBuffer));
            buttonEnrichmentArray[i].setOnAction(event -> {
                Boolean changeColor = true;
                for (int j = 0; j < buttonEnrichmentArray.length; j++) {
                    if (changeColor) {
                        buttonEnrichmentArray[j].setStyle(buttonEnrichmentStyleChecked);
                    } else {
                        buttonEnrichmentArray[j].setStyle(buttonEnrichmentStyleUnchecked);
                    }
                    if (event.getSource() == buttonEnrichmentArray[j]) {
                        changeColor = false;
                        ArpeggiatorumGUI.controllerHandle.sliderEnrichment.adjustValue((j / 15.0) * 100.0);
                    }
                }
            });
        }
        buttonEnrichmentArray[(int) Math.ceil(15.0 * (ArpeggiatorumGUI.controllerHandle.sliderEnrichment.getValue() / 100.0))].fire();

        toggleAudio = new ToggleSwitch("Audio IN");
        toggleAudio.selectedProperty().addListener((observable, oldValue, newValue) -> Arpeggiatorum.getInstance().Activate(newValue));
        toggleAudio.setTranslateX(0);
        toggleAudio.setTranslateY(0);
        toggleAudio.getStylesheets().addAll(ArpeggiatorumGUI.class.getResource("toggleSwitch.css").toExternalForm());

        sliderArticulation = TouchSliderBuilder.create()
                .prefSize(buttonSizeLarge * 3, buttonSizeLarge)
                .name("Articulation")
                .orientation(Orientation.HORIZONTAL)
                .minValue(50)
                .range(100)
                .sliderValue(ArpeggiatorumGUI.controllerHandle.sliderArticulation.getValue())
                .formatString("%.0f")
                .barBackgroundColor(Color.GAINSBORO)
                .barColor(Color.DARKGREEN)
                .thumbColor(Color.CHARTREUSE)
                .valueTextColor(Color.WHITE)
                .nameTextColor(Color.WHITE)
                .valueVisible(false)
                .nameVisible(true)
                .onTouchSliderEvent(e -> ArpeggiatorumGUI.controllerHandle.sliderArticulation.adjustValue(e.getValue()))
                .build();
        sliderArticulation.setTranslateX(buttonSizeLarge * 1.25);
        sliderArticulation.setTranslateY(0);


        toggleHeld = new ToggleSwitch("Held");
        int heldValue = ArpeggiatorumGUI.controllerHandle.comboHeldChannel.getSelectionModel().getSelectedIndex();
        if (heldValue != 0) {
            toggleHeld.setSelected(true);
        }
        toggleHeld.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                ArpeggiatorumGUI.controllerHandle.comboHeldChannel.getSelectionModel().select(heldValue);
            } else {
                ArpeggiatorumGUI.controllerHandle.comboHeldChannel.getSelectionModel().select(0);
            }
        });
        toggleHeld.setTranslateX(pixelWidth - ((buttonSizeLarge * 3) + (visualHorizontalBuffer * 2)));
        toggleHeld.setTranslateY(0);
        toggleHeld.getStylesheets().addAll(ArpeggiatorumGUI.class.getResource("toggleSwitch.css").toExternalForm());

        toggleArpeggio = new ToggleSwitch("Arpeggio");
        int arpeggioValue = ArpeggiatorumGUI.controllerHandle.comboArpeggioChannel.getSelectionModel().getSelectedIndex();
        if (arpeggioValue != 0) {
            toggleArpeggio.setSelected(true);
        }
        toggleArpeggio.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                ArpeggiatorumGUI.controllerHandle.comboArpeggioChannel.getSelectionModel().select(arpeggioValue);
            } else {
                ArpeggiatorumGUI.controllerHandle.comboArpeggioChannel.getSelectionModel().select(0);
            }
        });
        toggleArpeggio.setTranslateX(pixelWidth - ((buttonSizeLarge * 2) + (visualHorizontalBuffer * 1)));
        toggleArpeggio.setTranslateY(0);
        toggleArpeggio.getStylesheets().addAll(ArpeggiatorumGUI.class.getResource("toggleSwitch.css").toExternalForm());

        toggleBass = new ToggleSwitch("Bass");
        int bassValue = ArpeggiatorumGUI.controllerHandle.comboBassChannel.getSelectionModel().getSelectedIndex();
        if (bassValue != 0) {
            toggleBass.setSelected(true);
        }
        toggleBass.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                ArpeggiatorumGUI.controllerHandle.comboBassChannel.getSelectionModel().select(bassValue);
            } else {
                ArpeggiatorumGUI.controllerHandle.comboBassChannel.getSelectionModel().select(0);
            }
        });
        toggleBass.setTranslateX(pixelWidth - buttonSizeLarge);
        toggleBass.setTranslateY(0);
        toggleBass.getStylesheets().addAll(ArpeggiatorumGUI.class.getResource("toggleSwitch.css").toExternalForm());

        final EventHandler<ActionEvent> patternHandler = new EventHandler<>() {
            @Override
            public synchronized void handle(final ActionEvent paramT) {
                final RadialMenuItem item = (RadialMenuItem) paramT.getSource();
                ArpeggiatorumGUI.controllerHandle.comboPattern.setValue(NotePool.Pattern.fromString(item.getText()));
                radialAction(item);
            }
        };

        final EventHandler<ActionEvent> enrichmentHandler = new EventHandler<>() {
            @Override
            public synchronized void handle(final ActionEvent paramT) {
                final RadialMenuItem item = (RadialMenuItem) paramT.getSource();
                for (TonalEnrichmentChooserItem element : ArpeggiatorumGUI.controllerHandle.comboEnrichment.getItems()) {
                    if (element.toString().equals(item.getText())) {
                        ArpeggiatorumGUI.controllerHandle.comboEnrichment.setValue(element);
                    }
                }
                for (int i = 0; i < buttonEnrichmentArray.length; i++) {
                    buttonEnrichmentArray[i].setText(String.valueOf(ArpeggiatorumGUI.controllerHandle.comboEnrichment.getValue().getValue()[i]));
                }
                radialAction(item);
            }
        };


        radialMenuEnrichment = createCenterRadialMenu("Enrichment", ArpeggiatorumGUI.controllerHandle.comboEnrichment.getItems().stream().toList(), enrichmentHandler);
        radialMenuEnrichment.setTranslateX(pixelWidth * 0.2);
        radialMenuEnrichment.setTranslateY(pixelHeight * 0.5);
        radialMenuEnrichment.hideRadialMenu();

        radialMenuPattern = createCenterRadialMenu("Pattern", ArpeggiatorumGUI.controllerHandle.comboPattern.getItems().stream().toList(), patternHandler);
        radialMenuPattern.setTranslateX(pixelWidth * 0.8);
        radialMenuPattern.setTranslateY(pixelHeight * 0.5);
        radialMenuPattern.hideRadialMenu();


        //Debug Label
        actionPerformedLabel.setTranslateX(pixelWidth * 0.5);
        actionPerformedLabel.setTranslateY(pixelHeight * 0.25);
        actionPerformedLabel.setStyle("    -fx-font: 28 px;" +
                "    -fx-text-fill: WHITE;" +
                "    -fx-alignment: CENTER;" +
                "    -fx-wrap-text: TRUE;");

        regulatorTempo = RegulatorBuilder.create()
                .prefSize(buttonSizeLarge * 2.0, buttonSizeLarge * 2.0)
                .minValue(ArpeggiatorumGUI.controllerHandle.sliderTempo.getMin())
                .maxValue(ArpeggiatorumGUI.controllerHandle.sliderTempo.getMax())
                .targetValue(ArpeggiatorumGUI.controllerHandle.sliderTempo.getValue())
                .unit("BPM")
                .barColor(Color.CHARTREUSE)
                .textColor(Color.WHITE)
                .icon(FontAwesome.MUSIC)
                .iconColor(Color.WHITE)
                .color(Color.GAINSBORO)
                .onTargetSet(e -> ArpeggiatorumGUI.controllerHandle.sliderTempo.adjustValue(regulatorTempo.getTargetValue()))
                .build();
        regulatorTempo.setTranslateX((pixelWidth * 0.5) - (regulatorTempo.getPrefWidth() * 0.5));
        regulatorTempo.setTranslateY((pixelHeight * 0.5) - (regulatorTempo.getPrefHeight() * 0.5));

        buttonTap = new Button("Tap Tempo");
        buttonTap.setStyle(buttonTempoStyle);
        buttonTap.setTranslateX(pixelWidth * 0.5);
        buttonTap.setTranslateY(pixelHeight * 0.5);
        buttonTap.setOnAction(event -> {
            Arpeggiatorum.getInstance().tapTempo();
            regulatorTempo.setTargetValue(ArpeggiatorumGUI.controllerHandle.sliderTempo.getValue());
        });
        anchorPane.getChildren().addAll(toggleAudio,
                sliderArticulation,
                toggleHeld, toggleArpeggio, toggleBass,
                radialMenuPattern, radialMenuEnrichment, actionPerformedLabel,
                regulatorTempo,
                //buttonTap,
                buttonConfirmPanic, buttonPanic);
        anchorPane.getChildren().addAll(buttonEnrichmentArray);
    }

    private void radialAction(RadialMenuItem item) {
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

    public RadialMenu createCenterRadialMenu(String menuName, List
            menuItems, EventHandler<ActionEvent> eventHandler) {
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
        radialMenu.setInnerRadius(buttonSizeSmall);
        radialMenu.setGraphicsFitWidth(0.0);
        radialMenu.setRadius(buttonSizeLarge);
        radialMenu.setOffset(1.0);
        radialMenu.setInitialAngle(0.0);
        radialMenu.setStrokeWidth(1.5);

        return radialMenu;
    }

    private void handleEnrichmentArray(MouseEvent event) {
        Boolean changeColor = true;
        for (int j = 0; j < buttonEnrichmentArray.length; j++) {
            if (changeColor) {
                buttonEnrichmentArray[j].setStyle(buttonEnrichmentStyleChecked);
            } else {
                buttonEnrichmentArray[j].setStyle(buttonEnrichmentStyleUnchecked);
            }
            if (event.getSource() == buttonEnrichmentArray[j]) {
                changeColor = false;
                ArpeggiatorumGUI.controllerHandle.sliderEnrichment.adjustValue((j / 15.0) * 100.0);
            }
        }
    }

    private void buttonPanicHandle(ActionEvent event) {
        buttonConfirmPanic.setVisible(true);
        FadeTransition buttonFadeTransition = new FadeTransition(Duration.millis(400), buttonConfirmPanic);
        buttonFadeTransition.setDelay(Duration.seconds(1));
        buttonFadeTransition.setFromValue(1);
        buttonFadeTransition.setToValue(0);
        buttonFadeTransition.setOnFinished(e -> {
            buttonConfirmPanic.setVisible(false);
            buttonConfirmPanic.setOpacity(1.0);
        });
        buttonFadeTransition.play();
    }
}
