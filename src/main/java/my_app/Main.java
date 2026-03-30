package my_app;

import javafx.stage.Stage;
import megalodonte.ListenerManager;
import megalodonte.application.Context;
import megalodonte.application.MegalodonteApp;
import megalodonte.router.v3.Router;
import my_app.hotreload.HotReload;

import java.util.Set;

public class Main {
    static HotReload hotReload;
    static boolean devMode = true;

    public static JsonDB jsonDB = new JsonDB();
    public static Stage stage;

    static void main() {
        MegalodonteApp.run(context -> {

            final var stage = context.javafxStage();
            Main.stage = stage;
            stage.setTitle("licita-facil por Eliezer Dev");
            stage.setWidth(900);
            stage.setHeight(650);

            Router router = AppRouter.build();

            context.useRouter(router);
            context.useView(router.entrypoint().view());

            initialize(context);

            MegalodonteApp.onShutdown(() -> {
                System.out.println("Clicked on X - close application");

                if (hotReload != null) hotReload.stop();
                ListenerManager.disposeAll();
                EventBus.getInstance().disposeAll();
            });
        });
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
                    ))
                    .useRouter(); // sinaliza que há roteamento definido → hasRouteDefined = true
            hotReload.start();
        }
    }
}