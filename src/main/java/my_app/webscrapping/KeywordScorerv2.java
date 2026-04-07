package my_app.webscrapping;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KeywordScorerv2 {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public static int calcularScore(String nomeProduto, String tituloBusca, String palavrasChave) {
        if (nomeProduto == null || nomeProduto.isBlank()) return -1;

        String nome = normalizar(nomeProduto);
        String titulo = normalizar(tituloBusca);

        // 🚀 Early exit
        if (!nome.contains(titulo)) {
            return 0;
        }

        int score = 3;

        Set<String> tokensNome = tokenize(nome);
        int matches = 0;

        for (String token : tokenize(titulo)) {
            if (tokensNome.contains(token)) {
                matches++;
            }
        }

        score += matches;

        if (matches >= 2) {
            score += 2;
        }

        // ✅ NOVO: palavrasChave como frase normal
        if (palavrasChave != null && !palavrasChave.isBlank()) {
            int kwMatches = 0;

            for (String token : tokenize(normalizar(palavrasChave))) {
                if (tokensNome.contains(token)) {
                    kwMatches++;
                }
            }

            score += kwMatches;

            if (kwMatches >= 3) {
                score += 2;
            }
        }

        return score;
    }

    private static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        return Arrays.stream(WHITESPACE.split(text))
                .filter(t -> !t.isBlank() && t.length() > 1)
                .collect(Collectors.toSet());
    }


    static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer
                .normalize(s.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
