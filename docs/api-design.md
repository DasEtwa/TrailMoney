# TrailMoney API Design

Dieses Dokument beschreibt die oeffentliche TrailMoney API und die Leitplanken fuer spaetere Erweiterungen.

## Ziele

- Andere Plugins sollen TrailMoney sauber nutzen koennen.
- Die API soll stabiler und ausdrucksstaerker sein als klassische Vault Economy Calls.
- Ergebnisse sollen nicht nur `true` oder `false` sein.
- Geld wird nicht als `double` gespeichert.
- Storage, Formatting und Business-Regeln bleiben getrennt.

## Grundprinzipien

- Interne Betraege werden als `long minorUnits` modelliert.
- Beispiel: `1234` minorUnits bei 2 Dezimalstellen entspricht `12.34`.
- `Currency` definiert Dezimalstellen, Symbol und Anzeigenamen.
- Parsing und Formatting sind nicht Teil der Storage-Schicht.
- Spieler werden ueber UUID identifiziert.
- Namen sind nur Cache oder Anzeige.

## EconomyService

Aktuelle Verantwortungen:

- Account suchen oder erstellen
- Balance lesen
- Deposit ausfuehren
- Withdraw ausfuehren
- Transfer ausfuehren
- Limits pruefen
- Transaktionen erzeugen

Aktuelle Methoden:

```java
Currency defaultCurrency();

Account getOrCreatePlayerAccount(UUID playerUuid, String playerName);
CompletionStage<Account> getOrCreatePlayerAccountAsync(UUID playerUuid, String playerName);

Optional<Account> findAccount(AccountId accountId);
CompletionStage<Optional<Account>> findAccountAsync(AccountId accountId);

Money getBalance(AccountId accountId, Currency currency);
CompletionStage<Money> getBalanceAsync(AccountId accountId, Currency currency);

TransactionResult deposit(AccountId target, Money amount, TransactionReason reason);
CompletionStage<TransactionResult> depositAsync(AccountId target, Money amount, TransactionReason reason);

TransactionResult withdraw(AccountId source, Money amount, TransactionReason reason);
CompletionStage<TransactionResult> withdrawAsync(AccountId source, Money amount, TransactionReason reason);

TransactionResult transfer(AccountId source, AccountId target, Money amount, TransactionReason reason);
CompletionStage<TransactionResult> transferAsync(AccountId source, AccountId target, Money amount, TransactionReason reason);

TransactionResult setBalance(AccountId accountId, Money amount, TransactionReason reason);
CompletionStage<TransactionResult> setBalanceAsync(AccountId accountId, Money amount, TransactionReason reason);

List<BalanceEntry> topBalances(Currency currency, int limit);
CompletionStage<List<BalanceEntry>> topBalancesAsync(Currency currency, int limit);

List<Transaction> recentTransactions(AccountId accountId, Currency currency, int limit);
CompletionStage<List<Transaction>> recentTransactionsAsync(AccountId accountId, Currency currency, int limit);
```

Async ist der bevorzugte Integrationspfad fuer neue Plugins. Die synchronen Methoden bleiben fuer Bukkit Commands, Vault-Kompatibilitaet und einfache Legacy-Integrationen erhalten.

Core-Regel:

- Storage-Arbeit laeuft bei async Methoden auf einem TrailMoney-Worker.
- Bukkit/Paper Events werden trotzdem auf dem Server-Thread gefeuert.
- Consumer duerfen `CompletionStage#join()` nicht auf dem Server-Thread fuer normale Gameplay-Flows verwenden.
- Wenn ein Ergebnis wieder Bukkit-API beruehren muss, muss der Consumer zurueck auf den Server-Thread schedulen.

## Account

Ein Account steht fuer eine Geldentitaet.

Geplante Felder:

- `AccountId id`
- `AccountType type`
- `UUID ownerUuid`, falls Spieler-Account
- `String displayName`
- `Instant createdAt`
- `Instant lastSeenAt`

Geplante Account-Typen:

- `PLAYER`
- `SERVER`
- `VIRTUAL`

Spieler-Accounts nutzen UUID als Primary Identity. Namen werden nur fuer Anzeige und Cache verwendet.

## Money

`Money` ist ein Value Object.

Geplante Felder:

- `long minorUnits`
- `CurrencyKey currency`

Regeln:

