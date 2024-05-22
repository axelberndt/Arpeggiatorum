package arpeggiatorum.gui.regulators;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Locale;


public class Regulator extends Region implements RegulatorControl {
    private static final Color DEFAULT_COLOR = Color.GAINSBORO;
    private static final double PREFERRED_WIDTH = 250;
    private static final double PREFERRED_HEIGHT = 250;
    private static final double MINIMUM_WIDTH = 50;
    private static final double MINIMUM_HEIGHT = 50;
    private static final double MAXIMUM_WIDTH = 1024;
    private static final double MAXIMUM_HEIGHT = 1024;
    private static final double BAR_START_ANGLE = 270;
    private static final double ANGLE_RANGE = 360;
    private final RegulatorEvent TARGET_SET_EVENT = new RegulatorEvent(RegulatorEvent.TARGET_SET);
    private double size;
    private Arc barArc;
    private Shape ring;
    private Circle mainCircle;
    private Text text;
    private Circle indicator;
    private Region symbol;
    private StackPane iconPane;
    private FontIcon icon;
    private Pane pane;
    private DropShadow dropShadow;
    private InnerShadow highlight;
    private InnerShadow innerShadow;
    private DropShadow indicatorGlow;
    private InnerShadow indicatorInnerShadow;
    private InnerShadow indicatorHighlight;
    private Rotate indicatorRotate;
    private double scaleFactor;
    private DoubleProperty minValue;
    private DoubleProperty maxValue;
    private DoubleProperty targetValue;
    private IntegerProperty decimals;
    private StringProperty unit;
    private ObjectProperty<Color> symbolColor;
    private ObjectProperty<Color> iconColor;
    private ObjectProperty<Color> textColor;
    private ObjectProperty<Color> barColor;
    private ObjectProperty<Color> color;
    private ObjectProperty<Color> indicatorColor;
    private BooleanProperty selected;
    private String formatString;
    private double angleStep;


    // ******************** Constructors **************************************
    public Regulator() {
        getStylesheets().add(Regulator.class.getResource("regulator.css").toExternalForm());
        scaleFactor = 1.0;
        minValue = new DoublePropertyBase(0) {
            @Override
            public void set(final double VALUE) {
                super.set(clamp(-Double.MAX_VALUE, maxValue.get(), VALUE));
                angleStep = ANGLE_RANGE / (maxValue.get() - minValue.get());
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return "minValue";
            }
        };
        maxValue = new DoublePropertyBase(100) {
            @Override
            public void set(final double VALUE) {
                super.set(clamp(minValue.get(), Double.MAX_VALUE, VALUE));
                angleStep = ANGLE_RANGE / (maxValue.get() - minValue.get());
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return "maxValue";
            }
        };
        targetValue = new DoublePropertyBase(0) {
            @Override
            public void set(final double VALUE) {
                super.set(clamp(minValue.get(), maxValue.get(), VALUE));
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return "targetValue";
            }
        };
        decimals = new IntegerPropertyBase(0) {
            @Override
            public void set(final int VALUE) {
                super.set(clamp(0, 2, VALUE));
                formatString = new StringBuilder("%.").append(Integer.toString(decimals.get())).append("f").append(getUnit()).toString();
                redraw();
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return "decimals";
            }
        };
        unit = new StringPropertyBase("") {
            @Override
            public void set(final String VALUE) {
                super.set(VALUE.equals("%") ? "%%" : VALUE);
                formatString = new StringBuilder("%.").append(Integer.toString(decimals.get())).append("f").append(get()).toString();
                redraw();
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return "unit";
            }
        };
        symbolColor = new ObjectPropertyBase<Color>(Color.TRANSPARENT) {
            @Override
            protected void invalidated() {
                set(null == get() ? Color.WHITE : get());
                redraw();
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return "symbolColor";
            }
        };
        iconColor = new ObjectPropertyBase<Color>(Color.TRANSPARENT) {
            @Override
            protected void invalidated() {
                set(null == get() ? Color.WHITE : get());
                redraw();
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return "iconColor";
            }
        };
        textColor = new ObjectPropertyBase<Color>(Color.WHITE) {
            @Override
            protected void invalidated() {
                set(null == get() ? Color.WHITE : get());
                redraw();
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return "textColor";
            }
        };
        barColor = new ObjectPropertyBase<Color>(Color.CYAN) {
            @Override
            protected void invalidated() {
                super.set(null == get() ? Color.CYAN : get());
                redraw();
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return "barColor";
            }
        };
        color = new ObjectPropertyBase<Color>(DEFAULT_COLOR) {
            @Override
            protected void invalidated() {
                super.set(null == get() ? DEFAULT_COLOR : get());
                redraw();
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return "color";
            }
        };
        indicatorColor = new ObjectPropertyBase<Color>(Color.WHITE) {
            @Override
            protected void invalidated() {
                indicatorGlow.setColor(get());
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return "indicatorColor";
            }
        };
        selected = new BooleanPropertyBase(false) {
            @Override
            protected void invalidated() {
                if (get()) {
                    indicator.setFill(getIndicatorColor());
                    indicator.setStroke(getIndicatorColor().darker().darker());
                    indicator.setEffect(indicatorGlow);
                } else {
                    indicator.setFill(getColor().darker());
                    indicator.setStroke(getColor().darker().darker());
                    indicator.setEffect(null);
                }
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return "selected";
            }
        };
        formatString = new StringBuilder("%.").append(Integer.toString(decimals.get())).append("f").append(unit.get()).toString();
        angleStep = ANGLE_RANGE / (maxValue.get() - minValue.get());
        init();
        initGraphics();
        registerListeners();
    }


