package de.trailmoney.api.money;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {
    private final Currency coins = new Currency("coins", "Coins", "$", 2);

    @Test
    void addsMinorUnitsWithoutDoubleMath() {
        Money result = Money.ofMinor(125, coins).plus(Money.ofMinor(75, coins));

        assertEquals(200, result.minorUnits());
    }

    @Test
    void rejectsCurrencyMismatch() {
        Currency gems = new Currency("gems", "Gems", "G", 0);

        assertThrows(IllegalArgumentException.class, () -> Money.ofMinor(1, coins).plus(Money.ofMinor(1, gems)));
    }
}
