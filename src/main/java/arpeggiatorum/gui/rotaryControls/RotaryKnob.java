//package arpeggiatorum.gui.rotaryControls;
//
//import javafx.beans.property.DoubleProperty;
//import javafx.beans.property.SimpleDoubleProperty;
//import javafx.css.StyleableObjectProperty;
//import javafx.scene.control.Control;
//import javafx.scene.control.Skin;
//import javafx.scene.paint.Color;
//import javafx.scene.paint.Paint;
//
///**
// * @author F-effect
// */
//public class RotaryKnob extends Control {
//    private final Paint DEFAULT_COLOR_1 = Color.BLACK;
//    private final Paint DEFAULT_COLOR_2 = Color.CHARTREUSE;
//    private final Paint DEFAULT_COLOR_3 = Color.SILVER;
//
//    private DoubleProperty maxValue;
//    private final double _maxValue;
//    private DoubleProperty minValue;
//    private final double _minValue;
//    private DoubleProperty value;
//
//    private String maxText;
//    private String minText;
//    private DoubleProperty startAngle;
//    private final double _startAngle;
//    private final double _angleStep;
//    private final double _angleRange;
//
//    private StyleableObjectProperty<Paint> markerColor;
//    private StyleableObjectProperty<Paint> knobFill;
//    private StyleableObjectProperty<Paint> knobStroke;
//    private StyleableObjectProperty<Paint> tickMarkColor;
//
//
//
//    public RotaryKnob() {
//        getStyleClass().add("rotaryknob");
//
//        _minValue = 100;
//        _maxValue = 1000;
//        _startAngle = -45;
//        _angleRange = 270;
//        _angleStep = _angleRange / (_maxValue - _minValue);
//
//        maxText = "1000";
//        minText = "100";
//
//    }
//
//
//    @Override
//    protected Skin<?> createDefaultSkin() {
//        return new RotaryKnobSkin(this);
//    }
//
//    public final void setValue(double val) {
//        if (val <= _minValue) {
//            val = _minValue;
//        } else if (val >= _maxValue) {
//            val = _maxValue;
//        }
//        valueProperty().setValue(val);
//    }
//
//    public final DoubleProperty valueProperty() {
//        if (null == value) {
//            value = new SimpleDoubleProperty(RotaryKnob.this, "value", _minValue);
//        }
//        return value;
//    }
//
//    public final double getValue() {
//        return null == value ? 0 : value.get();
//    }
//
//    public final double getMaxValue() {
//        return null == maxValue ? _maxValue : maxValue.get();
//    }
//
//    public final DoubleProperty maxValueProperty() {
//        if (null == maxValue) {
//            maxValue = new SimpleDoubleProperty(RotaryKnob.this, "maxValue", _maxValue);
//        }
//        return maxValue;
//    }
//
//    public double getMinValue() {
//        return null == minValue ? _minValue : minValue.get();
//    }
//
//    public DoubleProperty minValueProperty() {
//        if (null == minValue) {
//            minValue = new SimpleDoubleProperty(RotaryKnob.this, "minValue", _minValue);
//        }
//        return minValue;
//    }
//
//    public double getStartAngle() {
//
//        return null == startAngle ? _startAngle : startAngle.get();
//    }
//
//    public DoubleProperty startAngleProperty() {
//        if (null == startAngle) {
//            startAngle = new SimpleDoubleProperty(RotaryKnob.this, "startAngle", _startAngle);
//        }
//        return startAngle;
//    }
//
//    public double getAngleStep() {
//        return _angleStep;
//    }
//
//    public final void setMarkerColor(final Paint color) {
//        markerColorProperty().set(color);
//    }
//
//    public final Paint getMarkerColor() {
//        return null == markerColor ? DEFAULT_COLOR_2 : markerColor.get();
//    }
//
//    public final StyleableObjectProperty<Paint> markerColorProperty() {
////            if(null == markerColor){
////                markerColor= CssHelper.createProperty(StyleableProperties.MARKER_COLOR, RotaryKnob.this);
////            }
//        return markerColor;
//    }
//
//    public final void setKnobFill(final Paint color) {
//        knobFillProperty().set(color);
//    }
//
//    public final Paint getKnobFill() {
//        return null == knobFill ? DEFAULT_COLOR_3 : knobFill.get();
//    }
//
//    public final StyleableObjectProperty<Paint> knobFillProperty() {
////            if(null == knobFill){
////                knobFill=CssHelper.createProperty(StyleableProperties.KNOB_FILL, RotaryKnob.this);
////            }
//        return knobFill;
//    }
//
//    public final void setKnobStroke(final Paint color) {
//        knobStrokeProperty().set(color);
//    }
//
//    public final Paint getKnobStroke() {
//        return null == knobStroke ? DEFAULT_COLOR_2 : knobStroke.get();
//    }
//
//    public final StyleableObjectProperty<Paint> knobStrokeProperty() {
////            if(null == knobStroke){
////                knobStroke=CssHelper.createProperty(StyleableProperties.KNOB_STROKE,RotaryKnob.this);
////            }
//        return knobStroke;
//    }
//
//    public final void setTickMarkColor(Paint color) {
//        tickMarkColorProperty().set(color);
//    }
//
//    public final Paint getTickMarkColor() {
//        return null == tickMarkColor ? DEFAULT_COLOR_1 : tickMarkColor.get();
//    }
//
//    public final StyleableObjectProperty<Paint> tickMarkColorProperty() {
////            if(null == tickMarkColor){
////                tickMarkColor=CssHelper.createProperty(StyleableProperties.TICKMARK_COLOR,RotaryKnob.this);
////            }
//        return tickMarkColor;
//    }
//
//    public final void setMaxText(String s) {
//        maxText = s;
//    }
//
//    public final String getMaxText() {
//        return maxText;
//    }
//
//    public final void setMinText(String s) {
//        minText = s;
//    }
//
//    public final String getMinText() {
//        return minText;
//    }
//
////        private static class StyleableProperties {
////            private static final DefaultPropertyBasedCssMetaData<RotaryKnob, Paint> MARKER_COLOR = CssHelper.createMetaData("-fx-knob-marker-color", PaintConverter.getInstance(), "markerColor",Color.AQUA);
////            private static final DefaultPropertyBasedCssMetaData<RotaryKnob, Paint> KNOB_FILL = CssHelper.createMetaData("-fx-knob-fill", PaintConverter.getInstance(), "knobFill",Color.SILVER);
////            private static final DefaultPropertyBasedCssMetaData<RotaryKnob, Paint> KNOB_STROKE = CssHelper.createMetaData("-fx-knob-stroke", PaintConverter.getInstance(), "knobStroke",Color.AQUA);
////            private static final DefaultPropertyBasedCssMetaData<RotaryKnob, Paint> TICKMARK_COLOR = CssHelper.createMetaData("-fx-knob-tick-mark-color", PaintConverter.getInstance(), "tickMarkColor",Color.BLACK);
////
////            private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES = CssHelper.createCssMetaDataList(Control.getClassCssMetaData(),MARKER_COLOR,
////                    KNOB_FILL,KNOB_STROKE,TICKMARK_COLOR);
////        }
////        @Override
////        public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
////            return StyleableProperties.STYLEABLES;
////        }
////        public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
////            return StyleableProperties.STYLEABLES;
////        }
//}
