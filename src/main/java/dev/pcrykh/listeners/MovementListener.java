package dev.pcrykh.listeners;

import dev.pcrykh.achievements.AchievementService;
import dev.pcrykh.achievements.MovementMode;
import org.bukkit.Statistic;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class MovementListener implements Listener {
    private final AchievementService achievementService;

    public MovementListener(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getWorld() != event.getTo().getWorld()) {
            return;
        }

        double distance = event.getFrom().distance(event.getTo());
        if (distance <= 0.0 || distance > 10.0) {
            return;
        }

        Player player = event.getPlayer();
        MovementMode mode = modeFor(player);
        achievementService.addDistance(player, mode, distance);
    }

    @EventHandler(ignoreCancelled = true)
    public void onJump(PlayerStatisticIncrementEvent event) {
        if (event.getStatistic() != Statistic.JUMP) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        int delta = event.getNewValue() - event.getPreviousValue();
        achievementService.addJumps(player, delta);
    }

    private MovementMode modeFor(Player player) {
        if (player.getVehicle() instanceof Boat) {
            return MovementMode.BOAT;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack chestplate = inventory.getChestplate();
        if (player.isGliding() && chestplate != null && chestplate.getType().name().equals("ELYTRA")) {
            return MovementMode.ETHEREAL_WING;
        }

        if (player.isSwimming()) {
            return MovementMode.SWIM;
        }

        if (player.isSneaking()) {
            return MovementMode.SNEAK;
        }

        if (player.isSprinting()) {
            return MovementMode.SPRINT;
        }

        return MovementMode.WALK;
    }
}
