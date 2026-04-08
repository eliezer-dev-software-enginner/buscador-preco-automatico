package my_app.webscrapping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Scraper da Especifarma (www.especifarma.com.br).
 *
 * O HTML é renderizado no servidor — Jsoup estático funciona sem Selenium.
 *
 * URL de busca:
 *   https://www.especifarma.com.br/loja/busca.php?loja=1141379&palavra_busca=TERMO
 *
 * Estrutura do HTML:
 *   Container:      div.product  (dentro de li.item)
 *   Indisponível:   span.product-message presente → produto pulado
 *   Nome:           div.product-name  (fallback: atributo data-ga4-name no div.product)
 *   Preço:          span.current-price
 *   Link:           a.space-image[href]  (URL completa, sem necessidade de prefixo)
 *
 * Nota: o atributo data-ga4-price contém o valor numérico limpo (ex: "121.36"),
 * usado como fallback caso o span.current-price esteja ausente.
 */
public class EspecifarmaScrapper extends WebscrappingBase {

    private static final String LOJA_ID = "1141379";

    public EspecifarmaScrapper() {
        super("https://www.especifarma.com.br", "Especifarma");
    }

    @Override
    public List<ResultSearch> searchProduct(String search, int limit, BiConsumer<String, String> onProgress) {
        String encoded = URLEncoder.encode(search.trim(), StandardCharsets.UTF_8);
        String url = urlBase + "/loja/busca.php?loja=" + LOJA_ID + "&palavra_busca=" + encoded;

        try {
            Document doc = connectAndGetHtml(url);

            // Cada produto fica dentro de li.item > div.product
            Elements produtos = doc.select("li.item div.product");

            List<ResultSearch> results = new ArrayList<>();
            for (Element produto : produtos) {
                if (results.size() >= limit) break;

                // Pula indisponíveis — "Esse acabou :(" está num span.product-message
                if (produto.selectFirst("span.product-message") != null) continue;

                String nome  = extrairNome(produto);
                String preco = extrairPreco(produto);
                String link  = extrairLink(produto);

                if (!nome.isBlank()) {
                    results.add(new ResultSearch(nome, preco, link));
                    transformProductIntoMessage(nome,preco, onProgress);
                }
            }
            return results;

        } catch (IOException e) {
            return List.of();
        }
    }

    // -------------------------------------------------------------------------
    // Extratores
    // -------------------------------------------------------------------------

    private String extrairNome(Element produto) {
        // 1. Texto do div.product-name (mais limpo, sem marca no final)
        Element nomeEl = produto.selectFirst("div.product-name");
        if (nomeEl != null) {
            String v = nomeEl.text().trim();
            if (!v.isEmpty()) return v;
        }

        // 2. Atributo data-ga4-name no próprio div.product
        String ga4Name = produto.attr("data-ga4-name").trim();
        if (!ga4Name.isEmpty()) return ga4Name;

        return "";
    }

    private String extrairPreco(Element produto) {
        // 1. span.current-price — texto formatado "R$ 121,36"
        Element currentPrice = produto.selectFirst("span.current-price");
        if (currentPrice != null) {
            String v = currentPrice.text().trim();
            if (!v.isEmpty()) return v;
        }

        // 2. data-ga4-price — valor numérico limpo ex: "121.36"
        //    converte para formato BR: "R$ 121,36"
        String ga4Price = produto.attr("data-ga4-price").trim();
        if (!ga4Price.isEmpty()) {
            return "R$ " + ga4Price.replace(".", ",");
        }

        return "";
    }

    private String extrairLink(Element produto) {
        Element linkEl = produto.selectFirst("a.space-image");
        if (linkEl != null) {
            String href = linkEl.attr("href").trim();
            // A URL já vem completa (https://...) neste site
            if (!href.startsWith("http")) {
                href = urlBase + (href.startsWith("/") ? "" : "/") + href;
            }
            return href;
        }
        return "";
    }
}
