import agents.Agent;
import agents.ClaudeCode;
import lsp.LanguageServerClient;

void main(String[] args) {
    if (args.length < 2 || !"-p".equals(args[0])) {
        System.err.println("Usage: program -p <prompt>");
        System.exit(1);
    }

    String prompt = args[1];

    LanguageServerClient lspClient;
    try {
        lspClient = LSPClient.fromEnvironment();
        if (lspClient != null) {
            System.err.println("LSP client connected");
        }
    } catch (Exception e) {
        throw new RuntimeException("Failed to initialize LSP client", e);
    }

    Agent agent = new ClaudeCode(lspClient);
    agent.run(prompt);
}


