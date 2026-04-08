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

public class SuturasOnlineScrapper extends WebscrappingBase {

    public SuturasOnlineScrapper() {
        super("https://loja.suturasonline.com.br", "Suturas online");
    }

    @Override
    public List<ResultSearch> searchProduct(String search, int limit, BiConsumer<String, String> onProgress) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);

        try {
            String encodedSearch = search.trim().replace(" ", "%20").replace(",", "%2C");
            String url = urlBase + "/#&search-term=" + encodedSearch;

            driver.get(url);

            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("div.product")));

            Document doc = Jsoup.parse(driver.getPageSource());
            Elements produtos = doc.select("div.product");

            List<ResultSearch> results = new ArrayList<>();
            for (Element produto : produtos) {
                if (results.size() >= limit) break;

                String nome = produto.selectFirst("div.product-name") != null
                        ? produto.selectFirst("div.product-name").text() : "";
                String preco = produto.selectFirst("span.price-card") != null
                        ? produto.selectFirst("span.price-card").text() : "";
                String link = produto.selectFirst("a.space-image") != null
                        ? "https:" + produto.selectFirst("a.space-image").attr("href") : "";

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
}