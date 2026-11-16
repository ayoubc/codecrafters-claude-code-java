package tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openai.core.JsonValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Read extends Tool {
    @JsonProperty("file_path")
    private String filePath;

    public Read() {
        super(
                "Read",
                "Read and return the contents of a file",
                "object",
                List.of("file_path")
        );
    }

    @Override
    public Map<String, JsonValue> getProperties() {
        return Map.of(
                "file_path", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("The path to the file to read")
                ))
        );
    }

    @Override
    public String execute() {
        try {
            return Files.readString(Path.of(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

