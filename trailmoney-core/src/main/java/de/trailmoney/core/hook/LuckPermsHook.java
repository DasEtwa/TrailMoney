package de.trailmoney.core.hook;

import de.trailmoney.api.money.Money;
import de.trailmoney.core.config.TrailMoneySettings;
import de.trailmoney.core.money.MoneyParser;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

public final class LuckPermsHook {
    private final Plugin plugin;
    private final LuckPerms luckPerms;
    private TrailMoneySettings settings;

    public LuckPermsHook(Plugin plugin, TrailMoneySettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.luckPerms = LuckPermsProvider.get();
    }

    public void updateSettings(TrailMoneySettings settings) {
        this.settings = settings;
    }

    public Money startBalance(UUID playerUuid) {
        return readMoneyMeta(playerUuid, settings.luckPerms().startBalanceKey())
            .orElse(settings.startBalance());
    }

    public Long maxBalanceMinorUnits(UUID playerUuid) {
        return readMoneyMeta(playerUuid, settings.luckPerms().maxBalanceKey())
            .map(Money::minorUnits)
            .orElseGet(() -> settings.luckPerms().fallbackMaxBalanceMinorUnits() == null
                ? settings.maxBalanceMinorUnits()
                : settings.luckPerms().fallbackMaxBalanceMinorUnits());
    }

    public double multiplier(UUID playerUuid) {
        return readStringMeta(playerUuid, settings.luckPerms().multiplierKey())
            .map(this::parseMultiplier)
            .orElse(settings.luckPerms().fallbackMultiplier());
    }

    private Optional<Money> readMoneyMeta(UUID playerUuid, String key) {
        return readStringMeta(playerUuid, key).flatMap(value -> {
            try {
                return Optional.of(MoneyParser.parse(value, settings.defaultCurrency()));
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "Ignoring invalid LuckPerms money meta " + key + "=" + value + " for " + playerUuid, exception);
                return Optional.empty();
            }
        });
    }

    private Optional<String> readStringMeta(UUID playerUuid, String key) {
        if (!settings.luckPerms().enabled() || !settings.luckPerms().useMeta()) {
            return Optional.empty();
        }

        Optional<User> user = loadUser(playerUuid);
        if (user.isEmpty()) {
            return Optional.empty();
        }

        String value = user.get().getCachedData().getMetaData(QueryOptions.defaultContextualOptions()).getMetaValue(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private Optional<User> loadUser(UUID playerUuid) {
        User cached = luckPerms.getUserManager().getUser(playerUuid);
        if (cached != null) {
            return Optional.of(cached);
        }

        try {
            return Optional.ofNullable(luckPerms.getUserManager().loadUser(playerUuid).join());
        } catch (CancellationException | CompletionException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not load LuckPerms user data for " + playerUuid, exception);
            return Optional.empty();
        }
    }

    private double parseMultiplier(String value) {
        try {
            double parsed = Double.parseDouble(value);
            return parsed > 0 ? parsed : settings.luckPerms().fallbackMultiplier();
        } catch (NumberFormatException exception) {
            return settings.luckPerms().fallbackMultiplier();
        }
    }
}
