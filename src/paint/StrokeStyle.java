package paint;

import java.awt.BasicStroke;
import java.awt.Stroke;

public enum StrokeStyle {
    SOLID("style.solid"),
    DASHED("style.dashed"),
    DOTTED("style.dotted");

    private final String key;

    StrokeStyle(String key) {
        this.key = key;
    }

    public Stroke createStroke(float width, boolean round) {
        int join = round ? BasicStroke.JOIN_ROUND : BasicStroke.JOIN_MITER;
        switch (this) {
            case DASHED:
                return new BasicStroke(width, BasicStroke.CAP_BUTT, join, 10f,
                        new float[] {width * 3 + 2, width * 2 + 2}, 0f);
            case DOTTED:
                return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                        new float[] {0.1f, width * 2 + 1}, 0f);
            default:
                return new BasicStroke(width, round ? BasicStroke.CAP_ROUND : BasicStroke.CAP_SQUARE, join);
        }
    }

    @Override
    public String toString() {
        return I18n.get(key);
    }
}
