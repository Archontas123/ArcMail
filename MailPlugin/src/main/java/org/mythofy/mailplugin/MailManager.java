package org.mythofy.mailplugin;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MailManager {

    private final MailPlugin plugin;
    private final Map<UUID, MailSendStep> mailSendSteps = new HashMap<>();
    private final Map<UUID, FavoriteAddStep> favoriteAddSteps = new HashMap<>();
    private final Map<UUID, GiftSendStep> giftSendSteps = new HashMap<>();
    private final Map<UUID, Object> stepData = new HashMap<>();

    public MailManager(MailPlugin plugin) {
        this.plugin = plugin;
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
                    player.sendMessage("You cannot send mail to yourself. Enter a different player name:");
                    return;
                }
                OfflinePlayer recipient = Bukkit.getOfflinePlayer(message);
                if (recipient != null && recipient.hasPlayedBefore()) {
                    stepData.put(player.getUniqueId(), recipient);
                    player.sendMessage("Recipient found: " + recipient.getName());
                    sendConfirmationMessage(player, "Confirm recipient: " + recipient.getName() + "?", "/confirmmailrecipient", "/cancelmailaction");
                } else {
                    player.sendMessage("Player not found or has never logged in. Please enter a valid player name:");
                }
                break;
            case MESSAGE:
                stepData.put(player.getUniqueId(), message);
                player.sendMessage("You typed: " + message);
                sendConfirmationMessage(player, "Confirm message?", "/confirmmailmessage", "/cancelmailaction");
                break;
        }
    }

    private void handleFavoriteAddInput(Player player, String message, FavoriteAddStep step) {
        if (step == FavoriteAddStep.RECIPIENT) {
            if (message.equalsIgnoreCase(player.getName())) {
                player.sendMessage("You cannot add yourself to favorites. Enter a different player name:");
                return;
            }
            OfflinePlayer favorite = Bukkit.getOfflinePlayer(message);
            if (favorite != null && favorite.hasPlayedBefore()) {
                player.sendMessage("Added " + favorite.getName() + " to favorites.");
                favoriteAddSteps.remove(player.getUniqueId());
            } else {
                player.sendMessage("Player not found or has never logged in. Please enter a valid player name:");
            }
        }
    }

    private void handleGiftSendInput(Player player, String message, GiftSendStep step) {
        switch (step) {
            case RECIPIENT:
                if (message.equalsIgnoreCase(player.getName())) {
                    player.sendMessage("You cannot send a gift to yourself. Enter a different player name:");
                    return;
                }
                OfflinePlayer recipient = Bukkit.getOfflinePlayer(message);
                if (recipient != null && recipient.hasPlayedBefore()) {
                    stepData.put(player.getUniqueId(), recipient);
                    player.sendMessage("Recipient found: " + recipient.getName());
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item != null) {
                        player.sendMessage("You are about to send: " + item.getAmount() + " x " + item.getType().name());
                        stepData.put(player.getUniqueId(), item);
                        sendConfirmationMessage(player, "Confirm gift send of " + item.getAmount() + " x " + item.getType().name() + "?", "/confirmgiftsend", "/cancelgiftaction");
                    } else {
                        player.sendMessage("You are not holding any item. Please hold an item and re-enter the player name:");
                        setGiftSendStep(player, GiftSendStep.RECIPIENT);
                    }
                } else {
                    player.sendMessage("Player not found or has never logged in. Please enter a valid player name:");
                }
                break;
        }
    }

    public void confirmMailRecipient(Player player) {
        if (mailSendSteps.get(player.getUniqueId()) == MailSendStep.RECIPIENT) {
            player.sendMessage("Enter the message you want to send:");
            setMailSendStep(player, MailSendStep.MESSAGE);
        }
    }

    public void confirmMailMessage(Player player) {
        if (mailSendSteps.get(player.getUniqueId()) == MailSendStep.MESSAGE) {
            OfflinePlayer recipient = (OfflinePlayer) stepData.get(player.getUniqueId());
            String message = (String) stepData.get(player.getUniqueId());
            if (recipient.isOnline()) {
                Player onlineRecipient = (Player) recipient;
                onlineRecipient.sendMessage("You have received a mail from " + player.getName() + ": " + message);
            }
            player.sendMessage("Message sent!");
            mailSendSteps.remove(player.getUniqueId());
            stepData.remove(player.getUniqueId());
        }
    }

    public void confirmGiftSend(Player player) {
        if (giftSendSteps.get(player.getUniqueId()) == GiftSendStep.CONFIRM_GIFT) {
            OfflinePlayer recipient = (OfflinePlayer) stepData.get(player.getUniqueId());
            ItemStack item = (ItemStack) stepData.get(player.getUniqueId());
            if (recipient.isOnline()) {
                Player onlineRecipient = (Player) recipient;
                onlineRecipient.getInventory().addItem(item);
            }
            player.getInventory().setItemInMainHand(null);
            player.sendMessage("Gift sent!");
            giftSendSteps.remove(player.getUniqueId());
            stepData.remove(player.getUniqueId());
        }
    }

    public void cancelMailAction(Player player) {
        mailSendSteps.remove(player.getUniqueId());
        stepData.remove(player.getUniqueId());
        player.sendMessage("Mail action cancelled.");
    }

    public void cancelGiftAction(Player player) {
        giftSendSteps.remove(player.getUniqueId());
        stepData.remove(player.getUniqueId());
        player.sendMessage("Gift action cancelled.");
    }

    private void sendConfirmationMessage(Player player, String message, String confirmCommand, String cancelCommand) {
        TextComponent confirmMessage = new TextComponent(message + " [Click to Confirm]");
        confirmMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, confirmCommand));
        TextComponent cancelMessage = new TextComponent(" [Click to Cancel]");
        cancelMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cancelCommand));
        player.spigot().sendMessage(confirmMessage, cancelMessage);
    }
}
