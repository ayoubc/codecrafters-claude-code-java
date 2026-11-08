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


    public SkillMetaData loadSkill(Path skillPath) {
        try {
            String frontmatterYaml = extractFrontmatterString(skillPath.resolve("SKILL.md"));

            if (!frontmatterYaml.isEmpty()) {
                SkillMetaData metadata = yamlMapper.readValue(frontmatterYaml, SkillMetaData.class);
                return metadata;
            }

        } catch (IOException e) {
            System.err.println("Error reading or parsing skill: " + e.getMessage());
        }
        return null;
    }

    public List<SkillMetaData> loadSkills() {
        try {
            return Files.list(skillsPath)
                    .filter(Files::isDirectory)
                    .map(this::loadSkill).toList();
        }
        catch (IOException e) {
            System.err.println("Error reading or parsing skills: " + e.getMessage());
        }
        return Collections.emptyList();
    }


    private static String extractFrontmatterString(Path filePath) throws IOException {
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
}
