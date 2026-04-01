package my_app;

import my_app.webscrapping.KeywordScorer;

public class CotacaoServiceTest {

    private static final String TITULO = "catéter nasal 20 un";
    private static final String PALAVRAS_CHAVE = "cateter nasal/tipo óculos/atóxico/caixa/20 unidades/20 un";

    public static void main(String[] args) {
        System.out.println("=== Testes do KeywordScorer ===\n");

        testarCasoUsuario();
        testarMatchBasico();
        testarNormalizacaoAcentos();
        testarVariantesNumericas();
        testarTokensParciais();
        testarFuzzyMatch();
        testarMatchesProduct();

        System.out.println("\n=== Testes de Integração do CotacaoService ===\n");

        testarCotacaoService();
    }

    static void testarCasoUsuario() {
        String nome = "Cateter Nasal Para Oxigênio Tipo Óculos - PCT C/20un- Embramed";
        int score = KeywordScorer.calcularScore(nome, TITULO, PALAVRAS_CHAVE);

        System.out.println("--- Caso do Usuário ---");
        System.out.println("Nome: " + nome);
        System.out.println("Score: " + score);
        System.out.println("Esperado: >= 3 (pelo menos cateter nasal + tipo oculos + tokens do titulo)");
        System.out.println("Status: " + (score >= 3 ? "✓ PASSOU" : "✗ FALHOU"));
        System.out.println();
    }

    static void testarMatchBasico() {
        System.out.println("--- Match Básico ---");

        assertScore("cateter nasal", TITULO, PALAVRAS_CHAVE, 2, "termo completo no titulo");
        assertScore("cateter nasal para oxigenio", TITULO, PALAVRAS_CHAVE, 4, "mais tokens");
        assertScore("cateter nasal tipo oculos 20un", TITULO, PALAVRAS_CHAVE, 6, "todos os tokens principais");

        System.out.println();
    }

    static void testarNormalizacaoAcentos() {
        System.out.println("--- Normalização de Acentos ---");

        assertScore("cateter nasal tipo oculos", TITULO, PALAVRAS_CHAVE, 5,
            "sem acentos (átóxico -> atoxico)");
        assertScore("catéter násal tipo óculos", TITULO, PALAVRAS_CHAVE, 5,
            "com acentos");

        System.out.println();
    }

    static void testarVariantesNumericas() {
        System.out.println("--- Variantes Numéricas (20un, 20 un, 20unidades) ---");

        assertScore("cateter nasal 20un", TITULO, PALAVRAS_CHAVE, 4,
            "20un (sem espaco)");
        assertScore("cateter nasal 20 un", TITULO, PALAVRAS_CHAVE, 4,
            "20 un (com espaco)");
        assertScore("cateter nasal 20unidades", TITULO, PALAVRAS_CHAVE, 4,
            "20unidades (por extenso)");

        System.out.println();
    }

    static void testarTokensParciais() {
        System.out.println("--- Tokens Parciais ---");

        assertScore("cateter nasal oculos", TITULO, PALAVRAS_CHAVE, 5,
            "tipo omitido");
        assertScore("cateter nasal", TITULO, PALAVRAS_CHAVE, 3,
            "apenas cateter + nasal (minimo relevante)");

        System.out.println();
    }

    static void testarFuzzyMatch() {
        System.out.println("--- Fuzzy Match ---");

        assertScore("cateter nasal oculos", TITULO, PALAVRAS_CHAVE, 5,
            "sem fuzzy necessario");
        assertScore("cateter nassal oculos", TITULO, PALAVRAS_CHAVE, 4,
            "fuzzy: nassal ~ nasal (aceita diferenca)");

        System.out.println();
    }

    static void testarMatchesProduct() {
        System.out.println("--- matchesProduct (threshold >= 4) ---");

        assertMatch("Cateter Nasal Para Oxigenio Tipo Oculos - PCT C/20un- Embramed",
            TITULO, PALAVRAS_CHAVE, true, "caso do usuario");
        assertMatch("Cateter Nasal Oculos 20un Embramed",
            TITULO, PALAVRAS_CHAVE, true, "versao minima com 3+ palavras-chave");
        assertMatch("Cateter Nasal Tipo Oculos Embramed",
            TITULO, PALAVRAS_CHAVE, true, "3 palavras-chave presentes");
        assertMatch("Sonda Nasal Adulto",
            TITULO, PALAVRAS_CHAVE, false, "produto diferente (apenas nasal, sem cateter)");
        assertMatch("Cateter Urinario",
            TITULO, PALAVRAS_CHAVE, false, "produto diferente (cateter mas nao nasal)");

        System.out.println();
    }

    static void testarCotacaoService() {
        System.out.println("--- Teste de Integracao CotacaoService ---");

        my_app.webscrapping.CotacaoService cotacaoService = new my_app.webscrapping.CotacaoService();

        cotacaoService.buscarAsync(
            TITULO,
            PALAVRAS_CHAVE,
            resultados -> {
                System.out.println("Busca concluida - " + resultados.size() + " resultados:");
                for (int i = 0; i < resultados.size(); i++) {
                    var r = resultados.get(i);
                    System.out.println((i + 1) + ". [" + r.fonte() + "] score=" + r.score());
                    System.out.println("   " + r.nomeProduto());
                }
            },
            erro -> System.err.println("Erro: " + erro)
        );

        try {
            Thread.sleep(35000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void assertScore(String nome, String titulo, String palavrasChave,
                                    int minExpected, String desc) {
        int score = KeywordScorer.calcularScore(nome, titulo, palavrasChave);
        String status = score >= minExpected ? "✓ PASSOU" : "✗ FALHOU";
        System.out.printf("%s | Score: %d (min: %d) | %s%n", status, score, minExpected, desc);
    }

    private static void assertMatch(String nome, String titulo, String palavrasChave,
                                    boolean expected, String desc) {
        boolean result = KeywordScorer.matchesProduct(nome, titulo, palavrasChave);
        String status = result == expected ? "✓ PASSOU" : "✗ FALHOU";
        System.out.printf("%s | matchesProduct: %s (esperado: %s) | %s%n",
            status, result, expected, desc);
    }
}
