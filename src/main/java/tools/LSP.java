package tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openai.core.JsonValue;

import java.util.List;
import java.util.Map;


public class LSP extends Tool {

    @JsonProperty("name")
    private String name;

    @JsonProperty("args")
    private String args;

    public LSP() {
        super(
                "LSP",
                "Returns the file, line, and character where a symbol is defined.",
                "object",
                List.of("operation", "filePath", "line", "character")
        );
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
            return "";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
