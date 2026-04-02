package my_app.screens.produtostablescreen;

import javafx.scene.input.*;
import megalodonte.ListState;
import megalodonte.State;
import megalodonte.base.UI;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.SimpleTable;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ColumnProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import my_app.*;
import my_app.models.ProdutoModel;

import java.util.List;

public class ProdutosTableScreen implements ScreenComponent {
    private final ScreenContext screenContext;
    private final ListState<ProdutoModel> produtosListState = ListState.ofEmpty();
    private final State<String> searchState = State.of(""); // <- novo
    private final ListState<ProdutoModel> filteredListState = ListState.ofEmpty(); // <- novo

    public ProdutosTableScreen(ScreenContext screenContext) {
        this.screenContext = screenContext;
    }

    @Override
    public void onMount() {
        fetchData();

        produtosListState.subscribe(ignored -> applyFilter());
        searchState.subscribe(ignored -> applyFilter());

        EventBus.getInstance().subscribe(event -> {
            if (event instanceof ModelCadastradoEvent || event instanceof ModelAtualizacaoEvent) {
                fetchData();
            }
        });
    }

    public Component render() {
        return new Column(new ColumnProps().paddingAll(20))
                .children(
                        searchField(),
                        table(),
                        new Text("Criado por Eliezer - 2026", new TextProps().fontSize(12))
                );
    }

    private void applyFilter() {
        String query = searchState.get().toLowerCase().trim();
        List<ProdutoModel> all = produtosListState.get();

        List<ProdutoModel> filtered = query.isEmpty()
                ? all
                : all.stream()
                .filter(p ->
                        contains(p.getCodigo(), query) ||
                                contains(p.getTituloBusca(), query) ||
                                contains(p.getUrlEncontrada(), query)
                )
                .toList();

        UI.runOnUi(() -> filteredListState.set(filtered));
    }

    private boolean contains(Object value, String query) {
        return value != null && value.toString().toLowerCase().contains(query);
    }


    void fetchData(){
        try{
            var list = Main.jsonDB.listarProdutos();
            UI.runOnUi(()->{
                produtosListState.set(list);
            });
        } catch (Exception e) {e.printStackTrace();}
    }

    private Component searchField() {
        return Components.InputColumn("Buscar por código, título ou URL...", searchState);
    }

    public Component table() {
        return new SimpleTable<ProdutoModel>()
                .fromData(filteredListState)
                .header()
                .columns()
                .column("Código", it -> it.getCodigo(), (double) 90)
                .column("Titulo-Busca", it -> it.getTituloBusca())
                .column("URL", it -> it.getUrlEncontrada())
                .column("Data de criação", it -> DateUtils.millisToBrazilianDateTime(it.getDataCriacao()))
                .end()
                .build()
                .onItemDoubleClick(it -> screenContext.navigate("produto-details/"+it.getCodigo()));
    }
}
