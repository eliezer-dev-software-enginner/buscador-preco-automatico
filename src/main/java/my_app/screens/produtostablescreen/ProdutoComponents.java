package my_app.screens.produtostablescreen;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
import megalodonte.ComputedState;
import megalodonte.State;
import megalodonte.base.UI;
import megalodonte.base.components.Component;
import megalodonte.components.Button;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ButtonProps;
import megalodonte.props.ColumnProps;
import megalodonte.props.RowProps;
import megalodonte.props.TextProps;
import megalodonte.utils.related.TextVariant;
import my_app.*;
import my_app.models.FornecedorModel;
import my_app.models.ProdutoModel;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ProdutoComponents {
    public static String cnpjFromUrl(String url){
        System.out.println(url);
        try {
            FornecedorModel fornecedorModel = Main.jsonDB.buscarFornecedorPorUrl(url);
            if(fornecedorModel == null) return "Falha/CNPJ";
            return fornecedorModel.cnpj();
        } catch (IOException e) {
            Components.ShowAlertError(e.getMessage());
            return "Falha/CNPJ";
        }
    }
}
