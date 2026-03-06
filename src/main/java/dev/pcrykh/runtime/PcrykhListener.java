package dev.pcrykh.runtime;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class PcrykhListener implements Listener {
    private final JavaPlugin plugin;
    private final RuntimeBootstrap bootstrap;

    public PcrykhListener(JavaPlugin plugin, RuntimeBootstrap bootstrap) {
        this.plugin = plugin;
        this.bootstrap = bootstrap;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        scheduleBeacon(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        scheduleBeacon(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        scheduleBeacon(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || !this.bootstrap.isManagedBeacon(event.getItem())) {
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK
                || event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            this.bootstrap.openMenuFromBeacon(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!this.bootstrap.isManagedBeacon(event.getItemDrop().getItemStack())) {
            return;
        }
        event.setCancelled(true);
        scheduleBeacon(event.getPlayer());
        this.bootstrap.openMenuFromBeacon(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (this.bootstrap.handleVillagerInteract(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVillagerInteractAt(PlayerInteractAtEntityEvent event) {
        if (this.bootstrap.handleVillagerInteract(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof PcrykhView view && event.getWhoClicked() instanceof Player player) {
            event.setCancelled(true);
            if (event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize()) {
                this.bootstrap.handleInventoryClick(player, view, event.getRawSlot());
            }
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (this.bootstrap.isManagedBeacon(current) || this.bootstrap.isManagedBeacon(cursor)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                scheduleBeacon(player);
                this.bootstrap.handleBeaconInventoryClick(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PcrykhView) {
            event.setCancelled(true);
            return;
        }
        if (this.bootstrap.isManagedBeacon(event.getOldCursor()) || event.getRawSlots().contains(8)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                scheduleBeacon(player);
            }
        }
    }

    private void scheduleBeacon(Player player) {
        Bukkit.getScheduler().runTask(this.plugin, () -> this.bootstrap.ensureHotbarBeacon(player));
    }
}