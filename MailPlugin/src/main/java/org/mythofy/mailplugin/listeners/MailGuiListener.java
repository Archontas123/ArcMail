package org.mythofy.mailplugin.listeners;

import org.mythofy.mailplugin.MailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.mythofy.mailplugin.FavoriteAddStep;
import org.mythofy.mailplugin.GiftSendStep;
import org.mythofy.mailplugin.MailSendStep;

public class MailGuiListener implements Listener {

    private final MailPlugin plugin;

    public MailGuiListener(MailPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.CHEST) return;

        if (event.getView().getTitle().equals("Mail Menu")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            player.closeInventory(); // Close the GUI
            switch (event.getSlot()) {
                case 2:
                    startMailSendProcess(player);
                    break;
                case 4:
                    startFavoriteAddProcess(player);
                    break;
                case 3:
                    startGiftSendProcess(player);
                    break;
            }
        }
    }

    private void startMailSendProcess(Player player) {
        player.sendMessage("Enter the player you want to send mail to:");
        plugin.getMailManager().setMailSendStep(player, MailSendStep.RECIPIENT);
    }

    private void startFavoriteAddProcess(Player player) {
        player.sendMessage("Enter the player you want to add to favorites:");
        plugin.getMailManager().setFavoriteAddStep(player, FavoriteAddStep.RECIPIENT);
    }

    private void startGiftSendProcess(Player player) {
        player.sendMessage("Enter the player you want to send a gift to:");
        plugin.getMailManager().setGiftSendStep(player, GiftSendStep.RECIPIENT);
    }
}
