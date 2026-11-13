package paint;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.Icon;

public final class Icons {
    private static final Color INK = new Color(45, 45, 45);
    private static final Color BLUE = new Color(30, 110, 210);
    private static final Color LIGHT_BLUE = new Color(205, 225, 250);

    private Icons() {
    }

    private static final class DrawnIcon implements Icon {
        private final int width;
        private final int height;
        private final double scale;
        private final Consumer<Graphics2D> painter;

        DrawnIcon(int width, int height, double scale, Consumer<Graphics2D> painter) {
            this.width = width;
            this.height = height;
            this.scale = scale;
            this.painter = painter;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.scale(scale, scale);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            painter.accept(g2);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }

    private static Icon icon(int size, Consumer<Graphics2D> painter) {
        return new DrawnIcon(size, size, size / 20.0, painter);
    }

    public static Icon disabled(Icon icon) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                icon.paintIcon(c, g2, x, y);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return icon.getIconWidth();
            }

            @Override
            public int getIconHeight() {
                return icon.getIconHeight();
            }
        };
    }

    public static Icon tool(Tool tool) {
        switch (tool) {
            case SELECT:
                return icon(20, g -> {
                    g.setColor(INK);
                    g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                            new float[] {2.5f, 2f}, 0f));
                    g.draw(new Rectangle2D.Float(3, 4, 14, 12));
                });
            case PENCIL:
                return icon(20, g -> {
                    g.rotate(Math.toRadians(-45), 10, 10);
                    g.setColor(new Color(250, 195, 40));
                    g.fill(new Rectangle2D.Float(6, 7.5f, 9.5f, 5));
                    g.setColor(new Color(235, 125, 145));
                    g.fill(new Rectangle2D.Float(15.5f, 7.5f, 3, 5));
                    GeneralPath tip = triangle(6, 7.5f, 1.5f, 10, 6, 12.5f);
                    g.setColor(new Color(240, 215, 170));
                    g.fill(tip);
                    g.setColor(INK);
                    g.fill(triangle(3.3f, 9, 1.5f, 10, 3.3f, 11));
                    g.setStroke(new BasicStroke(0.8f));
                    g.draw(new Rectangle2D.Float(6, 7.5f, 12.5f, 5));
                    g.draw(tip);
                });
            case ERASER:
                return icon(20, g -> {
                    g.rotate(Math.toRadians(-45), 10, 10);
                    RoundRectangle2D body = new RoundRectangle2D.Float(2.5f, 6.5f, 15, 7, 2.5f, 2.5f);
                    g.setColor(new Color(240, 120, 145));
                    g.fill(body);
                    Shape clip = g.getClip();
                    g.clip(body);
                    g.setColor(Color.WHITE);
                    g.fill(new Rectangle2D.Float(2.5f, 6.5f, 6, 7));
                    g.setClip(clip);
                    g.setColor(INK);
                    g.setStroke(new BasicStroke(0.9f));
                    g.draw(body);
                    g.draw(new Line2D.Float(8.5f, 6.5f, 8.5f, 13.5f));
                });
            case FILL:
                return icon(20, g -> {
                    Graphics2D b = (Graphics2D) g.create();
                    b.rotate(Math.toRadians(35), 9, 11);
                    GeneralPath bucket = new GeneralPath();
                    bucket.moveTo(3, 8);
                    bucket.lineTo(13, 8);
                    bucket.lineTo(11.8f, 17);
                    bucket.lineTo(4.2f, 17);
                    bucket.closePath();
                    b.setColor(new Color(220, 224, 232));
                    b.fill(bucket);
                    b.setColor(BLUE);
                    b.fill(new Ellipse2D.Float(3, 6.7f, 10, 2.6f));
                    b.setColor(INK);
                    b.setStroke(new BasicStroke(1f));
                    b.draw(bucket);
                    b.draw(new Arc2D.Float(4, 4, 8, 8, 0, 180, Arc2D.OPEN));
                    b.dispose();
                    GeneralPath drop = new GeneralPath();
                    drop.moveTo(16.8f, 10.5f);
                    drop.curveTo(18, 13, 19.8f, 15, 16.8f, 18.8f);
                    drop.curveTo(13.8f, 15, 15.6f, 13, 16.8f, 10.5f);
                    g.setColor(BLUE);
                    g.fill(drop);
                });
            case PICKER:
                return icon(20, g -> {
                    g.rotate(Math.toRadians(-45), 10, 10);
                    RoundRectangle2D tube = new RoundRectangle2D.Float(3.5f, 8.5f, 9, 3, 1.5f, 1.5f);
                    g.setColor(LIGHT_BLUE);
                    g.fill(tube);
                    g.setColor(BLUE);
                    g.fill(new Rectangle2D.Float(3.5f, 8.5f, 3.5f, 3));
                    g.fill(triangle(3.5f, 9, 1, 10, 3.5f, 11));
                    g.setColor(INK);
                    g.setStroke(new BasicStroke(0.8f));
                    g.draw(tube);
                    g.fill(new Rectangle2D.Float(12.5f, 7.5f, 1.8f, 5));
                    g.fill(new RoundRectangle2D.Float(14.3f, 7, 5, 6, 4, 4));
                });
            case SPRAY:
                return icon(20, g -> {
                    RoundRectangle2D can = new RoundRectangle2D.Float(3, 8, 7.5f, 11, 3, 3);
                    g.setColor(new Color(150, 160, 175));
                    g.fill(can);
                    g.setColor(INK);
                    g.setStroke(new BasicStroke(0.9f));
                    g.draw(can);
                    g.fill(new Rectangle2D.Float(4.75f, 5.5f, 4, 2.5f));
                    g.fill(new Rectangle2D.Float(8.75f, 6.1f, 2, 1.2f));
                    g.setColor(BLUE);
                    float[][] dots = {{13, 3}, {15.5f, 4.5f}, {12.5f, 6}, {17.5f, 6.5f}, {14.5f, 7.5f},
                        {17, 3}, {13.5f, 9.5f}, {16.5f, 10}};
                    for (float[] d : dots) {
                        g.fill(new Ellipse2D.Float(d[0], d[1], 1.7f, 1.7f));
                    }
                });
            case TEXT:
                return icon(20, g -> {
                    g.setColor(INK);
                    g.setFont(new Font(Font.SERIF, Font.BOLD, 18));
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString("A", (20 - fm.stringWidth("A")) / 2f, 16f);
                });
            case LINE:
                return icon(20, g -> {
                    g.setColor(INK);
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(new Line2D.Float(3.5f, 16.5f, 16.5f, 3.5f));
                });
            case ELLIPSE:
                return shapeIcon(new Ellipse2D.Float(2.5f, 4.5f, 15, 11));
            case ROUND_RECT:
                return shapeIcon(new RoundRectangle2D.Float(3, 5, 14, 10, 7, 7));
            case TRIANGLE:
                return shapeIcon(triangle(10, 3.5f, 17.5f, 16, 2.5f, 16));
            default:
                return shapeIcon(new Rectangle2D.Float(3, 5, 14, 10));
        }
    }

    private static Icon shapeIcon(Shape shape) {
        return icon(20, g -> {
            g.setColor(LIGHT_BLUE);
            g.fill(shape);
            g.setColor(INK);
            g.setStroke(new BasicStroke(1.4f));
            g.draw(shape);
        });
    }

    private static GeneralPath triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
        GeneralPath path = new GeneralPath();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.closePath();
        return path;
    }

    public static Icon newFile(int size) {
        return icon(size, g -> {
            GeneralPath page = new GeneralPath();
            page.moveTo(4, 2);
            page.lineTo(12.5f, 2);
            page.lineTo(16.5f, 6);
            page.lineTo(16.5f, 18);
            page.lineTo(4, 18);
            page.closePath();
            g.setColor(Color.WHITE);
            g.fill(page);
            g.setColor(INK);
            g.setStroke(new BasicStroke(1.1f));
            g.draw(page);
            GeneralPath fold = new GeneralPath();
            fold.moveTo(12.5f, 2);
            fold.lineTo(12.5f, 6);
            fold.lineTo(16.5f, 6);
            g.draw(fold);
        });
    }

    public static Icon open(int size) {
        return icon(size, g -> {
            GeneralPath back = new GeneralPath();
            back.moveTo(2, 4.5f);
            back.lineTo(7.5f, 4.5f);
            back.lineTo(9, 6.5f);
            back.lineTo(16.5f, 6.5f);
            back.lineTo(16.5f, 16.5f);
            back.lineTo(2, 16.5f);
            back.closePath();
            GeneralPath front = new GeneralPath();
            front.moveTo(2, 16.5f);
            front.lineTo(4.8f, 9.5f);
            front.lineTo(19, 9.5f);
            front.lineTo(16.5f, 16.5f);
            front.closePath();
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(225, 170, 50));
            g.fill(back);
            g.setColor(INK);
            g.draw(back);
            g.setColor(new Color(250, 210, 95));
            g.fill(front);
            g.setColor(INK);
            g.draw(front);
        });
    }

    public static Icon save(int size) {
        return icon(size, g -> {
            RoundRectangle2D body = new RoundRectangle2D.Float(2.5f, 2.5f, 15, 15, 2.5f, 2.5f);
            g.setColor(BLUE);
            g.fill(body);
            g.setColor(new Color(220, 225, 232));
            g.fill(new Rectangle2D.Float(6, 2.5f, 8, 5.5f));
            g.setColor(INK);
            g.fill(new Rectangle2D.Float(11, 3.5f, 1.6f, 3.5f));
            g.setColor(Color.WHITE);
            g.fill(new Rectangle2D.Float(5, 10.5f, 10, 7));
            g.setColor(new Color(160, 170, 185));
            g.draw(new Line2D.Float(6.5f, 13, 13.5f, 13));
            g.draw(new Line2D.Float(6.5f, 15.2f, 13.5f, 15.2f));
            g.setColor(INK);
            g.setStroke(new BasicStroke(1f));
            g.draw(body);
        });
    }

    public static Icon undo(int size) {
        return icon(size, Icons::paintUndoArrow);
    }

    public static Icon redo(int size) {
        return icon(size, g -> {
            g.translate(20, 0);
            g.scale(-1, 1);
            paintUndoArrow(g);
        });
    }

    private static void paintUndoArrow(Graphics2D g) {
        g.setColor(INK);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        GeneralPath arc = new GeneralPath();
        arc.moveTo(16, 16);
        arc.curveTo(16, 9.5f, 12.5f, 7, 6.5f, 7);
        g.draw(arc);
        g.fill(triangle(2.5f, 7, 7.5f, 3, 7.5f, 11));
    }

    public static Icon palette(int size) {
        return icon(size, g -> {
            Area shape = new Area(new Ellipse2D.Float(1.5f, 3, 17, 14));
            shape.subtract(new Area(new Ellipse2D.Float(10, 10.5f, 4, 3.5f)));
            g.setColor(new Color(240, 225, 195));
            g.fill(shape);
            g.setColor(INK);
            g.setStroke(new BasicStroke(1f));
            g.draw(shape);
            Color[] colors = {new Color(225, 55, 55), new Color(245, 195, 30), new Color(50, 170, 80), BLUE};
            float[][] spots = {{4, 6.5f}, {8, 4.8f}, {12.5f, 5.3f}, {4.3f, 10.5f}};
            for (int i = 0; i < colors.length; i++) {
                g.setColor(colors[i]);
                g.fill(new Ellipse2D.Float(spots[i][0], spots[i][1], 3.2f, 3.2f));
            }
        });
    }

    public static Icon position() {
        return icon(16, g -> {
            g.setColor(INK);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Ellipse2D.Float(5.5f, 5.5f, 9, 9));
            g.draw(new Line2D.Float(10, 1.5f, 10, 7));
            g.draw(new Line2D.Float(10, 13, 10, 18.5f));
            g.draw(new Line2D.Float(1.5f, 10, 7, 10));
            g.draw(new Line2D.Float(13, 10, 18.5f, 10));
        });
    }

    public static Icon selectionSize() {
        return icon(16, g -> {
            g.setColor(INK);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                    new float[] {3f, 2f}, 0f));
            g.draw(new Rectangle2D.Float(2.5f, 4.5f, 15, 11));
        });
    }

    public static Icon canvasSize() {
        return icon(16, g -> {
            g.setColor(Color.WHITE);
            g.fill(new Rectangle2D.Float(2.5f, 3.5f, 15, 13));
            g.setColor(INK);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Rectangle2D.Float(2.5f, 3.5f, 15, 13));
            g.fill(triangle(17.5f, 11.5f, 17.5f, 16.5f, 12.5f, 16.5f));
        });
    }

    public static Icon zoom() {
        return icon(16, g -> {
            g.setColor(INK);
            g.setStroke(new BasicStroke(1.8f));
            g.draw(new Ellipse2D.Float(2.5f, 2.5f, 10.5f, 10.5f));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Float(12.5f, 12.5f, 17.5f, 17.5f));
        });
    }

    public static Icon lineSample(int width) {
        return new DrawnIcon(36, 16, 1, g -> {
            g.setColor(INK);
            g.setStroke(new BasicStroke(Math.min(width, 12)));
            g.draw(new Line2D.Float(3, 8, 33, 8));
        });
    }

    public static Icon styleSample(StrokeStyle style) {
        return new DrawnIcon(36, 16, 1, g -> {
            g.setColor(INK);
            g.setStroke(style.createStroke(2, true));
            g.draw(new Line2D.Float(3, 8, 33, 8));
        });
    }

    public static Icon fillSample(FillMode mode) {
        return new DrawnIcon(22, 16, 1, g -> {
            Rectangle2D r = new Rectangle2D.Float(3, 3, 16, 10);
            g.setStroke(new BasicStroke(1.5f));
            switch (mode) {
                case FILLED:
                    g.setColor(INK);
                    g.fill(r);
                    break;
                case OUTLINE_FILLED:
                    g.setColor(LIGHT_BLUE);
                    g.fill(r);
                    g.setColor(INK);
                    g.draw(r);
                    break;
                case GRADIENT:
                    g.setPaint(new GradientPaint(3, 3, INK, 19, 13, LIGHT_BLUE));
                    g.fill(r);
                    break;
                default:
                    g.setColor(INK);
                    g.draw(r);
                    break;
            }
        });
    }

    public static Icon opacitySample(int percent) {
        return new DrawnIcon(16, 16, 1, g -> {
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    g.setColor((x + y) % 2 == 0 ? Color.WHITE : new Color(200, 200, 200));
                    g.fillRect(x * 4, y * 4, 4, 4);
                }
            }
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, percent / 100f));
            g.setColor(INK);
            g.fillRect(0, 0, 16, 16);
        });
    }

    public static List<Image> appImages() {
        List<Image> images = new ArrayList<>();
        for (int size : new int[] {16, 20, 24, 32, 40, 48, 64, 128}) {
            images.add(appImage(size));
        }
        return images;
    }

    public static BufferedImage appImage(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(size / 64.0, size / 64.0);

        g.setPaint(new GradientPaint(0, 0, new Color(70, 145, 245), 64, 64, new Color(25, 70, 165)));
        g.fill(new RoundRectangle2D.Float(2, 2, 60, 60, 16, 16));

        Area palette = new Area(new Ellipse2D.Float(8, 12, 46, 36));
        palette.subtract(new Area(new Ellipse2D.Float(31, 33, 10, 8)));
        g.setColor(new Color(250, 240, 220));
        g.fill(palette);
        Color[] colors = {new Color(230, 60, 60), new Color(250, 195, 35), new Color(55, 175, 85), new Color(35, 115, 225)};
        float[][] spots = {{14, 24}, {21, 16}, {32, 15}, {42, 20}};
        for (int i = 0; i < colors.length; i++) {
            g.setColor(colors[i]);
            g.fill(new Ellipse2D.Float(spots[i][0], spots[i][1], 8, 8));
        }

        g.rotate(Math.toRadians(-45), 44, 44);
        g.setColor(new Color(150, 90, 45));
        g.fill(new RoundRectangle2D.Float(40, 41, 20, 6, 4, 4));
        g.setColor(new Color(190, 195, 205));
        g.fill(new Rectangle2D.Float(34, 40.5f, 6, 7));
        g.setColor(INK);
        g.fill(triangle(34, 40.5f, 24, 44, 34, 47.5f));
        g.dispose();
        return img;
    }
}
