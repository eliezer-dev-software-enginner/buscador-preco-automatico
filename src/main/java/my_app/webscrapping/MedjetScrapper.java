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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Scraper do Medjet (www.medjet.com.br).
 *
 * O site usa SmartHint como motor de busca — o resultado é renderizado via JS,
 * então Jsoup estático retornaria HTML vazio. Usamos Selenium headless.
 *
 * URL de busca:
 *   https://www.medjet.com.br/#&search-term=avental+descartavel
 *
 * Estrutura do HTML renderizado:
 *   Container:    div.item.apoio-sh
 *   Nome (limpo): span.title  (display:none, mas contém o nome sem ruído)
 *   Preço atual:  span.current-price.smarthint-value
 *   Link:         a.space-image[href]  → vem como "//www.medjet.com.br/..."
 *                                         deve ser prefixado com "https:"
 *
 * Observação: span.current-price é o preço com desconto (sale price).
 * Se quiser o preço PIX, use: span.preco-avista span.smarthint-value
 */
public class MedjetScrapper extends WebscrappingBase {

    public MedjetScrapper() {
        super("https://www.medjet.com.br","Medjet");
    }

    @Override
    public List<ResultSearch> searchProduct(String search, int limit, BiConsumer<String, String> onProgress) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");

        WebDriver driver = new ChromeDriver(options);

        try {
            // SmartHint usa hash na URL — o termo deve estar codificado
            String encoded = search.trim()
                    .replace(" ", "%20")
                    .replace(",", "%2C");
            String url = urlBase + "/#&search-term=" + encoded;

            driver.get(url);

            // Aguarda o container do SmartHint com os produtos aparecer
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("div.item.apoio-sh")));

            // Pequena pausa para garantir que todos os itens carregaram
            Thread.sleep(800);

            Document doc = Jsoup.parse(driver.getPageSource());
            Elements itens = doc.select("div.item.apoio-sh");

            List<ResultSearch> results = new ArrayList<>();
            for (Element item : itens) {
                if (results.size() >= limit) break;

                // span.title está hidden mas contém o nome limpo sem HTML extra
                String nome = item.selectFirst("span.title") != null
                        ? item.selectFirst("span.title").text().trim()
                        : item.select("h3.product-name").text().trim();

                String preco = extrairPreco(item);

                // Link vem como "//www.medjet.com.br/caminho" — adiciona "https:"
                String link = "";
                Element linkEl = item.selectFirst("a.space-image");
                if (linkEl != null) {
                    String href = linkEl.attr("href");
                    link = href.startsWith("//") ? "https:" + href : href;
                }

                if (!nome.isBlank()) {
                    results.add(new ResultSearch(nome, preco, link));
                    transformProductIntoMessage(nome,preco, onProgress);
                }
            }
            return results;

        } catch (Exception e) {
            return List.of();
        } finally {
            driver.quit();
        }
    }

    /**
     * Hierarquia de preço:
     *   1. span.current-price  — preço com desconto (sale price), mais visível na UI
     *   2. span.preco-avista   — preço PIX (menor, mas menos comum)
     *   3. old-price            — preço original sem desconto (último recurso)
     */
    private String extrairPreco(Element item) {
        // 1. Preço atual (com desconto)
        Element currentPrice = item.selectFirst("span.current-price.smarthint-value");
        if (currentPrice != null) {
            String v = currentPrice.text().trim();
            if (!v.isEmpty()) return v;
        }

        // 2. Preço PIX (à vista) — o valor fica em span.smarthint-value dentro de span.preco-avista
        Element pixSpan = item.selectFirst("span.preco-avista span.smarthint-value");
        if (pixSpan != null) {
            String v = "R$ " + pixSpan.text().trim();
            if (!v.equals("R$ ")) return v;
        }

        // 3. Preço original (sem desconto) — só se não houver nenhum outro
        Element oldPrice = item.selectFirst("span.old-price.smarthint-value");
        if (oldPrice != null) {
            String v = oldPrice.text().trim();
            if (!v.isEmpty()) return v;
        }

        return "";
    }

    @Override
    public String getPriceFromUrlOfProduct(String url) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get(url);

            // espera o preço carregar
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("#preco_atual")));

            Document doc = Jsoup.parse(driver.getPageSource());

            // melhor fonte do preço
            Element precoInput = doc.selectFirst("#preco_atual");
            if (precoInput != null) {
                String preco = precoInput.attr("value");
                return "R$ " + preco.replace(".", ",");
            }

            // fallback se não existir input
            Element precoSpan = doc.selectFirst("#variacaoPreco");
            if (precoSpan != null) {
                return "R$ " + precoSpan.text().trim();
            }

            return "";

        } catch (Exception e) {
            return "";
        } finally {
            driver.quit();
        }
    }
}
