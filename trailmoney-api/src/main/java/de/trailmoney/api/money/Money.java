package de.trailmoney.api.money;

import java.util.Objects;

public record Money(long minorUnits, Currency currency) implements Comparable<Money> {
    public Money {
        Objects.requireNonNull(currency, "currency");
    }

    public static Money zero(Currency currency) {
        return new Money(0, currency);
    }

    public static Money ofMinor(long minorUnits, Currency currency) {
        return new Money(minorUnits, currency);
    }

    public boolean isZero() {
        return minorUnits == 0;
    }

    public boolean isPositive() {
        return minorUnits > 0;
    }

    public boolean isNegative() {
        return minorUnits < 0;
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(minorUnits, other.minorUnits), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(minorUnits, other.minorUnits), currency);
    }

    public Money negate() {
        return new Money(Math.negateExact(minorUnits), currency);
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return Long.compare(minorUnits, other.minorUnits);
    }

    public void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.key().equals(other.currency.key())) {
            throw new IllegalArgumentException("Currency mismatch: " + currency.key() + " != " + other.currency.key());
        }
    }
}
