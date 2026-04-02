package my_app;

import io.github.bonigarcia.wdm.WebDriverManager;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PdfSaver {
    public void saveAsPdf(String url, String codigo, String titulo, Consumer<String> callback) {
        new Thread(() -> {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");

            WebDriver driver = new ChromeDriver(options);

            try {
                driver.get(url);

                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.jsReturnsValue(
                                "return document.readyState === 'complete'"));

                Map<String, Object> params = new HashMap<>();
                params.put("landscape", false);
                params.put("printBackground", true);
                params.put("paperWidth", 8.27);
                params.put("paperHeight", 11.69);

                var response = ((ChromeDriver) driver).executeCdpCommand("Page.printToPDF", params);
                String pdf = (String) response.get("data");

                // Monta nome do arquivo: "www_url_com_br.pdf"
                String dominio = new java.net.URI(url).getHost()
                        .replaceAll("[^a-zA-Z0-9\\-_]", "_");
                String nomeArquivo = dominio + ".pdf";

                Path outputDir = Path.of(System.getProperty("user.home"), "Documents","licita-facil-app","cotacoes", codigo);

                Files.createDirectories(outputDir); // cria a pasta se não existir
                Path outputPath = outputDir.resolve(nomeArquivo);

                Files.write(outputPath, Base64.getDecoder().decode(pdf));
                callback.accept(outputPath.toString());

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                driver.quit();
            }
        }).start();
    }
}
