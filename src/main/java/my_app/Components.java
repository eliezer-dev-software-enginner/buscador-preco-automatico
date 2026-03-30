package my_app;

import javafx.animation.PauseTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import megalodonte.ReadableState;
import megalodonte.State;
import megalodonte.base.components.Component;
import megalodonte.components.Button;
import megalodonte.components.Select;
import megalodonte.components.Text;
import megalodonte.components.TextFlow;
import megalodonte.components.inputs.Input;
import megalodonte.components.inputs.OnChangeResult;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Row;
import megalodonte.props.InputProps;
import megalodonte.props.RowProps;
import megalodonte.props.SelectProps;
import megalodonte.props.TextProps;
import org.kordamp.ikonli.entypo.Entypo;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static my_app.Utils.formatPhone;

public class Components {

    public static void ShowModal(Component ui,int width, int height){
        Stage stage = new Stage();
        stage.setScene(new Scene((Parent) ui.getJavaFxNode(), width, height));
        stage.setTitle("Detalhes");
        stage.initModality(Modality.WINDOW_MODAL);
       // stage.initOwner(context.javafxStage());
        stage.show();
    }

    public static void ShowAlertError(String message) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Erro");

        ButtonType okButton = new ButtonType("Fechar", ButtonBar.ButtonData.OK_DONE);

        alert.getButtonTypes().add(okButton);
        alert.setOnCloseRequest(event -> alert.close());
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void ShowPopup(Stage stage, String message) {
        Popup popup = new Popup();

        Label label = new Label(message);
        label.setStyle("""
                -fx-background-color: #333;
                -fx-text-fill: white;
                -fx-padding: 10 16;
                -fx-background-radius: 6;
            """);

        popup.getContent().add(label);
        popup.show(stage);

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> popup.hide());
        delay.play();
    }


    public static Component produtoForm( State<String> urlState,  State<String> precoState, State<String> imprimiuState,
                                         State<String> cadastrouNoSiplanState){
        return new Row(new RowProps().spacingOf(10))
                .children(
                    InputColumn("url", urlState, "Url encontrada"),
                        InputColumnCurrency("Preço", precoState),
                        SelectColumn("Imprimiu?", List.of("Sim","Não"), imprimiuState, it->it),
                        SelectColumn("Registou no Siplan?", List.of("Sim","Não"), cadastrouNoSiplanState, it->it)
                );
    }


    private static final NumberFormat BRL =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    public static Component InputColumnCurrency(String label, State<String> inputState) {
        var icon = Entypo.CREDIT;
        var fonticon = FontIcon.of(icon, 15, Color.web("green"));

        var inputProps = getInputProps("R$ 0,00").width(140).borderWidth(1)
                .borderColor("black");

        // inputState armazena valores brutos (em centavos), campo exibe formato BRL
        var input = new Input(inputState, inputProps)
                .onInitialize(value -> {
                    if (value.matches("\\d+")) {
                        BigDecimal realValue = new BigDecimal(value).movePointLeft(2);
                        return OnChangeResult.of(BRL.format(realValue), value);
                    }
                    return OnChangeResult.of(value, value);
                })
                .onChange(value -> {
                    String numeric = value.replaceAll("[^0-9]", "");
                    if (numeric.isEmpty()) {
                        return OnChangeResult.of("R$ 0,00", "0");
                    }

                    // Converte centavos para BigDecimal do valor real
                    BigDecimal realValue = new BigDecimal(numeric).movePointLeft(2);
                    return OnChangeResult.of(BRL.format(realValue), numeric);
                })
                .lockCursorToEnd()
                .left(fonticon);

        return new Column()
                .c_child(new Text(label,  new TextProps()))
                .c_child(input);
    }

    static InputProps getInputProps(String placeholder){
        return getInputProps(placeholder, 31);
    }

    static InputProps getInputProps(String placeholder, int height){
        return   new InputProps().height(height)
                .placeHolder(placeholder);
    }

    public static Component InputColumn(String label, ReadableState<String> inputState, String placeholder) {
        var props =  getInputProps(placeholder);

        TextProps textProps = new TextProps();
        if(label.equals("Login") || label.equals("Senha") || label.equals("Licença")){
            textProps.textColor("#fff");
        }
        return new Column()
                .c_child(new Text(label, textProps))
                .c_child(new Input((State<String>) inputState,
                                props.borderWidth(1).borderColor("black")
                        )
                );
    }

    public static Component InputColumn(String label, ReadableState<String> inputState) {
        return InputColumn(label, inputState, "");
    }

    private final static SelectProps selectProps = new SelectProps()
            .minWidth(100)
            .height(31);

    public static <T> Component SelectColumn(String label, List<T> list, State<T> stateSelected, Function<T, String> display) {
        return new Column()
                .c_child(new Text(label,  new TextProps()))
                .c_child(new Select<T>(selectProps)
                        .items(list)
                        .value(stateSelected)
                        .displayText(display)
                );
    }


    public static <T> Component SelectColumn(String label, State<List<T>> listState, State<T> stateSelected, Function<T, String> display) {
        return new Column()
                .c_child(new Text(label,  new TextProps()))
                .c_child(new Select<T>(selectProps)
                        .items(listState)
                        .value(stateSelected)
                        .displayText(display)
                );
    }

    public static Component TextWithValue(String label, ReadableState<String> valueState) {
        return new Row()
                .r_child(new Text(label,  new TextProps().bold()))
                .r_child(new Text(valueState,  new TextProps()));
    }

    public static Row TextWithDetailsAndButton(String label, Object value,
                                               String btnLabel,
                                               Runnable action) {
        var textValueComponent = new Text(value == null? "" : value.toString(),
                new TextProps().bold());

        return new Row()
                .children(
                        new Button(btnLabel).onClick(action),
                        new Text(label, new TextProps().bold()),
                        textValueComponent
                );
    }

    public static Row TextWithDetails(String label, Object value, boolean wrapText) {
        var comp = new Text(value == null? "" : value.toString(),
                new TextProps().bold());

        var textValueComponent = wrapText?  new TextFlow(comp) :comp;

        return new Row()
                .children(
                        new Text(label, new TextProps().bold()),
                        textValueComponent
                );
    }
    public static Row TextWithDetails(String label, Object value) {
        return TextWithDetails(label, value, false);
    }

    public static Component InputColumnPhone(String label, State<String> inputState) {
        var inputProps =  new InputProps()
                .height(31).placeHolder("(00) 00000-0000")
                .borderWidth(1)
                .borderColor("black");


        var input = new Input(inputState, inputProps)
                .onInitialize(value -> {
                    String formatted = formatPhone(value);
                    return OnChangeResult.of(formatted, value);
                })
                .onChange(value -> {
                    String numeric = value.replaceAll("[^0-9]", "");

                    // Limita a 11 dígitos (padrão BR com DDD)
                    if (numeric.length() > 11) {
                        numeric = numeric.substring(0, 11);
                    }

                    String formatted = formatPhone(numeric);
                    return OnChangeResult.of(formatted, numeric);
                })
                .lockCursorToEnd();

        return new Column()
                .c_child(new Text(label,  new TextProps()))
                .c_child(input);
    }

}
