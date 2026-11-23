package lsp.payloads.requests;

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
