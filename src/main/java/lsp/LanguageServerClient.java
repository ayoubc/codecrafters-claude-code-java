package lsp;

public interface LanguageServerClient {
    public String call(String action, String json) throws Exception;
}
