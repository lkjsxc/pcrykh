package dev.pcrykh.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pcrykh.domain.QuestDefinition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class QuestDefinitionLoader {
    private final ObjectMapper mapper;

    public QuestDefinitionLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<QuestDefinition> loadAll(List<Path> files) {
        List<QuestDefinition> quests = new ArrayList<>();
        for (Path path : files) {
            quests.addAll(loadFile(path));
        }
        return quests;
    }

    public List<QuestDefinition> loadFile(Path path) {
        enforceLineLimit(path);
        try {
            JsonNode root = mapper.readTree(path.toFile());
            if (root.isArray()) {
                List<QuestDefinition> quests = new ArrayList<>();
                for (JsonNode node : root) {
                    quests.add(parseQuest(node, path.toString()));
                }
                return quests;
            }
            if (root.isObject()) {
                return List.of(parseQuest(root, path.toString()));
            }
            throw new ConfigException("Quest source must be an object or array: " + path);
        } catch (IOException ex) {
            throw new ConfigException("Failed to read quest file: " + path, ex);
        }
    }

    private QuestDefinition parseQuest(JsonNode root, String sourceLabel) {
        require(root, "id", sourceLabel);
        require(root, "npc_id", sourceLabel);
        require(root, "title", sourceLabel);
        require(root, "description", sourceLabel);

        String id = root.get("id").asText();
        String npcId = root.get("npc_id").asText();
        String title = root.get("title").asText();
        String description = root.get("description").asText();
        int affinityReward = root.has("affinity_reward") ? root.get("affinity_reward").asInt() : 0;

        if (id.isBlank()) {
            throw new ConfigException("quest id must be non-empty: " + sourceLabel);
        }
        if (npcId.isBlank()) {
            throw new ConfigException("quest npc_id must be non-empty: " + sourceLabel);
        }
        if (title.isBlank()) {
            throw new ConfigException("quest title must be non-empty: " + sourceLabel);
        }

        return new QuestDefinition(id, npcId, title, description, affinityReward);
    }

    private void require(JsonNode node, String field, String sourceLabel) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            throw new ConfigException("Missing quest field " + field + ": " + sourceLabel);
        }
    }

    private void enforceLineLimit(Path path) {
        try {
            long lineCount = Files.readAllLines(path, StandardCharsets.UTF_8).size();
            if (lineCount > 300) {
                throw new ConfigException("Quest file exceeds 300 lines: " + path);
            }
        } catch (IOException ex) {
            throw new ConfigException("Failed to read quest file: " + path, ex);
        }
    }
}
