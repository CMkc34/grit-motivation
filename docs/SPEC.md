# Spezifikation: grit-motivation (privater Fork von Grit)

Privater Fork von [shub39/Grit](https://github.com/shub39/Grit) (GPL-3.0, Kotlin Multiplatform,
Compose Material 3, Room). Ziel: kontextsensitives Motivations-/Sarkasmus-Pop-up beim Abhaken von
Habits (Phase 1); danach Energie & Aktiv-Serie (Phase 2, Abschnitt 4), Statistiken und ein Avatar (Abschnitt 5).

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
Umgesetzt in einem PR für #5, #6 und #7.

- `MotivationPopupState(habitTitle, activeDays, quote)` (`@Immutable`) in
  `UI/habit/motivation/MotivationPopupState.kt`; `HabitState.motivationPopup: MotivationPopupState?`.
- `HabitsAction.DismissMotivationPopup` setzt den State auf `null`.
- `HabitViewModel.insertHabitStatus(...)`:
  - Pop-up nur beim **Abhaken** (für das Datum existierte vorher kein Status). Abwählen zeigt keins.
  - Messbare Habits: nur der **erste** Wert eines Tages (`value > 0`) zählt als Check-In;
    Korrigieren oder auf 0 setzen zeigt keins.
  - Streak = `currentStreak` aus `repo.getHabitsWithAnalytics().first()` (dieselbe Zahl wie auf der
    Karte, nicht `+ 1`). Bei Streak 0 (Nachtragen eines alten Tages ohne laufende Serie) kein Pop-up.
  - Das ViewModel merkt sich die ID des letzten Spruchs (nur im Speicher) und übergibt sie als
    `previous`.
  - Nur Check-Ins für **heute oder gestern** zeigen ein Pop-up; Nachträge älterer Tage bleiben still
    (#8).
- Ein offenes Pop-up überlebt das Beenden der App im Hintergrund nicht (bewusst hingenommen).
- Tests: `shared/ui/src/commonTest/.../viewmodel/HabitViewModelMotivationTest.kt` über `onAction` → `state`,
  mit Fakes in `shared/ui/src/commonTest/.../fakes/`. Der Streak wird im Fake vorgegeben.
- Anzeige (#8): `MotivationPopupHost` (`UI/habit/motivation/`) ist **einmal zentral** in
  `GritNavDisplay` eingebunden, damit das Pop-up beim Abhaken in der Liste, auf der Detailseite und
  im Kalender sofort erscheint (alle Habit-Screens teilen sich dieselbe `HabitViewModel`-Instanz).
  Der Host beobachtet den eigenen Flow `HabitViewModel.motivationPopup`, nicht `state`, damit die
  Habit-Beobachter nicht auch auf dem Aufgaben-Tab laufen.
- Abhaken über Widget oder Benachrichtigung zeigt kein Pop-up (läuft am ViewModel vorbei).
- Abschaltbar (#9): Schalter „Motivations-Pop-up“ auf der Einstellungsseite, Standard **an**.
  Schnittstelle `MotivationSettings` in `core.motivation`, `MotivationSettingsImpl`
  (`logic/motivation/`) nutzt die bestehende Preferences-Datei mit dem Schlüssel
  `motivation_popup_enabled`. `HabitViewModel` liest den Wert beim Abhaken; ist er aus, wird
  `motivationPopup` nicht gesetzt.

### 3.4 Definition of Done (Phase 1)
Abgenommen am 2026-10-18 auf einem Pixel 8a (Android 17), Build aus #8.

- [x] `./gradlew :androidApp:assembleFossDebug` grün, ohne Billing-/Analytics-Keys (lokal + CI).
- [x] Abhaken eines Habits öffnet sofort das Pop-up.
- [x] Pop-up zeigt korrekten Habit-Namen und denselben Streak wie die Habit-Karte.
- [x] Abwählen eines gesetzten Häkchens zeigt **kein** Pop-up.
- [x] Nachtrag eines älteren Tages zeigt kein Pop-up; gestern abhaken zeigt eins.
- [x] Zurück und Button schließen das Pop-up, Tippen daneben nicht.
- [x] Unit-Tests (`shared:core`, `shared:ui`) grün; `spotlessCheck` grün.

---

## 4. Phase 2 – Energie & Aktiv-Serie

Design-Interview abgeschlossen am 2026-10-18. Epic #14, Meilenstein „Phase 2 – Energie & Aktiv-Serie“.

### 4.1 Grundidee
Belohnt wird die **Nutzung der App**, nicht die Serie einzelner Routinen.

- Ein Tag ist **aktiv**, wenn an diesem Tag mindestens ein Eintrag gemacht wurde – egal für welche
  Routine und für welches Datum (ein Nachtrag für gestern zählt für heute), auch über Widget oder
  Erinnerung. Wird das Häkchen wieder entfernt, bleibt der Tag aktiv.
- Die globale **Aktiv-Serie** zählt aktive Tage in Folge.
- Die Serien der einzelnen Routinen bleiben **unverändert** und ohne Freeze. Streak-Berechnung im
  Repository, Widgets und Kalender des Originals werden nicht angefasst.
- Das System gilt **ab Installation** der neuen Version (Startdatum); bisherige Zahlen bleiben.

### 4.2 Energie und Freezes
- **+1 ⚡** pro aktivem Tag, **keine Obergrenze** (Energie wird in Phase 4 für den Avatar ausgegeben).
- **Freeze = 7 ⚡**, überbrückt einen verpassten Tag der Aktiv-Serie, **max. 2 Tage am Stück**.
- Freezes werden **automatisch** eingesetzt, sobald genug Energie da ist (Abschalten: Backlog #30).
- Heute noch kein Eintrag → die Serie gilt bis Tagesende als nicht gerissen.
- Ein gefrorener Tag **hält** die Serie, **zählt aber nicht mit** (7 aktiv + 1 gefroren + 1 aktiv = 8).
- Eine Lücke wird nur **als Ganzes** gefroren: höchstens 2 Tage und genug Energie für alle Tage der
  Lücke. Sonst reißt die Serie, und es wird **keine** Energie verbraucht. Weil alles berechnet wird,
  gilt das auch rückwirkend: Wird aus einer 2-Tage-Lücke eine 3-Tage-Lücke, sind die beiden
  Freezes wieder frei.
- Reißt die Serie, **bleibt die Energie** erhalten.

### 4.3 Datenhaltung
- **Alles berechnet:** `ActivityPlanner` (reine Funktion in `shared:core`) spielt die aktiven Tage ab
  dem Startdatum chronologisch ab und liefert Aktiv-Serie, längste Aktiv-Serie, Kontostand,
  gefrorene Tage und den Tagesstatus der letzten Tage (#23).
- **Gespeichert** wird nur: Startdatum, Liste der aktiven Tage, bereits gesehene Freezes – in einem
  **eigenen DataStore** des Forks, nicht in der Datenbank des Originals (keine Kollision mit dessen
  Schema-Versionen) (#24).
- Ein Häkchen speichert nur, *für* welchen Tag es gilt, nicht *wann* es gesetzt wurde. Deshalb
  vermerkt das Repository beim Speichern eines Häkchens den **heutigen** Tag als aktiv
  (`HabitRepository.insertHabitStatus`, kleine Änderung im Original) (#24).
- Die drei gespeicherten Werte kommen ins Backup (#28).
- Umsetzung (#24): Schnittstellen `ActivityStore` und `ActivityRepo` in `core.activity`;
  `ActivityStoreImpl` nutzt die **bestehende** Preferences-Datei der App mit den Schlüsseln
  `activity_start_date`, `activity_days`, `activity_seen_freezes`. `ActivityRepository` liefert den
  Bericht als Flow.
- Startdatum: wird beim ersten Abruf des Berichts gesetzt oder – falls zuerst über Widget/Erinnerung
  eingetragen wird – beim ersten aktiven Tag.
- Der Bericht wird neu berechnet, wenn sich die gespeicherten Tage ändern oder die App ihn neu
  abruft. Über Mitternacht offen gelassen zeigt die App bis dahin den Stand von gestern (bewusst
  hingenommen).

### 4.4 UI
- **Kopfzeile der Routinen-Seite:** „🔥 12 Tage aktiv · ⚡ 14“; Antippen öffnet die Energie-Seite (#25).
- **Energie-Seite:** Kontostand, aktuelle und längste Aktiv-Serie, Leiste der letzten 30 Tage
  (✓ aktiv / ❄️ gefroren / – verpasst), kurze Erklärung der Regeln. Kein Shop-Platzhalter, aber so
  aufgebaut, dass in Phase 4 ein Shop darunter passt. Navigation des Originals bleibt unverändert (#25).
- **Freeze-Hinweis beim Öffnen**, nur wenn ein Freeze die Serie gerettet hat; kein Hinweis, wenn die
  Serie gerissen ist (#26).
- **Motivations-Pop-up:** beim ersten Eintrag eines Tages zusätzlich „+1 ⚡ · 13 Tage aktiv“ (#27).

### 4.5 Issues
| # | Inhalt |
|---|---|
| #23 | `ActivityPlanner` (Domain, test-first) |
| #24 | DataStore + Erfassung aktiver Tage |
| #25 | Kopfzeile + Energie-Seite |
| #26 | Freeze-Hinweis beim Öffnen |
| #27 | „+1 ⚡“ im Motivations-Pop-up |
| #28 | Backup |
| #29 | Abnahme auf dem Gerät |
| #30 | Backlog: Freezes abschaltbar (Phase 4) |

---

## 5. Roadmap

| Phase | Inhalt | Tracking |
|---|---|---|
| 1 | Motivations-Pop-up | abgeschlossen (Meilenstein „Phase 1“, #1–#9) |
| 2 | Energie & Aktiv-Serie | Meilenstein „Phase 2“, Epic #14, Abschnitt 4 |
| 3 | Statistiken | Meilenstein „Phase 3“, #10 |
| 4 | Avatar / Maskottchen | Meilenstein „Phase 4“, Epic #15 |

Reihenfolge und Abhängigkeiten:

- **Phase 4 nach Phase 2:** Der Avatar baut auf der Energie und den aktiven Tagen auf; Energie wird
  dort für Gegenstände ausgegeben (gespeicherte Ausgaben, Kontostand bleibt berechnet).
  Klein anfangen (Stufen/Aussehen), Training und Anpassung später.
- Phase 3 ist von Phase 2 unabhängig, da die Serien der einzelnen Routinen unverändert bleiben.
- Epics werden erst kurz vor Beginn der jeweiligen Phase per Design-Interview in Teilaufgaben
  zerlegt.

### Architektur-Regeln für alle Phasen

- `core.motivation` kennt weder Room noch DAOs. Neue SQL-Abfragen, Tabellen und State-Felder
  späterer Phasen dürfen die Motivations-Engine nicht berühren.
- Änderungen an Upstream-Dateien (z. B. Streak-Funktionen, Datenbank-Definition) möglichst klein
  halten, neue Logik in neue Dateien legen (siehe `docs/WORKFLOW.md`).
- Neue Room-Tabellen brauchen eine Migration, bestehende Nutzerdaten dürfen nicht verloren gehen.

## 6. Offene Fragen

- ~~Soll das Pop-up per Einstellung abschaltbar sein?~~ Umgesetzt in #9 (siehe 3.3).
- ~~Pop-up beim Abhaken eines vergangenen Tages ohne laufende Serie?~~ Entschieden: kein Pop-up bei
  Streak 0 (siehe 3.3).
