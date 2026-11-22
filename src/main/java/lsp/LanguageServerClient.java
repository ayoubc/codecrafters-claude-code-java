package lsp;

public interface LanguageServerClient {
    public String call(String action, String json) throws Exception;
    public String goToDefinition(String filePath, int line, int character) throws Exception;
    default String call(Action action, String json) throws Exception {
        return call(action.value(), json);
    }
}
