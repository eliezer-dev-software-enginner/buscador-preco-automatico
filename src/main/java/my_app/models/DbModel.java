package my_app.models;

import java.util.ArrayList;
import java.util.List;


public record DbModel(
        ConfigModel configuracoes,
        List<FornecedorModel> fornecedores, List<ProdutoModel> produtos) {
    // Construtor conveniente para banco vazio
    public static DbModel vazio() {
        return new DbModel(
                new ConfigModel(true),
                new ArrayList<>(), new ArrayList<>());
    }
}