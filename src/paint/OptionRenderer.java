package paint;

import java.awt.Component;
import java.util.function.Function;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;

public class OptionRenderer<T> extends DefaultListCellRenderer {
    private final Function<T, String> textOf;
    private final Function<T, Icon> iconOf;

    public OptionRenderer(Function<T, String> textOf, Function<T, Icon> iconOf) {
        this.textOf = textOf;
        this.iconOf = iconOf;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null) {
            @SuppressWarnings("unchecked")
            T item = (T) value;
            setText(textOf.apply(item));
            setIcon(iconOf.apply(item));
        }
        return this;
    }
}
