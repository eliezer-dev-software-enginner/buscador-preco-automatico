package my_app.screens.homescreen;

import javafx.stage.Stage;
import megalodonte.ComputedState;
import megalodonte.State;
import megalodonte.v2.Show;
import megalodonte.base.Redirect;
import megalodonte.base.UI;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.*;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.props.*;
import megalodonte.router.v4.Router;
import megalodonte.router.v4.ScreenContext;
import my_app.*;
import my_app.Main;
import my_app.models.ProdutoModel;
import my_app.webscrapping.CotacaoService;
import my_app.webscrapping.CotacaoService.ResultadoCotacao;
import org.controlsfx.control.Notifications;

import java.math.BigDecimal;

public class HomeScreen implements ScreenComponent {

    private ScreenContext context;

    // --- Formulário de busca ---

    //State<String> tituloBusca   = State.of("avental");
    //State<String> palavrasChave = State.of("descartável/manga longa/tnt/polipropileno/10 unidades");
    //State<String> codigo        = State.of("5666");


    State<String> tituloBusca   = State.of("catéter nasal 20 un");
    State<String> palavrasChave = State.of("cateter nasal/tipo óculos/atóxico/caixa/20 unidades/20 un");
    State<String> codigo = State.of("8031");

    // --- Controle de UX ---
    State<Boolean> buscando         = State.of(false);

    // --- Slot 1 (PNCP ou fallback 3º marketplace) ---
    State<String> fonteLabel1           = State.of("");
    State<String> urlState1             = State.of("");
    State<String> precoState1           = State.of("0");
    State<String> imprimiuState1        = State.of("Não");
    State<String> cadastrouState1       = State.of("Não");

    // --- Slot 2 (melhor marketplace) ---
    State<String> fonteLabel2           = State.of("");
    State<String> urlState2             = State.of("");
    State<String> precoState2           = State.of("0");
    State<String> imprimiuState2        = State.of("Não");
    State<String> cadastrouState2       = State.of("Não");

    // --- Slot 3 (2º melhor marketplace, fonte diferente) ---
    State<String> fonteLabel3           = State.of("");
    State<String> urlState3             = State.of("");
    State<String> precoState3           = State.of("0");
    State<String> imprimiuState3        = State.of("Não");
    State<String> cadastrouState3       = State.of("Não");

    ComputedState<String> precoMedio = ComputedState.of(() -> {
        double p1 = Double.parseDouble(precoState1.get()) / 100.0;
        double p2 = Double.parseDouble(precoState2.get()) / 100.0;
        double p3 = Double.parseDouble(precoState3.get()) / 100.0;
        return Utils.toBRLCurrency(BigDecimal.valueOf((p1 + p2 + p3) / 3));
    }, precoState1, precoState2, precoState3);

    private final CotacaoService cotacaoService = new CotacaoService();

    public HomeScreen(ScreenContext context) { this.context = context; }
    public HomeScreen() {}

    // =========================================================================
    // Render
    // =========================================================================
    @Override
    public Component render() {
        return new Container().children(
                menuBar(),
                new Column(new ColumnProps().paddingAll(20))
                        .children(
                                new Button("Siga-me no Github")
                                        .onClick(() -> Redirect.to("https://github.com/eliezer-dev-software-enginner")),
                                new SpacerVertical(10),
                                new Text("Algumas facilidades para agilizar o cadastro de fornecedores no Siplan"),
                                new SpacerVertical(20),

                                HomeScreenComponents.topForm(codigo, tituloBusca, this::handleLimparInputs, this::buscar, this::salvar),

                                new SpacerVertical(10),
                                Components.InputColumn("Palavras-chave (separadas por /)", palavrasChave),
                                new SpacerVertical(5),
                                new Text(
                                        "💡 Serão buscados vários resultados por site. O produto com mais " +
                                                "palavras-chave no nome será selecionado automaticamente.",
                                        new TextProps().fontSize(11)
                                ),
                                new SpacerVertical(20),

                                // Spinner
                                Show.when(buscando, () ->
                                        new Text("🔍 Buscando cotações em paralelo, aguarde...",
                                                new TextProps().fontSize(13))
                                ),

                                // Resultados
                                new Column(new ColumnProps().spacingOf(14))
                                                .children(
                                                        slotCompleto(fonteLabel1, urlState1, precoState1,
                                                                imprimiuState1, cadastrouState1),
                                                        slotCompleto(fonteLabel2, urlState2, precoState2,
                                                                imprimiuState2, cadastrouState2),
                                                        slotCompleto(fonteLabel3, urlState3, precoState3,
                                                                imprimiuState3, cadastrouState3)
                                                ),
                                new SpacerVertical(20),
                                Components.TextWithValue("Média: ", precoMedio),
                                new SpacerVertical(20),
                                new Text("Criado por Eliezer - 2026", new TextProps().fontSize(12))
                        )
        );
    }

    /** Label da fonte + form do produto agrupados */
    private Component slotCompleto(State<String> fonteLabel,
                                   State<String> url, State<String> preco,
                                   State<String> imprimiu, State<String> cadastrou) {
        return new Column(new ColumnProps().spacingOf(4)).children(
                new Text(fonteLabel, new TextProps().fontSize(11).bold()),
                Components.produtoForm(url, preco, imprimiu, cadastrou)
        );
    }

