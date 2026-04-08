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

public class AlthisScrapperv2 extends WebscrappingBase {

    public AlthisScrapperv2() {
        super("https://www.althis.com.br","Althis");
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
            String query = search.trim().replace(" ", "+");
            //https://www.althis.com.br/busca?q=gel+20g
            String url = urlBase + "/busca?q=" + query;

            driver.get(url);

            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("div.container-list ")));

            Document doc = Jsoup.parse(driver.getPageSource());
            System.out.println(doc.html().contains("container-list"));
            Elements produtos = doc.select("div.container-list ul li");

            List<ResultSearch> results = new ArrayList<>();
            for (Element produto : produtos) {
                if (results.size() >= limit) break;

                if (produto.selectFirst("a.out_stock") != null) continue;

                Element a = produto.selectFirst("a");

                if (a == null) continue;

                // NOME
                String nome = produto.selectFirst(".product-name") != null
                        ? produto.selectFirst(".product-name").text().trim()
                        : "";

                // PREÇO (prioriza preço pix)
                String preco = "";
                Element pix = produto.selectFirst(".primary-price .pix");
                if (pix != null) {
                    preco = pix.text().replace("R$", "").trim();
                }

                // fallback (caso não tenha pix)
                if (preco.isEmpty()) {
                    Element normal = produto.selectFirst(".primary-price");
                    if (normal != null) {
                        preco = normal.text().replace("R$", "").trim();
                    }
                }

                // LINK
                String link = a.attr("href");
                if (!link.startsWith("http")) {
                    link = urlBase + link;
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
}