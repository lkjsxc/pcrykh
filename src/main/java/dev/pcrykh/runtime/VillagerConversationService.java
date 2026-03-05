package dev.pcrykh.runtime;

import dev.pcrykh.domain.DialogueNode;
import dev.pcrykh.domain.NpcDefinition;
import dev.pcrykh.domain.QuestDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VillagerConversationService {
    private static final String NPC_TAG_PREFIX = "pcrykh_npc_";

    private final Plugin plugin;
    private final RuntimeConfig config;
    private final NpcCatalog npcCatalog;
    private final QuestService questService;
    private final PlayerStateStore stateStore;

    private final Map<String, Map<String, DialogueNode>> dialogueIndexByNpc = new HashMap<>();
    private final Map<UUID, String> npcIdByEntityId = new HashMap<>();
    private final Map<String, UUID> entityIdByNpcId = new HashMap<>();
    private final Map<UUID, ConversationSession> sessionByPlayer = new HashMap<>();
    private final Map<String, Integer> activeSessionsByNpc = new HashMap<>();

    public VillagerConversationService(
            Plugin plugin,
            RuntimeConfig config,
            NpcCatalog npcCatalog,
            QuestService questService,
            PlayerStateStore stateStore
    ) {
        this.plugin = plugin;
        this.config = config;
        this.npcCatalog = npcCatalog;
        this.questService = questService;
        this.stateStore = stateStore;
        buildDialogueIndex();
        initializeVillagers();
        startTimeoutTask();
    }

    public boolean handleInteraction(Player player, Villager villager) {
        String npcId = npcIdByEntityId.get(villager.getUniqueId());
        if (npcId == null) {
            return false;
        }
        if (villager.getProfession() != Villager.Profession.NONE) {
            return false;
        }

        ConversationSession active = sessionByPlayer.get(player.getUniqueId());
        if (active == null || !active.npcId.equals(npcId)) {
            if (active != null) {
                endSession(player, false);
            }
            startSession(player, npcId);
            return true;
        }

        active.lastInteractionAt = System.currentTimeMillis();
        advanceSession(player, active);
        return true;
    }

    public void endSession(Player player, boolean interrupted) {
        ConversationSession session = sessionByPlayer.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        if (!interrupted && session.pendingAffinityDelta != 0) {
            commitCheckpoint(player, session, session.currentNodeId);
        }

        releaseNpcLock(session.npcId);
        if (interrupted) {
            player.sendMessage(Component.text("Conversation interrupted. State restored to latest checkpoint.", NamedTextColor.RED));
        }
    }

    private void startSession(Player player, String npcId) {
        NpcDefinition npc = npcCatalog.get(npcId);
        if (npc == null) {
            return;
        }

        PlayerStateStore.NpcProgressState npcState = stateStore.getNpcState(player.getUniqueId(), npcId);
        npcState.dialogueVisits++;
        String startNodeId = npcState.lastSavedNodeId == null || npcState.lastSavedNodeId.isBlank()
                ? npc.startNodeId()
                : npcState.lastSavedNodeId;

        ConversationSession session = new ConversationSession(
            npcId,
            startNodeId,
            npcState.affinity,
            0,
            System.currentTimeMillis()
        );
        sessionByPlayer.put(player.getUniqueId(), session);
        activeSessionsByNpc.merge(npcId, 1, Integer::sum);
        applyNpcFreeze(npcId, true);
        renderNode(player, session);
    }

    private void advanceSession(Player player, ConversationSession session) {
        DialogueNode current = resolveNode(session.npcId, session.currentNodeId);
        if (current == null) {
            endSession(player, true);
            return;
        }

        if (current.nextNodeId() == null || current.nextNodeId().isBlank()) {
            endSession(player, false);
            return;
        }

        session.currentNodeId = current.nextNodeId();
        renderNode(player, session);
    }

    private void renderNode(Player player, ConversationSession session) {
        DialogueNode node = resolveNode(session.npcId, session.currentNodeId);
        if (node == null) {
            endSession(player, true);
            return;
        }

        if (node.affinityDelta() != 0) {
            session.pendingAffinityDelta += node.affinityDelta();
        }
        if (node.saveCheckpoint()) {
            commitCheckpoint(player, session, node.id());
        }

        player.sendMessage(Component.text(node.text(), NamedTextColor.YELLOW));

        if (node.acceptQuest()) {
            NpcDefinition npc = npcCatalog.get(session.npcId);
            commitCheckpoint(player, session, node.id());
            boolean accepted = npc != null && questService.acceptQuest(player, npc.questId());
            if (npc != null) {
                QuestDefinition quest = questService.getQuest(npc.questId());
                String questTitle = quest == null ? npc.questId() : quest.title();
                if (accepted) {
                    player.sendMessage(Component.text("Quest accepted: " + questTitle, NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Quest already accepted: " + questTitle, NamedTextColor.GRAY));
                }
            }
            endSession(player, false);
            return;
        }

        if (node.nextNodeId() == null || node.nextNodeId().isBlank()) {
            commitCheckpoint(player, session, node.id());
            endSession(player, false);
        }
    }

    private void commitCheckpoint(Player player, ConversationSession session, String checkpointNodeId) {
        PlayerStateStore.NpcProgressState npcState = stateStore.getNpcState(player.getUniqueId(), session.npcId);
        int nextAffinity = clampAffinity(session.checkpointAffinity + session.pendingAffinityDelta);
        npcState.affinity = nextAffinity;
        npcState.lastSavedNodeId = checkpointNodeId;

        session.checkpointAffinity = nextAffinity;
        session.pendingAffinityDelta = 0;
    }

    private DialogueNode resolveNode(String npcId, String nodeId) {
        Map<String, DialogueNode> index = dialogueIndexByNpc.get(npcId);
        if (index == null) {
            return null;
        }
        return index.get(nodeId);
    }

    private int clampAffinity(int affinity) {
        if (affinity < -100) {
            return -100;
        }
        if (affinity > 100) {
            return 100;
        }
        return affinity;
    }

    private void buildDialogueIndex() {
        for (NpcDefinition npc : npcCatalog.npcs()) {
            Map<String, DialogueNode> index = new HashMap<>();
            for (DialogueNode node : npc.dialogueNodes()) {
                index.put(node.id(), node);
            }
            dialogueIndexByNpc.put(npc.id(), index);
        }
    }

    private void initializeVillagers() {
        for (NpcDefinition npc : npcCatalog.npcs()) {
            World world = Bukkit.getWorld(npc.world());
            if (world == null) {
                plugin.getLogger().warning("Skipping NPC with unknown world: " + npc.id());
                continue;
            }
            String tag = tagFor(npc.id());
            Villager villager = findVillagerByTag(world, tag);
            if (villager == null) {
                Location location = new Location(world, npc.x(), npc.y(), npc.z());
                villager = world.spawn(location, Villager.class);
            }
            configureVillager(villager, npc, tag);
            npcIdByEntityId.put(villager.getUniqueId(), npc.id());
            entityIdByNpcId.put(npc.id(), villager.getUniqueId());
        }
    }

    private Villager findVillagerByTag(World world, String tag) {
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (villager.getScoreboardTags().contains(tag)) {
                return villager;
            }
        }
        return null;
    }

    private void configureVillager(Villager villager, NpcDefinition npc, String tag) {
        villager.customName(Component.text(npc.displayName()));
        villager.setCustomNameVisible(true);
        villager.setProfession(resolveProfession(npc.profession()));
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.getScoreboardTags().add(tag);
        villager.setAI(true);
    }

    private Villager.Profession resolveProfession(String raw) {
        if (raw == null || raw.isBlank()) {
            return Villager.Profession.NONE;
        }
        try {
            return Villager.Profession.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Villager.Profession.NONE;
        }
    }

    private String tagFor(String npcId) {
        return NPC_TAG_PREFIX + npcId;
    }

    private void startTimeoutTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::handleTimeouts, 20L, 20L);
    }

    private void handleTimeouts() {
        long timeoutMillis = config.dialogue().timeoutSeconds() * 1000L;
        long now = System.currentTimeMillis();

        List<UUID> expiredPlayers = new ArrayList<>();
        for (Map.Entry<UUID, ConversationSession> entry : sessionByPlayer.entrySet()) {
            if (now - entry.getValue().lastInteractionAt >= timeoutMillis) {
                expiredPlayers.add(entry.getKey());
            }
        }

        for (UUID playerId : expiredPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                endSession(player, true);
            } else {
                ConversationSession session = sessionByPlayer.remove(playerId);
                if (session != null) {
                    releaseNpcLock(session.npcId);
                }
            }
        }
    }

    private void releaseNpcLock(String npcId) {
        Integer current = activeSessionsByNpc.get(npcId);
        if (current == null) {
            return;
        }
        if (current <= 1) {
            activeSessionsByNpc.remove(npcId);
            applyNpcFreeze(npcId, false);
            return;
        }
        activeSessionsByNpc.put(npcId, current - 1);
    }

    private void applyNpcFreeze(String npcId, boolean freeze) {
        if (!config.dialogue().freezeVillager()) {
            return;
        }
        UUID entityId = entityIdByNpcId.get(npcId);
        if (entityId == null) {
            return;
        }
        var entity = Bukkit.getEntity(entityId);
        if (!(entity instanceof Villager villager)) {
            return;
        }
        villager.setAI(!freeze);
    }

    private static class ConversationSession {
        private final String npcId;
        private String currentNodeId;
        private int checkpointAffinity;
        private int pendingAffinityDelta;
        private long lastInteractionAt;

        private ConversationSession(
                String npcId,
                String currentNodeId,
                int checkpointAffinity,
                int pendingAffinityDelta,
                long lastInteractionAt
        ) {
            this.npcId = npcId;
            this.currentNodeId = currentNodeId;
            this.checkpointAffinity = checkpointAffinity;
            this.pendingAffinityDelta = pendingAffinityDelta;
            this.lastInteractionAt = lastInteractionAt;
        }
    }
}
