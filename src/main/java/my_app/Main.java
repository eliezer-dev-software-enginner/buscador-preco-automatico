package my_app;

import javafx.stage.Stage;
import megalodonte.ListenerManager;
import megalodonte.application.Context;
import megalodonte.application.MegalodonteApp;
import megalodonte.router.v3.RouteProps;
import megalodonte.router.v3.Router;
import my_app.hotreload.HotReload;
import my_app.screens.ConfiguracoesScreen;
import my_app.screens.FornecedoresScreen;
import my_app.screens.HomeScreen;

import java.util.Set;

public class Main {
    static HotReload hotReload;
    static boolean devMode = false;

    public static JsonDB jsonDB = new JsonDB();
    public static Stage stage;

    static void main() {
        MegalodonteApp.run(context -> {

            final var stage = context.javafxStage();
            Main.stage = stage;
            stage.setTitle("licita-facil por Eliezer Dev");
            stage.setWidth(900);
            stage.setHeight(650);

            var router = getRouter();

            context.useRouter(router);
            context.useView(router.entrypoint().view());

            initialize(context);

            MegalodonteApp.onShutdown(() -> {
                System.out.println("Clicked on X - close application");

                if(hotReload != null) hotReload.stop();
                ListenerManager.disposeAll();
                EventBus.getInstance().disposeAll();
            });
        });
    }

    private static Router getRouter() {

        var routes = Set.of(
                new Router.Route("home", ctx -> new HomeScreen(ctx),
                        new RouteProps(900, 550,null, false)),

                new Router.Route("fornecedores",ctx-> new FornecedoresScreen(ctx),
                        new RouteProps(900, 550, "Fornecedores", true)),
                new Router.Route("configuracoes",ctx-> new ConfiguracoesScreen(ctx),
                        new RouteProps(900, 550, "Tela de configurações", true)),
                new Router.Route("produtos",ctx-> new ConfiguracoesScreen(ctx),
                        new RouteProps(900, 550, "Listagem de produtos", true))
        );

        return new Router(routes, "home");
    }

    public static void initialize(Context context) {

        if (devMode) {
           hotReload = new HotReload()
                .sourcePath("src/main/java")
                .classesPath("build/classes/java/main")
                .resourcesPath("src/main/resources")
                .implementationClassName("my_app.hotreload.UIReloaderImpl")
                .screenClassName("my_app.screens.HomeScreen")
                .reloadContext(context)
                .classesToExclude(Set.of(
                    "my_app.Main",
                    "my_app.hotreload.Reloader",
                    "my_app.hotreload.UIReloaderImpl",
                    "my_app.hotreload.HotReload",
                    "my_app.hotreload.HotReloadClassLoader"
                ));
           hotReload.start();
        }
    }
}