package tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openai.core.JsonValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Bash extends Tool {
    @JsonProperty("command")
    private String command;

    public Bash() {
        super(
                "Bash",
                "Execute a shell command",
                "object",
                List.of("command")
        );
    }

    @Override
    public Map<String, JsonValue> getProperties() {
        return Map.of(
                "command", JsonValue.from(Map.of(
                        "type", JsonValue.from("string"),
                        "description", JsonValue.from("The command to execute")
                ))
        );
    }


    @Override
    public String execute() {
        try {
            ProcessBuilder pb;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                pb = new ProcessBuilder("/bin/bash", "-c", command);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            process.waitFor();
            return output;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
