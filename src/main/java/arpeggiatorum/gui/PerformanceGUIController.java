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
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Screen;
import javafx.util.Duration;

import org.controlsfx.control.ToggleSwitch;

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
    public Color bgLg1Color = Color.DARKKHAKI.deriveColor(1, 1, 1, 1);
    public Color bgLg2Color = Color.DARKKHAKI.deriveColor(1, 1, 1, 1);
    public Color bgHg1Color = Color.DARKKHAKI.deriveColor(1, 1, 1, 1);
    public Color bgHg2Color = Color.DARKKHAKI.deriveColor(1, 1, 1, 1);
    public Color bgMoLg1Color = Color.LEMONCHIFFON.deriveColor(1, 1, 1, 1);
    public Color bgMoLg2Color = Color.LEMONCHIFFON.deriveColor(1, 1, 1, 1);
    public Color strokeColor = Color.BLACK;
    public Color strokeMouseOnColor = Color.BLACK;


    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;
    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    int buttonSizeLarge = 200;
    int buttonSizeML;
    int buttonSizeMedium;
    int buttonSizeSmall;
    int button16;
    int buttonSizePanic;
    int toggleSizeLarge;
    int toggleSizeSmall;

    String buttonPanicStyle;
    String buttonTempoStyle;
    String buttonEnrichmentStyleUnchecked;
    String buttonEnrichmentStyleChecked;
    String labelActionStyle;

    Label labelAudio, labelSustained, labelArpeggio, labelBass;

    int pixelHeight;
    int pixelWidth;
    int visualVerticalBuffer = 0;
    int visualHorizontalBuffer = 24;

    @FXML // This method is called by the FXMLLoader when initialization is complete
    /*
      @param url
     * @param resourceBundle
     */
    @Override
    public synchronized void initialize(URL url, ResourceBundle resourceBundle) {
        //This gets us the usable size of the window/screen depends on if full screen (getVisualBounds)
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        pixelHeight = (int) screenBounds.getHeight();
        pixelWidth = (int) screenBounds.getWidth();

        buttonSizeLarge = pixelWidth / 8;
        buttonSizePanic = pixelWidth / 9;
        toggleSizeLarge = 400;
        toggleSizeSmall = 200;
        buttonSizeML = (int) (buttonSizeLarge * 0.75);
        buttonSizeMedium = (int) (buttonSizeLarge * 0.5);
        buttonSizeSmall = (int) (buttonSizeMedium * 0.75);
        button16 = pixelWidth / 16;

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
            buttonEnrichmentArray[i].setTranslateY(pixelHeight - (buttonSizeMedium + visualVerticalBuffer) - button16);
            buttonEnrichmentArray[i].setOnAction(this::buttonEnrichmentHandle);
            buttonEnrichmentArray[i].addEventHandler(TouchEvent.ANY, this::touchEnrichmentHandle);
        }
        buttonEnrichmentArray[(int) Math.ceil(15.0 * (ArpeggiatorumGUI.controllerHandle.sliderEnrichment.getValue() / 100.0))].fire();

        toggleAudio = new ToggleSwitch();
        boolean audioValue = ArpeggiatorumGUI.controllerHandle.toggleButtonActivate.isSelected();
        if (audioValue) {
            toggleAudio.setSelected(true);
        }
        toggleAudio.selectedProperty().addListener((observable, oldValue, newValue) -> Arpeggiatorum.getInstance().Activate(newValue));
        //Check if touchevents can be handled directly
        //toggleAudio.addEventHandler(TouchEvent.ANY,this::handleAudio);
        toggleAudio.setTranslateX(pixelWidth - button16 - visualHorizontalBuffer - toggleSizeSmall);
        toggleAudio.setTranslateY(-buttonSizeLarge * 0.5);
        toggleAudio.setRotate(-90);
        toggleAudio.getStylesheets().addAll(ArpeggiatorumGUI.class.getResource("toggleSwitchSmall.css").toExternalForm());

        labelAudio = new Label("Audio In");
        labelAudio.setStyle(labelActionStyle);
        labelAudio.setMouseTransparent(true);
        labelAudio.setPrefWidth(toggleSizeLarge * 0.5);
        labelAudio.setTranslateX(toggleAudio.getTranslateX());
        labelAudio.setTranslateY(0);


        sliderArticulation = TouchSliderBuilder.create()
                .prefSize(pixelWidth, buttonSizeMedium)
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
                .onTouchSliderEvent(e -> ArpeggiatorumGUI.controllerHandle.sliderArticulation.adjustValue(e.getValue()))
                .build();
        sliderArticulation.setTranslateX(0);
        sliderArticulation.setTranslateY(pixelHeight - buttonSizeMedium);


        toggleSustained = new ToggleSwitch();
        int heldValue = ArpeggiatorumGUI.controllerHandle.comboHeldChannel.getSelectionModel().getSelectedIndex();
        if (heldValue != 0) {
            toggleSustained.setSelected(true);
        }
        toggleSustained.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                ArpeggiatorumGUI.controllerHandle.comboHeldChannel.getSelectionModel().select(heldValue);
            } else {
                ArpeggiatorumGUI.controllerHandle.comboHeldChannel.getSelectionModel().select(0);
            }
        });

        //toggleSustained.setTranslateX(pixelWidth - ((buttonSizeLarge * 3) + (visualHorizontalBuffer * 2)));
        toggleSustained.setTranslateX(0);
        toggleSustained.setTranslateY(-buttonSizeLarge * 0.5);
        toggleSustained.getStylesheets().addAll(ArpeggiatorumGUI.class.getResource("toggleSwitch.css").toExternalForm());
        toggleSustained.setRotate(-90);

        labelSustained = new Label("Sustained");
        labelSustained.setStyle(labelActionStyle);
        labelSustained.setMouseTransparent(true);
        labelSustained.setPrefWidth(toggleSizeLarge * 1);
        labelSustained.setTranslateX(toggleSustained.getTranslateX());
        labelSustained.setTranslateY(0);

        toggleArpeggio = new ToggleSwitch();
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
//        toggleArpeggio.setTranslateX(pixelWidth - ((buttonSizeLarge * 2) + (visualHorizontalBuffer * 1)));
        toggleArpeggio.setTranslateX(toggleSizeLarge + (visualHorizontalBuffer * 1));
        toggleArpeggio.setTranslateY(-buttonSizeLarge * 0.5);
        toggleArpeggio.setRotate(-90);
        toggleArpeggio.getStylesheets().addAll(ArpeggiatorumGUI.class.getResource("toggleSwitch.css").toExternalForm());

        labelArpeggio = new Label("Arpeggio");
        labelArpeggio.setStyle(labelActionStyle);
        labelArpeggio.setMouseTransparent(true);
        labelArpeggio.setTranslateX(toggleArpeggio.getTranslateX());
        labelArpeggio.setPrefWidth(toggleSizeLarge * 1);
        labelArpeggio.setTranslateY(0);

        toggleBass = new ToggleSwitch();
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
        toggleBass.setTranslateX((toggleSizeLarge * 2) + (visualHorizontalBuffer * 2));
        toggleBass.setTranslateY(-buttonSizeLarge * 0.5);
        toggleBass.setRotate(-90);

        toggleBass.getStylesheets().addAll(ArpeggiatorumGUI.class.getResource("toggleSwitch.css").toExternalForm());

        labelBass = new Label("Bass");
        labelBass.setStyle(labelActionStyle);
        labelBass.setMouseTransparent(true);
        labelBass.setPrefWidth(toggleSizeLarge * 1);
        labelBass.setTranslateX(toggleBass.getTranslateX());
        labelBass.setTranslateY(0);


        radialMenuEnrichment = createCenterRadialMenu("Tonal\r\nEnrichment", ArpeggiatorumGUI.controllerHandle.comboEnrichment.getItems().stream().toList(), enrichmentHandler, bgLg1Color, bgLg2Color, bgMoLg1Color, bgMoLg2Color);
        radialMenuEnrichment.setTranslateX(pixelWidth * 0.17);
        radialMenuEnrichment.setTranslateY(pixelHeight * 0.475 - visualVerticalBuffer);
        radialMenuEnrichment.showRadialMenu();
        for (RadialMenuItem item : radialMenuEnrichment.getItems()) {
            if (item.getText().equals(ArpeggiatorumGUI.controllerHandle.comboEnrichment.getSelectionModel().getSelectedItem().toString())) {
                item.setStyle("-fx-font-weight: BOLD;");
                item.setMouseOn(true);
                item.setBackgroundColorProperty(bgMoLg1Color);
            }
        }

        //actionPerformedLabelEnrichment.setTranslateX(pixelWidth * 0.25);
        //actionPerformedLabelEnrichment.setTranslateY(pixelHeight * 0.8);
        //actionPerformedLabelEnrichment.setStyle(labelActionStyle);
        //actionPerformedLabelEnrichment.setText(ArpeggiatorumGUI.controllerHandle.comboEnrichment.getSelectionModel().getSelectedItem().toString());

        radialMenuPattern = createCenterRadialMenu("   Pattern", ArpeggiatorumGUI.controllerHandle.comboPattern.getItems().stream().toList(), patternHandler, bgHg1Color, bgHg2Color, bgMoLg1Color, bgMoLg2Color);
        radialMenuPattern.setTranslateX(pixelWidth * 0.83);
        radialMenuPattern.setTranslateY(pixelHeight * 0.475 - visualVerticalBuffer);
        radialMenuPattern.showRadialMenu();
        for (RadialMenuItem item : radialMenuPattern.getItems()) {
            if (item.getText().equals(ArpeggiatorumGUI.controllerHandle.comboPattern.getSelectionModel().getSelectedItem().toString())) {
                item.setStyle("-fx-font-weight: BOLD;");
                item.setMouseOn(true);
                item.setBackgroundColorProperty(bgMoLg1Color);
            }
        }

        // actionPerformedLabelPattern.setTranslateX(pixelWidth * 0.55);
        // actionPerformedLabelPattern.setTranslateY(pixelHeight * 0.8);
        // actionPerformedLabelPattern.setStyle(labelActionStyle);
        //actionPerformedLabelPattern.setText(ArpeggiatorumGUI.controllerHandle.comboPattern.getSelectionModel().getSelectedItem().toString());


        regulatorTempo = RegulatorBuilder.create()
                .prefSize(buttonSizeLarge * 2.6, buttonSizeLarge * 2.6)
                .minValue(ArpeggiatorumGUI.controllerHandle.sliderTempo.getMin())
                .maxValue(ArpeggiatorumGUI.controllerHandle.sliderTempo.getMax())
                .targetValue(ArpeggiatorumGUI.controllerHandle.sliderTempo.getValue())
                .unit(" bpm")
                .barColor(Color.DODGERBLUE)
                .textColor(Color.WHITE)
                //.icon(FontAwesome.MUSIC)
                //.iconColor(Color.WHITE)
                .color(Color.DIMGREY)
                .indicatorColor(Color.DEEPSKYBLUE)
                .onTargetSet(e -> ArpeggiatorumGUI.controllerHandle.sliderTempo.adjustValue(regulatorTempo.getTargetValue()))
                .build();
        regulatorTempo.setTranslateX((pixelWidth * 0.5) - (regulatorTempo.getPrefWidth() * 0.5));
        regulatorTempo.setTranslateY((pixelHeight * 0.475) - (regulatorTempo.getPrefHeight() * 0.5) - visualVerticalBuffer);

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
                // actionPerformedLabelPattern, actionPerformedLabelEnrichment,
                regulatorTempo,
                buttonConfirmPanic, buttonPanic
        );
        anchorPane.getChildren().addAll(labelAudio,
                labelSustained,
                labelArpeggio,
                labelBass
        );


    }

