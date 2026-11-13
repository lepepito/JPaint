package paint;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

public class ImageDropHandler extends TransferHandler {
    private final PaintFrame frame;

    public ImageDropHandler(PaintFrame frame) {
        this.frame = frame;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        try {
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if (files.isEmpty()) {
                return false;
            }
            File file = files.get(0);
            SwingUtilities.invokeLater(() -> frame.openDroppedFile(file));
            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            return false;
        }
    }
}
