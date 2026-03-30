package my_app.webscrapping;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

/**
 * Calcula o score de relevância de um produto baseado nas palavras-chave.
 *
 * Sistema de pontuação:
 *   - Título da busca presente no nome: +3 pontos  (critério principal)
 *   - Cada palavra-chave (separada por "/") presente: +1 ponto cada
 *
 * Tudo é normalizado antes da comparação (sem acentos, minúsculo),
 * então "descartável" bate em "descartavel" sem problema.
 */
public class KeywordScorer {

    public static int calcularScore(String nomeProduto, String tituloBusca, String palavrasChave) {
        if (nomeProduto == null || nomeProduto.isBlank()) return -1;

        String nomeNorm = normalizar(nomeProduto);
        int score = 0;

        if (nomeNorm.contains(normalizar(tituloBusca))) {
            score += 3;
        }

        if (palavrasChave != null && !palavrasChave.isBlank()) {
            List<String> keywords = Arrays.stream(palavrasChave.split("/"))
                    .map(String::trim)
                    .filter(k -> !k.isBlank())
                    .map(KeywordScorer::normalizar)
                    .toList();

            for (String kw : keywords) {
                if (nomeNorm.contains(kw)) score++;
            }
        }

        return score;
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer
                .normalize(s.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
