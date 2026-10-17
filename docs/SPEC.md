# Spezifikation: grit-motivation (privater Fork von Grit)

Privater Fork von [shub39/Grit](https://github.com/shub39/Grit) (GPL-3.0, Kotlin Multiplatform,
Compose Material 3, Room). Ziel: kontextsensitives Motivations-/Sarkasmus-Pop-up beim Abhaken von
Habits (Phase 1); später Energie & Streak-Freeze, Statistiken und ein Avatar (siehe Abschnitt 4).

Basis-Stand: Upstream `master` @ `3e203449` (Release 6.2.2).

---

## 1. Ergebnisse der Vor-Analyse (Step 0)

### 1.1 Build-Konfiguration

| Punkt | Befund |
|---|---|
| Flavors | `play` und `foss` (`androidApp/build.gradle.kts`, Dimension `version`) |
| Billing im FOSS-Flavor | `androidApp/src/foss/.../billing/BillingHandler.kt` liefert fest `isPlus = true` → alle Plus-Features frei, **kein RevenueCat-Key nötig** |
| Analytics im FOSS-Flavor | No-op (`androidApp/src/foss/.../analytics/AnalyticsImpl.kt`), PostHog nur im `play`-Flavor |
| Build-Befehl | `./gradlew :androidApp:assembleFossDebug` |
| Toolchain | JDK 21, Gradle 9.8, AGP 9.4.1, Kotlin 2.4.21, compileSdk 37, minSdk 29 |
| Formatierung | Spotless + ktfmt (kotlinlang-Style) mit GPL-Lizenz-Header → `./gradlew spotlessApply` |
| Desktop-Hot-Reload | `./gradlew :desktopApp:hotRunJvm --auto` (schnelles UI-Testen ohne Emulator) |

**Wir arbeiten ausschließlich mit dem `foss`-Flavor.** Der `play`-Flavor bleibt unverändert.

### 1.2 Relevante Pfade

Präfix `shared/ui/src/commonMain/kotlin/com/shub39/grit/shared/ui/` = `UI/`

| Was | Wo |
|---|---|
| Habit-Liste (Screen-Section) | `UI/habit/ui/sections/HabitsList.kt` (`HabitsList` → `HabitsListContent`) |
| Habit-Karte mit Check-Button | `UI/habit/ui/component/HabitCard.kt` (Callback `onInsertStatus`) |
| Abhak-Actions | `UI/habit/HabitsAction.kt`: `InsertStatus(habit, date)` (Toggle) und `InsertMeasuredStatus(habit, date, value)` |
| Toggle-Logik (an/ab) | `UI/viewmodel/HabitViewModel.kt` → `insertHabitStatus(...)` – entscheidet bereits, ob Status gelöscht oder eingefügt wird |
| Screen-State | `UI/habit/HabitState.kt` |
| Streak-Berechnung | `shared/logic/.../habits/repository/Util.kt` → `countCurrentStreak(dates, eligibleWeekdays)` |
| Streak im Domain-Modell | `shared/core/.../habits/HabitWithAnalytics.kt` → `currentStreak` (befüllt in `HabitRepository`) |

### 1.3 Modul-Abhängigkeiten (wichtig für die Architektur)

```
androidApp ──► shared:ui ──► shared:core
          └──► shared:logic ──► shared:core
```

`shared:ui` hängt **nicht** von `shared:logic` ab – `countCurrentStreak` ist aus dem ViewModel also
nicht direkt aufrufbar.

---

## 2. Korrekturen gegenüber der Ursprungs-Spezifikation

Die ursprüngliche Spec (andere KI) wurde gegen den echten Code geprüft. Abweichungen:

1. **Check-In-Erkennung im ViewModel statt im Composable.**
   Die Spec wollte `remember { mutableStateOf(...) }` im Screen und dort `completedDates.contains(...)`
   prüfen. Das ViewModel trifft diese Entscheidung aber bereits in `insertHabitStatus`. Die doppelte
   Logik im UI würde UDF verletzen und kann auseinanderlaufen.
   → Popup-Daten werden Teil von `HabitState`, Schließen läuft über eine neue Action.

2. **Streak nicht als `currentStreak + 1` berechnen.**
   Falsch u. a. bei: Abhaken eines vergangenen Tages, Lückenschluss zwischen zwei Serien,
   Habits mit eingeschränkten Wochentagen.
   → Nach dem Insert den aktualisierten `HabitWithAnalytics.currentStreak` aus dem Repository-Flow
   lesen (`repo.getHabitsWithAnalytics().first()`). Damit gilt **eine** Wahrheit: dieselbe Zahl wie
   auf der Habit-Karte.

