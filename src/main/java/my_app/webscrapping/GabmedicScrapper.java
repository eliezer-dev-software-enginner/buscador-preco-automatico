package my_app.webscrapping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GabmedicScrapper extends WebscrappingBase {

    public GabmedicScrapper() {
        super("https://www.gabmedic.com.br");
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
                String link  = produto.select("a.produto-sobrepor").attr("href");

                if (!link.startsWith("http")) link = urlBase + link;
                if (!nome.isBlank()) results.add(new ResultSearch(nome, preco, link));
            }
            return results;

        } catch (IOException e) {
            return List.of();
        }
    }

    private String extrairPreco(Element produto) {
        Element comSellPrice = produto.selectFirst("strong[data-sell-price]");
        if (comSellPrice != null) {
            String val = comSellPrice.attr("data-sell-price").trim();
            if (!val.isEmpty()) return "R$ " + val.replace(".", ",");
        }

        String[] seletores = {
                "strong.preco-promocional.cor-principal.titulo",
                "strong.preco-promocional.cor-principal",
                "strong.preco-promocional",
                "span.desconto-a-vista strong.cor-principal",
                "strong.preco-normal",
                ".preco strong",
        };
        for (String s : seletores) {
            String v = produto.select(s).text().trim();
            if (!v.isEmpty()) return v;
        }

        Element meta = produto.selectFirst("meta[itemprop=price]");
        if (meta != null) {
            String content = meta.attr("content").trim();
            if (!content.isEmpty()) return "R$ " + content.replace(".", ",");
        }
        return "";
    }
}