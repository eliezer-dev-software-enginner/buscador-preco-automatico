package my_app;

import megalodonte.*;
import megalodonte.base.Redirect;
import megalodonte.base.UI;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.*;
import megalodonte.components.inputs.Input;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.Row;
import megalodonte.props.*;
import megalodonte.router.v3.ScreenContext;
import my_app.models.ProdutoModel;
import megalodonte.base.components.Component;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class HomeScreen implements ScreenComponent {
    private final ScreenContext context;

    State<String> tituloBusca = State.of("");
    State<String> codigo = State.of("");
    State<Boolean> buscarWasClicked = State.of(false);

    State<String> urlState1=State.of(""),  precoState1=State.of("0"), imprimiuState1=State.of("Não"), cadastrouNoSiplanState1=State.of("Não");
    State<String> urlState2=State.of(""),  precoState2=State.of("0"), imprimiuState2=State.of("Não"), cadastrouNoSiplanState2=State.of("Não");
    State<String> urlState3=State.of(""),  precoState3=State.of("0"), imprimiuState3=State.of("Não"), cadastrouNoSiplanState3=State.of("Não");

    ComputedState<String> precoMedioComputedState = ComputedState.of(()->{
        double preco1 = Double.parseDouble(precoState1.get()) / 100.0;
        double preco2 = Double.parseDouble(precoState2.get()) / 100.0;
        double preco3 = Double.parseDouble(precoState3.get()) / 100.0;

        double result = (preco1 + preco2 + preco3)/3;

        return Utils.toBRLCurrency(BigDecimal.valueOf(result));
    }, precoState1, precoState2, precoState3 );

    public HomeScreen(ScreenContext context) {
        this.context = context;
    }

    public Component render() {

        return new Container().children(
                menuBar(),
                new Column(new ColumnProps().paddingAll(20))
                        .children(
                                new Button("Siga-me no Github").onClick(()-> Redirect.to("https://github.com/eliezer-dev-software-enginner")),
                                new SpacerVertical(10),
                                new Text("Algumas facilidades para agilizar o cadastro de fornecedores no Siplan"),
                                new SpacerVertical(20),
                                topForm(),
                                new SpacerVertical(20),
                                Show.when(buscarWasClicked, ()->
                                        new Column(new ColumnProps().spacingOf(10))
                                                .children(
                                                        Components.produtoForm(urlState1,  precoState1, imprimiuState1, cadastrouNoSiplanState1),
                                                        Components.produtoForm(urlState2,  precoState2, imprimiuState2, cadastrouNoSiplanState2),
                                                        Components.produtoForm(urlState3,  precoState3, imprimiuState3, cadastrouNoSiplanState3)
                                                )
                                ),
                                new SpacerVertical(20),
                                Components.TextWithValue("Média: ", precoMedioComputedState),
                                new SpacerVertical(20),
                                new Text("Criado por Eliezer - 2026", new TextProps().fontSize(12))
                        )
        );
    }

    private Component menuBar(){
        return new MenuBar()
                .menu(new Menu("Cadastros")
                        .item("Fornecedores", ()-> {
                          // var stage =  new Stage();
                           //stage.setTitle("Fornecedores - Site/CNPJ");

                          context.router().spawnWindow("fornecedores",e-> System.out.println(e.getMessage()));

//                           var scene = new Scene((Parent) new FornecedoresScreen(router).render().getJavaFxNode());
//                           stage.setScene(scene);
//                           stage.setWidth(800);
//                           stage.setHeight(600);
//
//                           stage.show();
                        })
                        .item("Ver Produtos", ()-> {
//                            var stage =  new Stage();
//                            stage.setTitle("Produtos");
//
//                            var scene = new Scene((Parent) new ProdutosTableScreen(router).render().getJavaFxNode());
//                            stage.setScene(scene);
//                            stage.setWidth(800);
//                            stage.setHeight(600);
//
//                            stage.show();
                        })
                );
    }

    private Row topForm() {
        return new Row().children(
                new Column().children(
                        new Text("Código"),
                        new Input(codigo, new InputProps().placeHolder("").borderColor("black"))
                ),
                new SpacerHorizontal(20),
                new Column().children(
                        new Text("Titulo da busca"),
                        new Input(tituloBusca, new InputProps().placeHolder("Ex: Produto teste").borderColor("black")),
                        new SpacerVertical(10)
                ),
                new SpacerHorizontal(20),
                new Column().children(
                        new SpacerVertical(13),
                        new Button("Buscar", new ButtonProps().height(30)).onClick(this::buscar)
                ),
                new SpacerHorizontal(20),
                new Column().children(
                        new SpacerVertical(13),
                        new Button("Limpar", new ButtonProps().height(30)).onClick(()->{
                            limparInputs();
                            buscarWasClicked.set(false);
                        })
                ),
                new SpacerHorizontal(20),
                new Column().children(
                        new SpacerVertical(13),
                        new Button("Salvar", new ButtonProps().height(30)).onClick(this::salvar)
                )
        );
    }


//EventBus.getInstance().publish(DadosFinanceirosAtualizadosEvent.getInstance());

    //EventBus.getInstance().subscribe(event -> {
    //            if (event instanceof DadosFinanceirosAtualizadosEvent) {
    //                calcularFinanceiroMesAtual();
    //            }
    //        });
    void buscar(){
        buscarWasClicked.set(true);
        String query = tituloBusca.get().trim();

        if(query.isEmpty()){
            Components.ShowAlertError("Titulo da busca está vazio!");
            return;
        }

        if(codigo.get().trim().isEmpty()){
            Components.ShowAlertError("Código está vazio!");
            return;
        }

        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://www.google.com/search?q=" + encoded;
        //getHostServices().showDocument(url);

        Utils.abrirUrlEmBrowser(url);
    }

    void salvar(){
        ProdutoModel model1 = new ProdutoModel();
        model1.tituloBusca = tituloBusca.get().trim();
        model1.codigo = codigo.get().trim();

        model1.urlEncontrada = urlState1.get().trim();
        model1.precoEncontrado = Utils.deCentavosParaReal(precoState1.get());
        model1.imprimiu = imprimiuState1.get().equals("Sim");
        model1.imprimiu = cadastrouNoSiplanState1.get().equals("Sim");

        ProdutoModel model2 = new ProdutoModel();
        model2.tituloBusca = tituloBusca.get().trim();
        model2.codigo = codigo.get().trim();

        model2.urlEncontrada = urlState2.get().trim();
        model2.precoEncontrado = Utils.deCentavosParaReal(precoState2.get());
        model2.imprimiu = imprimiuState2.get().equals("Sim");
        model2.imprimiu = cadastrouNoSiplanState2.get().equals("Sim");

        ProdutoModel model3 = new ProdutoModel();
        model3.tituloBusca = tituloBusca.get().trim();
        model3.codigo = codigo.get().trim();

        model3.urlEncontrada = urlState3.get().trim();
        model3.precoEncontrado = Utils.deCentavosParaReal(precoState3.get());
        model3.imprimiu = imprimiuState3.get().equals("Sim");
        model3.imprimiu = cadastrouNoSiplanState3.get().equals("Sim");


        UI.runOnUi(()->{
            try{
                Main.jsonDB.salvarProduto(model1);
                Components.ShowPopup(Main.stage, "Produto 1 Foi salvo");
                Main.jsonDB.salvarProduto(model2);
                Components.ShowPopup(Main.stage, "Produto 2 Foi salvo");
                Main.jsonDB.salvarProduto(model3);
                Components.ShowPopup(Main.stage, "Produto 3 Foi salvo");

                EventBus.getInstance().publish(ModelCadastradoEvent.getInstance());

                limparInputs();
            } catch (Exception e) {
                Components.ShowAlertError(e.getMessage());
            }
        });

    }


    void limparInputs(){
        codigo.set("");

        urlState1.set("");
        precoState1.set("0");
        imprimiuState1.set("Não");
        cadastrouNoSiplanState1.set("Não");
        //
        urlState2.set("");
        precoState2.set("0");
        imprimiuState2.set("Não");
        cadastrouNoSiplanState2.set("Não");
        //
        urlState3.set("");
        precoState3.set("0");
        imprimiuState3.set("Não");
        cadastrouNoSiplanState3.set("Não");
    }

}
