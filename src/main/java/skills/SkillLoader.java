package skills;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class SkillLoader {
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private static final Path skillsPath = Path.of(".claude", "skills");
    private static final String skillMdFileName = "SKILL.md";


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
                String trimmed = line.trim();

                if ("---".equals(trimmed)) {
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
                String trimmed = line.trim();

                if ("---".equals(trimmed)) {
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
}
