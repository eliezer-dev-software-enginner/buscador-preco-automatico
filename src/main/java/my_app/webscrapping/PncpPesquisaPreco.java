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

/**
 * Pesquisa de Preços Praticados no PNCP — filtrando por UF=MG
 *
 * Dependências (Maven):
 *   <dependency>
 *       <groupId>com.fasterxml.jackson.core</groupId>
 *       <artifactId>jackson-databind</artifactId>
 *       <version>2.17.0</version>
 *   </dependency>
 *
 * Ou Gradle:
 *   implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.0'
 *
 * Requer Java 11+
 */
public class PncpPesquisaPreco {

    private static final String BASE_CONSULTA = "https://pncp.gov.br/api/consulta/v1";
    private static final String BASE_PNCP     = "https://pncp.gov.br/api/pncp/v1";

    /**
     * Códigos de modalidade do PNCP.
     * O parâmetro codigoModalidadeContratacao é OBRIGATÓRIO na API —
     * sem ele a API retorna HTTP 400.
     */
    public enum Modalidade {
        LEILAO(1),
        DIALOGO_COMPETITIVO(2),
        CONCURSO(3),
        CONCORRENCIA(4),
        CONCORRENCIA_INTERNACIONAL(5),
        PREGAO_ELETRONICO(6),   // mais comum para compras municipais
        PREGAO_PRESENCIAL(7),
        DISPENSA(8),
        INEXIGIBILIDADE(9);

        public final int codigo;
        Modalidade(int codigo) { this.codigo = codigo; }
    }

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public PncpPesquisaPreco() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // Modelo de item encontrado
    // -------------------------------------------------------------------------
    public static class ItemEncontrado {
        public String municipio;
        public String cnpjOrgao;
        public String orgaoNome;
        public int    modalidade;
        public String ano;
        public String sequencial;
        public String numeroItem;
        public String descricao;
        public String unidade;
        public Double quantidade;
        public Double valorUnitario;
        public Double valorTotal;

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
    // Etapa 1 — Buscar contratações em MG por modalidade
    // -------------------------------------------------------------------------
    public List<JsonNode> buscarContratacoesMG(
            String dataInicial,
            String dataFinal,
            Modalidade modalidade,
            int totalPaginas) {

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

                if (data == null || !data.isArray() || data.isEmpty()) {
                    break;
                }

                for (JsonNode node : data) {
                    contratacoes.add(node);
                }

                System.out.printf("   [%s] Pag. %d: %d contratacoes%n",
                        modalidade.name(), pagina, data.size());

                Thread.sleep(400);

            } catch (Exception e) {
                System.err.printf("   Erro [%s] pag. %d: %s%n",
                        modalidade.name(), pagina, e.getMessage());
                break;
            }
        }

        return contratacoes;
    }

    // -------------------------------------------------------------------------
    // Etapa 2 — Buscar itens de uma contratação
    // -------------------------------------------------------------------------
    public List<JsonNode> buscarItensContratacao(String cnpj, String ano, String sequencial) {
        String cnpjLimpo = cnpj.replaceAll("[.\\-/]", "");
        String url = BASE_PNCP + "/orgaos/" + cnpjLimpo
                + "/compras/" + ano + "/" + sequencial + "/itens";
        try {
            String json = get(url);
            JsonNode root = mapper.readTree(json);
            List<JsonNode> itens = new ArrayList<>();
            if (root.isArray()) {
                root.forEach(itens::add);
            } else if (root.has("data") && root.get("data").isArray()) {
                root.get("data").forEach(itens::add);
            }
            return itens;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Busca o resultado (vencedor/preco homologado) de um item especifico.
     * Este e o PRECO PRATICADO real — nao apenas o estimado.
     */
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
            if (root.isArray()) {
                root.forEach(resultados::add);
            } else if (root.has("data") && root.get("data").isArray()) {
                root.get("data").forEach(resultados::add);
            }
            return resultados;
        } catch (Exception e) {
            return List.of();
        }
    }

    // -------------------------------------------------------------------------
    // Pesquisa principal
    // -------------------------------------------------------------------------
    public List<ItemEncontrado> pesquisarPrecoItem(
            String descricaoBusca,
            String dataInicial,
            String dataFinal,
            Modalidade... modalidades) {

        System.out.printf("Buscando '%s' em MG (%s a %s)...%n",
                descricaoBusca, dataInicial, dataFinal);

        List<ItemEncontrado> resultados = new ArrayList<>();

        for (Modalidade modalidade : modalidades) {
            System.out.printf("%nModalidade: %s (codigo %d)%n",
                    modalidade.name(), modalidade.codigo);

            List<JsonNode> contratacoes =
                    buscarContratacoesMG(dataInicial, dataFinal, modalidade, 5);

            System.out.printf("   %d contratacoes. Filtrando itens...%n",
                    contratacoes.size());

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
                        String numeroItem = textoPlano(item, "numeroItem");

                        ItemEncontrado ie = new ItemEncontrado();
                        ie.municipio     = municipio;
                        ie.cnpjOrgao     = cnpj;
                        ie.orgaoNome     = orgaoNome;
                        ie.modalidade    = modalidade.codigo;
                        ie.ano           = ano;
                        ie.sequencial    = sequencial;
                        ie.numeroItem    = numeroItem;
                        ie.descricao     = descricao;
                        ie.unidade       = textoPlano(item, "unidadeMedida");
                        ie.quantidade    = doubleNode(item, "quantidade");
                        ie.valorUnitario = doubleNode(item, "valorUnitarioEstimado");
                        ie.valorTotal    = doubleNode(item, "valorTotal");

                        // Para buscar o PRECO HOMOLOGADO (vencedor real),
                        // descomente o bloco abaixo:
                        // List<JsonNode> res = buscarResultadoItem(cnpj, ano, sequencial, numeroItem);
                        // if (!res.isEmpty()) {
                        //     ie.valorUnitario = doubleNode(res.get(0), "valorUnitario");
                        // }

                        resultados.add(ie);
                    }
                }

                try { Thread.sleep(250); } catch (InterruptedException ignored) {}
            }
        }

        return resultados;
    }

    // -------------------------------------------------------------------------
    // Relatorio e exportacao CSV
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
                .map(i -> i.valorUnitario)
                .toList();

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
            for (ItemEncontrado ie : resultados) {
                fw.write(ie.toCsvLine() + "\n");
            }
        }
        System.out.println("\nExportado: " + arquivo);
    }

    // -------------------------------------------------------------------------
    // Helpers HTTP e JSON
    // -------------------------------------------------------------------------
    private String get(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp =
                httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode());
        }
        return resp.body();
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
    // Main — configure aqui sua busca
    // -------------------------------------------------------------------------
    public static void main(String[] args) throws Exception {
        PncpPesquisaPreco pesquisa = new PncpPesquisaPreco();

        String descricao   = "papel a4";   // <- palavra-chave do item
        String dataInicial = "20250101";   // <- formato YYYYMMDD
        String dataFinal   = "20250329";

        // Pregao Eletronico (6) e Dispensa (8) são as mais comuns em municipios.
        // Para varrer tudo: Modalidade.values()
        List<ItemEncontrado> resultados = pesquisa.pesquisarPrecoItem(
                descricao, dataInicial, dataFinal,
                Modalidade.PREGAO_ELETRONICO,
                Modalidade.DISPENSA
        );

        pesquisa.exibirRelatorio(resultados, descricao);

        String nomeArquivo = "precos_" + descricao.replace(" ", "_") + "_mg.csv";
        pesquisa.exportarCsv(resultados, nomeArquivo);
    }
}