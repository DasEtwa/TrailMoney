# TrailMoney

TrailMoney is a Paper-first economy plugin for Minecraft Paper 26.1.2.

It is planned as a real economy core with its own public API, SQLite storage, commands, events, and an optional Vault Economy bridge. It is not a Vault permissions/chat replacement and it does not replace LuckPerms.

## Current Status

Early implementation skeleton.

Implemented target modules:

- `trailmoney-api`: public API models, service interface, and events.
- `trailmoney-core`: Paper plugin, SQLite storage, commands, config, and service registration.
- `trailmoney-vault-bridge`: optional Vault Economy provider bridge.

## Requirements

- Paper 26.1.2
- Java 25
- Gradle 9.5+

## Build

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

Expected jars:

- `trailmoney-core/build/libs/TrailMoney-<version>.jar`
- `trailmoney-vault-bridge/build/libs/TrailMoneyVaultBridge-<version>.jar`

Install the core jar first. Install the Vault bridge jar only when compatibility with Vault Economy plugins is needed. The current bridge requires a real Vault jar at runtime and registers TrailMoney as a Vault Economy provider. A full no-Vault replacement jar is a separate packaging/licensing task.

## Design Boundaries

- No NMS or Minecraft internals.
- Money is stored as `long` minor units, never as `double`.
- Player identity uses UUIDs, not names.
- LuckPerms is optional and must never be required for core startup.
- Vault compatibility is a bridge only.

See `docs/` for the planning documents.
