package de.trailmoney.core.storage.sqlite;

import de.trailmoney.api.account.Account;
import de.trailmoney.api.account.AccountId;
import de.trailmoney.api.account.AccountType;
import de.trailmoney.api.money.BalanceEntry;
import de.trailmoney.api.money.Currency;
import de.trailmoney.api.money.Money;
import de.trailmoney.api.transaction.Transaction;
import de.trailmoney.api.transaction.TransactionReason;
import de.trailmoney.api.transaction.TransactionResultCode;
import de.trailmoney.api.transaction.TransactionStatus;
import de.trailmoney.core.storage.AccountCreationResult;
import de.trailmoney.core.storage.BalanceChange;
import de.trailmoney.core.storage.EconomyStorage;
import de.trailmoney.core.storage.StorageException;
import de.trailmoney.core.storage.StorageMutationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteEconomyStorage implements EconomyStorage {
    private final Path databaseFile;
    private Connection connection;

    public SqliteEconomyStorage(Path databaseFile) {
        this.databaseFile = databaseFile;
    }

    @Override
    public synchronized void initialize() {
        try {
            Files.createDirectories(databaseFile.getParent());
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA foreign_keys=ON");
            }
            migrate();
        } catch (IOException | ClassNotFoundException | SQLException exception) {
            throw new StorageException("Failed to initialize SQLite storage", exception);
        }
    }

    @Override
    public synchronized AccountCreationResult getOrCreatePlayerAccount(UUID playerUuid, String playerName, Money startBalance) {
        AccountId accountId = AccountId.player(playerUuid);
        long now = now();

        try {
            Optional<Account> existing = findAccountInternal(accountId);
            if (existing.isPresent()) {
                updateDisplayName(accountId, playerName, now);
                return new AccountCreationResult(findAccountInternal(accountId).orElseThrow(), false);
            }

            connection.setAutoCommit(false);
            Account account = new Account(accountId, AccountType.PLAYER, playerUuid, playerName, Instant.ofEpochMilli(now), Instant.ofEpochMilli(now));
            insertAccount(account, now);
            upsertBalance(accountId, startBalance, now);
            if (!startBalance.isZero()) {
                Transaction transaction = Transaction.pending(null, accountId, startBalance, TransactionReason.system("account_start"))
                    .withStatus(TransactionStatus.SUCCESS);
                insertTransaction(transaction);
            }
            connection.commit();
            return new AccountCreationResult(account, true);
        } catch (SQLException exception) {
            rollbackQuietly();
            throw new StorageException("Failed to create player account", exception);
        } finally {
            restoreAutoCommit();
        }
    }

    @Override
    public synchronized Optional<Account> findAccount(AccountId accountId) {
        try {
            return findAccountInternal(accountId);
        } catch (SQLException exception) {
            throw new StorageException("Failed to find account", exception);
        }
    }

    @Override
    public synchronized Money getBalance(AccountId accountId, Currency currency) {
        try {
            return Money.ofMinor(readBalance(accountId, currency), currency);
        } catch (SQLException exception) {
            throw new StorageException("Failed to read balance", exception);
        }
    }

    @Override
    public synchronized StorageMutationResult deposit(AccountId target, Money amount, Money minBalance, Long maxBalanceMinorUnits, Transaction transaction) {
        try {
            connection.setAutoCommit(false);
            if (!accountExists(target)) {
                return commitFailure(TransactionResultCode.ACCOUNT_NOT_FOUND, transaction, "Target account does not exist");
            }

            Money oldBalance = Money.ofMinor(readBalance(target, amount.currency()), amount.currency());
            Money newBalance = oldBalance.plus(amount);
            if (exceedsMax(newBalance, maxBalanceMinorUnits)) {
                return commitFailure(TransactionResultCode.LIMIT_EXCEEDED, transaction, "Target account would exceed max balance");
            }

            upsertBalance(target, newBalance, now());
            Transaction success = transaction.withStatus(TransactionStatus.SUCCESS);
            insertTransaction(success);
            connection.commit();
            return StorageMutationResult.success(success, List.of(new BalanceChange(target, oldBalance, newBalance)));
        } catch (SQLException exception) {
            rollbackQuietly();
            throw new StorageException("Failed to deposit money", exception);
        } finally {
            restoreAutoCommit();
        }
    }

    @Override
    public synchronized StorageMutationResult withdraw(AccountId source, Money amount, Money minBalance, Transaction transaction) {
        try {
            connection.setAutoCommit(false);
            if (!accountExists(source)) {
                return commitFailure(TransactionResultCode.ACCOUNT_NOT_FOUND, transaction, "Source account does not exist");
            }

            Money oldBalance = Money.ofMinor(readBalance(source, amount.currency()), amount.currency());
            Money newBalance = oldBalance.minus(amount);
            if (newBalance.compareTo(minBalance) < 0) {
                return commitFailure(TransactionResultCode.INSUFFICIENT_FUNDS, transaction, "Source account has insufficient funds");
            }

            upsertBalance(source, newBalance, now());
            Transaction success = transaction.withStatus(TransactionStatus.SUCCESS);
            insertTransaction(success);
            connection.commit();
            return StorageMutationResult.success(success, List.of(new BalanceChange(source, oldBalance, newBalance)));
        } catch (SQLException exception) {
            rollbackQuietly();
            throw new StorageException("Failed to withdraw money", exception);
        } finally {
            restoreAutoCommit();
        }
    }

    @Override
    public synchronized StorageMutationResult transfer(AccountId source, AccountId target, Money amount, Money minBalance, Long maxBalanceMinorUnits, Transaction transaction) {
        try {
            connection.setAutoCommit(false);
            if (!accountExists(source) || !accountExists(target)) {
                return commitFailure(TransactionResultCode.ACCOUNT_NOT_FOUND, transaction, "Source or target account does not exist");
            }

            Money oldSourceBalance = Money.ofMinor(readBalance(source, amount.currency()), amount.currency());
            Money oldTargetBalance = Money.ofMinor(readBalance(target, amount.currency()), amount.currency());
            Money newSourceBalance = oldSourceBalance.minus(amount);
            Money newTargetBalance = oldTargetBalance.plus(amount);

            if (newSourceBalance.compareTo(minBalance) < 0) {
                return commitFailure(TransactionResultCode.INSUFFICIENT_FUNDS, transaction, "Source account has insufficient funds");
            }
            if (exceedsMax(newTargetBalance, maxBalanceMinorUnits)) {
                return commitFailure(TransactionResultCode.LIMIT_EXCEEDED, transaction, "Target account would exceed max balance");
            }

            long now = now();
            upsertBalance(source, newSourceBalance, now);
            upsertBalance(target, newTargetBalance, now);
            Transaction success = transaction.withStatus(TransactionStatus.SUCCESS);
            insertTransaction(success);
            connection.commit();

            List<BalanceChange> changes = new ArrayList<>();
            changes.add(new BalanceChange(source, oldSourceBalance, newSourceBalance));
            changes.add(new BalanceChange(target, oldTargetBalance, newTargetBalance));
            return StorageMutationResult.success(success, changes);
        } catch (SQLException exception) {
            rollbackQuietly();
            throw new StorageException("Failed to transfer money", exception);
        } finally {
            restoreAutoCommit();
        }
    }

    @Override
    public synchronized StorageMutationResult setBalance(AccountId accountId, Money amount, Money minBalance, Long maxBalanceMinorUnits, Transaction transaction) {
        try {
            connection.setAutoCommit(false);
            if (!accountExists(accountId)) {
                return commitFailure(TransactionResultCode.ACCOUNT_NOT_FOUND, transaction, "Account does not exist");
            }
            if (amount.compareTo(minBalance) < 0 || exceedsMax(amount, maxBalanceMinorUnits)) {
                return commitFailure(TransactionResultCode.LIMIT_EXCEEDED, transaction, "New balance is outside configured limits");
            }

            Money oldBalance = Money.ofMinor(readBalance(accountId, amount.currency()), amount.currency());
            upsertBalance(accountId, amount, now());
            Transaction success = transaction.withStatus(TransactionStatus.SUCCESS);
            insertTransaction(success);
            connection.commit();
            return StorageMutationResult.success(success, List.of(new BalanceChange(accountId, oldBalance, amount)));
        } catch (SQLException exception) {
            rollbackQuietly();
            throw new StorageException("Failed to set balance", exception);
        } finally {
            restoreAutoCommit();
        }
    }

    @Override
    public synchronized List<BalanceEntry> topBalances(Currency currency, int limit) {
        String sql = """
            SELECT a.id, a.type, a.owner_uuid, a.display_name, a.created_at, a.updated_at, b.minor_units
            FROM balances b
            JOIN accounts a ON a.id = b.account_id
            WHERE b.currency_key = ? AND a.type = 'PLAYER'
            ORDER BY b.minor_units DESC
            LIMIT ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currency.key());
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<BalanceEntry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    Account account = accountFromResultSet(resultSet);
                    Money balance = Money.ofMinor(resultSet.getLong("minor_units"), currency);
                    entries.add(new BalanceEntry(account, balance));
                }
                return entries;
            }
        } catch (SQLException exception) {
            throw new StorageException("Failed to read top balances", exception);
        }
    }

    @Override
    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            throw new StorageException("Failed to close SQLite storage", exception);
        }
    }

    private void migrate() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    owner_uuid TEXT NULL,
                    display_name TEXT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS balances (
                    account_id TEXT NOT NULL,
                    currency_key TEXT NOT NULL,
                    minor_units INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (account_id, currency_key)
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id TEXT PRIMARY KEY,
                    source_account_id TEXT NULL,
                    target_account_id TEXT NULL,
                    currency_key TEXT NOT NULL,
                    amount_minor_units INTEGER NOT NULL,
                    reason TEXT NOT NULL,
                    status TEXT NOT NULL,
                    actor TEXT NULL,
                    message TEXT NULL,
                    created_at INTEGER NOT NULL
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS migrations (
                    version INTEGER PRIMARY KEY,
                    description TEXT NOT NULL,
                    applied_at INTEGER NOT NULL
                )
                """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_balances_top ON balances(currency_key, minor_units DESC)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_accounts_owner_uuid ON accounts(owner_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at)");
        }
    }

    private StorageMutationResult commitFailure(TransactionResultCode code, Transaction transaction, String message) throws SQLException {
        Transaction failed = transaction.withStatus(code == TransactionResultCode.CANCELLED ? TransactionStatus.CANCELLED : TransactionStatus.FAILED);
        insertTransaction(failed);
        connection.commit();
        return StorageMutationResult.failure(code, failed, message);
    }

    private Optional<Account> findAccountInternal(AccountId accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT id, type, owner_uuid, display_name, created_at, updated_at
            FROM accounts
            WHERE id = ?
            """)) {
            statement.setString(1, accountId.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(accountFromResultSet(resultSet));
            }
        }
    }

    private boolean accountExists(AccountId accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM accounts WHERE id = ?")) {
            statement.setString(1, accountId.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private Account accountFromResultSet(ResultSet resultSet) throws SQLException {
        String ownerUuid = resultSet.getString("owner_uuid");
        return new Account(
            new AccountId(resultSet.getString("id")),
            AccountType.valueOf(resultSet.getString("type")),
            ownerUuid == null ? null : UUID.fromString(ownerUuid),
            resultSet.getString("display_name"),
            Instant.ofEpochMilli(resultSet.getLong("created_at")),
            Instant.ofEpochMilli(resultSet.getLong("updated_at"))
        );
    }

    private void insertAccount(Account account, long now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO accounts (id, type, owner_uuid, display_name, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """)) {
            statement.setString(1, account.id().value());
            statement.setString(2, account.type().name());
            statement.setString(3, account.ownerUuid() == null ? null : account.ownerUuid().toString());
            statement.setString(4, account.displayName());
            statement.setLong(5, now);
            statement.setLong(6, now);
            statement.executeUpdate();
        }
    }

    private void updateDisplayName(AccountId accountId, String playerName, long now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            UPDATE accounts
            SET display_name = ?, updated_at = ?
            WHERE id = ?
            """)) {
            statement.setString(1, playerName);
            statement.setLong(2, now);
            statement.setString(3, accountId.value());
            statement.executeUpdate();
        }
    }

    private long readBalance(AccountId accountId, Currency currency) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT minor_units
            FROM balances
            WHERE account_id = ? AND currency_key = ?
            """)) {
            statement.setString(1, accountId.value());
            statement.setString(2, currency.key());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return 0L;
                }
                return resultSet.getLong("minor_units");
            }
        }
    }

    private void upsertBalance(AccountId accountId, Money money, long now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO balances (account_id, currency_key, minor_units, updated_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(account_id, currency_key)
            DO UPDATE SET minor_units = excluded.minor_units, updated_at = excluded.updated_at
            """)) {
            statement.setString(1, accountId.value());
            statement.setString(2, money.currency().key());
            statement.setLong(3, money.minorUnits());
            statement.setLong(4, now);
            statement.executeUpdate();
        }
    }

    private void insertTransaction(Transaction transaction) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO transactions (
                id, source_account_id, target_account_id, currency_key, amount_minor_units,
                reason, status, actor, message, created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            statement.setString(1, transaction.id().toString());
            statement.setString(2, transaction.source() == null ? null : transaction.source().value());
            statement.setString(3, transaction.target() == null ? null : transaction.target().value());
            statement.setString(4, transaction.amount().currency().key());
            statement.setLong(5, transaction.amount().minorUnits());
            statement.setString(6, transaction.reason().key());
            statement.setString(7, transaction.status().name());
            statement.setString(8, transaction.reason().actor());
            statement.setString(9, transaction.reason().message());
            statement.setLong(10, transaction.timestamp().toEpochMilli());
            statement.executeUpdate();
        }
    }

    private boolean exceedsMax(Money money, Long maxBalanceMinorUnits) {
        return maxBalanceMinorUnits != null && money.minorUnits() > maxBalanceMinorUnits;
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private void rollbackQuietly() {
        try {
            if (connection != null && !connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (SQLException ignored) {
        }
    }

    private void restoreAutoCommit() {
        try {
            if (connection != null && !connection.getAutoCommit()) {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ignored) {
        }
    }
}
