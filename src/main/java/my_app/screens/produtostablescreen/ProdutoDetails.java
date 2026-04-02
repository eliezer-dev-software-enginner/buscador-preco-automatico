package my_app.screens.produtostablescreen;

import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import megalodonte.ComputedState;
import megalodonte.ForEachState;
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
import megalodonte.router.v4.ScreenContext;
import megalodonte.utils.related.TextVariant;
import my_app.*;
import my_app.models.ProdutoModel;

import java.io.IOException;
import java.util.List;

public class ProdutoDetails implements ScreenComponent {
    private final ScreenContext ctx;
    private String cod;
    ListState<ProdutoModel> produtoModelListState = ListState.ofEmpty();

    public ProdutoDetails(ScreenContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onMount() {
        var params = ctx.getParams();
        this.cod = params.get("cod");

        loadProdutosByCodigo();

        KeyBind keyBind = createKeyBind();
        ctx.whenReady((scene) -> keyBind.attach(scene));
    }

    @Override
    public Component render() {

        ColumnProps columnProps = new ColumnProps().spacingOf(5);

        ForEachState<ProdutoModel, Component> produtoModelComponentForEachState = ForEachState.of(
                produtoModelListState, model-> {
                    String cnpjFromUrl = JsonDB.cnpjFromUrl(model.getUrlEncontrada());
                    int index = produtoModelListState.indexOf(model) + 1;

                    return new Column(columnProps)
                            .children(
                                    Components.TextWithDetailsAndButton("URL: ", model.getUrlEncontrada(),
                                            "Abrir", ()-> Utils.abrirUrlEmBrowser(model.getUrlEncontrada())),
                                    Components.TextWithDetailsAndButton("CNPJ: ", cnpjFromUrl,String.format( "Copiar (SHIFT + %s)",index), ()-> copyToClipboard(cnpjFromUrl, "CNPJ copiado para o teclado!")),
                                    Components.TextWithDetailsAndButton("Preço: ", Utils.toBRLCurrency(model.getPrecoEncontrado()),String.format( "Copiar (ALT + %s)",index), ()->{
                                        copyToClipboard(Utils.toBRLNumber(model.getPrecoEncontrado()), "Preço copiado para o teclado!");
                                    }),
                                    getItemDetailsRow(model)
                            );
                }
        );

        return new Column(new ColumnProps().paddingAll(20))
                .children(
                        new Row().children(
                                new Button("< Voltar").onClick(()-> ctx.navigate("produtos")),
                                new Text("Detalhes do produto", new TextProps().variant(TextVariant.SUBTITLE))
                        ),
                        new SpacerVertical(10),
                        new Row().children(
                                Components.TextWithDetails("Código: ", cod),
                                new Button("Copiar código (CTRL + q)").onClick(()-> copyToClipboard(cod, "Código copiado!"))
                        ),
                        new SpacerVertical(30)
                )
                .items(produtoModelComponentForEachState, 20);
    }


    public Row getItemDetailsRow(ProdutoModel f) {
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

    private void loadProdutosByCodigo() {
        try {
            var produtos = Main.jsonDB.listarProdutosPorCodigo(cod);
            UI.runOnUi(() -> produtoModelListState.set(produtos));
        } catch (Exception ignored) {}
    }

    private KeyBind createKeyBind() {
        KeyBind keyBind = new KeyBind();

        // Bind para copiar código
        keyBind.on(KeyCode.Q, () -> copyToClipboard(this.cod, "Código copiado + "), KeyBind.Modifier.CTRL);
        keyBind.on(KeyCode.ESCAPE, () -> ctx.navigate("produtos"));

        // Binds para CNPJ (Shift + 1,2,3)
        for (int i = 0; i < 3; i++) {
            final int index = i;
            KeyCode keyCode = getDigitKeyCode(index + 1);

            keyBind.on(keyCode, () -> copyCnpjAtIndex(index), KeyBind.Modifier.SHIFT);
            keyBind.on(keyCode, () -> copyPrecoAtIndex(index), KeyBind.Modifier.ALT);
        }

        return keyBind;
    }

    private KeyCode getDigitKeyCode(int digit) {
        return switch (digit) {
            case 1 -> KeyCode.DIGIT1;
            case 2 -> KeyCode.DIGIT2;
            case 3 -> KeyCode.DIGIT3;
            default -> throw new IllegalArgumentException("Digit must be 1-3");
        };
    }

    private void copyCnpjAtIndex(int index) {
        if (produtoModelListState.get().size() > index) {
            var model = produtoModelListState.get().get(index);
            String cnpjFromUrl = JsonDB.cnpjFromUrl(model.getUrlEncontrada());
            copyToClipboard(cnpjFromUrl, String.format("CNPJ(%d): %s copiado com sucesso", index + 1, cnpjFromUrl));
        }
    }

    private void copyPrecoAtIndex(int index) {
        if (produtoModelListState.get().size() > index) {
            var model = produtoModelListState.get().get(index);
            String precoFormatado = Utils.toBRLNumber(model.getPrecoEncontrado());
            copyToClipboard(precoFormatado, String.format("Preço copiado para o teclado! (Preço %d)", index + 1));
        }
    }


    private void copyToClipboard(String strToCopy, String message) {
        var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        var content = new ClipboardContent();
        content.putString(strToCopy);
        clipboard.setContent(content);
        Components.ShowPopup(ctx.selfStage(), message);
    }


}
