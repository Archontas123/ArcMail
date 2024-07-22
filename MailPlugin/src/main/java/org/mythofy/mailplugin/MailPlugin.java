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
        try {
            mailManager = new MailManager(this);
            mailManager.loadData(); // Load data on startup

            this.getCommand("mail").setExecutor(new MailOpenCommand(mailManager));
            getServer().getPluginManager().registerEvents(new MailGuiListener(mailManager), this);
            getServer().getPluginManager().registerEvents(new ChatListener(mailManager), this);

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
        } catch (Exception e) {
            getLogger().severe("Failed to initialize MailPlugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this); // Disable the plugin if initialization fails
        }
    }

    @Override
    public void onDisable() {
        if (mailManager != null) {
            mailManager.saveData(); // Save data on shutdown
        }
    }

    public MailManager getMailManager() {
        return mailManager;
    }
}
