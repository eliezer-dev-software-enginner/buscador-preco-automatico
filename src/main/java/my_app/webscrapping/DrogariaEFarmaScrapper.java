package my_app.webscrapping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DrogariaEFarmaScrapper extends WebscrappingBase {

    public DrogariaEFarmaScrapper() {
        super("https://www.drogariaefarma.com.br");
    }

    @Override
    public List<ResultSearch> searchProduct(String search, int limit) {
        String query = search.trim().replace(" ", "+");
        String url = urlBase + "/search?search_query=" + query;

        try {
            Document doc = connectAndGetHtml(url);
            Elements produtos = doc.select("li.ProductItem");

            List<ResultSearch> results = new ArrayList<>();
            for (Element produto : produtos) {
                if (results.size() >= limit) break;

                String nome = produto.select("h2 a").text();

                Element precoContainer = produto.selectFirst("div.prod_valor .ValorProduto");
                String preco = "";
                if (precoContainer != null) {
                    String inteiro   = precoContainer.select(".price-integer").text();
                    String separador = precoContainer.select(".price-decimal-str").text();
                    String decimal   = precoContainer.select(".price-decimal").text();
                    preco = "R$ " + inteiro + separador + decimal;
                }

                String link = produto.select("input.ProductLink").attr("value");

                if (!nome.isBlank()) results.add(new ResultSearch(nome, preco, link));
            }
            return results;

        } catch (IOException e) {
            return List.of();
        }
    }
}