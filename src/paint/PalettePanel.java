package paint;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class PalettePanel extends JPanel {
    private static final Color[] DEFAULT_COLORS = {
        new Color(0, 0, 0), new Color(128, 128, 128), new Color(128, 0, 0), new Color(128, 128, 0),
        new Color(0, 128, 0), new Color(0, 128, 128), new Color(0, 0, 128), new Color(128, 0, 128),
        new Color(128, 128, 64), new Color(0, 64, 64), new Color(0, 128, 255), new Color(0, 64, 128),
        new Color(128, 0, 255), new Color(128, 64, 0),
        new Color(255, 255, 255), new Color(192, 192, 192), new Color(255, 0, 0), new Color(255, 255, 0),
        new Color(0, 255, 0), new Color(0, 255, 255), new Color(0, 0, 255), new Color(255, 0, 255),
        new Color(255, 255, 128), new Color(0, 255, 128), new Color(128, 255, 255), new Color(128, 128, 255),
        new Color(255, 0, 128), new Color(255, 128, 64)
    };
    private static final Color BORDER = new Color(110, 110, 110);

    private final DrawingPanel canvas;

    public PalettePanel(DrawingPanel canvas) {
        super(new FlowLayout(FlowLayout.LEADING, 10, 6));
        this.canvas = canvas;
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(205, 205, 205)));

        add(new CurrentColors());

        JPanel swatches = new JPanel(new GridLayout(2, 14, 3, 3));
        swatches.setOpaque(false);
        for (Color color : DEFAULT_COLORS) {
            swatches.add(new Swatch(color));
        }
        add(swatches);

        JButton edit = I18n.text(new JButton(Icons.palette(20)), "palette.edit");
        edit.setFocusable(false);
        edit.addActionListener(e -> chooseColor(true));
        add(edit);
    }

    private void chooseColor(boolean first) {
        Color current = first ? canvas.getPrimary() : canvas.getSecondary();
        Color chosen = JColorChooser.showDialog(SwingUtilities.getWindowAncestor(this),
                I18n.get(first ? "palette.choose1" : "palette.choose2"), current);
        if (chosen != null) {
            if (first) {
                canvas.setPrimary(chosen);
            } else {
                canvas.setSecondary(chosen);
            }
        }
    }

    private static void paintColorBox(Graphics g, Rectangle r, Color color) {
        g.setColor(color);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(Color.WHITE);
        g.drawRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3);
        g.setColor(BORDER);
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
    }

    private class CurrentColors extends JComponent {
        private static final int SIZE = 24;

        CurrentColors() {
            setPreferredSize(new Dimension(42, 42));
            I18n.tooltip(this, "palette.indicator");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (front().contains(e.getPoint())) {
                        chooseColor(true);
                    } else if (back().contains(e.getPoint())) {
                        chooseColor(false);
                    }
                }
            });
        }

        private Rectangle front() {
            return new Rectangle(1, 1, SIZE, SIZE);
        }

        private Rectangle back() {
            return new Rectangle(17, 17, SIZE, SIZE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintColorBox(g, back(), canvas.getSecondary());
            paintColorBox(g, front(), canvas.getPrimary());
        }
    }

    private class Swatch extends JComponent {
        private Color color;

        Swatch(Color initial) {
            color = initial;
            setPreferredSize(new Dimension(19, 19));
            I18n.tooltip(this, "palette.swatch");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        canvas.setSecondary(color);
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        canvas.setPrimary(color);
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        Color chosen = JColorChooser.showDialog(SwingUtilities.getWindowAncestor(Swatch.this),
                                I18n.get("palette.chooseSwatch"), color);
                        if (chosen != null) {
                            color = chosen;
                            repaint();
                            canvas.setPrimary(chosen);
                        }
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintColorBox(g, new Rectangle(0, 0, getWidth(), getHeight()), color);
        }
    }
}