3. **Messbare Habits** (`InsertMeasuredStatus`) wurden nicht berücksichtigt.
   → Pop-up auch dort, aber nur wenn für das Datum vorher **kein** Status existierte und `value > 0`
   (also kein Pop-up beim Korrigieren eines Werts).

4. **Dateiaufteilung:** Statt einer Datei `HabitMotivationDialog.kt` für alles:
   - Engine + Modelle (reines Kotlin, ohne Compose/Room) in `shared:core`
     → Package `com.shub39.grit.core.motivation` – testbar per JVM-Unit-Test, strikt getrennt von DAOs.
   - Composable in `shared:ui` → `UI/habit/ui/component/HabitMotivationPopup.kt`.

5. **Zufall injizierbar:** `GritQuoteEngine.getQuote(activeDays, random: Random = Random.Default, ...)`
   für deterministische Tests.

6. **Grammatik:** „1 Tag aktiv“ / „n Tage aktiv“ statt „Tag(e)“.

7. **Übersetzbare Sprüche (Entscheidung aus dem Design-Interview):** `shared:core` kennt keine
   Compose-Resources. Die Engine liefert deshalb nur eine **ID** (plus Ton und Kategorie), den Text
   holt die UI aus String-Resources und setzt die Tage per Formatargument ein (`%1$d`).
   `MotivationQuote` hat daher kein Feld `text`, und die Engine ersetzt `{days}` nicht selbst.

8. **Kategorien statt loser Intervalle:** Jeder Spruch gehört zu einer `StreakTier`
   (Streak-Bereich je Stufe an einer einzigen Stelle definiert).

9. **Kein doppelter Spruch hintereinander:** Optionaler Parameter `previous`; das ViewModel merkt
   sich den zuletzt gezeigten Spruch.

---

## 3. Phase 1 – Anforderungen

### 3.1 Domain (`shared:core`)
Package `com.shub39.grit.core.motivation`, reines Kotlin ohne Compose/Room.

- `enum class QuoteTone { MOTIVATING, SARCASTIC }`
- `enum class StreakTier(val range: IntRange)` mit `STARTER (1..1)`, `BUILDING (2..4)`,
  `ESTABLISHED (5..13)`, `LEGEND (14..Int.MAX_VALUE)` und `fun forStreak(days: Int)`
  (`days < 1` → `STARTER`).
- `data class MotivationQuote(id: String, tone: QuoteTone, tier: StreakTier)` – **kein Text**.
- `object QuoteCatalog` (eigene Datei): Liste aller Sprüche (nur ID, Ton, Stufe) und ein fester
  Fallback-Spruch.
- `object GritQuoteEngine`
  - `getQuote(activeDays: Int, random: Random = Random.Default, previous: String? = null,
    quotes: List<MotivationQuote> = QuoteCatalog.all): MotivationQuote`
  - Wählt rein zufällig unter den Sprüchen der passenden `StreakTier` (keine Gewichtung der Töne).
  - `previous` (ID) wird ausgeschlossen, solange es Alternativen gibt.
  - Kein passender Spruch (z. B. leere Liste) → fester Fallback-Spruch aus dem Katalog.
  - Zustandslos; kennt weder Room noch Compose.
- Jede Stufe enthält im Katalog mindestens einen Spruch **pro Ton** (per Test abgesichert).

### 3.1a Texte (`shared:ui`, Compose-Resources)
- Eigene Dateien `composeResources/values/motivation_strings.xml` (Englisch) und
  `values-de/motivation_strings.xml` (Deutsch). Die `strings.xml` des Originals bleibt unberührt
  (dort ändern Übersetzungs-Tools laufend, eigene Einträge würden Merge-Konflikte erzeugen).
- Spruch-ID = Resource-Name (`quote_<stufe>_<ton>_<nr>`, `quote_fallback`). Die UI schlägt den Text
  über `Res.allStringResources[id]` nach, keine handgepflegte Zuordnung.
- 4 Sprüche pro Stufe und Ton (32) plus Fallback. Anrede „du“ / „you“.
- Sarkasmus leicht steigend: STARTER ~3, BUILDING/ESTABLISHED ~4, LEGEND ~5 von 10. Nie beschämend,
  keine Kraftausdrücke. Englisch frei übertragen, nicht wörtlich.
- Die Tage kommen immer als Formatargument `%1$d`; ein Spruch darf sie nutzen, muss aber nicht. Keine
  Plurals nötig: STARTER ist immer 1 Tag, alle anderen Stufen ≥ 2.
- Fallback ohne Zahl: „Abgehakt! Weiter so.“ / „Done! Keep it up.“
- Tests in `shared:ui` (`commonTest`): jede Katalog-ID hat einen String, jeder `quote_*`-String hat
  eine Katalog-ID, Deutsch und Englisch haben dieselben Schlüssel.

