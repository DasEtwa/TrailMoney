package de.trailmoney.core.storage;

import de.trailmoney.api.account.Account;

public record AccountCreationResult(Account account, boolean created) {
}
