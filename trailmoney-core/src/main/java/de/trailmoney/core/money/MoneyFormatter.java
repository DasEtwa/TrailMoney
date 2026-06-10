package de.trailmoney.core.money;

import de.trailmoney.api.money.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyFormatter {
    private MoneyFormatter() {
    }

    public static String format(Money money) {
        BigDecimal major = BigDecimal.valueOf(money.minorUnits(), money.currency().decimals())
            .setScale(money.currency().decimals(), RoundingMode.UNNECESSARY);
        return money.currency().symbol() + major.toPlainString();
    }
}
