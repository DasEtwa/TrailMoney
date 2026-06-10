package de.trailmoney.core.config;

import de.trailmoney.api.money.Currency;
import de.trailmoney.api.money.Money;
import de.trailmoney.core.money.MoneyParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Objects;

public record TrailMoneySettings(
    Currency defaultCurrency,
    Money startBalance,
    Money minBalance,
    Long maxBalanceMinorUnits,
    LuckPermsSettings luckPerms,
    Path sqliteFile,
    boolean createAccountOnJoin,
    int topLimit,
    String messagePrefix
) {
    public TrailMoneySettings {
        Objects.requireNonNull(defaultCurrency, "defaultCurrency");
        Objects.requireNonNull(startBalance, "startBalance");
        Objects.requireNonNull(minBalance, "minBalance");
        Objects.requireNonNull(luckPerms, "luckPerms");
        Objects.requireNonNull(sqliteFile, "sqliteFile");
        Objects.requireNonNull(messagePrefix, "messagePrefix");
        if (topLimit < 1) {
            topLimit = 10;
        }
    }

    public static TrailMoneySettings load(JavaPlugin plugin) {
        String currencyKey = plugin.getConfig().getString("currency.default", "coins");
        ConfigurationSection currencySection = plugin.getConfig().getConfigurationSection("currency.currencies." + currencyKey);
        if (currencySection == null) {
            throw new IllegalStateException("Missing currency config for key: " + currencyKey);
        }

        Currency currency = new Currency(
            currencyKey,
            currencySection.getString("display-name", "Coins"),
            currencySection.getString("symbol", "$"),
            currencySection.getInt("decimals", 2)
        );

        Money startBalance = MoneyParser.parse(plugin.getConfig().getString("economy.start-balance", "0"), currency);
        Money minBalance = MoneyParser.parse(plugin.getConfig().getString("economy.min-balance", "0"), currency);

        String maxBalanceRaw = plugin.getConfig().getString("economy.max-balance", "-1");
        Long maxBalance = "-1".equals(maxBalanceRaw) ? null : MoneyParser.parse(maxBalanceRaw, currency).minorUnits();

        String sqliteFile = plugin.getConfig().getString("storage.sqlite.file", "trailmoney.db");

        return new TrailMoneySettings(
            currency,
            startBalance,
            minBalance,
            maxBalance,
            LuckPermsSettings.load(plugin, currency),
            plugin.getDataFolder().toPath().resolve(sqliteFile),
            plugin.getConfig().getBoolean("economy.create-account-on-join", true),
            plugin.getConfig().getInt("economy.top-limit", 10),
            plugin.getConfig().getString("messages.prefix", "[TrailMoney]")
        );
    }

    public record LuckPermsSettings(
        boolean enabled,
        boolean useMeta,
        String multiplierKey,
        String maxBalanceKey,
        String startBalanceKey,
        double fallbackMultiplier,
        Long fallbackMaxBalanceMinorUnits
    ) {
        public LuckPermsSettings {
            Objects.requireNonNull(multiplierKey, "multiplierKey");
            Objects.requireNonNull(maxBalanceKey, "maxBalanceKey");
            Objects.requireNonNull(startBalanceKey, "startBalanceKey");
        }

        private static LuckPermsSettings load(JavaPlugin plugin, Currency currency) {
            String fallbackMaxRaw = plugin.getConfig().getString("luckperms.fallback.max-balance", "-1");
            Long fallbackMax = "-1".equals(fallbackMaxRaw) ? null : MoneyParser.parse(fallbackMaxRaw, currency).minorUnits();

            return new LuckPermsSettings(
                plugin.getConfig().getBoolean("luckperms.enabled", true),
                plugin.getConfig().getBoolean("luckperms.use-meta", true),
                plugin.getConfig().getString("luckperms.meta.multiplier-key", "trailmoney.multiplier"),
                plugin.getConfig().getString("luckperms.meta.max-balance-key", "trailmoney.max-balance"),
                plugin.getConfig().getString("luckperms.meta.start-balance-key", "trailmoney.start-balance"),
                plugin.getConfig().getDouble("luckperms.fallback.multiplier", 1.0D),
                fallbackMax
            );
        }
    }
}
