# TrailMoney API Design

Dieses Dokument beschreibt die geplante oeffentliche API. Es ist noch keine Implementierung.

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

Geplante Verantwortungen:

- Account suchen oder erstellen
- Balance lesen
- Deposit ausfuehren
- Withdraw ausfuehren
- Transfer ausfuehren
- Limits pruefen
- Transaktionen erzeugen

Moegliche Methoden als Konzept:

```java
CompletionStage<AccountLookupResult> findAccount(AccountId id);
CompletionStage<Account> getOrCreateAccount(AccountId id);
CompletionStage<Money> getBalance(AccountId id, CurrencyKey currency);
CompletionStage<TransactionResult> deposit(AccountId target, Money amount, TransactionReason reason);
CompletionStage<TransactionResult> withdraw(AccountId source, Money amount, TransactionReason reason);
CompletionStage<TransactionResult> transfer(AccountId source, AccountId target, Money amount, TransactionReason reason);
```

Ob die API synchron, async oder gemischt wird, bleibt offen. Fuer Storage-Operationen spricht vieles fuer async intern, aber Minecraft Commands und Events brauchen klare Main-Thread-Regeln.

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

## Nicht-Ziele der API

- Keine Permissions-API.
- Keine Chat-API.
- Keine LuckPerms-Ersatzlogik.
- Keine Commands in der API.
- Keine Datenbank-Implementierung in der API.
- Keine direkten Hooks auf private Plugins.
