package dev.pcrykh.runtime;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PcrykhCommand implements CommandExecutor {
    private final RuntimeBootstrap bootstrap;

    public PcrykhCommand(RuntimeBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("pcrykh menu is player-only.");
            return true;
        }

        this.bootstrap.openMainMenu(player);
        return true;
    }
}