package dev.pcrykh.runtime;

import dev.pcrykh.domain.QuestDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestCatalog {
    private final List<QuestDefinition> quests;
    private final Map<String, QuestDefinition> byId = new HashMap<>();

    public QuestCatalog(List<QuestDefinition> quests, NpcCatalog npcCatalog) {
        this.quests = List.copyOf(quests);
        for (QuestDefinition quest : quests) {
            if (quest.id() == null || quest.id().isBlank()) {
                throw new ConfigException("quest id must be non-empty");
            }
            if (quest.npcId() == null || quest.npcId().isBlank()) {
                throw new ConfigException("quest npc_id must be non-empty: " + quest.id());
            }
            if (npcCatalog.get(quest.npcId()) == null) {
                throw new ConfigException("quest references unknown npc_id: " + quest.npcId());
            }
            if (byId.putIfAbsent(quest.id(), quest) != null) {
                throw new ConfigException("Duplicate quest id: " + quest.id());
            }
        }
    }

    public List<QuestDefinition> quests() {
        return Collections.unmodifiableList(quests);
    }

    public QuestDefinition get(String id) {
        return byId.get(id);
    }
}
