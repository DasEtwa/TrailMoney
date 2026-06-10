package de.trailmoney.core.command;

import de.trailmoney.api.account.Account;
import de.trailmoney.api.money.Money;
import de.trailmoney.api.transaction.TransactionResult;
import de.trailmoney.core.TrailMoneyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PayCommand implements TabExecutor {
    private final TrailMoneyPlugin plugin;

    public PayCommand(TrailMoneyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            CommandSupport.error(plugin, sender, "Only players can use /pay.");
            return true;
        }

        if (args.length != 2) {
            CommandSupport.error(plugin, sender, "Usage: /pay <player> <amount>");
            return true;
        }

        OfflinePlayer target = CommandSupport.resolveOfflinePlayer(args[0]);
        if (target.getUniqueId().equals(player.getUniqueId())) {
            CommandSupport.error(plugin, sender, "You cannot pay yourself.");
            return true;
        }

        Money amount;
        try {
            amount = CommandSupport.parseAmount(plugin, args[1]);
        } catch (RuntimeException exception) {
            CommandSupport.error(plugin, sender, "Invalid amount: " + args[1]);
            return true;
        }

        Account sourceAccount = plugin.economyService().getOrCreatePlayerAccount(player.getUniqueId(), player.getName());
        Account targetAccount = plugin.economyService().getOrCreatePlayerAccount(target.getUniqueId(), CommandSupport.displayName(target, args[0]));

        TransactionResult result = plugin.economyService().transfer(
            sourceAccount.id(),
            targetAccount.id(),
            amount,
            CommandSupport.reason(sender, "player_pay")
        );

        if (!result.successful()) {
            CommandSupport.sendTransactionResult(plugin, sender, result, "");
            return true;
        }

        String targetName = CommandSupport.displayName(targetAccount);
        CommandSupport.success(plugin, sender, "Paid " + targetName + " " + CommandSupport.format(amount) + ".");

        Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
        if (onlineTarget != null) {
            CommandSupport.success(plugin, onlineTarget, player.getName() + " paid you " + CommandSupport.format(amount) + ".");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 1) {
            return CommandSupport.onlinePlayerSuggestions(args[0]);
        }
        if (args.length == 2) {
            return List.of("1", "10", "100");
        }
        return List.of();
    }
}
