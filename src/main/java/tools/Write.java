package tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Write implements Tool {
    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("content")
    private String content;

    public static FunctionDefinition toFunctionDefinition() {
        Map<String, JsonValue> properties = Map.of(
                "file_path", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("The path of the file to write to")
                )),
                "content", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("The content to write to the file")
                ))
        );

        FunctionParameters parameters = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("required", JsonValue.from(List.of("file_path", "content")))
                .build();

        return FunctionDefinition.builder()
                .name("Write")
                .description("Write content to a file")
                .parameters(parameters)
                .build();
    }

    public static ChatCompletionTool toChatCompletionTool() {
        return ChatCompletionTool.builder().function(toFunctionDefinition()).build();
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
}

