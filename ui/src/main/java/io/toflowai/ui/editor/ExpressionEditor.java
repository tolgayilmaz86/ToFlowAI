package io.toflowai.ui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

/**
 * Rich expression editor component with syntax highlighting,
 * variable autocomplete, and validation.
 */
public class ExpressionEditor extends VBox {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]*)}");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
            "\\b(if|else|and|or|not|eq|ne|gt|lt|gte|lte|contains|startsWith|endsWith|length|trim|upper|lower|concat|substring|replace|split|join|now|format|parse|toNumber|toString|toBoolean)\\b");

    private final TextArea expressionArea;
    private final Label validationLabel;
    private final HBox toolbar;
    private final ListView<String> suggestionList;
    private final Popup suggestionPopup;

    private final StringProperty expressionProperty = new SimpleStringProperty("");
    private final BooleanProperty validProperty = new SimpleBooleanProperty(true);

    private final List<String> availableVariables = new ArrayList<>();
    private final List<String> availableFunctions = List.of(
            "if(condition, then, else)",
            "and(a, b)",
            "or(a, b)",
            "not(value)",
            "eq(a, b)",
            "ne(a, b)",
            "gt(a, b)",
            "lt(a, b)",
            "gte(a, b)",
            "lte(a, b)",
            "contains(string, search)",
            "startsWith(string, prefix)",
            "endsWith(string, suffix)",
            "length(string)",
            "trim(string)",
            "upper(string)",
            "lower(string)",
            "concat(a, b, ...)",
            "substring(string, start, end)",
            "replace(string, search, replacement)",
            "split(string, delimiter)",
            "join(array, delimiter)",
            "now()",
            "format(date, pattern)",
            "parse(string, pattern)",
            "toNumber(value)",
            "toString(value)",
            "toBoolean(value)");

    private final List<String> validFunctionNames = List.of(
            "if", "and", "or", "not", "eq", "ne", "gt", "lt", "gte", "lte",
            "contains", "startsWith", "endsWith", "length", "trim", "upper", "lower",
            "concat", "substring", "replace", "split", "join", "now", "format", "parse",
            "toNumber", "toString", "toBoolean");

    private Consumer<String> onExpressionChange;

    public ExpressionEditor() {
        setSpacing(8);
        setPadding(new Insets(10));
        setStyle(
                "-fx-background-color: #1e1e1e; -fx-border-color: #404040; -fx-border-radius: 5; -fx-background-radius: 5;");

        toolbar = createToolbar();
        expressionArea = createExpressionArea();
        validationLabel = createValidationLabel();

        suggestionList = new ListView<>();
        suggestionPopup = new Popup();
        setupSuggestions();

        VBox.setVgrow(expressionArea, Priority.ALWAYS);
        getChildren().addAll(toolbar, expressionArea, validationLabel);

        // Bind properties
        expressionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            expressionProperty.set(newVal);
            validateExpression(newVal);
            if (onExpressionChange != null) {
                onExpressionChange.accept(newVal);
            }
        });
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 5, 0));

        Label label = new Label("Expression");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #e5e5e5;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button insertVarBtn = new Button("Variable");
        insertVarBtn.setGraphic(FontIcon.of(MaterialDesignV.VARIABLE, 12));
        insertVarBtn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: #e5e5e5; -fx-font-size: 11px;");
        insertVarBtn.setOnAction(e -> insertVariable());

        Button insertFuncBtn = new Button("Function");
        insertFuncBtn.setGraphic(FontIcon.of(MaterialDesignF.FUNCTION_VARIANT, 12));
        insertFuncBtn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: #e5e5e5; -fx-font-size: 11px;");
        insertFuncBtn.setOnAction(e -> showFunctionPicker());

        Button helpBtn = new Button();
        helpBtn.setGraphic(FontIcon.of(MaterialDesignH.HELP_CIRCLE_OUTLINE, 14));
        helpBtn.setStyle("-fx-background-color: transparent;");
        helpBtn.setOnAction(e -> showHelp());

        toolbar.getChildren().addAll(label, spacer, insertVarBtn, insertFuncBtn, helpBtn);
        return toolbar;
    }

    private TextArea createExpressionArea() {
        TextArea area = new TextArea();
        area.setPromptText("Enter expression... e.g., ${variableName} or concat('Hello', ' ', ${name})");
        area.setPrefRowCount(3);
        area.setWrapText(true);
        area.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; " +
                "-fx-font-size: 13px; " +
                "-fx-control-inner-background: #252525; " +
                "-fx-text-fill: #d4d4d4;");

        // Handle keyboard shortcuts
        area.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.SPACE) {
                showSuggestions();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideSuggestions();
            }
        });

        return area;
    }

    private Label createValidationLabel() {
        Label label = new Label();
        label.setStyle("-fx-font-size: 11px;");
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    private void setupSuggestions() {
        suggestionList.setPrefHeight(150);
        suggestionList.setPrefWidth(300);
        suggestionList.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #404040;");
        suggestionList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #e5e5e5;");
                }
            }
        });

        suggestionList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                insertSuggestion();
            }
        });

        suggestionList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                insertSuggestion();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideSuggestions();
            }
        });

        suggestionPopup.getContent().add(suggestionList);
        suggestionPopup.setAutoHide(true);
    }

    private void showSuggestions() {
        List<String> suggestions = new ArrayList<>();

        // Add variables
        for (String var : availableVariables) {
            suggestions.add("${" + var + "}");
        }

        // Add functions
        suggestions.addAll(availableFunctions);

        suggestionList.getItems().setAll(suggestions);

        if (!suggestions.isEmpty() && getScene() != null) {
            var bounds = expressionArea.localToScreen(expressionArea.getBoundsInLocal());
            suggestionPopup.show(expressionArea, bounds.getMinX(), bounds.getMaxY());
        }
    }

    private void hideSuggestions() {
        suggestionPopup.hide();
    }

    private void insertSuggestion() {
        String selected = suggestionList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int caret = expressionArea.getCaretPosition();
            expressionArea.insertText(caret, selected);
            hideSuggestions();
            expressionArea.requestFocus();
        }
    }

    private void insertVariable() {
        if (availableVariables.isEmpty()) {
            showInfo("No Variables", "No variables are defined. Use the Variable Manager to create variables.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(availableVariables.getFirst(), availableVariables);
        dialog.setTitle("Insert Variable");
        dialog.setHeaderText("Select a variable to insert");
        dialog.setContentText("Variable:");

        dialog.showAndWait().ifPresent(var -> {
            int caret = expressionArea.getCaretPosition();
            expressionArea.insertText(caret, "${" + var + "}");
            expressionArea.requestFocus();
        });
    }

    private void showFunctionPicker() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(availableFunctions.getFirst(), availableFunctions);
        dialog.setTitle("Insert Function");
        dialog.setHeaderText("Select a function to insert");
        dialog.setContentText("Function:");

        dialog.showAndWait().ifPresent(func -> {
            int caret = expressionArea.getCaretPosition();
            expressionArea.insertText(caret, func);
            expressionArea.requestFocus();
        });
    }

    private void validateExpression(String expression) {
        if (expression == null || expression.isBlank()) {
            setValid(true, null);
            return;
        }

        // Check for unclosed variable references
        int openBraces = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '$' && i + 1 < expression.length() && expression.charAt(i + 1) == '{') {
                openBraces++;
                i++;
            } else if (c == '}' && openBraces > 0) {
                openBraces--;
            }
        }

        if (openBraces > 0) {
            setValid(false, "Unclosed variable reference: missing '}'");
            return;
        }

        // Check for balanced parentheses
        int parenCount = 0;
        for (char c : expression.toCharArray()) {
            if (c == '(')
                parenCount++;
            else if (c == ')')
                parenCount--;
            if (parenCount < 0) {
                setValid(false, "Unbalanced parentheses: extra ')'");
                return;
            }
        }

        if (parenCount > 0) {
            setValid(false, "Unbalanced parentheses: missing ')'");
            return;
        }

        // Check for empty variable references
        Matcher matcher = Pattern.compile("\\$\\{\\s*}").matcher(expression);
        if (matcher.find()) {
            setValid(false, "Empty variable reference");
            return;
        }

        // Check for undefined variables
        Matcher varMatcher = VARIABLE_PATTERN.matcher(expression);
        while (varMatcher.find()) {
            String varName = varMatcher.group(1).trim();
            if (!availableVariables.contains(varName)) {
                setValid(false, "Unknown variable: " + varName);
                return;
            }
        }

        // Check for undefined functions
        Matcher funcMatcher = FUNCTION_PATTERN.matcher(expression);
        while (funcMatcher.find()) {
            String funcName = funcMatcher.group(1);
            if (!validFunctionNames.contains(funcName)) {
                setValid(false, "Unknown function: " + funcName);
                return;
            }
        }

        setValid(true, "✓ Valid expression");
    }

    private void setValid(boolean valid, String message) {
        validProperty.set(valid);

        if (message != null) {
            validationLabel.setText(message);
            validationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (valid ? "#22c55e" : "#ef4444") + ";");
            validationLabel.setVisible(true);
            validationLabel.setManaged(true);
        } else {
            validationLabel.setVisible(false);
            validationLabel.setManaged(false);
        }

        // Update border color
        expressionArea.setStyle(expressionArea.getStyle() +
                (valid ? "" : " -fx-border-color: #ef4444;"));
    }

    private void showHelp() {
        Alert help = new Alert(Alert.AlertType.INFORMATION);
        help.setTitle("Expression Editor Help");
        help.setHeaderText("Writing Expressions");
        help.setContentText("""
                Expressions allow you to create dynamic values using:

                • Variables: ${variableName}
                  Reference stored values

                • Functions: functionName(arguments)
                  Transform and combine values

                • Text: Combine with regular text
                  Example: Hello, ${name}!

                Common Functions:
                • if(condition, then, else)
                • concat(a, b, ...) - Join strings
                • upper(text) / lower(text)
                • trim(text) - Remove whitespace
                • contains(text, search)
                • now() - Current timestamp

                Press Ctrl+Space for autocomplete.
                """);
        help.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Public API

    public String getExpression() {
        return expressionProperty.get();
    }

    public void setExpression(String expression) {
        expressionArea.setText(expression);
    }

    public StringProperty expressionProperty() {
        return expressionProperty;
    }

    public boolean isValid() {
        return validProperty.get();
    }

    public BooleanProperty validProperty() {
        return validProperty;
    }

    public void setAvailableVariables(List<String> variables) {
        availableVariables.clear();
        availableVariables.addAll(variables);
        validateExpression(getExpression());
    }

    public void setOnExpressionChange(Consumer<String> handler) {
        this.onExpressionChange = handler;
    }

    public void setPromptText(String text) {
        expressionArea.setPromptText(text);
    }

    public void setPrefRowCount(int count) {
        expressionArea.setPrefRowCount(count);
    }
}
