package lsp;

public enum Action {
    NOTIFY("notify"), REQUEST("request");

    private final String value;

    Action(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
