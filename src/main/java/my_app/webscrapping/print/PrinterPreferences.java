package my_app.webscrapping.print;

import java.util.prefs.Preferences;
import javafx.print.Printer;

public class PrinterPreferences {

    private static final Preferences prefs =
            Preferences.userNodeForPackage(PrinterPreferences.class);

    private static final String KEY_PRINTER = "selected_printer";

    public static void savePrinter(Printer printer) {
        prefs.put(KEY_PRINTER, printer.getName());
    }

    public static Printer loadPrinter() {
        String savedName = prefs.get(KEY_PRINTER, null);
        if (savedName == null) return Printer.getDefaultPrinter();

        Printer found = PrinterUtils.findByName(savedName);
        if (found == null) {
            System.out.println("Impressora salva '" + savedName + "' não encontrada. Usando padrão.");
            return Printer.getDefaultPrinter();
        }
        return found;
    }

    public static void clear() {
        prefs.remove(KEY_PRINTER);
    }
}