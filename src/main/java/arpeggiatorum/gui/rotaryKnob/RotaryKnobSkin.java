package arpeggiatorum.gui.rotaryKnob;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

/**
 * @author F-effect
 */
public class RotaryKnobSkin extends SkinBase<RotaryKnob> {

    private static final double PREFERRED_WIDTH = 300;
    private static final double PREFERRED_HEIGHT = 300;
    private static final double MINIMUM_WIDTH = 50;
    private static final double MINIMUM_HEIGHT = 50;
    private static final double MAXIMUM_WIDTH = 1024;
    private static final double MAXIMUM_HEIGHT = 1024;

    double sinValue;
    double cosValue;
    double center_radiusX;
    double center_radiusY;
    double startAngle;
    double angleStep;
    double centerX;
    double centerY;
    double radius;
    double valueAngle;

    private Canvas canvas;
    private GraphicsContext gc;
    private Label minLabel, maxLabel, valueLabel;

    private Button rotator_dial;
    private final int roundingFactor = 100;
    private final DoubleProperty rotation = new SimpleDoubleProperty();

    public final DoubleProperty rotationProperty() {
        return this.rotation;
    }

    public final double getRotationProperty() {
        return this.rotation.get();
    }

    public final void setRotationProperty(double value) {
        this.rotation.set(value);
    }

    public RotaryKnobSkin(RotaryKnob knob) {
        super(knob);
        init();
        initGraphics();
        registerListeners();
    }

    @Override
    public void dispose() {
        getChildren().clear();
        super.dispose();
    }

