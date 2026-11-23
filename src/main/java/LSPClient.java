// Only needed for the LSP extension.
// You can ignore this file until you reach that extension.
// Course-supplied transport; pass JSON using your existing JSON library.
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lsp.Action;
import lsp.payloads.notifications.DidOpen;
import lsp.payloads.requests.Definition;
import lsp.payloads.requests.References;
import lsp.payloads.requests.TextDocumentIdentifier;
import lsp.payloads.requests.Position;
import lsp.response.Location;
import lsp.LanguageServerClient;

public final class LSPClient implements LanguageServerClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Set<String> openedUris = ConcurrentHashMap.newKeySet();

    private final String url;
    private final HttpClient client = HttpClient.newHttpClient();
    public final Map<String, String> extensionToLanguage;

    private record Configuration(Map<String, String> extensionToLanguage) {}

    public static LSPClient fromEnvironment() throws Exception {
        if (System.getenv("CODECRAFTERS_LSP_URL") == null) return null;
        return new LSPClient();
    }

    public LSPClient() throws Exception {
        url = System.getenv("CODECRAFTERS_LSP_URL");
        if (url == null) throw new IllegalStateException("Run with the course LSP transport");
        extensionToLanguage = new ObjectMapper()
                .readValue(call("configuration", "{}"), Configuration.class).extensionToLanguage();
    }

    @Override
    public String call(String action, String json) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url + "/" + action))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException(response.body());
        return response.body();
    }

    @Override
    public String goToDefinition(String filePath, int line, int character) throws Exception {
        Path workingDir = Path.of("").toAbsolutePath().normalize();
        Path file = workingDir.resolve(filePath).normalize();
        String uri = file.toUri().toASCIIString();

        sendNotificationDidOpen(file, uri);

        String rawResponse = sendDefinitionRequest(uri, line - 1, character - 1);
        return formatDefinitionResult(workingDir, rawResponse);
    }

    private String sendDefinitionRequest(String uri, int zeroBasedLine, int zeroBasedChar) throws Exception {

        Definition payload = Definition.builder()
                .params(Definition.DefinitionParams.builder()
                        .textDocument(TextDocumentIdentifier.builder()
                                .uri(uri)
                                .build())
                        .position(Position.builder()
                                .line(Math.max(0, zeroBasedLine))
                                .character(Math.max(0, zeroBasedChar))
                                .build())
                        .build())
                .build();

        return call(Action.REQUEST, MAPPER.writeValueAsString(payload));
    }

    private void sendNotificationDidOpen(Path file, String uri) throws Exception {
        if (openedUris.contains(uri)) return;

        String fileName = file.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String ext = dotIndex >= 0 ? fileName.substring(dotIndex) : "";
        String languageId = extensionToLanguage != null
                ? extensionToLanguage.getOrDefault(ext, "plaintext")
                : "plaintext";

        DidOpen payload = DidOpen.builder()
                .params(DidOpen.DidOpenParams.builder()
                        .textDocument(DidOpen.TextDocumentItem.builder()
                                .uri(uri)
                                .languageId(languageId)
                                .version(1)
                                .text(Files.readString(file))
                                .build())
                        .build())
                .build();
        call(Action.NOTIFY, MAPPER.writeValueAsString(payload));
        openedUris.add(uri);
    }

    private String formatDefinitionResult(Path workingDir, String rawResponse) throws Exception {
        Location response = MAPPER.readValue(rawResponse, Location.class);

        if (response == null || response.range() == null || response.range().start() == null) {
            throw new IllegalStateException("An expected response body for definition request " + rawResponse);
        }
        int oneBasedLine = response.range().start().line() + 1;
        int oneBasedChar = response.range().start().character() + 1;

        Path targetPath = Path.of(URI.create(response.uri())).toAbsolutePath().normalize();
        String relativePath = workingDir.relativize(targetPath).toString().replace('\\', '/');

        return String.format("Defined in %s:%d:%d", relativePath, oneBasedLine, oneBasedChar);
    }

    @Override
    public String findReferences(String filePath, int line, int character) throws Exception {
        Path workingDir = Path.of("").toAbsolutePath().normalize();
        Path file = workingDir.resolve(filePath).normalize();
        String uri = file.toUri().toASCIIString();

        sendNotificationDidOpen(file, uri);

        References payload = References.builder()
                .params(References.ReferenceParams.builder()
                        .textDocument(TextDocumentIdentifier.builder().uri(uri).build())
                        .position(Position.builder()
                                .line(Math.max(0, line - 1))
                                .character(Math.max(0, character - 1))
                                .build())
                        .context(References.ReferenceContext.builder().includeDeclaration(true).build())
                        .build())
                .build();

        String rawResponse = call(Action.REQUEST, MAPPER.writeValueAsString(payload));
        return formatReferencesResult(workingDir, rawResponse);
    }

    private String formatReferencesResult(Path workingDir, String rawResponse) throws Exception {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "No references found";
        }

        List<Location> locations = MAPPER.readValue(rawResponse, new TypeReference<List<Location>>() {});

        if (locations.isEmpty()) {
            return "No references found";
        }

        Map<String, List<String>> byFile = new HashMap<>();
        for (Location loc : locations) {
            Path refPath = Path.of(URI.create(loc.uri())).toAbsolutePath().normalize();
            String relativePath = workingDir.relativize(refPath).toString().replace('\\', '/');

            int oneBasedLine = loc.range().start().line() + 1;
            int oneBasedChar = loc.range().start().character() + 1;

            byFile.computeIfAbsent(relativePath, k -> new ArrayList<>())
                    .add("  Line " + oneBasedLine + ":" + oneBasedChar);
        }

        int totalCount = locations.size();
        int fileCount = byFile.size();

        StringBuilder sb = new StringBuilder();
        sb.append("Found ")
                .append(totalCount).append(totalCount == 1 ? " reference" : " references")
                .append(" across ")
                .append(fileCount).append(fileCount == 1 ? " file:\n" : " files:\n");

        for (Map.Entry<String, List<String>> entry : byFile.entrySet()) {
            sb.append("\n").append(entry.getKey()).append(":\n");
            for (String lineEntry : entry.getValue()) {
                sb.append(lineEntry).append("\n");
            }
        }

        return sb.toString();
    }
}