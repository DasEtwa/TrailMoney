# TrailMoney LuckPerms Support

TrailMoney ersetzt LuckPerms nicht. LuckPerms bleibt das Rank- und Permission-System. TrailMoney kann optional LuckPerms Meta-Werte lesen.

## Ziele

- Core startet ohne LuckPerms.
- Normale Permission Checks laufen ueber Bukkit/Paper.
- LuckPerms Meta-Werte koennen Economy-Regeln beeinflussen.
- Keine hardcoded Gruppen.
- Alle Meta Keys sind konfigurierbar.

## Nicht-Ziele

- Kein Permission-System im TrailMoney Core.
- Kein Rank-System.
- Kein Gruppen-Shop im Core.
- Keine festen Gruppen wie `vip`, `owner` oder `helper`.

## Geplante Meta Keys

- `trailmoney.multiplier`
- `trailmoney.max-balance`
- `trailmoney.start-balance`

Beispiele:

```text
/lp group vip meta set trailmoney.multiplier 1.25
/lp group vip meta set trailmoney.max-balance 500000
/lp group tester meta set trailmoney.start-balance 1000
```

Diese Beispiele sind nur Beispiele. TrailMoney darf keine Gruppen hart einbauen.

## Config-Idee

```yaml
luckperms:
  enabled: true
  use-meta: true
  meta:
    multiplier-key: "trailmoney.multiplier"
    max-balance-key: "trailmoney.max-balance"
    start-balance-key: "trailmoney.start-balance"
  fallback:
    multiplier: 1.0
    max-balance: -1
```

## Hook-Regeln

- LuckPerms ist `softdepend`, kein `depend`.
- Wenn LuckPerms fehlt, werden Fallback-Werte genutzt.
- Wenn Meta fehlt oder ungueltig ist, werden Fallback-Werte genutzt und optional gewarnt.
- TrailMoney darf nicht crashen, nur weil LuckPerms nicht installiert ist.
- Meta Parsing muss klar und konservativ sein.

## Geplante Nutzungen

### Multiplier

Kann spaeter fuer Rewards, Jobs, externe Plugins oder Addons genutzt werden.

MVP-Hinweis:

- Core Economy-Transfers sollten nicht automatisch multipliziert werden.
- Multiplikatoren sind eher fuer Reward-Quellen sinnvoll, nicht fuer `/pay`.

Aktueller Implementierungsstand:

- `trailmoney.multiplier` wird vom Hook lesbar gemacht, aber im Core noch nicht automatisch auf Transfers angewendet.
- Damit vermeidet TrailMoney unerwartete Multiplikation bei `/pay`, Admin-Operationen oder Vault-Bridge-Aufrufen.

### Max Balance

Kann Max-Balance pro Spieler erhoehen oder begrenzen.

Regel:

- Meta-Wert ueberschreibt oder ergaenzt die Config nur nach klar dokumentierter Prioritaet.

Aktueller Implementierungsstand:

- Wenn LuckPerms installiert ist und Userdaten geladen oder nachladbar sind, ueberschreibt `trailmoney.max-balance` das globale `economy.max-balance`.
- Wenn kein Meta-Wert vorhanden ist, nutzt TrailMoney `luckperms.fallback.max-balance`, falls gesetzt.
- Wenn auch dieser Wert `-1` ist, nutzt TrailMoney `economy.max-balance`.

### Start Balance

Kann Startgeld fuer neue Accounts beeinflussen.

Regel:

- Wird beim Account-Erstellen ausgewertet.
- Spaetere Meta-Aenderungen aendern bestehende Balances nicht automatisch.

Aktueller Implementierungsstand:

- Wenn LuckPerms installiert ist und Userdaten geladen oder nachladbar sind, ueberschreibt `trailmoney.start-balance` das globale `economy.start-balance` bei der Account-Erstellung.

## Technische Risiken

- LuckPerms Meta kann gecached sein und sich aendern.
- Offline-Spieler-Meta ist je nach API-Aufruf nicht immer sofort verfuegbar.
- Async Loading muss sauber behandelt werden.
- Economy-Operationen duerfen nicht blockierend auf externe Meta-Ladevorgaenge warten, wenn das den Server belastet.

## Offene Entscheidungen

- Ob LuckPerms Hook im Core liegt oder als eigenes Modul `trailmoney-luckperms-hook` ausgelagert wird.
- Ob Meta-Werte live gelesen oder gecached werden.
- Wie Cache-Invalidierung bei LuckPerms Events umgesetzt wird.
