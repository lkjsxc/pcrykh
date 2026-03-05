package dev.pcrykh.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStateStore {
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final Path directory;
    private final Map<UUID, PlayerStoryState> cache = new HashMap<>();

    public PlayerStateStore(ObjectMapper mapper, Path dataFolder, RuntimeConfig config) {
        this.mapper = mapper;
        this.enabled = config.persistence().playerState().enabled();
        this.directory = dataFolder.resolve(config.persistence().playerState().directory());
        if (enabled) {
            try {
                Files.createDirectories(directory);
            } catch (IOException ex) {
                throw new ConfigException("Failed to create player state directory: " + directory, ex);
            }
        }
    }

    public PlayerStoryState load(UUID playerId) {
        PlayerStoryState state = cache.get(playerId);
        if (state != null) {
            return state;
        }
        if (!enabled) {
            PlayerStoryState fresh = new PlayerStoryState();
            cache.put(playerId, fresh);
            return fresh;
        }

        Path path = pathFor(playerId);
        if (!Files.exists(path)) {
            PlayerStoryState fresh = new PlayerStoryState();
            cache.put(playerId, fresh);
            return fresh;
        }

        try {
            PlayerStoryState loaded = mapper.readValue(path.toFile(), PlayerStoryState.class);
            loaded.normalize();
            cache.put(playerId, loaded);
            return loaded;
        } catch (IOException ex) {
            throw new ConfigException("Failed to read player state: " + path, ex);
        }
    }

    public void save(UUID playerId) {
        if (!enabled) {
            return;
        }
        PlayerStoryState state = cache.get(playerId);
        if (state == null) {
            return;
        }
        state.normalize();
        Path path = pathFor(playerId);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), state);
        } catch (IOException ex) {
            throw new ConfigException("Failed to write player state: " + path, ex);
        }
    }

    public void saveAll() {
        if (!enabled) {
            return;
        }
        for (UUID playerId : cache.keySet()) {
            save(playerId);
        }
    }

    public NpcProgressState getNpcState(UUID playerId, String npcId) {
        PlayerStoryState state = load(playerId);
        return state.npcs.computeIfAbsent(npcId, ignored -> new NpcProgressState());
    }

    public QuestProgressState getQuestState(UUID playerId, String questId) {
        PlayerStoryState state = load(playerId);
        return state.quests.computeIfAbsent(questId, ignored -> new QuestProgressState());
    }

    private Path pathFor(UUID playerId) {
        return directory.resolve(playerId + ".json");
    }

    public static class PlayerStoryState {
        public Map<String, NpcProgressState> npcs = new HashMap<>();
        public Map<String, QuestProgressState> quests = new HashMap<>();

        public void normalize() {
            if (npcs == null) {
                npcs = new HashMap<>();
            }
            if (quests == null) {
                quests = new HashMap<>();
            }
        }
    }

    public static class NpcProgressState {
        public int affinity = 0;
        public String lastSavedNodeId = "";
        public String activeQuestId = "";
    }

    public static class QuestProgressState {
        public boolean accepted = false;
        public boolean completed = false;
    }
}
