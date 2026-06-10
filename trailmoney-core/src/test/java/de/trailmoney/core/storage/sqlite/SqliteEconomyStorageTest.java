package de.trailmoney.core.storage.sqlite;

import de.trailmoney.api.account.Account;
import de.trailmoney.api.account.AccountId;
import de.trailmoney.api.money.Currency;
import de.trailmoney.api.money.Money;
import de.trailmoney.api.transaction.Transaction;
import de.trailmoney.api.transaction.TransactionReason;
import de.trailmoney.api.transaction.TransactionResultCode;
import de.trailmoney.core.storage.AccountCreationResult;
import de.trailmoney.core.storage.StorageMutationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteEconomyStorageTest {
    private static final Currency COINS = new Currency("coins", "Coins", "$", 2);
    private static final Money ZERO = Money.zero(COINS);
    private static final Money MIN_BALANCE = Money.ofMinor(0, COINS);

    @TempDir
    Path tempDir;

    @Test
    void createsPlayerAccountWithStartBalanceAndPersistsIt() {
        UUID playerUuid = UUID.randomUUID();
        Path database = tempDir.resolve("trailmoney.db");

        try (SqliteEconomyStorage storage = open(database)) {
            AccountCreationResult result = storage.getOrCreatePlayerAccount(playerUuid, "DasEtwa", Money.ofMinor(1250, COINS));

            assertTrue(result.created());
            assertEquals(AccountId.player(playerUuid), result.account().id());
            assertEquals(1250, storage.getBalance(result.account().id(), COINS).minorUnits());
        }

        try (SqliteEconomyStorage storage = open(database)) {
            assertEquals(1250, storage.getBalance(AccountId.player(playerUuid), COINS).minorUnits());
        }
    }

    @Test
    void depositWithdrawAndSetBalanceRespectLimits() {
        try (SqliteEconomyStorage storage = open(tempDir.resolve("trailmoney.db"))) {
            Account account = createPlayer(storage, "Tester", ZERO);

            StorageMutationResult deposit = storage.deposit(
                account.id(),
                Money.ofMinor(5000, COINS),
                MIN_BALANCE,
                10_000L,
                transaction(null, account.id(), 5000, "test_deposit")
            );

            assertEquals(TransactionResultCode.SUCCESS, deposit.code());
            assertEquals(5000, storage.getBalance(account.id(), COINS).minorUnits());

            StorageMutationResult rejectedDeposit = storage.deposit(
                account.id(),
                Money.ofMinor(6000, COINS),
                MIN_BALANCE,
                10_000L,
                transaction(null, account.id(), 6000, "test_deposit_limit")
            );

            assertEquals(TransactionResultCode.LIMIT_EXCEEDED, rejectedDeposit.code());
            assertEquals(5000, storage.getBalance(account.id(), COINS).minorUnits());

            StorageMutationResult withdraw = storage.withdraw(
                account.id(),
                Money.ofMinor(1250, COINS),
                MIN_BALANCE,
                transaction(account.id(), null, 1250, "test_withdraw")
            );

            assertEquals(TransactionResultCode.SUCCESS, withdraw.code());
            assertEquals(3750, storage.getBalance(account.id(), COINS).minorUnits());

            StorageMutationResult rejectedWithdraw = storage.withdraw(
                account.id(),
                Money.ofMinor(4000, COINS),
                MIN_BALANCE,
                transaction(account.id(), null, 4000, "test_withdraw_limit")
            );

            assertEquals(TransactionResultCode.INSUFFICIENT_FUNDS, rejectedWithdraw.code());
            assertEquals(3750, storage.getBalance(account.id(), COINS).minorUnits());

            StorageMutationResult set = storage.setBalance(
                account.id(),
                Money.ofMinor(9999, COINS),
                MIN_BALANCE,
                10_000L,
                transaction(null, account.id(), 9999, "test_set")
            );

            assertEquals(TransactionResultCode.SUCCESS, set.code());
            assertEquals(9999, storage.getBalance(account.id(), COINS).minorUnits());
        }
    }

    @Test
    void transferIsAtomicAndUpdatesBothAccounts() {
        try (SqliteEconomyStorage storage = open(tempDir.resolve("trailmoney.db"))) {
            Account source = createPlayer(storage, "Source", Money.ofMinor(10_000, COINS));
            Account target = createPlayer(storage, "Target", ZERO);

            StorageMutationResult transfer = storage.transfer(
                source.id(),
                target.id(),
                Money.ofMinor(2500, COINS),
                MIN_BALANCE,
                5000L,
                transaction(source.id(), target.id(), 2500, "test_transfer")
            );

            assertEquals(TransactionResultCode.SUCCESS, transfer.code());
            assertEquals(2, transfer.changes().size());
            assertEquals(7500, storage.getBalance(source.id(), COINS).minorUnits());
            assertEquals(2500, storage.getBalance(target.id(), COINS).minorUnits());

            StorageMutationResult rejectedTransfer = storage.transfer(
                source.id(),
                target.id(),
                Money.ofMinor(3000, COINS),
                MIN_BALANCE,
                5000L,
                transaction(source.id(), target.id(), 3000, "test_transfer_limit")
            );

            assertEquals(TransactionResultCode.LIMIT_EXCEEDED, rejectedTransfer.code());
            assertEquals(7500, storage.getBalance(source.id(), COINS).minorUnits());
            assertEquals(2500, storage.getBalance(target.id(), COINS).minorUnits());
        }
    }

    @Test
    void topBalancesOnlyReturnsPlayerAccountsInDescendingOrder() {
        try (SqliteEconomyStorage storage = open(tempDir.resolve("trailmoney.db"))) {
            Account low = createPlayer(storage, "Low", Money.ofMinor(100, COINS));
            Account high = createPlayer(storage, "High", Money.ofMinor(500, COINS));
            Account middle = createPlayer(storage, "Middle", Money.ofMinor(300, COINS));

            List<String> orderedNames = storage.topBalances(COINS, 3).stream()
                .map(entry -> entry.account().displayName())
                .toList();

            assertEquals(List.of(high.displayName(), middle.displayName(), low.displayName()), orderedNames);
        }
    }

    @Test
    void existingAccountIsReturnedAndDisplayNameIsUpdated() {
        UUID playerUuid = UUID.randomUUID();

        try (SqliteEconomyStorage storage = open(tempDir.resolve("trailmoney.db"))) {
            AccountCreationResult first = storage.getOrCreatePlayerAccount(playerUuid, "OldName", ZERO);
            AccountCreationResult second = storage.getOrCreatePlayerAccount(playerUuid, "NewName", ZERO);

            assertTrue(first.created());
            assertFalse(second.created());
            assertEquals("NewName", second.account().displayName());
        }
    }

    private SqliteEconomyStorage open(Path database) {
        SqliteEconomyStorage storage = new SqliteEconomyStorage(database);
        storage.initialize();
        return storage;
    }

    private Account createPlayer(SqliteEconomyStorage storage, String name, Money startBalance) {
        return storage.getOrCreatePlayerAccount(UUID.randomUUID(), name, startBalance).account();
    }

    private Transaction transaction(AccountId source, AccountId target, long amount, String reason) {
        return Transaction.pending(source, target, Money.ofMinor(amount, COINS), TransactionReason.system(reason));
    }
}
