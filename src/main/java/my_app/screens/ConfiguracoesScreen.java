package my_app.screens;

import megalodonte.State;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.router.v4.ScreenContext;
import my_app.Components;

import java.util.List;

public class ConfiguracoesScreen implements ScreenComponent {
    State<String> buscarNoPncp = State.of("Sim");

    public ConfiguracoesScreen(ScreenContext ctx) {
    }

    @Override
    public Component render() {
        return new Container()
                .children(
                        new Column().children(
                                new Text("Configurações do sistema"),
                                Components.SelectColumn("Buscar no PNCP", List.of("Sim","Não"), buscarNoPncp, it->it )
                        )
                );
    }

    @Override
    public void onMount() {
        ScreenComponent.super.onMount();
    }
    
    
}