//    private void handleAudio(TouchEvent touchEvent) {
//        if (touchEvent.getEventType()==TouchEvent.TOUCH_PRESSED || touchEvent.getEventType()==TouchEvent.TOUCH_RELEASED) {
//        toggleAudio.fire();
//        }
//    }


    public RadialMenu createCenterRadialMenu(String menuName, List
            menuItems, EventHandler<ActionEvent> eventHandler, Color color1, Color color2, Color color3, Color color4) {
        LinearGradient background = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, color1), new Stop(0.8, color2));
        LinearGradient backgroundMouseOn = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, color3), new Stop(0.8, color4));

        Label centerLabel = new Label(menuName);
        centerLabel.setStyle("-fx-text-fill: WHITE; -fx-text-alignment: CENTER;");
        //Some magic
        centerLabel.setTranslateX(-50);
        centerLabel.setTranslateY(-30);
        RadialMenu radialMenu = new RadialMenu(0, buttonSizeSmall * 1.0, buttonSizeLarge * 1.30, 1.0,
                background, backgroundMouseOn, strokeColor, strokeMouseOnColor,
                false, RadialMenu.CenterVisibility.ALWAYS, centerLabel);
        //radialMenu.setStrokeWidth(STROKE_WIDTH);
        //radialMenu.setOutlineStrokeWidth(STROKE_WIDTH);
        //radialMenu.setOutlineStrokeFill(outlineColor);
        //radialMenu.setOutlineStrokeMouseOnFill(outlineMouseOnColor);
        //Populate with items
        for (Object element : menuItems) {
            Label itemLabel = new Label(element.toString());
            itemLabel.setStyle("-fx-text-fill: BLACK;-fx-text-alignment: CENTER;");
            //Some magic
//            itemLabel.setTranslateX(-50);
//            itemLabel.setTranslateY(-15);
            radialMenu.addMenuItem(new RadialMenuItem(360.0 / menuItems.size(), element.toString(), itemLabel, eventHandler));
        }
        //Settings for RadialMenu:
        radialMenu.setMenuItemSize(360.0 / menuItems.size());
        //radialMenu.setInnerRadius(buttonSizeSmall * 1.0);
        //radialMenu.setGraphicsFitWidth(0);
        //radialMenu.setRadius(buttonSizeLarge * 1.30);
        //radialMenu.setOffset(1.0);
        //radialMenu.setInitialAngle(0.0);
        // radialMenu.setStrokeWidth(1.5);

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

    private void buttonEnrichmentHandle(ActionEvent actionEvent) {
        Boolean changeColor = true;
        for (int j = 0; j < buttonEnrichmentArray.length; j++) {
            if (changeColor) {
                buttonEnrichmentArray[j].setStyle(buttonEnrichmentStyleChecked);
            } else {
                buttonEnrichmentArray[j].setStyle(buttonEnrichmentStyleUnchecked);
            }
            if (actionEvent.getSource() == buttonEnrichmentArray[j]) {
                changeColor = false;
                ArpeggiatorumGUI.controllerHandle.sliderEnrichment.adjustValue((j / 15.0) * 100.0);
            }
        }
    }

    public final EventHandler<ActionEvent> patternHandler = new EventHandler<>() {
        @Override
        public synchronized void handle(final ActionEvent paramT) {
            for (RadialMenuItem item : radialMenuPattern.getItems()) {
                item.setStyle("-fx-font-weight: normal;");
                item.setBackgroundColorProperty(bgHg1Color);

            }
            final RadialMenuItem item = (RadialMenuItem) paramT.getSource();
            item.setStyle("-fx-font-weight: bold;");
            item.setBackgroundColorProperty(bgMoLg1Color);

            ArpeggiatorumGUI.controllerHandle.comboPattern.setValue(NotePool.Pattern.fromString(item.getText()));
            // actionPerformedLabelPattern.setText(ArpeggiatorumGUI.controllerHandle.comboPattern.getSelectionModel().getSelectedItem().toString());

        }
    };
    public final EventHandler<ActionEvent> enrichmentHandler = new EventHandler<>() {
        @Override
        public synchronized void handle(final ActionEvent paramT) {
            for (RadialMenuItem item : radialMenuEnrichment.getItems()) {
                item.setStyle("-fx-font-weight: normal;");
                item.setBackgroundColorProperty(bgHg1Color);
            }
            final RadialMenuItem item = (RadialMenuItem) paramT.getSource();
            item.setStyle("-fx-font-weight: bold");
            item.setBackgroundColorProperty(bgMoLg1Color);
            for (TonalEnrichmentChooserItem element : ArpeggiatorumGUI.controllerHandle.comboEnrichment.getItems()) {
                if (element.toString().equals(item.getText())) {
                    ArpeggiatorumGUI.controllerHandle.comboEnrichment.setValue(element);
                }
            }
            for (int i = 0; i < buttonEnrichmentArray.length; i++) {
                buttonEnrichmentArray[i].setText(String.valueOf(ArpeggiatorumGUI.controllerHandle.comboEnrichment.getValue().getValue()[i]));
            }
            //actionPerformedLabelEnrichment.setText(ArpeggiatorumGUI.controllerHandle.comboEnrichment.getSelectionModel().getSelectedItem().toString());
        }
    };

}
