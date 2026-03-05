package dev.pcrykh.achievements;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class AchievementService {
    private final List<MovementAchievement> movementAchievements = List.of(
        new MovementAchievement("walk_i", "Walker I", "Walk 1000 blocks", MovementMode.WALK, 1000),
        new MovementAchievement("sprint_i", "Runner I", "Sprint 750 blocks", MovementMode.SPRINT, 750),
        new MovementAchievement("sneak_i", "Croucher I", "Crouch-walk 250 blocks", MovementMode.SNEAK, 250),
        new MovementAchievement("swim_i", "Swimmer I", "Swim 300 blocks", MovementMode.SWIM, 300),
        new MovementAchievement("jump_i", "Jumper I", "Jump 100 times", MovementMode.JUMP, 100),
        new MovementAchievement("ethereal_wing_i", "Winged I", "Glide 500 blocks with Elytra", MovementMode.ETHEREAL_WING, 500),
        new MovementAchievement("boat_i", "Navigator I", "Travel 500 blocks by boat", MovementMode.BOAT, 500)
    );

    private final Map<UUID, EnumMap<MovementMode, Double>> movementProgress = new ConcurrentHashMap<>();

    public List<MovementAchievement> getMovementAchievements() {
        return movementAchievements;
    }

    public void addDistance(Player player, MovementMode mode, double distance) {
        if (distance <= 0.0 || mode == MovementMode.JUMP) {
            return;
        }
        EnumMap<MovementMode, Double> progress = progressFor(player.getUniqueId());
        progress.merge(mode, distance, Double::sum);
    }

    public void addJumps(Player player, int jumps) {
        if (jumps <= 0) {
            return;
        }
        EnumMap<MovementMode, Double> progress = progressFor(player.getUniqueId());
        progress.merge(MovementMode.JUMP, (double) jumps, Double::sum);
    }

    public int getProgress(Player player, MovementMode mode) {
        EnumMap<MovementMode, Double> progress = progressFor(player.getUniqueId());
        return progress.getOrDefault(mode, 0.0).intValue();
    }

    public boolean isCompleted(Player player, MovementAchievement achievement) {
        return getProgress(player, achievement.mode()) >= achievement.target();
    }

    public int completedCount(Player player) {
        int completed = 0;
        for (MovementAchievement achievement : movementAchievements) {
            if (isCompleted(player, achievement)) {
                completed++;
            }
        }
        return completed;
    }

    private EnumMap<MovementMode, Double> progressFor(UUID playerId) {
        return movementProgress.computeIfAbsent(playerId, key -> {
            EnumMap<MovementMode, Double> map = new EnumMap<>(MovementMode.class);
            for (MovementMode mode : MovementMode.values()) {
                map.put(mode, 0.0);
            }
            return map;
        });
    }
}
