# TrailMoney Plan

TrailMoney ist ein geplantes Paper-first Economy-Plugin fuer Minecraft Paper 26.1.2. Es soll ein eigenes, modernes Economy-Core-System bieten und nicht nur Vault nachbauen.

## Paper 26.1.2 Fakten

Stand der Planung: 2026-06-10.

- Paper 26.1.2 existiert und ist ueber PaperMC verfuegbar.
- Die geplante Paper API Dependency ist:

```kotlin
compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
```

- Die Java Toolchain soll zu Paper 26.1.2 passen. Paper 26.1+ dokumentiert Java 25.
- TrailMoney nutzt keine NMS- oder Minecraft-Internals.
- Fuer maximale Kompatibilitaet wird zuerst eine klassische `plugin.yml` geplant.
- `paper-plugin.yml` wird vorerst nicht als Hauptformat genutzt, weil Paper Plugins weiterhin andere Classloading- und Lifecycle-Eigenschaften haben und fuer Bridge-Kompatibilitaet unnoetige Risiken erzeugen koennen.

Quellen fuer diese Planungsannahmen:

- Paper Downloads: https://papermc.io/downloads/paper
- Paper Project Setup: https://docs.papermc.io/paper/dev/project-setup/
- Paper Getting Started: https://docs.papermc.io/paper/getting-started/
- Paper plugin.yml: https://docs.papermc.io/paper/dev/plugin-yml/
- Paper Plugins: https://docs.papermc.io/paper/dev/getting-started/paper-plugins/

## Was TrailMoney ist

- Ein eigenes Economy-System fuer Paper Server.
- Eine oeffentliche API fuer andere Plugins.
- Ein Paper Plugin mit Commands, Permissions, Events, Storage und Konfiguration.
- Optional kompatibel zu Vault Economy ueber eine Bridge.
- Optional LuckPerms-aware fuer Meta-Werte wie Multiplikatoren und Limits.
- Oeffentlich releasbar und dokumentiert.

## Was TrailMoney nicht ist

- Kein Vault-Klon.
- Kein Ersatz fuer Vault Permissions oder Vault Chat.
- Kein Ersatz fuer LuckPerms.
- Kein Rank-Shop im Core.
- Kein Plugin mit hardcoded Integrationen in andere private Plugins.
- Kein NMS-basiertes System.
- Kein Enterprise-Framework fuer ein einfaches Minecraft-Plugin.

## Warum kein reiner Vault-Klon

Vault ist vor allem eine API und Bridge fuer Economy, Permissions und Chat. TrailMoney soll nur Economy abdecken. Die interne API soll moderner sein als Vaults `boolean`- und `double`-orientiertes Economy-Modell.

Vault-Kompatibilitaet ist wichtig fuer alte Plugins, aber sie ist nur eine optionale Aussenhuelle. Das interne System bleibt TrailMoney-eigen.

## Geplante Module

### trailmoney-api

Oeffentliche API fuer andere Plugins.

Geplante Inhalte:

- `EconomyService`
- `Account`
- `AccountType`
- `Currency`
- `Money`
- `Transaction`
- `TransactionResult`
- Event-Typen oder Event-Contracts

Regeln:

- Keine Paper-Internals, wenn vermeidbar.
- Keine Storage-Implementierung.
- Keine Commands.
- Keine direkten Abhaengigkeiten zu privaten Plugins.

### trailmoney-core

Das eigentliche Paper Plugin.

Geplante Inhalte:

- Plugin Lifecycle
- Config Loading
- Commands
- Permissions
- Storage
- Migrations
- Bukkit/Paper `ServicesManager` Registrierung fuer TrailMoney API
- Events
- SQLite Default Storage

### trailmoney-vault-bridge

Optionales Kompatibilitaetsmodul fuer Plugins, die Vault Economy erwarten.

Geplante Inhalte:

- Registrierung eines Vault Economy Providers ueber den Bukkit/Paper `ServicesManager`
- Mapping von Vault Economy Calls auf TrailMoney API
- Dokumentierte Einschraenkungen fuer Vault Bank API
- Pruefung von `provides: [Vault]`, `softdepend`, `depend` und `loadbefore`

