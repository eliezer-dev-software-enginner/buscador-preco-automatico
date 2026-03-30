package my_app;

public class ModelAtualizacaoEvent {
    private static final ModelAtualizacaoEvent INSTANCE = new ModelAtualizacaoEvent();

    private ModelAtualizacaoEvent() {}

    public static ModelAtualizacaoEvent getInstance() {
        return INSTANCE;
    }
}
