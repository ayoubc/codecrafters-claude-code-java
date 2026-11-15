package skills;

public record SkillMetaData(String name, String description, String context) {
    public boolean isFork() {
        return "fork".equals(context);
    }
}
