package my_app.webscrapping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class DrogariaEFarmaScrapper extends WebscrappingBase {

    public DrogariaEFarmaScrapper() {
        super("https://www.drogariaefarma.com.br");
    }

    @Override
    public ResultSearch searchProduct(String search) {
        String query = search.trim().replace(" ", "+");
        String url = urlBase + "/search?search_query=" + query;

        try {
            Document doc = connectAndGetHtml(url);

            Elements produtos = doc.select("li.ProductItem");

            if (produtos.isEmpty()) {
                throw new RuntimeException("Nenhum produto encontrado para: " + search);
            }

            Element produto = produtos.getFirst();

            String nome = produto.select("h2 a").text();

            // preço vem fragmentado em 3 spans: inteiro, separador decimal e decimal
            Element precoContainer = produto.selectFirst("div.prod_valor .ValorProduto");
            String preco = "";
            if (precoContainer != null) {
                String inteiro   = precoContainer.select(".price-integer").text();
                String separador = precoContainer.select(".price-decimal-str").text();
                String decimal   = precoContainer.select(".price-decimal").text();
                preco = "R$ " + inteiro + separador + decimal;
            }

            // link vem completo no hidden input
            String link = produto.select("input.ProductLink").attr("value");

            return new ResultSearch(nome, preco, link);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao buscar produtos: " + e.getMessage());
        }
    }
}