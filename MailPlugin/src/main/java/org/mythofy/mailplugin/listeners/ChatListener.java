package org.mythofy.mailplugin.listeners;

import org.mythofy.mailplugin.MailPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final MailPlugin plugin;

    public ChatListener(MailPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        plugin.getMailManager().handleChatInput(event.getPlayer(), event.getMessage());
        event.setCancelled(true);
    }
}
