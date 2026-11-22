package lsp.payloads;

import java.util.Objects;

public record DefinitionRequest(String method, DefinitionParams params) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String method = "textDocument/definition";
        private DefinitionParams params;

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder params(DefinitionParams params) {
            this.params = params;
            return this;
        }

        public DefinitionRequest build() {
            return new DefinitionRequest(
                    Objects.requireNonNull(method, "method"),
                    Objects.requireNonNull(params, "params")
            );
        }
    }

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

    public record DefinitionParams(TextDocumentIdentifier textDocument, Position position) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private TextDocumentIdentifier textDocument;
            private Position position;

            public Builder textDocument(TextDocumentIdentifier textDocument) {
                this.textDocument = textDocument;
                return this;
            }

            public Builder position(Position position) {
                this.position = position;
                return this;
            }

            public DefinitionParams build() {
                return new DefinitionParams(
                        Objects.requireNonNull(textDocument, "textDocument"),
                        Objects.requireNonNull(position, "position")
                );
            }
        }
    }

    public record Position(int line, int character) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int line;
            private int character;

            public Builder line(int line) {
                this.line = line;
                return this;
            }

            public Builder character(int character) {
                this.character = character;
                return this;
            }

            public Position build() {
                return new Position(line, character);
            }
        }
    }
}