package skills;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillLoader {
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private static final Path skillsPath = Path.of(".claude", "skills");
    private static final String skillMdFileName = "SKILL.md";
    // Matches $ARGUMENTS[N] or $N (e.g. $ARGUMENTS[0], $ARGUMENTS[1], $0, $1)
    private static final Pattern INDEXED_PATTERN = Pattern.compile("\\$ARGUMENTS\\[(\\d+)\\]|\\$(\\d+)");


    public SkillMetaData loadSkillMetaData(Path skillPath) {
        try {
            String frontmatterYaml = extractFrontmatterText(skillPath.resolve(skillMdFileName));

            if (!frontmatterYaml.isEmpty()) {
                SkillMetaData metadata = yamlMapper.readValue(frontmatterYaml, SkillMetaData.class);
                return metadata;
            }

        } catch (IOException e) {
            System.err.println("Error reading or parsing skill: " + e.getMessage());
        }
        return null;
    }

    public List<SkillMetaData> loadSkillsMetaData() {
        try {
            return Files.list(skillsPath)
                    .filter(Files::isDirectory)
                    .map(this::loadSkillMetaData).toList();
        }
        catch (IOException e) {
            System.err.println("Error reading or parsing skills: " + e.getMessage());
        }
        return Collections.emptyList();
    }


    private static String extractFrontmatterText(Path filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean insideFrontmatter = false;

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if ("---".equals(line.trim())) {
                    if (!insideFrontmatter) {
                        insideFrontmatter = true;
                        continue;
                    } else {
                        break;
                    }
                }
                if (insideFrontmatter) {
                    sb.append(line).append("\n");
                }
            }
        }
        return sb.toString();
    }

    public static String loadSkillBody(String skillName) throws IOException {
        StringBuilder sb = new StringBuilder();
        int frontMatterBorder = 0;

        Path skillMdFilePath = skillsPath.resolve(skillName, skillMdFileName);
        try (BufferedReader reader = Files.newBufferedReader(skillMdFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if ("---".equals(line.trim())) {
                    frontMatterBorder++;
                    continue;
                }

                if (frontMatterBorder >= 2) {
                    sb.append(line).append("\n");
                }
            }
        }
        return sb.toString();
    }

    public static String loadSkillBody(String skillName, String rawArguments) throws IOException {

        String skillBody = loadSkillBody(skillName);

        String fullArgs = rawArguments != null ? rawArguments.trim() : "";
        if (fullArgs.isEmpty()) {
            return skillBody;
        }

        // Split arguments by 1 or more whitespace characters
        String[] args = fullArgs.split("\\s+");

        // Replace indexed placeholders ($ARGUMENTS[N] and $N)
        Matcher matcher = INDEXED_PATTERN.matcher(skillBody);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            // Group 1 matches $ARGUMENTS[N], Group 2 matches $N
            String indexStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            int index = Integer.parseInt(indexStr);

            if (index < args.length) {
                // Escape backslashes and dollar signs in the argument to avoid regex replacement errors
                String replacement = Matcher.quoteReplacement(args[index]);
                matcher.appendReplacement(sb, replacement);
            } else {
                // If index is out of bounds, leave the placeholder untouched in the body
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        String result = sb.toString();

        // Replace full $ARGUMENTS string
        return result.replace("$ARGUMENTS", fullArgs);
    }
}
