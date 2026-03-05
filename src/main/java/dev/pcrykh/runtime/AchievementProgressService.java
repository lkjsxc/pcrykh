package dev.pcrykh.runtime;

import dev.pcrykh.domain.AchievementDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

public class AchievementProgressService {
    private final Plugin plugin;
    private final AchievementCatalog catalog;
    private final RuntimeConfig config;

    private final Map<String, CriteriaSpec> criteriaById = new HashMap<>();
    private final Map<String, List<AchievementDefinition>> blockBreakIndex = new HashMap<>();
    private final Map<String, List<AchievementDefinition>> itemCraftIndex = new HashMap<>();
    private final Map<String, List<AchievementDefinition>> entityKillIndex = new HashMap<>();
    private final Map<String, List<AchievementDefinition>> fishCatchIndex = new HashMap<>();
    private final Map<String, List<AchievementDefinition>> itemEnchantIndex = new HashMap<>();
    private final Map<String, List<AchievementDefinition>> movementModeIndex = new HashMap<>();

    private final Map<UUID, Map<String, Integer>> progress = new HashMap<>();
    private final Map<UUID, Set<String>> unlocked = new HashMap<>();

    private final Map<UUID, PriorityQueue<ProgressUpdate>> actionBarQueues = new HashMap<>();
    private final Map<UUID, Map<String, ProgressUpdate>> queuedByAchievement = new HashMap<>();
    private final Map<UUID, Long> lastActionBarAt = new HashMap<>();

    public AchievementProgressService(Plugin plugin, AchievementCatalog catalog, RuntimeConfig config) {
        this.plugin = plugin;
        this.catalog = catalog;
        this.config = config;
        indexAchievements();
        startActionBarDispatcher();
    }

    public int getProgress(Player player, AchievementDefinition achievement) {
        return progress
                .getOrDefault(player.getUniqueId(), Map.of())
                .getOrDefault(achievement.id(), 0);
    }

    public int getTarget(AchievementDefinition achievement) {
        CriteriaSpec spec = criteriaById.get(achievement.id());
        return spec == null ? 0 : spec.count();
    }

    public int getAp(AchievementDefinition achievement) {
        return achievement.rewards().get("ap").asInt();
    }

    public int getCompletedCount(Player player) {
        return unlocked.getOrDefault(player.getUniqueId(), Set.of()).size();
    }

    public boolean isCompleted(Player player, AchievementDefinition achievement) {
        return unlocked
                .getOrDefault(player.getUniqueId(), Set.of())
                .contains(achievement.id());
    }

    public int getTotalAp(Player player) {
        int total = 0;
        Set<String> completed = unlocked.getOrDefault(player.getUniqueId(), Set.of());
        for (AchievementDefinition achievement : catalog.achievements()) {
            if (completed.contains(achievement.id())) {
                total += getAp(achievement);
            }
        }
        return total;
    }

    public void onBlockBreak(Player player, Material material) {
        String key = normalizeMaterial(material);
        handleProgress(player, blockBreakIndex.getOrDefault(key, List.of()), 1);
    }

    public void onItemCraft(Player player, Material material) {
        String key = normalizeMaterial(material);
        handleProgress(player, itemCraftIndex.getOrDefault(key, List.of()), 1);
    }

    public void onEntityKill(Player player, EntityType type) {
        String key = normalizeEntity(type);
        handleProgress(player, entityKillIndex.getOrDefault(key, List.of()), 1);
    }

    public void onFishCatch(Player player, Material material) {
        String key = normalizeMaterial(material);
        handleProgress(player, fishCatchIndex.getOrDefault(key, List.of()), 1);
    }

    public void onItemEnchant(Player player, Material material) {
        String key = normalizeMaterial(material);
        handleProgress(player, itemEnchantIndex.getOrDefault(key, List.of()), 1);
    }

    public void onMovement(Player player, String mode, int amount) {
        if (mode == null || mode.isBlank()) {
            return;
        }
        if (amount <= 0) {
            return;
        }
        String key = mode.toLowerCase();
        handleProgress(player, movementModeIndex.getOrDefault(key, List.of()), amount);
    }

    private void handleProgress(Player player, List<AchievementDefinition> achievements, int increment) {
        if (achievements.isEmpty()) {
            return;
        }
        if (increment <= 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        Map<String, Integer> playerProgress = progress.computeIfAbsent(playerId, ignored -> new HashMap<>());
        Set<String> playerUnlocked = unlocked.computeIfAbsent(playerId, ignored -> new HashSet<>());

        for (AchievementDefinition achievement : achievements) {
            CriteriaSpec spec = criteriaById.get(achievement.id());
            if (spec == null) {
                continue;
            }
            int current = playerProgress.getOrDefault(achievement.id(), 0);
            int next = Math.min(spec.count(), current + increment);
            if (next == current) {
                continue;
            }
            playerProgress.put(achievement.id(), next);

            if (next >= spec.count() && !playerUnlocked.contains(achievement.id())) {
                playerUnlocked.add(achievement.id());
                broadcastUnlock(player, achievement);
            }

            notifyProgress(player, achievement, next, spec.count(), increment);
        }
    }

    private void broadcastUnlock(Player player, AchievementDefinition achievement) {
        if (!config.chat().announceAchievements()) {
            return;
        }
        String message = config.chat().prefix() + player.getName() + " unlocked " + achievement.title();
        Bukkit.getServer().broadcast(Component.text(message));
    }

    private void notifyProgress(Player player, AchievementDefinition achievement, int current, int target, int increment) {
        if (!config.actionBar().progressEnabled()) {
            return;
        }
        if (target <= 0) {
            return;
        }

        if (!config.actionBar().priority().enabled()) {
            sendActionBar(player, achievement, current, target);
            return;
        }

        ProgressUpdate update = new ProgressUpdate(achievement, current, target, increment);
        UUID playerId = player.getUniqueId();
        Map<String, ProgressUpdate> latest = queuedByAchievement.computeIfAbsent(playerId, ignored -> new HashMap<>());

        if (!config.actionBar().priority().preemptOnHigherPriority() && latest.containsKey(achievement.id())) {
            return;
        }

        latest.put(achievement.id(), update);
        actionBarQueues
                .computeIfAbsent(playerId, ignored -> new PriorityQueue<>(progressComparator()))
                .offer(update);
    }

    private void sendActionBar(Player player, AchievementDefinition achievement, int current, int target) {
        Component meter = buildProgressBar(current, target, 16);
        Component message = Component.text(achievement.title(), NamedTextColor.AQUA)
                .append(Component.space())
                .append(meter)
                .append(Component.space())
                .append(Component.text(current + "/" + target, NamedTextColor.GRAY));
        player.sendActionBar(message);
    }

    private void startActionBarDispatcher() {
        if (!config.actionBar().progressEnabled()) {
            return;
        }
        if (!config.actionBar().priority().enabled()) {
            return;
        }
        int intervalTicks = Math.max(1, config.actionBar().priority().displayIntervalTicks());
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::dispatchQueuedProgress, intervalTicks, intervalTicks);
    }

