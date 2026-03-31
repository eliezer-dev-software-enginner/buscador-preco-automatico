package my_app.webscrapping.print;

import javafx.scene.web.WebEngine;

public class PrintJsInjector {

    public static void injectAndPrint(WebEngine engine, PrintProfile profile) {
        String css = PrintCssBuilder.build(profile);

        // Escapa o CSS para uso seguro dentro de JS
        String escapedCss = css
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$");

        String js = """
            (function() {
                // Remove injeção anterior se existir
                var old = document.getElementById('__smart_print_css__');
                if (old) old.remove();
                
                // Injeta novo CSS
                var style = document.createElement('style');
                style.id = '__smart_print_css__';
                style.textContent = `%s`;
                document.head.appendChild(style);
                
                // Pequeno delay para o CSS ser aplicado antes de imprimir
                setTimeout(function() {
                    window.print();
                }, 300);
            })();
            """.formatted(escapedCss);

        engine.executeScript(js);
    }
}