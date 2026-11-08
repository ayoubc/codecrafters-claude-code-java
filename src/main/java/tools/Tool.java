package tools;

import com.openai.models.FunctionDefinition;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionTool;

public interface Tool {
    public Object execute();

    public FunctionDefinition toFunctionDefinition();
    default ChatCompletionTool toChatCompletionTool() {
        return ChatCompletionTool.ofFunction(
                ChatCompletionFunctionTool
                        .builder()
                        .function(toFunctionDefinition())
                        .build()
        );
    }
}
