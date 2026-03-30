package my_app.webscrapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public class PncpPesquisaPreco {

    private static final String BASE_CONSULTA = "https://pncp.gov.br/api/consulta/v1";
    private static final String BASE_PNCP     = "https://pncp.gov.br/api/pncp/v1";

    public enum Modalidade {
        LEILAO(1), DIALOGO_COMPETITIVO(2), CONCURSO(3), CONCORRENCIA(4),
        CONCORRENCIA_INTERNACIONAL(5), PREGAO_ELETRONICO(6), PREGAO_PRESENCIAL(7),
        DISPENSA(8), INEXIGIBILIDADE(9);

        public final int codigo;
        Modalidade(int codigo) { this.codigo = codigo; }
    }

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public PncpPesquisaPreco() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(45))
                .build();
        this.mapper = new ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // Modelo
    // -------------------------------------------------------------------------
    public static class ItemEncontrado {
        public String municipio, cnpjOrgao, orgaoNome, ano, sequencial, numeroItem, descricao, unidade;
        public int    modalidade;
        public Double quantidade, valorUnitario, valorTotal;

        @Override
        public String toString() {
            return String.format("%-25s | %-55s | R$ %10.2f | %s",
                    municipio     != null ? municipio : "-",
                    descricao     != null ? (descricao.length() > 55
                            ? descricao.substring(0, 52) + "..." : descricao) : "-",
                    valorUnitario != null ? valorUnitario : 0.0,
                    unidade       != null ? unidade : "-");
        }

        public String toCsvLine() {
            String desc = descricao != null ? descricao.replace("\"", "'") : "";
            return String.format("%s,%s,\"%s\",%d,%s,%s,%s,\"%s\",%s,%s,%s,%s",
                    municipio, cnpjOrgao, orgaoNome, modalidade,
                    ano, sequencial, numeroItem, desc,
                    unidade, quantidade, valorUnitario, valorTotal);
        }
    }

    // -------------------------------------------------------------------------
    // Etapa 1
    // -------------------------------------------------------------------------
    public List<JsonNode> buscarContratacoesMG(
            String dataInicial, String dataFinal,
            Modalidade modalidade, int totalPaginas) {

        List<JsonNode> contratacoes = new ArrayList<>();

        for (int pagina = 1; pagina <= totalPaginas; pagina++) {
            String url = BASE_CONSULTA + "/contratacoes/publicacao"
                    + "?dataInicial=" + dataInicial
                    + "&dataFinal="   + dataFinal
                    + "&codigoModalidadeContratacao=" + modalidade.codigo
                    + "&uf=MG"
                    + "&pagina="      + pagina
                    + "&tamanhoPagina=50";

            try {
                String json = get(url);
                JsonNode root = mapper.readTree(json);
                JsonNode data = root.get("data");

                if (data == null || !data.isArray() || data.isEmpty()) break;

                for (JsonNode node : data) contratacoes.add(node);

                System.out.printf("   [%s] Pag. %d: %d contratacoes%n",
                        modalidade.name(), pagina, data.size());

                Thread.sleep(800); // era 400ms

            } catch (Exception e) {
                System.err.printf("   Erro [%s] pag. %d: %s%n",
                        modalidade.name(), pagina, e.getMessage());
                break;
            }
        }

        return contratacoes;
    }

    // -------------------------------------------------------------------------
    // Etapa 2
    // -------------------------------------------------------------------------
    public List<JsonNode> buscarItensContratacao(String cnpj, String ano, String sequencial) {
        String cnpjLimpo = cnpj.replaceAll("[.\\-/]", "");
        String url = BASE_PNCP + "/orgaos/" + cnpjLimpo
                + "/compras/" + ano + "/" + sequencial + "/itens";
        try {
            String json = get(url);
            JsonNode root = mapper.readTree(json);
            List<JsonNode> itens = new ArrayList<>();
            if (root.isArray()) root.forEach(itens::add);
            else if (root.has("data") && root.get("data").isArray())
                root.get("data").forEach(itens::add);
            return itens;
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<JsonNode> buscarResultadoItem(
            String cnpj, String ano, String sequencial, String numeroItem) {
        String cnpjLimpo = cnpj.replaceAll("[.\\-/]", "");
        String url = BASE_PNCP + "/orgaos/" + cnpjLimpo
                + "/compras/" + ano + "/" + sequencial
                + "/itens/" + numeroItem + "/resultados";
        try {
            String json = get(url);
            JsonNode root = mapper.readTree(json);
            List<JsonNode> resultados = new ArrayList<>();
            if (root.isArray()) root.forEach(resultados::add);
            else if (root.has("data") && root.get("data").isArray())
                root.get("data").forEach(resultados::add);
            return resultados;
        } catch (Exception e) {
            return List.of();
        }
    }

    // -------------------------------------------------------------------------
    // pesquisarPrecoItem — overload com maxPaginas para o CotacaoService controlar
    // -------------------------------------------------------------------------

    /**
     * Versão usada pelo CotacaoService — recebe maxPaginas explicitamente.
     * Use maxPaginas=2 para buscas rápidas na UI; maxPaginas=5+ para pesquisas completas.
     */
    public List<ItemEncontrado> pesquisarPrecoItem(
            String descricaoBusca,
            String dataInicial,
            String dataFinal,
            int maxPaginas,
            Modalidade... modalidades) {

        List<ItemEncontrado> resultados = new ArrayList<>();

        for (Modalidade modalidade : modalidades) {
            List<JsonNode> contratacoes =
                    buscarContratacoesMG(dataInicial, dataFinal, modalidade, maxPaginas);

            for (JsonNode c : contratacoes) {
                String cnpj       = texto(c, "orgaoEntidade", "cnpj");
                String orgaoNome  = texto(c, "orgaoEntidade", "razaoSocial");
                String ano        = textoPlano(c, "anoCompra");
                String sequencial = textoPlano(c, "sequencialCompra");
                String municipio  = texto(c, "unidadeOrgao", "municipioNome");

                if (cnpj == null || ano == null || sequencial == null) continue;

                List<JsonNode> itens = buscarItensContratacao(cnpj, ano, sequencial);

                for (JsonNode item : itens) {
                    String descricao = textoPlano(item, "descricao");
                    if (descricao == null) continue;

                    if (descricao.toLowerCase().contains(descricaoBusca.toLowerCase())) {
                        ItemEncontrado ie = new ItemEncontrado();
                        ie.municipio     = municipio;
                        ie.cnpjOrgao     = cnpj;
                        ie.orgaoNome     = orgaoNome;
                        ie.modalidade    = modalidade.codigo;
                        ie.ano           = ano;
                        ie.sequencial    = sequencial;
                        ie.numeroItem    = textoPlano(item, "numeroItem");
                        ie.descricao     = descricao;
                        ie.unidade       = textoPlano(item, "unidadeMedida");
                        ie.quantidade    = doubleNode(item, "quantidade");
                        ie.valorUnitario = doubleNode(item, "valorUnitarioEstimado");
                        ie.valorTotal    = doubleNode(item, "valorTotal");
                        resultados.add(ie);
                    }
                }

                try { Thread.sleep(500); } catch (InterruptedException ignored) {} // era 250ms
            }
        }

        return resultados;
    }

    /**
     * Versão original mantida para compatibilidade (usa 5 páginas por padrão).
     */
    public List<ItemEncontrado> pesquisarPrecoItem(
            String descricaoBusca,
            String dataInicial,
            String dataFinal,
            Modalidade... modalidades) {
        return pesquisarPrecoItem(descricaoBusca, dataInicial, dataFinal, 5, modalidades);
    }

    // -------------------------------------------------------------------------
    // Relatório e CSV (mantidos do original)
    // -------------------------------------------------------------------------
    public void exibirRelatorio(List<ItemEncontrado> resultados, String descricaoBusca) {
        if (resultados.isEmpty()) {
            System.out.printf("%nNenhum item com '%s' encontrado.%n", descricaoBusca);
            return;
        }
        System.out.printf("%n%d ocorrencias de '%s' encontradas!%n%n",
                resultados.size(), descricaoBusca);
        System.out.printf("%-25s | %-55s | %12s | %s%n",
                "Municipio", "Descricao", "Vlr Unitario", "Unidade");
        System.out.println("-".repeat(105));
        resultados.forEach(System.out::println);

        List<Double> valores = resultados.stream()
                .filter(i -> i.valorUnitario != null && i.valorUnitario > 0)
                .map(i -> i.valorUnitario).toList();

        if (!valores.isEmpty()) {
            OptionalDouble media = valores.stream().mapToDouble(Double::doubleValue).average();
            double min = valores.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double max = valores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            System.out.println("\nEstatisticas de preco unitario:");
            System.out.printf("   Media:   R$ %,.2f%n", media.orElse(0));
            System.out.printf("   Minimo:  R$ %,.2f%n", min);
            System.out.printf("   Maximo:  R$ %,.2f%n", max);
        }
    }

    public void exportarCsv(List<ItemEncontrado> resultados, String arquivo) throws IOException {
        try (FileWriter fw = new FileWriter(arquivo)) {
            fw.write("municipio,cnpj_orgao,orgao_nome,modalidade,ano,sequencial," +
                    "item_numero,descricao,unidade,qtd,valor_unitario,valor_total\n");
            for (ItemEncontrado ie : resultados) fw.write(ie.toCsvLine() + "\n");
        }
        System.out.println("\nExportado: " + arquivo);
    }

    // -------------------------------------------------------------------------
    // Helpers HTTP e JSON
    // -------------------------------------------------------------------------
    private String get(String url) throws IOException, InterruptedException {
        int tentativas = 0;

        while (tentativas < 3) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(40))
                        .header("Accept", "application/json")
                        .header("User-Agent", "Mozilla/5.0")
                        .GET()
                        .build();

                HttpResponse<String> resp =
                        httpClient.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200)
                    return resp.body();

                throw new IOException("HTTP " + resp.statusCode());

            } catch (Exception e) {
                tentativas++;
                System.out.println("Retry " + tentativas + " -> " + url);
                Thread.sleep(1500);
            }
        }

        throw new IOException("Falha após retries");
    }

    private String texto(JsonNode node, String... campos) {
        JsonNode atual = node;
        for (String campo : campos) {
            if (atual == null || !atual.has(campo)) return null;
            atual = atual.get(campo);
        }
        return (atual != null && !atual.isNull()) ? atual.asText() : null;
    }

    private String textoPlano(JsonNode node, String campo) {
        JsonNode n = node.get(campo);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }

    private Double doubleNode(JsonNode node, String campo) {
        JsonNode n = node.get(campo);
        return (n != null && !n.isNull() && n.isNumber()) ? n.asDouble() : null;
    }

    // -------------------------------------------------------------------------
    // Main — uso standalone (mantido do original)
    // -------------------------------------------------------------------------
    public static void main(String[] args) throws Exception {
        PncpPesquisaPreco pesquisa = new PncpPesquisaPreco();
        String descricao   = "papel a4";
        String dataInicial = "20250101";
        String dataFinal   = "20250329";

        List<ItemEncontrado> resultados = pesquisa.pesquisarPrecoItem(
                descricao, dataInicial, dataFinal,
                Modalidade.PREGAO_ELETRONICO, Modalidade.DISPENSA);

        pesquisa.exibirRelatorio(resultados, descricao);
        pesquisa.exportarCsv(resultados, "precos_" + descricao.replace(" ", "_") + "_mg.csv");
    }
}