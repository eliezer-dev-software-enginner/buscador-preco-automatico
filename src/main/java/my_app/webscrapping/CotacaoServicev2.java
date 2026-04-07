package my_app.webscrapping;

import my_app.Utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Orquestra a busca em paralelo com timeout global controlado.
 *
 * Configurações importantes para evitar timeout:
 *   TIMEOUT_TOTAL_SEGUNDOS  — tempo máximo para TODA a busca (scrapers + PNCP)
 *   PNCP_MAX_PAGINAS        — páginas por modalidade no PNCP (cada página = ~400ms)
 *   PNCP_JANELA_DIAS        — quanto tempo atrás buscar no PNCP
 *
 * Custo estimado do PNCP:
 *   2 modalidades × PNCP_MAX_PAGINAS × (400ms sleep + ~300ms HTTP) = N segundos
 *   Com PNCP_MAX_PAGINAS=2: ~2,8s    ← recomendado para uso cotidiano
 *   Com PNCP_MAX_PAGINAS=5: ~7s
 */
public class CotacaoServicev2 {

    // --- Configurações de performance ---
    private static final int TIMEOUT_TOTAL_SEGUNDOS = 30;
    private static final int LIMITE_POR_SCRAPER     = 10;
    private static final int PNCP_MAX_PAGINAS       = 3;   // ↑ para mais resultados, ↓ para mais velocidade
    private static final int PNCP_JANELA_DIAS       = 210;

    private final List<WebscrappingBase> scrapers = List.of(
            new LoganMedScrapper()
            //new GabmedicScrapper(),
            //new KajavetScrapper(),
            //new DrogariaEFarmaScrapper(),
            //new EspecifarmaScrapper(),
            //new MagazineMedicaScrapper(),
            //new RhosseScrapper(),
            //new DrogaraiaScrapper(),
            //new CfcarehospitalarScrapper(),
            //new AlthisScrapperv2(),
            //new MedjetScrapper(),
            //new SuturasOnlineScrapper()
    );

    // -------------------------------------------------------------------------
    // DTO
    // -------------------------------------------------------------------------
    public record ResultadoCotacao(
            String nomeProduto,
            String preco,
            String link,
            String fonte,
            int    score,
            boolean encontrado
    ) {
        public static ResultadoCotacao vazio(String fonte) {
            return new ResultadoCotacao("Não encontrado", "", "", fonte, -1, false);
        }
    }

    // -------------------------------------------------------------------------
    // API assíncrona — use esta na HomeScreen
    // -------------------------------------------------------------------------
    public void buscarAsync(
            String tituloBusca,
            String palavrasChave,
            Consumer<List<ResultadoCotacao>> onConcluido,
            Consumer<String> onErro
    ) {
        Thread.ofVirtual().start(() -> {
            try {
                onConcluido.accept(buscar(tituloBusca, palavrasChave));
            } catch (Exception e) {
                onErro.accept(e.getMessage());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Busca síncrona — NÃO chame na thread da UI
    // Retorna sempre exatamente 3 itens: [slot1, slot2, slot3]
    //
    // Slot 1 → melhor do PNCP; se PNCP vazio → 3º melhor marketplace
    // Slot 2 → melhor marketplace por score
    // Slot 3 → 2º melhor marketplace (fonte diferente do slot 2)
    // -------------------------------------------------------------------------
    public List<ResultadoCotacao> buscar(String tituloBusca, String palavrasChave) {

        ExecutorService pool = Executors.newFixedThreadPool(scrapers.size() + 1);

        // Dispara tudo em paralelo
//        Future<ResultadoCotacao> futurePncp =
//                pool.submit(() -> buscarMelhorPncp(tituloBusca, palavrasChave));

        List<Future<List<ResultadoCotacao>>> futuresScrapers = scrapers.stream()
                .map(s -> pool.submit(() -> buscarComScore(s, tituloBusca, palavrasChave)))
                .toList();

        pool.shutdown();

        // Aguarda com timeout global — ninguém segura a UI mais que isso
        try {
            pool.awaitTermination(TIMEOUT_TOTAL_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        // Coleta PNCP (pode ter terminado ou não dentro do timeout)
       // ResultadoCotacao pncp = obter(futurePncp, ResultadoCotacao.vazio("PNCP"));

        // Coleta e ordena marketplaces por score
        List<ResultadoCotacao> todos = new ArrayList<>();
        for (var f : futuresScrapers) {
            todos.addAll(obter(f, List.of()));
        }
        todos.sort(Comparator.comparingInt(ResultadoCotacao::score).reversed());

        // Top 3 de fontes diferentes
        List<ResultadoCotacao> top3 = new ArrayList<>();
        for (ResultadoCotacao r : todos) {
            if (top3.stream().noneMatch(x -> x.fonte().equals(r.fonte()))) top3.add(r);
            if (top3.size() == 3) break;
        }
        while (top3.size() < 3) top3.add(ResultadoCotacao.vazio("Marketplace " + top3.size()));

        // Monta slots: slot1 = PNCP ou fallback pro 3º marketplace
        //ResultadoCotacao slot1 = pncp.encontrado() ? pncp : top3.get(2);
        ResultadoCotacao slot1 = top3.get(2);
        ResultadoCotacao slot2 = top3.get(0);
        ResultadoCotacao slot3 = top3.get(1);

        return List.of(slot1, slot2, slot3);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private List<ResultadoCotacao> buscarComScore(
            WebscrappingBase scraper, String tituloBusca, String palavrasChave) {
        try {
            String fonte = Utils.nomeFonte(scraper.urlBase);
            return scraper.searchProduct(tituloBusca, LIMITE_POR_SCRAPER).stream()
                    .map(r -> new ResultadoCotacao(
                            r.nomeProdutoEncontrado(), r.preco(), r.link(), fonte,
                            KeywordScorerv2.calcularScore(
                                    r.nomeProdutoEncontrado(), tituloBusca, palavrasChave),
                            true))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private <T> T obter(Future<T> future, T fallback) {
        try {
            // Timeout individual por Future — garante que nenhum scraper trava o resultado
            return future.get(TIMEOUT_TOTAL_SEGUNDOS, TimeUnit.SECONDS);
        } catch (Exception e) {
            return fallback;
        }
    }

    static void main() {
      var list = new CotacaoServicev2().buscar("eletrodo descartável ecg","100 unidades");
      /*
      ResultadoCotacao[nomeProduto=Eletrodo ECG Quadrado C/ 50 Unidades - Solidor, preco=R$ 16,50, link=https://www.loganmed.com.br/eletrodo-ecg-quadrado-c-50-unidades-solidor, fonte=loganmed.com.br, score=0, encontrado=true]
ResultadoCotacao[nomeProduto=Eletrodo Descartável ECG Retangular Adulto Pacote com 50 - Medix, preco=14,90, link=https://www.althis.com.br/eletrodo-descartavel-p-ecg-adulto-pct-c-50-und-medix, fonte=althis.com.br, score=8, encontrado=true]
ResultadoCotacao[nomeProduto=Eletrodo Descartável ECG 44x32mm com 50 Un. Solidor, preco=R$ 14,57, link=https://www.medjet.com.br/equipamentos/ecg-eletrocardiografia/eletrodo-descartavel-ecg-44x32mm-msgst-06-pct-c-50un-solidor, fonte=medjet.com.br, score=8, encontrado=true]

       */

      list.forEach(System.out::println);
    }
}