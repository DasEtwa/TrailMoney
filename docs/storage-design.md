# TrailMoney Storage Design

Dieses Dokument plant die Storage-Schicht. Es ist keine finale Datenbankmigration.

## Ziele

- SQLite als Default Storage.
- MariaDB/MySQL spaeter.
- PostgreSQL optional spaeter.
- Storage muss austauschbar sein.
- Transfers muessen atomar sein.
- Money wird als `long minorUnits` gespeichert.
- Spieler-UUID ist die Primaeridentitaet.
- Spielername ist nur Cache oder Anzeige.

## Storage Backends

### SQLite

MVP Default.

Vorteile:

- Keine externe Einrichtung.
- Gut fuer Einzelserver.
- Einfach fuer erste Releases.

Risiken:

- Concurrency begrenzt.
- `eco top` und grosse Transaktionslogs koennen Indizes brauchen.

### MariaDB/MySQL

Spaeter fuer Netzwerke und groessere Server.

Regeln:

- Gleiche Repository-Interfaces wie SQLite.
- Keine Core-Logik in SQL-Strings.
- Connection Pooling einplanen.

### PostgreSQL

Optional spaeter.

## Repository-Schicht

Geplante Interfaces:

- `AccountRepository`
- `BalanceRepository`
- `TransactionRepository`
- `MigrationRepository`

Core-Services sprechen gegen Interfaces. Backend-spezifische Implementierungen bleiben austauschbar.

## Tabellenentwurf MVP

### accounts

```sql
CREATE TABLE accounts (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    owner_uuid TEXT NULL,
    display_name TEXT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Hinweise:

- `id` ist eine TrailMoney Account ID.
- Spieler-Accounts referenzieren `owner_uuid`.
- Namen sind nicht eindeutig und nicht Primary Identity.

### balances

```sql
CREATE TABLE balances (
    account_id TEXT NOT NULL,
    currency_key TEXT NOT NULL,
    minor_units INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (account_id, currency_key)
);
```

Hinweise:

- `minor_units` ist ein signed 64-bit Integer.
- Min-/Max-Balance wird in der Service-Schicht geprueft.
- Datenbankconstraints koennen spaeter ergaenzen, aber nicht die Service-Regeln ersetzen.

### transactions

```sql
CREATE TABLE transactions (
    id TEXT PRIMARY KEY,
    source_account_id TEXT NULL,
    target_account_id TEXT NULL,
    currency_key TEXT NOT NULL,
    amount_minor_units INTEGER NOT NULL,
    reason TEXT NOT NULL,
    status TEXT NOT NULL,
    actor TEXT NULL,
    metadata TEXT NULL,
    created_at INTEGER NOT NULL
);
```

Hinweise:

- Jede Balance-Aenderung erzeugt einen nachvollziehbaren Eintrag.
- `metadata` kann JSON sein, soll aber nicht fuer Kernlogik missbraucht werden.

### migrations

```sql
CREATE TABLE migrations (
    version INTEGER PRIMARY KEY,
    description TEXT NOT NULL,
    applied_at INTEGER NOT NULL
);
```

## Migrationen

Regeln:

- Jede Schema-Aenderung bekommt eine Migration.
- Migrationen laufen beim Start vor Service-Registrierung.
- Fehlgeschlagene Migrationen verhindern den Start sauber.
- Keine stillen Schema-Reparaturen ohne Log.

## Atomare Transaktionen

Transfers muessen in einer DB-Transaktion laufen:

1. Source Balance laden und sperren, soweit Backend moeglich.
2. Funds und Limits pruefen.
3. Source abbuchen.
4. Target gutschreiben.
5. Transaction Log schreiben.
6. Commit.

Wenn ein Schritt fehlschlaegt, wird rollback ausgefuehrt.

SQLite braucht besondere Sorgfalt, weil echtes Row-Level-Locking nicht wie bei Server-Datenbanken funktioniert. Fuer MVP reicht ein klarer write-lock-orientierter Ansatz, solange alle Economy-Schreiboperationen durch TrailMoney laufen.

## Config-Idee

```yaml
storage:
  type: sqlite
  sqlite:
    file: "trailmoney.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "trailmoney"
    username: "trailmoney"
    password: ""

currency:
  default: "coins"
  currencies:
    coins:
      display-name: "Coins"
      symbol: "$"
      decimals: 2

economy:
  start-balance: 0
  min-balance: 0
  max-balance: -1
```

## Performance-Notizen

- `eco top` braucht einen Index auf `(currency_key, minor_units)`.
- Transaktionslog kann gross werden und braucht Pagination.
- Balance Reads duerfen gecached werden, aber Writes muessen konsistent bleiben.
- Cache-Invalidierung ist spaeteres Thema, nicht MVP-Pflicht.
