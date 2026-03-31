package my_app.webscrapping.print;

import javafx.print.*;
import java.util.List;

public class PrintProfile {
    private String urlPattern;
    private PageOrientation orientation;
    private List<Integer> pagesToPrint; // ex: [1, 4]
    private boolean duplex; // frente e verso

    public PrintProfile(String urlPattern, PageOrientation orientation,
                        List<Integer> pagesToPrint, boolean duplex) {
        this.urlPattern = urlPattern;
        this.orientation = orientation;
        this.pagesToPrint = pagesToPrint;
        this.duplex = duplex;
    }

    public String getUrlPattern() { return urlPattern; }
    public PageOrientation getOrientation() { return orientation; }
    public List<Integer> getPagesToPrint() { return pagesToPrint; }
    public boolean isDuplex() { return duplex; }
}