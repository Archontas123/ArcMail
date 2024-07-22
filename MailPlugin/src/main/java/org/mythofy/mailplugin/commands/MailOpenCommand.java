package org.mythofy.mailplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mythofy.mailplugin.MailManager;

public class MailOpenCommand implements CommandExecutor {

    private final MailManager mailManager;

    public MailOpenCommand(MailManager mailManager) {
        this.mailManager = mailManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            mailManager.reopenBaseGui(player);
            return true;
        }
        sender.sendMessage("Only players can use this command.");
        return false;
    }
}
