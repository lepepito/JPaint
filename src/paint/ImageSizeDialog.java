package paint;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class ImageSizeDialog extends JDialog {
    private final JTextField widthField;
    private final JTextField heightField;
    private final JRadioButton stretchButton;
    private boolean validated;
    private int imageWidth;
    private int imageHeight;

    public ImageSizeDialog(Frame owner, int width, int height) {
        super(owner, I18n.get("resize.title"), true);

        widthField = new JTextField(String.valueOf(width), 6);
        heightField = new JTextField(String.valueOf(height), 6);
        JRadioButton canvasButton = new JRadioButton(I18n.get("resize.canvas"), true);
        stretchButton = new JRadioButton(I18n.get("resize.stretch"));
        ButtonGroup group = new ButtonGroup();
        group.add(canvasButton);
        group.add(stretchButton);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel(I18n.get("resize.width")), c);
        c.gridx = 1;
        form.add(widthField, c);
        c.gridx = 0;
        c.gridy = 1;
        form.add(new JLabel(I18n.get("resize.height")), c);
        c.gridx = 1;
        form.add(heightField, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        form.add(canvasButton, c);
        c.gridy = 3;
        form.add(stretchButton, c);

        JButton ok = new JButton(I18n.get("dialog.ok"));
        JButton cancel = new JButton(I18n.get("dialog.cancel"));
        ok.addActionListener(e -> validateAndClose());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttons.add(ok);
        buttons.add(cancel);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        applyComponentOrientation(I18n.orientation());
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void validateAndClose() {
        try {
            int w = Integer.parseInt(widthField.getText().trim());
            int h = Integer.parseInt(heightField.getText().trim());
            if (w < 1 || h < 1 || w > DrawingPanel.MAX_SIZE || h > DrawingPanel.MAX_SIZE) {
                throw new NumberFormatException();
            }
            imageWidth = w;
            imageHeight = h;
            validated = true;
            dispose();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, I18n.format("resize.error", String.valueOf(DrawingPanel.MAX_SIZE)),
                    I18n.get("error.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean showDialog() {
        setVisible(true);
        return validated;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public boolean isStretch() {
        return stretchButton.isSelected();
    }
}
