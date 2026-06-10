package de.trailmoney.api.account;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record AccountId(String value) {
    private static final String PLAYER_PREFIX = "player:";
    private static final String SERVER_PREFIX = "server:";
    private static final String VIRTUAL_PREFIX = "virtual:";

    public AccountId {
        Objects.requireNonNull(value, "value");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Account id cannot be blank");
        }
    }

    public static AccountId player(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return new AccountId(PLAYER_PREFIX + playerUuid);
    }

    public static AccountId server(String key) {
        return new AccountId(SERVER_PREFIX + normalizeKey(key));
    }

    public static AccountId virtual(String key) {
        return new AccountId(VIRTUAL_PREFIX + normalizeKey(key));
    }

    public AccountType type() {
        if (value.startsWith(PLAYER_PREFIX)) {
            return AccountType.PLAYER;
        }
        if (value.startsWith(SERVER_PREFIX)) {
            return AccountType.SERVER;
        }
        if (value.startsWith(VIRTUAL_PREFIX)) {
            return AccountType.VIRTUAL;
        }
        return AccountType.UNKNOWN;
    }

    private static String normalizeKey(String key) {
        Objects.requireNonNull(key, "key");
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Account key may only contain a-z, 0-9, underscore, dot and dash");
        }
        return normalized;
    }
}
