package agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.*;
import skills.SkillLoader;
import skills.SkillMetaData;
import tools.Bash;
import tools.Read;
import tools.Tool;
import tools.Write;

import java.io.IOException;
import java.util.List;

public class ClaudeCode implements Agent {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String MODEL = "anthropic/claude-haiku-4.5";
    private static final SkillLoader skillLoader = new SkillLoader();

    private String apiKey;
    private String baseUrl;
    private OpenAIClient client;
    private ChatCompletionCreateParams.Builder createParamsBuilder;


    public ClaudeCode () {
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        String baseUrl = System.getenv("OPENROUTER_BASE_URL");

        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENROUTER_API_KEY is not set");
        }

        client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        createParamsBuilder = ChatCompletionCreateParams.builder()
                .model(MODEL)
                .addTool(new Read().toChatCompletionTool())
                .addTool(new Write().toChatCompletionTool())
                .addTool(new Bash().toChatCompletionTool())
                .addSystemMessage(buildSystemPrompt());
    }

    public void run(String prompt) {
        prompt = prompt.trim();

        createParamsBuilder.addUserMessage(transformSkillToPrompt(prompt));

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
                                        .content(executeFunction(toolCall.function().get().function()))
                                        .toolCallId(toolCall.function().get().id())
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

    private String transformSkillToPrompt(String prompt) {
        if (prompt.startsWith("/")) {
            String[] parts = prompt.split("\\s+", 2);

            String skillName = parts[0].substring(1);
            String remainingArgs = parts.length > 1 ? parts[1] : null;
            String skillBody = loadSkillBody(skillName, remainingArgs);
            if (skillBody == null || skillBody.isEmpty()) {
                return prompt;
            }
            // when calling a skill the prompt becaomse the body of the skill
            return skillBody;
        }
        return prompt;
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder("You have access to the following skills:\n\n");
        List<SkillMetaData> skills = skillLoader.loadSkillsMetaData();
//        System.out.println(skills);
        for (SkillMetaData skill : skills) {
            sb.append("- ")
                    .append(skill.name())
                    .append(": ")
                    .append(skill.description())
                    .append("\n");
        }
        return sb.toString();
    }

    private String loadSkillBody(String skillName, String args) {
        try {
            return skillLoader.loadSkillBody(skillName, args);
        } catch (IOException e) {
            System.err.println("Could not body of the skill: " + skillName);
            e.printStackTrace();
        }
        return null;
    }

    private String executeFunction(ChatCompletionMessageFunctionToolCall.Function fun) throws JsonProcessingException {
        Tool tool = switch (fun.name()) {
            case "Read" -> mapper.readValue(fun.arguments(), Read.class);
            case "Write" -> mapper.readValue(fun.arguments(), Write.class);
            case "Bash" -> mapper.readValue(fun.arguments(), Bash.class);
            default -> throw new IllegalArgumentException("Unsupported function " + fun.name());
        };
        return tool.execute().toString();
    }
}
