package org.mythofy.mailplugin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MailManager {

    private final MailPlugin plugin;
    private final Map<UUID, MailSendStep> mailSendSteps = new HashMap<>();
    private final Map<UUID, FavoriteAddStep> favoriteAddSteps = new HashMap<>();
    private final Map<UUID, GiftSendStep> giftSendSteps = new HashMap<>();
    private final Map<String, Object> stepData = new HashMap<>();
    private final Map<UUID, List<ItemStack>> giftInboxes = new HashMap<>();
    private final Map<UUID, List<ItemStack>> mailInboxes = new HashMap<>();
    private final Map<UUID, List<UUID>> favorites = new HashMap<>();
    private final Map<UUID, Set<String>> claimedGifts = new HashMap<>(); // Track claimed gifts

    private final File dataFile;
    private final Gson gson = new Gson();

    public MailManager(MailPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "mailData.yml");
        loadData();
    }

    public MailPlugin getPlugin() {
        return plugin;
    }

    public void setMailSendStep(Player player, MailSendStep step) {
        mailSendSteps.put(player.getUniqueId(), step);
    }

    public void setFavoriteAddStep(Player player, FavoriteAddStep step) {
        favoriteAddSteps.put(player.getUniqueId(), step);
    }

    public void setGiftSendStep(Player player, GiftSendStep step) {
        giftSendSteps.put(player.getUniqueId(), step);
    }

    public boolean isInMailProcess(Player player) {
        return mailSendSteps.containsKey(player.getUniqueId());
    }

    public boolean isInGiftProcess(Player player) {
        return giftSendSteps.containsKey(player.getUniqueId());
    }

    public boolean isInFavoriteProcess(Player player) {
        return favoriteAddSteps.containsKey(player.getUniqueId());
    }

    public void handleChatInput(Player player, String message) {
        UUID playerId = player.getUniqueId();
        if (mailSendSteps.containsKey(playerId)) {
            handleMailSendInput(player, message, mailSendSteps.get(playerId));
        } else if (favoriteAddSteps.containsKey(playerId)) {
            handleFavoriteAddInput(player, message, favoriteAddSteps.get(playerId));
        } else if (giftSendSteps.containsKey(playerId)) {
            handleGiftSendInput(player, message, giftSendSteps.get(playerId));
        }
    }

    private void handleMailSendInput(Player player, String message, MailSendStep step) {
        switch (step) {
            case RECIPIENT:
                if (message.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "You cannot send mail to yourself. Enter a different player name:");
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                    return;
                }
                OfflinePlayer recipient = Bukkit.getOfflinePlayer(message);
                if (recipient != null && recipient.hasPlayedBefore()) {
                    stepData.put(player.getUniqueId() + "_recipient", recipient.getUniqueId());
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GREEN + "Recipient found: " + ChatColor.YELLOW + recipient.getName());
                    setMailSendStep(player, MailSendStep.MESSAGE);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.closeInventory();
                        }
                    }.runTask(plugin);
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Enter the message you want to send:");
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                } else {
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "Player not found or has never logged in. Please enter a valid player name:");
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                }
                break;
            case MESSAGE:
                stepData.put(player.getUniqueId() + "_message", message);
                player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You typed: " + ChatColor.YELLOW + message);
                sendConfirmationMessage(player, "Confirm message?", "/confirmmailmessage", "/cancelmailaction");
                break;
        }
    }

    private void handleFavoriteAddInput(Player player, String message, FavoriteAddStep step) {
        if (step == FavoriteAddStep.RECIPIENT) {
            if (message.equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "You cannot add yourself to favorites. Enter a different player name:");
                player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                return;
            }
            OfflinePlayer favorite = Bukkit.getOfflinePlayer(message);
            if (favorite != null && favorite.hasPlayedBefore()) {
                UUID playerId = player.getUniqueId();
                List<UUID> favoriteList = favorites.computeIfAbsent(playerId, k -> new ArrayList<>());
                if (favoriteList.contains(favorite.getUniqueId())) {
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + favorite.getName() + " is already in your favorites.");
                } else {
                    favoriteList.add(favorite.getUniqueId());
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GREEN + "Added " + ChatColor.YELLOW + favorite.getName() + ChatColor.GREEN + " to favorites.");
                }
                favoriteAddSteps.remove(player.getUniqueId());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.closeInventory();
                    }
                }.runTask(plugin);
                saveData();
                reopenBaseGui(player);
            } else {
                player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "Player not found or has never logged in. Please enter a valid player name:");
                player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
            }
        }
    }

    private void handleGiftSendInput(Player player, String message, GiftSendStep step) {
        switch (step) {
            case RECIPIENT:
                if (message.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "You cannot send a gift to yourself. Enter a different player name:");
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                    return;
                }
                OfflinePlayer recipient = Bukkit.getOfflinePlayer(message);
                if (recipient != null && recipient.hasPlayedBefore()) {
                    stepData.put(player.getUniqueId() + "_recipient", recipient.getUniqueId());
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GREEN + "Recipient found: " + ChatColor.YELLOW + recipient.getName());
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item != null) {
                        player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You are about to send: " + ChatColor.YELLOW + "" + item.getAmount() + " x " + item.getType().name());
                        stepData.put(player.getUniqueId() + "_gift", item);
                        setGiftSendStep(player, GiftSendStep.CONFIRM_GIFT);
                        player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                        sendConfirmationMessage(player, "Confirm gift send of " + item.getAmount() + " x " + item.getType().name() + "?", "/confirmgiftsend", "/cancelgiftaction");
                    } else {
                        player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "You are not holding any item. Please hold an item and re-enter the player name:");
                        player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                        setGiftSendStep(player, GiftSendStep.RECIPIENT);
                    }
                } else {
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "Player not found or has never logged in. Please enter a valid player name:");
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                }
                break;
        }
    }

    public void confirmMailRecipient(Player player) {
        if (mailSendSteps.get(player.getUniqueId()) == MailSendStep.RECIPIENT) {
            player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Enter the message you want to send:");
            setMailSendStep(player, MailSendStep.MESSAGE);
        } else {
            player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "This action has expired.");
        }
    }

    public void confirmMailMessage(Player player) {
        if (mailSendSteps.get(player.getUniqueId()) == MailSendStep.MESSAGE) {
            UUID recipientId = (UUID) stepData.get(player.getUniqueId() + "_recipient");
            if (recipientId == null) {
                player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "Error: Recipient not found. Please start the mail process again.");
                cancelMailAction(player);
                return;
            }

            OfflinePlayer recipient = Bukkit.getOfflinePlayer(recipientId);
            String message = (String) stepData.get(player.getUniqueId() + "_message");

            if (message == null) {
                player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "Error: Message not found. Please start the mail process again.");
                cancelMailAction(player);
                return;
            }

            if (recipient.isOnline()) {
                Player onlineRecipient = (Player) recipient;
                onlineRecipient.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You have received a mail from " + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + ": " + message);
            }
            player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Message sent!");
            mailInboxes.computeIfAbsent(recipientId, k -> new ArrayList<>()).add(createMailItem(player.getName(), message));
            mailSendSteps.remove(player.getUniqueId());
            stepData.remove(player.getUniqueId() + "_recipient");
            stepData.remove(player.getUniqueId() + "_message");
        } else {
            player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "This action has expired.");
        }
    }


    public void confirmGiftSend(Player player) {
        if (giftSendSteps.get(player.getUniqueId()) == GiftSendStep.CONFIRM_GIFT) {
            UUID recipientId = (UUID) stepData.get(player.getUniqueId() + "_recipient");
            if (recipientId == null) {
                player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "Error: Recipient not found. Please start the gift process again.");
                cancelGiftAction(player);
                return;
            }

            OfflinePlayer recipient = Bukkit.getOfflinePlayer(recipientId);
            ItemStack item = (ItemStack) stepData.get(player.getUniqueId() + "_gift");

            if (item == null) {
                player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "Error: Gift not found. Please start the gift process again.");
                cancelGiftAction(player);
                return;
            }

            List<ItemStack> gifts = giftInboxes.computeIfAbsent(recipientId, k -> new ArrayList<>());
            gifts.add(createGiftItem(player.getName(), item));

            player.getInventory().setItemInMainHand(null);
            player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GREEN + "Gift sent!");
            giftSendSteps.remove(player.getUniqueId());
            stepData.remove(player.getUniqueId() + "_recipient");
            stepData.remove(player.getUniqueId() + "_gift");
            saveData();
            reopenBaseGui(player);
        } else {
            player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "This action has expired.");
        }
    }

    public void cancelMailAction(Player player) {
        if (mailSendSteps.containsKey(player.getUniqueId())) {
            mailSendSteps.remove(player.getUniqueId());
            stepData.remove(player.getUniqueId() + "_recipient");
            stepData.remove(player.getUniqueId() + "_message");
            player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Mail action cancelled.");
            new BukkitRunnable() {
                @Override
                public void run() {
                    reopenBaseGui(player);
                }
            }.runTask(plugin);
        } else {
            player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "This action has expired.");
        }
    }

    public void cancelGiftAction(Player player) {
        if (giftSendSteps.containsKey(player.getUniqueId())) {
            giftSendSteps.remove(player.getUniqueId());
            stepData.remove(player.getUniqueId() + "_recipient");
            stepData.remove(player.getUniqueId() + "_gift");
            player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Gift action cancelled.");
            new BukkitRunnable() {
                @Override
                public void run() {
                    reopenBaseGui(player);
                }
            }.runTask(plugin);
        } else {
            player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "This action has expired.");
        }
    }

    public void cancelCurrentAction(Player player) {
        if (isInMailProcess(player)) {
            cancelMailAction(player);
        } else if (isInGiftProcess(player)) {
            cancelGiftAction(player);
        } else if (favoriteAddSteps.containsKey(player.getUniqueId())) {
            favoriteAddSteps.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Favorite action cancelled.");
            new BukkitRunnable() {
                @Override
                public void run() {
                    reopenBaseGui(player);
                }
            }.runTask(plugin);
        }
    }

    private void sendConfirmationMessage(Player player, String message, String confirmCommand, String cancelCommand) {
        TextComponent confirmMessage = new TextComponent(ChatColor.YELLOW + message + " " + ChatColor.GREEN + "[Click to Confirm]");
        confirmMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, confirmCommand));
        TextComponent cancelMessage = new TextComponent(ChatColor.RED + " [Click to Cancel]");
        cancelMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cancelCommand));
        player.spigot().sendMessage(confirmMessage, cancelMessage);
    }

    private ItemStack createMailItem(String sender, String message) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + message);
        lore.add(ChatColor.GREEN + "Left-Click to Reply");
        lore.add(ChatColor.RED + "Shift-Right-Click to Delete");
        return createPlayerHead(sender, ChatColor.YELLOW + "Mail from " + sender, lore);
    }

    private ItemStack createGiftItem(String sender, ItemStack item) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + String.valueOf(item.getAmount()) + " x " + item.getType().name());
        lore.add(ChatColor.GREEN + "Left-Click to Claim");
        lore.add(ChatColor.RED + "Shift-Right-Click to Delete");
        lore.add(serializeItemMap(item.serialize())); // Add serialized item data as the last element in the lore
        return createPlayerHead(sender, ChatColor.YELLOW + "Gift from " + sender, lore);
    }




    private boolean containsMailItem(List<ItemStack> mails, String sender, String message) {
        for (ItemStack mail : mails) {
            ItemMeta meta = mail.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.hasLore()) {
                if (meta.getDisplayName().equals(ChatColor.YELLOW + "Mail from " + sender) && meta.getLore().get(0).equals(message)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void openMailInbox(Player player, int page) {
        Inventory mailInbox = Bukkit.createInventory(null, 45, "Mail Inbox - Page " + (page + 1));
        List<ItemStack> mails = mailInboxes.get(player.getUniqueId());
        if (mails != null) {
            for (int i = page * 36; i < mails.size() && i < (page + 1) * 36; i++) {
                mailInbox.addItem(mails.get(i));
            }
        }
        addNavigationButtons(mailInbox, page, mails != null ? mails.size() : 0);
        player.openInventory(mailInbox);
    }


    public void openGiftInbox(Player player, int page) {
        Inventory giftInbox = Bukkit.createInventory(null, 45, "Gift Inbox - Page " + (page + 1));
        List<ItemStack> gifts = giftInboxes.get(player.getUniqueId());
        if (gifts != null) {
            for (int i = page * 36; i < gifts.size() && i < (page + 1) * 36; i++) {
                giftInbox.addItem(gifts.get(i));
            }
        }
        addNavigationButtons(giftInbox, page, gifts != null ? gifts.size() : 0);
        player.openInventory(giftInbox);
    }


    public void openFavoritesList(Player player, int page) {
        Inventory favoritesList = Bukkit.createInventory(null, 45, "Favorites List - Page " + (page + 1));
        List<UUID> favoriteIds = favorites.get(player.getUniqueId());
        if (favoriteIds != null) {
            for (int i = page * 36; i < favoriteIds.size() && i < (page + 1) * 36; i++) {
                OfflinePlayer favorite = Bukkit.getOfflinePlayer(favoriteIds.get(i));
                ItemStack favoriteItem = createPlayerHead(favorite.getName(), ChatColor.YELLOW + favorite.getName(), Arrays.asList(
                        ChatColor.GREEN + "Left-Click to Message",
                        ChatColor.RED + "Shift-Right-Click to Remove"
                ));
                favoritesList.addItem(favoriteItem);
            }
        }
        addNavigationButtons(favoritesList, page, favoriteIds != null ? favoriteIds.size() : 0);
        player.openInventory(favoritesList);
    }


    private void addNavigationButtons(Inventory inventory, int page, int totalItems) {
        ItemStack nextPage = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextPage.getItemMeta();
        nextMeta.setDisplayName("Next Page");
        nextPage.setItemMeta(nextMeta);

        ItemStack prevPage = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevPage.getItemMeta();
        prevMeta.setDisplayName("Previous Page");
        prevPage.setItemMeta(prevMeta);

        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("Back to Main Menu");
        backButton.setItemMeta(backMeta);

        ItemStack clearButton = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta clearMeta = clearButton.getItemMeta();
        clearMeta.setDisplayName("Clear All");
        clearButton.setItemMeta(clearMeta);

        if (page > 0) {
            inventory.setItem(36, prevPage);
        }
        if (totalItems > (page + 1) * 36) {
            inventory.setItem(44, nextPage);
        }
        inventory.setItem(40, backButton);
        inventory.setItem(41, clearButton);
    }


    private ItemStack getPlayerHead(UUID playerId, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerId));
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    public void handleInboxClick(Player player, ItemStack item, boolean isShiftRightClick, int page) {
        if (item != null && item.getType() == Material.PLAYER_HEAD) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.hasLore()) {
                String senderName = ChatColor.stripColor(meta.getDisplayName().substring(10)); // "Mail from " length
                List<String> lore = meta.getLore();
                if (lore != null && !lore.isEmpty()) {
                    if (!isShiftRightClick) {
                        String message = lore.get(0);
                        player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Replying to " + ChatColor.YELLOW + senderName + ChatColor.GRAY + ": " + message);
                        setMailSendStep(player, MailSendStep.MESSAGE);
                        stepData.put(player.getUniqueId() + "_recipient", Bukkit.getOfflinePlayer(senderName).getUniqueId());
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.closeInventory();
                            }
                        }.runTask(plugin);
                        player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Enter the message you want to send to " + ChatColor.YELLOW + senderName + ChatColor.GRAY + ":");
                        player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                    } else {
                        mailInboxes.get(player.getUniqueId()).remove(item);
                        player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "Deleting mail from " + senderName + ".");
                        openMailInbox(player, page);
                    }
                }
            }
        }
    }


    public void handleGiftInboxClick(Player player, ItemStack item, boolean isShiftRightClick, int page) {
        if (item != null && item.getType() == Material.PLAYER_HEAD) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.hasLore()) {
                String senderName = ChatColor.stripColor(meta.getDisplayName().substring(10)); // "Gift from " length
                List<String> lore = meta.getLore();
                if (lore != null && !lore.isEmpty()) {
                    if (!isShiftRightClick) {
                        String serializedData = lore.get(lore.size() - 1);
                        ItemStack giftItem = ItemStack.deserialize(deserializeItemMap(serializedData));
                        player.getInventory().addItem(giftItem);
                        giftInboxes.get(player.getUniqueId()).remove(item);
                        player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Claiming gift from " + ChatColor.YELLOW + senderName + ChatColor.GRAY + ".");
                        openGiftInbox(player, page);
                    } else {
                        giftInboxes.get(player.getUniqueId()).remove(item);
                        player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + "Deleting gift from " + ChatColor.YELLOW + senderName + ChatColor.RED + ".");
                        openGiftInbox(player, page);
                    }
                }
            }
        }
    }


    public void handleFavoritesClick(Player player, ItemStack item, boolean isShiftRightClick, int page) {
        if (item != null && item.getType() == Material.PLAYER_HEAD) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String favoriteName = ChatColor.stripColor(meta.getDisplayName());
                OfflinePlayer favorite = Bukkit.getOfflinePlayer(favoriteName);
                if (!isShiftRightClick) {
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Sending a message to " + ChatColor.YELLOW + favorite.getName());
                    setMailSendStep(player, MailSendStep.MESSAGE);
                    stepData.put(player.getUniqueId() + "_recipient", favorite.getUniqueId());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.closeInventory();
                        }
                    }.runTask(plugin);
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "Enter the message you want to send to " + ChatColor.YELLOW + favorite.getName() + ChatColor.GRAY + ":");
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.GRAY + "You can type 'cancel' to return to the main menu.");
                } else {
                    favorites.get(player.getUniqueId()).remove(favorite.getUniqueId());
                    player.sendMessage(ChatColor.GOLD + "[Mail] " + ChatColor.RED + favorite.getName() + " has been removed from your favorites.");
                    openFavoritesList(player, page);
                }
            }
        }
    }

    private ItemStack createPlayerHead(String playerName, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        ((org.bukkit.inventory.meta.SkullMeta) meta).setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        head.setItemMeta(meta);
        return head;
    }


    public void reopenBaseGui(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Inventory baseGui = Bukkit.createInventory(null, 27, "Mail Menu");

                // Add stained glass borders
                ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta glassMeta = grayGlass.getItemMeta();
                glassMeta.setDisplayName(" ");
                grayGlass.setItemMeta(glassMeta);
                for (int i = 0; i < 27; i++) {
                    if (i < 9 || i > 17) {
                        baseGui.setItem(i, grayGlass);
                    }
                }

                // Base menu items
                baseGui.setItem(10, createGuiItem(Material.BOOK, ChatColor.YELLOW + "Mail Inbox"));
                baseGui.setItem(11, createGuiItem(Material.ENDER_CHEST, ChatColor.YELLOW + "Gift Inbox"));
                baseGui.setItem(12, createGuiItem(Material.PAPER, ChatColor.YELLOW + "Send Mail"));
                baseGui.setItem(13, createGuiItem(Material.CHEST_MINECART, ChatColor.YELLOW + "Send Gift"));
                baseGui.setItem(14, createGuiItem(Material.NAME_TAG, ChatColor.YELLOW + "Add to Favorites"));
                baseGui.setItem(15, createGuiItem(Material.PLAYER_HEAD, ChatColor.YELLOW + "Favorites List"));
                baseGui.setItem(16, createGuiItem(Material.RED_BANNER, ChatColor.RED + "Close GUI"));

                player.openInventory(baseGui);
            }
        }.runTaskLater(plugin, 1L);
    }



    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public void saveData() {
        File dataFile = new File(plugin.getDataFolder(), "data.yml");
        FileConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Save mail inboxes
        for (UUID playerId : mailInboxes.keySet()) {
            dataConfig.set("mailInboxes." + playerId.toString(), mailInboxes.get(playerId));
        }

        // Save gift inboxes
        for (UUID playerId : giftInboxes.keySet()) {
            dataConfig.set("giftInboxes." + playerId.toString(), giftInboxes.get(playerId));
        }

        // Save favorites
        for (UUID playerId : favorites.keySet()) {
            List<String> favoriteIds = new ArrayList<>();
            for (UUID id : favorites.get(playerId)) {
                favoriteIds.add(id.toString());
            }
            dataConfig.set("favorites." + playerId.toString(), favoriteIds);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadData() {
        File dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            plugin.saveResource("data.yml", false);
        }
        FileConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Ensure the sections are present in the configuration
        if (!dataConfig.contains("mailInboxes")) {
            dataConfig.createSection("mailInboxes");
        }
        if (!dataConfig.contains("giftInboxes")) {
            dataConfig.createSection("giftInboxes");
        }
        if (!dataConfig.contains("favorites")) {
            dataConfig.createSection("favorites");
        }

        // Load mail inboxes
        ConfigurationSection mailSection = dataConfig.getConfigurationSection("mailInboxes");
        if (mailSection != null) {
            for (String playerId : mailSection.getKeys(false)) {
                List<ItemStack> mails = (List<ItemStack>) mailSection.getList(playerId);
                mailInboxes.put(UUID.fromString(playerId), mails);
            }
        }

        // Load gift inboxes
        ConfigurationSection giftSection = dataConfig.getConfigurationSection("giftInboxes");
        if (giftSection != null) {
            for (String playerId : giftSection.getKeys(false)) {
                List<ItemStack> gifts = (List<ItemStack>) giftSection.getList(playerId);
                giftInboxes.put(UUID.fromString(playerId), gifts);
            }
        }

        // Load favorites
        ConfigurationSection favoriteSection = dataConfig.getConfigurationSection("favorites");
        if (favoriteSection != null) {
            for (String playerId : favoriteSection.getKeys(false)) {
                List<String> favoriteIds = favoriteSection.getStringList(playerId);
                List<UUID> favoritesList = new ArrayList<>();
                for (String id : favoriteIds) {
                    favoritesList.add(UUID.fromString(id));
                }
                favorites.put(UUID.fromString(playerId), favoritesList);
            }
        }
    }


    private String serializeItemMap(Map<String, Object> itemMap) {
        return itemMap.toString();
    }

    private Map<String, Object> deserializeItemMap(String serializedData) {
        // Implement deserialization logic here
        return new HashMap<>();
    }


    public void clearMailInbox(UUID playerId) {
        mailInboxes.remove(playerId);
        saveData();
    }

    public void clearGiftInbox(UUID playerId) {
        giftInboxes.remove(playerId);
        saveData();
    }
}
