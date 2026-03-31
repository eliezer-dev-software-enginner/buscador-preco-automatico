package my_app.webscrapping.print;

import javafx.print.Printer;
import java.util.Set;

public class PrinterUtils {

    public static Set<Printer> getAvailablePrinters() {
        return Printer.getAllPrinters();
    }

    public static void listPrinters() {
        Set<Printer> printers = Printer.getAllPrinters();
        if (printers.isEmpty()) {
            System.out.println("Nenhuma impressora encontrada no sistema.");
        } else {
            printers.forEach(p -> System.out.println("Impressora: " + p.getName()));
        }
    }

    public static Printer findByName(String name) {
        return Printer.getAllPrinters().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}