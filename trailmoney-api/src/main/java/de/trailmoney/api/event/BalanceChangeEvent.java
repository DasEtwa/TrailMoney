package de.trailmoney.api.event;

import de.trailmoney.api.account.AccountId;
import de.trailmoney.api.money.Money;
import de.trailmoney.api.transaction.Transaction;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public class BalanceChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final AccountId accountId;
    private final Money oldBalance;
    private final Money newBalance;
    private final Transaction transaction;

    public BalanceChangeEvent(AccountId accountId, Money oldBalance, Money newBalance, Transaction transaction) {
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.oldBalance = Objects.requireNonNull(oldBalance, "oldBalance");
        this.newBalance = Objects.requireNonNull(newBalance, "newBalance");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
    }

    public AccountId accountId() {
        return accountId;
    }

    public Money oldBalance() {
        return oldBalance;
    }

    public Money newBalance() {
        return newBalance;
    }

    public Transaction transaction() {
        return transaction;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