    private void dispatchQueuedProgress() {
        long now = System.currentTimeMillis();
        long cooldownMillis = config.actionBar().priority().cooldownTicks() * 50L;

        for (UUID playerId : new ArrayList<>(actionBarQueues.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                actionBarQueues.remove(playerId);
                queuedByAchievement.remove(playerId);
                lastActionBarAt.remove(playerId);
                continue;
            }

            long lastAt = lastActionBarAt.getOrDefault(playerId, 0L);
            if (cooldownMillis > 0 && now - lastAt < cooldownMillis) {
                continue;
            }

            PriorityQueue<ProgressUpdate> queue = actionBarQueues.get(playerId);
            Map<String, ProgressUpdate> latest = queuedByAchievement.get(playerId);
            if (queue == null || latest == null || queue.isEmpty() || latest.isEmpty()) {
                continue;
            }

            ProgressUpdate next = null;
            while (!queue.isEmpty()) {
                ProgressUpdate candidate = queue.poll();
                ProgressUpdate current = latest.get(candidate.achievement().id());
                if (candidate == current) {
                    next = candidate;
                    latest.remove(candidate.achievement().id());
                    break;
                }
            }

            if (next == null) {
                continue;
            }

            sendActionBar(player, next.achievement(), next.current(), next.target());
            lastActionBarAt.put(playerId, now);
        }
    }

    private Comparator<ProgressUpdate> progressComparator() {
        return Comparator
                .comparingInt(ProgressUpdate::remaining)
                .thenComparing((ProgressUpdate update) -> -update.increment())
                .thenComparing(update -> update.achievement().id());
    }

    private Component buildProgressBar(int current, int target, int width) {
        if (width <= 0 || target <= 0) {
            return Component.text("", NamedTextColor.DARK_GRAY);
        }
        double ratio = Math.min(1.0, Math.max(0.0, current / (double) target));
        int filled = (int) Math.floor(ratio * width);
        NamedTextColor filledColor = progressColor(ratio);
        Component bar = Component.empty();
        for (int i = 0; i < width; i++) {
            if (i < filled) {
                bar = bar.append(Component.text("█", filledColor));
            } else {
                bar = bar.append(Component.text("░", NamedTextColor.DARK_GRAY));
            }
        }
        return bar;
    }

    private NamedTextColor progressColor(double ratio) {
        if (ratio < 0.25) {
            return NamedTextColor.RED;
        }
        if (ratio < 0.50) {
            return NamedTextColor.GOLD;
        }
        if (ratio < 0.75) {
            return NamedTextColor.YELLOW;
        }
        return NamedTextColor.GREEN;
    }

    private void indexAchievements() {
        for (AchievementDefinition achievement : catalog.achievements()) {
            CriteriaSpec spec = CriteriaSpec.from(achievement.criteria());
            criteriaById.put(achievement.id(), spec);
            switch (spec.type()) {
                case "block_break" -> indexList(blockBreakIndex, spec.materials(), achievement);
                case "item_craft" -> indexSingle(itemCraftIndex, spec.item(), achievement);
                case "entity_kill" -> indexList(entityKillIndex, spec.entities(), achievement);
                case "fish_catch" -> indexList(fishCatchIndex, spec.items(), achievement);
                case "item_enchant" -> indexList(itemEnchantIndex, spec.items(), achievement);
                case "movement" -> indexList(movementModeIndex, spec.movementModes(), achievement);
                default -> {
                }
            }
        }
    }

    private void indexList(Map<String, List<AchievementDefinition>> index, Set<String> keys, AchievementDefinition achievement) {
        for (String key : keys) {
            index.computeIfAbsent(key, ignored -> new ArrayList<>()).add(achievement);
        }
    }

    private void indexSingle(Map<String, List<AchievementDefinition>> index, String key, AchievementDefinition achievement) {
        if (key == null || key.isBlank()) {
            return;
        }
        index.computeIfAbsent(key, ignored -> new ArrayList<>()).add(achievement);
    }

    private String normalizeMaterial(Material material) {
        return material.name().toLowerCase();
    }

    private String normalizeEntity(EntityType type) {
        return type.name().toLowerCase();
    }

    private record ProgressUpdate(AchievementDefinition achievement, int current, int target, int increment) {
        private int remaining() {
            return Math.max(0, target - current);
        }
    }
}
