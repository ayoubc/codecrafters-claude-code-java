package lsp.payloads.requests;

import java.util.Objects;

public record TextDocumentIdentifier(String uri) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String uri;

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public TextDocumentIdentifier build() {
            return new TextDocumentIdentifier(Objects.requireNonNull(uri, "uri"));
        }
    }
}