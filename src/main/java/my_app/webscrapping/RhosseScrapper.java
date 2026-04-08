package my_app.webscrapping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Scraper da Rhosse (www.rhosse.com.br).
 *
 * HTML renderizado no servidor — Jsoup estático funciona sem Selenium.
 *
 * URL de busca:
 *   https://www.rhosse.com.br/busca?q=canula+orofaringea
 *
 * Estrutura do HTML:
 *   Container:  div.item-product
 *   Nome:       img[title]  ← atributo title tem o nome completo sem truncamento
 *               fallback: h2.title a (texto do link do título)
 *   Preço PIX:  p.promotion-price.pix-price > strong  → "R$ 7,60"
 *   Preço card: input[name="price"][value]             → "8.0" (fallback numérico limpo)
 *   Link:       a.item-image[href]                    → relativo "/slug/p", prefixar base
 *
 * Observação sobre estoque: a página de listagem da Rhosse não exibe sinal de
 * indisponibilidade — todos os itens retornados têm botão "Comprar" ativo.
 * Se um produto estiver sem estoque, ele simplesmente não aparece na busca.
 * Portanto, não há necessidade de filtro de indisponível aqui.
 */
public class RhosseScrapper extends WebscrappingBase {

    public RhosseScrapper() {
        super("https://www.rhosse.com.br", "Rhosse");
    }

    @Override
    public List<ResultSearch> searchProduct(String search, int limit, BiConsumer<String, String> onProgress) {
        String encoded = URLEncoder.encode(search.trim(), StandardCharsets.UTF_8);
        String url = urlBase + "/busca?q=" + encoded;

        try {
            Document doc = connectAndGetHtml(url);
            Elements produtos = doc.select("div.item-product");

            List<ResultSearch> results = new ArrayList<>();
            for (Element produto : produtos) {
                if (results.size() >= limit) break;

                String nome  = extrairNome(produto);
                String preco = extrairPreco(produto);
                String link  = extrairLink(produto);

                if (!nome.isBlank()) {
                    results.add(new ResultSearch(nome, preco, link));
                    transformProductIntoMessage(nome,preco, onProgress);
                }
            }
            return results;

        } catch (IOException e) {
            return List.of();
        }
    }

    // -------------------------------------------------------------------------
    // Extratores
    // -------------------------------------------------------------------------

    private String extrairNome(Element produto) {
        // 1. Atributo title da imagem — sempre completo, sem "..."
        Element img = produto.selectFirst("img.img-lazy");
        if (img != null) {
            String titulo = img.attr("title").trim();
            if (!titulo.isEmpty()) return titulo;
        }

        // 2. Texto do link h2.title a
        Element tituloLink = produto.selectFirst("h2.title a");
        if (tituloLink != null) {
            String texto = tituloLink.text().trim();
            if (!texto.isEmpty()) return texto;
        }

        return "";
    }

    private String extrairPreco(Element produto) {
        // 1. Preço PIX/boleto: p.promotion-price.pix-price > strong
        //    Ex: "R$ 7,60"
        Element pixPreco = produto.selectFirst("p.promotion-price.pix-price strong");
        if (pixPreco != null) {
            String v = pixPreco.text().trim();
            if (!v.isEmpty()) return v;
        }

        // 2. Preço PIX no selo (span.seal-pix): pode aparecer antes do bloco principal
        Element sealPix = produto.selectFirst("p.seal-pix strong");
        if (sealPix != null) {
            String v = sealPix.text().trim();
            if (!v.isEmpty()) return v;
        }

        // 3. input[name="price"] — valor numérico "8.0" ou "290.0"
        //    Converte para formato BR: "R$ 8,00"
        Element inputPreco = produto.selectFirst("input[name=price]");
        if (inputPreco != null) {
            String rawValue = inputPreco.attr("value").trim();
            if (!rawValue.isEmpty()) {
                try {
                    double valor = Double.parseDouble(rawValue);
                    return String.format("R$ %.2f", valor).replace(".", ",");
                } catch (NumberFormatException ignored) {}
            }
        }

        return "";
    }

    private String extrairLink(Element produto) {
        // a.item-image href="/canula-orofaringea-de-guedel-n5-1100mm/p"
        Element linkEl = produto.selectFirst("a.item-image");
        if (linkEl != null) {
            String href = linkEl.attr("href").trim();
            if (!href.isEmpty()) {
                return href.startsWith("http") ? href : urlBase + href;
            }
        }
        return "";
    }
}
