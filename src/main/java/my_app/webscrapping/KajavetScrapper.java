package my_app.webscrapping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class KajavetScrapper extends WebscrappingBase {

    public KajavetScrapper() {
        super("https://www.kajavet.com.br");
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

            Element produto = produtos.getFirst();
            String nome = produto.select("a.nome-produto").text();
            String preco = produto.select("strong.preco-promocional.titulo").text();
            String link = produto.select("a.nome-produto").attr("href");

            return new ResultSearch(nome, preco, link);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao buscar produtos: " + e.getMessage());
        }
    }
}