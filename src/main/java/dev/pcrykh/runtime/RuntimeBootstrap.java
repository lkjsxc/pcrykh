package dev.pcrykh.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.pcrykh.config.ConfigLoader;
import dev.pcrykh.config.ConfigLoader.AchievementDefinition;
import dev.pcrykh.config.ConfigLoader.DialogueNodeDefinition;
import dev.pcrykh.config.ConfigLoader.LoadedRuntime;
import dev.pcrykh.config.ConfigLoader.NpcDefinition;
import dev.pcrykh.config.ConfigLoader.QuestDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class RuntimeBootstrap {
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
    private final NamespacedKey npcKey;
    private final Map<UUID, ConversationSession> conversations;
    private final Map<UUID, Map<String, NpcProgressState>> npcStates;
    private final Map<UUID, Map<String, PlayerQuestState>> questStates;
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
        this.npcKey = new NamespacedKey(plugin, "npc_id");
        this.conversations = new HashMap<>();
        this.npcStates = new HashMap<>();
        this.questStates = new HashMap<>();
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
        this.conversations.clear();
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
        List<PlayerQuestState> quests = new ArrayList<>(this.questStates
                .getOrDefault(player.getUniqueId(), Map.of())
                .values());
        quests.sort(Comparator
                .comparing(PlayerQuestState::completed)
                .thenComparing(PlayerQuestState::questId));

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
                PlayerQuestState quest = quests.get(index);
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
        return item.getItemMeta().getPersistentDataContainer().has(this.beaconKey, PersistentDataType.STRING);
    }

    public void openMenuFromBeacon(Player player) {
        openMainMenu(player);
    }

    public void handleBeaconInventoryClick(Player player) {
        openMainMenu(player);
    }

    public boolean handleVillagerInteract(Player player, Entity entity) {
        if (!(entity instanceof Villager villager)) {
            return false;
        }

        String npcId = villager.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
        if (npcId == null) {
            return false;
        }

        NpcDefinition npc = findNpc(npcId);
        if (npc == null) {
            return false;
        }

        ConversationSession session = this.conversations.get(player.getUniqueId());
        if (session == null || !session.npcId().equals(npcId)) {
            startConversation(player, npc);
        } else {
            advanceConversation(player, npc, session);
        }
        return true;
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
        scheduleVillagerGuard();
        scheduleConversationTimeoutGuard();
        Bukkit.getOnlinePlayers().forEach(this::ensureHotbarBeacon);
        ensureManagedVillagers();

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

    private void scheduleVillagerGuard() {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::ensureManagedVillagers, 1L, 200L);
        this.tasks.add(task);
    }

    private void scheduleConversationTimeoutGuard() {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::expireTimedOutConversations, 20L, 20L);
        this.tasks.add(task);
    }

    private void ensureManagedVillagers() {
        for (NpcDefinition npc : this.loadedRuntime.npcs()) {
            World world = Bukkit.getWorld(npc.world());
            if (world == null) {
                this.plugin.getLogger().warning("NPC world is missing: " + npc.world());
                continue;
            }

            Villager villager = world.getEntitiesByClass(Villager.class).stream()
                    .filter(entity -> npc.id().equals(entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING)))
                    .findFirst()
                    .orElseGet(() -> spawnVillager(world, npc));

            syncVillager(villager, npc);
        }
    }

    private Villager spawnVillager(World world, NpcDefinition npc) {
        Location location = new Location(world, npc.position().x(), npc.position().y(), npc.position().z());
        return world.spawn(location, Villager.class, villager -> syncVillager(villager, npc));
    }

    private void syncVillager(Villager villager, NpcDefinition npc) {
        villager.getPersistentDataContainer().set(this.npcKey, PersistentDataType.STRING, npc.id());
        villager.customName(Component.text(npc.displayName()));
        villager.setCustomNameVisible(true);
        villager.setRemoveWhenFarAway(false);
        villager.setPersistent(true);
        villager.setInvulnerable(true);
        villager.setCanPickupItems(false);
        villager.setProfession(parseProfession(npc.profession()));
    }

    private Villager.Profession parseProfession(String profession) {
        try {
            return Villager.Profession.valueOf(profession);
        } catch (IllegalArgumentException exception) {
            return Villager.Profession.NONE;
        }
    }

    private void expireTimedOutConversations() {
        long now = System.currentTimeMillis();
        long timeoutMillis = this.loadedRuntime.config().runtime().dialogue().timeoutSeconds() * 1000L;
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, ConversationSession> entry : this.conversations.entrySet()) {
            if (now - entry.getValue().lastInteractionEpochMs() >= timeoutMillis) {
                expired.add(entry.getKey());
            }
        }

        for (UUID playerId : expired) {
            Player player = Bukkit.getPlayer(playerId);
            ConversationSession session = this.conversations.remove(playerId);
            if (player != null && session != null) {
                player.sendMessage(Component.text("Conversation timed out."));
                NpcProgressState progress = npcProgress(playerId, session.npcId());
                progress.currentNodeId = progress.lastSavedNodeId;
            }
        }
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

    private void startConversation(Player player, NpcDefinition npc) {
        NpcProgressState progress = npcProgress(player.getUniqueId(), npc.id());
        String nodeId = progress.lastSavedNodeId != null && npc.dialogueGraph().nodes().containsKey(progress.lastSavedNodeId)
                ? progress.lastSavedNodeId
                : npc.dialogueGraph().startNodeId();
        progress.currentNodeId = nodeId;
        progress.dialogueVisits++;
        this.conversations.put(player.getUniqueId(), new ConversationSession(npc.id(), nodeId, System.currentTimeMillis()));
        renderDialogueNode(player, npc, npc.dialogueGraph().nodes().get(nodeId));
    }

    private void advanceConversation(Player player, NpcDefinition npc, ConversationSession session) {
        DialogueNodeDefinition currentNode = npc.dialogueGraph().nodes().get(session.currentNodeId());
        if (currentNode == null) {
            this.conversations.remove(player.getUniqueId());
            player.sendMessage(Component.text("Dialogue state was invalid and has been reset."));
            return;
        }

        NpcProgressState progress = npcProgress(player.getUniqueId(), npc.id());
        if (currentNode.affinityDelta() != 0) {
            progress.affinity = clampAffinity(progress.affinity + currentNode.affinityDelta());
        }

        String nextNodeId = currentNode.toNodeId();
        if (currentNode.saveCheckpoint() && nextNodeId != null) {
            progress.lastSavedNodeId = nextNodeId;
        }
        if (nextNodeId == null) {
            this.conversations.remove(player.getUniqueId());
            return;
        }

        DialogueNodeDefinition nextNode = npc.dialogueGraph().nodes().get(nextNodeId);
        if (nextNode == null) {
            this.conversations.remove(player.getUniqueId());
            player.sendMessage(Component.text("Dialogue target is missing and the conversation was reset."));
            progress.currentNodeId = npc.dialogueGraph().startNodeId();
            progress.lastSavedNodeId = npc.dialogueGraph().startNodeId();
            return;
        }

        progress.currentNodeId = nextNodeId;
        this.conversations.put(player.getUniqueId(), new ConversationSession(npc.id(), nextNodeId, System.currentTimeMillis()));
        renderDialogueNode(player, npc, nextNode);

        if ("accept_quest".equals(nextNode.type())) {
            acceptQuest(player, npc);
            this.conversations.remove(player.getUniqueId());
        } else if ("end".equals(nextNode.type())) {
            this.conversations.remove(player.getUniqueId());
        }
    }

    private void renderDialogueNode(Player player, NpcDefinition npc, DialogueNodeDefinition node) {
        player.sendMessage(Component.text(npc.displayName() + ": " + node.text()));
    }

    private void acceptQuest(Player player, NpcDefinition npc) {
        QuestDefinition quest = this.loadedRuntime.quests().stream()
                .filter(candidate -> candidate.npcId().equals(npc.id()))
                .findFirst()
                .orElse(null);
        if (quest == null) {
            player.sendMessage(Component.text("No quest is bound to this villager."));
            return;
        }

        this.questStates
                .computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .put(quest.id(), new PlayerQuestState(quest.id(), quest.npcId(), quest.title(), "accepted", 0, quest.target(), false));
        npcProgress(player.getUniqueId(), npc.id()).activeQuestId = quest.id();
        player.sendMessage(Component.text("Quest accepted: " + quest.title()));
    }

    private NpcDefinition findNpc(String npcId) {
        return this.loadedRuntime.npcs().stream()
                .filter(npc -> npc.id().equals(npcId))
                .findFirst()
                .orElse(null);
    }

    private NpcProgressState npcProgress(UUID playerId, String npcId) {
        return this.npcStates
                .computeIfAbsent(playerId, ignored -> new HashMap<>())
                .computeIfAbsent(npcId, ignored -> new NpcProgressState());
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
        if (slot == 53 && page < pageCount(itemCount(player, screen)) - 1) {
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

    private int itemCount(Player player, PcrykhView.Screen screen) {
        return switch (screen) {
            case ACHIEVEMENTS -> this.loadedRuntime.achievements().size();
            case QUESTS -> this.questStates.getOrDefault(player.getUniqueId(), Map.of()).size();
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
        int apTotal = this.loadedRuntime.achievements().stream().mapToInt(AchievementDefinition::ap).sum();
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Progress"));
            meta.lore(List.of(
                    Component.text("completed: " + total + "/" + total),
                    Component.text("ap: " + apTotal),
                    Component.text("completion: 100%")
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
                    Component.text("progress: " + achievement.amount() + "/" + achievement.amount()),
                    Component.text("ap: " + achievement.ap())
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack questItem(PlayerQuestState quest) {
        ItemStack stack = new ItemStack(Material.MAP);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(quest.title()));
            meta.lore(List.of(
                    Component.text("id: " + quest.questId()),
                    Component.text("npc: " + quest.npcId()),
                    Component.text("stage: " + quest.stage()),
                    Component.text("progress: " + quest.progress() + "/" + quest.target()),
                    Component.text("state: " + (quest.completed() ? "completed" : "accepted"))
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

    private int clampAffinity(int value) {
        return Math.max(-100, Math.min(100, value));
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

    private record ConversationSession(String npcId, String currentNodeId, long lastInteractionEpochMs) {
    }

    private static final class NpcProgressState {
        private int affinity;
        private String lastSavedNodeId;
        private int dialogueVisits;
        private String activeQuestId;
        private String currentNodeId;
    }

    private record PlayerQuestState(String questId, String npcId, String title, String stage, int progress, int target, boolean completed) {
    }
}