package lsp;

import java.util.Map;

public interface LanguageServerClient {
    public String call(String action, String json) throws Exception;
    public Map<String, String> getExtensionToLanguageMap();
}
