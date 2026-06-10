package de.trailmoney.api.transaction;

import de.trailmoney.api.account.AccountId;
import de.trailmoney.api.money.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Transaction(
    UUID id,
    AccountId source,
    AccountId target,
    Money amount,
    TransactionReason reason,
    Instant timestamp,
    TransactionStatus status
) {
    public Transaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(status, "status");
    }

    public static Transaction pending(AccountId source, AccountId target, Money amount, TransactionReason reason) {
        return new Transaction(UUID.randomUUID(), source, target, amount, reason, Instant.now(), TransactionStatus.PENDING);
    }

    public Transaction withStatus(TransactionStatus newStatus) {
        return new Transaction(id, source, target, amount, reason, timestamp, newStatus);
    }
}
