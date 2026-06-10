package de.trailmoney.api.transaction;

import java.util.Objects;
import java.util.Optional;

public record TransactionResult(TransactionResultCode code, Transaction transaction, String message) {
    public TransactionResult {
        Objects.requireNonNull(code, "code");
    }

    public static TransactionResult success(Transaction transaction) {
        return new TransactionResult(TransactionResultCode.SUCCESS, transaction, null);
    }

    public static TransactionResult failure(TransactionResultCode code, Transaction transaction, String message) {
        if (code == TransactionResultCode.SUCCESS) {
            throw new IllegalArgumentException("Use success() for successful transactions");
        }
        return new TransactionResult(code, transaction, message);
    }

    public boolean successful() {
        return code == TransactionResultCode.SUCCESS;
    }

    public Optional<Transaction> transactionOptional() {
        return Optional.ofNullable(transaction);
    }

    public Optional<String> messageOptional() {
        return Optional.ofNullable(message);
    }
}
