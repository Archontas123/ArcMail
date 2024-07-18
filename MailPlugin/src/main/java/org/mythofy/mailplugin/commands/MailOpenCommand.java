package org.mythofy.mailplugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MailOpenCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            openMailGui(player);
            return true;
        }
        sender.sendMessage("Only players can use this command.");
        return false;
    }

    private void openMailGui(Player player) {
        Inventory mailGui = Bukkit.createInventory(null, 9, "Mail Menu");

        // Create buttons
        mailGui.setItem(0, createGuiItem(Material.BOOK, "Mail Inbox"));
        mailGui.setItem(1, createGuiItem(Material.CHEST, "Gift Inbox"));
        mailGui.setItem(2, createGuiItem(Material.PAPER, "Mail Send"));
        mailGui.setItem(3, createGuiItem(Material.CHEST_MINECART, "Gift Send"));
        mailGui.setItem(4, createGuiItem(Material.NAME_TAG, "Favorite Add"));
        mailGui.setItem(5, createGuiItem(Material.HEART_OF_THE_SEA, "Favorites"));

        player.openInventory(mailGui);
    }

    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
