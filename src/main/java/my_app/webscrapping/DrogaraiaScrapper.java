package my_app.webscrapping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DrogaraiaScrapper extends WebscrappingBase {

    public DrogaraiaScrapper() {
        super("https://www.drogaraia.com.br");
    }

    @Override
    public List<ResultSearch> searchProduct(String search, int limit) {
        String encoded = URLEncoder.encode(search.trim(), StandardCharsets.UTF_8);
        String url = urlBase + "/search?w=" + encoded;

        try {
            Document doc = connectAndGetHtml(url);

            Elements produtos = doc.select("article[data-card=product]");

            List<ResultSearch> results = new ArrayList<>();

            for (Element produto : produtos) {
                if (results.size() >= limit) break;

                String nome  = extrairNome(produto);
                String preco = extrairPreco(produto);
                String link  = extrairLink(produto);

                if (!nome.isBlank()) {
                    results.add(new ResultSearch(nome, preco, link));
                }
            }

            return results;

        } catch (IOException e) {
            return List.of();
        }
    }

    // ---------------------------------------------------------
    // Extratores
    // ---------------------------------------------------------

    private String extrairNome(Element produto) {
        Element nomeEl = produto.selectFirst("h2 a");
        if (nomeEl != null) {
            return nomeEl.text().trim();
        }
        return "";
    }

    private String extrairPreco(Element produto) {
        // Preço principal (promo ou normal)
        Element precoEl = produto.selectFirst("[data-testid=price]");
        if (precoEl != null) {
            return precoEl.text().trim();
        }

        // fallback preço normal
        Element precoNormal = produto.selectFirst("div:contains(R$)");
        if (precoNormal != null) {
            return precoNormal.text().trim();
        }

        return "";
    }

    private String extrairLink(Element produto) {
        Element linkEl = produto.selectFirst("h2 a");
        if (linkEl != null) {
            String href = linkEl.attr("href").trim();
            if (!href.isEmpty()) {
                return href.startsWith("http") ? href : urlBase + href;
            }
        }
        return "";
    }
}