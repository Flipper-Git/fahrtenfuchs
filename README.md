# Fahrtenfuchs

Private App zur Abrechnung gefahrener Kilometer (0.30 CHF/km, abzüglich Tankkosten, monatliche Abrechnung als xlsx-Export).

## Status

Sprint 0 – Projekt-Grundgerüst (Kotlin, Jetpack Compose, GitHub Actions CI).

## Build

Lokal (benötigt JDK 17):

```
./gradlew assembleDebug
```

Die APK entsteht unter `app/build/outputs/apk/debug/app-debug.apk`.

Über GitHub Actions wird bei jedem Push auf `main` automatisch eine Debug-APK gebaut und als Workflow-Artefakt bereitgestellt (Tab "Actions" → letzter Lauf → "fahrtenfuchs-debug-apk").

## Installation auf dem Smartphone (GrapheneOS)

1. APK aus dem GitHub-Actions-Artefakt bzw. später einem Release herunterladen.
2. Beim ersten Öffnen der Datei einmalig "Installation von unbekannten Apps" für den Browser/Dateimanager erlauben.
3. APK installieren.

## Geplanter Funktionsumfang

- Kilometereingabe über 4 Modi: Drehrad, Zahleneingabe, Distanzberechnung zwischen Orten, Schnellwahl-Kacheln
- Vollständig offline Schweizer Orts- und Routingdaten (GraphHopper + OSM-Extrakt, einmaliger Download beim ersten Start)
- Monatliche Abrechnung mit xlsx-Export
