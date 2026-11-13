package paint;

import java.io.File;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
            }

            I18n.setLocale(I18n.systemLocale());

            PaintFrame frame = new PaintFrame();
            frame.setVisible(true);

            if (args.length > 0) {
                frame.openFile(new File(args[0]));
            }
        });
    }
}
