package lsp.payloads;

import java.util.Objects;

public record DidOpenNotification(String method, DidOpenParams params) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String method = "textDocument/didOpen";
        private DidOpenParams params;

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder params(DidOpenParams params) {
            this.params = params;
            return this;
        }

        public DidOpenNotification build() {
            return new DidOpenNotification(
                    Objects.requireNonNull(method, "method"),
                    Objects.requireNonNull(params, "params")
            );
        }
    }

    public record DidOpenParams(TextDocumentItem textDocument) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private TextDocumentItem textDocument;

            public Builder textDocument(TextDocumentItem textDocument) {
                this.textDocument = textDocument;
                return this;
            }

            public DidOpenParams build() {
                return new DidOpenParams(Objects.requireNonNull(textDocument, "textDocument"));
            }
        }
    }

    public record TextDocumentItem(String uri, String languageId, int version, String text) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String uri;
            private String languageId;
            private int version = 1;
            private String text;

            public Builder uri(String uri) {
                this.uri = uri;
                return this;
            }

            public Builder languageId(String languageId) {
                this.languageId = languageId;
                return this;
            }

            public Builder version(int version) {
                this.version = version;
                return this;
            }

            public Builder text(String text) {
                this.text = text;
                return this;
            }

            public TextDocumentItem build() {
                return new TextDocumentItem(
                        Objects.requireNonNull(uri, "uri"),
                        Objects.requireNonNull(languageId, "languageId"),
                        version,
                        Objects.requireNonNull(text, "text")
                );
            }
        }
    }
}