    // ******************** Initialization ************************************
    private void init() {
        if (Double.compare(getPrefWidth(), 0.0) <= 0 || Double.compare(getPrefHeight(), 0.0) <= 0 ||
                Double.compare(getWidth(), 0.0) <= 0 || Double.compare(getHeight(), 0.0) <= 0) {
            if (getPrefWidth() > 0 && getPrefHeight() > 0) {
                setPrefSize(getPrefWidth(), getPrefHeight());
            } else {
                setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }
        if (Double.compare(getMinWidth(), 0.0) <= 0 || Double.compare(getMinHeight(), 0.0) <= 0) {
            setMinSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        }
        if (Double.compare(getMaxWidth(), 0.0) <= 0 || Double.compare(getMaxHeight(), 0.0) <= 0) {
            setMaxSize(MAXIMUM_WIDTH, MAXIMUM_HEIGHT);
        }
    }

    private void initGraphics() {
//        dropShadow  = new DropShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.65), PREFERRED_WIDTH * 0.016, 0.0, 0, PREFERRED_WIDTH * 0.028);
//        highlight   = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(255, 255, 255, 0.2), PREFERRED_WIDTH * 0.008, 0.0, 0, PREFERRED_WIDTH * 0.008);
//        innerShadow = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.2), PREFERRED_WIDTH * 0.008, 0.0, 0, -PREFERRED_WIDTH * 0.008);
        dropShadow = new DropShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.0), PREFERRED_WIDTH * 0.016, 0.0, 0, PREFERRED_WIDTH * 0.028);
        highlight = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(255, 255, 255, 0.0), PREFERRED_WIDTH * 0.008, 0.0, 0, PREFERRED_WIDTH * 0.008);
        innerShadow = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.0), PREFERRED_WIDTH * 0.008, 0.0, 0, -PREFERRED_WIDTH * 0.008);
        highlight.setInput(innerShadow);
        dropShadow.setInput(highlight);

        barArc = new Arc(PREFERRED_WIDTH * 0.5, PREFERRED_HEIGHT * 0.5, PREFERRED_WIDTH * 0.46, PREFERRED_HEIGHT * 0.46, BAR_START_ANGLE, 0);
        barArc.setType(ArcType.OPEN);
        barArc.setStrokeLineCap(StrokeLineCap.ROUND);
        barArc.setFill(null);
        barArc.setStroke(barColor.get());

        double center = PREFERRED_WIDTH * 0.5;
//        ring = Shape.subtract(new Circle(center, center, PREFERRED_WIDTH * 0.42),
//                new Circle(center, center, PREFERRED_WIDTH * 0.3));
//        ring.setFill(color.get());
//        ring.setEffect(dropShadow);

        mainCircle = new Circle();
        mainCircle.setFill(color.get().darker().darker());

        text = new Text(String.format(Locale.US, formatString, getTargetValue()));
        text.setFill(Color.WHITE);
        text.setTextOrigin(VPos.CENTER);

        indicatorRotate = new Rotate(-ANGLE_RANGE * 0.5, center, center);

        indicatorGlow = new DropShadow(BlurType.TWO_PASS_BOX, getIndicatorColor(), PREFERRED_WIDTH * 0.02, 0.0, 0, 0);
