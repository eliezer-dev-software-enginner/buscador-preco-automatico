package my_app.webscrapping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Scraper da Magazine Médica (magazinemedica.com.br).
 *
 * HTML renderizado no servidor — Jsoup estático funciona sem Selenium.
 *
 * URL de busca:
 *   https://magazinemedica.com.br/busca/?keywords=seringa+descart%C3%A1vel
 *
 * Estrutura do HTML:
 *   Container:    div.col-xs-12.product  (contém div.wrath-content-box)
 *   Indisponível: button[data-stock="False"]  OU  span com texto "Indisponível"
 *   Nome:         p.text-primary.text-center.text-bold[title]
 *                 → o atributo title vem com o nome COMPLETO, sem truncamento
 *   Preço:        span.text-bold.text-primary  (primeiro dentro do bloco de preço)
 *                 → é o preço "Por R$ X,XX à vista / no pix"
 *   Link:         a[id="product_small_first"][href]
 *                 → vem relativo ("/produtos/..."), precisa de prefixo da base
 */
public class MagazineMedicaScrapper extends WebscrappingBase {

    public MagazineMedicaScrapper() {
        super("https://magazinemedica.com.br");
    }

    @Override
    public List<ResultSearch> searchProduct(String search, int limit) {
        String encoded = URLEncoder.encode(search.trim(), StandardCharsets.UTF_8);
        String url = urlBase + "/busca/?keywords=" + encoded;

        try {
            Document doc = connectAndGetHtml(url);

            // Cada produto está em: div.col-xs-12.product
            Elements produtos = doc.select("div.col-xs-12.product");

            List<ResultSearch> results = new ArrayList<>();
            for (Element produto : produtos) {
                if (results.size() >= limit) break;

                // Indisponível: botão de compra tem data-stock="False"
                Element botaoComprar = produto.selectFirst("button.ajax-add-to-cart");
                if (botaoComprar != null && "False".equals(botaoComprar.attr("data-stock"))) continue;

                // Segunda verificação: texto "Indisponível" no bloco de preço
                Element blocoPreco = produto.selectFirst("div.col-xs-12.product-details");
                if (blocoPreco != null && blocoPreco.text().contains("Indisponível")) continue;

                String nome  = extrairNome(produto);
                String preco = extrairPreco(produto);
                String link  = extrairLink(produto);

                if (!nome.isBlank()) results.add(new ResultSearch(nome, preco, link));
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
        // O atributo title do <p> vem com o nome completo (sem "..." de truncamento)
        Element nomeEl = produto.selectFirst("p.text-primary.text-center.text-bold");
        if (nomeEl != null) {
            String titulo = nomeEl.attr("title").trim();
            if (!titulo.isEmpty()) return titulo;

            // Fallback: texto do elemento (pode vir truncado com "…")
            String texto = nomeEl.text().trim();
            if (!texto.isEmpty()) return texto;
        }
        return "";
    }

    private String extrairPreco(Element produto) {
        // O bloco de preço tem a estrutura:
        //   Por <span class="text-bold text-primary" style="font-size: 1.2em;">R$ 2,20</span>
        //
        // Pega o primeiro span.text-bold.text-primary dentro do produto
        // (o segundo, se existir, é o preço no cartão — não queremos esse)
        Element precoEl = produto.selectFirst("span.text-bold.text-primary");
        if (precoEl != null) {
            String v = precoEl.text().trim();
            if (!v.isEmpty()) return v;
        }
        return "";
    }

    private String extrairLink(Element produto) {
        // <a id="product_small_first" href="/produtos/...">
        Element linkEl = produto.selectFirst("a#product_small_first");
        if (linkEl != null) {
            String href = linkEl.attr("href").trim();
            if (!href.isEmpty()) {
                // Link vem relativo: "/produtos/seringa-..." → completa com a base
                return href.startsWith("http") ? href : urlBase + href;
            }
        }
        return "";
    }
}
