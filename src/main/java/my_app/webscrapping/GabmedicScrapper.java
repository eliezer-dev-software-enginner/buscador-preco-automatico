package my_app.webscrapping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class GabmedicScrapper extends WebscrappingBase {

    public GabmedicScrapper() {
        super("https://www.gabmedic.com.br");
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
            String preco = produto.select("strong.preco-promocional.cor-principal.titulo").text();
            String link = produto.select("a.produto-sobrepor").attr("href");

            // link pode vir relativo, completa com a base se necessário
            if (!link.startsWith("http")) {
                link = urlBase + link;
            }

            return new ResultSearch(nome, preco, link);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao buscar produtos: " + e.getMessage());
        }
    }
}