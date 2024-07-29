package org.mythofy.mailplugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.mythofy.mailplugin.MailManager;

public class ChatListener implements Listener {

    private final MailManager mailManager;

    public ChatListener(MailManager mailManager) {
        this.mailManager = mailManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (mailManager.isInMailProcess(event.getPlayer()) || mailManager.isInGiftProcess(event.getPlayer()) || mailManager.isInFavoriteProcess(event.getPlayer())) {
            event.setCancelled(true); // Suppress public chat

            String message = event.getMessage();
            if (message.equalsIgnoreCase("cancel")) {
                mailManager.cancelCurrentAction(event.getPlayer());
            } else {
                mailManager.handleChatInput(event.getPlayer(), message);
            }
        }
    }
}
