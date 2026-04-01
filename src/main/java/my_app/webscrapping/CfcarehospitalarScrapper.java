package my_app.webscrapping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CfcarehospitalarScrapper extends WebscrappingBase {

    public CfcarehospitalarScrapper() {
        super("https://www.cfcarehospitalar.com.br");
    }


    /*
        <div class="showcase__slider">
           <div class="showcase__slide">
              <div class="product">...</div>
           </div>

           <div class="showcase__slide">
              <div class="product">...</div>
           </div>

           <div class="showcase__slide">
              <div class="product">...</div>
           </div>
        </div>
     */
    @Override
    public List<ResultSearch> searchProduct(String search, int limit) {
        String query = search.trim().replace(" ", "+");
        String url = urlBase + "/loja/busca.php?loja=1183384&palavra_busca=" + query;

        try {
            Document doc = connectAndGetHtml(url);
            Elements produtos = doc.select("div.product");

            List<ResultSearch> results = new ArrayList<>();

            for (Element produto : produtos) {
                if (results.size() >= limit) break;
                String nome  = produto.select(".product__name a").text();
                String link  = produto.select(".product__link").attr("href");
                String preco = extrairPreco(produto);

                if (!link.isBlank() && !link.startsWith("http"))
                    link = urlBase + link;

                if (!nome.isBlank())
                    results.add(new ResultSearch(nome, preco, link));
            }

            return results;

        } catch (IOException e) {
            return List.of();
        }
    }

    /*
    Sempre que abrir um produto no navegador, procura:

application/ld+json

Se existir → acabou → scraping fica fácil.

Quase todos ecommerce têm isso:

Tray
Shopify
WooCommerce
Magento
Nuvemshop
     */
    private String extrairPreco(Element produto) {
        Element jsonScript = produto.selectFirst("script[type=application/ld+json]");
        if (jsonScript != null) {
            String json = jsonScript.html();

            // procurar price dentro do json
            int idx = json.indexOf("\"price\":");
            if (idx != -1) {
                int start = json.indexOf("\"", idx + 8) + 1;
                int end = json.indexOf("\"", start);
                String price = json.substring(start, end);
                return "R$ " + price.replace(".", ",");
            }
        }
        return "";
    }
}