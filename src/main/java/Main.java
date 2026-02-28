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

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        List<ChatCompletionMessage> messageHistory = new ArrayList<>();
        messageHistory.add(
                ChatCompletionMessage.builder()
                        .role(JsonValue.from("user"))
                        .refusal("")
                        .content(prompt)
                        .build()
        );

        while (true) {
            ChatCompletionCreateParams.Builder chatParams = ChatCompletionCreateParams.builder()
                    .model("anthropic/claude-haiku-4.5")
                    .addTool(ChatCompletionTool.builder().function(function).build());

            messageHistory.forEach(chatParams::addMessage);
            
            ChatCompletion response = client.chat().completions().create(chatParams.build());
            int i=0;
            for (var choice:response.choices()) {
                System.out.println((i++)+ " " + choice.finishReason());
            }
            ChatCompletion.Choice choice = response.choices().getFirst();
            ChatCompletionMessage message = choice.message();
            messageHistory.add(message);
            List<ChatCompletionMessageToolCall> toolCalls = message.toolCalls().orElse(null);
            if (toolCalls != null && !toolCalls.isEmpty()) {
                for (ChatCompletionMessageToolCall toolCall:toolCalls) {
                    ChatCompletionMessageToolCall.Function fun = toolCall.function();
                    if (fun.name().equals("Read")) {
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            JsonNode rootNode = mapper.readTree(fun.arguments());
                            messageHistory.add(
                                    ChatCompletionMessage.builder()
                                            .role(JsonValue.from("tool"))
                                            .content(readFile(rootNode.get("file_path").asText()))
                                            .putAdditionalProperty("tool_call_id", JsonValue.from(toolCall.id()))
                                            .refusal("")
                                            .build()
                            );
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            else {
                System.out.println(message.content().orElse(""));
                break;
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
