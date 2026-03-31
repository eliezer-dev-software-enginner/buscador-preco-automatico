package my_app;

import megalodonte.router.v3.RouteProps;
import megalodonte.router.v3.Router;
import my_app.screens.ConfiguracoesScreen;
import my_app.screens.FornecedoresScreen;
import my_app.screens.homescreen.HomeScreen;
import my_app.screens.produtostablescreen.ProdutosTableScreen;

import java.util.Set;

public class AppRouter {
    public static Router build() {
        var routes = Set.of(
                new Router.Route("home", ctx -> new HomeScreen(ctx),
                        new RouteProps(900, 550, null, false)),

                new Router.Route("fornecedores", ctx -> new FornecedoresScreen(ctx),
                        new RouteProps(900, 550, "Fornecedores", true)),

                new Router.Route("configuracoes", ctx -> new ConfiguracoesScreen(ctx),
                        new RouteProps(900, 550, "Tela de configurações", true)),

                new Router.Route("produtos", ctx -> new ProdutosTableScreen(ctx),
                        new RouteProps(900, 550, "Listagem de produtos", true))
        );

        return new Router(routes, "produtos");
    }
}
