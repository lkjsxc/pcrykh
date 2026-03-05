package dev.pcrykh.gui;

import dev.pcrykh.achievements.AchievementService;
import dev.pcrykh.achievements.MovementAchievement;
import dev.pcrykh.achievements.MovementMode;
import dev.pcrykh.quest.QuestEntry;
import dev.pcrykh.quest.QuestService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuiService {
    public static final String MENU_TITLE = "Pcrykh";
    public static final String ACHIEVEMENTS_TITLE = "Achievements";
    public static final String QUESTS_TITLE = "Quests";
    public static final String PROFILE_TITLE = "Profile";
    public static final String SETTINGS_TITLE = "Settings";

    private static final int PAGE_SIZE = 45;

    private final AchievementService achievementService;
    private final QuestService questService;
    private final Map<UUID, Integer> achievementPageByPlayer = new HashMap<>();
    private final Map<UUID, Integer> questPageByPlayer = new HashMap<>();

    public GuiService(AchievementService achievementService, QuestService questService) {
        this.achievementService = achievementService;
        this.questService = questService;
    }

    public boolean isManagedTitle(String title) {
        return MENU_TITLE.equals(title)
            || ACHIEVEMENTS_TITLE.equals(title)
            || QUESTS_TITLE.equals(title)
            || PROFILE_TITLE.equals(title)
            || SETTINGS_TITLE.equals(title);
    }

    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, MENU_TITLE);
        inventory.setItem(20, item(Material.PLAYER_HEAD, "Profile", List.of("View your personal record"), false));
        inventory.setItem(22, item(Material.BOOK, "Achievements", List.of("Browse the catalog"), false));
        inventory.setItem(24, item(Material.MAP, "Quests", List.of("Track your active journeys"), false));
        inventory.setItem(26, item(Material.REDSTONE, "Settings", List.of("Configure notifications"), false));
        player.openInventory(inventory);
    }

    public void openAchievements(Player player, int page) {
        List<MovementAchievement> achievements = achievementService.getMovementAchievements();
        int totalPages = totalPages(achievements.size());
        int safePage = clampPage(page, totalPages);
        achievementPageByPlayer.put(player.getUniqueId(), safePage);

        Inventory inventory = Bukkit.createInventory(null, 54, ACHIEVEMENTS_TITLE);
        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, achievements.size());

        if (achievements.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER, "No achievements loaded.", List.of(), false));
        } else {
            for (int i = start; i < end; i++) {
                MovementAchievement achievement = achievements.get(i);
                int progress = achievementService.getProgress(player, achievement.mode());
                boolean complete = achievementService.isCompleted(player, achievement);
                List<String> lore = new ArrayList<>();
                lore.add(achievement.description());
                lore.add("mode: " + achievement.mode().token());
                lore.add("progress: " + progress + "/" + achievement.target());
                inventory.setItem(i - start, item(modeMaterial(achievement.mode()), achievement.title(), lore, complete));
            }
        }

        applyNavigation(inventory, safePage, totalPages);
        player.openInventory(inventory);
    }

    public void openQuests(Player player, int page) {
        List<QuestEntry> quests = questService.getQuestsFor(player);
        int totalPages = totalPages(quests.size());
        int safePage = clampPage(page, totalPages);
        questPageByPlayer.put(player.getUniqueId(), safePage);

        Inventory inventory = Bukkit.createInventory(null, 54, QUESTS_TITLE);
        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, quests.size());

        if (quests.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER, "No quests loaded.", List.of(), false));
        } else {
            for (int i = start; i < end; i++) {
                QuestEntry quest = quests.get(i);
                List<String> lore = new ArrayList<>();
                lore.add("id: " + quest.id());
                lore.add("npc: " + quest.npcId());
                lore.add("stage: " + quest.stage());
                lore.add("progress: " + quest.progress() + "/" + quest.target());
                lore.add("state: " + (quest.completed() ? "completed" : "accepted"));
                inventory.setItem(i - start, item(Material.MAP, quest.title(), lore, quest.completed()));
            }
        }

        applyNavigation(inventory, safePage, totalPages);
        player.openInventory(inventory);
    }

    public void openProfile(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, PROFILE_TITLE);

        inventory.setItem(20, item(Material.PLAYER_HEAD, "You", List.of(
            "name: " + player.getName(),
            "uuid: " + player.getUniqueId()
        ), false));

        int completed = achievementService.completedCount(player);
        int total = achievementService.getMovementAchievements().size();
        int percent = total == 0 ? 0 : (completed * 100) / total;
        inventory.setItem(24, item(Material.PAPER, "Progress", List.of(
            "completed: " + completed + "/" + total,
            "ap: " + completed,
            "completion: " + percent + "%"
        ), false));

        inventory.setItem(45, item(Material.BARRIER, "Back", List.of(), false));
        player.openInventory(inventory);
    }

    public void openSettings(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, SETTINGS_TITLE);
        inventory.setItem(20, item(Material.NAME_TAG, "Achievement Broadcasts", List.of(
            "global chat announcements",
            "state: on"
        ), false));
        inventory.setItem(22, item(Material.BOOK, "Random Facts", List.of(
            "periodic global facts",
            "state: off"
        ), false));
        inventory.setItem(24, item(Material.GLOWSTONE_DUST, "Progress Indicators", List.of(
            "action bar progress updates",
            "state: on"
        ), false));
        inventory.setItem(45, item(Material.BARRIER, "Back", List.of(), false));
        player.openInventory(inventory);
    }

    public int achievementPage(Player player) {
        return achievementPageByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public int questPage(Player player) {
        return questPageByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    private void applyNavigation(Inventory inventory, int page, int totalPages) {
        inventory.setItem(45, item(Material.BARRIER, "Back", List.of(), false));
        if (page > 0) {
            inventory.setItem(47, item(Material.ARROW, "Previous", List.of(), false));
        }
        inventory.setItem(49, item(Material.PAPER, "Page", List.of((page + 1) + "/" + totalPages), false));
        if (page < totalPages - 1) {
            inventory.setItem(53, item(Material.ARROW, "Next", List.of(), false));
        }
    }

    private int totalPages(int count) {
        return Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private int clampPage(int page, int totalPages) {
        if (page < 0) {
            return 0;
        }
        if (page >= totalPages) {
            return totalPages - 1;
        }
        return page;
    }

    private Material modeMaterial(MovementMode mode) {
        return switch (mode) {
            case WALK -> Material.LEATHER_BOOTS;
            case SPRINT -> Material.FEATHER;
            case SNEAK -> Material.COBBLED_DEEPSLATE;
            case SWIM -> Material.PRISMARINE_CRYSTALS;
            case JUMP -> Material.SLIME_BLOCK;
            case ETHEREAL_WING -> Material.ELYTRA;
            case BOAT -> Material.OAK_BOAT;
        };
    }

    private ItemStack item(Material material, String name, List<String> lore, boolean glow) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + name);
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ChatColor.GRAY + line);
        }
        if (!coloredLore.isEmpty()) {
            meta.setLore(coloredLore);
        }
        if (glow) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
