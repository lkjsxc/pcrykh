package dev.pcrykh.runtime;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerStateLifecycleListener implements Listener {
    private final PlayerStateStore stateStore;
    private final VillagerConversationService conversationService;

    public PlayerStateLifecycleListener(PlayerStateStore stateStore, VillagerConversationService conversationService) {
        this.stateStore = stateStore;
        this.conversationService = conversationService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        stateStore.load(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        conversationService.endSession(event.getPlayer(), true);
        stateStore.save(event.getPlayer().getUniqueId());
    }
}
