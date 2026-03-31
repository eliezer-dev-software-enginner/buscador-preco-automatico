package my_app.webscrapping.print;

import java.util.prefs.Preferences;

public class PrintDialogPreferences {

    private static final Preferences prefs =
            Preferences.userNodeForPackage(PrintDialogPreferences.class);

    // Chaves
    private static final String KEY_DUPLEX       = "print_duplex";
    private static final String KEY_COLOR        = "print_color";
    private static final String KEY_COPIES       = "print_copies";
    private static final String KEY_PAPER        = "print_paper";

    // Duplex
    public static void saveDuplex(boolean duplex) { prefs.putBoolean(KEY_DUPLEX, duplex); }
    public static boolean loadDuplex() { return prefs.getBoolean(KEY_DUPLEX, true); }

    // Colorido ou P&B
    public static void saveColor(boolean color) { prefs.putBoolean(KEY_COLOR, color); }
    public static boolean loadColor() { return prefs.getBoolean(KEY_COLOR, false); }

    // Cópias
    public static void saveCopies(int copies) { prefs.putInt(KEY_COPIES, copies); }
    public static int loadCopies() { return prefs.getInt(KEY_COPIES, 1); }

    // Tamanho do papel
    public static void savePaper(String paper) { prefs.put(KEY_PAPER, paper); }
    public static String loadPaper() { return prefs.get(KEY_PAPER, "A4"); }
}