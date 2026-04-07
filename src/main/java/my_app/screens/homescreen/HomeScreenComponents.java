package my_app.screens.homescreen;

import megalodonte.State;
import megalodonte.base.components.Component;
import megalodonte.components.Button;
import megalodonte.components.SpacerHorizontal;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.inputs.Input;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ButtonProps;
import megalodonte.props.InputProps;
import megalodonte.props.RowProps;
import my_app.Utils;

import java.util.List;

import static my_app.Components.*;
import static my_app.Components.SelectColumn;

public class HomeScreenComponents {
    public static Row topForm(State<String> codigo,State<String> tituloBusca,
                              Runnable handleClearInputs, Runnable handleClickSearch,
                              Runnable handleClickSave) {
        return new Row().children(
                new Column().children(
                        new Text("Código"),
                        new Input(codigo, new InputProps().placeHolder("").borderColor("black"))
                ),
                new SpacerHorizontal(20),
                new Column().children(
                        new Text("Título da busca"),
                        new Input(tituloBusca,
                                new InputProps().placeHolder("Ex: avental").borderColor("black"))
                ),
                new SpacerHorizontal(20),
                new Column().children(
                        new SpacerVertical(13),
                        new Button("Buscar", new ButtonProps().height(30)).onClick(handleClickSearch)
                ),
                new SpacerHorizontal(20),
                new Column().children(
                        new SpacerVertical(13),
                        new Button("Limpar", new ButtonProps().height(30).bgColor("#DD0426")).onClick(handleClearInputs)
                ),
                new SpacerHorizontal(20),
                new Column().children(
                        new SpacerVertical(13),
                        new Button("Salvar", new ButtonProps().height(30)).onClick(handleClickSave)
                ),
                new SpacerHorizontal(20),
                new Column().children(
                        new SpacerVertical(13),
                        new Button("Buscar no Google", new ButtonProps().height(30)).onClick(()->{
                            Utils.pesquisarTextNoGoogle(tituloBusca.get().trim());
                        })
                )
        );
    }

    static ButtonProps propsbtnClearFields = new ButtonProps().bgColor("#C1666B");

    public static Component produtoForm(State<String> urlState, State<String> precoState, State<String> imprimiuState,
                                 State<String> cadastrouNoSiplanState, Runnable handleClear){
        return new Row(new RowProps().spacingOf(10))
                .children(
                        InputColumn("url", urlState, "Url encontrada"),
                        InputColumnCurrency("Preço", precoState),
                        SelectColumn("Imprimiu?", List.of("Sim","Não"), imprimiuState, it->it),
                        SelectColumn("Registou no Siplan?", List.of("Sim","Não"), cadastrouNoSiplanState, it->it),
                        new Button("Limpar campos", propsbtnClearFields).onClick(handleClear)
                );
    }

}
