package dev.pcrykh.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;

public class ConfigSaver {
    private final ObjectMapper mapper;

    public ConfigSaver(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void save(Path dataFolder, RuntimeConfig config) {
        Path configPath = dataFolder.resolve("config.json");
        ObjectNode root = mapper.createObjectNode();
        root.put("spec_version", config.specVersion());

        ObjectNode commands = mapper.createObjectNode();
        commands.put("root", config.commandRoot());
        root.set("commands", commands);

        ObjectNode runtime = mapper.createObjectNode();
        ObjectNode autosave = mapper.createObjectNode();
        autosave.put("enabled", config.autosave().enabled());
        autosave.put("interval_seconds", config.autosave().intervalSeconds());
        runtime.set("autosave", autosave);

        ObjectNode chat = mapper.createObjectNode();
        chat.put("announce_achievements", config.chat().announceAchievements());
        chat.put("facts_enabled", config.chat().factsEnabled());
        chat.put("facts_interval_seconds", config.chat().factsIntervalSeconds());
        chat.put("prefix", config.chat().prefix());
        runtime.set("chat", chat);

        ObjectNode actionBar = mapper.createObjectNode();
        actionBar.put("progress_enabled", config.actionBar().progressEnabled());
        ObjectNode priority = mapper.createObjectNode();
        priority.put("enabled", config.actionBar().priority().enabled());
        priority.put("display_interval_ticks", config.actionBar().priority().displayIntervalTicks());
        priority.put("cooldown_ticks", config.actionBar().priority().cooldownTicks());
        priority.put("preempt_on_higher_priority", config.actionBar().priority().preemptOnHigherPriority());
        actionBar.set("priority", priority);
        runtime.set("action_bar", actionBar);

        ObjectNode dialogue = mapper.createObjectNode();
        dialogue.put("timeout_seconds", config.dialogue().timeoutSeconds());
        dialogue.put("freeze_villager", config.dialogue().freezeVillager());
        runtime.set("dialogue", dialogue);

        ObjectNode persistence = mapper.createObjectNode();
        ObjectNode playerState = mapper.createObjectNode();
        playerState.put("enabled", config.persistence().playerState().enabled());
        playerState.put("directory", config.persistence().playerState().directory());
        persistence.set("player_state", playerState);
        runtime.set("persistence", persistence);

        root.set("runtime", runtime);

        root.set("facts_sources", toArray(config.factsSources()));
        root.set("category_sources", toArray(config.categorySources()));
        root.set("achievement_sources", toArray(config.achievementSources()));
        root.set("npc_sources", toArray(config.npcSources()));
        root.set("quest_sources", toArray(config.questSources()));

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), root);
        } catch (IOException ex) {
            throw new ConfigException("Failed to write config.json", ex);
        }
    }

    private ArrayNode toArray(Iterable<String> values) {
        ArrayNode arrayNode = mapper.createArrayNode();
        for (String value : values) {
            arrayNode.add(value);
        }
        return arrayNode;
    }
}
