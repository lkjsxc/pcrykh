package dev.pcrykh.runtime;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class PcrykhView implements InventoryHolder {
    private final Screen screen;
    private final int page;
    private final String title;
    private Inventory inventory;

    public PcrykhView(Screen screen, int page, String title) {
        this.screen = screen;
        this.page = page;
        this.title = title;
    }

    public Inventory createInventory() {
        this.inventory = Bukkit.createInventory(this, 54, this.title);
        return this.inventory;
    }

    public Screen screen() {
        return this.screen;
    }

    public int page() {
        return this.page;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public enum Screen {
        MAIN,
        PROFILE,
        ACHIEVEMENTS,
        QUESTS,
        SETTINGS
    }
}