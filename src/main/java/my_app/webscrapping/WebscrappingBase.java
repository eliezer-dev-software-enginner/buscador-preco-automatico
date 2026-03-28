package my_app.webscrapping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public abstract class WebscrappingBase {
    protected String urlBase;
    public abstract ResultSearch searchProduct(String search);

    public record ResultSearch(String nomeProdutoEncontrado, String preco, String link){}

    public WebscrappingBase(String s){
        setUrlBase(s);
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    protected static Document connectAndGetHtml(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10_000)
                .get();
        return doc;
    }
}
