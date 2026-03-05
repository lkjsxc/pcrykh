package dev.pcrykh;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pcrykh.gui.AchievementMenuListener;
import dev.pcrykh.gui.AchievementMenuService;
import dev.pcrykh.gui.HotbarBeaconListener;
import dev.pcrykh.gui.HotbarBeaconService;
import dev.pcrykh.runtime.AchievementCatalog;
import dev.pcrykh.runtime.AchievementDefinitionLoader;
import dev.pcrykh.runtime.AchievementProgressListener;
import dev.pcrykh.runtime.AchievementProgressService;
import dev.pcrykh.runtime.AchievementSourceResolver;
import dev.pcrykh.runtime.CategoryLoader;
import dev.pcrykh.runtime.CategoryCatalog;
import dev.pcrykh.runtime.CategorySourceResolver;
import dev.pcrykh.runtime.ConfigException;
import dev.pcrykh.runtime.ConfigLoader;
import dev.pcrykh.runtime.ConfigSaver;
import dev.pcrykh.runtime.FactsBroadcaster;
import dev.pcrykh.runtime.NpcCatalog;
import dev.pcrykh.runtime.NpcDefinitionLoader;
import dev.pcrykh.runtime.NpcSourceResolver;
import dev.pcrykh.runtime.PlayerStateLifecycleListener;
import dev.pcrykh.runtime.PlayerStateStore;
import dev.pcrykh.runtime.QuestCatalog;
import dev.pcrykh.runtime.QuestDefinitionLoader;
import dev.pcrykh.runtime.QuestService;
import dev.pcrykh.runtime.QuestSourceResolver;
import dev.pcrykh.runtime.RuntimeConfig;
import dev.pcrykh.runtime.VillagerConversationService;
import dev.pcrykh.runtime.VillagerInteractionListener;
import dev.pcrykh.runtime.command.PcrykhCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.List;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PcrykhPlugin extends JavaPlugin {
    private AchievementCatalog catalog;
    private AchievementMenuService menuService;
    private HotbarBeaconService hotbarService;
    private PlayerStateStore playerStateStore;

    @Override
    public void onEnable() {
        try {
            ensureDefaultResources();

            ObjectMapper mapper = new ObjectMapper();
            ConfigLoader configLoader = new ConfigLoader(mapper);
            RuntimeConfig config = configLoader.load(getDataFolder().toPath());
            ConfigSaver configSaver = new ConfigSaver(mapper);

            CategorySourceResolver categoryResolver = new CategorySourceResolver();
            List<Path> categoryFiles = categoryResolver.resolve(getDataFolder().toPath(), config.categorySources());
            CategoryLoader categoryLoader = new CategoryLoader(mapper);
            CategoryCatalog categoryCatalog = new CategoryCatalog(categoryLoader.loadAll(categoryFiles));

            AchievementSourceResolver resolver = new AchievementSourceResolver();
            List<Path> achievementFiles = resolver.resolve(getDataFolder().toPath(), config.achievementSources());
            AchievementDefinitionLoader achievementLoader = new AchievementDefinitionLoader(mapper);
            catalog = new AchievementCatalog(achievementLoader.loadAll(achievementFiles), categoryCatalog);

            NpcSourceResolver npcResolver = new NpcSourceResolver();
            List<Path> npcFiles = npcResolver.resolve(getDataFolder().toPath(), config.npcSources());
            NpcDefinitionLoader npcLoader = new NpcDefinitionLoader(mapper);
            NpcCatalog npcCatalog = new NpcCatalog(npcLoader.loadAll(npcFiles));

            QuestSourceResolver questResolver = new QuestSourceResolver();
            List<Path> questFiles = questResolver.resolve(getDataFolder().toPath(), config.questSources());
            QuestDefinitionLoader questLoader = new QuestDefinitionLoader(mapper);
            QuestCatalog questCatalog = new QuestCatalog(questLoader.loadAll(questFiles), npcCatalog);

            playerStateStore = new PlayerStateStore(mapper, getDataFolder().toPath(), config);
            QuestService questService = new QuestService(questCatalog, playerStateStore);
            VillagerConversationService conversationService = new VillagerConversationService(this, config, npcCatalog, questService, playerStateStore);

            AchievementProgressService progressService = new AchievementProgressService(this, catalog, config);

            menuService = new AchievementMenuService(catalog, progressService, config);
            hotbarService = new HotbarBeaconService(this, menuService);

            Bukkit.getPluginManager().registerEvents(new AchievementMenuListener(menuService, config, configSaver, getDataFolder().toPath()), this);
            Bukkit.getPluginManager().registerEvents(new HotbarBeaconListener(hotbarService), this);
            Bukkit.getPluginManager().registerEvents(new AchievementProgressListener(progressService), this);
            Bukkit.getPluginManager().registerEvents(new VillagerInteractionListener(conversationService), this);
            Bukkit.getPluginManager().registerEvents(new PlayerStateLifecycleListener(playerStateStore, conversationService), this);

            if (getCommand("pcrykh") != null) {
                getCommand("pcrykh").setExecutor(new PcrykhCommand(menuService));
            }

            new FactsBroadcaster(this, config).start();
            hotbarService.startEnforcementTask();

            if (config.autosave().enabled()) {
                int intervalTicks = Math.max(20, config.autosave().intervalSeconds() * 20);
                Bukkit.getScheduler().runTaskTimer(this, () -> {
                    if (playerStateStore != null) {
                        playerStateStore.saveAll();
                    }
                }, intervalTicks, intervalTicks);
            }
        } catch (ConfigException ex) {
            getLogger().severe("Config error: " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        } catch (Exception ex) {
            getLogger().severe("Startup failure: " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void ensureDefaultResources() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new ConfigException("Failed to create data folder");
        }
        saveResource("config.json", false);
        saveResourceDirectory("achievements");
        saveResourceDirectory("facts");
        saveResourceDirectory("npcs");
        saveResourceDirectory("quests");
    }

    @Override
    public void onDisable() {
        if (playerStateStore != null) {
            playerStateStore.saveAll();
        }
    }

    private void saveResourceDirectory(String resourceDir) {
        String normalized = resourceDir.endsWith("/") ? resourceDir : resourceDir + "/";

        URL dirUrl = getClass().getClassLoader().getResource(normalized);
        if (dirUrl != null && "file".equals(dirUrl.getProtocol())) {
            try {
                Path dirPath = Paths.get(dirUrl.toURI());
                if (Files.exists(dirPath)) {
                    copyDirectoryResources(dirPath, normalized);
                    return;
                }
            } catch (URISyntaxException | FileSystemNotFoundException ex) {
                throw new ConfigException("Failed to resolve resource directory: " + resourceDir, ex);
            }
        }

        CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return;
        }

        try {
            URI location = codeSource.getLocation().toURI();
            Path sourcePath = Paths.get(location);
            if (Files.isRegularFile(sourcePath)) {
                try (JarFile jar = new JarFile(sourcePath.toFile())) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith(normalized) && !entry.isDirectory()) {
                            saveResource(name, false);
                        }
                    }
                }
                return;
            }

            if (Files.isDirectory(sourcePath)) {
                Path dirPath = sourcePath.resolve(resourceDir);
                if (Files.exists(dirPath)) {
                    copyDirectoryResources(dirPath, normalized);
                }
            }
        } catch (Exception ex) {
            throw new ConfigException("Failed to copy default resources for: " + resourceDir, ex);
        }
    }

    private void copyDirectoryResources(Path dirPath, String normalized) {
        try (var stream = Files.walk(dirPath)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                Path relative = dirPath.relativize(path);
                String resourcePath = normalized + relative.toString().replace('\\', '/');
                saveResource(resourcePath, false);
            });
        } catch (Exception ex) {
            throw new ConfigException("Failed to copy resources from: " + dirPath, ex);
        }
    }
}
