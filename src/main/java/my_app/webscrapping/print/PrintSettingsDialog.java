package my_app.webscrapping.print;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PrintSettingsDialog {

    public static void show(Stage owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Configurações de Impressão");
        dialog.setHeaderText("Preferências padrão do diálogo de impressão");

        CheckBox duplexCheck = new CheckBox("Imprimir frente e verso (Duplex)");
        duplexCheck.setSelected(PrintDialogPreferences.loadDuplex());

        CheckBox colorCheck = new CheckBox("Imprimir colorido");
        colorCheck.setSelected(PrintDialogPreferences.loadColor());

        Spinner<Integer> copiesSpinner = new Spinner<>(1, 99, PrintDialogPreferences.loadCopies());
        copiesSpinner.setEditable(true);

        ComboBox<String> paperCombo = new ComboBox<>();
        paperCombo.getItems().addAll("A4", "A3", "Carta", "Ofício");
        paperCombo.setValue(PrintDialogPreferences.loadPaper());

        VBox content = new VBox(12,
                duplexCheck,
                colorCheck,
                new Label("Número de cópias:"), copiesSpinner,
                new Label("Tamanho do papel:"), paperCombo
        );
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);

        ButtonType saveBtn = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                PrintDialogPreferences.saveDuplex(duplexCheck.isSelected());
                PrintDialogPreferences.saveColor(colorCheck.isSelected());
                PrintDialogPreferences.saveCopies(copiesSpinner.getValue());
                PrintDialogPreferences.savePaper(paperCombo.getValue());
            }
            return null;
        });

        dialog.showAndWait();
    }
}