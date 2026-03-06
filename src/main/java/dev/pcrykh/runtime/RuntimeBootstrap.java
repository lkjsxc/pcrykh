package dev.pcrykh.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.pcrykh.config.ConfigLoader;
import dev.pcrykh.config.ConfigLoader.AchievementDefinition;
import dev.pcrykh.config.ConfigLoader.LoadedRuntime;
import dev.pcrykh.config.ConfigLoader.QuestDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class RuntimeBootstrap {
    private static final int INVENTORY_SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final List<String> DEFAULT_RESOURCES = List.of(
            "config.json",
            "achievements/categories/starter.json",
            "achievements/entries/first_steps.json",
            "facts/packs/starter.json",
            "npcs/guide.json",
            "quests/first_story.json"
    );

    private final JavaPlugin plugin;
    private final List<BukkitTask> tasks;
    private final NamespacedKey beaconKey;
    private LoadedRuntime loadedRuntime;
    private BukkitTask factsTask;
    private boolean announceAchievementsEnabled;
    private boolean factsEnabled;
    private boolean progressIndicatorsEnabled;
    private String lastBroadcastFact;

    private RuntimeBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
        this.tasks = new ArrayList<>();
        this.beaconKey = new NamespacedKey(plugin, "menu_beacon");
    }

    public static RuntimeBootstrap start(JavaPlugin plugin) throws Exception {
        RuntimeBootstrap bootstrap = new RuntimeBootstrap(plugin);
        bootstrap.initialize();
        return bootstrap;
    }

    public void shutdown() {
        for (BukkitTask task : this.tasks) {
            task.cancel();
        }
        this.tasks.clear();
        this.factsTask = null;
    }

    public void openMainMenu(Player player) {
        PcrykhView view = new PcrykhView(PcrykhView.Screen.MAIN, 0, "Pcrykh");
        Inventory inventory = view.createInventory();
        inventory.setItem(20, button(Material.PLAYER_HEAD, "Profile", List.of("View your personal record")));
        inventory.setItem(22, button(Material.BOOK, "Achievements", List.of("Browse the catalog")));
        inventory.setItem(24, button(Material.MAP, "Quests", List.of("Track your active journeys")));
        inventory.setItem(26, button(Material.REDSTONE, "Settings", List.of("Configure notifications")));
        player.openInventory(inventory);
    }

    public void openProfileMenu(Player player) {
        PcrykhView view = new PcrykhView(PcrykhView.Screen.PROFILE, 0, "Profile");
        Inventory inventory = view.createInventory();
        inventory.setItem(20, playerIdentity(player));
        inventory.setItem(24, profileSummary());
        inventory.setItem(45, button(Material.BARRIER, "Back", List.of()));
        player.openInventory(inventory);
    }

    public void openAchievementsMenu(Player player, int page) {
        List<AchievementDefinition> achievements = this.loadedRuntime.achievements();
        int totalPages = pageCount(achievements.size());
        int safePage = clampPage(page, totalPages);

        PcrykhView view = new PcrykhView(PcrykhView.Screen.ACHIEVEMENTS, safePage, "Achievements");
        Inventory inventory = view.createInventory();
        if (achievements.isEmpty()) {
            inventory.setItem(22, button(Material.BARRIER, "No achievements loaded.", List.of()));
        } else {
            int startIndex = safePage * PAGE_SIZE;
            int endIndex = Math.min(startIndex + PAGE_SIZE, achievements.size());
            for (int index = startIndex; index < endIndex; index++) {
                AchievementDefinition achievement = achievements.get(index);
                inventory.setItem(index - startIndex, achievementItem(achievement));
            }
        }
        applyNavigation(inventory, safePage, totalPages);
        player.openInventory(inventory);
    }

    public void openQuestsMenu(Player player, int page) {
        List<QuestDefinition> quests = this.loadedRuntime.quests();
        int totalPages = pageCount(quests.size());
        int safePage = clampPage(page, totalPages);

        PcrykhView view = new PcrykhView(PcrykhView.Screen.QUESTS, safePage, "Quests");
        Inventory inventory = view.createInventory();
        if (quests.isEmpty()) {
            inventory.setItem(22, button(Material.BARRIER, "No quests loaded.", List.of()));
        } else {
            int startIndex = safePage * PAGE_SIZE;
            int endIndex = Math.min(startIndex + PAGE_SIZE, quests.size());
            for (int index = startIndex; index < endIndex; index++) {
                QuestDefinition quest = quests.get(index);
                inventory.setItem(index - startIndex, questItem(quest));
            }
        }
        applyNavigation(inventory, safePage, totalPages);
        player.openInventory(inventory);
    }

    public void openSettingsMenu(Player player) {
        PcrykhView view = new PcrykhView(PcrykhView.Screen.SETTINGS, 0, "Settings");
        Inventory inventory = view.createInventory();
        inventory.setItem(20, button(Material.NAME_TAG, "Achievement Broadcasts", List.of(
                "global chat announcements",
                "state: " + onOff(this.announceAchievementsEnabled)
        )));
        inventory.setItem(22, button(Material.BOOK, "Random Facts", List.of(
                "periodic global facts",
                "state: " + onOff(this.factsEnabled)
        )));
        inventory.setItem(24, button(Material.GLOWSTONE_DUST, "Progress Indicators", List.of(
                "action bar progress updates",
                "state: " + onOff(this.progressIndicatorsEnabled)
        )));
        inventory.setItem(45, button(Material.BARRIER, "Back", List.of()));
        player.openInventory(inventory);
    }

    public void handleInventoryClick(Player player, PcrykhView view, int slot) {
        switch (view.screen()) {
            case MAIN -> handleMainMenuClick(player, slot);
            case PROFILE -> handleBackOnly(player, slot);
            case ACHIEVEMENTS -> handlePagedClick(player, slot, view.page(), PcrykhView.Screen.ACHIEVEMENTS);
            case QUESTS -> handlePagedClick(player, slot, view.page(), PcrykhView.Screen.QUESTS);
            case SETTINGS -> handleSettingsClick(player, slot);
        }
    }

    public LoadedRuntime loadedRuntime() {
        return this.loadedRuntime;
    }

    public void ensureHotbarBeacon(Player player) {
        if (!player.hasPermission("pcrykh.use")) {
            ItemStack slotItem = player.getInventory().getItem(8);
            if (isManagedBeacon(slotItem)) {
                player.getInventory().setItem(8, null);
            }
            return;
        }
        player.getInventory().setItem(8, hotbarBeacon());
    }

    public boolean isManagedBeacon(ItemStack item) {
        if (item == null || item.getType() != Material.BEACON || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(this.beaconKey, PersistentDataType.STRING);
    }

    public void openMenuFromBeacon(Player player) {
        openMainMenu(player);
    }

    private void initialize() throws Exception {
        installDefaultResources();
        this.loadedRuntime = new ConfigLoader().load(this.plugin.getDataFolder().toPath());
        this.announceAchievementsEnabled = this.loadedRuntime.config().runtime().chat().announceAchievements();
        this.factsEnabled = this.loadedRuntime.config().runtime().chat().factsEnabled();
        this.progressIndicatorsEnabled = this.loadedRuntime.config().runtime().actionBar().progressEnabled();

        Files.createDirectories(this.plugin.getDataFolder().toPath()
                .resolve(this.loadedRuntime.config().runtime().persistence().playerState().directory())
                .normalize());

        scheduleFacts();
        scheduleBeaconGuard();
        Bukkit.getOnlinePlayers().forEach(this::ensureHotbarBeacon);

        this.plugin.getLogger().info("Loaded catalogs: "
                + this.loadedRuntime.summary().categories() + " categories, "
                + this.loadedRuntime.summary().achievements() + " achievements, "
                + this.loadedRuntime.summary().facts() + " facts, "
                + this.loadedRuntime.summary().npcs() + " npcs, "
                + this.loadedRuntime.summary().quests() + " quests.");
    }

    private void installDefaultResources() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }
        for (String resource : DEFAULT_RESOURCES) {
            if (!this.plugin.getDataFolder().toPath().resolve(resource).toFile().exists()) {
                this.plugin.saveResource(resource, false);
            }
        }
    }

    private void scheduleFacts() {
        if (this.factsTask != null) {
            this.factsTask.cancel();
            this.tasks.remove(this.factsTask);
            this.factsTask = null;
        }
        if (!this.factsEnabled || this.loadedRuntime.facts().isEmpty()) {
            return;
        }

        long intervalTicks = (long) this.loadedRuntime.config().runtime().chat().factsIntervalSeconds() * 20L;
        this.factsTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            String fact = nextFact();
            Bukkit.broadcast(Component.text(this.loadedRuntime.config().runtime().chat().prefix() + fact));
        }, intervalTicks, intervalTicks);
        this.tasks.add(this.factsTask);
    }

    private void scheduleBeaconGuard() {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> Bukkit.getOnlinePlayers().forEach(this::ensureHotbarBeacon), 1L, 100L);
        this.tasks.add(task);
    }

    private String nextFact() {
        List<String> facts = this.loadedRuntime.facts();
        if (facts.size() == 1) {
            this.lastBroadcastFact = facts.get(0);
            return this.lastBroadcastFact;
        }

        String next;
        do {
            next = facts.get(ThreadLocalRandom.current().nextInt(facts.size()));
        } while (Objects.equals(next, this.lastBroadcastFact));
        this.lastBroadcastFact = next;
        return next;
    }

    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 20 -> openProfileMenu(player);
            case 22 -> openAchievementsMenu(player, 0);
            case 24 -> openQuestsMenu(player, 0);
            case 26 -> openSettingsMenu(player);
            default -> {
            }
        }
    }

    private void handleBackOnly(Player player, int slot) {
        if (slot == 45) {
            openMainMenu(player);
        }
    }

    private void handlePagedClick(Player player, int slot, int page, PcrykhView.Screen screen) {
        if (slot == 45) {
            openMainMenu(player);
            return;
        }
        if (slot == 47 && page > 0) {
            openPagedScreen(player, screen, page - 1);
            return;
        }
        if (slot == 53 && page < pageCount(itemCount(screen)) - 1) {
            openPagedScreen(player, screen, page + 1);
        }
    }

    private void openPagedScreen(Player player, PcrykhView.Screen screen, int page) {
        if (screen == PcrykhView.Screen.ACHIEVEMENTS) {
            openAchievementsMenu(player, page);
        } else if (screen == PcrykhView.Screen.QUESTS) {
            openQuestsMenu(player, page);
        }
    }

    private int itemCount(PcrykhView.Screen screen) {
        return switch (screen) {
            case ACHIEVEMENTS -> this.loadedRuntime.achievements().size();
            case QUESTS -> this.loadedRuntime.quests().size();
            default -> 0;
        };
    }

    private void handleSettingsClick(Player player, int slot) {
        switch (slot) {
            case 20 -> toggleSetting(player, "runtime.chat.announce_achievements");
            case 22 -> toggleSetting(player, "runtime.chat.facts_enabled");
            case 24 -> toggleSetting(player, "runtime.action_bar.progress_enabled");
            case 45 -> openMainMenu(player);
            default -> {
            }
        }
    }

    private void toggleSetting(Player player, String path) {
        boolean previous;
        boolean next;
        switch (path) {
            case "runtime.chat.announce_achievements" -> {
                previous = this.announceAchievementsEnabled;
                next = !previous;
                this.announceAchievementsEnabled = next;
            }
            case "runtime.chat.facts_enabled" -> {
                previous = this.factsEnabled;
                next = !previous;
                this.factsEnabled = next;
            }
            case "runtime.action_bar.progress_enabled" -> {
                previous = this.progressIndicatorsEnabled;
                next = !previous;
                this.progressIndicatorsEnabled = next;
            }
            default -> {
                return;
            }
        }

        try {
            persistToggle(path, next);
            if ("runtime.chat.facts_enabled".equals(path)) {
                scheduleFacts();
            }
            openSettingsMenu(player);
        } catch (IOException exception) {
            restoreSetting(path, previous);
            player.sendMessage("Failed to persist settings. Change rolled back.");
            this.plugin.getLogger().warning("Failed to persist config toggle " + path + ": " + exception.getMessage());
            openSettingsMenu(player);
        }
    }

    private void restoreSetting(String path, boolean value) {
        switch (path) {
            case "runtime.chat.announce_achievements" -> this.announceAchievementsEnabled = value;
            case "runtime.chat.facts_enabled" -> this.factsEnabled = value;
            case "runtime.action_bar.progress_enabled" -> this.progressIndicatorsEnabled = value;
            default -> {
            }
        }
    }

    private void persistToggle(String path, boolean value) throws IOException {
        Path configPath = this.plugin.getDataFolder().toPath().resolve("config.json");
        ObjectNode root = (ObjectNode) ConfigLoader.MAPPER.readTree(Files.readString(configPath));
        String[] segments = path.split("\\.");
        ObjectNode current = root;
        for (int index = 0; index < segments.length - 1; index++) {
            JsonNode next = current.get(segments[index]);
            if (!(next instanceof ObjectNode objectNode)) {
                throw new IOException("Config path is not an object: " + path);
            }
            current = objectNode;
        }
        current.put(segments[segments.length - 1], value);

        Path tempPath = configPath.resolveSibling("config.json.tmp");
        Files.writeString(tempPath, ConfigLoader.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));
        Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void applyNavigation(Inventory inventory, int page, int totalPages) {
        inventory.setItem(45, button(Material.BARRIER, "Back", List.of()));
        if (page > 0) {
            inventory.setItem(47, button(Material.ARROW, "Previous", List.of()));
        }
        inventory.setItem(49, button(Material.PAPER, "Page", List.of((page + 1) + "/" + totalPages)));
        if (page < totalPages - 1) {
            inventory.setItem(53, button(Material.ARROW, "Next", List.of()));
        }
    }

    private ItemStack playerIdentity(Player player) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.displayName(Component.text("You"));
            skullMeta.lore(List.of(
                    Component.text("name: " + player.getName()),
                    Component.text("uuid: " + player.getUniqueId())
            ));
            skullMeta.setOwningPlayer(player);
            stack.setItemMeta(skullMeta);
        }
        return stack;
    }

    private ItemStack profileSummary() {
        int total = this.loadedRuntime.achievements().size();
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Progress"));
            meta.lore(List.of(
                    Component.text("completed: 0/" + total),
                    Component.text("ap: 0"),
                    Component.text("completion: 0%")
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack achievementItem(AchievementDefinition achievement) {
        ItemStack stack = new ItemStack(materialOrFallback(achievement.icon(), Material.PAPER));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(achievement.title()));
            meta.lore(List.of(
                    Component.text(achievement.description()),
                    Component.text("category: " + achievement.categoryId()),
                    Component.text("progress: 0/" + achievement.amount()),
                    Component.text("ap: " + achievement.ap())
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack questItem(QuestDefinition quest) {
        ItemStack stack = new ItemStack(Material.MAP);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(quest.title()));
            meta.lore(List.of(
                    Component.text("id: " + quest.id()),
                    Component.text("npc: " + quest.npcId()),
                    Component.text("stage: accepted"),
                    Component.text("progress: 0/" + quest.target()),
                    Component.text("state: accepted")
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack hotbarBeacon() {
        ItemStack stack = new ItemStack(Material.BEACON);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Pcrykh Menu"));
            meta.lore(List.of(Component.text("Open the catalog")));
            meta.getPersistentDataContainer().set(this.beaconKey, PersistentDataType.STRING, "menu");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack button(Material material, String title, List<String> loreLines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(title));
            if (!loreLines.isEmpty()) {
                meta.lore(loreLines.stream().map(Component::text).toList());
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Material materialOrFallback(String name, Material fallback) {
        Material material = Material.matchMaterial(name);
        return material == null ? fallback : material;
    }

    private String onOff(boolean value) {
        return value ? "on" : "off";
    }

    private int pageCount(int itemCount) {
        return Math.max(1, (int) Math.ceil(itemCount / (double) PAGE_SIZE));
    }

    private int clampPage(int page, int totalPages) {
        if (page < 0) {
            return 0;
        }
        return Math.min(page, totalPages - 1);
    }
}