package tools;

import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionTool;

import java.util.List;
import java.util.Map;

public abstract class Tool {
    protected String toolName;
    protected String toolDescription;
    protected String functionParamsType;
    protected List<String> requiredParams;

    public Tool(String name, String desc, String paramsType, List<String> reqParams) {
        toolName = name;
        toolDescription = desc;
        functionParamsType = paramsType;
        requiredParams = reqParams;
    }
    public abstract  Object execute();

    public FunctionDefinition toFunctionDefinition() {
        return FunctionDefinition.builder()
                .name(toolName)
                .description(toolDescription)
                .parameters(getFunctionParams())
                .build();
    }
    public FunctionParameters getFunctionParams() {
        return FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from(functionParamsType))
                .putAdditionalProperty("properties", JsonValue.from(getProperties()))
                .putAdditionalProperty("required", JsonValue.from(requiredParams))
                .build();
    }
    public abstract Map<String, JsonValue> getProperties();
    public ChatCompletionTool toChatCompletionTool() {
        return ChatCompletionTool.ofFunction(
                ChatCompletionFunctionTool
                        .builder()
                        .function(toFunctionDefinition())
                        .build()
        );
    }
}
