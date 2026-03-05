package dev.pcrykh.runtime;

import dev.pcrykh.domain.QuestDefinition;
import org.bukkit.entity.Player;

import java.util.UUID;

public class QuestService {
    private final QuestCatalog questCatalog;
    private final PlayerStateStore stateStore;

    public QuestService(QuestCatalog questCatalog, PlayerStateStore stateStore) {
        this.questCatalog = questCatalog;
        this.stateStore = stateStore;
    }

    public boolean acceptQuest(Player player, String questId) {
        QuestDefinition quest = questCatalog.get(questId);
        if (quest == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        PlayerStateStore.QuestProgressState questState = stateStore.getQuestState(playerId, questId);
        if (questState.completed || questState.accepted) {
            return false;
        }

        questState.accepted = true;

        PlayerStateStore.NpcProgressState npcState = stateStore.getNpcState(playerId, quest.npcId());
        npcState.activeQuestId = questId;
        return true;
    }

    public QuestDefinition getQuest(String questId) {
        return questCatalog.get(questId);
    }
}
