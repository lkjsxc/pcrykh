package dev.pcrykh.runtime;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AchievementProgressListener implements Listener {
    private final AchievementProgressService progressService;
    private final Map<UUID, Long> lastJumpAt = new HashMap<>();
    private final Map<UUID, Map<String, Double>> movementRemainders = new HashMap<>();

    public AchievementProgressListener(AchievementProgressService progressService) {
        this.progressService = progressService;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        progressService.onBlockBreak(event.getPlayer(), event.getBlock().getType());
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }
        progressService.onItemCraft(player, result.getType());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity killer = event.getEntity().getKiller();
        if (!(killer instanceof Player player)) {
            return;
        }
        EntityType type = event.getEntityType();
        progressService.onEntityKill(player, type);
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Entity caught = event.getCaught();
        if (!(caught instanceof org.bukkit.entity.Item itemEntity)) {
            return;
        }
        ItemStack item = itemEntity.getItemStack();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        progressService.onFishCatch(player, item.getType());
    }

    @EventHandler
    public void onItemEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        progressService.onItemEnchant(player, item.getType());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }

        Player player = event.getPlayer();
        String mode = resolveMovementMode(player, event);
        if ("jump".equals(mode)) {
            progressService.onMovement(player, mode, 1);
            return;
        }

        double distance = event.getFrom().toVector().distance(event.getTo().toVector());
        if (distance <= 0) {
            return;
        }

        Map<String, Double> byMode = movementRemainders.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        double total = byMode.getOrDefault(mode, 0.0) + distance;
        int delta = (int) Math.floor(total);
        byMode.put(mode, total - delta);
        if (delta > 0) {
            progressService.onMovement(player, mode, delta);
        }
    }

    private String resolveMovementMode(Player player, PlayerMoveEvent event) {
        if (isBoatTravel(player)) {
            return "boat";
        }
        if (isEtherealWing(player)) {
            return "ethereal_wing";
        }
        if (isJump(player, event)) {
            return "jump";
        }
        if (player.isSneaking()) {
            return "sneak";
        }
        if (player.isSprinting()) {
            return "sprint";
        }
        return "walk";
    }

    private boolean isBoatTravel(Player player) {
        if (!player.isInsideVehicle()) {
            return false;
        }
        if (player.getVehicle() == null) {
            return false;
        }
        String type = player.getVehicle().getType().name();
        return type.contains("BOAT") || type.endsWith("_BOAT");
    }

    private boolean isEtherealWing(Player player) {
        if (!player.isGliding()) {
            return false;
        }
        ItemStack chest = player.getInventory().getChestplate();
        return chest != null && chest.getType() == Material.ELYTRA;
    }

    private boolean isJump(Player player, PlayerMoveEvent event) {
        if (player.isFlying() || player.isGliding() || player.isInsideVehicle()) {
            return false;
        }
        if (event.getTo().getY() <= event.getFrom().getY()) {
            return false;
        }
        long now = System.currentTimeMillis();
        long last = lastJumpAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 250) {
            return false;
        }
        lastJumpAt.put(player.getUniqueId(), now);
        return true;
    }
}
