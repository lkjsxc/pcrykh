package dev.pcrykh.command;

import dev.pcrykh.gui.GuiService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PcrykhCommand implements CommandExecutor {
    private final GuiService guiService;

    public PcrykhCommand(GuiService guiService) {
        this.guiService = guiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("pcrykh menu is player-only.");
            return true;
        }

        if (!player.hasPermission("pcrykh.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        guiService.openMainMenu(player);
        return true;
    }
}
