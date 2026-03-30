package my_app.webscrapping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Scraper do LoganMed.
 *
 * Observações do HTML real:
 *
 * 1. Produto INDISPONÍVEL → o div ".acoes-produto" tem a classe "indisponivel"
 *    e o bloco de preço renderiza apenas o formulário "avise-me", sem texto de preço.
 *    Esses produtos são IGNORADOS — não faz sentido cotar algo fora de estoque.
 *
 * 2. Produto DISPONÍVEL com promoção → usa strong.preco-promocional.cor-principal.titulo
 *    que carrega também o atributo data-sell-price="25.00" (valor numérico limpo).
 *
 * 3. Produto DISPONÍVEL sem promoção → usa strong.preco-normal ou similar.
 *
 * Hierarquia de extração de preço (do mais confiável ao mais genérico):
 *   a) data-sell-price em qualquer strong dentro do bloco de preço  ← mais confiável
 *   b) strong.preco-promocional.cor-principal
 *   c) strong.preco-promocional
 *   d) span.desconto-a-vista strong.cor-principal  (preço PIX)
 *   e) strong.preco-normal
 *   f) .preco strong
 */
public class LoganMedScrapper extends WebscrappingBase {

    public LoganMedScrapper() {
        super("https://www.loganmed.com.br");
    }

    @Override
    public List<ResultSearch> searchProduct(String search, int limit) {
        String query = search.trim().replace(" ", "+");
        String url = urlBase + "/buscar?q=" + query;

        try {
            Document doc = connectAndGetHtml(url);
            Elements produtos = doc.select("div.listagem-item");

            List<ResultSearch> results = new ArrayList<>();
            for (Element produto : produtos) {
                if (results.size() >= limit) break;

                // Ignora produtos indisponíveis —
                // Verifica se o produto tem a classe "indisponivel"
                boolean indisponivel = produto.hasClass("indisponivel");

                // alternativa extra (mais seguro)
                if (!produto.select(".bandeira-indisponivel").isEmpty()) {
                    indisponivel = true;
                }
                if (indisponivel) continue;

                String nome  = produto.select("a.nome-produto").text();
                String preco = extrairPreco(produto);
                String link  = produto.select("a.nome-produto").attr("href");

                if (!link.isBlank() && !link.startsWith("http")) link = urlBase + link;
                if (!nome.isBlank()) results.add(new ResultSearch(nome, preco, link));
            }
            return results;

        } catch (IOException e) {
            return List.of();
        }
    }

    private String extrairPreco(Element produto) {
        // 1. data-sell-price — presente mesmo quando o texto do strong está formatado
        //    de forma diferente. Ex: data-sell-price="25.00"
        Element comSellPrice = produto.selectFirst("strong[data-sell-price]");
        if (comSellPrice != null) {
            String val = comSellPrice.attr("data-sell-price").trim();
            if (!val.isEmpty()) {
                // Converte "25.00" → "R$ 25,00"
                return "R$ " + val.replace(".", ",");
            }
        }

        // 2. Seletores de texto, do mais específico ao mais genérico
        String[] seletores = {
                "strong.preco-promocional.cor-principal.titulo",
                "strong.preco-promocional.cor-principal",
                "strong.preco-promocional",
                "span.desconto-a-vista strong.cor-principal",
                "strong.preco-normal",
                ".preco strong",
        };
        for (String seletor : seletores) {
            String v = produto.select(seletor).text().trim();
            if (!v.isEmpty()) return v;
        }

        // 3. meta itemprop="price" — presente mesmo em indisponíveis, mas chegamos
        //    aqui só com produtos disponíveis, então é seguro como último recurso
        Element meta = produto.selectFirst("meta[itemprop=price]");
        if (meta != null) {
            String content = meta.attr("content").trim();
            if (!content.isEmpty()) return "R$ " + content.replace(".", ",");
        }

        return "";
    }
}