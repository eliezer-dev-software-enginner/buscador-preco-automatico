package my_app.webscrapping;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PncpPesquisaPreco {

    public record ItemEncontrado(
            String municipio,
            String cnpjOrgao,
            String orgaoNome,
            int modalidade,
            String ano,
            String sequencial,
            String numeroItem,
            String descricao,
            String unidade,
            Double quantidade,
            Double valorUnitario,
            Double valorTotal
    ) {
        @Override
        public String toString() {
            return String.format("%-25s | %-55s | R$ %10.2f | %s",
                    municipio != null ? municipio : "-",
                    descricao != null ? (descricao.length() > 55
                            ? descricao.substring(0, 52) + "..." : descricao) : "-",
                    valorUnitario != null ? valorUnitario : 0.0,
                    unidade != null ? unidade : "-");
        }

        public String toCsvLine() {
            String desc = descricao != null ? descricao.replace("\"", "'") : "";
            return String.format("%s,%s,\"%s\",%d,%s,%s,%s,\"%s\",%s,%s,%s,%s",
                    municipio, cnpjOrgao, orgaoNome, modalidade,
                    ano, sequencial, numeroItem, desc,
                    unidade, quantidade, valorUnitario, valorTotal);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContratacaoResponse(List<Contratacao> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contratacao(
            OrgaoEntidade orgaoEntidade,
            UnidadeOrgao unidadeOrgao,
            Integer anoCompra,
            Integer sequencialCompra
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrgaoEntidade(
            String cnpj,
            String razaoSocial
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UnidadeOrgao(
            String municipioNome
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemPncp(
            String numeroItem,
            String descricao,
            String unidadeMedida,
            Double quantidade,
            Double valorUnitarioEstimado,
            Double valorTotal
    ) {}

    private static final String BASE_CONSULTA = "https://pncp.gov.br/api/consulta/v1";
    private static final String BASE_PNCP     = "https://pncp.gov.br/api/pncp/v1";

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public enum Modalidade {
        LEILAO(1), DIALOGO_COMPETITIVO(2), CONCURSO(3), CONCORRENCIA(4),
        CONCORRENCIA_INTERNACIONAL(5), PREGAO_ELETRONICO(6), PREGAO_PRESENCIAL(7),
        DISPENSA(8), INEXIGIBILIDADE(9);

        public final int codigo;
        Modalidade(int codigo) { this.codigo = codigo; }
    }

    // ---------------------------------------------------------------------
    // HTTP
    // ---------------------------------------------------------------------
    private String get(String url) throws IOException {
        Request req = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful())
                throw new IOException("HTTP " + resp.code());
            return resp.body() != null ? resp.body().string() : "";
        }
    }

    // ---------------------------------------------------------------------
    // Buscar contratações
    // ---------------------------------------------------------------------
    public List<Contratacao> buscarContratacoesMG(
            String dataInicial,
            String dataFinal,
            Modalidade modalidade,
            int paginas) throws Exception {

        List<Contratacao> lista = new ArrayList<>();

        for (int p = 1; p <= paginas; p++) {
            String url = BASE_CONSULTA + "/contratacoes/publicacao"
                    + "?dataInicial=" + dataInicial
                    + "&dataFinal=" + dataFinal
                    + "&codigoModalidadeContratacao=" + modalidade.codigo
                    + "&uf=MG&pagina=" + p + "&tamanhoPagina=50";

            System.out.println("   [" + modalidade.name() + "] Pag. " + p);
            ContratacaoResponse resp =
                    mapper.readValue(get(url), ContratacaoResponse.class);

            if (resp.data() == null || resp.data().isEmpty())
                break;

            lista.addAll(resp.data());
            Thread.sleep(300);
        }

        return lista;
    }

    // ---------------------------------------------------------------------
    // Buscar itens
    // ---------------------------------------------------------------------
    public List<ItemPncp> buscarItens(String cnpj, String ano, String seq) throws Exception {
        String cnpjLimpo = cnpj.replaceAll("[.\\-/]", "");
        String url = BASE_PNCP + "/orgaos/" + cnpjLimpo
                + "/compras/" + ano + "/" + seq + "/itens";

        ItemPncp[] itens = mapper.readValue(get(url), ItemPncp[].class);
        return Arrays.asList(itens);
    }

    // ---------------------------------------------------------------------
    // Pesquisa principal
    // ---------------------------------------------------------------------
    public List<ItemEncontrado> pesquisarPrecoItem(
            String descricaoBusca,
            String dataInicial,
            String dataFinal,
            int paginas,
            Modalidade... modalidades) throws Exception {

        List<ItemEncontrado> resultados = new ArrayList<>();

        for (Modalidade modalidade : modalidades) {
            List<Contratacao> contratacoes =
                    buscarContratacoesMG(dataInicial, dataFinal, modalidade, paginas);

            for (Contratacao c : contratacoes) {

                if (c.orgaoEntidade() == null) continue;

                String cnpj = c.orgaoEntidade().cnpj();
                String orgaoNome = c.orgaoEntidade().razaoSocial();
                String municipio = c.unidadeOrgao() != null
                        ? c.unidadeOrgao().municipioNome() : null;

                String ano = String.valueOf(c.anoCompra());
                String seq = String.valueOf(c.sequencialCompra());

                List<ItemPncp> itens = buscarItens(cnpj, ano, seq);

                for (ItemPncp item : itens) {
                    if (item.descricao() == null) continue;

                    if (item.descricao().toLowerCase()
                            .contains(descricaoBusca.toLowerCase())) {

                        resultados.add(new ItemEncontrado(
                                municipio,
                                cnpj,
                                orgaoNome,
                                modalidade.codigo,
                                ano,
                                seq,
                                item.numeroItem(),
                                item.descricao(),
                                item.unidadeMedida(),
                                item.quantidade(),
                                item.valorUnitarioEstimado(),
                                item.valorTotal()
                        ));
                    }
                }

                Thread.sleep(400);
            }
        }

        return resultados;
    }
    public static void main(String[] args) throws Exception {
         PncpPesquisaPreco pesquisa = new PncpPesquisaPreco();
         String descricao = "papel a4";
         String dataInicial = "20250101";
         String dataFinal = "20250329";

          List<ItemEncontrado> resultados = pesquisa.pesquisarPrecoItem(
                  descricao, dataInicial, dataFinal,2, Modalidade.PREGAO_ELETRONICO, Modalidade.DISPENSA);

        for (ItemEncontrado resultado : resultados) {
            System.out.println(resultado);
        }

     }

}