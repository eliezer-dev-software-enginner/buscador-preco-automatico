package my_app.webscrapping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

//Copiei o "Element"
public class LoganMedScrapper extends WebscrappingBase {

    public LoganMedScrapper() {
        super("https://www.loganmed.com.br");
    }

    @Override
    public ResultSearch searchProduct(String search) {
        String query = search.trim().replace(" ", "+");
        String url = urlBase + "/buscar?q=" + query;

        try {
            Document doc = connectAndGetHtml(url);

            Elements produtos = doc.select("div.listagem-item");

            if (produtos.isEmpty()) {
                throw new RuntimeException("Nenhum produto encontrado para: " + search);
            }

           var produto = produtos.getFirst();
                String nome = produto.select("a.nome-produto").text();
                String preco = produto.select("strong.preco-promocional").text();
                String precoPix = produto.select("span.desconto-a-vista strong.cor-principal").text();
                String link = produto.select("a.nome-produto").attr("href");

            return new ResultSearch(nome, preco, link);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao buscar produtos: " + e.getMessage());
        }
    }
}