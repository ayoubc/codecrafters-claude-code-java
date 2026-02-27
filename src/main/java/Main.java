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
import java.nio.file.Path;
import java.util.*;

public class Main {
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

        Queue<ChatCompletionMessage> messages = new LinkedList<>();
        messages.add(
                ChatCompletionMessage.builder()
                        .role( JsonValue.from("user"))
                        .content(prompt)
                        .build()
        );
        while (!messages.isEmpty()) {
            ChatCompletionMessage message = messages.poll();
            List<ChatCompletionMessageToolCall> toolCalls = message.toolCalls().orElse(null);
            if (toolCalls != null && !toolCalls.isEmpty()) {
                ChatCompletionMessageToolCall.Function fun = toolCalls.getFirst().function();
                if (fun.name().equals("Read")) {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        JsonNode rootNode = mapper.readTree(fun.arguments());
                        messages.add(
                                ChatCompletionMessage.builder()
                                        .role( JsonValue.from("tool"))
                                        // need to add tool_call_id
                                        .content(readFile(rootNode.get("file_path").asText()))
                                        .build()
                        );
//                        System.out.println(readFile(rootNode.get("file_path").asText()));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            else {
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

                FunctionDefinition function = FunctionDefinition.builder()
                        .name("Read")
                        .description("Read and return the contents of a file")
                        .parameters(parameters)
                        .build();

                ChatCompletion response = client.chat().completions().create(
                        ChatCompletionCreateParams.builder()
                                .model("anthropic/claude-haiku-4.5")
                                .addUserMessage(message.content().orElse(""))
                                .addTool(ChatCompletionTool.builder().function(function).build())
                                .build()
                );
                ChatCompletion.Choice choice = response.choices().getFirst();

                if (choice.finishReason().equals(ChatCompletion.Choice.FinishReason.STOP)) {
                    System.out.println(choice.message().content().orElse(""));
                }
                else {
                    messages.add(choice.message());
                }
            }
        }


//        if (response.choices().isEmpty()) {
//            throw new RuntimeException("no choices in response");
//        }

        // You can use print statements as follows for debugging, they'll be visible when running tests.
//        System.err.println("Logs from your program will appear here!");

        // TODO: Uncomment the line below to pass the first stage
//        System.out.print(response.choices().get(0).message().content().orElse(""));
//        List<ChatCompletionMessageToolCall> toolCalls = response.choices().getFirst()
//                 .message().toolCalls().orElse(null);

//        if (toolCalls != null && !toolCalls.isEmpty()) {
//            ChatCompletionMessageToolCall.Function fun = toolCalls.getFirst().function();
//            if (fun.name().equals("Read")) {
//                ObjectMapper mapper = new ObjectMapper();
//                try {
//                    JsonNode rootNode = mapper.readTree(fun.arguments());
//                    System.out.println(readFile(rootNode.get("file_path").asText()));
//                }
//                catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    }
    private static String readFile(String path) {
        try (InputStream is = new FileInputStream(path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