### trailmoney-luckperms-hook

Optional spaeter als eigenes Modul oder zuerst intern unter `hooks/luckperms`.

Regeln:

- LuckPerms ist nur optional.
- Core startet vollstaendig ohne LuckPerms.
- Normale Permission Checks laufen ueber Bukkit/Paper.
- Meta Keys sind konfigurierbar.
- Keine hardcoded Gruppen.

## MVP Features

- `/money`
- `/money <spieler>`
- `/pay <spieler> <betrag>`
- `/eco give <spieler> <betrag>`
- `/eco take <spieler> <betrag>`
- `/eco set <spieler> <betrag>`
- `/eco reload`
- `/eco top`
- Startgeld
- Min-Balance
- Max-Balance
- Transaktionslog
- Offline-Spieler-Support ueber UUID
- Config fuer Currency Name, Symbol und Dezimalstellen
- Permissions
- Events
- SQLite als Default Storage

## Spaetere Features

- MariaDB/MySQL Storage
- PostgreSQL optional
- Multi-Currency
- Bankkonten
- Gebuehren und Taxes
- Daily Rewards
- Importer von EssentialsX oder anderen Vault-Economies
- Webhooks oder externe API optional
- Rank-Shop als separates Addon, nicht im Core

## Build- und Tech-Plan

- Gradle Kotlin DSL
- Java Toolchain 25
- Paper API fuer 26.1.2
- Keine NMS
- Klassische `plugin.yml` fuer Kompatibilitaet
- CI mit GitHub Actions spaeter
- Releases auf GitHub, Modrinth, Hangar und optional SpigotMC spaeter

Beispiel fuer spaetere Gradle-Konfiguration:

```kotlin
plugins {
    java
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
}
```

## Geplante plugin.yml Richtung

Core:

```yaml
name: TrailMoney
main: de.trailmoney.core.TrailMoneyPlugin
version: '${version}'
api-version: '26.1.2'
load: POSTWORLD
```

Vault Bridge:

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

Diese Bridge-Konfiguration ist eine Planungsentscheidung, keine finale Zusage. `provides: [Vault]` kann bei manchen Plugins helfen, die nur Plugin-Abhaengigkeiten pruefen. Plugins, die echte Vault-Klassen laden oder exakt den Plugin-Namen `Vault` erwarten, koennen trotzdem Probleme machen.

## Lizenzoptionen

Moegliche Lizenzen:

- MIT: einfach, permissiv, gut fuer kleine oeffentliche Plugins.
- Apache-2.0: permissiv, mit expliziter Patentklausel.

Hinweis:

- Direkte VaultAPI-Nutzung oder Implementierung muss lizenzrechtlich geprueft werden.
- VaultAPI steht unter LGPL-3.0. Das kann fuer Packaging, Shading und Distribution relevant sein.
- Ziel: TrailMoney Core/API moeglichst eigenstaendig und offen lizenzieren.

Quelle fuer VaultAPI: https://github.com/MilkBowl/VaultAPI

## Technische Risiken

- Harte Vault-Abhaengigkeiten alter Plugins koennen schwer sauber zu emulieren sein.
- Economy-Registrierung muss frueh genug erfolgen, ohne den Core-Lifecycle unsauber zu machen.
- Storage-Transaktionen muessen atomar sein, besonders bei Transfers.
- Offline-Spieler-Namen duerfen nicht als Identity genutzt werden.
- Multi-Currency darf den MVP nicht verkomplizieren.
- LuckPerms Meta-Werte muessen optional und fehlertolerant sein.

## Offene Entscheidungen

- Projektgruppe und Package-Namen.
- Finaler Lizenztyp.
- Ob Vault Bridge ein separates Jar bleibt oder spaeter ein Spezial-Jar mit Plugin-Name `Vault` angeboten wird.
- Ob `Money` intern nur eine Currency erlaubt oder bereits multi-currency-faehig modelliert wird.
- Ob `eco top` aus SQLite live berechnet oder gecached wird.
- Welche Importer zuerst kommen.
