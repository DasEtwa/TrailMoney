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
import de.trailmoney.core.storage.AccountCreationResult;
import de.trailmoney.core.storage.EconomyStorage;
import de.trailmoney.core.storage.StorageException;
import de.trailmoney.core.storage.StorageMutationResult;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class TrailEconomyService implements EconomyService {
    private final Plugin plugin;
    private final EconomyStorage storage;
    private TrailMoneySettings settings;

    public TrailEconomyService(Plugin plugin, EconomyStorage storage, TrailMoneySettings settings) {
        this.plugin = plugin;
        this.storage = storage;
        this.settings = settings;
    }

    public void updateSettings(TrailMoneySettings settings) {
        this.settings = settings;
    }

    @Override
    public Currency defaultCurrency() {
        return settings.defaultCurrency();
    }

    @Override
    public Account getOrCreatePlayerAccount(UUID playerUuid, String playerName) {
        AccountCreationResult result = storage.getOrCreatePlayerAccount(playerUuid, playerName, settings.startBalance());
        if (result.created()) {
            Bukkit.getPluginManager().callEvent(new AccountCreateEvent(result.account()));
        }
        return result.account();
    }

    @Override
    public Optional<Account> findAccount(AccountId accountId) {
        return storage.findAccount(accountId);
    }

    @Override
    public Money getBalance(AccountId accountId, Currency currency) {
        return storage.getBalance(accountId, currency);
    }

    @Override
    public TransactionResult deposit(AccountId target, Money amount, TransactionReason reason) {
        Transaction transaction = Transaction.pending(null, target, amount, reason);
        return executeMoneyMutation(transaction, amount, () -> storage.deposit(target, amount, settings.minBalance(), settings.maxBalanceMinorUnits(), transaction));
    }

    @Override
    public TransactionResult withdraw(AccountId source, Money amount, TransactionReason reason) {
        Transaction transaction = Transaction.pending(source, null, amount, reason);
        return executeMoneyMutation(transaction, amount, () -> storage.withdraw(source, amount, settings.minBalance(), transaction));
    }

    @Override
    public TransactionResult transfer(AccountId source, AccountId target, Money amount, TransactionReason reason) {
        if (source.equals(target)) {
            return TransactionResult.failure(TransactionResultCode.INVALID_AMOUNT, null, "Source and target accounts are the same");
        }
        Transaction transaction = Transaction.pending(source, target, amount, reason);
        return executeMoneyMutation(transaction, amount, () -> storage.transfer(source, target, amount, settings.minBalance(), settings.maxBalanceMinorUnits(), transaction));
    }

    @Override
    public TransactionResult setBalance(AccountId accountId, Money amount, TransactionReason reason) {
        Transaction transaction = Transaction.pending(null, accountId, amount, reason);
        return executeMutation(transaction, () -> storage.setBalance(accountId, amount, settings.minBalance(), settings.maxBalanceMinorUnits(), transaction));
    }

    @Override
    public List<BalanceEntry> topBalances(Currency currency, int limit) {
        return storage.topBalances(currency, limit);
    }

    private TransactionResult executeMoneyMutation(Transaction transaction, Money amount, Supplier<StorageMutationResult> operation) {
        if (!amount.isPositive()) {
            return TransactionResult.failure(TransactionResultCode.INVALID_AMOUNT, transaction.withStatus(TransactionStatus.FAILED), "Amount must be positive");
        }
        return executeMutation(transaction, operation);
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
}
