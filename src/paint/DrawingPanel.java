package paint;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class DrawingPanel extends JPanel {
    public static final int DEFAULT_WIDTH = 800;
    public static final int DEFAULT_HEIGHT = 600;
    public static final int MAX_SIZE = 10000;

    private static final double[] ZOOM_LEVELS = {0.25, 0.5, 0.75, 1, 1.5, 2, 3, 4, 6, 8};
    private static final int FILL_TOLERANCE = 32;
    private static final int GRID_STEP = 20;
    private static final Color SELECTION_COLOR = new Color(0, 102, 204);

    private BufferedImage image;
    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<>();
    private boolean modified;

    private Tool tool = Tool.PENCIL;
    private Tool previousTool = Tool.PENCIL;
    private Color primary = Color.BLACK;
    private Color secondary = Color.WHITE;
    private int strokeWidth = 3;
    private StrokeStyle strokeStyle = StrokeStyle.SOLID;
    private FillMode fillMode = FillMode.OUTLINE;
    private float opacity = 1f;
    private boolean antialiasing = true;
    private boolean gridVisible;
    private double zoom = 1;
    private Font textFont = new Font(Font.SANS_SERIF, Font.PLAIN, 24);

    private int gestureButton;
    private boolean useSecondColor;
    private Point startPoint;
    private Point endPoint;
    private Path2D freehand;
    private Point sprayPoint;
    private Point hoverPoint;
    private final Timer sprayTimer = new Timer(30, e -> spray());
    private final Random random = new Random();

    private Rectangle selection;
    private BufferedImage floating;
    private Point dragOffset;

    private DrawingListener listener;
    private JPopupMenu selectionMenu;

    public DrawingPanel() {
        newImage(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setFocusable(true);
        updateCursor();
        MouseHandler mouse = new MouseHandler();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
        addKeyListener(new KeyHandler());
    }

    public void setDrawingListener(DrawingListener drawingListener) {
        listener = drawingListener;
    }

    public void setSelectionMenu(JPopupMenu menu) {
        selectionMenu = menu;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void newImage(int width, int height) {
        BufferedImage blank = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = blank.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        replaceImage(blank);
    }

    public void setImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, copy.getWidth(), copy.getHeight());
        g.drawImage(source, 0, 0, null);
        g.dispose();
        replaceImage(copy);
    }

    private void replaceImage(BufferedImage newImage) {
        cancelGesture();
        selection = null;
        floating = null;
        image = newImage;
        undoStack.clear();
        redoStack.clear();
        modified = false;
        revalidate();
        repaint();
        fireStateChanged();
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean value) {
        modified = value;
        fireStateChanged();
    }

    private void changed() {
        modified = true;
        repaint();
        fireStateChanged();
    }

    private void resized() {
        revalidate();
        changed();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension((int) Math.ceil(image.getWidth() * zoom), (int) Math.ceil(image.getHeight() * zoom));
    }

    private void saveUndo() {
        undoStack.push(copyOf(image));
        while (undoStack.size() > maxUndo()) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    private int maxUndo() {
        long bytes = (long) image.getWidth() * image.getHeight() * 4;
        return (int) Math.max(5, Math.min(30, 200_000_000L / bytes));
    }

    private static BufferedImage copyOf(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        finishPending();
        if (!undoStack.isEmpty()) {
            redoStack.push(image);
            image = undoStack.pop();
            resized();
        }
    }

    public void redo() {
        finishPending();
        if (!redoStack.isEmpty()) {
            undoStack.push(image);
            image = redoStack.pop();
            resized();
        }
    }

    private void finishPending() {
        cancelGesture();
        commitSelection();
        selection = null;
        repaint();
        fireStateChanged();
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool newTool) {
        if (newTool == tool) {
            return;
        }
        cancelGesture();
        if (newTool != Tool.SELECT) {
            commitSelection();
            selection = null;
        }
        if (newTool == Tool.PICKER) {
            previousTool = tool;
        }
        tool = newTool;
        updateCursor();
        repaint();
        if (listener != null) {
            listener.toolChanged(tool);
        }
        fireStateChanged();
    }

    private void updateCursor() {
        int type = tool == Tool.TEXT ? Cursor.TEXT_CURSOR : Cursor.CROSSHAIR_CURSOR;
        setCursor(Cursor.getPredefinedCursor(type));
    }

    public Color getPrimary() {
        return primary;
    }

    public Color getSecondary() {
        return secondary;
    }

    public void setPrimary(Color color) {
        primary = color;
        fireColorsChanged();
    }

    public void setSecondary(Color color) {
        secondary = color;
        fireColorsChanged();
    }

    public void setStrokeWidth(int width) {
        strokeWidth = width;
        repaint();
    }

    public void setStrokeStyle(StrokeStyle style) {
        strokeStyle = style;
        repaint();
    }

    public void setFillMode(FillMode mode) {
        fillMode = mode;
        repaint();
    }

    public void setOpacity(float value) {
        opacity = value;
        repaint();
    }

    public void setAntialiasing(boolean on) {
        antialiasing = on;
        repaint();
    }

    public void setGridVisible(boolean visible) {
        gridVisible = visible;
        repaint();
    }

    private void applyQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                antialiasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                antialiasing ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private AlphaComposite transparency() {
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
    }

    private Color drawColor() {
        return useSecondColor ? secondary : primary;
    }

    private Color otherColor() {
        return useSecondColor ? primary : secondary;
    }

    public double getZoom() {
        return zoom;
    }

    public void zoomIn() {
        setZoom(nextZoom(true), null);
    }

    public void zoomOut() {
        setZoom(nextZoom(false), null);
    }

    public void resetZoom() {
        setZoom(1, null);
    }

    private double nextZoom(boolean in) {
        if (in) {
            for (double level : ZOOM_LEVELS) {
                if (level > zoom + 1e-9) {
                    return level;
                }
            }
        } else {
            for (int i = ZOOM_LEVELS.length - 1; i >= 0; i--) {
                if (ZOOM_LEVELS[i] < zoom - 1e-9) {
                    return ZOOM_LEVELS[i];
                }
            }
        }
        return zoom;
    }

    private void setZoom(double newZoom, Point anchor) {
        if (newZoom == zoom) {
            return;
        }
        if (anchor == null) {
            Rectangle visible = getVisibleRect();
            anchor = new Point(visible.x + visible.width / 2, visible.y + visible.height / 2);
        }
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
        Point anchorInViewport = viewport == null ? null : SwingUtilities.convertPoint(this, anchor, viewport);
        double imageX = anchor.x / zoom;
        double imageY = anchor.y / zoom;

        zoom = newZoom;
        revalidate();
        if (viewport != null) {
            viewport.getParent().validate();
            Point canvasInView = SwingUtilities.convertPoint(this, 0, 0, viewport.getView());
            Dimension view = viewport.getViewSize();
            Dimension extent = viewport.getExtentSize();
            int x = (int) Math.round(canvasInView.x + imageX * zoom - anchorInViewport.x);
            int y = (int) Math.round(canvasInView.y + imageY * zoom - anchorInViewport.y);
            x = Math.max(0, Math.min(x, view.width - extent.width));
            y = Math.max(0, Math.min(y, view.height - extent.height));
            viewport.setViewPosition(new Point(x, y));
        }
        repaint();
        fireStateChanged();
    }

    private Point toImage(Point screen) {
        return new Point((int) Math.floor(screen.x / zoom), (int) Math.floor(screen.y / zoom));
    }

    private boolean isInImage(Point p) {
        return p.x >= 0 && p.y >= 0 && p.x < image.getWidth() && p.y < image.getHeight();
    }

    private Point clampToImage(Point p) {
        return new Point(Math.max(0, Math.min(p.x, image.getWidth() - 1)),
                Math.max(0, Math.min(p.y, image.getHeight() - 1)));
    }

    private class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
            if (tool == Tool.SELECT && e.isPopupTrigger()) {
                showSelectionMenu(e);
                return;
            }
            if (gestureButton == e.getButton()) {
                cancelGesture();
            }
            boolean left = SwingUtilities.isLeftMouseButton(e);
            boolean right = SwingUtilities.isRightMouseButton(e);
            if ((!left && !right) || gestureButton != 0) {
                return;
            }
            if (tool == Tool.SELECT && right) {
                return;
            }
            gestureButton = e.getButton();
            useSecondColor = right;
            Point p = toImage(e.getPoint());

            switch (tool) {
                case SELECT:
                    startSelection(p);
                    break;
                case PENCIL:
                case ERASER:
                    freehand = new Path2D.Float();
                    freehand.moveTo(p.x + 0.5f, p.y + 0.5f);
                    freehand.lineTo(p.x + 0.51f, p.y + 0.5f);
                    repaint();
                    break;
                case SPRAY:
                    saveUndo();
                    sprayPoint = p;
                    spray();
                    sprayTimer.start();
                    break;
                case FILL:
                    fill(p);
                    break;
                case PICKER:
                    pickColor(p);
                    break;
                case TEXT:
                    break;
                default:
                    startPoint = p;
                    endPoint = p;
                    break;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point p = toImage(e.getPoint());
            hoverPoint = p;
            fireCursorMoved(isInImage(p) ? p : null);
            if (freehand != null) {
                freehand.lineTo(p.x + 0.5f, p.y + 0.5f);
                repaint();
            } else if (sprayPoint != null) {
                sprayPoint = p;
            } else if (dragOffset != null) {
                selection.setLocation(p.x - dragOffset.x, p.y - dragOffset.y);
                repaint();
            } else if (startPoint != null) {
                endPoint = tool == Tool.SELECT ? clampToImage(p) : constrain(startPoint, p, e.isShiftDown());
                Rectangle r = tool == Tool.SELECT ? selectionRectangle() : null;
                fireShapeSizeChanged(r != null ? r.getSize()
                        : new Dimension(Math.abs(endPoint.x - startPoint.x), Math.abs(endPoint.y - startPoint.y)));
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (tool == Tool.SELECT && e.isPopupTrigger()) {
                showSelectionMenu(e);
                return;
            }
            if (e.getButton() != gestureButton) {
                return;
            }
            gestureButton = 0;
            Point p = toImage(e.getPoint());

            if (freehand != null) {
                saveUndo();
                Graphics2D g = image.createGraphics();
                paintFreehand(g, freehand);
                g.dispose();
                freehand = null;
                changed();
            } else if (sprayPoint != null) {
                sprayTimer.stop();
                sprayPoint = null;
            } else if (dragOffset != null) {
                dragOffset = null;
                fireStateChanged();
            } else if (startPoint != null) {
                if (tool == Tool.SELECT) {
                    finishSelection();
                } else {
                    finishShape();
                }
            } else if (tool == Tool.TEXT) {
                insertText(p);
            }
            fireShapeSizeChanged(null);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Point p = toImage(e.getPoint());
            hoverPoint = p;
            fireCursorMoved(isInImage(p) ? p : null);
            if (tool == Tool.SELECT) {
                boolean overSelection = selection != null && selection.contains(p);
                setCursor(Cursor.getPredefinedCursor(overSelection ? Cursor.MOVE_CURSOR : Cursor.CROSSHAIR_CURSOR));
            } else if (tool == Tool.ERASER) {
                repaint();
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            hoverPoint = null;
            fireCursorMoved(null);
            if (tool == Tool.ERASER) {
                repaint();
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isControlDown()) {
                double rotation = e.getPreciseWheelRotation();
                if (rotation != 0) {
                    setZoom(nextZoom(rotation < 0), e.getPoint());
                }
            } else {
                getParent().dispatchEvent(SwingUtilities.convertMouseEvent(DrawingPanel.this, e, getParent()));
            }
        }
    }

    private class KeyHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    if (gestureButton != 0) {
                        cancelGesture();
                    } else {
                        commitSelection();
                        selection = null;
                        fireStateChanged();
                    }
                    repaint();
                    break;
                case KeyEvent.VK_LEFT:
                    nudgeSelection(-1, 0, e);
                    break;
                case KeyEvent.VK_RIGHT:
                    nudgeSelection(1, 0, e);
                    break;
                case KeyEvent.VK_UP:
                    nudgeSelection(0, -1, e);
                    break;
                case KeyEvent.VK_DOWN:
                    nudgeSelection(0, 1, e);
                    break;
                default:
                    break;
            }
        }
    }

    private void cancelGesture() {
        gestureButton = 0;
        startPoint = null;
        endPoint = null;
        freehand = null;
        dragOffset = null;
        sprayPoint = null;
        sprayTimer.stop();
        fireShapeSizeChanged(null);
    }

    private Point constrain(Point a, Point b, boolean shift) {
        if (!shift) {
            return b;
        }
        int dx = b.x - a.x;
        int dy = b.y - a.y;
        if (tool == Tool.LINE) {
            double angle = Math.round(Math.atan2(dy, dx) / (Math.PI / 4)) * (Math.PI / 4);
            double length = Math.hypot(dx, dy);
            return new Point(a.x + (int) Math.round(length * Math.cos(angle)),
                    a.y + (int) Math.round(length * Math.sin(angle)));
        }
        int side = Math.max(Math.abs(dx), Math.abs(dy));
        return new Point(a.x + (dx < 0 ? -side : side), a.y + (dy < 0 ? -side : side));
    }

    private static Shape createShape(Tool shapeTool, Point a, Point b) {
        float x1 = a.x + 0.5f;
        float y1 = a.y + 0.5f;
        float x2 = b.x + 0.5f;
        float y2 = b.y + 0.5f;
        float x = Math.min(x1, x2);
        float y = Math.min(y1, y2);
        float w = Math.abs(x2 - x1);
        float h = Math.abs(y2 - y1);
        switch (shapeTool) {
            case LINE:
                return new Line2D.Float(x1, y1, x2, y2);
            case ELLIPSE:
                return new Ellipse2D.Float(x, y, w, h);
            case ROUND_RECT:
                float arc = Math.min(w, h) / 3;
                return new RoundRectangle2D.Float(x, y, w, h, arc, arc);
            case TRIANGLE:
                GeneralPath triangle = new GeneralPath();
                triangle.moveTo(x + w / 2, y);
                triangle.lineTo(x + w, y + h);
                triangle.lineTo(x, y + h);
                triangle.closePath();
                return triangle;
            default:
                return new Rectangle2D.Float(x, y, w, h);
        }
    }

    private void paintShape(Graphics2D g, Tool shapeTool, Point a, Point b) {
        applyQuality(g);
        g.setComposite(transparency());
        Shape shape = createShape(shapeTool, a, b);
        Color c1 = drawColor();
        Color c2 = otherColor();
        Stroke stroke = strokeStyle.createStroke(strokeWidth, shapeTool == Tool.LINE);

        if (shapeTool == Tool.LINE || fillMode == FillMode.OUTLINE) {
            g.setColor(c1);
            g.setStroke(stroke);
            g.draw(shape);
        } else if (fillMode == FillMode.FILLED) {
            g.setColor(c1);
            g.fill(shape);
        } else if (fillMode == FillMode.OUTLINE_FILLED) {
            g.setColor(c2);
            g.fill(shape);
            g.setColor(c1);
            g.setStroke(stroke);
            g.draw(shape);
        } else {
            Rectangle2D r = shape.getBounds2D();
            g.setPaint(new GradientPaint((float) r.getMinX(), (float) r.getMinY(), c1,
                    (float) r.getMaxX(), (float) r.getMaxY(), c2));
            g.fill(shape);
        }
    }

    private void finishShape() {
        if (!startPoint.equals(endPoint)) {
            saveUndo();
            Graphics2D g = image.createGraphics();
            paintShape(g, tool, startPoint, endPoint);
            g.dispose();
            changed();
        }
        startPoint = null;
        endPoint = null;
        repaint();
    }

    private int eraserSize() {
        return Math.max(6, strokeWidth * 3);
    }

    private void paintFreehand(Graphics2D g, Path2D path) {
        if (tool == Tool.ERASER) {
            g.setColor(secondary);
            g.setStroke(new BasicStroke(eraserSize(), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        } else {
            applyQuality(g);
            g.setComposite(transparency());
            g.setColor(drawColor());
            g.setStroke(strokeStyle.createStroke(strokeWidth, true));
        }
        g.draw(path);
    }

    private void spray() {
        if (sprayPoint == null) {
            return;
        }
        Graphics2D g = image.createGraphics();
        g.setComposite(transparency());
        g.setColor(drawColor());
        int radius = Math.max(4, strokeWidth * 3);
        for (int i = 0; i < radius * 2; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            int x = sprayPoint.x + (int) Math.round(Math.cos(angle) * distance);
            int y = sprayPoint.y + (int) Math.round(Math.sin(angle) * distance);
            g.fillRect(x, y, 1, 1);
        }
        g.dispose();
        changed();
    }

    private void fill(Point p) {
        if (!isInImage(p)) {
            return;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
        int target = pixels[p.y * w + p.x];
        int color = drawColor().getRGB();
        if (opacity >= 1f && (target & 0xFFFFFF) == (color & 0xFFFFFF)) {
            return;
        }
        saveUndo();
        boolean[] done = new boolean[w * h];
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(p.y * w + p.x);
        while (!stack.isEmpty()) {
            int index = stack.pop();
            if (done[index]) {
                continue;
            }
            int y = index / w;
            int row = y * w;
            int left = index - row;
            while (left > 0 && !done[row + left - 1] && similar(pixels[row + left - 1], target)) {
                left--;
            }
            int right = index - row;
            while (right < w - 1 && !done[row + right + 1] && similar(pixels[row + right + 1], target)) {
                right++;
            }
            for (int x = left; x <= right; x++) {
                pixels[row + x] = blend(color, pixels[row + x]);
                done[row + x] = true;
            }
            if (y > 0) {
                pushSegments(stack, pixels, done, row - w, left, right, target);
            }
            if (y < h - 1) {
                pushSegments(stack, pixels, done, row + w, left, right, target);
            }
        }
        image.setRGB(0, 0, w, h, pixels, 0, w);
        changed();
    }

    private static void pushSegments(Deque<Integer> stack, int[] pixels, boolean[] done,
                                     int row, int left, int right, int target) {
        boolean inSegment = false;
        for (int x = left; x <= right; x++) {
            boolean fillable = !done[row + x] && similar(pixels[row + x], target);
            if (fillable && !inSegment) {
                stack.push(row + x);
            }
            inSegment = fillable;
        }
    }

    private static boolean similar(int a, int b) {
        return Math.abs(((a >> 16) & 255) - ((b >> 16) & 255)) <= FILL_TOLERANCE
                && Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)) <= FILL_TOLERANCE
                && Math.abs((a & 255) - (b & 255)) <= FILL_TOLERANCE;
    }

    private int blend(int color, int pixel) {
        if (opacity >= 1f) {
            return color | 0xFF000000;
        }
        int r = Math.round(((color >> 16) & 255) * opacity + ((pixel >> 16) & 255) * (1 - opacity));
        int g = Math.round(((color >> 8) & 255) * opacity + ((pixel >> 8) & 255) * (1 - opacity));
        int b = Math.round((color & 255) * opacity + (pixel & 255) * (1 - opacity));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void pickColor(Point p) {
        if (!isInImage(p)) {
            return;
        }
        Color picked = new Color(image.getRGB(p.x, p.y));
        if (useSecondColor) {
            secondary = picked;
        } else {
            primary = picked;
        }
        fireColorsChanged();
        gestureButton = 0;
        setTool(previousTool);
    }

    private void insertText(Point p) {
        TextDialog dialog = new TextDialog(SwingUtilities.getWindowAncestor(this), textFont, drawColor());
        String text = dialog.showDialog();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        textFont = dialog.getSelectedFont();
        saveUndo();
        Graphics2D g = image.createGraphics();
        applyQuality(g);
        g.setComposite(transparency());
        g.setColor(drawColor());
        g.setFont(textFont);
        g.drawString(text, p.x, p.y + g.getFontMetrics().getAscent());
        g.dispose();
        changed();
    }

    public boolean hasSelection() {
        return selection != null;
    }

    public Dimension getSelectionSize() {
        return selection == null ? null : selection.getSize();
    }

    private void startSelection(Point p) {
        if (selection != null && selection.contains(p)) {
            if (floating == null) {
                liftSelection();
            }
            dragOffset = new Point(p.x - selection.x, p.y - selection.y);
        } else {
            commitSelection();
            selection = null;
            startPoint = clampToImage(p);
            endPoint = startPoint;
            fireStateChanged();
        }
        repaint();
    }

    private Rectangle selectionRectangle() {
        int x = Math.min(startPoint.x, endPoint.x);
        int y = Math.min(startPoint.y, endPoint.y);
        return new Rectangle(x, y, Math.abs(endPoint.x - startPoint.x) + 1, Math.abs(endPoint.y - startPoint.y) + 1);
    }

    private void finishSelection() {
        selection = startPoint.equals(endPoint) ? null : selectionRectangle();
        startPoint = null;
        endPoint = null;
        repaint();
        fireStateChanged();
    }

    private void liftSelection() {
        saveUndo();
        floating = new BufferedImage(selection.width, selection.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = floating.createGraphics();
        g.drawImage(image, -selection.x, -selection.y, null);
        g.dispose();
        Graphics2D gi = image.createGraphics();
        gi.setColor(secondary);
        gi.fill(selection);
        gi.dispose();
        modified = true;
    }

    public void commitSelection() {
        if (floating == null) {
            return;
        }
        Graphics2D g = image.createGraphics();
        g.drawImage(floating, selection.x, selection.y, null);
        g.dispose();
        floating = null;
        changed();
    }

    private void nudgeSelection(int dx, int dy, KeyEvent e) {
        if (selection == null || tool != Tool.SELECT || gestureButton != 0) {
            return;
        }
        int step = e.isShiftDown() ? 10 : 1;
        if (floating == null) {
            liftSelection();
        }
        selection.translate(dx * step, dy * step);
        e.consume();
        changed();
    }

    public void selectAll() {
        finishPending();
        setTool(Tool.SELECT);
        selection = new Rectangle(0, 0, image.getWidth(), image.getHeight());
        repaint();
        fireStateChanged();
    }

    public void copy() {
        if (selection == null) {
            return;
        }
        BufferedImage content = new BufferedImage(selection.width, selection.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = content.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, selection.width, selection.height);
        if (floating != null) {
            g.drawImage(floating, 0, 0, null);
        } else {
            g.drawImage(image, -selection.x, -selection.y, null);
        }
        g.dispose();
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(content), null);
        } catch (IllegalStateException e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void cut() {
        if (selection != null) {
            copy();
            deleteSelection();
        }
    }

    public void deleteSelection() {
        if (selection == null) {
            return;
        }
        if (floating != null) {
            floating = null;
        } else {
            saveUndo();
            Graphics2D g = image.createGraphics();
            g.setColor(secondary);
            g.fill(selection);
            g.dispose();
        }
        selection = null;
        changed();
    }

    public boolean paste() {
        Image pasted;
        try {
            Transferable content = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (content == null || !content.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                return false;
            }
            pasted = (Image) content.getTransferData(DataFlavor.imageFlavor);
        } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
            return false;
        }
        return pasteImage(pasted);
    }

    public boolean pasteImage(Image pasted) {
        ImageIcon icon = new ImageIcon(pasted);
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        if (w <= 0 || h <= 0) {
            return false;
        }
        finishPending();
        if (w > image.getWidth() || h > image.getHeight()) {
            int answer = JOptionPane.showConfirmDialog(this, I18n.get("dialog.paste.large"), I18n.get("app.name"),
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                resizeImage(Math.max(w, image.getWidth()), Math.max(h, image.getHeight()), false);
            }
        }
        saveUndo();
        floating = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = floating.createGraphics();
        g.drawImage(icon.getImage(), 0, 0, null);
        g.dispose();
        Rectangle visible = getVisibleRect();
        selection = new Rectangle((int) (visible.x / zoom), (int) (visible.y / zoom), w, h);
        setTool(Tool.SELECT);
        changed();
        return true;
    }

    private void showSelectionMenu(MouseEvent e) {
        if (selectionMenu != null) {
            selectionMenu.show(this, e.getX(), e.getY());
        }
    }

    public void clearImage() {
        finishPending();
        saveUndo();
        Graphics2D g = image.createGraphics();
        g.setColor(secondary);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
        changed();
    }

    public void resizeImage(int width, int height, boolean stretch) {
        finishPending();
        saveUndo();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setColor(secondary);
        g.fillRect(0, 0, width, height);
        if (stretch) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, 0, 0, width, height, null);
        } else {
            g.drawImage(image, 0, 0, null);
        }
        g.dispose();
        image = result;
        resized();
    }

    public void rotate(boolean clockwise) {
        int w = image.getWidth();
        int h = image.getHeight();
        AffineTransform t = new AffineTransform();
        if (clockwise) {
            t.translate(h, 0);
            t.rotate(Math.PI / 2);
        } else {
            t.translate(0, w);
            t.rotate(-Math.PI / 2);
        }
        transformImage(t, h, w);
    }

    public void flip(boolean horizontal) {
        int w = image.getWidth();
        int h = image.getHeight();
        AffineTransform t = new AffineTransform();
        if (horizontal) {
            t.translate(w, 0);
            t.scale(-1, 1);
        } else {
            t.translate(0, h);
            t.scale(1, -1);
        }
        transformImage(t, w, h);
    }

    private void transformImage(AffineTransform transform, int newWidth, int newHeight) {
        finishPending();
        saveUndo();
        BufferedImage result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, transform, null);
        g.dispose();
        image = result;
        resized();
    }

    public void invertColors() {
        finishPending();
        saveUndo();
        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] ^= 0x00FFFFFF;
        }
        image.setRGB(0, 0, w, h, pixels, 0, w);
        changed();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.scale(zoom, zoom);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, zoom < 1
                ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(image, 0, 0, null);
        if (freehand != null) {
            Graphics2D preview = (Graphics2D) g2.create();
            paintFreehand(preview, freehand);
            preview.dispose();
        }
        if (startPoint != null && tool.isShape()) {
            Graphics2D preview = (Graphics2D) g2.create();
            paintShape(preview, tool, startPoint, endPoint);
            preview.dispose();
        }
        if (floating != null) {
            g2.drawImage(floating, selection.x, selection.y, null);
        }
        g2.dispose();

        Graphics2D screen = (Graphics2D) g.create();
        if (gridVisible) {
            paintGrid(screen);
        }
        Rectangle frame = (tool == Tool.SELECT && startPoint != null) ? selectionRectangle() : selection;
        if (frame != null) {
            paintSelectionFrame(screen, frame);
        }
        if (tool == Tool.ERASER && hoverPoint != null) {
            int size = (int) Math.round(eraserSize() * zoom);
            int cx = (int) Math.round((hoverPoint.x + 0.5) * zoom);
            int cy = (int) Math.round((hoverPoint.y + 0.5) * zoom);
            screen.setColor(Color.BLACK);
            screen.drawRect(cx - size / 2, cy - size / 2, size, size);
        }
        screen.dispose();
    }

    private void paintGrid(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 45));
        int width = (int) Math.round(image.getWidth() * zoom);
        int height = (int) Math.round(image.getHeight() * zoom);
        for (int x = GRID_STEP; x < image.getWidth(); x += GRID_STEP) {
            int sx = (int) Math.round(x * zoom);
            g.drawLine(sx, 0, sx, height);
        }
        for (int y = GRID_STEP; y < image.getHeight(); y += GRID_STEP) {
            int sy = (int) Math.round(y * zoom);
            g.drawLine(0, sy, width, sy);
        }
    }

    private void paintSelectionFrame(Graphics2D g, Rectangle r) {
        int x = (int) Math.round(r.x * zoom);
        int y = (int) Math.round(r.y * zoom);
        int w = Math.max(1, (int) Math.round(r.width * zoom) - 1);
        int h = Math.max(1, (int) Math.round(r.height * zoom) - 1);
        g.setColor(Color.WHITE);
        g.drawRect(x, y, w, h);
        g.setColor(SELECTION_COLOR);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 4f}, 0f));
        g.drawRect(x, y, w, h);
    }

    private void fireStateChanged() {
        if (listener != null) {
            listener.stateChanged();
        }
    }

    private void fireColorsChanged() {
        if (listener != null) {
            listener.colorsChanged();
        }
    }

    private void fireCursorMoved(Point p) {
        if (listener != null) {
            listener.cursorMoved(p);
        }
    }

    private void fireShapeSizeChanged(Dimension size) {
        if (listener != null) {
            listener.shapeSizeChanged(size);
        }
    }
}
