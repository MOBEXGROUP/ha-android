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

## Upload in Intune (einmalig)

1. **Intune Admin Center** → **Apps** → **Android** → **Hinzufügen**
2. App-Typ: **Branchenspezifische App** (Line-of-Business)
3. Die APK `app-minimal-release.apk` hochladen
4. Metadaten ausfüllen (Name z. B. „Home Assistant (MOBEX)", Herausgeber „MOBEX")
5. **Zuweisen** an die gewünschte Mitarbeiter-Gruppe (Erforderlich oder Verfügbar)

Intune veröffentlicht die APK automatisch als **private App** im Managed Google Play der Organisation — sie ist nur für MOBEX sichtbar. Die erste Synchronisierung zu Google Play kann einige Minuten bis wenige Stunden dauern.

## Voraussetzungen im Arbeitsprofil

- **Microsoft Edge** muss im Arbeitsprofil vorhanden und **Standard-Browser** sein. Der Login öffnet sich als Custom Tab im Standard-Browser, und nur Edge kann gegenüber Entra ID die Gerätekonformität nachweisen (Chrome nicht).
- Die Home-Assistant-URL muss aus dem Arbeitsprofil erreichbar sein.
- Auf dem HA-Server muss der angepasste `hass-oidc-auth`-Fork laufen (Login-Button auf der Code-Seite), sonst erscheint statt des SSO-Logins weiterhin nur der Geräte-Code.
- Falls die offizielle Home-Assistant-App bisher zugewiesen war: Zuweisung entfernen, um Verwechslung zu vermeiden (beide Apps können technisch parallel installiert sein).

## Updates verteilen

Neue Versionen werden im **selben Intune-App-Eintrag** hochgeladen (Eigenschaften → App-Paketdatei bearbeiten). Zwei Bedingungen:

1. Gleiche Signatur (derselbe MOBEX-Release-Keystore)
2. Höherer `versionCode` als die vorherige Version (Schema: Datum `JJJJMMTT`, z. B. `20260813`)

## Build reproduzieren (Entwickler)

```bash
# Einmalig: Mock-Konfigurationen für den Build (gitignored)
cp .github/mock-google-services.json app/src/debug/google-services.json
cp .github/mock-google-services.json app/src/minimal/google-services.json

# Release-Build, signiert mit dem MOBEX-Keystore (Zugangsdaten: ~/keystores/mobex-ha-release.txt)
export KEYSTORE_PATH="$HOME/keystores/mobex-ha-release.keystore"
export KEYSTORE_PASSWORD="<siehe mobex-ha-release.txt>"
export KEYSTORE_ALIAS="mobex-ha"
export KEYSTORE_ALIAS_PASSWORD="<siehe mobex-ha-release.txt>"
VERSION_CODE=$(date +%Y%m%d) ./gradlew :app:assembleMinimalRelease
# Ergebnis: app/build/outputs/apk/minimal/release/app-minimal-release.apk
```

**Wichtig:** Der Keystore `mobex-ha-release.keystore` muss sicher aufbewahrt und gesichert werden — ohne ihn können keine Updates mehr für die bereits verteilte App signiert werden.
