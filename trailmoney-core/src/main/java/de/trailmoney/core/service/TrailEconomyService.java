package de.trailmoney.core.service;

import de.trailmoney.api.EconomyService;
import de.trailmoney.api.account.Account;
import de.trailmoney.api.account.AccountId;
import de.trailmoney.api.event.AccountCreateEvent;
import de.trailmoney.api.event.BalanceChangeEvent;
import de.trailmoney.api.event.TransactionPostEvent;
import de.trailmoney.api.event.TransactionPreEvent;
import de.trailmoney.api.money.BalanceEntry;
import de.trailmoney.api.money.Currency;
import de.trailmoney.api.money.Money;
import de.trailmoney.api.transaction.Transaction;
import de.trailmoney.api.transaction.TransactionReason;
import de.trailmoney.api.transaction.TransactionResult;
import de.trailmoney.api.transaction.TransactionResultCode;
import de.trailmoney.api.transaction.TransactionStatus;
import de.trailmoney.core.config.TrailMoneySettings;
import de.trailmoney.core.hook.LuckPermsHook;
import de.trailmoney.core.storage.AccountCreationResult;
import de.trailmoney.core.storage.EconomyStorage;
import de.trailmoney.core.storage.StorageException;
import de.trailmoney.core.storage.StorageMutationResult;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class TrailEconomyService implements EconomyService {
    private final Plugin plugin;
    private final EconomyStorage storage;
    private final ExecutorService executor;
    private TrailMoneySettings settings;
    private LuckPermsHook luckPermsHook;

    public TrailEconomyService(Plugin plugin, EconomyStorage storage, TrailMoneySettings settings) {
        this.plugin = plugin;
        this.storage = storage;
        this.settings = settings;
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "TrailMoney-Storage");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void updateSettings(TrailMoneySettings settings) {
        this.settings = settings;
    }

    public void updateLuckPermsHook(LuckPermsHook luckPermsHook) {
        this.luckPermsHook = luckPermsHook;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    @Override
    public Currency defaultCurrency() {
        return settings.defaultCurrency();
    }

    @Override
    public Account getOrCreatePlayerAccount(UUID playerUuid, String playerName) {
        AccountCreationResult result = storage.getOrCreatePlayerAccount(playerUuid, playerName, startBalance(playerUuid));
        if (result.created()) {
            Bukkit.getPluginManager().callEvent(new AccountCreateEvent(result.account()));
        }
        return result.account();
    }

    @Override
    public CompletionStage<Account> getOrCreatePlayerAccountAsync(UUID playerUuid, String playerName) {
        return supplyAsync(() -> {
            AccountCreationResult result = storage.getOrCreatePlayerAccount(playerUuid, playerName, startBalance(playerUuid));
            if (result.created()) {
                callEventSync(new AccountCreateEvent(result.account()));
            }
            return result.account();
        });
    }

    @Override
    public Optional<Account> findAccount(AccountId accountId) {
        return storage.findAccount(accountId);
    }

    @Override
    public CompletionStage<Optional<Account>> findAccountAsync(AccountId accountId) {
        return supplyAsync(() -> storage.findAccount(accountId));
    }

    @Override
    public Money getBalance(AccountId accountId, Currency currency) {
        return storage.getBalance(accountId, currency);
    }

    @Override
    public CompletionStage<Money> getBalanceAsync(AccountId accountId, Currency currency) {
        return supplyAsync(() -> storage.getBalance(accountId, currency));
    }

    @Override
    public TransactionResult deposit(AccountId target, Money amount, TransactionReason reason) {
        Transaction transaction = Transaction.pending(null, target, amount, reason);
        return executeMoneyMutation(transaction, amount, () -> storage.deposit(target, amount, settings.minBalance(), maxBalance(target), transaction));
    }

    @Override
    public CompletionStage<TransactionResult> depositAsync(AccountId target, Money amount, TransactionReason reason) {
        Transaction transaction = Transaction.pending(null, target, amount, reason);
        return executeMoneyMutationAsync(transaction, amount, () -> storage.deposit(target, amount, settings.minBalance(), maxBalance(target), transaction));
    }

    @Override
    public TransactionResult withdraw(AccountId source, Money amount, TransactionReason reason) {
        Transaction transaction = Transaction.pending(source, null, amount, reason);
        return executeMoneyMutation(transaction, amount, () -> storage.withdraw(source, amount, settings.minBalance(), transaction));
    }

    @Override
    public CompletionStage<TransactionResult> withdrawAsync(AccountId source, Money amount, TransactionReason reason) {
        Transaction transaction = Transaction.pending(source, null, amount, reason);
        return executeMoneyMutationAsync(transaction, amount, () -> storage.withdraw(source, amount, settings.minBalance(), transaction));
    }

    @Override
    public TransactionResult transfer(AccountId source, AccountId target, Money amount, TransactionReason reason) {
        if (source.equals(target)) {
            return TransactionResult.failure(TransactionResultCode.INVALID_AMOUNT, null, "Source and target accounts are the same");
        }
        Transaction transaction = Transaction.pending(source, target, amount, reason);
        return executeMoneyMutation(transaction, amount, () -> storage.transfer(source, target, amount, settings.minBalance(), maxBalance(target), transaction));
    }

    @Override
    public CompletionStage<TransactionResult> transferAsync(AccountId source, AccountId target, Money amount, TransactionReason reason) {
        if (source.equals(target)) {
            return CompletableFuture.completedFuture(TransactionResult.failure(TransactionResultCode.INVALID_AMOUNT, null, "Source and target accounts are the same"));
        }
        Transaction transaction = Transaction.pending(source, target, amount, reason);
        return executeMoneyMutationAsync(transaction, amount, () -> storage.transfer(source, target, amount, settings.minBalance(), maxBalance(target), transaction));
    }

    @Override
    public TransactionResult setBalance(AccountId accountId, Money amount, TransactionReason reason) {
        Transaction transaction = Transaction.pending(null, accountId, amount, reason);
        return executeMutation(transaction, () -> storage.setBalance(accountId, amount, settings.minBalance(), maxBalance(accountId), transaction));
    }

    @Override
    public CompletionStage<TransactionResult> setBalanceAsync(AccountId accountId, Money amount, TransactionReason reason) {
        Transaction transaction = Transaction.pending(null, accountId, amount, reason);
        return executeMutationAsync(transaction, () -> storage.setBalance(accountId, amount, settings.minBalance(), maxBalance(accountId), transaction));
    }

    @Override
    public List<BalanceEntry> topBalances(Currency currency, int limit) {
        return storage.topBalances(currency, limit);
    }

    @Override
    public CompletionStage<List<BalanceEntry>> topBalancesAsync(Currency currency, int limit) {
        return supplyAsync(() -> storage.topBalances(currency, limit));
    }

    private TransactionResult executeMoneyMutation(Transaction transaction, Money amount, Supplier<StorageMutationResult> operation) {
        if (!amount.isPositive()) {
            return TransactionResult.failure(TransactionResultCode.INVALID_AMOUNT, transaction.withStatus(TransactionStatus.FAILED), "Amount must be positive");
        }
        return executeMutation(transaction, operation);
    }

    private CompletionStage<TransactionResult> executeMoneyMutationAsync(Transaction transaction, Money amount, Supplier<StorageMutationResult> operation) {
        if (!amount.isPositive()) {
            return CompletableFuture.completedFuture(TransactionResult.failure(TransactionResultCode.INVALID_AMOUNT, transaction.withStatus(TransactionStatus.FAILED), "Amount must be positive"));
        }
        return executeMutationAsync(transaction, operation);
    }

    private Money startBalance(UUID playerUuid) {
        if (luckPermsHook == null) {
            return settings.startBalance();
        }
        return luckPermsHook.startBalance(playerUuid);
    }

    private Long maxBalance(AccountId accountId) {
        UUID playerUuid = playerUuid(accountId);
        if (luckPermsHook == null || playerUuid == null) {
            return settings.maxBalanceMinorUnits();
        }
        return luckPermsHook.maxBalanceMinorUnits(playerUuid);
    }

    private UUID playerUuid(AccountId accountId) {
        if (accountId == null || accountId.type() != de.trailmoney.api.account.AccountType.PLAYER) {
            return null;
        }
        String value = accountId.value();
        String prefix = "player:";
        if (!value.startsWith(prefix)) {
            return null;
        }
        try {
            return UUID.fromString(value.substring(prefix.length()));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private TransactionResult executeMutation(Transaction transaction, Supplier<StorageMutationResult> operation) {
        if (!transaction.amount().currency().key().equals(settings.defaultCurrency().key())) {
            return TransactionResult.failure(
                TransactionResultCode.INVALID_AMOUNT,
                transaction.withStatus(TransactionStatus.FAILED),
                "Only the configured default currency is supported in the MVP"
            );
        }

        TransactionPreEvent preEvent = new TransactionPreEvent(transaction);
        Bukkit.getPluginManager().callEvent(preEvent);
        if (preEvent.isCancelled()) {
            Transaction cancelled = transaction.withStatus(TransactionStatus.CANCELLED);
            TransactionResult result = TransactionResult.failure(
                TransactionResultCode.CANCELLED,
                cancelled,
                preEvent.cancellationReason() == null ? "Transaction cancelled by event" : preEvent.cancellationReason()
            );
            Bukkit.getPluginManager().callEvent(new TransactionPostEvent(result));
            return result;
        }

        try {
            StorageMutationResult mutation = operation.get();
            TransactionResult result = mutation.code() == TransactionResultCode.SUCCESS
                ? TransactionResult.success(mutation.transaction())
                : TransactionResult.failure(mutation.code(), mutation.transaction(), mutation.message());

            if (result.successful()) {
                for (var change : mutation.changes()) {
                    Bukkit.getPluginManager().callEvent(new BalanceChangeEvent(
                        change.accountId(),
                        change.oldBalance(),
                        change.newBalance(),
                        mutation.transaction()
                    ));
                }
            }
            Bukkit.getPluginManager().callEvent(new TransactionPostEvent(result));
            return result;
        } catch (StorageException exception) {
            plugin.getLogger().log(Level.SEVERE, "Economy storage operation failed", exception);
            Transaction failed = transaction.withStatus(TransactionStatus.FAILED);
            TransactionResult result = TransactionResult.failure(TransactionResultCode.STORAGE_ERROR, failed, exception.getMessage());
            Bukkit.getPluginManager().callEvent(new TransactionPostEvent(result));
            return result;
        }
    }

    private CompletionStage<TransactionResult> executeMutationAsync(Transaction transaction, Supplier<StorageMutationResult> operation) {
        return supplyAsync(() -> {
            if (!transaction.amount().currency().key().equals(settings.defaultCurrency().key())) {
                return TransactionResult.failure(
                    TransactionResultCode.INVALID_AMOUNT,
                    transaction.withStatus(TransactionStatus.FAILED),
                    "Only the configured default currency is supported in the MVP"
                );
            }

            TransactionPreEvent preEvent = new TransactionPreEvent(transaction);
            callEventSync(preEvent);
            if (preEvent.isCancelled()) {
                Transaction cancelled = transaction.withStatus(TransactionStatus.CANCELLED);
                TransactionResult result = TransactionResult.failure(
                    TransactionResultCode.CANCELLED,
                    cancelled,
                    preEvent.cancellationReason() == null ? "Transaction cancelled by event" : preEvent.cancellationReason()
                );
                callEventSync(new TransactionPostEvent(result));
                return result;
            }

            try {
                StorageMutationResult mutation = operation.get();
                TransactionResult result = mutation.code() == TransactionResultCode.SUCCESS
                    ? TransactionResult.success(mutation.transaction())
                    : TransactionResult.failure(mutation.code(), mutation.transaction(), mutation.message());

                if (result.successful()) {
                    for (var change : mutation.changes()) {
                        callEventSync(new BalanceChangeEvent(
                            change.accountId(),
                            change.oldBalance(),
                            change.newBalance(),
                            mutation.transaction()
                        ));
                    }
                }
                callEventSync(new TransactionPostEvent(result));
                return result;
            } catch (StorageException exception) {
                plugin.getLogger().log(Level.SEVERE, "Economy storage operation failed", exception);
                Transaction failed = transaction.withStatus(TransactionStatus.FAILED);
                TransactionResult result = TransactionResult.failure(TransactionResultCode.STORAGE_ERROR, failed, exception.getMessage());
                callEventSync(new TransactionPostEvent(result));
                return result;
            }
        });
    }

    private <T> CompletionStage<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    private void callEventSync(Event event) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(event);
            return;
        }

        try {
            Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                Bukkit.getPluginManager().callEvent(event);
                return null;
            }).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new StorageException("Interrupted while firing Bukkit event", exception);
        } catch (ExecutionException exception) {
            throw new StorageException("Failed to fire Bukkit event", exception);
        }
    }
}
