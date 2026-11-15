package tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import skills.SkillLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Skill implements Tool {
    private static final SkillLoader skillLoader = SkillLoader.getInstance();

    @JsonProperty("name")
    private String name;

    @JsonProperty("args")
    private String args;

    @Override
    public FunctionDefinition toFunctionDefinition() {
        Map<String, JsonValue> properties = Map.of(
                "name", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("The name of the skill to use")
                )),
                "args", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("Optional arguments for the skill")
                ))
        );

        FunctionParameters parameters = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("required", JsonValue.from(List.of("name")))
                .build();

        return FunctionDefinition.builder()
                .name("Skill")
                .description("Load a skill's instructions into the conversation")
                .parameters(parameters)
                .build();
    }

    @Override
    public String execute() {
        try {
            return skillLoader.loadSkillBody(name, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
