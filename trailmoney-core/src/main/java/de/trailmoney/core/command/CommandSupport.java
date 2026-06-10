package de.trailmoney.core.command;

import de.trailmoney.api.account.Account;
import de.trailmoney.api.money.Money;
import de.trailmoney.api.transaction.TransactionReason;
import de.trailmoney.api.transaction.TransactionResult;
import de.trailmoney.core.TrailMoneyPlugin;
import de.trailmoney.core.money.MoneyFormatter;
import de.trailmoney.core.money.MoneyParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class CommandSupport {
    private CommandSupport() {
    }

    static void success(TrailMoneyPlugin plugin, CommandSender sender, String message) {
        sender.sendMessage(Component.text(plugin.settings().messagePrefix() + " ", NamedTextColor.GRAY)
            .append(Component.text(message, NamedTextColor.GREEN)));
    }

    static void error(TrailMoneyPlugin plugin, CommandSender sender, String message) {
        sender.sendMessage(Component.text(plugin.settings().messagePrefix() + " ", NamedTextColor.GRAY)
            .append(Component.text(message, NamedTextColor.RED)));
    }

    static void info(TrailMoneyPlugin plugin, CommandSender sender, String message) {
        sender.sendMessage(Component.text(plugin.settings().messagePrefix() + " ", NamedTextColor.GRAY)
            .append(Component.text(message, NamedTextColor.YELLOW)));
    }

    static boolean requirePermission(TrailMoneyPlugin plugin, CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("trailmoney.admin")) {
            return true;
        }
        error(plugin, sender, "You do not have permission: " + permission);
        return false;
    }

    static Money parseAmount(TrailMoneyPlugin plugin, String raw) {
        return MoneyParser.parse(raw, plugin.settings().defaultCurrency());
    }

    static String format(Money money) {
        return MoneyFormatter.format(money);
    }

    @SuppressWarnings("deprecation")
    static OfflinePlayer resolveOfflinePlayer(String input) {
        try {
            UUID uuid = UUID.fromString(input);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            Player onlinePlayer = Bukkit.getPlayerExact(input);
            if (onlinePlayer != null) {
                return onlinePlayer;
            }
            return Bukkit.getOfflinePlayerIfCached(input);
        }
    }

    static void unknownPlayer(TrailMoneyPlugin plugin, CommandSender sender, String input) {
        error(plugin, sender, "Unknown offline player: " + input + ". Use a cached player name or UUID.");
    }

    static String displayName(OfflinePlayer player, String fallback) {
        String name = player.getName();
        return name == null ? fallback : name;
    }

    static String displayName(Account account) {
        if (account.displayName() != null && !account.displayName().isBlank()) {
            return account.displayName();
        }
        return account.id().value();
    }

    static TransactionReason reason(CommandSender sender, String key) {
        return new TransactionReason(key, null, actorName(sender));
    }

    static String actorName(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getUniqueId().toString();
        }
        return sender.getName();
    }

    static List<String> onlinePlayerSuggestions(String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                suggestions.add(player.getName());
            }
        }
        return suggestions;
    }

    static void sendTransactionResult(TrailMoneyPlugin plugin, CommandSender sender, TransactionResult result, String successMessage) {
        if (result.successful()) {
            success(plugin, sender, successMessage);
            return;
        }
        error(plugin, sender, result.messageOptional().orElse("Transaction failed: " + result.code()));
    }
}
