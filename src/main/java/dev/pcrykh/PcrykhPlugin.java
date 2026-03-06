package dev.pcrykh;

import dev.pcrykh.runtime.PcrykhCommand;
import dev.pcrykh.runtime.PcrykhListener;
import dev.pcrykh.runtime.RuntimeBootstrap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PcrykhPlugin extends JavaPlugin {
    private RuntimeBootstrap bootstrap;

    @Override
    public void onEnable() {
        try {
            this.bootstrap = RuntimeBootstrap.start(this);

            PluginCommand command = getCommand("pcrykh");
            if (command == null) {
                throw new IllegalStateException("Command 'pcrykh' is missing from plugin.yml");
            }

            command.setExecutor(new PcrykhCommand(this.bootstrap));
            getServer().getPluginManager().registerEvents(new PcrykhListener(this, this.bootstrap), this);
        } catch (Exception exception) {
            getLogger().severe("Failed to enable pcrykh: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.shutdown();
        }
    }
}