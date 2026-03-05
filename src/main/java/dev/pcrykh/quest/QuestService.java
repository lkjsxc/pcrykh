package dev.pcrykh.quest;

import java.util.List;
import org.bukkit.entity.Player;

public final class QuestService {
    private static final List<QuestEntry> DEFAULT_QUESTS = List.of(
        new QuestEntry("shoreline_scout", "Shoreline Scout", "harbormaster", "distance", 140, 300, false),
        new QuestEntry("moonlit_dispatch", "Moonlit Dispatch", "librarian", "complete", 1, 1, true)
    );

    public List<QuestEntry> getQuestsFor(Player player) {
        return DEFAULT_QUESTS;
    }
}