- Keine Speicherung als `double`.
- Negative Werte sind fuer Balance-Aenderungen nur erlaubt, wenn die jeweilige Operation das explizit erlaubt.
- `Money` macht keine Locale-Formatierung.
- `Money` kennt keine Datenbank.

## Currency

Geplante Felder:

- `String key`
- `String displayName`
- `String symbol`
- `int decimals`

MVP:

- Eine Default-Waehrung.

Spaeter:

- Multi-Currency, ohne die MVP-API zu brechen.

## Transaction

Jede relevante Balance-Aenderung soll nachvollziehbar sein.

Geplante Felder:

- `UUID id`
- `AccountId source`
- `AccountId target`
- `Money amount`
- `TransactionReason reason`
- `Instant timestamp`
- `TransactionStatus status`
- `String actor`
- `String metadata`

`source` oder `target` koennen bei Admin-Operationen oder Systemoperationen leer bzw. systemisch sein. Das muss in der API klar modelliert werden, nicht mit magischen Strings.

## TransactionResult

Kein simples `boolean`.

Geplante Statuswerte:

- `SUCCESS`
- `INSUFFICIENT_FUNDS`
- `ACCOUNT_NOT_FOUND`
- `INVALID_AMOUNT`
- `LIMIT_EXCEEDED`
- `CANCELLED`
- `STORAGE_ERROR`

Geplante Inhalte:

- Status
- optionale Transaction ID
- lesbare Fehlermeldung fuer Logs
- optionaler Fehlercode fuer andere Plugins

## Events

Events sollen Paper/Bukkit-kompatibel geplant werden.

Geplante Events:

- `BalanceChangeEvent`
- `TransactionPreEvent`
- `TransactionPostEvent`
- `AccountCreateEvent`

Regeln:

- `TransactionPreEvent` kann cancellable sein.
- `TransactionPostEvent` informiert nur nach Abschluss.
- Storage-Fehler sollen nicht still verschwinden.
- Events duerfen keine halbfertigen Transaktionen sichtbar machen.

## ServicesManager

TrailMoney Core soll seine eigene API ueber den Bukkit/Paper `ServicesManager` registrieren.

Geplantes Muster:

```java
server.getServicesManager().register(
    EconomyService.class,
    economyService,
    plugin,
    ServicePriority.Normal
);
```

Andere Plugins sollen TrailMoney ueber diese API oder eine kleine API-Dependency nutzen, nicht ueber interne Core-Klassen.

## Nutzung in anderen Plugins

Andere Plugins sollen gegen `trailmoney-api` kompilieren und zur Laufzeit den Service aus dem Bukkit `ServicesManager` lesen.

Gradle-Beispiel:

```kotlin
dependencies {
    compileOnly("de.trailmoney:trailmoney-api:<version>")
}
```

`plugin.yml` Beispiel:

```yaml
softdepend:
  - TrailMoney
```

Java-Beispiel:

```java
RegisteredServiceProvider<EconomyService> registration =
    Bukkit.getServicesManager().getRegistration(EconomyService.class);

if (registration == null) {
    // TrailMoney ist nicht installiert oder noch nicht registriert.
    return;
}

EconomyService economy = registration.getProvider();
Currency currency = economy.defaultCurrency();
Account account = economy.getOrCreatePlayerAccount(player.getUniqueId(), player.getName());

economy.depositAsync(
    account.id(),
    Money.ofMinor(250, currency),
    TransactionReason.plugin(plugin.getName(), "quest_reward")
).thenAccept(result -> {
    if (!result.successful()) {
        plugin.getLogger().warning("TrailMoney deposit failed: " + result.code());
    }
});
```

Regeln fuer Integrationen:

- Keine Core-Klassen wie `de.trailmoney.core.*` verwenden.
- Kein Geld als `double` an TrailMoney vorbei rechnen.
- Fuer Spieler immer UUIDs nutzen.
- Fehler immer ueber `TransactionResult#code()` behandeln, nicht nur ueber Nachrichten.
- Neue Integrationen sollen async Methoden bevorzugen.
- Vault nur nutzen, wenn ein altes Plugin nicht direkt gegen `trailmoney-api` integriert werden kann.

## Nicht-Ziele der API

- Keine Permissions-API.
- Keine Chat-API.
- Keine LuckPerms-Ersatzlogik.
- Keine Commands in der API.
- Keine Datenbank-Implementierung in der API.
- Keine direkten Hooks auf private Plugins.
