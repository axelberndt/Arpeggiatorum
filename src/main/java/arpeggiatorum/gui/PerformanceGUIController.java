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
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Cell;
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

    String buttonPanicStyle = "-fx-text-fill: RED;-fx-font-size: 28;-fx-pref-width: 200;-fx-pref-height: 200;";
    String buttonTempoStyle = "-fx-text-fill: BLACK;-fx-font-size: 14;-fx-pref-width: 100;-fx-pref-height: 100;";
    String buttonEnrichmentStyleUnchecked = "-fx-text-fill: BLACK;-fx-font-size: 14;-fx-pref-width: 75;-fx-pref-height: 75;";
    String buttonEnrichmentStyleChecked = "-fx-text-fill: BLACK;-fx-font-size: 14;-fx-pref-width: 75;-fx-pref-height: 75;-fx-background-color: Chartreuse;";

    int pixelHeight;
    int pixelWidth;

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


        buttonPanic = new Button("PANIC!");
        buttonPanic.setStyle(buttonPanicStyle);
        buttonPanic.setTranslateX(pixelWidth - 200);
        buttonPanic.setTranslateY(pixelHeight - 200);
        buttonPanic.setOnAction(event -> {
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
        });


        buttonConfirmPanic = new Button("Confirm Panic");
        buttonConfirmPanic.setStyle(buttonPanicStyle + "-fx-wrap-text: TRUE;-fx-text-alignment: CENTER;");
        buttonConfirmPanic.setTranslateX(buttonPanic.getTranslateX() - 200);
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
            buttonEnrichmentArray[i].setTranslateX((i * 75));
            buttonEnrichmentArray[i].setTranslateY(pixelHeight - 75);
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
            buttonEnrichmentArray[i].setOnTouchPressed(event -> {
                //But touch isn't working
                System.out.println("TOUCH");

            });
            //Workaround for touch, but not great
            // buttonEnrichmentArray[i].setOnMouseEntered(this::handleEnrichmentArray);

        }
        buttonEnrichmentArray[(int) Math.ceil(15.0 * (ArpeggiatorumGUI.controllerHandle.sliderEnrichment.getValue() / 100.0))].fire();

        toggleAudio = new ToggleSwitch("Audio IN");
        toggleAudio.selectedProperty().addListener((observable, oldValue, newValue) -> Arpeggiatorum.getInstance().Activate(newValue));
        toggleAudio.setTranslateX(0);
        toggleAudio.setTranslateY(0);
        toggleAudio.getStylesheets().addAll(ArpeggiatorumGUI.class.getResource("toggleSwitch.css").toExternalForm());

        sliderArticulation = TouchSliderBuilder.create()
                .prefSize(600, 200)
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
        sliderArticulation.setTranslateX(300);
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
        toggleHeld.setTranslateX(1000);
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
        toggleArpeggio.setTranslateX(1250);
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
        toggleBass.setTranslateX(1500);
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

        radialMenuPattern = createCenterRadialMenu("Pattern", ArpeggiatorumGUI.controllerHandle.comboPattern.getItems().stream().toList(), patternHandler);
        radialMenuPattern.setTranslateX(1400);
        radialMenuPattern.setTranslateY(450);
        radialMenuPattern.hideRadialMenu();

        radialMenuEnrichment = createCenterRadialMenu("Enrichment", ArpeggiatorumGUI.controllerHandle.comboEnrichment.getItems().stream().toList(), enrichmentHandler);
        radialMenuEnrichment.setTranslateX(250);
        radialMenuEnrichment.setTranslateY(450);
        radialMenuEnrichment.hideRadialMenu();

        //Debug Label
        actionPerformedLabel.setTranslateX(800);
        actionPerformedLabel.setTranslateY(250);

        regulatorTempo = RegulatorBuilder.create()
                .prefSize(300, 300)
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
        regulatorTempo.setTranslateX(700);
        regulatorTempo.setTranslateY(300);

        buttonTap = new Button("Tap Tempo");
        buttonTap.setStyle(buttonTempoStyle);
        buttonTap.setTranslateX(750);
        buttonTap.setTranslateY(550);
        buttonTap.setOnAction(event -> {
            Arpeggiatorum.getInstance().tapTempo();
            regulatorTempo.setTargetValue(ArpeggiatorumGUI.controllerHandle.sliderTempo.getValue());
        });
        anchorPane.getChildren().addAll(toggleAudio,
                sliderArticulation,
                toggleHeld, toggleArpeggio, toggleBass,
                radialMenuPattern, radialMenuEnrichment, actionPerformedLabel,
                regulatorTempo, buttonTap,
                buttonConfirmPanic, buttonPanic);
        anchorPane.getChildren().addAll(buttonEnrichmentArray);

        //Here we might be able to get the actual size
        Platform.runLater(() -> {
            //This does not include scaling
            System.out.println(buttonPanic.getBoundsInLocal().getWidth());
        });
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
        radialMenu.setInnerRadius(50.0);
        radialMenu.setGraphicsFitWidth(0.0);
        radialMenu.setRadius(200.0);
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
}
