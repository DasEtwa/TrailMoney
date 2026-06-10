package de.trailmoney.api.transaction;

import java.util.Objects;

public record TransactionReason(String key, String message, String actor) {
    public TransactionReason {
        Objects.requireNonNull(key, "key");
        key = key.trim();
        if (key.isBlank()) {
            throw new IllegalArgumentException("Reason key cannot be blank");
        }
    }

    public static TransactionReason system(String key) {
        return new TransactionReason(key, null, "system");
    }

    public static TransactionReason player(String key, String playerName) {
        return new TransactionReason(key, null, playerName);
    }

    public static TransactionReason plugin(String pluginName, String key) {
        Objects.requireNonNull(pluginName, "pluginName");
        String actor = "plugin:" + pluginName.trim();
        return new TransactionReason(key, null, actor);
    }
}
