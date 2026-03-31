package my_app.screens.homescreen;

import megalodonte.State;
import megalodonte.components.Button;
import megalodonte.components.SpacerHorizontal;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.inputs.Input;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ButtonProps;
import megalodonte.props.InputProps;

public class HomeScreenComponents {
    public static Row topForm(State<String> codigo,State<String> tituloBusca,
                              Runnable handleClearInputs, Runnable handleClickSearch,  Runnable handleClickSave) {
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
                        new Button("Limpar", new ButtonProps().height(30)).onClick(handleClearInputs)
                ),
                new SpacerHorizontal(20),
                new Column().children(
                        new SpacerVertical(13),
                        new Button("Salvar", new ButtonProps().height(30)).onClick(handleClickSave)
                )
        );
    }
}
