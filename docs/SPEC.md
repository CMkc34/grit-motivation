# Spezifikation: grit-motivation (privater Fork von Grit)

Privater Fork von [shub39/Grit](https://github.com/shub39/Grit) (GPL-3.0, Kotlin Multiplatform,
Compose Material 3, Room). Ziel: kontextsensitives Motivations-/Sarkasmus-Pop-up beim Abhaken von
Habits (Phase 1), später erweiterte Statistiken (Phase 2).

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

5. **Zufall injizierbar:** `GritQuoteEngine.getQuote(activeDays, random: Random = Random.Default)`
   für deterministische Tests.

6. **Grammatik:** „1 Tag aktiv“ / „n Tage aktiv“ statt „Tag(e)“.

---

## 3. Phase 1 – Anforderungen

### 3.1 Domain (`shared:core`)
- `enum class QuoteTone { MOTIVATING, SARCASTIC }`
- `data class MotivationQuote(text: String, tone: QuoteTone, minStreak: Int, maxStreak: Int)`
  – `text` enthält Platzhalter `{days}`.
- `object GritQuoteEngine`
  - Deutscher Spruchkatalog, gemischt motivierend/sarkastisch, Intervalle
    `1..1`, `2..4`, `5..13`, `14..Int.MAX_VALUE`.
  - `getQuote(activeDays: Int, random: Random = Random.Default): MotivationQuote`
    filtert nach Intervall, wählt zufällig, ersetzt `{days}`.
  - `activeDays < 1` wird wie `1` behandelt (kein Crash).
  - Jedes Intervall enthält mindestens einen Spruch **pro Ton**.

### 3.2 UI (`shared:ui`)
- `@Composable fun HabitMotivationPopup(habitTitle, activeDays, quote, onDismiss, modifier = Modifier)`
  als Material-3-`AlertDialog`:
  - Sub-Header je Ton: `SARCASTIC` → „Na sieh mal einer an…“, `MOTIVATING` → „Starkes Ding!“
  - Titel (fett): `"$habitTitle • $activeDays Tag(e) aktiv"` (mit korrektem Singular/Plural)
  - Body: aufbereiteter `quote.text`
  - Confirm-Button → `onDismiss()`
- Texte über Compose-Resources (`composeResources/values-de/strings.xml` bzw. bestehende Struktur),
  nicht hart im Composable.

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

## 4. Phase 2 – Vorbereitung (noch nicht implementieren)

- Ausbau von Statistiken/Auswertungen im Analytics-Bereich
  (`UI/habit/ui/sections/AnalyticsPage.kt`, `OverallAnalytics.kt`, `shared/logic/.../database/*Dao.kt`).
- Architektur-Regel: `core.motivation` kennt weder Room noch DAOs. Neue SQL-Abfragen und
  State-Felder in Phase 2 dürfen die Motivations-Engine nicht berühren.

## 5. Offene Fragen

- Soll das Pop-up per Einstellung abschaltbar sein? (Vorschlag: ja, als optionales Issue.)
- Beim Abhaken eines **vergangenen** Tages, der nicht an heute anschließt, bleibt der aktuelle
  Streak unverändert – Pop-up trotzdem zeigen? (Vorschlag: ja, mit dem aktuellen Streak.)
