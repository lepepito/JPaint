package paint;

import java.awt.Dimension;
import java.awt.LayoutManager;
import javax.swing.JPanel;

public class CompactPanel extends JPanel {
    public CompactPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
