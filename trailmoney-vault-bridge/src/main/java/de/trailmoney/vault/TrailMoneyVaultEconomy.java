package de.trailmoney.vault;

import de.trailmoney.api.EconomyService;
import de.trailmoney.api.account.Account;
import de.trailmoney.api.account.AccountId;
import de.trailmoney.api.money.Currency;
import de.trailmoney.api.money.Money;
import de.trailmoney.api.transaction.TransactionReason;
import de.trailmoney.api.transaction.TransactionResult;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class TrailMoneyVaultEconomy implements Economy {
    private static final String BANK_NOT_IMPLEMENTED = "TrailMoney bank support is not implemented in the Vault bridge.";

    private final EconomyService economyService;

    public TrailMoneyVaultEconomy(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "TrailMoney";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return currency().decimals();
    }

    @Override
    public String format(double amount) {
        Money money = toMoneyOrZero(amount);
        BigDecimal major = BigDecimal.valueOf(money.minorUnits(), money.currency().decimals())
            .setScale(money.currency().decimals(), RoundingMode.UNNECESSARY);
        return money.currency().symbol() + major.toPlainString();
    }

    @Override
    public String currencyNamePlural() {
        return currency().displayName();
    }

    @Override
    public String currencyNameSingular() {
        return currency().displayName();
    }

    @Override
    public boolean hasAccount(String playerName) {
        OfflinePlayer player = resolveOfflinePlayer(playerName);
        return player != null && hasAccount(player);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return economyService.findAccount(AccountId.player(player.getUniqueId())).isPresent();
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        OfflinePlayer player = resolveOfflinePlayer(playerName);
        return player == null ? 0 : getBalance(player);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        Money balance = economyService.getBalance(AccountId.player(player.getUniqueId()), currency());
        return toDouble(balance);
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        OfflinePlayer player = resolveOfflinePlayer(playerName);
        return player != null && has(player, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        try {
            Money requested = toMoney(amount);
            Money balance = economyService.getBalance(AccountId.player(player.getUniqueId()), currency());
            return balance.compareTo(requested) >= 0;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        OfflinePlayer player = resolveOfflinePlayer(playerName);
        if (player == null) {
            return failure(amount, 0, "Unknown offline player: " + playerName);
        }
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        try {
            Account account = getOrCreate(player);
            Money money = toMoney(amount);
            TransactionResult result = economyService.withdraw(account.id(), money, TransactionReason.system("vault_withdraw"));
            return response(result, money, account.id());
        } catch (IllegalArgumentException exception) {
            return failure(amount, getBalance(player), exception.getMessage());
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        OfflinePlayer player = resolveOfflinePlayer(playerName);
        if (player == null) {
            return failure(amount, 0, "Unknown offline player: " + playerName);
        }
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        try {
            Account account = getOrCreate(player);
            Money money = toMoney(amount);
            TransactionResult result = economyService.deposit(account.id(), money, TransactionReason.system("vault_deposit"));
            return response(result, money, account.id());
        } catch (IllegalArgumentException exception) {
            return failure(amount, getBalance(player), exception.getMessage());
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return bankNotImplemented();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return bankNotImplemented();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return bankNotImplemented();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return bankNotImplemented();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return bankNotImplemented();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return bankNotImplemented();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return bankNotImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return bankNotImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return bankNotImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return bankNotImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return bankNotImplemented();
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        OfflinePlayer player = resolveOfflinePlayer(playerName);
        if (player == null) {
            return false;
        }
        getOrCreate(player);
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        getOrCreate(player);
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    private Currency currency() {
        return economyService.defaultCurrency();
    }

    private Account getOrCreate(OfflinePlayer player) {
        String name = player.getName() == null ? player.getUniqueId().toString() : player.getName();
        return economyService.getOrCreatePlayerAccount(player.getUniqueId(), name);
    }

    private EconomyResponse response(TransactionResult result, Money amount, AccountId accountId) {
        double balance = toDouble(economyService.getBalance(accountId, currency()));
        if (result.successful()) {
            return new EconomyResponse(toDouble(amount), balance, EconomyResponse.ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, result.messageOptional().orElse(result.code().name()));
    }

    private EconomyResponse failure(double amount, double balance, String message) {
        return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, message);
    }

    private EconomyResponse bankNotImplemented() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, BANK_NOT_IMPLEMENTED);
    }

    private Money toMoney(double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Amount must be a finite positive number");
        }
        BigDecimal major = BigDecimal.valueOf(amount).setScale(currency().decimals(), RoundingMode.UNNECESSARY);
        long minor = major.movePointRight(currency().decimals()).longValueExact();
        return Money.ofMinor(minor, currency());
    }

    private Money toMoneyOrZero(double amount) {
        try {
            return toMoney(amount);
        } catch (IllegalArgumentException exception) {
            return Money.zero(currency());
        }
    }

    private double toDouble(Money money) {
        return BigDecimal.valueOf(money.minorUnits(), money.currency().decimals()).doubleValue();
    }

    private OfflinePlayer resolveOfflinePlayer(String input) {
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(input));
        } catch (IllegalArgumentException ignored) {
            OfflinePlayer onlinePlayer = Bukkit.getPlayerExact(input);
            if (onlinePlayer != null) {
                return onlinePlayer;
            }
            return Bukkit.getOfflinePlayerIfCached(input);
        }
    }
}
