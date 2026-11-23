package lsp.payloads.requests;

import java.util.Objects;

public record Definition(String method, DefinitionParams params) {
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

        public Definition build() {
            return new Definition(
                    Objects.requireNonNull(method, "method"),
                    Objects.requireNonNull(params, "params")
            );
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
}