package dev.pcrykh.listeners;

import dev.pcrykh.gui.GuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class MenuListener implements Listener {
    private final GuiService guiService;

    public MenuListener(GuiService guiService) {
        this.guiService = guiService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!guiService.isManagedTitle(title)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();
        if (GuiService.MENU_TITLE.equals(title)) {
            handleMenuClick(player, slot);
            return;
        }

        if (GuiService.ACHIEVEMENTS_TITLE.equals(title)) {
            handleAchievementsClick(player, slot);
            return;
        }

        if (GuiService.QUESTS_TITLE.equals(title)) {
            handleQuestsClick(player, slot);
            return;
        }

        if (GuiService.PROFILE_TITLE.equals(title) || GuiService.SETTINGS_TITLE.equals(title)) {
            if (slot == 45) {
                guiService.openMainMenu(player);
            }
        }
    }

    private void handleMenuClick(Player player, int slot) {
        switch (slot) {
            case 20 -> guiService.openProfile(player);
            case 22 -> guiService.openAchievements(player, 0);
            case 24 -> guiService.openQuests(player, 0);
            case 26 -> guiService.openSettings(player);
            default -> {
            }
        }
    }

    private void handleAchievementsClick(Player player, int slot) {
        int page = guiService.achievementPage(player);
        switch (slot) {
            case 45 -> guiService.openMainMenu(player);
            case 47 -> guiService.openAchievements(player, page - 1);
            case 53 -> guiService.openAchievements(player, page + 1);
            default -> {
            }
        }
    }

    private void handleQuestsClick(Player player, int slot) {
        int page = guiService.questPage(player);
        switch (slot) {
            case 45 -> guiService.openMainMenu(player);
            case 47 -> guiService.openQuests(player, page - 1);
            case 53 -> guiService.openQuests(player, page + 1);
            default -> {
            }
        }
    }
}
