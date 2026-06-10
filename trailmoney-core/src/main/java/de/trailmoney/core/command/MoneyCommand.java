package de.trailmoney.core.command;

import de.trailmoney.api.account.Account;
import de.trailmoney.api.account.AccountId;
import de.trailmoney.api.money.Money;
import de.trailmoney.core.TrailMoneyPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MoneyCommand implements TabExecutor {
    private final TrailMoneyPlugin plugin;

    public MoneyCommand(TrailMoneyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                CommandSupport.error(plugin, sender, "Usage: /money <player>");
                return true;
            }
            if (!CommandSupport.requirePermission(plugin, sender, "trailmoney.balance")) {
                return true;
            }
            Account account = plugin.economyService().getOrCreatePlayerAccount(player.getUniqueId(), player.getName());
            Money balance = plugin.economyService().getBalance(account.id(), plugin.settings().defaultCurrency());
            CommandSupport.info(plugin, sender, "Your balance: " + CommandSupport.format(balance));
            return true;
        }

        if (args.length == 1) {
            if (!CommandSupport.requirePermission(plugin, sender, "trailmoney.balance.others")) {
                return true;
            }
            OfflinePlayer target = CommandSupport.resolveOfflinePlayer(args[0]);
            if (target == null) {
                CommandSupport.unknownPlayer(plugin, sender, args[0]);
                return true;
            }
            String displayName = CommandSupport.displayName(target, args[0]);
            Account account = plugin.economyService().getOrCreatePlayerAccount(target.getUniqueId(), displayName);
            Money balance = plugin.economyService().getBalance(AccountId.player(target.getUniqueId()), plugin.settings().defaultCurrency());
            CommandSupport.info(plugin, sender, CommandSupport.displayName(account) + "'s balance: " + CommandSupport.format(balance));
            return true;
        }

        CommandSupport.error(plugin, sender, "Usage: /money [player]");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 1 && CommandSupport.hasPermission(sender, "trailmoney.balance.others")) {
            return CommandSupport.onlinePlayerSuggestions(args[0]);
        }
        return List.of();
    }
}
