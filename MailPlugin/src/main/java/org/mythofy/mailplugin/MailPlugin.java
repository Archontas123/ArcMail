package org.mythofy.mailplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.mythofy.mailplugin.commands.MailOpenCommand;
import org.mythofy.mailplugin.listeners.ChatListener;
import org.mythofy.mailplugin.listeners.MailGuiListener;

public class MailPlugin extends JavaPlugin {

    private MailManager mailManager;

    @Override
    public void onEnable() {
        mailManager = new MailManager(this);
        this.getCommand("mail").setExecutor(new MailOpenCommand());
        getServer().getPluginManager().registerEvents(new MailGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // Register confirmation and cancel commands
        getCommand("confirmmailrecipient").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    mailManager.confirmMailRecipient(player);
                }
                return true;
            }
        });

        getCommand("confirmmailmessage").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    mailManager.confirmMailMessage(player);
                }
                return true;
            }
        });

        getCommand("confirmgiftsend").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    mailManager.confirmGiftSend(player);
                }
                return true;
            }
        });

        getCommand("cancelmailaction").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    mailManager.cancelMailAction(player);
                }
                return true;
            }
        });

        getCommand("cancelgiftaction").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    mailManager.cancelGiftAction(player);
                }
                return true;
            }
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public MailManager getMailManager() {
        return mailManager;
    }
}
