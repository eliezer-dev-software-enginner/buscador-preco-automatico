package my_app;

import my_app.webscrapping.*;

public class MainTeste {
    public static void main(String[] args) {
        System.out.println("=== LoganMed ===");
        var loganMed = new LoganMedScrapper();
        var r1 = loganMed.searchProduct("gel silicone 20g");
        System.out.println("Produto: " + r1.nomeProdutoEncontrado());
        System.out.println("Preço  : " + r1.preco());
        System.out.println("Link   : " + r1.link());

        System.out.println("\n=== Kajavet ===");
        var kajavet = new KajavetScrapper();
        var r2 = kajavet.searchProduct("Atadura 1,80cm x 20cm 9 fios 12 unidades");
        System.out.println("Produto: " + r2.nomeProdutoEncontrado());
        System.out.println("Preço  : " + r2.preco());
        System.out.println("Link   : " + r2.link());

        System.out.println("\n=== Gabmedic ===");
        var gabmedic = new GabmedicScrapper();
        var r3 = gabmedic.searchProduct("Atadura 1,80cm x 10cm 9 fios 12 unidades");
        System.out.println("Produto : " + r3.nomeProdutoEncontrado());
        System.out.println("Preço   : " + r3.preco());
        System.out.println("Link    : " + r3.link());

        System.out.println("\n=== SuturasOnline ===");
        var suturas = new SuturasOnlineScrapper();
        var r4 = suturas.searchProduct("Atadura 1,80cm x 10cm 18 fios pacote 12");
        System.out.println("Produto : " + r4.nomeProdutoEncontrado());
        System.out.println("Preço   : " + r4.preco());
        System.out.println("Link    : " + r4.link());

        System.out.println("\n=== Drogaria e Farma ===");
        var drogaria = new DrogariaEFarmaScrapper();
        var r5 = drogaria.searchProduct("Atadura 1,80cm x 20cm 12 unidades");
        System.out.println("Produto : " + r5.nomeProdutoEncontrado());
        System.out.println("Preço   : " + r5.preco());
        System.out.println("Link    : " + r5.link());
    }
}
