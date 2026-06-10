package de.trailmoney.core.storage;

import de.trailmoney.api.transaction.Transaction;
import de.trailmoney.api.transaction.TransactionResultCode;

import java.util.List;
import java.util.Objects;

public record StorageMutationResult(
    TransactionResultCode code,
    Transaction transaction,
    List<BalanceChange> changes,
    String message
) {
    public StorageMutationResult {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(changes, "changes");
    }

    public static StorageMutationResult success(Transaction transaction, List<BalanceChange> changes) {
        return new StorageMutationResult(TransactionResultCode.SUCCESS, transaction, List.copyOf(changes), null);
    }

    public static StorageMutationResult failure(TransactionResultCode code, Transaction transaction, String message) {
        return new StorageMutationResult(code, transaction, List.of(), message);
    }
}
