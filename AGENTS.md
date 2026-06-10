# TrailMoney Agent Notes

Diese Datei gibt spaeteren Codex-Sessions Projektleitplanken.

## Projektziel

TrailMoney ist ein geplantes Paper-first Economy-Plugin fuer Paper 26.1.2.

Es soll:

- ein eigenes Economy-Core-System bieten
- eine oeffentliche API fuer andere Plugins bereitstellen
- SQLite als MVP-Storage nutzen
- optional Vault Economy kompatibel sein
- optional LuckPerms Meta-Werte lesen
- oeffentlich releasbar sein

## Harte Grenzen

- Nicht ohne Auftrag implementieren, wenn der User nur Planung will.
- Kein NMS.
- Kein Vault Permissions oder Chat Ersatz.
- Kein LuckPerms Ersatz.
- Kein hardcoded Support fuer private Plugins.
- Kein Geld als `double` speichern.
- Keine Spieler-Namen als Primaeridentitaet nutzen.
- Kein Rank-Shop im Core.

## Architektur

Geplante Module:

- `trailmoney-api`
- `trailmoney-core`
- `trailmoney-vault-bridge`
- optional spaeter `trailmoney-luckperms-hook`

## Tech-Vorgaben

- Gradle Kotlin DSL.
- Java Toolchain 25.
- Paper API `io.papermc.paper:paper-api:26.1.2.build.+`.
- Klassische `plugin.yml` zuerst.
- `paper-plugin.yml` nur pruefen, nicht als Default annehmen.

## Dokumente

Vor Implementierung diese Dateien lesen:

- `docs/trailmoney-plan.md`
- `docs/api-design.md`
- `docs/storage-design.md`
- `docs/vault-compatibility.md`
- `docs/luckperms-support.md`
- `docs/roadmap.md`

## Stil

Sauber und modern, aber pragmatisch. Kein Enterprise-Muell fuer ein Minecraft Plugin.
