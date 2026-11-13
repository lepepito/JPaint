package paint;

import java.awt.ComponentOrientation;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;

public final class I18n {
    public static final Locale FRENCH = Locale.FRENCH;
    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale ARABIC = Locale.forLanguageTag("ar-MA");

    private static final String BUNDLE_NAME = "paint.i18n.messages";
    private static final List<Runnable> UPDATERS = new ArrayList<>();

    private static Locale locale = FRENCH;
    private static ResourceBundle bundle = load(FRENCH);

    private I18n() {
    }

    public static Locale systemLocale() {
        String language = Locale.getDefault().getLanguage();
        if (language.equals("en")) {
            return ENGLISH;
        }
        if (language.equals("ar")) {
            return ARABIC;
        }
        return FRENCH;
    }

    public static void setLocale(Locale newLocale) {
        locale = newLocale;
        bundle = load(newLocale);
        Locale.setDefault(newLocale);
        JComponent.setDefaultLocale(newLocale);
        UIManager.put("OptionPane.okButtonText", get("dialog.ok"));
        UIManager.put("OptionPane.cancelButtonText", get("dialog.cancel"));
        UIManager.put("OptionPane.yesButtonText", get("dialog.yes"));
        UIManager.put("OptionPane.noButtonText", get("dialog.no"));
        for (Runnable updater : new ArrayList<>(UPDATERS)) {
            updater.run();
        }
    }

    public static Locale getLocale() {
        return locale;
    }

    public static ComponentOrientation orientation() {
        return ComponentOrientation.getOrientation(locale);
    }

    public static String get(String key) {
        return raw(key).replace("&", "");
    }

    public static String format(String key, Object... values) {
        return new MessageFormat(get(key), locale).format(values);
    }

    private static String raw(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }

    public static <T extends AbstractButton> T text(T button, String key) {
        onChange(() -> applyText(button, key));
        return button;
    }

    public static JLabel text(JLabel label, String key) {
        onChange(() -> label.setText(get(key)));
        return label;
    }

    public static <T extends JComponent> T tooltip(T component, String key) {
        onChange(() -> component.setToolTipText(get(key)));
        return component;
    }

    public static void onChange(Runnable updater) {
        updater.run();
        UPDATERS.add(updater);
    }

    private static void applyText(AbstractButton button, String key) {
        String value = raw(key);
        int amp = value.indexOf('&');
        button.setText(value.replace("&", ""));
        char letter = (amp >= 0 && amp + 1 < value.length()) ? Character.toUpperCase(value.charAt(amp + 1)) : 0;
        if ((letter >= 'A' && letter <= 'Z') || (letter >= '0' && letter <= '9')) {
            button.setMnemonic(letter);
            button.setDisplayedMnemonicIndex(amp);
        } else {
            button.setMnemonic(0);
        }
    }

    private static ResourceBundle load(Locale l) {
        return ResourceBundle.getBundle(BUNDLE_NAME, l, new Utf8Control());
    }

    private static class Utf8Control extends ResourceBundle.Control {
        @Override
        public List<String> getFormats(String baseName) {
            return FORMAT_PROPERTIES;
        }

        @Override
        public Locale getFallbackLocale(String baseName, Locale l) {
            return null;
        }

        @Override
        public ResourceBundle newBundle(String baseName, Locale l, String format, ClassLoader loader,
                                        boolean reload) throws IOException {
            String resource = toResourceName(toBundleName(baseName, l), "properties");
            InputStream in = loader.getResourceAsStream(resource);
            if (in == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(reader);
            }
        }
    }
}
