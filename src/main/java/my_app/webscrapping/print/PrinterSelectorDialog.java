package my_app.webscrapping.print;

import javafx.print.Printer;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PrinterSelectorDialog {

    public static Optional<Printer> show(Stage owner) {
        Set<Printer> printers = Printer.getAllPrinters();

        if (printers.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(owner);
            alert.setTitle("Erro");
            alert.setHeaderText("Nenhuma impressora encontrada");
            alert.setContentText(
                    "Nenhuma impressora foi detectada no sistema.\n" +
                            "Verifique se há impressoras instaladas e tente novamente."
            );
            alert.showAndWait();
            return Optional.empty();
        }

        List<String> names = printers.stream()
                .map(Printer::getName)
                .sorted()
                .toList();

        // Marca a impressora já salva
        Printer saved = PrinterPreferences.loadPrinter();
        String defaultName = saved != null ? saved.getName() : names.get(0);

        ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultName, names);
        dialog.initOwner(owner);
        dialog.setTitle("Selecionar Impressora");
        dialog.setHeaderText("Escolha a impressora padrão para esta aplicação:");
        dialog.setContentText("Impressora:");

        Optional<String> result = dialog.showAndWait();
        return result.map(PrinterUtils::findByName);
    }
}