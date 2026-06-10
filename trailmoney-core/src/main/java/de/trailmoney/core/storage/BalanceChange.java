package de.trailmoney.core.storage;

import de.trailmoney.api.account.AccountId;
import de.trailmoney.api.money.Money;

public record BalanceChange(AccountId accountId, Money oldBalance, Money newBalance) {
}
