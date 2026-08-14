# Home Assistant (MOBEX) — Verteilung ins Arbeitsprofil

Anleitung für die Bereitstellung der internen Home-Assistant-App über Intune / Managed Google Play.

## Was ist das für eine App?

Ein Fork der offiziellen [Home Assistant Companion App](https://play.google.com/store/apps/details?id=io.homeassistant.companion.android) mit einer Zusatzfunktion: Der Login öffnet sich im System-Browser (Edge) statt in der eingebetteten WebView. Dadurch funktioniert die Anmeldung per Microsoft-SSO (OIDC-Integration auf dem HA-Server) ohne Geräte-Code — Edge im Arbeitsprofil ist bereits am Firmenkonto angemeldet und erfüllt die Conditional-Access-Anforderungen (konformes Gerät).

| | |
|---|---|
| Package-ID | `de.mobex.homeassistant.minimal` |
| Variante | `minimal` (FOSS, ohne Google-Play-Dienste) |
| Benachrichtigungen | Direkte WebSocket-Verbindung zum HA-Server (kein Firebase/FCM, keine Daten über Dritt-Server) |
| Signatur | Eigener MOBEX-Release-Key (nicht der Play-Store-Key der offiziellen App) |

Die eigene Package-ID ist nötig, weil `io.homeassistant.companion.android` im Play Store von der offiziellen App belegt ist — beide Apps können dadurch parallel existieren.

## APK beziehen

Die aktuelle APK liegt unter [Releases](../../releases) und wird bei jeder Änderung am Branch `browser-sso` automatisch neu gebaut. Der Dateiname enthält den Version Code, z. B. `home-assistant-mobex-29766240.apk`.

## Upload in Intune (einmalig)

1. **Intune Admin Center** → **Apps** → **Android** → **Hinzufügen**
2. App-Typ: **Branchenspezifische App** (Line-of-Business)
3. Die APK hochladen
4. Metadaten ausfüllen (Name z. B. „Home Assistant (MOBEX)", Herausgeber „MOBEX")
5. **Zuweisen** an die gewünschte Mitarbeiter-Gruppe (Erforderlich oder Verfügbar)

Intune veröffentlicht die APK automatisch als **private App** im Managed Google Play der Organisation — sie ist nur für MOBEX sichtbar. Die erste Synchronisierung zu Google Play kann einige Minuten bis wenige Stunden dauern.

Für Updates wird im **selben Intune-App-Eintrag** die neue APK hochgeladen (Eigenschaften → App-Paketdatei bearbeiten). Der Version Code steigt bei jedem Pipeline-Build automatisch, die Signatur bleibt gleich — beides Voraussetzung dafür, dass Android das Update akzeptiert.

## Voraussetzungen im Arbeitsprofil

- **Microsoft Edge** muss im Arbeitsprofil vorhanden und **Standard-Browser** sein. Der Login öffnet sich als Custom Tab im Standard-Browser, und nur Edge kann gegenüber Entra ID die Gerätekonformität nachweisen (Chrome nicht).
- Die Home-Assistant-URL muss aus dem Arbeitsprofil erreichbar sein.
- Auf dem HA-Server muss der angepasste `hass-oidc-auth`-Fork laufen (Login-Button auf der Code-Seite), sonst erscheint statt des SSO-Logins weiterhin nur der Geräte-Code.
- Falls die offizielle Home-Assistant-App bisher zugewiesen war: Zuweisung entfernen, um Verwechslung zu vermeiden (beide Apps können technisch parallel installiert sein).

---

# Wartung des Forks

## Branches

| Branch | Zweck |
|---|---|
| `main` | Unveränderte Kopie von [home-assistant/android](https://github.com/home-assistant/android). Nie direkt committen — nur so bleibt der „Sync fork"-Button nutzbar. |
| `browser-sso` | Der ausgelieferte Stand: `main` plus Browser-Login und MOBEX-Paketierung. Hier bauen die Pipelines. Default-Branch, weil GitHub geplante Workflows ausschließlich aus dem Default-Branch startet. Gegen Force-Push und Löschen per Ruleset geschützt. |

> **Beim „Sync fork"-Button vorher auf `main` umschalten.** Der Button bezieht sich immer auf den gerade angezeigten Branch; auf `browser-sso` bietet er nur „Discard commits" an, was alle Anpassungen verwerfen würde. Das Ruleset blockiert diesen Fall, die Meldung ist dann aber verwirrend.

## Upstream-Änderungen übernehmen

Der Workflow **MOBEX Sync upstream** läuft montags automatisch (und lässt sich jederzeit über den Actions-Tab manuell starten). Er aktualisiert `main` und öffnet einen Pull Request nach `browser-sso`. Zum Übernehmen:

1. Den offenen Pull Request „Sync upstream (…)" öffnen
2. Warten, bis der Check **MOBEX Check** grün ist (Code-Style, Unit-Tests, Debug-Build)
3. **Merge pull request** klicken → die APK-Pipeline baut anschließend automatisch ein neues Release

Bei Merge-Konflikten ist der Merge-Button gesperrt. Dann lokal auflösen:

```bash
git fetch origin
git switch browser-sso && git merge origin/main
# Konflikte lösen (erfahrungsgemäß: gradle/libs.versions.toml, *.lockfile, changelog_master.xml)
./gradlew alldependencies --write-locks   # nach Abhängigkeitskonflikten
./gradlew ktlintFormat && ./gradlew :app:testFullDebugUnitTest
git push
```

Meldet der Workflow, dass `main` nicht automatisch aktualisiert werden konnte, einmal den **„Sync fork"**-Button auf der Repository-Seite drücken und den Workflow erneut starten. Ursache: Das automatische Token darf keine Änderungen an `.github/workflows/` pushen — das passiert immer dann, wenn Upstream seine eigenen Workflows anfasst.

## Benötigte Repository-Secrets

Unter **Settings → Secrets and variables → Actions**:

| Secret | Inhalt |
|---|---|
| `MOBEX_KEYSTORE` | Der Keystore, base64-kodiert: `base64 -w0 mobex-ha-release.keystore` |
| `MOBEX_KEYSTORE_PASSWORD` | Das Keystore-Passwort (Store- und Key-Passwort sind identisch) |

Der Key-Alias ist `mobex-ha` und steht fest im Workflow.

> **Der Keystore muss gesichert werden.** Geht er verloren, kann die verteilte App nie wieder aktualisiert werden — Android akzeptiert Updates nur mit identischer Signatur. Da die Secrets jedem Workflow dieses Repositories zur Verfügung stehen, sollten Upstream-Änderungen an `.github/workflows/` vor dem Merge kurz gesichtet werden.

## Upstream-Workflows abschalten

Der Fork erbt alle CI-Workflows des offiziellen Projekts. Die schlagen mangels Secrets fehl und erzeugen rote Häkchen und Mails. Einmalig im **Actions**-Tab jeden Workflow außer den drei `MOBEX …`-Workflows auswählen und über „•••" → **Disable workflow** deaktivieren. Das ändert keine Dateien und überlebt daher jeden Sync.

## Lokal bauen

```bash
# Einmalig: Mock-Konfigurationen für den Build (gitignored)
cp .github/mock-google-services.json app/src/debug/google-services.json
cp .github/mock-google-services.json app/src/minimal/google-services.json

export KEYSTORE_PATH="$HOME/keystores/mobex-ha-release.keystore"
export KEYSTORE_PASSWORD="<siehe mobex-ha-release.txt>"
export KEYSTORE_ALIAS="mobex-ha"
export KEYSTORE_ALIAS_PASSWORD="<siehe mobex-ha-release.txt>"
# Der Version Code muss über dem des letzten Pipeline-Builds liegen
VERSION_CODE=$(( $(date -u +%s) / 60 )) ./gradlew :app:assembleMinimalRelease
# Ergebnis: app/build/outputs/apk/minimal/release/app-minimal-release.apk
```
