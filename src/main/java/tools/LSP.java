package tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.JsonValue;
import lsp.LanguageServerClient;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class LSP extends Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> OPENED_URIS = ConcurrentHashMap.newKeySet();
    private transient LanguageServerClient lspClient;

    @JsonProperty("operation")
    private String operation;

    @JsonProperty("filePath")
    private String filePath;

    @JsonProperty("line")
    private Integer line;

    @JsonProperty("character")
    private Integer character;

    public LSP() {
        super(
                "LSP",
                "Returns the file, line, and character where a symbol is defined.",
                "object",
                List.of("operation", "filePath", "line", "character")
        );
    }

    public void setLspClient(LanguageServerClient lspClient) {
        this.lspClient = lspClient;
    }

    @Override
    public Map<String, JsonValue> getProperties() {
        return Map.of(
                "operation", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "enum", JsonValue.from(List.of("goToDefinition"))
                )),
                "filePath", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("Absolute path, or a path relative to the agent's current working directory.")
                )),
                "line", JsonValue.from(Map.of(
                        "type", JsonValue.from("integer"),
                        "minimum", JsonValue.from(1),
                        "description", JsonValue.from("1-based line number of the symbol.")
                )),
                "character", JsonValue.from(Map.of(
                        "type", JsonValue.from("integer"),
                        "minimum", JsonValue.from(1),
                        "description", JsonValue.from("1-based character offset in UTF-16 code units (same as character count for ASCII).")
                ))
        );
    }

    @Override
    public String execute() {
        try {
            if (lspClient == null) {
                throw new IllegalStateException("LSP client is not configured");
            }

            Path workingDir = Path.of("").toAbsolutePath().normalize();
            Path resolvedFile = workingDir.resolve(filePath).normalize();
            String uri = resolvedFile.toUri().toASCIIString();

            ensureDocumentOpened(resolvedFile, uri);

            Map<String, Object> definitionPayload = Map.of(
                    "method", "textDocument/definition",
                    "params", Map.of(
                            "textDocument", Map.of("uri", uri),
                            "position", Map.of(
                                    "line", Math.max(0, line - 1),
                                    "character", Math.max(0, character - 1)
                            )
                    )
            );

            String rawResponse = lspClient.call("request", MAPPER.writeValueAsString(definitionPayload));
            JsonNode root = MAPPER.readTree(rawResponse);
            JsonNode location = root.isArray() && !root.isEmpty() ? root.get(0) : root;

            String targetUri = location.path("uri").asText();
            JsonNode start = location.path("range").path("start");
            int targetLine = start.path("line").asInt() + 1;
            int targetCharacter = start.path("character").asInt() + 1;

            Path targetPath = Path.of(URI.create(targetUri)).toAbsolutePath().normalize();
            Path relativeTarget = workingDir.relativize(targetPath);

            return String.format("Defined in %s:%d:%d", relativeTarget.toString().replace('\\', '/'), targetLine, targetCharacter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureDocumentOpened(Path resolvedFile, String uri) throws Exception {
        if (OPENED_URIS.contains(uri)) {
            return;
        }

        String fileName = resolvedFile.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex) : "";

        Map<String, String> langMap = lspClient.getExtensionToLanguageMap();
        String languageId = langMap != null ? langMap.getOrDefault(extension, "plaintext") : "plaintext";
        String text = Files.readString(resolvedFile);

        Map<String, Object> textDocument = new HashMap<>();
        textDocument.put("uri", uri);
        textDocument.put("languageId", languageId);
        textDocument.put("version", 1);
        textDocument.put("text", text);

        Map<String, Object> didOpenPayload = Map.of(
                "method", "textDocument/didOpen",
                "params", Map.of("textDocument", textDocument)
        );

        lspClient.call("notify", MAPPER.writeValueAsString(didOpenPayload));
        OPENED_URIS.add(uri);
    }
}