//        indicatorInnerShadow = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.5), PREFERRED_WIDTH * 0.008, 0.0, 0, PREFERRED_WIDTH * 0.008);
//        indicatorHighlight   = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(255, 255, 255, 0.35), PREFERRED_WIDTH * 0.008, 0.0, 0, -PREFERRED_WIDTH * 0.008);
        indicatorInnerShadow = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.0), PREFERRED_WIDTH * 0.008, 0.0, 0, PREFERRED_WIDTH * 0.008);
        indicatorHighlight = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(255, 255, 255, 0.0), PREFERRED_WIDTH * 0.008, 0.0, 0, -PREFERRED_WIDTH * 0.008);

        indicatorHighlight.setInput(indicatorInnerShadow);


        indicator = new Circle();
        indicator.setFill(color.get().darker());
        indicator.setStroke(color.get().darker().darker());
        indicator.setMouseTransparent(true);
        indicator.getTransforms().add(indicatorRotate);

        Group indicatorGroup = new Group(indicator);
        indicatorGroup.setEffect(indicatorHighlight);

        symbol = new Region();
        symbol.getStyleClass().setAll("symbol");
        symbol.setCacheHint(CacheHint.SPEED);

        icon = new FontIcon();
        icon.setTextOrigin(VPos.CENTER);

        //iconPane = new StackPane(symbol, icon);

        pane = new Pane(barArc,
               // ring,
                mainCircle,
                text,
                indicatorGroup
                //iconPane
                );
        pane.setPrefSize(PREFERRED_HEIGHT, PREFERRED_HEIGHT);
        pane.setBackground(new Background(new BackgroundFill(color.get().darker(), new CornerRadii(1024), Insets.EMPTY)));
        pane.setEffect(highlight);

        getChildren().setAll(pane);
    }

    private void registerListeners() {
        widthProperty().addListener(o -> resize());
        heightProperty().addListener(o -> resize());
        disabledProperty().addListener(o -> setOpacity(isDisabled() ? 0.4 : 1.0));
        targetValueProperty().addListener(o -> rotate(targetValue.get()));
        mainCircle.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (isDisabled()) return;
            touchRotate(e.getSceneX(), e.getSceneY());
        });
        mainCircle.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (isDisabled()) return;
            touchRotate(e.getSceneX(), e.getSceneY());
        });
        mainCircle.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (isDisabled()) return;
            fireEvent(TARGET_SET_EVENT);
        });
        pane.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (isDisabled()) return;
            touchRotate(e.getSceneX(), e.getSceneY());
        });
        pane.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (isDisabled()) return;
            touchRotate(e.getSceneX(), e.getSceneY());
        });
        pane.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (isDisabled()) return;
            fireEvent(TARGET_SET_EVENT);
        });
    }


    // ******************** Methods *******************************************
    public double getMinValue() {
        return minValue.get();
    }

    public void setMinValue(final double VALUE) {
        minValue.set(VALUE);
    }

    public DoubleProperty minValueProperty() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue.get();
    }

    public void setMaxValue(final double VALUE) {
        maxValue.set(VALUE);
    }

    public DoubleProperty maxValueProperty() {
        return maxValue;
    }

    @Override
    public double getTargetValue() {
        return targetValue.get();
    }

    @Override
    public void setTargetValue(final double VALUE) {
        targetValue.set(VALUE);
    }

    @Override
    public DoubleProperty targetValueProperty() {
        return targetValue;
    }

    public int getDecimals() {
        return decimals.get();
    }

    public void setDecimals(final int VALUE) {
        decimals.set(VALUE);
    }

    public IntegerProperty decimalsProperty() {
        return decimals;
    }

    public String getUnit() {
        return unit.get();
    }

    public void setUnit(final String UNIT) {
        unit.set(UNIT);
    }

    public StringProperty unitProperty() {
        return unit;
    }

    public Color getSymbolColor() {
        return symbolColor.get();
    }

    public void setSymbolColor(final Color COLOR) {
        symbolColor.set(COLOR);
    }

    public ObjectProperty<Color> symbolColorProperty() {
        return symbolColor;
    }

    public Color getIconColor() {
        return iconColor.get();
    }

    public void setIconColor(final Color COLOR) {
        iconColor.set(COLOR);
    }

    public ObjectProperty<Color> iconColorProperty() {
        return iconColor;
    }

    @Override
    public Color getTextColor() {
        return textColor.get();
    }

    @Override
    public void setTextColor(final Color COLOR) {
        textColor.set(COLOR);
    }

    @Override
    public ObjectProperty<Color> textColorProperty() {
        return textColor;
    }

    public Color getBarColor() {
        return barColor.get();
    }

    public void setBarColor(final Color COLOR) {
        barColor.set(COLOR);
    }

    public ObjectProperty<Color> barColorProperty() {
        return barColor;
    }

    @Override
    public Color getColor() {
        return color.get();
    }

    @Override
    public void setColor(final Color COLOR) {
        color.set(COLOR);
    }

    @Override
    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    @Override
    public Color getIndicatorColor() {
        return indicatorColor.get();
    }

    @Override
    public void setIndicatorColor(final Color COLOR) {
        indicatorColor.set(COLOR);
    }

    @Override
    public ObjectProperty<Color> indicatorColorProperty() {
        return indicatorColor;
    }

    @Override
    public boolean isSelected() {
        return selected.get();
    }

    @Override
    public void setSelected(final boolean SELECTED) {
        selected.set(SELECTED);
    }

    @Override
    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSymbolPath(final double SCALE_X, final double SCALE_Y, final String PATH) {
        if (PATH.isEmpty()) {
            symbol.setVisible(false);
        } else {
            symbol.setStyle(new StringBuilder().append("-fx-scale-x:").append(clamp(0.0, 1.0, SCALE_X)).append(";")
                    .append("-fx-scale-y:").append(clamp(0.0, 1.0, SCALE_Y)).append(";")
                    .append("-fx-shape:\"").append(PATH).append("\";")
                    .toString());
            symbol.setVisible(true);
        }
        symbol.setCache(false);
        resize();
        symbol.setCache(true);
    }

    public void setIcon(final Ikon ICON) {
        icon.setIconCode(ICON);
        resize();
    }

    private <T extends Number> T clamp(final T MIN, final T MAX, final T VALUE) {
        if (VALUE.doubleValue() < MIN.doubleValue()) return MIN;
        if (VALUE.doubleValue() > MAX.doubleValue()) return MAX;
        return VALUE;
    }

    private void adjustTextSize(final Text TEXT, final double MAX_WIDTH, double fontSize) {
        final String FONT_NAME = TEXT.getFont().getName();
        while (TEXT.getLayoutBounds().getWidth() > MAX_WIDTH && fontSize > 0) {
            fontSize -= 0.005;
            TEXT.setFont(new Font(FONT_NAME, fontSize));
        }
    }

    private void touchRotate(final double X, final double Y) {
        Point2D p = sceneToLocal(X, Y);
        double deltaX = p.getX() - (pane.getLayoutX() + size * 0.5);
        double deltaY = p.getY() - (pane.getLayoutY() + size * 0.5);
        double radius = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
        double nx = deltaX / radius;
        double ny = deltaY / radius;
        double theta = Math.atan2(ny, nx);
        theta = Double.compare(theta, 0.0) >= 0 ? Math.toDegrees(theta) : Math.toDegrees((theta)) + 360.0;
//        double angle  = (theta + 230) % 360;
//        if (angle > 320 && angle < 360) {
//            angle = 0;
//        } else if (angle <= 320 && angle > ANGLE_RANGE) {
//            angle = ANGLE_RANGE;
//        }
        double angle = (theta + 270) % 360;
//        if (angle > 320 && angle < 360) {
//            angle = 0;
//        } else if (angle <= 320 && angle > ANGLE_RANGE) {
//        angle = ANGLE_RANGE;
//        }
        setTargetValue(angle / angleStep + minValue.get());
    }


    // ******************** Resizing ******************************************
    private void rotate(final double VALUE) {
        drawBar(VALUE);
        indicatorRotate.setAngle((VALUE - minValue.get()) * angleStep - ANGLE_RANGE * 0.5);
        text.setText(String.format(Locale.US, formatString, VALUE));
        adjustTextSize(text, size * 0.48, size * 0.216);
        text.setLayoutX((size - text.getLayoutBounds().getWidth()) * 0.5);
    }

    private void drawBar(final double VALUE) {
        barArc.setLength(-(VALUE - minValue.get()) * angleStep);
    }

    private void resize() {
        double width = getWidth() - getInsets().getLeft() - getInsets().getRight();
        double height = getHeight() - getInsets().getTop() - getInsets().getBottom();
        size = width < height ? width : height;

        if (width > 0 && height > 0) {
            pane.setMaxSize(size, size);
            pane.setPrefSize(size, size);
            pane.relocate((getWidth() - size) * 0.5, (getHeight() - size) * 0.5);

            barArc.setCenterX(size * 0.5);
            barArc.setCenterY(size * 0.5);
            barArc.setRadiusX(size * 0.4);
            barArc.setRadiusY(size * 0.4);
            barArc.setStrokeWidth(size * 0.2);
            drawBar(targetValue.get());

            double shadowRadius = clamp(1.0, 2.0, size * 0.004);
            dropShadow.setRadius(shadowRadius);
            dropShadow.setOffsetY(shadowRadius);
            highlight.setRadius(shadowRadius);
            highlight.setOffsetY(shadowRadius);
            innerShadow.setRadius(shadowRadius);
            innerShadow.setOffsetY(-shadowRadius);

            double center = size * 0.5;
            scaleFactor = size / PREFERRED_WIDTH;
//            ring.setCache(false);
//            ring.getTransforms().setAll(new Scale(scaleFactor, scaleFactor, 0, 0));
//            ring.setCache(true);
//            ring.setCacheHint(CacheHint.SPEED);

            mainCircle.setCache(false);
            mainCircle.setRadius(size * 0.3);
            mainCircle.setCenterX(center);
            mainCircle.setCenterY(center);
            mainCircle.setCache(true);
            mainCircle.setCacheHint(CacheHint.SPEED);

           // text.setFont(Fonts.robotoMedium(size * 0.216));
//            text.relocate((size - text.getLayoutBounds().getWidth()) * 0.5, size * 0.33);
//            text.setStyle("    -fx-font-size: 19pt;\n" +
//                    "    -fx-font-family: \"Roboto Condensed\";");
            text.relocate(size - (text.getLayoutBounds().getWidth() * 0.5), (size - text.getLayoutBounds().getHeight()) * 0.5);


            indicatorGlow.setRadius(size * 0.02);
            indicatorInnerShadow.setRadius(size * 0.008);
            indicatorInnerShadow.setOffsetY(size * 0.006);
            indicatorHighlight.setRadius(size * 0.008);
            indicatorHighlight.setOffsetY(-size * 0.004);

            indicator.setRadius(size * 0.1);
            indicator.setCenterX(center);
            indicator.setCenterY(size * 0.1);

            indicatorRotate.setPivotX(center);
            indicatorRotate.setPivotY(center);

            icon.setIconSize((int) (size * 0.112));

            //iconPane.setPrefSize(size * 0.112, size * 0.112);
          //  iconPane.relocate((size - iconPane.getPrefWidth()) * 0.5, size * 0.62);

            redraw();
        }
    }

    private void redraw() {
        pane.setBackground(new Background(new BackgroundFill(color.get(), new CornerRadii(1024), Insets.EMPTY)));
        mainCircle.setFill(color.get());
//        ring.setFill(color.get().darker());
        indicator.setFill(isSelected() ? indicatorColor.get() : indicatorColor.get());
        indicator.setStroke(isSelected() ? indicatorColor.get() : indicatorColor.get());
        symbol.setBackground(new Background(new BackgroundFill(symbolColor.get(), CornerRadii.EMPTY, Insets.EMPTY)));
        icon.setFill(iconColor.get());
        text.setFill(textColor.get());
        barArc.setStroke(barColor.get());
        rotate(targetValue.get());
    }


    // ******************** Event Handling ************************************
    public void setOnTargetSet(final EventHandler<RegulatorEvent> HANDLER) {
        addEventHandler(RegulatorEvent.TARGET_SET, HANDLER);
    }

    public void removeOnTargetSet(final EventHandler<RegulatorEvent> HANDLER) {
        removeEventHandler(RegulatorEvent.TARGET_SET, HANDLER);
    }
}
