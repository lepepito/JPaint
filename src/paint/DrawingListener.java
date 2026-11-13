package paint;

import java.awt.Dimension;
import java.awt.Point;

public interface DrawingListener {
    void stateChanged();

    void toolChanged(Tool tool);

    void colorsChanged();

    void cursorMoved(Point point);

    void shapeSizeChanged(Dimension size);
}
