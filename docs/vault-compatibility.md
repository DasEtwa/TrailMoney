# TrailMoney Vault Compatibility

TrailMoney soll optional Vault Economy kompatibel sein. Diese Kompatibilitaet ist eine Bridge, nicht das interne Economy-System.

## Ziel

Alte Plugins, die Vault Economy erwarten, sollen moeglichst mit TrailMoney funktionieren. TrailMoney ersetzt aber nicht Vault Permissions oder Vault Chat.

## Nicht-Ziele

- Kein Vault Permissions Provider.
- Kein Vault Chat Provider.
- Keine Kopie der Vault-Architektur.
- Kein hardcoded Support fuer einzelne alte Plugins.

## Grundmodell

Das Modul `trailmoney-vault-bridge` registriert einen Vault Economy Provider ueber den Bukkit/Paper `ServicesManager`.

Konzept:

```java
server.getServicesManager().register(
    net.milkbowl.vault.economy.Economy.class,
    new TrailMoneyVaultEconomy(economyService),
    plugin,
    ServicePriority.Normal
);
```

Die Bridge nutzt intern ausschliesslich die TrailMoney API.

## Geplantes Method Mapping

Vault Economy Methode:

- `getBalance(player)` -> TrailMoney `getBalance(playerAccount)`
- `has(player, amount)` -> Balance-Vergleich ueber minorUnits
- `depositPlayer(player, amount)` -> TrailMoney `deposit`
- `withdrawPlayer(player, amount)` -> TrailMoney `withdraw`
- `createPlayerAccount(player)` -> TrailMoney `getOrCreateAccount`
- `format(amount)` -> TrailMoney Formatter fuer Default Currency

Regeln:

- Vault liefert oft `double`. Die Bridge muss sauber nach `minorUnits` konvertieren.
- Rundungsregeln muessen dokumentiert und konfiguriert werden.
- Das interne System speichert nie `double`.

## Vault Bank API

Vault enthaelt Bank-Methoden. TrailMoney MVP plant noch keine Bankkonten.

Optionen:

- Bank-Methoden geben bewusst `NOT_IMPLEMENTED` oder Failure zurueck.
- Spaeter koennen Bankkonten ueber TrailMoney AccountType `VIRTUAL` oder eigenes Bank-Modul umgesetzt werden.

MVP-Entscheidung:

- Bank API wird nicht als Core-Feature versprochen.
- Die Bridge dokumentiert klar, welche Methoden nicht unterstuetzt werden.

## plugin.yml Planung

Bridge als eigenes Plugin:

```yaml
name: TrailMoneyVaultBridge
main: de.trailmoney.vault.TrailMoneyVaultBridgePlugin
version: '${version}'
api-version: '26.1.2'
depend:
  - TrailMoney
softdepend:
  - Vault
provides:
  - Vault
```

## provides: [Vault]

`provides` kann Paper/Bukkit signalisieren, dass ein Plugin eine andere Plugin-Funktionalitaet bereitstellt. Fuer alte Plugins mit `softdepend: [Vault]` oder einfachen Plugin-Checks kann das helfen.

Einschraenkungen:

- Es garantiert nicht, dass echte Vault-Klassen vorhanden sind.
- Es garantiert nicht, dass ein Plugin exakt `Vault` als Plugin-Name erwartet.
- Es garantiert nicht, dass alte Plugins ihre Economy-Abfrage zum richtigen Zeitpunkt machen.
- Es ersetzt keine lizenzrechtliche Pruefung der VaultAPI.

## loadbefore

`loadbefore` kann helfen, wenn bekannte Plugins nach Vault/Services suchen und TrailMoney Bridge vorher geladen werden soll.

MVP-Regel:

- Kein grosses hardcoded `loadbefore`-Array fuer beliebige Plugins.
- Nur dokumentieren und spaeter gezielt nutzen, falls echte Kompatibilitaetsfaelle auftauchen.

Beispiel fuer spaetere gezielte Kompatibilitaet:

```yaml
loadbefore:
  - SomeShopPlugin
```

Das soll nicht von Anfang an breit genutzt werden.

## Harte Vault-Abhaengigkeiten alter Plugins

Viele alte Plugins pruefen eventuell:

- ob ein Plugin exakt `Vault` heisst
- ob Vault-Klassen vorhanden sind
- ob Economy schon beim Start registriert ist
- ob `depend: [Vault]` erfuellt ist

Daraus folgen Risiken:

- `TrailMoneyVaultBridge` mit `provides: [Vault]` kann reichen, muss aber nicht.
- Ein Spezial-Jar mit Plugin-Name `Vault` koennte spaeter noetig sein, ist aber eine eigene Release- und Lizenzentscheidung.
- Plugins, die Vault-Klassen direkt referenzieren, brauchen die VaultAPI zur Compile-/Runtime-Kompatibilitaet.

## Lizenzhinweis

VaultAPI steht unter LGPL-3.0. Direkte Nutzung, Implementierung, Shading oder Distribution muss vor Release geprueft werden.

Quelle: https://github.com/MilkBowl/VaultAPI

Ziel:

- TrailMoney Core/API bleibt eigenstaendig.
- Vault-Kompatibilitaet bleibt optional und klar getrennt.
