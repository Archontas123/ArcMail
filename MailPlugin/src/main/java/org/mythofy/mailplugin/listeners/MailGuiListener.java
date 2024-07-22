package org.mythofy.mailplugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.mythofy.mailplugin.FavoriteAddStep;
import org.mythofy.mailplugin.GiftSendStep;
import org.mythofy.mailplugin.MailManager;
import org.mythofy.mailplugin.MailSendStep;

public class MailGuiListener implements Listener {

    private final MailManager mailManager;

    public MailGuiListener(MailManager mailManager) {
        this.mailManager = mailManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        int page = title.contains("Page") ? Integer.parseInt(title.split("Page ")[1]) - 1 : 0;

        if (title.equals("Mail Menu")) {
            event.setCancelled(true);
            switch (event.getSlot()) {
                case 10:
                    mailManager.openMailInbox(player, 0);
                    break;
                case 11:
                    mailManager.openGiftInbox(player, 0);
                    break;
                case 12:
                    mailManager.setMailSendStep(player, MailSendStep.RECIPIENT);
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Enter the player you want to send mail to:");
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.closeInventory();
                        }
                    }.runTask(mailManager.getPlugin());
                    break;
                case 13:
                    mailManager.setGiftSendStep(player, GiftSendStep.RECIPIENT);
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Enter the player you want to send a gift to:");
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.closeInventory();
                        }
                    }.runTask(mailManager.getPlugin());
                    break;
                case 14:
                    mailManager.setFavoriteAddStep(player, FavoriteAddStep.RECIPIENT);
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Enter the player you want to add to favorites:");
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.closeInventory();
                        }
                    }.runTask(mailManager.getPlugin());
                    break;
                case 15:
                    mailManager.openFavoritesList(player, 0);
                    break;
                case 16: // Close GUI button
                    player.closeInventory();
                    break;
            }
        } else if (title.startsWith("Mail Inbox") || title.startsWith("Gift Inbox") || title.startsWith("Favorites List")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item != null) {
                if (item.getType() == Material.ARROW && item.getItemMeta().getDisplayName().equals("Previous Page")) {
                    if (page > 0) {
                        if (title.startsWith("Mail Inbox")) {
                            mailManager.openMailInbox(player, page - 1);
                        } else if (title.startsWith("Gift Inbox")) {
                            mailManager.openGiftInbox(player, page - 1);
                        } else if (title.startsWith("Favorites List")) {
                            mailManager.openFavoritesList(player, page - 1);
                        }
                    }
                } else if (item.getType() == Material.ARROW && item.getItemMeta().getDisplayName().equals("Next Page")) {
                    if (title.startsWith("Mail Inbox")) {
                        mailManager.openMailInbox(player, page + 1);
                    } else if (title.startsWith("Gift Inbox")) {
                        mailManager.openGiftInbox(player, page + 1);
                    } else if (title.startsWith("Favorites List")) {
                        mailManager.openFavoritesList(player, page + 1);
                    }
                } else if (item.getType() == Material.BARRIER && item.getItemMeta().getDisplayName().equals("Back to Main Menu")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.closeInventory();
                            mailManager.reopenBaseGui(player);
                        }
                    }.runTask(mailManager.getPlugin());
                } else if (item.getType() == Material.LAVA_BUCKET && item.getItemMeta().getDisplayName().equals("Clear All")) {
                    if (title.startsWith("Mail Inbox")) {
                        mailManager.clearMailInbox(player.getUniqueId());
                        player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "All mails have been cleared.");
                        mailManager.openMailInbox(player, page);
                    } else if (title.startsWith("Gift Inbox")) {
                        mailManager.clearGiftInbox(player.getUniqueId());
                        player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "All gifts have been cleared.");
                        mailManager.openGiftInbox(player, page);
                    }
                } else {
                    if (title.startsWith("Mail Inbox")) {
                        mailManager.handleInboxClick(player, item, event.isShiftClick(), page);
                    } else if (title.startsWith("Gift Inbox")) {
                        mailManager.handleGiftInboxClick(player, item, event.isShiftClick(), page);
                    } else if (title.startsWith("Favorites List")) {
                        mailManager.handleFavoritesClick(player, item, event.isShiftClick(), page);
                    }
                }
            }
        }
    }
}
