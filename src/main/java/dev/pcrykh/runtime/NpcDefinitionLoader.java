package dev.pcrykh.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pcrykh.domain.DialogueNode;
import dev.pcrykh.domain.NpcDefinition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NpcDefinitionLoader {
    private final ObjectMapper mapper;

    public NpcDefinitionLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<NpcDefinition> loadAll(List<Path> files) {
        List<NpcDefinition> npcs = new ArrayList<>();
        for (Path path : files) {
            npcs.addAll(loadFile(path));
        }
        return npcs;
    }

    public List<NpcDefinition> loadFile(Path path) {
        enforceLineLimit(path);
        try {
            JsonNode root = mapper.readTree(path.toFile());
            if (root.isArray()) {
                List<NpcDefinition> npcs = new ArrayList<>();
                for (JsonNode node : root) {
                    npcs.add(parseNpc(node, path.toString()));
                }
                return npcs;
            }
            if (root.isObject()) {
                return List.of(parseNpc(root, path.toString()));
            }
            throw new ConfigException("NPC source must be an object or array: " + path);
        } catch (IOException ex) {
            throw new ConfigException("Failed to read npc file: " + path, ex);
        }
    }

    private NpcDefinition parseNpc(JsonNode root, String sourceLabel) {
        require(root, "id", sourceLabel);
        require(root, "display_name", sourceLabel);
        require(root, "world", sourceLabel);
        require(root, "position", sourceLabel);
        require(root, "dialogue", sourceLabel);
        require(root, "start_node_id", sourceLabel);
        require(root, "quest_id", sourceLabel);

        JsonNode position = root.get("position");
        require(position, "x", sourceLabel);
        require(position, "y", sourceLabel);
        require(position, "z", sourceLabel);

        String id = root.get("id").asText();
        String displayName = root.get("display_name").asText();
        String world = root.get("world").asText();
        String profession = root.has("profession") ? root.get("profession").asText() : "NONE";
        String questId = root.get("quest_id").asText();
        String startNodeId = root.get("start_node_id").asText();
        double x = position.get("x").asDouble();
        double y = position.get("y").asDouble();
        double z = position.get("z").asDouble();

        List<DialogueNode> dialogueNodes = parseDialogue(root.get("dialogue"), sourceLabel);
        validateDialogue(dialogueNodes, startNodeId, sourceLabel);

        if (id.isBlank()) {
            throw new ConfigException("npc id must be non-empty: " + sourceLabel);
        }
        if (displayName.isBlank()) {
            throw new ConfigException("npc display_name must be non-empty: " + sourceLabel);
        }
        if (world.isBlank()) {
            throw new ConfigException("npc world must be non-empty: " + sourceLabel);
        }
        if (startNodeId.isBlank()) {
            throw new ConfigException("npc start_node_id must be non-empty: " + sourceLabel);
        }
        if (questId.isBlank()) {
            throw new ConfigException("npc quest_id must be non-empty: " + sourceLabel);
        }

        return new NpcDefinition(id, displayName, world, x, y, z, profession, questId, startNodeId, dialogueNodes);
    }

    private List<DialogueNode> parseDialogue(JsonNode node, String sourceLabel) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            throw new ConfigException("npc dialogue must be a non-empty array: " + sourceLabel);
        }
        List<DialogueNode> nodes = new ArrayList<>();
        for (JsonNode entry : node) {
            require(entry, "id", sourceLabel);
            require(entry, "text", sourceLabel);
            String id = entry.get("id").asText();
            String text = entry.get("text").asText();
            String nextNodeId = entry.has("next_node_id") ? entry.get("next_node_id").asText() : "";
            boolean saveCheckpoint = entry.has("save_checkpoint") && entry.get("save_checkpoint").asBoolean();
            boolean acceptQuest = entry.has("accept_quest") && entry.get("accept_quest").asBoolean();
            int affinityDelta = entry.has("affinity_delta") ? entry.get("affinity_delta").asInt() : 0;
            nodes.add(new DialogueNode(id, text, nextNodeId, saveCheckpoint, acceptQuest, affinityDelta));
        }
        return nodes;
    }

    private void validateDialogue(List<DialogueNode> nodes, String startNodeId, String sourceLabel) {
        Set<String> ids = new HashSet<>();
        for (DialogueNode node : nodes) {
            if (node.id() == null || node.id().isBlank()) {
                throw new ConfigException("dialogue node id must be non-empty: " + sourceLabel);
            }
            if (!ids.add(node.id())) {
                throw new ConfigException("duplicate dialogue node id: " + node.id());
            }
        }
        if (!ids.contains(startNodeId)) {
            throw new ConfigException("start_node_id not found in dialogue: " + sourceLabel);
        }
        for (DialogueNode node : nodes) {
            if (node.nextNodeId() != null && !node.nextNodeId().isBlank() && !ids.contains(node.nextNodeId())) {
                throw new ConfigException("dialogue next_node_id not found: " + node.nextNodeId());
            }
        }
    }

    private void require(JsonNode node, String field, String sourceLabel) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            throw new ConfigException("Missing npc field " + field + ": " + sourceLabel);
        }
    }

    private void enforceLineLimit(Path path) {
        try {
            long lineCount = Files.readAllLines(path, StandardCharsets.UTF_8).size();
            if (lineCount > 300) {
                throw new ConfigException("NPC file exceeds 300 lines: " + path);
            }
        } catch (IOException ex) {
            throw new ConfigException("Failed to read npc file: " + path, ex);
        }
    }
}
