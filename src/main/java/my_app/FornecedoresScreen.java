package my_app;

import megalodonte.ForEachState;
import megalodonte.ListState;
import megalodonte.State;
import megalodonte.base.Redirect;
import megalodonte.base.UI;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Button;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.inputs.Input;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Row;
import megalodonte.props.*;
import megalodonte.router.v3.ScreenContext;
import my_app.models.FornecedorModel;

import java.util.List;

public class FornecedoresScreen implements ScreenComponent {
    private final ScreenContext context;
    State<String> cnpj = State.of("");
    State<String> siteUrl = State.of("");

    ListState<FornecedorModel> fornecedorModelListState = ListState.of(List.of());

    public FornecedoresScreen(ScreenContext context){
        this.context = context;
        loadList();
    }

    void loadList(){
        UI.runOnUi(()->{
            try{
              fornecedorModelListState.set(Main.jsonDB.listarFornecedores());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Component render() {

        ForEachState<FornecedorModel, Component> forEachState = ForEachState.of(fornecedorModelListState, (it)-> fornecedorItem(it));

        return new Column(new ColumnProps().paddingAll(20))
                .children(
                        new Button("Siga-me no Github").onClick(()-> Redirect.to("https://github.com/eliezer-dev-software-enginner")),
                        new SpacerVertical(10),
                        new Text("Algumas facilidades para agilizar o cadastro de fornecedores no Siplan Web"),
                        new SpacerVertical(20),
                        new Text("CNPJ do fornecedor"),
                        new Input(cnpj, new InputProps().placeHolder("Ex: xxxxxxxxxx").borderColor("black")),
                        new Text("Site do fornecedor"),
                        new Input(siteUrl, new InputProps().placeHolder("Ex: www.site.com").borderColor("black")),
                        new SpacerVertical(10),
                        new Button("Adicionar").onClick(this::handleClickAdicionar),
                        new SpacerVertical(10),
                        new Column().items(forEachState,true),
                        new SpacerVertical(20),
                        new Row(new RowProps().centerHorizontally()).children(
                                new Text("Criado por Eliezer - 2026", new TextProps().fontSize(12))
                        )
                );
    }

    Component fornecedorItem(FornecedorModel model){
        return new Row(new RowProps().spacingOf(10)).children(
                new Column().children(
                        new Text("CNPJ"),
                        new Text(model.cnpj())
                ),
               new Column().children(
                       new Text("Site"),
                       new Text(model.site())
               )
        );
    }

    void handleClickAdicionar(){
        UI.runOnUi(()->{
            try{
                String urlTrimmed = siteUrl.get().trim();
                if(Main.jsonDB.urlJaExiste(urlTrimmed)){
                    Components.ShowAlertError("URL já cadastrada");
                    return;
                }

                String baseUrl = getBaseUrl(urlTrimmed);
                System.out.println(baseUrl);

                String cnpj_ = cnpj.get().trim();
                if(!CnpjValidator.isValid(cnpj_)){
                    Components.ShowAlertError("CNPJ inválido");
                    return;
                }

                var data = new FornecedorModel(cnpj_, baseUrl);

                Main.jsonDB.salvarFornecedor(data);
                fornecedorModelListState.add(data);

                EventBus.getInstance().publish(ModelCadastradoEvent.getInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    String getBaseUrl(String fullUrl){
        String[] splited = fullUrl.split(".br/");

        //vai até o .com
        return splited[0] + ".br";
    }
}
