# TrailMoney Roadmap

Diese Roadmap ist absichtlich schlank gehalten. TrailMoney soll realistisch fuer ein Paper Plugin bleiben.

## Phase 0: Planung

- Dokumente fuer Architektur, API, Storage, Vault und LuckPerms erstellen.
- Offene Entscheidungen markieren.
- Keine Implementierung.

## Phase 1: Projektgrundlage

- Gradle Kotlin DSL Multi-Modul-Projekt anlegen.
- Java Toolchain 25 konfigurieren.
- Paper API 26.1.2 Dependency setzen.
- Grundmodule vorbereiten:
  - `trailmoney-api`
  - `trailmoney-core`
  - `trailmoney-vault-bridge`
- Klassische `plugin.yml` fuer Core planen.

## Phase 2: Core MVP

- Plugin Lifecycle.
- Config Loading.
- SQLite Storage.
- Migrationen.
- Account-Erstellung ueber UUID.
- Balance Lesen und Schreiben.
- Transaktionslog.
- Permissions.
- Events.

## Phase 3: Commands MVP

- `/money`
- `/money <spieler>`
- `/pay <spieler> <betrag>`
- `/eco give <spieler> <betrag>`
- `/eco take <spieler> <betrag>`
- `/eco set <spieler> <betrag>`
- `/eco reload`
- `/eco top`

## Phase 4: API Stabilisierung

- Public API pruefen.
- Beispielintegration schreiben.
- ServicesManager Registrierung dokumentieren.
- Keine internen Core-Klassen fuer externe Plugins freigeben.

## Phase 5: Vault Bridge

- Vault Economy Provider registrieren.
- Vault Methoden auf TrailMoney API mappen.
- Bank API Grenzen dokumentieren.
- `provides: [Vault]` testen.
- Kompatibilitaet mit realen Shop-/Jobs-/Rankup-Plugins pruefen.

## Phase 6: LuckPerms Hook

- Optionaler Hook.
- Meta Keys konfigurierbar.
- Fallbacks.
- Keine hardcoded Gruppen.
- Core muss ohne LuckPerms starten.

## Phase 7: Public Release

- README.
- Admin-Doku.
- Developer-Doku.
- License.
- GitHub Actions Build.
- GitHub Release.
- Modrinth.
- Hangar.
- Optional SpigotMC.

## Spaeter

- MariaDB/MySQL.
- PostgreSQL.
- Multi-Currency.
- Bankkonten.
- Gebuehren und Taxes.
- Daily Rewards.
- Importer von EssentialsX und anderen Vault-Economies.
- Webhooks oder externe API.
- Rank-Shop Addon als separates Plugin.

## Nicht in den Core ziehen

- Rank-Shop.
- Jobs-System.
- Daily-Rewards-Komplettsystem.
- Webpanel.
- Permissions-Verwaltung.
- Chat-Verwaltung.
- Private Plugin-Hardcodes.
