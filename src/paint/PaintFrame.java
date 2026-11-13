package paint;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class PaintFrame extends JFrame implements DrawingListener {
    private static final String AUTHORS = "HALOUBI Ahmed, EL KIHEL Riyad";
    private static final String SUPERVISOR = "M. Mustapha OUJAOURA";

    private static final Color WORKSPACE = new Color(201, 208, 220);
    private static final Color LINE = new Color(205, 205, 205);

    private final DrawingPanel canvas = new DrawingPanel();
    private final PalettePanel palette = new PalettePanel(canvas);
    private File currentFile;
    private File lastDirectory;

    private final Map<Tool, JToggleButton> toolButtons = new EnumMap<>(Tool.class);
    private final Map<Tool, JRadioButtonMenuItem> toolItems = new EnumMap<>(Tool.class);
    private final List<JMenuItem> selectionItems = new ArrayList<>();
    private final JPopupMenu selectionMenu = new JPopupMenu();
    private JMenuItem undoItem;
    private JMenuItem redoItem;
    private JButton undoButton;
    private JButton redoButton;
    private JToolBar optionsBar;
    private JToolBar toolsBar;
    private JPanel statusBar;
    private JComboBox<Integer> widthCombo;
    private JComboBox<StrokeStyle> styleCombo;
    private JComboBox<FillMode> fillCombo;
    private JComboBox<Integer> opacityCombo;
    private JLabel widthLabel;
    private JLabel styleLabel;
    private JLabel fillLabel;
    private JLabel opacityLabel;
    private final JLabel hintLabel = new JLabel(" ");
    private final JLabel positionLabel = new JLabel();
    private final JLabel sizeLabel = new JLabel();
    private final JLabel canvasLabel = new JLabel();
    private final JLabel zoomLabel = new JLabel();
    private Dimension shapeSize;

    public PaintFrame() {
        setIconImages(Icons.appImages());
        setJMenuBar(createMenuBar());
        add(createOptionsBar(), BorderLayout.NORTH);
        add(createToolsBar(), BorderLayout.LINE_START);
        add(createCanvasArea(), BorderLayout.CENTER);
        add(createBottomArea(), BorderLayout.SOUTH);
        createSelectionMenu();

        canvas.setDrawingListener(this);
        canvas.setSelectionMenu(selectionMenu);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        toolChanged(canvas.getTool());
        stateChanged();
        I18n.onChange(this::languageChanged);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1200, screen.width * 9 / 10), Math.min(860, screen.height * 9 / 10));
        setMinimumSize(new Dimension(760, 540));
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = menu("menu.file");
        file.add(item("menu.file.new", ctrl(KeyEvent.VK_N), Icons.newFile(16), e -> newImage()));
        file.add(item("menu.file.open", ctrl(KeyEvent.VK_O), Icons.open(16), e -> open()));
        file.add(item("menu.file.save", ctrl(KeyEvent.VK_S), Icons.save(16), e -> save()));
        file.add(item("menu.file.saveAs", ctrlShift(KeyEvent.VK_S), null, e -> saveAs()));
        file.addSeparator();
        file.add(item("menu.file.quit", ctrl(KeyEvent.VK_Q), null, e -> quit()));
        bar.add(file);

        JMenu edit = menu("menu.edit");
        undoItem = edit.add(item("menu.edit.undo", ctrl(KeyEvent.VK_Z), Icons.undo(16), e -> canvas.undo()));
        redoItem = edit.add(item("menu.edit.redo", ctrl(KeyEvent.VK_Y), Icons.redo(16), e -> canvas.redo()));
        edit.addSeparator();
        edit.add(selectionItem(item("menu.edit.cut", ctrl(KeyEvent.VK_X), null, e -> canvas.cut())));
        edit.add(selectionItem(item("menu.edit.copy", ctrl(KeyEvent.VK_C), null, e -> canvas.copy())));
        edit.add(item("menu.edit.paste", ctrl(KeyEvent.VK_V), null, e -> paste()));
        edit.add(selectionItem(item("menu.edit.delete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), null,
                e -> canvas.deleteSelection())));
        edit.addSeparator();
        edit.add(item("menu.edit.selectAll", ctrl(KeyEvent.VK_A), null, e -> canvas.selectAll()));
        edit.add(item("menu.edit.clear", ctrlShift(KeyEvent.VK_N), null, e -> canvas.clearImage()));
        bar.add(edit);

        JMenu tools = menu("menu.tools");
        ButtonGroup toolGroup = new ButtonGroup();
        for (Tool tool : Tool.values()) {
            if (tool == Tool.LINE) {
                tools.addSeparator();
            }
            JRadioButtonMenuItem item = I18n.text(new JRadioButtonMenuItem(Icons.tool(tool)), tool.key());
            item.addActionListener(e -> canvas.setTool(tool));
            toolGroup.add(item);
            toolItems.put(tool, item);
            tools.add(item);
        }
        bar.add(tools);

        JMenu image = menu("menu.image");
        image.add(item("menu.image.resize", ctrl(KeyEvent.VK_E), null, e -> resizeCanvas()));
        image.addSeparator();
        image.add(item("menu.image.rotateRight", ctrl(KeyEvent.VK_R), null, e -> canvas.rotate(true)));
        image.add(item("menu.image.rotateLeft", ctrlShift(KeyEvent.VK_R), null, e -> canvas.rotate(false)));
        image.add(item("menu.image.flipH", null, null, e -> canvas.flip(true)));
        image.add(item("menu.image.flipV", null, null, e -> canvas.flip(false)));
        image.add(item("menu.image.invert", ctrl(KeyEvent.VK_I), null, e -> canvas.invertColors()));
        image.addSeparator();
        image.add(checkItem("menu.image.antialias", null, true, canvas::setAntialiasing));
        bar.add(image);

        JMenu view = menu("menu.view");
        view.add(item("menu.view.zoomIn", ctrl(KeyEvent.VK_PAGE_UP), null, e -> canvas.zoomIn()));
        view.add(item("menu.view.zoomOut", ctrl(KeyEvent.VK_PAGE_DOWN), null, e -> canvas.zoomOut()));
        view.add(item("menu.view.zoomReset", ctrl(KeyEvent.VK_0), null, e -> canvas.resetZoom()));
        view.addSeparator();
        view.add(checkItem("menu.view.grid", ctrl(KeyEvent.VK_G), false, canvas::setGridVisible));
        view.add(checkItem("menu.view.toolbar", null, true, visible -> {
            optionsBar.setVisible(visible);
            toolsBar.setVisible(visible);
        }));
        view.add(checkItem("menu.view.statusbar", null, true, visible -> statusBar.setVisible(visible)));
        bar.add(view);

        JMenu language = menu("menu.language");
        ButtonGroup languageGroup = new ButtonGroup();
        addLanguage(language, languageGroup, "Fran\u00e7ais", I18n.FRENCH);
        addLanguage(language, languageGroup, "English", I18n.ENGLISH);
        addLanguage(language, languageGroup, "\u0627\u0644\u0639\u0631\u0628\u064a\u0629", I18n.ARABIC);
        bar.add(language);

        JMenu help = menu("menu.help");
        help.add(item("menu.help.about", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), null, e -> about()));
        bar.add(help);
        return bar;
    }

    private JMenu menu(String key) {
        return I18n.text(new JMenu(), key);
    }

    private JMenuItem item(String key, KeyStroke accelerator, Icon icon, ActionListener action) {
        JMenuItem item = I18n.text(new JMenuItem(), key);
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        if (icon != null) {
            item.setIcon(icon);
            item.setDisabledIcon(Icons.disabled(icon));
        }
        item.addActionListener(action);
        return item;
    }

    private JCheckBoxMenuItem checkItem(String key, KeyStroke accelerator, boolean selected, Consumer<Boolean> action) {
        JCheckBoxMenuItem item = I18n.text(new JCheckBoxMenuItem(), key);
        item.setSelected(selected);
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        item.addItemListener(e -> action.accept(item.isSelected()));
        return item;
    }

    private JMenuItem selectionItem(JMenuItem item) {
        selectionItems.add(item);
        return item;
    }

    private void addLanguage(JMenu menu, ButtonGroup group, String name, Locale locale) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(name, locale.equals(I18n.getLocale()));
        item.addActionListener(e -> I18n.setLocale(locale));
        group.add(item);
        menu.add(item);
    }

    private static KeyStroke ctrl(int key) {
        return KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK);
    }

    private static KeyStroke ctrlShift(int key) {
        return KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    }

    private void createSelectionMenu() {
        selectionMenu.add(selectionItem(item("menu.edit.cut", null, null, e -> canvas.cut())));
        selectionMenu.add(selectionItem(item("menu.edit.copy", null, null, e -> canvas.copy())));
        selectionMenu.add(item("menu.edit.paste", null, null, e -> paste()));
        selectionMenu.add(selectionItem(item("menu.edit.delete", null, null, e -> canvas.deleteSelection())));
        selectionMenu.addSeparator();
        selectionMenu.add(item("menu.edit.selectAll", null, null, e -> canvas.selectAll()));
    }

    private JToolBar createOptionsBar() {
        optionsBar = new JToolBar();
        optionsBar.setFloatable(false);
        optionsBar.setRollover(true);
        optionsBar.add(commandButton(Icons.newFile(20), "menu.file.new", "Ctrl+N", e -> newImage()));
        optionsBar.add(commandButton(Icons.open(20), "menu.file.open", "Ctrl+O", e -> open()));
        optionsBar.add(commandButton(Icons.save(20), "menu.file.save", "Ctrl+S", e -> save()));
        optionsBar.addSeparator();
        undoButton = commandButton(Icons.undo(20), "menu.edit.undo", "Ctrl+Z", e -> canvas.undo());
        redoButton = commandButton(Icons.redo(20), "menu.edit.redo", "Ctrl+Y", e -> canvas.redo());
        optionsBar.add(undoButton);
        optionsBar.add(redoButton);
        optionsBar.addSeparator();

        widthCombo = new JComboBox<>(new Integer[] {1, 2, 3, 5, 8, 12, 16, 24, 32});
        widthCombo.setRenderer(new OptionRenderer<Integer>(w -> w + " px", Icons::lineSample));
        widthCombo.setSelectedItem(3);
        widthCombo.addActionListener(e -> canvas.setStrokeWidth((Integer) widthCombo.getSelectedItem()));
        widthLabel = I18n.text(new JLabel(), "options.width");
        optionsBar.add(option(widthLabel, widthCombo));

        styleCombo = new JComboBox<>(StrokeStyle.values());
        styleCombo.setRenderer(new OptionRenderer<StrokeStyle>(StrokeStyle::toString, Icons::styleSample));
        styleCombo.addActionListener(e -> canvas.setStrokeStyle((StrokeStyle) styleCombo.getSelectedItem()));
        styleLabel = I18n.text(new JLabel(), "options.style");
        optionsBar.add(option(styleLabel, styleCombo));

        fillCombo = new JComboBox<>(FillMode.values());
        fillCombo.setRenderer(new OptionRenderer<FillMode>(FillMode::toString, Icons::fillSample));
        fillCombo.addActionListener(e -> canvas.setFillMode((FillMode) fillCombo.getSelectedItem()));
        fillLabel = I18n.text(new JLabel(), "options.fill");
        optionsBar.add(option(fillLabel, fillCombo));

        opacityCombo = new JComboBox<>(new Integer[] {100, 75, 50, 25});
        opacityCombo.setRenderer(new OptionRenderer<Integer>(p -> p + " %", Icons::opacitySample));
        opacityCombo.addActionListener(e -> canvas.setOpacity((Integer) opacityCombo.getSelectedItem() / 100f));
        opacityLabel = I18n.text(new JLabel(), "options.opacity");
        optionsBar.add(option(opacityLabel, opacityCombo));
        return optionsBar;
    }

    private JButton commandButton(Icon icon, String key, String shortcut, ActionListener action) {
        JButton button = new JButton(icon);
        button.setDisabledIcon(Icons.disabled(icon));
        button.setFocusable(false);
        button.addActionListener(action);
        I18n.onChange(() -> button.setToolTipText(I18n.get(key).replace("\u2026", "") + " (" + shortcut + ")"));
        return button;
    }

    private JPanel option(JLabel label, JComboBox<?> combo) {
        combo.setFocusable(false);
        label.setLabelFor(combo);
        JPanel panel = new CompactPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        panel.add(label);
        panel.add(combo);
        return panel;
    }

    private JToolBar createToolsBar() {
        toolsBar = new JToolBar(SwingConstants.VERTICAL);
        toolsBar.setFloatable(false);
        JPanel grid = new CompactPanel(new GridLayout(0, 2, 3, 3));
        grid.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        ButtonGroup group = new ButtonGroup();
        for (Tool tool : Tool.values()) {
            JToggleButton button = new JToggleButton(Icons.tool(tool));
            button.setFocusable(false);
            button.setMargin(new Insets(5, 5, 5, 5));
            I18n.tooltip(button, tool.key());
            button.addActionListener(e -> canvas.setTool(tool));
            group.add(button);
            grid.add(button);
            toolButtons.put(tool, button);
        }
        toolsBar.add(grid);
        return toolsBar;
    }

    private JScrollPane createCanvasArea() {
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEADING, 14, 14));
        holder.setBackground(WORKSPACE);
        holder.add(canvas);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WORKSPACE);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getHorizontalScrollBar().setUnitIncrement(20);

        ImageDropHandler drop = new ImageDropHandler(this);
        canvas.setTransferHandler(drop);
        holder.setTransferHandler(drop);
        scroll.getViewport().setTransferHandler(drop);
        return scroll;
    }

    private JPanel createBottomArea() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(palette, BorderLayout.CENTER);
        statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, LINE),
                BorderFactory.createEmptyBorder(3, 8, 3, 4)));
        statusBar.add(hintLabel, BorderLayout.CENTER);
        JPanel cells = new JPanel(new GridLayout(1, 4));
        cells.add(statusCell(positionLabel, Icons.position(), "status.position"));
        cells.add(statusCell(sizeLabel, Icons.selectionSize(), "status.size"));
        cells.add(statusCell(canvasLabel, Icons.canvasSize(), "status.canvas"));
        cells.add(statusCell(zoomLabel, Icons.zoom(), "status.zoom"));
        statusBar.add(cells, BorderLayout.LINE_END);
        bottom.add(statusBar, BorderLayout.SOUTH);
        return bottom;
    }

    private JLabel statusCell(JLabel label, Icon icon, String tooltipKey) {
        label.setIcon(icon);
        label.setIconTextGap(6);
        label.setPreferredSize(new Dimension(128, 20));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, LINE),
                BorderFactory.createEmptyBorder(0, 8, 0, 4)));
        I18n.tooltip(label, tooltipKey);
        return label;
    }

    @Override
    public void stateChanged() {
        undoItem.setEnabled(canvas.canUndo());
        undoButton.setEnabled(canvas.canUndo());
        redoItem.setEnabled(canvas.canRedo());
        redoButton.setEnabled(canvas.canRedo());
        boolean hasSelection = canvas.hasSelection();
        for (JMenuItem item : selectionItems) {
            item.setEnabled(hasSelection);
        }
        updateTitle();
        updateStatus();
    }

    @Override
    public void toolChanged(Tool tool) {
        toolButtons.get(tool).setSelected(true);
        toolItems.get(tool).setSelected(true);
        widthLabel.setEnabled(tool.usesWidth());
        widthCombo.setEnabled(tool.usesWidth());
        styleLabel.setEnabled(tool.usesStyle());
        styleCombo.setEnabled(tool.usesStyle());
        fillLabel.setEnabled(tool.isClosedShape());
        fillCombo.setEnabled(tool.isClosedShape());
        opacityLabel.setEnabled(tool.usesOpacity());
        opacityCombo.setEnabled(tool.usesOpacity());
        updateHint();
    }

    @Override
    public void colorsChanged() {
        palette.repaint();
    }

    @Override
    public void cursorMoved(Point point) {
        positionLabel.setText(point == null ? "" : point.x + ", " + point.y + " px");
    }

    @Override
    public void shapeSizeChanged(Dimension size) {
        shapeSize = size;
        updateStatus();
    }

    private void updateTitle() {
        setTitle((canvas.isModified() ? "*" : "") + documentName() + " - " + I18n.get("app.name"));
    }

    private String documentName() {
        return currentFile != null ? currentFile.getName() : I18n.get("app.untitled");
    }

    private void updateHint() {
        String hint = I18n.get(canvas.getTool().hintKey());
        hintLabel.setText(hint);
        hintLabel.setToolTipText(hint);
    }

    private void updateStatus() {
        Dimension size = shapeSize != null ? shapeSize : canvas.getSelectionSize();
        sizeLabel.setText(size == null ? "" : size.width + " \u00d7 " + size.height + " px");
        BufferedImage img = canvas.getImage();
        canvasLabel.setText(img.getWidth() + " \u00d7 " + img.getHeight() + " px");
        zoomLabel.setText(Math.round(canvas.getZoom() * 100) + " %");
    }

    private void languageChanged() {
        applyComponentOrientation(I18n.orientation());
        selectionMenu.applyComponentOrientation(I18n.orientation());
        SwingUtilities.updateComponentTreeUI(this);
        SwingUtilities.updateComponentTreeUI(selectionMenu);
        updateTitle();
        updateHint();
        updateStatus();
    }

    private boolean confirmDiscard() {
        if (!canvas.isModified()) {
            return true;
        }
        String[] options = {I18n.get("dialog.save.yes"), I18n.get("dialog.save.no"), I18n.get("dialog.cancel")};
        int choice = JOptionPane.showOptionDialog(this, I18n.format("dialog.save.message", documentName()),
                I18n.get("app.name"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        if (choice == 0) {
            return save();
        }
        return choice == 1;
    }

    private void newImage() {
        if (confirmDiscard()) {
            currentFile = null;
            canvas.newImage(DrawingPanel.DEFAULT_WIDTH, DrawingPanel.DEFAULT_HEIGHT);
        }
    }

    private void open() {
        if (!confirmDiscard()) {
            return;
        }
        JFileChooser chooser = createFileChooser(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openFile(chooser.getSelectedFile());
        }
    }

    public void openFile(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                showError(I18n.format("error.format", file.getName()));
                return;
            }
            currentFile = file;
            lastDirectory = file.getAbsoluteFile().getParentFile();
            canvas.setImage(img);
        } catch (IOException e) {
            showError(I18n.format("error.open", file.getName(), e.getLocalizedMessage()));
        } catch (OutOfMemoryError e) {
            showError(I18n.get("error.memory"));
        }
    }

    void openDroppedFile(File file) {
        if (confirmDiscard()) {
            openFile(file);
        }
    }

    private boolean save() {
        if (currentFile == null || formatOf(currentFile) == null) {
            return saveAs();
        }
        return writeFile(currentFile);
    }

    private boolean saveAs() {
        JFileChooser chooser = createFileChooser(true);
        String name = currentFile != null ? currentFile.getName() : I18n.get("app.untitled") + ".png";
        chooser.setSelectedFile(new File(lastDirectory, name));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        File file = chooser.getSelectedFile();
        if (formatOf(file) == null) {
            FileFilter filter = chooser.getFileFilter();
            String extension = filter instanceof FileNameExtensionFilter
                    ? ((FileNameExtensionFilter) filter).getExtensions()[0] : "png";
            file = new File(file.getParentFile(), file.getName() + "." + extension);
        }
        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(this, I18n.format("dialog.overwrite.message", file.getName()),
                    I18n.get("dialog.overwrite.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        if (!writeFile(file)) {
            return false;
        }
        currentFile = file;
        lastDirectory = file.getAbsoluteFile().getParentFile();
        updateTitle();
        return true;
    }

    private boolean writeFile(File file) {
        canvas.commitSelection();
        try {
            if (!ImageIO.write(canvas.getImage(), formatOf(file), file)) {
                showError(I18n.format("error.format", file.getName()));
                return false;
            }
            canvas.setModified(false);
            return true;
        } catch (IOException e) {
            showError(I18n.format("error.save", file.getName(), e.getLocalizedMessage()));
            return false;
        }
    }

    private static String formatOf(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        String extension = name.substring(name.lastIndexOf('.') + 1);
        switch (extension) {
            case "png":
            case "bmp":
            case "gif":
                return extension;
            case "jpg":
            case "jpeg":
                return "jpg";
            default:
                return null;
        }
    }

    private JFileChooser createFileChooser(boolean forSaving) {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter all = new FileNameExtensionFilter(I18n.get("filter.images"),
                "png", "jpg", "jpeg", "bmp", "gif");
        FileNameExtensionFilter png = new FileNameExtensionFilter(I18n.get("filter.png"), "png");
        FileNameExtensionFilter jpg = new FileNameExtensionFilter(I18n.get("filter.jpg"), "jpg", "jpeg");
        FileNameExtensionFilter bmp = new FileNameExtensionFilter(I18n.get("filter.bmp"), "bmp");
        FileNameExtensionFilter gif = new FileNameExtensionFilter(I18n.get("filter.gif"), "gif");
        if (!forSaving) {
            chooser.addChoosableFileFilter(all);
        }
        chooser.addChoosableFileFilter(png);
        chooser.addChoosableFileFilter(jpg);
        chooser.addChoosableFileFilter(bmp);
        chooser.addChoosableFileFilter(gif);
        if (forSaving) {
            String format = currentFile == null ? null : formatOf(currentFile);
            chooser.setFileFilter("jpg".equals(format) ? jpg : "bmp".equals(format) ? bmp
                    : "gif".equals(format) ? gif : png);
        } else {
            chooser.setFileFilter(all);
        }
        chooser.applyComponentOrientation(I18n.orientation());
        return chooser;
    }

    private void paste() {
        if (!canvas.paste()) {
            JOptionPane.showMessageDialog(this, I18n.get("error.noImage"), I18n.get("app.name"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void resizeCanvas() {
        BufferedImage img = canvas.getImage();
        ImageSizeDialog dialog = new ImageSizeDialog(this, img.getWidth(), img.getHeight());
        if (dialog.showDialog()) {
            canvas.resizeImage(dialog.getImageWidth(), dialog.getImageHeight(), dialog.isStretch());
        }
    }

    private void about() {
        String today = DateFormat.getDateInstance(DateFormat.FULL, I18n.getLocale()).format(new Date());
        String message = I18n.get("app.name") + " " + I18n.get("about.version") + "\n"
                + I18n.get("about.description") + "\n"
                + I18n.get("about.course") + "\n\n"
                + I18n.get("about.authors") + " " + AUTHORS + "\n"
                + I18n.get("about.supervisor") + " " + SUPERVISOR + "\n\n"
                + I18n.get("about.today") + " " + today;
        JOptionPane.showMessageDialog(this, message, I18n.get("about.title"), JOptionPane.INFORMATION_MESSAGE,
                new ImageIcon(Icons.appImage(64)));
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, I18n.get("error.title"), JOptionPane.ERROR_MESSAGE);
    }

    private void quit() {
        if (confirmDiscard()) {
            dispose();
            System.exit(0);
        }
    }
}
