package my_app.webscrapping.print;

import io.github.bonigarcia.wdm.WebDriverManager;
import javafx.print.PageOrientation;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

public class SmartPrintService {

    private final PrintProfileRegistry registry = new PrintProfileRegistry();

    public void printUrl(String url, javafx.stage.Stage ownerStage) {
        // Roda em thread separada para não travar a UI do JavaFX
        new Thread(() -> {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            // SEM --headless: Chrome abre visível
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            // Mantém o perfil do usuário → Chrome lembra duplex, cópias, etc
            options.addArguments("--user-data-dir=" + getProfileDir());

            WebDriver driver = new ChromeDriver(options);

            try {
                driver.get(url);

                // Aguarda a página carregar
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.jsReturnsValue(
                                "return document.readyState === 'complete'"));

                // Resolve o perfil de impressão para a URL
                PrintProfile profile = registry.resolve(url);
                String orientation = profile.getOrientation() == PageOrientation.LANDSCAPE
                        ? "landscape" : "portrait";
                String paper = PrintDialogPreferences.loadPaper();

                // Injeta CSS de orientação
                String cssJs = """
                    (function() {
                        var old = document.getElementById('__smart_print_css__');
                        if (old) old.remove();
                        var style = document.createElement('style');
                        style.id = '__smart_print_css__';
                        style.textContent = `
                            @media print {
                                @page { size: %s %s; }
                            }
                        `;
                        document.head.appendChild(style);
                    })();
                    """.formatted(paper, orientation);

                ((JavascriptExecutor) driver).executeScript(cssJs);

                // Pequeno delay para o CSS ser aplicado
                Thread.sleep(500);

                // Abre o diálogo de impressão nativo do Chrome
                // Opção 2: JS sem aguardar retorno
                ((JavascriptExecutor) driver).executeAsyncScript(
                        "var callback = arguments[arguments.length - 1];" +
                                "setTimeout(function() { window.print(); callback(); }, 100);"
                );

                // Mantém o Chrome aberto — o usuário fecha quando quiser
                // (não chama driver.quit())

            } catch (Exception e) {
                e.printStackTrace();
                driver.quit();
            }
        }).start();
    }

    private String getProfileDir() {
        // Usa um perfil dedicado para a aplicação
        // → Chrome salva as preferências de impressão aqui entre sessões
        String home = System.getProperty("user.home");
        return home + "/.buscador-precos-chrome-profile";
    }
}