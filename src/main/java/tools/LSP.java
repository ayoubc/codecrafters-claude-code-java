package tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openai.core.JsonValue;
import lsp.LanguageServerClient;

import java.util.List;
import java.util.Map;


public class LSP extends Tool {
    private transient LanguageServerClient lspClient;

    @JsonProperty("operation")
    private String operation;

    @JsonProperty("filePath")
    private String filePath;

    @JsonProperty("line")
    private Integer line;

    @JsonProperty("character")
    private Integer character;

    public LSP() {
        super(
                "LSP",
                "Returns the file, line, and character where a symbol is defined.",
                "object",
                List.of("operation", "filePath", "line", "character")
        );
    }

    public void setLspClient(LanguageServerClient lspClient) {
        this.lspClient = lspClient;
    }

    @Override
    public Map<String, JsonValue> getProperties() {
        return Map.of(
                "operation", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "enum", JsonValue.from(List.of("goToDefinition"))
                )),
                "filePath", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("Absolute path, or a path relative to the agent's current working directory.")
                )),
                "line", JsonValue.from(Map.of(
                        "type", JsonValue.from("integer"),
                        "minimum", JsonValue.from(1),
                        "description", JsonValue.from("1-based line number of the symbol.")
                )),
                "character", JsonValue.from(Map.of(
                        "type", JsonValue.from("integer"),
                        "minimum", JsonValue.from(1),
                        "description", JsonValue.from("1-based character offset in UTF-16 code units (same as character count for ASCII).")
                ))
        );
    }

    @Override
    public String execute() {
        try {
            if (lspClient == null) {
                throw new IllegalStateException("LSP client is not configured");
            }
            return switch (operation) {
                case "goToDefinition" -> lspClient.goToDefinition(filePath, line, character);
                case "findReferences" -> lspClient.findReferences(filePath, line, character);
                default -> throw new IllegalArgumentException("Unsupported LSP operation: " + operation);
            };

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
