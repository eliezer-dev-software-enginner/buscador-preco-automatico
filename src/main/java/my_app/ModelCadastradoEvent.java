package my_app;

public class ModelCadastradoEvent {
    private static final ModelCadastradoEvent INSTANCE = new ModelCadastradoEvent();

    private ModelCadastradoEvent() {}

    public static ModelCadastradoEvent getInstance() {
        return INSTANCE;
    }
}
