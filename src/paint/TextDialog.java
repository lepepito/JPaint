package paint;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

public class TextDialog extends JDialog {
    private static final Integer[] SIZES = {8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 60, 72};

    private final JTextField textField = new JTextField(24);
    private final JComboBox<String> fontCombo;
    private final JComboBox<Integer> sizeCombo = new JComboBox<>(SIZES);
    private final JCheckBox boldBox;
    private final JCheckBox italicBox;
    private final JLabel preview = new JLabel(" ", SwingConstants.CENTER);
    private boolean validated;

    public TextDialog(Window owner, Font font, Color color) {
        super(owner, I18n.get("text.title"), ModalityType.APPLICATION_MODAL);

        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontCombo = new JComboBox<>(families);
        fontCombo.setSelectedItem(font.getFamily());
        fontCombo.setMaximumRowCount(15);
        fontCombo.setPrototypeDisplayValue("Times New Roman Italic");
        sizeCombo.setSelectedItem(font.getSize());
        boldBox = new JCheckBox(I18n.get("text.bold"), font.isBold());
        italicBox = new JCheckBox(I18n.get("text.italic"), font.isItalic());

        preview.setOpaque(true);
        preview.setBackground(Color.WHITE);
        preview.setForeground(color);
        preview.setPreferredSize(new Dimension(420, 100));
        preview.setBorder(BorderFactory.createTitledBorder(I18n.get("text.preview")));

        ActionListener update = e -> updatePreview();
        fontCombo.addActionListener(update);
        sizeCombo.addActionListener(update);
        boldBox.addActionListener(update);
        italicBox.addActionListener(update);
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updatePreview();
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.LINE_START;

        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel(I18n.get("text.text")), c);
        c.gridx = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(textField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel(I18n.get("text.font")), c);
        c.gridx = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(fontCombo, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel(I18n.get("text.size")), c);
        c.gridx = 1;
        form.add(sizeCombo, c);
        c.gridx = 2;
        form.add(boldBox, c);
        c.gridx = 3;
        c.weightx = 1;
        form.add(italicBox, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.BOTH;
        form.add(preview, c);

        JButton ok = new JButton(I18n.get("dialog.ok"));
        JButton cancel = new JButton(I18n.get("dialog.cancel"));
        ok.addActionListener(e -> {
            validated = true;
            dispose();
        });
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
        updatePreview();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public Font getSelectedFont() {
        int style = (boldBox.isSelected() ? Font.BOLD : 0) | (italicBox.isSelected() ? Font.ITALIC : 0);
        return new Font((String) fontCombo.getSelectedItem(), style, (Integer) sizeCombo.getSelectedItem());
    }

    private void updatePreview() {
        preview.setFont(getSelectedFont());
        String text = textField.getText();
        preview.setText(text.isEmpty() ? "AaBbCc 123" : text);
    }

    public String showDialog() {
        setVisible(true);
        return validated ? textField.getText() : null;
    }
}
