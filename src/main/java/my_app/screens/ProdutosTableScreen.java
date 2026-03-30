package my_app.screens;

import javafx.scene.input.ClipboardContent;
import megalodonte.ComputedState;
import megalodonte.ListState;
import megalodonte.State;
import megalodonte.base.UI;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Button;
import megalodonte.components.SimpleTable;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ColumnProps;
import megalodonte.props.TextProps;
import megalodonte.router.v3.Router;
import megalodonte.utils.related.TextVariant;
import my_app.*;
import my_app.models.ProdutoModel;

import java.io.IOException;
import java.util.List;

public class ProdutosTableScreen implements ScreenComponent {

    ListState<ProdutoModel> produtosListState = ListState.of(List.of());

    public ProdutosTableScreen() {
        fetchData();

        EventBus.getInstance().subscribe(event -> {
                    if (event instanceof ModelCadastradoEvent) {
                        fetchData();
                    }
                });
    }

    public Component render() {
        return new Column(new ColumnProps().paddingAll(20))
                .children(
                        table(),
                        new Text("Criado por Eliezer - 2026", new TextProps().fontSize(12))
                );
    }


    void fetchData(){
        try{
            var list = Main.jsonDB.listarProdutos();
            UI.runOnUi(()->{
                produtosListState.set(list);
            });
        } catch (Exception e) {e.printStackTrace();}
    }


    public Component table() {
        return new SimpleTable<ProdutoModel>()
                .fromData(produtosListState)
                .header()
                .columns()
                .column("Código", it -> it.getCodigo(), (double) 90)
                .column("Titulo-Busca", it -> it.getTituloBusca())
                .column("URL", it -> it.getUrlEncontrada())
                .column("Data de criação", it -> DateUtils.millisToBrazilianDateTime(it.getDataCriacao()))
                .end()
                .build()
                .onItemDoubleClick(it-> {
                    Components.ShowModal( ItemDetails(it), 800,550);
                });
    }


    String cnpjFromUrl(String url){
        System.out.println(url);
        try {
            return Main.jsonDB.buscarFornecedorPorUrl(url).cnpj();
        } catch (IOException e) {
            Components.ShowAlertError(e.getMessage());
            return "Falha/CNPJ";
        }
    }

    Component ItemDetails(ProdutoModel model) {
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
                .c_child(Components.TextWithDetails("Código: ", model.getCodigo()))
                .c_child(Components.TextWithDetails("Titulo: ", model.getTituloBusca()));

        for (int i = 0; i < fornecedores.size(); i++) {
            ProdutoModel f = fornecedores.get(i);
            String cnpjFromUrl = cnpjFromUrl(f.getUrlEncontrada());

            final State<Boolean> imprimiu = State.of(f.getImprimiu());
            final var imprimiuStr = ComputedState.of(()-> imprimiu.get()? "Foi Impresso": "Marcar como impresso", imprimiu);

            col.c_child(new Text("-------- Fornecedor " + (i + 1) + " --------------"))
                    .c_child(Components.TextWithDetailsAndButton("URL: ", f.getUrlEncontrada(),
                            "Abrir", ()->{
                                Utils.abrirUrlEmBrowser(f.getUrlEncontrada());
                    }))
                    .c_child(Components.TextWithDetailsAndButton("CNPJ: ", cnpjFromUrl,"Copiar", ()->{
                        var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                        var content = new ClipboardContent();
                        content.putString(cnpjFromUrl);
                        clipboard.setContent(content);
                        Components.ShowPopup(Main.stage, "CNPJ copiado para o teclado!");
                    }))
                    .c_child(Components.TextWithDetails("Preço: ", Utils.toBRLCurrency(f.getPrecoEncontrado())))
                    .c_child(new Button(imprimiuStr).onClick(()->{
                        boolean newStateValue = !imprimiu.get();
                        imprimiu.set(newStateValue);

                        try {
                            Main.jsonDB.atualizarStatusDeImpressao(newStateValue, f);
                        } catch (IOException e) {
                            UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
                        }
                    }));
        }

        return col;
    }
}
