package dev.pcrykh;

import dev.pcrykh.achievements.AchievementService;
import dev.pcrykh.command.PcrykhCommand;
import dev.pcrykh.gui.GuiService;
import dev.pcrykh.listeners.MenuListener;
import dev.pcrykh.listeners.MovementListener;
import dev.pcrykh.quest.QuestService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PcrykhPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        AchievementService achievementService = new AchievementService();
        QuestService questService = new QuestService();
        GuiService guiService = new GuiService(achievementService, questService);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new MovementListener(achievementService), this);
        pluginManager.registerEvents(new MenuListener(guiService), this);

        PluginCommand pcrykhCommand = getCommand("pcrykh");
        if (pcrykhCommand == null) {
            getLogger().severe("Command 'pcrykh' is not defined in plugin.yml");
            return;
        }
        pcrykhCommand.setExecutor(new PcrykhCommand(guiService));

        getLogger().info("pcrykh enabled");
    }
}
