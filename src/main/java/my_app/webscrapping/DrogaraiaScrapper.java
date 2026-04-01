package my_app.webscrapping;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Scraper da Drogaria Raia (www.drogaraia.com.br).
 *
 * O site é Next.js/React — o HTML é renderizado no servidor (SSR), mas os
 * preços são injetados via hidratação do React APÓS o carregamento inicial.
 * Isso explica por que Jsoup retornava nome e link corretos, mas preço vazio:
 * o Jsoup pega o HTML do primeiro response HTTP, antes da hidratação.
 * Por isso usamos Selenium para aguardar o preço aparecer na DOM.
 *
 * URL de busca:
 *   https://www.drogaraia.com.br/search?w=gel+silicone+20g
 *
 * Estrutura HTML relevante:
 *   Container:  article[data-card="product"]
 *   Nome:       h2 a  (texto do link)
 *   Preço:      [data-testid="price"]  — aparece EM PARES por produto:
 *                 [0] = preço "de" (riscado, maior)
 *                 [1] = preço "por" (atual, menor) ← o que queremos
 *               Quando não há desconto, só existe [0] com o preço único.
 *               Estrutura interna: <span>R$</span> 194,99
 *               O valor numérico fica como textNode direto do div.
 *               .text() do Jsoup retorna "R$ 194,99" (concatena span + textNode).
 *   Link:       h2 a[href] → relativo, prefixar com base
 */
public class DrogaraiaScrapper extends WebscrappingBase {

    public DrogaraiaScrapper() {
        super("https://www.drogaraia.com.br");
    }

    @Override
    public List<ResultSearch> searchProduct(String search, int limit) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        // User-agent de desktop para evitar versão mobile
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        WebDriver driver = new ChromeDriver(options);

        try {
            String encoded = URLEncoder.encode(search.trim(), StandardCharsets.UTF_8);
            String url = urlBase + "/search?w=" + encoded;

            driver.get(url);

            // Aguarda pelo menos um article de produto aparecer com preço hidratado
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("article[data-card='product']")));

            // Pausa extra para React hidratar os preços
            Thread.sleep(1500);

            Document doc = Jsoup.parse(driver.getPageSource());
            Elements produtos = doc.select("article[data-card=product]");

            List<ResultSearch> results = new ArrayList<>();
            for (Element produto : produtos) {
                if (results.size() >= limit) break;

                String nome  = extrairNome(produto);
                String preco = extrairPreco(produto);
                String link  = extrairLink(produto);

                if (!nome.isBlank()) results.add(new ResultSearch(nome, preco, link));
            }
            return results;

        } catch (Exception e) {
            return List.of();
        } finally {
            driver.quit();
        }
    }

    // -------------------------------------------------------------------------
    // Extratores
    // -------------------------------------------------------------------------

    private String extrairNome(Element produto) {
        Element nomeEl = produto.selectFirst("h2 a");
        return nomeEl != null ? nomeEl.text().trim() : "";
    }

    private String extrairPreco(Element produto) {
        // [data-testid="price"] aparece em pares quando há promoção:
        //   índice 0 = preço "de" (riscado)
        //   índice 1 = preço "por" (atual) ← queremos este
        // Quando não há promoção, só existe índice 0 com o preço único.
        //
        // Estrutura interna: <div data-testid="price"><span>R$</span> 194,99</div>
        // .text() retorna "R$ 194,99" (Jsoup concatena span e textNode).
        Elements precos = produto.select("[data-testid=price]");

        if (precos.size() >= 2) {
            // Tem desconto: pega o segundo (preço atual/menor)
            return normalizar(precos.get(1).text());
        } else if (precos.size() == 1) {
            // Preço único sem desconto
            return normalizar(precos.get(0).text());
        }

        return "";
    }

    /**
     * Normaliza o texto do preço.
     * Jsoup às vezes retorna "R$ 194,99" com espaço irregular entre "R$" e o valor.
     * Garante formato "R$ 194,99".
     */
    private String normalizar(String texto) {
        if (texto == null || texto.isBlank()) return "";
        // Remove espaços extras e garante "R$ X,XX"
        return texto.replaceAll("\\s+", " ").trim();
    }

    private String extrairLink(Element produto) {
        Element linkEl = produto.selectFirst("h2 a");
        if (linkEl != null) {
            String href = linkEl.attr("href").trim();
            if (!href.isEmpty()) {
                return href.startsWith("http") ? href : urlBase + href;
            }
        }
        return "";
    }
}