    private void init() {
        if (Double.compare(getSkinnable().getPrefWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getPrefHeight(), 0.0) <= 0 ||
                Double.compare(getSkinnable().getWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getHeight(), 0.0) <= 0) {
            if (getSkinnable().getPrefWidth() < 0 && getSkinnable().getPrefHeight() < 0) {
                getSkinnable().setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }
        if (Double.compare(getSkinnable().getMinWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getMinHeight(), 0.0) <= 0) {
            getSkinnable().setMinSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        }

        if (Double.compare(getSkinnable().getMaxWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getMaxHeight(), 0.0) <= 0) {
            getSkinnable().setMaxSize(MAXIMUM_WIDTH, MAXIMUM_HEIGHT);
        }

        rotator_dial = new Button("");
        rotator_dial.setStyle("-fx-min-width: 30; -fx-max-width: 30; -fx-min-height: 30; -fx-min-height: 30; -fx-background-radius: 100; -fx-background-color: CHARTREUSE");
        rotator_dial.setMnemonicParsing(false);
        rotator_dial.setOnMouseDragged(this::handle);
        rotator_dial.setOnMousePressed(this::handle);
    }

    private void initGraphics() {

        startAngle = getSkinnable().getStartAngle();
        angleStep = getSkinnable().getAngleStep();
        centerX = getSkinnable().getPrefWidth() * 0.5;
        centerY = getSkinnable().getPrefHeight() * 0.5;
        radius = getSkinnable().getPrefWidth() * 0.02;

        canvas = new Canvas(getSkinnable().getPrefHeight() + radius, getSkinnable().getPrefWidth() + radius);
        gc = canvas.getGraphicsContext2D();


//        for (double angle = 0, counter = 0; counter <= getSkinnable().getMaxValue() ; angle -=angleStep, counter++) {
//
//            sinValue = Math.sin(Math.toRadians(angle + startAngle));
//            cosValue = Math.cos(Math.toRadians(angle + startAngle));
//
//            center_radiusX = (centerX  * sinValue) + centerX;
//            center_radiusY = (centerY  * cosValue) + centerY;
//
//            gc.fillOval(center_radiusX, center_radiusY, radius,radius);
//            gc.setFill(getSkinnable().getTickMarkColor());
//        }

        drawKnobStroke(centerX, centerY);
        drawKnob(centerX, centerY);

        drawMarkerPoint(gc);
        //drawMarkerTickMarks(gc);

        drawMaxLabel(gc);
        drawMinLabel(gc);

        drawValueText(gc);

        StackPane test = new StackPane();
        test.getChildren().addAll(rotator_dial);
        getChildren().addAll(canvas, test);
    }

    private void rotate(Double value) {
        double rounded = this.round(value, 100);
        this.rotation.set(rounded);
    }

    private double round(double value, int roundingFactor) {
        double doubleRounded = (double) Math.round(value * (double) roundingFactor);
        return doubleRounded / (double) roundingFactor;
    }

    private void registerListeners() {
        getSkinnable().valueProperty().addListener(e -> {
            getChildren().clear();
            initGraphics();
        });
//        this.rotator_dial.setOnAction((event) -> {
//            event.consume();
//        });
    }

    //Draws outer circle
    private void drawKnobStroke(double x, double y) {
        Circle knobStroke = new Circle(x * 0.88);
        knobStroke.setStrokeWidth(x * 0.09);
        knobStroke.setStroke(getSkinnable().getKnobStroke());
        knobStroke.setFill(Color.TRANSPARENT);
        knobStroke.setLayoutX(x);
        knobStroke.setLayoutY(y);

        getChildren().addAll(knobStroke);
    }

    //This draws the center (big silver)
    private void drawKnob(double x, double y) {
        Circle knobStroke = new Circle(x * 0.81);
        knobStroke.setFill(getSkinnable().getKnobFill());
        knobStroke.setLayoutX(x);
        knobStroke.setLayoutY(y);
        getChildren().addAll(knobStroke);
    }

    //This method draws the handle point (tiny dot)
    private void drawMarkerPoint(GraphicsContext gc) {

        startAngle = getSkinnable().getStartAngle();
        valueAngle = startAngle - (getSkinnable().getValue() - getSkinnable().getMinValue()) * getSkinnable().getAngleStep();

        radius = getSkinnable().getPrefWidth() * 0.035;
        double _radius = getSkinnable().getPrefWidth() * 0.034;

        sinValue = Math.sin(Math.toRadians(valueAngle));
        cosValue = Math.cos(Math.toRadians(valueAngle));

        centerX = getSkinnable().getPrefWidth() * 0.5;
        centerY = getSkinnable().getPrefHeight() * 0.5;

        center_radiusX = (centerX * 0.75) * sinValue + centerX;
        center_radiusY = (centerY * 0.75) * cosValue + centerY;

//        gc.setFill(getSkinnable().getMarkerColor());
//        gc.fillOval(center_radiusX, center_radiusY, _radius, _radius);
//        gc.setStroke(Color.BLACK);
//        gc.strokeOval(center_radiusX, center_radiusY, radius, radius);
//        //Unknown Offset
        rotator_dial.setTranslateX(centerX * 0.5);
        rotator_dial.setTranslateY(centerY * 0.5);
    }

    private void drawMarkerTickMarks(GraphicsContext gc) {

        startAngle = getSkinnable().getStartAngle();
        angleStep = getSkinnable().getAngleStep();
        centerX = getSkinnable().getPrefWidth() * 0.5;
        centerY = getSkinnable().getPrefHeight() * 0.5;
        radius = getSkinnable().getPrefWidth() * 0.018;

        for (double angle = 0, counter = 0; counter <= getSkinnable().getValue(); angle -= angleStep, counter++) {

            sinValue = Math.sin(Math.toRadians(angle + startAngle));
            cosValue = Math.cos(Math.toRadians(angle + startAngle));

            center_radiusX = (centerX * sinValue) + centerX;
            center_radiusY = (centerY * cosValue) + centerY;

            gc.fillOval(center_radiusX, center_radiusY, radius, radius);
            gc.setFill(getSkinnable().getMarkerColor());
        }

    }

    private void drawMinLabel(GraphicsContext gc) {

        centerX = getSkinnable().getPrefWidth() * 0.5;
        centerY = getSkinnable().getPrefHeight() * 0.56;

        startAngle = getSkinnable().getStartAngle();
        valueAngle = startAngle - ((int) getSkinnable().getMinValue() - getSkinnable().getMinValue()) * getSkinnable().getAngleStep();

        sinValue = Math.sin(Math.toRadians(valueAngle));
        cosValue = Math.cos(Math.toRadians(valueAngle));

        center_radiusX = (centerX * sinValue) + centerX;
        center_radiusY = (centerY * cosValue) + centerY;

        gc.setFont(Font.font(getSkinnable().getPrefWidth() / 25));
        gc.strokeText(getSkinnable().getMinText(), center_radiusX, center_radiusY);
    }

    private void drawMaxLabel(GraphicsContext gc) {

        centerX = getSkinnable().getPrefWidth() * 0.5;
        centerY = getSkinnable().getPrefHeight() * 0.56;

        startAngle = getSkinnable().getStartAngle();
        valueAngle = startAngle - ((int) getSkinnable().getMaxValue() - getSkinnable().getMinValue()) * getSkinnable().getAngleStep();

        sinValue = Math.sin(Math.toRadians(valueAngle));
        cosValue = Math.cos(Math.toRadians(valueAngle));

        center_radiusX = (centerX * sinValue) + centerX;
        center_radiusY = (centerY * cosValue) + centerY;

        gc.setFont(Font.font(getSkinnable().getPrefWidth() / 25));
//        maxLabel= new Label(getSkinnable().getMaxText());
//        maxLabel.setFont(Font.font(getSkinnable().getPrefWidth() / 25));
//        maxLabel.setTranslateX(center_radiusX);
//        maxLabel.setTranslateY(center_radiusY);
//        getChildren().add(maxLabel);
        gc.strokeText(getSkinnable().getMaxText(), center_radiusX, center_radiusY);
    }

    private void drawValueText(GraphicsContext gc) {
        centerX = getSkinnable().getPrefWidth() * 0.45;
        centerY = getSkinnable().getPrefHeight() * 0.45;

        gc.setFont(Font.font(getSkinnable().getPrefWidth() / 10));
        gc.strokeText(String.valueOf((int) getSkinnable().getValue()), centerX, centerY);

    }

    private void handle(MouseEvent mouseEvent) {
        Parent p = rotator_dial.getParent();
        Bounds b = rotator_dial.getLayoutBounds();
        Double centerX = b.getMinX() + b.getWidth() / 2.0;
        Double centerY = b.getMinY() + b.getHeight() / 2.0;
        Point2D center = p.localToParent(centerX, centerY);
        Point2D mouse = p.localToParent(mouseEvent.getX(), mouseEvent.getY());
        Double deltaX = mouse.getX() - center.getX();
        Double deltaY = mouse.getY() - center.getY();
        Double radians = Math.atan2(deltaY, deltaX);
        p.setRotate(this.round(Math.toDegrees(radians),100));
    }
}
