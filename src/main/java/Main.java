import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String MODEL = "anthropic/claude-haiku-4.5";

    public static void main(String[] args) {
        if (args.length < 2 || !"-p".equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(1);
        }

        String prompt = args[1];

        String apiKey = System.getenv("OPENROUTER_API_KEY");
        String baseUrl = System.getenv("OPENROUTER_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENROUTER_API_KEY is not set");
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        final ChatCompletionCreateParams.Builder createParamsBuilder = ChatCompletionCreateParams.builder()
                .model(MODEL)
                .addTool(Read.toChatCompletionTool())
                .addTool(Write.toChatCompletionTool())
                .addTool(Bash.toChatCompletionTool())
                .addUserMessage(prompt);

        while (true) {
            ChatCompletion response = client.chat().completions().create(createParamsBuilder.build());
            if (response.choices().isEmpty()) {
                throw new RuntimeException("no choices in response");
            }

            ChatCompletion.Choice choice = response.choices().getFirst();
            ChatCompletionMessage message = choice.message();
            createParamsBuilder.addMessage(message);

            if (ChatCompletion.Choice.FinishReason.TOOL_CALLS.equals(choice.finishReason())) {
                List<ChatCompletionMessageToolCall> toolCalls = message.toolCalls().orElse(null);
                if (toolCalls == null || toolCalls.isEmpty()) {
                    throw new IllegalStateException("no tool calls found in the message");
                }
                for (ChatCompletionMessageToolCall toolCall:toolCalls) {
                    try {
                        createParamsBuilder.addMessage(
                                ChatCompletionToolMessageParam.builder()
                                        .content(executeFunction(toolCall.function()))
                                        .toolCallId(toolCall.id())
                                        .build()
                        );
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (ChatCompletion.Choice.FinishReason.STOP.equals(choice.finishReason())) {
                message.content().ifPresent(System.out::println);
                break;
            }
        }
    }
    private static String executeFunction(ChatCompletionMessageToolCall.Function fun) throws JsonProcessingException {
        Tool tool = switch (fun.name()) {
            case "Read" -> mapper.readValue(fun.arguments(), Read.class);
            case "Write" -> mapper.readValue(fun.arguments(), Write.class);
            case "Bash" -> mapper.readValue(fun.arguments(), Bash.class);
            default -> throw new IllegalArgumentException("Unsupported function " + fun.name());
        };
        return tool.execute().toString();
    }
    private interface Tool {
        public Object execute();
    }

    private static class Read implements Tool {
        @JsonProperty("file_path")
        private String filePath;

        public static FunctionDefinition toFunctionDefinition() {
            Map<String, JsonValue> properties = Map.of(
                    "file_path", JsonValue.from(Map.of(
                            "type", JsonValue.from("string"),
                            "description", JsonValue.from("The path to the file to read")
                    ))
            );

            FunctionParameters parameters = FunctionParameters.builder()
                    .putAdditionalProperty("type", JsonValue.from("object"))
                    .putAdditionalProperty("properties", JsonValue.from(properties))
                    .putAdditionalProperty("required", JsonValue.from(List.of("file_path")))
                    .build();

            return FunctionDefinition.builder()
                    .name("Read")
                    .description("Read and return the contents of a file")
                    .parameters(parameters)
                    .build();
        }
        public static ChatCompletionTool toChatCompletionTool() {
            return ChatCompletionTool.builder().function(toFunctionDefinition()).build();
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

    private static class Write implements Tool {
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

    private static class Bash implements Tool {
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
}
