package de.trailmoney.api.account;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Account(
    AccountId id,
    AccountType type,
    UUID ownerUuid,
    String displayName,
    Instant createdAt,
    Instant updatedAt
) {
    public Account {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
