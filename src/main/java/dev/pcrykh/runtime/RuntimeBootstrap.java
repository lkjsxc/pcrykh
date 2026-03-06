package dev.pcrykh.runtime;

import dev.pcrykh.config.ConfigLoader;
import dev.pcrykh.config.ConfigLoader.LoadedRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class RuntimeBootstrap {
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
    private LoadedRuntime loadedRuntime;

    private RuntimeBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
        this.tasks = new ArrayList<>();
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
    }

    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, "Pcrykh");
        inventory.setItem(10, menuItem(Material.BOOK, "Achievements"));
        inventory.setItem(12, menuItem(Material.WRITABLE_BOOK, "Quests"));
        inventory.setItem(14, menuItem(Material.PLAYER_HEAD, "Profile"));
        inventory.setItem(16, menuItem(Material.COMPARATOR, "Settings"));
        player.openInventory(inventory);
    }

    public LoadedRuntime loadedRuntime() {
        return this.loadedRuntime;
    }

    private void initialize() throws Exception {
        installDefaultResources();
        this.loadedRuntime = new ConfigLoader().load(this.plugin.getDataFolder().toPath());
        this.plugin.getDataFolder().toPath()
                .resolve(this.loadedRuntime.config().runtime().persistence().playerState().directory())
                .normalize()
                .toFile()
                .mkdirs();
        scheduleFacts();
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
        if (!this.loadedRuntime.config().runtime().chat().factsEnabled()) {
            return;
        }
        if (this.loadedRuntime.facts().isEmpty()) {
            return;
        }
        long intervalTicks = (long) this.loadedRuntime.config().runtime().chat().factsIntervalSeconds() * 20L;
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            String fact = this.loadedRuntime.facts().get(ThreadLocalRandom.current().nextInt(this.loadedRuntime.facts().size()));
            Bukkit.broadcastMessage(this.loadedRuntime.config().runtime().chat().prefix() + fact);
        }, intervalTicks, intervalTicks);
        this.tasks.add(task);
    }

    private ItemStack menuItem(Material material, String title) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(title);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}