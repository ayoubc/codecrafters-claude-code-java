package tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionTool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Bash implements Tool {
    @JsonProperty("command")
    private String command;

    public static FunctionDefinition toFunctionDefinition() {
        Map<String, JsonValue> properties = Map.of(
                "command", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("The command to execute")
                ))
        );

        FunctionParameters parameters = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("required", JsonValue.from(List.of("command")))
                .build();

        return FunctionDefinition.builder()
                .name("Bash")
                .description("Execute a shell command")
                .parameters(parameters)
                .build();
    }

    public static ChatCompletionTool toChatCompletionTool() {
        return ChatCompletionTool.builder().function(toFunctionDefinition()).build();
    }

    @Override
    public String execute() {
        try {
            ProcessBuilder pb;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                pb = new ProcessBuilder("/bin/bash", "-c", command);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            process.waitFor();
            return output;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
