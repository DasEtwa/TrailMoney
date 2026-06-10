package de.trailmoney.core.money;

import de.trailmoney.api.money.Currency;
import de.trailmoney.api.money.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class MoneyParser {
    private MoneyParser() {
    }

    public static Money parse(String input, Currency currency) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(currency, "currency");

        String normalized = input.trim()
            .replace(currency.symbol(), "")
            .replace("_", "")
            .replace(",", ".");

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Amount cannot be blank");
        }

        BigDecimal major = new BigDecimal(normalized).setScale(currency.decimals(), RoundingMode.UNNECESSARY);
        long minor = major.movePointRight(currency.decimals()).longValueExact();
        return Money.ofMinor(minor, currency);
    }
}
