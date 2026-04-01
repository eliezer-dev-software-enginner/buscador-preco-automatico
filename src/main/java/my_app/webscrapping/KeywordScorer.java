package my_app.webscrapping;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KeywordScorer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public static int calcularScore(String nomeProduto, String tituloBusca, String palavrasChave) {
        if (nomeProduto == null || nomeProduto.isBlank()) return -1;

        String nomeNorm = normalizar(nomeProduto);
        String tituloNorm = normalizar(tituloBusca);

        Set<String> tokensNome = tokenize(nomeNorm);
        Set<String> tokensTitulo = tokenize(tituloNorm);

        int score = 0;

        int tituloMatches = countTokenMatches(tokensTitulo, tokensNome);
        score += tituloMatches * 3;

        int tituloMatchCount = 0;
        for (String token : tokensTitulo) {
            if (tokensNome.contains(token)) {
                tituloMatchCount++;
            }
        }
        if (tituloMatchCount >= 2) {
            score += 3;
        }

        if (palavrasChave != null && !palavrasChave.isBlank()) {
            List<String> keywords = Arrays.stream(palavrasChave.split("/"))
                    .map(String::trim)
                    .filter(k -> !k.isBlank())
                    .toList();

            int kwMatches = 0;
            for (String kw : keywords) {
                String kwNorm = normalizar(kw);
                
                if (kwNorm.contains(" ")) {
                    if (matchesFlexible(kwNorm, tokensNome)) {
                        score += 2;
                        kwMatches++;
                    } else if (nomeNorm.contains(kwNorm)) {
                        score += 2;
                        kwMatches++;
                    }
                } else {
                    if (matchesFlexible(kwNorm, tokensNome)) {
                        score++;
                        kwMatches++;
                    } else if (nomeNorm.contains(kwNorm)) {
                        score++;
                        kwMatches++;
                    }
                }
            }
            
            if (kwMatches >= 3) {
                score += 3;
            }
        }

        return Math.max(0, score);
    }

    public static boolean matchesProduct(String nomeProduto, String tituloBusca, String palavrasChave) {
        return calcularScore(nomeProduto, tituloBusca, palavrasChave) >= 4;
    }

    private static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        return Arrays.stream(WHITESPACE.split(text))
                .filter(t -> !t.isBlank() && t.length() > 1)
                .collect(Collectors.toSet());
    }

    private static int countTokenMatches(Set<String> tokensBuscar, Set<String> tokensNome) {
        int matches = 0;
        for (String token : tokensBuscar) {
            if (containsToken(token, tokensNome)) {
                matches++;
            }
        }
        return matches;
    }

    private static boolean containsToken(String token, Set<String> tokens) {
        if (tokens.contains(token)) return true;
        if (token.length() < 4) return false;
        for (String t : tokens) {
            if (t.length() < 4) continue;
            if (t.equals(token)) return true;
            if (t.startsWith(token) || token.startsWith(t)) return true;
        }
        return false;
    }

    private static boolean matchesFlexible(String keyword, Set<String> tokensNome) {
        if (tokensNome.contains(keyword)) return true;

        for (String token : tokensNome) {
            if (fuzzyMatch(keyword, token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean fuzzyMatch(String a, String b) {
        if (a.equals(b)) return true;
        if (a.length() < 3 || b.length() < 3) return false;

        if (a.contains(b) || b.contains(a)) return true;

        int distance = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        int threshold = Math.max(2, maxLen / 3);
        return distance <= threshold;
    }

    private static String normalizeVariants(String s) {
        return s.replaceAll("(?i)(un|unidades?|caixa|pac|pacote|cx)\\.?", "")
                .replaceAll("\\d+", "")
                .trim();
    }

    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[a.length()][b.length()];
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
