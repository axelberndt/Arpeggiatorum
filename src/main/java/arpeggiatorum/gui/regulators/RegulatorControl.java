package arpeggiatorum.gui.regulators;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.paint.Color;


/**
 * Created by hansolo on 13.12.16.
 */
public interface RegulatorControl {

    public double getTargetValue();
    public void setTargetValue(final double VALUE);
    public DoubleProperty targetValueProperty();

    public Color getTextColor();
    public void setTextColor(final Color COLOR);
    public ObjectProperty<Color> textColorProperty();

    public Color getColor();
    public void setColor(final Color COLOR);
    public ObjectProperty<Color> colorProperty();

    public Color getIndicatorColor();
    public void setIndicatorColor(final Color COLOR);
    public ObjectProperty<Color> indicatorColorProperty();

    public boolean isSelected();
    public void setSelected(final boolean SELECTED);
    public BooleanProperty selectedProperty();

}
