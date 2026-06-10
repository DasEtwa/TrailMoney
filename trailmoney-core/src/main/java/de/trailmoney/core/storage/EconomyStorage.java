package de.trailmoney.core.storage;

import de.trailmoney.api.account.Account;
import de.trailmoney.api.account.AccountId;
import de.trailmoney.api.money.BalanceEntry;
import de.trailmoney.api.money.Currency;
import de.trailmoney.api.money.Money;
import de.trailmoney.api.transaction.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EconomyStorage extends AutoCloseable {
    void initialize();

    AccountCreationResult getOrCreatePlayerAccount(UUID playerUuid, String playerName, Money startBalance);

    Optional<Account> findAccount(AccountId accountId);

    Money getBalance(AccountId accountId, Currency currency);

    StorageMutationResult deposit(AccountId target, Money amount, Money minBalance, Long maxBalanceMinorUnits, Transaction transaction);

    StorageMutationResult withdraw(AccountId source, Money amount, Money minBalance, Transaction transaction);

    StorageMutationResult transfer(AccountId source, AccountId target, Money amount, Money minBalance, Long maxBalanceMinorUnits, Transaction transaction);

    StorageMutationResult setBalance(AccountId accountId, Money amount, Money minBalance, Long maxBalanceMinorUnits, Transaction transaction);

    List<BalanceEntry> topBalances(Currency currency, int limit);

    List<Transaction> recentTransactions(AccountId accountId, Currency currency, int limit);

    @Override
    void close();
}
