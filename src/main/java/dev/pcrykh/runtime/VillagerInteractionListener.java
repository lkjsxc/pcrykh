package dev.pcrykh.runtime;

import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class VillagerInteractionListener implements Listener {
    private final VillagerConversationService conversationService;

    public VillagerInteractionListener(VillagerConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (conversationService.handleInteraction(event.getPlayer(), villager)) {
            event.setCancelled(true);
        }
    }
}
