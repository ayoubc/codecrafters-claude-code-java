package lsp.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Location(String uri, Range range) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Range(Position start, Position end) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Position(int line, int character) {}
}