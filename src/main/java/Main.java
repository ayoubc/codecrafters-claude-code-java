import agents.Agent;
import agents.ClaudeCode;
import com.openai.models.chat.completions.*;

public class Main {

    public static void main(String[] args) {
        if (args.length < 2 || !"-p".equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(1);
        }

        String prompt = args[1];

        Agent agent = new ClaudeCode();
        agent.run(prompt);
    }

}
