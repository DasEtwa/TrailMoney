package de.trailmoney.core.command;

import de.trailmoney.api.account.AccountId;
import de.trailmoney.api.account.Account;
import de.trailmoney.api.money.BalanceEntry;
import de.trailmoney.api.money.Money;
import de.trailmoney.api.transaction.Transaction;
import de.trailmoney.api.transaction.TransactionResult;
import de.trailmoney.core.TrailMoneyPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class EcoCommand implements TabExecutor {
    private static final List<String> SUBCOMMANDS = List.of("give", "take", "set", "reload", "top", "history");

    private final TrailMoneyPlugin plugin;

    public EcoCommand(TrailMoneyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "give" -> handleMoneyMutation(sender, args, "trailmoney.eco.give", "admin_give", MutationType.GIVE);
            case "take" -> handleMoneyMutation(sender, args, "trailmoney.eco.take", "admin_take", MutationType.TAKE);
            case "set" -> handleMoneyMutation(sender, args, "trailmoney.eco.set", "admin_set", MutationType.SET);
            case "reload" -> handleReload(sender);
            case "top" -> handleTop(sender, args);
            case "history" -> handleHistory(sender, args);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream()
                .filter(value -> value.startsWith(prefix))
                .toList();
        }
        if (args.length == 2 && List.of("give", "take", "set").contains(args[0].toLowerCase(Locale.ROOT))) {
            return CommandSupport.onlinePlayerSuggestions(args[1]);
        }
        if (args.length == 3 && List.of("give", "take", "set").contains(args[0].toLowerCase(Locale.ROOT))) {
            return List.of("1", "10", "100");
        }
        if (args.length == 2 && "top".equalsIgnoreCase(args[0])) {
            return List.of("10", "25", "50");
        }
        if (args.length == 2 && "history".equalsIgnoreCase(args[0])) {
            return CommandSupport.onlinePlayerSuggestions(args[1]);
        }
        if (args.length == 3 && "history".equalsIgnoreCase(args[0])) {
            return List.of("5", "10", "25");
        }
        return List.of();
    }

    private boolean handleMoneyMutation(CommandSender sender, String[] args, String permission, String reasonKey, MutationType mutationType) {
        if (!CommandSupport.requirePermission(plugin, sender, permission)) {
            return true;
        }
        if (args.length != 3) {
            CommandSupport.error(plugin, sender, "Usage: /eco " + args[0] + " <player> <amount>");
            return true;
        }

        Money amount;
        try {
            amount = CommandSupport.parseAmount(plugin, args[2]);
        } catch (RuntimeException exception) {
            CommandSupport.error(plugin, sender, "Invalid amount: " + args[2]);
            return true;
        }

        OfflinePlayer target = CommandSupport.resolveOfflinePlayer(args[1]);
        if (target == null) {
            CommandSupport.unknownPlayer(plugin, sender, args[1]);
            return true;
        }
        Account account = plugin.economyService().getOrCreatePlayerAccount(target.getUniqueId(), CommandSupport.displayName(target, args[1]));

        TransactionResult result = switch (mutationType) {
            case GIVE -> plugin.economyService().deposit(account.id(), amount, CommandSupport.reason(sender, reasonKey));
            case TAKE -> plugin.economyService().withdraw(account.id(), amount, CommandSupport.reason(sender, reasonKey));
            case SET -> plugin.economyService().setBalance(account.id(), amount, CommandSupport.reason(sender, reasonKey));
        };

        CommandSupport.sendTransactionResult(
            plugin,
            sender,
            result,
            "Updated " + CommandSupport.displayName(account) + " by " + CommandSupport.format(amount) + "."
        );
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!CommandSupport.requirePermission(plugin, sender, "trailmoney.eco.reload")) {
            return true;
        }
        try {
            plugin.reloadTrailMoney();
            CommandSupport.success(plugin, sender, "Configuration reloaded.");
        } catch (RuntimeException exception) {
            CommandSupport.error(plugin, sender, "Reload failed: " + exception.getMessage());
        }
        return true;
    }

    private boolean handleHistory(CommandSender sender, String[] args) {
        if (!CommandSupport.requirePermission(plugin, sender, "trailmoney.eco.history")) {
            return true;
        }
        if (args.length < 2 || args.length > 3) {
            CommandSupport.error(plugin, sender, "Usage: /eco history <player> [limit]");
            return true;
        }

        OfflinePlayer target = CommandSupport.resolveOfflinePlayer(args[1]);
        if (target == null) {
            CommandSupport.unknownPlayer(plugin, sender, args[1]);
            return true;
        }

        int limit = 10;
        if (args.length == 3) {
            try {
                limit = Math.min(50, Math.max(1, Integer.parseInt(args[2])));
            } catch (NumberFormatException exception) {
                CommandSupport.error(plugin, sender, "Invalid limit: " + args[2]);
                return true;
            }
        }

        AccountId accountId = AccountId.player(target.getUniqueId());
        String displayName = CommandSupport.displayName(target, args[1]);
        plugin.economyService().recentTransactionsAsync(accountId, plugin.settings().defaultCurrency(), limit)
            .whenComplete((transactions, throwable) -> {
                if (!plugin.isEnabled()) {
                    return;
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> sendHistory(sender, accountId, displayName, transactions, throwable));
            });
        return true;
    }

    private boolean handleTop(CommandSender sender, String[] args) {
        if (!CommandSupport.requirePermission(plugin, sender, "trailmoney.eco.top")) {
            return true;
        }

        int limit = plugin.settings().topLimit();
        if (args.length >= 2) {
            try {
                limit = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException exception) {
                CommandSupport.error(plugin, sender, "Invalid limit: " + args[1]);
                return true;
            }
        }

        List<BalanceEntry> entries = plugin.economyService().topBalances(plugin.settings().defaultCurrency(), limit);
        if (entries.isEmpty()) {
            CommandSupport.info(plugin, sender, "No balances found.");
            return true;
        }

        CommandSupport.info(plugin, sender, "Top balances:");
        int rank = 1;
        for (BalanceEntry entry : entries) {
            sender.sendMessage(rank + ". " + CommandSupport.displayName(entry.account()) + ": " + CommandSupport.format(entry.balance()));
            rank++;
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        CommandSupport.error(plugin, sender, "Usage: /eco <give|take|set|reload|top|history> [args]");
    }

    private void sendHistory(CommandSender sender, AccountId accountId, String displayName, List<Transaction> transactions, Throwable throwable) {
        if (throwable != null) {
            CommandSupport.error(plugin, sender, "Could not read transaction history: " + throwable.getMessage());
            return;
        }
        if (transactions.isEmpty()) {
            CommandSupport.info(plugin, sender, "No transactions found for " + displayName + ".");
            return;
        }

        CommandSupport.info(plugin, sender, "Recent transactions for " + displayName + ":");
        int rank = 1;
        for (Transaction transaction : transactions) {
            sender.sendMessage(rank + ". " + formatHistoryLine(accountId, transaction));
            rank++;
        }
    }

    private String formatHistoryLine(AccountId accountId, Transaction transaction) {
        String action = historyAction(accountId, transaction);
        String amount = switch (action) {
            case "OUT" -> "-" + CommandSupport.format(transaction.amount());
            case "IN" -> "+" + CommandSupport.format(transaction.amount());
            default -> CommandSupport.format(transaction.amount());
        };
        String actor = transaction.reason().actor() == null ? "unknown" : transaction.reason().actor();
        return transaction.status() + " " + action + " " + amount
            + " reason=" + transaction.reason().key()
            + " actor=" + actor
            + " id=" + transaction.id().toString().substring(0, 8);
    }

    private String historyAction(AccountId accountId, Transaction transaction) {
        if ("admin_set".equals(transaction.reason().key())) {
            return "SET";
        }
        if (accountId.equals(transaction.source())) {
            return "OUT";
        }
        if (accountId.equals(transaction.target())) {
            return "IN";
        }
        return "TX";
    }

    private enum MutationType {
        GIVE,
        TAKE,
        SET
    }
}
