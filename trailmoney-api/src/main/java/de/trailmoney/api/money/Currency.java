package de.trailmoney.api.money;

import java.util.Locale;
import java.util.Objects;

public record Currency(String key, String displayName, String symbol, int decimals) {
    public Currency {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(symbol, "symbol");

        key = key.trim().toLowerCase(Locale.ROOT);
        displayName = displayName.trim();

        if (!key.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Currency key may only contain a-z, 0-9, underscore, dot and dash");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Currency display name cannot be blank");
        }
        if (decimals < 0 || decimals > 8) {
            throw new IllegalArgumentException("Currency decimals must be between 0 and 8");
        }
    }

    public long minorUnitsFactor() {
        long factor = 1;
        for (int i = 0; i < decimals; i++) {
            factor *= 10;
        }
        return factor;
    }
}
