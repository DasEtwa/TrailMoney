package de.trailmoney.api.money;

import de.trailmoney.api.account.Account;

import java.util.Objects;

public record BalanceEntry(Account account, Money balance) {
    public BalanceEntry {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(balance, "balance");
    }
}
