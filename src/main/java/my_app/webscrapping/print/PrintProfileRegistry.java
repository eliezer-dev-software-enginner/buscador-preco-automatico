package my_app.webscrapping.print;

import javafx.print.PageOrientation;
import java.util.*;

public class PrintProfileRegistry {

    private final List<PrintProfile> profiles = new ArrayList<>();

    public PrintProfileRegistry() {
        // Mercado Livre: portrait, páginas 1 e 4, frente e verso
        profiles.add(new PrintProfile(
                "mercadolivre.com",
                PageOrientation.PORTRAIT,
                List.of(1, 4),
                true
        ));

        // Medject: landscape, páginas 1 e 4, frente e verso
        profiles.add(new PrintProfile(
                "medjet.com.br",
                PageOrientation.LANDSCAPE,
                List.of(1, 4),
                true
        ));

        // Padrão global: portrait, todas as páginas, frente e verso
        profiles.add(new PrintProfile(
                "*",
                PageOrientation.PORTRAIT,
                null,
                true
        ));
    }

    public PrintProfile resolve(String url) {
        return profiles.stream()
                .filter(p -> p.getUrlPattern().equals("*") || url.contains(p.getUrlPattern()))
                .findFirst()
                .orElse(getDefaultProfile());
    }

    private PrintProfile getDefaultProfile() {
        return new PrintProfile("*", PageOrientation.PORTRAIT, null, true);
    }
}