    // =========================================================================
    // Menu e formulário de cabeçalho
    // =========================================================================
    private Component menuBar() {
        Router router = context.router();

        return new MenuBar()
                .menu(new Menu("Cadastros")
                        .item("Fornecedores", () ->
                                router.spawnWindow("fornecedores",
                                        e -> System.out.println(e.getMessage())))
                        .item("Produtos", ()-> router.spawnWindow("produtos",
                                e -> System.out.println(e.getMessage())))
                        .item("Abrir siplan-web", ()-> Utils.abrirUrlEmBrowser("https://pm-braspires.siplanweb.com.br/siplan-v2/siplan"))
                );
    }


    // =========================================================================
    // Ações
    // =========================================================================
    void buscar() {
        precoState1.set("0");precoState2.set("0");precoState3.set("0");

        if (tituloBusca.get().trim().isEmpty()) {
            Components.ShowAlertError("Título da busca está vazio!");
            return;
        }
        if (codigo.get().trim().isEmpty()) {
            Components.ShowAlertError("Código está vazio!");
            return;
        }

        buscando.set(true);

        cotacaoService.buscarAsync(
                tituloBusca.get().trim(),
                palavrasChave.get(),
                resultados -> UI.runOnUi(() -> {
                    preencherSlot(resultados.get(0), fonteLabel1, urlState1, precoState1);
                    preencherSlot(resultados.get(1), fonteLabel2, urlState2, precoState2);
                    preencherSlot(resultados.get(2), fonteLabel3, urlState3, precoState3);
                    buscando.set(false);
                    Components.ShowPopup(context.selfStage(),"Busca de preços finalizada");
                    AudioUtils.playAudio("mixkit-correct-answer-tone-2870.wav");

                    Notifications.create()
                            .title("Buscador de preço")
                            .text("Busca de preços finalizada!")
                            .showWarning();
                }),
                erro -> UI.runOnUi(() -> {
                    buscando.set(false);
                    Components.ShowAlertError("Erro na busca: " + erro);
                    AudioUtils.playAudio("mixkit-wrong-answer-fail-notification-946.wav");
                })
        );
    }

    private void preencherSlot(ResultadoCotacao r,
                               State<String> fonteLabel,
                               State<String> urlState,
                               State<String> precoState) {
        fonteLabel.set(r.encontrado()
                ? r.fonte() + " — " + r.nomeProduto()
                : "(" + r.fonte() + ") Não encontrado");
        urlState.set(r.link());

        // Converte "R$ 12,50" → "1250" (centavos como string inteira)
        String digits = r.preco()
                .replace("R$", "").replace(".", "").replace(",", "").trim();
        try {
            precoState.set(String.valueOf(Long.parseLong(digits)));
        } catch (NumberFormatException e) {
            precoState.set("0");
        }
    }

    void salvar() {
        Stage stage = context.selfStage();

        if(codigo.get().trim().isEmpty()){
            Components.ShowAlertError("Preencha o código!");
            return;
        }

        if(tituloBusca.get().trim().isEmpty()){
            Components.ShowAlertError("Preencha o título de busca!");
            return;
        }

        if(inputsAreInvalid(urlState1, precoState1) || inputsAreInvalid(urlState2, precoState2) ||
        inputsAreInvalid(urlState3, precoState3) ){
            Components.ShowAlertError("Confira os dados e tente de novo! (preço, url)");
            return;
        }

        UI.runOnUi(() -> {
            try {
                Main.jsonDB.salvarProduto(montarModel(1, urlState1, precoState1, imprimiuState1, cadastrouState1));
                Components.ShowPopup(stage, "Produto 1 salvo!");

                Main.jsonDB.salvarProduto(montarModel(2, urlState2, precoState2, imprimiuState2, cadastrouState2));
                Components.ShowPopup(stage, "Produto 2 salvo!");

                Main.jsonDB.salvarProduto(montarModel(3, urlState3, precoState3, imprimiuState3, cadastrouState3));
                Components.ShowPopup(stage, "Produto 3 salvo!");

                EventBus.getInstance().publish(ModelCadastradoEvent.getInstance());
                limparInputs();
            } catch (Exception e) {
                Components.ShowAlertError(e.getMessage());
            }
        });
    }

    private boolean inputsAreInvalid(State<String> urlState, State<String> precoState) {
        String precoValue = precoState.get().trim();
        return urlState.get().trim().isEmpty() || precoValue.isEmpty() || precoValue.equals("0");
    }


    private ProdutoModel montarModel(int slot,
                                     State<String> urlState, State<String> precoState,
                                     State<String> imprimiuState, State<String> cadastrouState) {

        return new ProdutoModel(
                codigo.get().trim(),
                tituloBusca.get().trim(),
                urlState.get().trim(),
                Utils.deCentavosParaReal(precoState.get()),
                imprimiuState.get().equals("Sim"),
                cadastrouState.get().equals("Sim"),
                palavrasChave.get().trim()
        );
    }

    void handleLimparInputs() {
            limparInputs();
    }

    void limparInputs() {
        codigo.set("");
        fonteLabel1.set(""); urlState1.set(""); precoState1.set("0");
        imprimiuState1.set("Não"); cadastrouState1.set("Não");

        fonteLabel2.set(""); urlState2.set(""); precoState2.set("0");
        imprimiuState2.set("Não"); cadastrouState2.set("Não");

        fonteLabel3.set(""); urlState3.set(""); precoState3.set("0");
        imprimiuState3.set("Não"); cadastrouState3.set("Não");

        precoState1.set("0");precoState2.set("0");precoState3.set("0");
    }
}