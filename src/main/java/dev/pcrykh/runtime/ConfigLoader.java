package dev.pcrykh.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigLoader {
    private final ObjectMapper mapper;

    public ConfigLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public RuntimeConfig load(Path dataFolder) {
        Path configPath = dataFolder.resolve("config.json");
        if (!Files.exists(configPath)) {
            throw new ConfigException("Missing config.json");
        }

        try {
            long lineCount = Files.readAllLines(configPath, StandardCharsets.UTF_8).size();
            if (lineCount > 300) {
                throw new ConfigException("config.json exceeds 300 lines");
            }

            JsonNode root = mapper.readTree(configPath.toFile());
            require(root, "spec_version");
            require(root, "commands");
            require(root, "runtime");
            require(root, "facts_sources");
            require(root, "category_sources");
            require(root, "achievement_sources");
            require(root, "npc_sources");
            require(root, "quest_sources");

            JsonNode commands = root.get("commands");
            require(commands, "root");
            String commandRoot = commands.get("root").asText();
            if (!"pcrykh".equals(commandRoot)) {
                throw new ConfigException("commands.root must be pcrykh");
            }

            String specVersion = root.get("spec_version").asText();
            if (!specVersion.startsWith("5.")) {
                throw new ConfigException("spec_version must start with 5.");
            }

            JsonNode runtime = root.get("runtime");
            require(runtime, "autosave");
            require(runtime, "chat");
            require(runtime, "action_bar");
            require(runtime, "dialogue");
            require(runtime, "persistence");

            List<String> achievementSources = parseSourceArray(root.get("achievement_sources"), "achievement_sources");
            List<String> factSources = parseSourceArray(root.get("facts_sources"), "facts_sources");
            List<String> categorySources = parseSourceArray(root.get("category_sources"), "category_sources");
            List<String> npcSources = parseSourceArray(root.get("npc_sources"), "npc_sources");
            List<String> questSources = parseSourceArray(root.get("quest_sources"), "quest_sources");

            RuntimeConfig.AutosaveConfig autosave = parseAutosave(runtime.get("autosave"));
            RuntimeConfig.ChatConfig chat = parseChat(runtime.get("chat"));
            RuntimeConfig.ActionBarConfig actionBar = parseActionBar(runtime.get("action_bar"));
            RuntimeConfig.DialogueConfig dialogue = parseDialogue(runtime.get("dialogue"));
            RuntimeConfig.PersistenceConfig persistence = parsePersistence(runtime.get("persistence"));

            FactsSourceResolver factsResolver = new FactsSourceResolver();
            List<Path> factFiles = factsResolver.resolve(dataFolder, factSources);
            FactsLoader factsLoader = new FactsLoader(mapper);
            List<String> facts = factsLoader.loadAll(factFiles);
            if (facts.isEmpty()) {
                throw new ConfigException("facts_sources produced an empty fact list");
            }

            return new RuntimeConfig(
                    specVersion,
                    commandRoot,
                    autosave,
                    chat,
                    actionBar,
                    dialogue,
                    persistence,
                    facts,
                    factSources,
                    categorySources,
                    achievementSources,
                    npcSources,
                    questSources
            );
        } catch (IOException ex) {
            throw new ConfigException("Failed to read config.json", ex);
        }
    }

    private void require(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            throw new ConfigException("Missing required field: " + field);
        }
    }

    private List<String> parseSourceArray(JsonNode node, String fieldName) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            throw new ConfigException(fieldName + " must be a non-empty array");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode entry : node) {
            String value = entry.asText();
            if (value == null || value.isBlank()) {
                throw new ConfigException(fieldName + " entries must be non-empty");
            }
            values.add(value);
        }
        return values;
    }

    private RuntimeConfig.AutosaveConfig parseAutosave(JsonNode autosave) {
        require(autosave, "enabled");
        require(autosave, "interval_seconds");
        return new RuntimeConfig.AutosaveConfig(
                autosave.get("enabled").asBoolean(),
                Math.max(1, autosave.get("interval_seconds").asInt())
        );
    }

    private RuntimeConfig.ChatConfig parseChat(JsonNode chat) {
        require(chat, "announce_achievements");
        require(chat, "facts_enabled");
        require(chat, "facts_interval_seconds");
        require(chat, "prefix");
        return new RuntimeConfig.ChatConfig(
                chat.get("announce_achievements").asBoolean(),
                chat.get("facts_enabled").asBoolean(),
                Math.max(1, chat.get("facts_interval_seconds").asInt()),
                chat.get("prefix").asText()
        );
    }

    private RuntimeConfig.ActionBarConfig parseActionBar(JsonNode actionBar) {
        require(actionBar, "progress_enabled");
        require(actionBar, "priority");

        JsonNode priority = actionBar.get("priority");
        require(priority, "enabled");
        require(priority, "display_interval_ticks");
        require(priority, "cooldown_ticks");
        require(priority, "preempt_on_higher_priority");

        RuntimeConfig.PriorityConfig priorityConfig = new RuntimeConfig.PriorityConfig(
                priority.get("enabled").asBoolean(),
                Math.max(1, priority.get("display_interval_ticks").asInt()),
                Math.max(0, priority.get("cooldown_ticks").asInt()),
                priority.get("preempt_on_higher_priority").asBoolean()
        );

        return new RuntimeConfig.ActionBarConfig(actionBar.get("progress_enabled").asBoolean(), priorityConfig);
    }

    private RuntimeConfig.DialogueConfig parseDialogue(JsonNode dialogue) {
        require(dialogue, "timeout_seconds");
        require(dialogue, "freeze_villager");
        return new RuntimeConfig.DialogueConfig(
                Math.max(1, dialogue.get("timeout_seconds").asInt()),
                dialogue.get("freeze_villager").asBoolean()
        );
    }

    private RuntimeConfig.PersistenceConfig parsePersistence(JsonNode persistence) {
        require(persistence, "player_state");
        JsonNode playerState = persistence.get("player_state");
        require(playerState, "enabled");
        require(playerState, "directory");
        RuntimeConfig.PlayerStateConfig playerStateConfig = new RuntimeConfig.PlayerStateConfig(
                playerState.get("enabled").asBoolean(),
                playerState.get("directory").asText()
        );
        return new RuntimeConfig.PersistenceConfig(playerStateConfig);
    }
}
