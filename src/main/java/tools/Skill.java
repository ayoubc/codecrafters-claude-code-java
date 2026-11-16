package tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openai.core.JsonValue;
import skills.SkillLoader;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Skill extends Tool {
    private static final SkillLoader skillLoader = SkillLoader.getInstance();

    @JsonProperty("name")
    private String name;

    @JsonProperty("args")
    private String args;

    private transient Function<String, String> subAgentRunner;

    public void setSubAgentRunner(Function<String, String> runner) {
        subAgentRunner = runner;
    }

    public Skill() {
        super(
               "Skill",
                "Load a skill's instructions into the conversation",
                "object",
                List.of("name")
        );
    }

    @Override
    public Map<String, JsonValue> getProperties() {
        return Map.of(
                "name", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("The name of the skill to use")
                )),
                "args", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("Optional arguments for the skill")
                ))
        );
    }

    @Override
    public String execute() {
        try {
            var skillBody = skillLoader.loadSkillBody(name, args);
            if (skillLoader.loadSkillMetaData(name).isFork()) {
                return subAgentRunner.apply(skillBody);
            }
            return skillBody;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
