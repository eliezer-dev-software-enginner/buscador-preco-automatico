package my_app.models;

import java.math.BigDecimal;
import java.util.UUID;

public class ProdutoModel {

    private String id;
    private String codigo;
    private String tituloBusca;
    private String palavrasChaves;
    private String urlEncontrada;
    private BigDecimal precoEncontrado;
    private Boolean imprimiu;
    private Boolean cadastrouNoSiplan;
    private long dataCriacao;

    private String pdfCaminho;

    public ProdutoModel(String codigo,
                        String tituloBusca,
                        String urlEncontrada,
                        BigDecimal precoEncontrado,
                        boolean imprimiu,
                        boolean cadastrouNoSiplan,
                        Long dataCriacao,
                        String id,
                        String palavrasChaves
                        ) {

        this.id = id;
        this.codigo = codigo;
        this.tituloBusca = tituloBusca;
        this.urlEncontrada = urlEncontrada;
        this.precoEncontrado = precoEncontrado;
        this.imprimiu = false;
        this.cadastrouNoSiplan = false;
        this.dataCriacao = dataCriacao;
        this.imprimiu = imprimiu;
        this.cadastrouNoSiplan = cadastrouNoSiplan;
        this.palavrasChaves = palavrasChaves;
    }

    public ProdutoModel(String codigo,
                        String tituloBusca,
                        String urlEncontrada,
                        BigDecimal precoEncontrado,
                        boolean imprimiu,
                        boolean cadastrouNoSiplan,
                        String palavrasChaves
                        ) {

        this(codigo, tituloBusca, urlEncontrada, precoEncontrado,
                imprimiu, cadastrouNoSiplan, System.currentTimeMillis(),
                UUID.randomUUID().toString(), palavrasChaves);
    }

    // Construtor vazio (necessário para Jackson)
    public ProdutoModel() {}

    public String getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTituloBusca() {
        return tituloBusca;
    }

    public String getUrlEncontrada() {
        return urlEncontrada;
    }

    public BigDecimal getPrecoEncontrado() {
        return precoEncontrado;
    }

    public Boolean getImprimiu() {
        return imprimiu;
    }

    public Boolean getCadastrouNoSiplan() {
        return cadastrouNoSiplan;
    }

    public long getDataCriacao() {
        return dataCriacao;
    }

    public void setImprimiu(Boolean imprimiu) {
        this.imprimiu = imprimiu;
    }

    public void setCadastrouNoSiplan(Boolean cadastrouNoSiplan) {
        this.cadastrouNoSiplan = cadastrouNoSiplan;
    }

    public String getPalavrasChaves() {
        return palavrasChaves;
    }

    public String getPdfCaminho() { return pdfCaminho; }
    public void setPdfCaminho(String pdfCaminho) {
        this.pdfCaminho = pdfCaminho;
    }
}