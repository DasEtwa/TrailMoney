package de.trailmoney.api;

import de.trailmoney.api.account.Account;
import de.trailmoney.api.account.AccountId;
import de.trailmoney.api.money.BalanceEntry;
import de.trailmoney.api.money.Currency;
import de.trailmoney.api.money.Money;
import de.trailmoney.api.transaction.TransactionReason;
import de.trailmoney.api.transaction.TransactionResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EconomyService {
    Currency defaultCurrency();

    Account getOrCreatePlayerAccount(UUID playerUuid, String playerName);

    Optional<Account> findAccount(AccountId accountId);

    Money getBalance(AccountId accountId, Currency currency);

    TransactionResult deposit(AccountId target, Money amount, TransactionReason reason);

    TransactionResult withdraw(AccountId source, Money amount, TransactionReason reason);

    TransactionResult transfer(AccountId source, AccountId target, Money amount, TransactionReason reason);

    TransactionResult setBalance(AccountId accountId, Money amount, TransactionReason reason);

    List<BalanceEntry> topBalances(Currency currency, int limit);
}
