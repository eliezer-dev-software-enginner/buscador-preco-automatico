package my_app.webscrapping.print;

import javafx.print.PageOrientation;

public class PrintCssBuilder {

    /**
     * Gera CSS que:
     * - Oculta tudo no @media print
     * - Exibe apenas as páginas desejadas
     * - Define orientação (portrait/landscape)
     */
    public static String build(PrintProfile profile) {
        StringBuilder css = new StringBuilder();

        // Orientação
        String orientation = profile.getOrientation() == PageOrientation.LANDSCAPE
                ? "landscape" : "portrait";

        css.append("""
            @media print {
                @page { 
                    size: A4 %s;
                    margin: 10mm;
                }
            """.formatted(orientation));

        // Se tem páginas específicas, oculta todas e mostra só as desejadas
        if (profile.getPagesToPrint() != null && !profile.getPagesToPrint().isEmpty()) {
            // Oculta tudo
            css.append("""
                    body > * { display: none !important; }
                    .page, [class*='page'], [id*='page'] { display: none !important; }
                """);

            // Mostra apenas as páginas selecionadas
            for (int page : profile.getPagesToPrint()) {
                css.append("""
                        .page:nth-child(%d),
                        [class*='page']:nth-child(%d) { 
                            display: block !important; 
                        }
                    """.formatted(page, page));
            }
        }

        css.append("}"); // fecha @media print
        return css.toString();
    }
}