### 3.2 UI (`shared:ui`)
- `@Composable fun HabitMotivationPopup(habitTitle, activeDays, quote, onDismiss, modifier = Modifier)`
  in `UI/habit/ui/component/HabitMotivationPopup.kt`, zustandslos.
- Basis ist der projekteigene `GritDialog` (wie die übrigen Dialoge der App), nicht `AlertDialog`.
  `GritDialog` hat dafür einen optionalen Parameter `properties` bekommen (rückwärtskompatibel).
- Schließt per Button und Zurück-Taste, **nicht** durch Tippen daneben und nicht automatisch.
- Aufbau von oben nach unten, ohne Symbol:
  - Unterzeile je Ton: `SARCASTIC` → „Na sieh mal einer an…“ / „Well, look at that…“,
    `MOTIVATING` → „Starkes Ding!“ / „Nice one!“
  - Titel (fett, beliebig viele Zeilen): Plural-Resource „%1$s • %2$d Tag(e) aktiv“
  - Spruch: `stringResource(quote.textResource(), activeDays)`
  - Button: „Weiter geht’s“ / „Let’s go“
- Alle Texte in `motivation_strings.xml` (DE + EN). Der Übersetzungstest prüft Strings und Plurals.
- Zwei Previews (je Ton) mit `GritPreviewWrapper`; keine UI-Tests.

### 3.3 State & Integration
- `HabitState.motivationPopup: MotivationPopupState?` mit `habitTitle`, `activeDays`, `quote`.
- `HabitsAction.DismissMotivationPopup` setzt den State auf `null`.
- `HabitViewModel.insertHabitStatus(...)`: nur im **Insert-Zweig** (Check-In) nach dem Schreiben
  den neuen Streak lesen und `motivationPopup` setzen. Im Delete-Zweig (Un-Check) nichts.
- `HabitsList` rendert `HabitMotivationPopup`, wenn `state.motivationPopup != null`.

### 3.4 Definition of Done (Phase 1)
- [ ] `./gradlew :androidApp:assembleFossDebug` grün, ohne Billing-/Analytics-Keys (lokal + CI).
- [ ] Abhaken eines Habits öffnet sofort das M3-Pop-up.
- [ ] Pop-up zeigt korrekten Habit-Namen und denselben Streak wie die Habit-Karte.
- [ ] Abwählen eines gesetzten Häkchens zeigt **kein** Pop-up.
- [ ] Unit-Tests für `GritQuoteEngine` grün; `spotlessCheck` grün.

---

## 4. Roadmap (noch nicht implementieren)

| Phase | Inhalt | Tracking |
|---|---|---|
| 1 | Motivations-Pop-up | Meilenstein „Phase 1“, Issues #1–#9 |
| 2 | Energie & Streak-Freeze | Meilenstein „Phase 2“, Epic #14 |
| 3 | Statistiken | Meilenstein „Phase 3“, #10 |
| 4 | Avatar / Maskottchen | Meilenstein „Phase 4“, Epic #15 |

Reihenfolge und Abhängigkeiten:

- **Phase 2 vor Phase 3:** Ein Streak-Freeze ändert die Bedeutung von „Streak“.
  `countCurrentStreak` / `countBestStreak` (`shared/logic/.../repository/Util.kt`) müssen überbrückte
  Tage berücksichtigen. Alle Zahlen (Habit-Karte, Pop-up, Statistik) nutzen dieselbe Berechnung.
- **Phase 4 nach Phase 2:** Der Avatar baut auf der Energie-Währung und den aktiven Tagen auf.
  Klein anfangen (Stufen/Aussehen), Training und Anpassung später.
- Epics werden erst kurz vor Beginn der jeweiligen Phase per Design-Interview in Teilaufgaben
  zerlegt. Offene Regelfragen stehen in den Epic-Issues.

### Architektur-Regeln für alle Phasen

- `core.motivation` kennt weder Room noch DAOs. Neue SQL-Abfragen, Tabellen und State-Felder
  späterer Phasen dürfen die Motivations-Engine nicht berühren.
- Änderungen an Upstream-Dateien (z. B. Streak-Funktionen, Datenbank-Definition) möglichst klein
  halten, neue Logik in neue Dateien legen (siehe `docs/WORKFLOW.md`).
- Neue Room-Tabellen brauchen eine Migration, bestehende Nutzerdaten dürfen nicht verloren gehen.

## 5. Offene Fragen

- Soll das Pop-up per Einstellung abschaltbar sein? (Vorschlag: ja, als optionales Issue.)
- Beim Abhaken eines **vergangenen** Tages, der nicht an heute anschließt, bleibt der aktuelle
  Streak unverändert – Pop-up trotzdem zeigen? (Vorschlag: ja, mit dem aktuellen Streak.)
