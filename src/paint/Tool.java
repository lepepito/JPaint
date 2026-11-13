package paint;

public enum Tool {
    SELECT("select"),
    PENCIL("pencil"),
    ERASER("eraser"),
    FILL("fill"),
    PICKER("picker"),
    SPRAY("spray"),
    TEXT("text"),
    LINE("line"),
    RECTANGLE("rect"),
    ELLIPSE("ellipse"),
    ROUND_RECT("roundRect"),
    TRIANGLE("triangle");

    private final String key;

    Tool(String key) {
        this.key = key;
    }

    public String key() {
        return "tool." + key;
    }

    public String hintKey() {
        return "tool." + key + ".hint";
    }

    public boolean isShape() {
        return this == LINE || this == RECTANGLE || this == ELLIPSE || this == ROUND_RECT || this == TRIANGLE;
    }

    public boolean isClosedShape() {
        return isShape() && this != LINE;
    }

    public boolean usesWidth() {
        return isShape() || this == PENCIL || this == ERASER || this == SPRAY;
    }

    public boolean usesStyle() {
        return isShape() || this == PENCIL;
    }

    public boolean usesOpacity() {
        return isShape() || this == PENCIL || this == SPRAY || this == FILL || this == TEXT;
    }
}
