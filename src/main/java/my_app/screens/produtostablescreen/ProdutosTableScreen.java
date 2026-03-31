package my_app.screens.produtostablescreen;

import javafx.stage.Stage;
import megalodonte.ListState;
import megalodonte.base.UI;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.SimpleTable;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ColumnProps;
import megalodonte.props.TextProps;
import megalodonte.router.v3.ScreenContext;
import my_app.*;
import my_app.models.ProdutoModel;

import java.util.List;

public class ProdutosTableScreen implements ScreenComponent {

    private final Stage stage;
    private final ListState<ProdutoModel> produtosListState = ListState.of(List.of());

    public ProdutosTableScreen(ScreenContext screenContext) {
        this.stage = screenContext.selfStage();
    }

    @Override
    public void onMount() {
        fetchData();

        EventBus.getInstance().subscribe(event -> {
            if (event instanceof ModelCadastradoEvent || event instanceof ModelAtualizacaoEvent) {
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
                    Components.ShowModal( ProdutoComponents.ItemDetails(it, stage), 800,690);
                });
    }

}
