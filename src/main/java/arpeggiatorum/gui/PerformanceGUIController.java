/**
 * Sample Skeleton for 'PerformanceGUI.fxml' Controller Class
 */

package arpeggiatorum.gui;

import arpeggiatorum.Arpeggiatorum;
import arpeggiatorum.gui.cornerRadialMenu.RadialMenu;
import arpeggiatorum.gui.cornerRadialMenu.RadialMenuItem;
import arpeggiatorum.gui.regulators.Regulator;
import arpeggiatorum.gui.regulators.RegulatorBuilder;
import arpeggiatorum.gui.touchSlider.TouchSlider;
import arpeggiatorum.gui.touchSlider.TouchSliderBuilder;
import arpeggiatorum.notePool.NotePool;
import arpeggiatorum.supplementary.TonalEnrichmentChooserItem;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Screen;
import javafx.util.Duration;

import org.controlsfx.control.ToggleSwitch;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class PerformanceGUIController implements Initializable {
    @FXML
    public AnchorPane anchorPane;

    public Regulator regulatorTempo;

    public RadialMenu radialMenuPattern;
    public RadialMenu radialMenuEnrichment;

    public ToggleSwitch toggleAudio;
    public ToggleSwitch toggleSustained;
    public ToggleSwitch toggleArpeggio;
    public ToggleSwitch toggleBass;

    public TouchSlider sliderArticulation;

    public Button buttonPanic;
    public Button buttonConfirmPanic;
    public Button buttonTap;

    @FXML
    public EnrichmentButton[] buttonEnrichmentArray;

    //Colors
    public static Color bgLg1Color = Color.DARKKHAKI.deriveColor(1, 1, 1, 1);
    public static Color bgLg2Color = Color.DARKKHAKI.deriveColor(1, 1, 1, 1);
    public static Color bgHg1Color = Color.DARKKHAKI.deriveColor(1, 1, 1, 1);
    public static Color bgHg2Color = Color.DARKKHAKI.deriveColor(1, 1, 1, 1);
    public static Color bgMoLg1Color = Color.LEMONCHIFFON.deriveColor(1, 1, 1, 1);
    public static Color bgMoLg2Color = Color.LEMONCHIFFON.deriveColor(1, 1, 1, 1);
    public static Color strokeColor = Color.BLACK;
    public static Color strokeMouseOnColor = Color.BLACK;


    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;
    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    double buttonSizeLarge;
    double buttonSizeML;
    double buttonSizeMedium;
    double buttonSizeSmall;
    double button16;
    double buttonSizePanic;

    String buttonPanicStyle;
    String buttonTempoStyle;
    String buttonEnrichmentStyleUnchecked;
    String buttonEnrichmentStyleChecked;
    String labelActionStyle;

    Label labelAudio, labelSustained, labelArpeggio, labelBass;

    double actualPixelHeight;
    double actualPixelWidth;
    double pixelHeight = 1067;
    double pixelWidth = 1600;
    double visualVerticalBuffer = 10;
    double visualHorizontalBuffer = actualPixelWidth * 0.05;
    double smallToggleWidth;
    double largeToggleWidth;
    double largeToggleFootprint;
    double centralControlsShift;

    @FXML // This method is called by the FXMLLoader when initialization is complete
    /*
      @param url
     * @param resourceBundle
     */
    @Override
    public synchronized void initialize(URL url, ResourceBundle resourceBundle) {
        //This gets us the usable size of the window/screen depends on if full screen (getVisualBounds)
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();

        buttonSizeLarge = pixelWidth / 8;
        buttonSizePanic = pixelWidth / 9;
        buttonSizeML = (int) (buttonSizeLarge * 0.75);
        buttonSizeMedium = (int) (buttonSizeLarge * 0.5);
        buttonSizeSmall = (int) (buttonSizeMedium * 0.75);
        button16 = pixelWidth / 16;
        smallToggleWidth = pixelWidth * 0.17;
        largeToggleWidth = pixelWidth * 0.23;
        largeToggleFootprint = pixelWidth * 0.25;
        centralControlsShift = pixelHeight * 0.47;

        buttonPanicStyle = "-fx-text-fill: WHITE; -fx-background-color: dimgrey;-fx-background-radius: 20%; -fx-pref-width: " + button16 + ";-fx-pref-height: " + button16 + ";";
        buttonTempoStyle = "-fx-text-fill: BLACK;-fx-pref-width: " + buttonSizeMedium + ";-fx-pref-height: " + buttonSizeMedium + ";";
        buttonEnrichmentStyleUnchecked = "-fx-text-fill: WHITE; -fx-background-radius: 20%; -fx-pref-width: " + button16 + ";-fx-pref-height: " + button16 + ";-fx-background-color: dimgrey;";
        buttonEnrichmentStyleChecked = "-fx-text-fill: BLACK; -fx-background-radius: 20%; -fx-pref-width: " + button16 + ";-fx-pref-height: " + button16 + ";-fx-background-color: lemonchiffon;";
        labelActionStyle = "-fx-text-fill: BLACK; -fx-alignment: CENTER; -fx-wrap-text: TRUE;";


        buttonPanic = new Button("Panic");
        buttonPanic.setStyle(buttonPanicStyle);
        buttonPanic.setTranslateX(pixelWidth - (button16));
        buttonPanic.setTranslateY(0);
        buttonPanic.setOnAction(this::buttonPanicHandle);
        buttonPanic.addEventHandler(TouchEvent.TOUCH_PRESSED, touchEvent -> buttonPanic.fire());

        buttonConfirmPanic = new Button("Trigger");
        buttonConfirmPanic.setStyle(buttonPanicStyle + "-fx-background-color: orangered; -fx-wrap-text: TRUE;-fx-text-alignment: CENTER;");
        buttonConfirmPanic.setTranslateX(buttonPanic.getTranslateX());
        buttonConfirmPanic.setTranslateY(buttonPanic.getTranslateY() + button16);
        buttonConfirmPanic.setVisible(false);
        buttonConfirmPanic.setOnAction(this::buttonConfirmPanicHandle);
        buttonConfirmPanic.addEventHandler(TouchEvent.TOUCH_PRESSED, touchEvent -> buttonConfirmPanic.fire());


        buttonEnrichmentArray = new EnrichmentButton[16];
        for (int i = 0; i < buttonEnrichmentArray.length; i++) {
            buttonEnrichmentArray[i] = new EnrichmentButton();
            buttonEnrichmentArray[i].setText(String.valueOf(ArpeggiatorumGUI.controllerHandle.comboEnrichment.getValue().getValue()[i]));
            buttonEnrichmentArray[i].setStyle(buttonEnrichmentStyleUnchecked);
            buttonEnrichmentArray[i].setTranslateX((i * button16));
            buttonEnrichmentArray[i].setTranslateY(pixelHeight - (buttonSizeML + visualVerticalBuffer) - button16);
            buttonEnrichmentArray[i].setOnAction(this::buttonEnrichmentHandle);
            buttonEnrichmentArray[i].addEventHandler(TouchEvent.ANY, this::touchEnrichmentHandle);
            buttonEnrichmentArray[i].addEventHandler(MouseEvent.ANY, this::mouseEnrichmentHandle);

        }
        buttonEnrichmentArray[(int) Math.floor(15.0 * (ArpeggiatorumGUI.controllerHandle.sliderEnrichment.getValue() / 100.0))].fire();

        toggleAudio = new ToggleSwitch();
        toggleAudio.selectedProperty().addListener((observable, oldValue, newValue) -> Arpeggiatorum.getInstance().Activate(newValue));
        boolean audioValue = ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.isSelected();
        if (audioValue) {
            toggleAudio.setSelected(true);
        }

        toggleAudio.setTranslateX(largeToggleFootprint * 3);
        toggleAudio.setTranslateY(-buttonSizeLarge * 0.5);
        toggleAudio.setRotate(-90);
        toggleAudio.getStylesheets().addAll(Objects.requireNonNull(ArpeggiatorumGUI.class.getResource("toggleSwitchSmall.css")).toExternalForm());
        toggleAudio.setStyle(" -preferred-actual-dims: " + smallToggleWidth + ";");
        //TODO find a solution for padding property
//        toggleAudio.setStyle(" -thumb-padding-top: " + pixelWidth * 0.15*0.5 + ";");
//        toggleAudio.setStyle(" -thumb-padding-bottom: " + pixelWidth * 0.15*0.5 + ";");

        labelAudio = new Label("Audio In");
        labelAudio.setStyle(labelActionStyle);
        labelAudio.setMouseTransparent(true);
        labelAudio.setPrefWidth(smallToggleWidth);
        labelAudio.setTranslateX(toggleAudio.getTranslateX());
        labelAudio.setTranslateY(visualVerticalBuffer);


        sliderArticulation = TouchSliderBuilder.create()
                .prefSize(pixelWidth, buttonSizeML)
                .name("Articulation")
                .orientation(Orientation.HORIZONTAL)
                .minValue(50)
                .range(100)
                .sliderValue(ArpeggiatorumGUI.controllerHandle.sliderArticulation.getValue())
                .formatString("%.0f")
                .barBackgroundColor(Color.DIMGREY)
                .barColor(Color.DODGERBLUE)
                .thumbColor(Color.DEEPSKYBLUE)
                .valueTextColor(Color.WHITE)
                .nameTextColor(Color.WHITE)
                .valueVisible(false)
                .nameVisible(true)
                .onTouchSliderEvent(e -> ArpeggiatorumGUI.controllerHandle.sliderArticulation.setValue(e.getValue()))
                .build();
        sliderArticulation.setTranslateX(0);
        sliderArticulation.setTranslateY(pixelHeight - buttonSizeML);


        toggleArpeggio = new ToggleSwitch();
        int arpeggioValue = ArpeggiatorumGUI.controllerHandle.comboArpeggioChannel.getSelectionModel().getSelectedIndex();
        if (arpeggioValue != 0) {
            toggleArpeggio.setSelected(true);
        }
        toggleArpeggio.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                ArpeggiatorumGUI.controllerHandle.comboArpeggioChannel.getSelectionModel().select(ArpeggiatorumGUI.sessionArpeggioChannel+1);
            } else {
                ArpeggiatorumGUI.controllerHandle.comboArpeggioChannel.getSelectionModel().select(0);
            }
        });
        toggleArpeggio.setTranslateX(0);
        toggleArpeggio.setTranslateY(-buttonSizeLarge * 0.5);
        toggleArpeggio.setRotate(-90);
        toggleArpeggio.getStylesheets().addAll(Objects.requireNonNull(ArpeggiatorumGUI.class.getResource("toggleSwitch.css")).toExternalForm());
        toggleArpeggio.setStyle(" -preferred-actual-dims: " + largeToggleWidth + ";");

        labelArpeggio = new Label("Arpeggio");
        labelArpeggio.setStyle(labelActionStyle);
        labelArpeggio.setMouseTransparent(true);
        labelArpeggio.setTranslateX(toggleArpeggio.getTranslateX());
        labelArpeggio.setPrefWidth(largeToggleWidth);
        labelArpeggio.setTranslateY(visualVerticalBuffer);

        toggleSustained = new ToggleSwitch();
        int heldValue = ArpeggiatorumGUI.controllerHandle.comboHeldChannel.getSelectionModel().getSelectedIndex();
        if (heldValue != 0) {
            toggleSustained.setSelected(true);
        }
        toggleSustained.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                ArpeggiatorumGUI.controllerHandle.comboHeldChannel.getSelectionModel().select(ArpeggiatorumGUI.sessionSustainedChannel+1);
            } else {
                ArpeggiatorumGUI.controllerHandle.comboHeldChannel.getSelectionModel().select(0);
            }
        });

        toggleSustained.setTranslateX(largeToggleFootprint);
        toggleSustained.setTranslateY(-buttonSizeLarge * 0.5);
        toggleSustained.getStylesheets().addAll(Objects.requireNonNull(ArpeggiatorumGUI.class.getResource("toggleSwitch.css")).toExternalForm());
        toggleSustained.setRotate(-90);
        toggleSustained.setStyle(" -preferred-actual-dims: " + largeToggleWidth + ";");

        labelSustained = new Label("Sustained");
        labelSustained.setStyle(labelActionStyle);
        labelSustained.setMouseTransparent(true);
        labelSustained.setPrefWidth(largeToggleWidth);
        labelSustained.setTranslateX(toggleSustained.getTranslateX());
        labelSustained.setTranslateY(visualVerticalBuffer);

        toggleBass = new ToggleSwitch();
        int bassValue = ArpeggiatorumGUI.controllerHandle.comboBassChannel.getSelectionModel().getSelectedIndex();
        if (bassValue != 0) {
            toggleBass.setSelected(true);
        }
        toggleBass.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                ArpeggiatorumGUI.controllerHandle.comboBassChannel.getSelectionModel().select(ArpeggiatorumGUI.sessionBassChannel+1);
            } else {
                ArpeggiatorumGUI.controllerHandle.comboBassChannel.getSelectionModel().select(0);
            }
        });
        toggleBass.setTranslateX((largeToggleFootprint * 2));
        toggleBass.setTranslateY(-buttonSizeLarge * 0.5);
        toggleBass.setRotate(-90);
        toggleBass.getStylesheets().addAll(Objects.requireNonNull(ArpeggiatorumGUI.class.getResource("toggleSwitch.css")).toExternalForm());
        toggleBass.setStyle(" -preferred-actual-dims: " + largeToggleWidth + ";");

        labelBass = new Label("Bass");
        labelBass.setStyle(labelActionStyle);
        labelBass.setMouseTransparent(true);
        labelBass.setPrefWidth(largeToggleWidth);
        labelBass.setTranslateX(toggleBass.getTranslateX());
        labelBass.setTranslateY(visualVerticalBuffer);


        radialMenuEnrichment = createCenterRadialMenu("Tonal\r\nEnrichment", ArpeggiatorumGUI.controllerHandle.comboEnrichment.getItems().stream().toList(), enrichmentHandler, bgLg1Color, bgLg2Color, bgMoLg1Color, bgMoLg2Color,-50,-30);
        radialMenuEnrichment.setTranslateX(pixelWidth * 0.165);
        radialMenuEnrichment.setTranslateY(centralControlsShift - visualVerticalBuffer);
        radialMenuEnrichment.showRadialMenu();
        for (RadialMenuItem item : radialMenuEnrichment.getItems()) {
            if (item.getText().equals(ArpeggiatorumGUI.controllerHandle.comboEnrichment.getSelectionModel().getSelectedItem().toString())) {
                item.setMouseOn(true);
                item.setBackgroundColorProperty(bgMoLg1Color);
            }
        }

        radialMenuPattern = createCenterRadialMenu("Pattern", ArpeggiatorumGUI.controllerHandle.comboPattern.getItems().stream().toList(), patternHandler, bgHg1Color, bgHg2Color, bgMoLg1Color, bgMoLg2Color, -31,-13);
        radialMenuPattern.setTranslateX(pixelWidth * 0.835);
        radialMenuPattern.setTranslateY(centralControlsShift - visualVerticalBuffer);
        radialMenuPattern.showRadialMenu();
        for (RadialMenuItem item : radialMenuPattern.getItems()) {
            if (item.getText().equals(ArpeggiatorumGUI.controllerHandle.comboPattern.getSelectionModel().getSelectedItem().toString())) {
                item.setMouseOn(true);
                item.setBackgroundColorProperty(bgMoLg1Color);
            }
        }

        regulatorTempo = RegulatorBuilder.create()
                .prefSize(pixelWidth * 0.33, pixelWidth * 0.33)
                .minValue(ArpeggiatorumGUI.controllerHandle.sliderTempo.getMin())
                .maxValue(ArpeggiatorumGUI.controllerHandle.sliderTempo.getMax())
                .targetValue(ArpeggiatorumGUI.controllerHandle.sliderTempo.getValue())
                .unit(" bpm")
                .barColor(Color.DODGERBLUE)
                .textColor(Color.WHITE)
                .color(Color.DIMGREY)
                .indicatorColor(Color.DEEPSKYBLUE)
                .onTargetSet(e -> ArpeggiatorumGUI.controllerHandle.sliderTempo.adjustValue(regulatorTempo.getTargetValue()))
                .build();
        regulatorTempo.setTranslateX((pixelWidth * 0.5) - (regulatorTempo.getPrefWidth() * 0.5));
        regulatorTempo.setTranslateY(centralControlsShift - (regulatorTempo.getPrefHeight() * 0.5) - visualVerticalBuffer);

        buttonTap = new Button("Tap Tempo");
        buttonTap.setStyle(buttonTempoStyle);
        buttonTap.setTranslateX(pixelWidth * 0.5);
        buttonTap.setTranslateY(pixelHeight * 0.5);
        buttonTap.setOnAction(event -> {
            Arpeggiatorum.getInstance().tapTempo();
            regulatorTempo.setTargetValue(ArpeggiatorumGUI.controllerHandle.sliderTempo.getValue());
        });
        anchorPane.getChildren().addAll(buttonEnrichmentArray);
        anchorPane.getChildren().addAll(toggleAudio,
                sliderArticulation,
                toggleSustained,
                toggleArpeggio,
                toggleBass,
                radialMenuPattern, radialMenuEnrichment,
                regulatorTempo,
                buttonConfirmPanic, buttonPanic
        );
        anchorPane.getChildren().addAll(labelAudio,
                labelSustained,
                labelArpeggio,
                labelBass
        );

        actualPixelHeight = screenBounds.getHeight();
        actualPixelWidth = screenBounds.getWidth();

        double scalingX = actualPixelWidth / pixelWidth;
        double scalingY = actualPixelHeight / pixelHeight;
        double scaling = Math.min(scalingX, scalingY);

        Scale scalingSystem = new Scale(scaling, scaling, 0, 0);
        Translate translateSystem;
        if (scalingX >= scalingY) {
            translateSystem = new Translate((actualPixelWidth - (pixelWidth * scaling)) / 2, 0);

        } else {
            translateSystem = new Translate(0, (actualPixelHeight - (pixelHeight * scaling)) / 2);
        }
        anchorPane.getTransforms().addAll(scalingSystem, translateSystem);
    }


    public RadialMenu createCenterRadialMenu(String menuName, List
            menuItems, EventHandler<ActionEvent> eventHandler, Color color1, Color color2, Color color3, Color color4, int offsetX, int offsetY) {
        LinearGradient background = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, color1), new Stop(0.8, color2));
        LinearGradient backgroundMouseOn = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, color3), new Stop(0.8, color4));

        Label centerLabel = new Label(menuName);
        centerLabel.setStyle("-fx-text-fill: WHITE; -fx-text-alignment: CENTER;");
        //Some magic
        centerLabel.setTranslateX(offsetX);
        centerLabel.setTranslateY(offsetY);
        RadialMenu radialMenu = new RadialMenu(0, buttonSizeSmall, (pixelWidth * 0.33) * 0.5, 1.0,
                background, backgroundMouseOn, strokeColor, strokeMouseOnColor,
                false, RadialMenu.CenterVisibility.ALWAYS, centerLabel);
        //Populate with items
        for (Object element : menuItems) {
            Label itemLabel = new Label(element.toString());
            itemLabel.setStyle("-fx-text-fill: BLACK;-fx-text-alignment: CENTER;");
            radialMenu.addMenuItem(new RadialMenuItem(360.0 / menuItems.size(), element.toString(), itemLabel, eventHandler));
        }
        //Settings for RadialMenu:
        radialMenu.setMenuItemSize(360.0 / menuItems.size());
        radialMenu.requestDraw();
        return radialMenu;
    }


    private synchronized void buttonPanicHandle(ActionEvent event) {
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

    private synchronized void buttonConfirmPanicHandle(ActionEvent actionEvent) {
        if (toggleAudio.isSelected()) {
            toggleAudio.setSelected(false);
        }
        if (ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.isSelected()) {
            ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.fire();
        }
        Arpeggiatorum.getInstance().getArpeggiator().panic();
    }

    private synchronized void touchEnrichmentHandle(TouchEvent touchEvent) {
        EventType<? extends TouchEvent> type = touchEvent.getEventType();
        TouchPoint point = touchEvent.getTouchPoint();
        double x = point.getX();
        double y = point.getY();

        if (touchEvent.getTouchPoint().getPickResult().getIntersectedNode().getClass().equals(EnrichmentButton.class)) {
            ((Button) touchEvent.getTouchPoint().getPickResult().getIntersectedNode()).fire();
        }

    }

    private boolean buttonSelected = false;

    private void mouseEnrichmentHandle(MouseEvent mouseEvent) {
        EventType<? extends MouseEvent> type = mouseEvent.getEventType();
        double x = mouseEvent.getX();
        double y = mouseEvent.getY();

        if (type == MouseEvent.MOUSE_PRESSED) {
            buttonSelected = true;
        }
        if (type == MouseEvent.MOUSE_RELEASED) {
            buttonSelected = false;

        }

        if (buttonSelected) {
            if (mouseEvent.getPickResult().getIntersectedNode().getClass().equals(EnrichmentButton.class)) {
                ((Button) mouseEvent.getPickResult().getIntersectedNode()).fire();
            }
        }
    }

    private void buttonEnrichmentHandle(ActionEvent actionEvent) {
        boolean changeColor = true;
        for (int j = 0; j < buttonEnrichmentArray.length; j++) {
            if (changeColor) {
                buttonEnrichmentArray[j].setStyle(buttonEnrichmentStyleChecked);
            } else {
                buttonEnrichmentArray[j].setStyle(buttonEnrichmentStyleUnchecked);
            }
            if (actionEvent.getSource() == buttonEnrichmentArray[j]) {
                changeColor = false;
                ArpeggiatorumGUI.controllerHandle.sliderEnrichment.setValue(((j + 1) / 16.0) * 100.0);
            }
        }
    }

    public final EventHandler<ActionEvent> patternHandler = new EventHandler<>() {
        @Override
        public synchronized void handle(final ActionEvent paramT) {
            for (RadialMenuItem item : radialMenuPattern.getItems()) {
                item.setBackgroundColorProperty(bgHg1Color);
            }
            final RadialMenuItem item = (RadialMenuItem) paramT.getSource();
            item.setBackgroundColorProperty(bgMoLg1Color);

            ArpeggiatorumGUI.controllerHandle.comboPattern.setValue(NotePool.Pattern.fromString(item.getText()));
        }
    };
    public final EventHandler<ActionEvent> enrichmentHandler = new EventHandler<>() {
        @Override
        public synchronized void handle(final ActionEvent paramT) {
            for (RadialMenuItem item : radialMenuEnrichment.getItems()) {
                item.setBackgroundColorProperty(bgHg1Color);
            }
            final RadialMenuItem item = (RadialMenuItem) paramT.getSource();
            item.setBackgroundColorProperty(bgMoLg1Color);

            for (TonalEnrichmentChooserItem element : ArpeggiatorumGUI.controllerHandle.comboEnrichment.getItems()) {
                if (element.toString().equals(item.getText())) {
                    ArpeggiatorumGUI.controllerHandle.comboEnrichment.setValue(element);
                }
            }
            for (int i = 0; i < buttonEnrichmentArray.length; i++) {
                buttonEnrichmentArray[i].setText(String.valueOf(ArpeggiatorumGUI.controllerHandle.comboEnrichment.getValue().getValue()[i]));
            }
        }
    };

}
