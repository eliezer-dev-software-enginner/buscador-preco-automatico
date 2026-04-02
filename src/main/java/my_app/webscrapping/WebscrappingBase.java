package my_app.webscrapping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;

public abstract class WebscrappingBase {
    protected String urlBase;

    /**
     * Busca o termo e retorna até {@code limit} resultados.
     * Nunca lança exceção — retorna lista vazia em caso de falha.
     */
    public abstract List<ResultSearch> searchProduct(String search, int limit);

    public record ResultSearch(String nomeProdutoEncontrado, String preco, String link) {}

    public WebscrappingBase(String urlBase) {
        this.urlBase = urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

//    protected static Document connectAndGetHtml(String url) throws IOException {
//        return Jsoup.connect(url)
//                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
//                .timeout(10_000)
//                .get();
//    }

    protected Document connectAndGetHtml(String url) throws IOException {
        return org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36")
                .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .timeout(30000)
                .get();
    }

    public String getPriceFromUrlOfProduct(String url){
        return "0";
    }
}