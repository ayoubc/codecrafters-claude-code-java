package lsp.payloads.requests;


import java.util.Objects;

public record References(String method, ReferenceParams params) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String method = "textDocument/references";
        private ReferenceParams params;

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder params(ReferenceParams params) {
            this.params = params;
            return this;
        }

        public References build() {
            return new References(
                    Objects.requireNonNull(method, "method"),
                    Objects.requireNonNull(params, "params")
            );
        }
    }

    public record ReferenceParams(
            TextDocumentIdentifier textDocument,
            Position position,
            ReferenceContext context
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private TextDocumentIdentifier textDocument;
            private Position position;
            private ReferenceContext context = ReferenceContext.builder().includeDeclaration(true).build();

            public Builder textDocument(TextDocumentIdentifier textDocument) {
                this.textDocument = textDocument;
                return this;
            }

            public Builder position(Position position) {
                this.position = position;
                return this;
            }

            public Builder context(ReferenceContext context) {
                this.context = context;
                return this;
            }

            public ReferenceParams build() {
                return new ReferenceParams(
                        Objects.requireNonNull(textDocument, "textDocument"),
                        Objects.requireNonNull(position, "position"),
                        Objects.requireNonNull(context, "context")
                );
            }
        }
    }

    public record ReferenceContext(boolean includeDeclaration) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private boolean includeDeclaration = true;

            public Builder includeDeclaration(boolean includeDeclaration) {
                this.includeDeclaration = includeDeclaration;
                return this;
            }

            public ReferenceContext build() {
                return new ReferenceContext(includeDeclaration);
            }
        }
    }
}