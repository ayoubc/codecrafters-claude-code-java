package tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openai.core.JsonValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Write extends Tool {
    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("content")
    private String content;

    public Write() {
        super(
                "Write",
                "Write content to a file",
                "object",
                List.of("file_path", "content")
        );
    }

    @Override
    public String execute() {
        try {
            Files.writeString(Path.of(filePath), content);
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, JsonValue> getProperties() {
        return Map.of(
                "file_path", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("The path of the file to write to")
                )),
                "content", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("The content to write to the file")
                ))
        );
    }
}

