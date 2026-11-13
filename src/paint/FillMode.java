package paint;

public enum FillMode {
    OUTLINE("fill.outline"),
    FILLED("fill.filled"),
    OUTLINE_FILLED("fill.both"),
    GRADIENT("fill.gradient");

    private final String key;

    FillMode(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return I18n.get(key);
    }
}
