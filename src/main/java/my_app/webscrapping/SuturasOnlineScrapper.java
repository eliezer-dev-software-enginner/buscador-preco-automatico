package my_app.webscrapping;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SuturasOnlineScrapper extends WebscrappingBase {

    public SuturasOnlineScrapper() {
        super("https://loja.suturasonline.com.br");
    }

    @Override
    public ResultSearch searchProduct(String search) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");        // roda sem abrir janela
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);

        try {
            String encodedSearch = search.trim().replace(" ", "%20").replace(",", "%2C");
            String url = urlBase + "/#&search-term=" + encodedSearch;

            driver.get(url);

            // aguarda os produtos aparecerem na página
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("div.product")));

            // pega o HTML renderizado e passa pro Jsoup
            Document doc = Jsoup.parse(driver.getPageSource());

            Element produto = doc.selectFirst("div.product");
            if (produto == null) {
                throw new RuntimeException("Nenhum produto encontrado para: " + search);
            }

            String nome = produto.selectFirst("div.product-name") != null
                    ? produto.selectFirst("div.product-name").text()
                    : "";

            String preco = produto.selectFirst("span.price-card") != null
                    ? produto.selectFirst("span.price-card").text()
                    : "";

            String link = produto.selectFirst("a.space-image") != null
                    ? "https:" + produto.selectFirst("a.space-image").attr("href")
                    : "";

            return new ResultSearch(nome, preco, link);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar produtos: " + e.getMessage());
        } finally {
            driver.quit(); // sempre fecha o browser
        }
    }
}