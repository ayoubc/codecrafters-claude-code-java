import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
                .addTool(ChatCompletionTool.builder().function(Read.toFunctionDefinition()).build())
                .addTool(ChatCompletionTool.builder().function(Write.toFunctionDefinition()).build())
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
                    ChatCompletionMessageToolCall.Function fun = toolCall.function();
                    String result = "";
                    if (fun.name().equals("Read")) {
                        try {
                            Read read = mapper.readValue(fun.arguments(), Read.class);
                            result = read.execute();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    else if (fun.name().equals("Write")) {
                        try {
                            Write write = mapper.readValue(fun.arguments(), Write.class);
                            result = write.execute();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    createParamsBuilder.addMessage(
                            ChatCompletionToolMessageParam.builder()
                                    .content(result)
                                    .toolCallId(toolCall.id())
                                    .build()
                    );
                }
            }
            else if (ChatCompletion.Choice.FinishReason.STOP.equals(choice.finishReason())) {
                message.content().ifPresent(System.out::println);
                break;
            }
        }
    }
    private static String readFile(String path) {
        try (InputStream is = new FileInputStream(path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
}
