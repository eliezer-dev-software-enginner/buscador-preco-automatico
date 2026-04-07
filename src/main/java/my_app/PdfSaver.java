package my_app;

import io.github.bonigarcia.wdm.WebDriverManager;
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
            options.addArguments("--lang=pt-BR");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36");

            WebDriver driver = new ChromeDriver(options);

            try {
                driver.get(url);

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));

                // 1. Aguarda DOM pronto
                wait.until(ExpectedConditions.jsReturnsValue(
                        "return document.readyState === 'complete'"));

                // 2. Remove rastro de webdriver
                ((JavascriptExecutor) driver).executeScript(
                        "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

                Thread.sleep(Duration.ofSeconds(45));

                // 3. Força lazy images a carregarem
                ((JavascriptExecutor) driver).executeScript("""
                    document.querySelectorAll('img[loading="lazy"], img[data-src]').forEach(img => {
                        img.loading = 'eager';
                        if (img.dataset.src) img.src = img.dataset.src;
                    });
                """);

                // 4. Scroll gradual para ativar lazy loading de seções
                scrollToBottom(driver);

                // 5. Aguarda rede ociosa (sem requisições por 500ms, timeout 15s)
                waitForNetworkIdle(driver, 900, 17_000);

                // 6. Pausa final para renderização visual terminar
                Thread.sleep(3500);

                // 7. Volta ao topo
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
                Thread.sleep(500);

                // 8. Gera o PDF
                Map<String, Object> params = new HashMap<>();
                params.put("landscape", false);
                params.put("printBackground", true);
                params.put("paperWidth", 10.27);
                params.put("paperHeight", 11.69);

                var response = ((ChromeDriver) driver).executeCdpCommand("Page.printToPDF", params);
                String pdf = (String) response.get("data");

                String dominio = new java.net.URI(url).getHost()
                        .replaceAll("[^a-zA-Z0-9\\-_]", "_");
                String nomeArquivo = dominio + ".pdf";

                Path outputDir = Path.of(System.getProperty("user.home"),
                        "Documents", "licita-facil-app", "cotacoes", codigo);
                Files.createDirectories(outputDir);
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

    /**
     * Scroll gradual até o fim da página para ativar lazy loading.
     * Relê a altura a cada passo pois o conteúdo pode crescer dinamicamente.
     */
    private void scrollToBottom(WebDriver driver) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long totalHeight = (long) js.executeScript("return document.body.scrollHeight");
        long scrollStep = 800;
        long currentPos = 0;

        while (currentPos < totalHeight) {
            currentPos = Math.min(currentPos + scrollStep, totalHeight);
            js.executeScript("window.scrollTo(0, " + currentPos + ")");
            Thread.sleep(200);
            totalHeight = (long) js.executeScript("return document.body.scrollHeight");
        }
    }

    /**
     * Monitora XMLHttpRequest e fetch para detectar quando a rede ficou ociosa.
     * Aguarda idleMs sem requisições ativas. Desiste após timeoutMs.
     */
    private void waitForNetworkIdle(WebDriver driver, int idleMs, int timeoutMs)
            throws InterruptedException {

        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript("""
            window.__activeRequests = 0;
            const origOpen = XMLHttpRequest.prototype.open;
            XMLHttpRequest.prototype.open = function() {
                window.__activeRequests++;
                this.addEventListener('loadend', () => window.__activeRequests--);
                origOpen.apply(this, arguments);
            };
            const origFetch = window.fetch;
            window.fetch = function() {
                window.__activeRequests++;
                return origFetch.apply(this, arguments)
                    .finally(() => window.__activeRequests--);
            };
        """);

        long deadline = System.currentTimeMillis() + timeoutMs;
        long idleStart = -1;

        while (System.currentTimeMillis() < deadline) {
            long active = (long) js.executeScript("return window.__activeRequests || 0");
            if (active == 0) {
                if (idleStart < 0) idleStart = System.currentTimeMillis();
                if (System.currentTimeMillis() - idleStart >= idleMs) break;
            } else {
                idleStart = -1;
            }
            Thread.sleep(100);
        }
    }

    static void main() {
        new PdfSaver().saveAsPdf(
                "https://www.cfcarehospitalar.com.br/curativos/alginato-de-calcio/curativo-de-fibras-de-alginato-de-calcio-kangli-sorb-10x10cm-vitamedical-c10-unidades",
                "TESTE", "TESTE_TITULO", System.out::println);
    }
}