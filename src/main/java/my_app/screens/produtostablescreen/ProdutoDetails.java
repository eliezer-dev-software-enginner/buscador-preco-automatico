package my_app.screens.produtostablescreen;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
import megalodonte.ComputedState;
import megalodonte.ListState;
import megalodonte.State;
import megalodonte.base.KeyBind;
import megalodonte.base.UI;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Button;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ButtonProps;
import megalodonte.props.ColumnProps;
import megalodonte.props.RowProps;
import megalodonte.props.TextProps;
import megalodonte.router.RouteParamsAware;
import megalodonte.router.v4.ScreenContext;
import megalodonte.utils.related.TextVariant;
import my_app.*;
import my_app.models.FornecedorModel;
import my_app.models.ProdutoModel;
import org.controlsfx.control.Notifications;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ProdutoDetails implements ScreenComponent, RouteParamsAware {

    private final ScreenContext ctx;
    private String cod;
    ListState<ProdutoModel> produtoModelListState = ListState.of(List.of());

    public ProdutoDetails(ScreenContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onRouteParams(Map<String, String> params) {
        this.cod = params.get("cod");

        System.out.println("cod received: " + cod);
    }

    @Override
    public void onMount() {
        try{
          var produtos =  Main.jsonDB.listarProdutosPorCodigo(cod);
          UI.runOnUi(()-> produtoModelListState.set(produtos));
        }catch (Exception e){}
//
//        KeyBind keyBind = new KeyBind()
//                .on(KeyCode.DIGIT1, ()-> {}, KeyBind.Modifier.SHIFT)
//                .on(KeyCode.S,  () -> System.out.println("salvar"), KeyBind.Modifier.CTRL)
//                .on(KeyCode.F5, () -> System.out.println("reload"));

        //ctx.whenReady((scene)-> keyBind.attach(scene));
    }

    @Override
    public Component render() {
        List<ProdutoModel> fornecedores;
        try {
            fornecedores = Main.jsonDB.listarProdutosPorCodigo(cod);
        } catch (IOException e) {
            Components.ShowAlertError(e.getMessage());
            return new Text("Erro ao carregar fornecedores");
        }

        Column col = new Column(new ColumnProps().paddingAll(20))
                .c_child(
                        new Row().children(
                                new Button("< Voltar").onClick(()-> ctx.navigate("produtos")),
                                new Text("Detalhes do produto", new TextProps().variant(TextVariant.SUBTITLE))
                        )
                )
                .c_child(new SpacerVertical(20))
                .c_child(
                        new Row().children(
                                Components.TextWithDetails("Código: ", cod),
                                new Button("Copiar código").onClick(()-> copyToClipboard(cod, "Código copiado!"))
                        )
                );
               // .c_child(Components.TextWithDetails("Titulo: ", model.getTituloBusca()));

        for (int i = 0; i < fornecedores.size(); i++) {
            ProdutoModel f = fornecedores.get(i);
            String cnpjFromUrl = Main.jsonDB.cnpjFromUrl(f.getUrlEncontrada());

            col.c_child(new Text("-------- Fornecedor " + (i + 1) + " --------------"))
                    .c_child(Components.TextWithDetailsAndButton("URL: ", f.getUrlEncontrada(),
                            "Abrir", ()->{
                                Utils.abrirUrlEmBrowser(f.getUrlEncontrada());
                            }))
                    .c_child(Components.TextWithDetailsAndButton("CNPJ: ", cnpjFromUrl,"Copiar", ()->{
                        copyToClipboard(cnpjFromUrl, "CNPJ copiado para o teclado!");
                    }))
                    .c_child(Components.TextWithDetailsAndButton("Preço: ", Utils.toBRLCurrency(f.getPrecoEncontrado()),"Copiar preço", ()->{
                        copyToClipboard(Utils.toBRLNumber(f.getPrecoEncontrado()), "Preço copiado para o teclado!");
                    }))
                    .c_child(new SpacerVertical(20))
                    .c_child(ProdutoDetails.getItemDetailsRow(f))
                    .c_child(new SpacerVertical(10));
        }

        return col;
    }


    private void copyToClipboard(String strToCopy, String message) {
        var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        var content = new ClipboardContent();
        content.putString(strToCopy);
        clipboard.setContent(content);
        Components.ShowPopup(ctx.selfStage(), message);
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

}
