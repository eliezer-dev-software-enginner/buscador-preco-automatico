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

    public static Component ItemDetails(ProdutoModel model, Stage stage) {

        List<ProdutoModel> fornecedores;
        try {
            fornecedores = Main.jsonDB.listarProdutosPorCodigo(model.getCodigo());
        } catch (IOException e) {
            Components.ShowAlertError(e.getMessage());
            return new Text("Erro ao carregar fornecedores");
        }

        Column col = new Column(new ColumnProps().paddingAll(20))
                .c_child(new Text("Detalhes do produto", new TextProps().variant(TextVariant.SUBTITLE)))
                .c_child(new SpacerVertical(20))
                .c_child(
                        new Row().children(
                                Components.TextWithDetails("Código: ", model.getCodigo()),
                                new Button("Copiar código").onClick(()-> copyToClipboard(model.getCodigo(), stage, "Código copiado!"))
                                )
                        )
                .c_child(Components.TextWithDetails("Titulo: ", model.getTituloBusca()));

        for (int i = 0; i < fornecedores.size(); i++) {
            ProdutoModel f = fornecedores.get(i);
            String cnpjFromUrl = cnpjFromUrl(f.getUrlEncontrada());

            col.c_child(new Text("-------- Fornecedor " + (i + 1) + " --------------"))
                    .c_child(Components.TextWithDetailsAndButton("URL: ", f.getUrlEncontrada(),
                            "Abrir", ()->{
                                Utils.abrirUrlEmBrowser(f.getUrlEncontrada());
                            }))
                    .c_child(Components.TextWithDetailsAndButton("CNPJ: ", cnpjFromUrl,"Copiar", ()->{
                        copyToClipboard(cnpjFromUrl, stage, "CNPJ copiado para o teclado!");
                    }))
                    .c_child(Components.TextWithDetailsAndButton("Preço: ", Utils.toBRLCurrency(f.getPrecoEncontrado()),"Copiar preço", ()->{
                        copyToClipboard(Utils.toBRLNumber(f.getPrecoEncontrado()), stage, "Preço copiado para o teclado!");
                    }))
                    .c_child(new SpacerVertical(20))
                    .c_child(ProdutoComponents.getItemDetailsRow(f))
                    .c_child(new SpacerVertical(10));
        }

        return col;
    }

    private static void copyToClipboard(String strToCopy, Stage stage, String message) {
        var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        var content = new ClipboardContent();
        content.putString(strToCopy);
        clipboard.setContent(content);
        Components.ShowPopup(stage, message);
    }

    public static Row getItemDetailsRow(ProdutoModel f) {
        final State<Boolean> imprimiu = State.of(f.getImprimiu());
        final var imprimiuStr = ComputedState.of(()-> imprimiu.get()? "Foi Impresso": "Marcar como impresso", imprimiu);

        final State<Boolean> cadastrouNoSiplan = State.of(f.getCadastrouNoSiplan());
        final var cadastrouNoSiplanStr = ComputedState.of(()-> cadastrouNoSiplan.get()? "Foi cadastrado": "Marcar como cadastrado no Siplan", cadastrouNoSiplan);

        return new Row(new RowProps().spacingOf(30))
                .children(
                        new Button(imprimiuStr, new ButtonProps().bgColor(imprimiu.map(v-> v? "#136F63": "#211A1E")))
                                .onClick(() -> {
                            boolean newStateValue = !imprimiu.get();
                            imprimiu.set(newStateValue);

                            try {
                                Main.jsonDB.atualizarStatusDeImpressao(newStateValue, f);
                                EventBus.getInstance().publish(ModelAtualizacaoEvent.getInstance());
                            } catch (IOException e) {
                                UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
                            }
                        })
                )
                .children(
                        new Button(cadastrouNoSiplanStr, new ButtonProps().bgColor(cadastrouNoSiplan.map(v-> v? "#136F63": "#211A1E"))).onClick(() -> {
                            boolean newStateValue = !cadastrouNoSiplan.get();
                            cadastrouNoSiplan.set(newStateValue);

                            try {
                                Main.jsonDB.atualizarStatusDeCadastroNoSiplan(newStateValue, f);
                                EventBus.getInstance().publish(ModelAtualizacaoEvent.getInstance());
                            } catch (IOException e) {
                                UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
                            }
                        })
                );
    }


    public static void ShowModal(
            Component ui,
            int width,
            int height,
            Map<KeyCombination, Runnable> shortcuts){

        Stage stage = new Stage();

        var scene = new Scene((Parent) ui.getJavaFxNode(), width, height);
        stage.setScene(scene);

        // registra todos atalhos
        for (var entry : shortcuts.entrySet()) {
            scene.getAccelerators().put(entry.getKey(), entry.getValue());
        }

        stage.setTitle("Detalhes");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.show();
    }

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
