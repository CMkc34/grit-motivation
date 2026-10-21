/*
 * Copyright (C) 2026  Shubham Gorai
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.shub39.grit.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shub39.grit.core.AnalyticsEvent
import com.shub39.grit.core.activity.ActivityRepo
import com.shub39.grit.core.activity.DayStatus
import com.shub39.grit.core.activity.FreezeNotice
import com.shub39.grit.core.habits.Habit
import com.shub39.grit.core.habits.HabitRepo
import com.shub39.grit.core.habits.HabitStatus
import com.shub39.grit.core.interfaces.AlarmScheduler
import com.shub39.grit.core.interfaces.AnalyticsWrapper
import com.shub39.grit.core.interfaces.SettingsDatastore
import com.shub39.grit.core.motivation.GritQuoteEngine
import com.shub39.grit.core.motivation.MotivationSettings
import com.shub39.grit.core.now
import com.shub39.grit.shared.ui.habit.HabitState
import com.shub39.grit.shared.ui.habit.HabitsAction
import com.shub39.grit.shared.ui.habit.motivation.MotivationPopupState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
class HabitViewModel(
    @Provided private val scheduler: AlarmScheduler,
    @Provided private val repo: HabitRepo,
    @Provided private val datastore: SettingsDatastore,
    @Provided private val analytics: AnalyticsWrapper,
    @Provided private val motivationSettings: MotivationSettings,
    @Provided private val activityRepo: ActivityRepo,
) : ViewModel() {
    private var habitStatusJob: Job? = null
    private var overallAnalyticsJob: Job? = null
    private var observeDatastoreJob: Job? = null
    private var completedHabitsFetchJob: Job? = null
    private var activityJob: Job? = null

    // id of the last motivation quote, so the next popup shows a different one
    private var lastQuoteId: String? = null

    private val _state = MutableStateFlow(HabitState())

    val state =
        _state
            .asStateFlow()
            .onStart {
                observeDataStore()
                observeHabitStatuses()
                observeOverallAnalytics()
                observeActivity()

                rescheduleAllHabits()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitState())

    /**
     * Popup shown after a check-in. Separate from [state] so the app-wide popup host can observe it
     * without starting the habit observers above.
     */
    val motivationPopup: StateFlow<MotivationPopupState?> =
        _state
            .map { it.motivationPopup }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Freezes that saved the activity streak, shown app-wide when the app opens. Recalculated when
     * the app returns after a while in the background, so a freeze from the last days shows up
     * then.
     */
    val freezeNotice: StateFlow<FreezeNotice?> =
        activityRepo
            .getFreezeNotice()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // handles actions from habit page
    fun onAction(action: HabitsAction) {
        viewModelScope.launch {
            when (action) {
                is AddHabit -> {
                    analytics.trackEvent(
                        AnalyticsEvent.HABIT_CREATED,
                        mapOf(
                            "has_reminder" to action.habit.reminder,
                            "is_measurable" to action.habit.isMeasurable,
                        ),
                    )
                    upsertHabit(action.habit)
                }

                is DeleteHabit -> {
                    analytics.trackEvent(
                        AnalyticsEvent.HABIT_DELETED,
                        mapOf("has_reminder" to action.habit.reminder),
                    )
                    deleteHabit(action.habit)
                }

                is InsertStatus -> insertHabitStatus(action.habit, action.date)

                is UpdateHabit -> {
                    analytics.trackEvent(
                        AnalyticsEvent.HABIT_EDITED,
                        mapOf("has_reminder" to action.habit.reminder),
                    )
                    upsertHabit(action.habit)
                }

                ReorderHabits -> {
                    val currentList =
                        _state.value.habitsWithAnalytics.mapIndexed { index, analytics ->
                            analytics.habit.copy(index = index)
                        }

                    currentList.forEach { upsertHabit(it) }
                }

                is PrepareAnalytics -> {
                    if (action.habit != null) {
                        analytics.trackEvent(
                            AnalyticsEvent.HABIT_ANALYTICS_VIEWED,
                            mapOf("has_reminder" to action.habit.reminder),
                        )
                    }
                    _state.update { it.copy(analyticsHabitId = action.habit?.id) }
                }

                OnAddHabitClicked -> {
                    analytics.trackEvent(AnalyticsEvent.HABIT_SHEET_OPENED, emptyMap())
                    _state.update { it.copy(showHabitAddSheet = true) }
                }

                DismissAddHabitDialog -> {
                    analytics.trackEvent(AnalyticsEvent.HABIT_SHEET_DISMISSED, emptyMap())
                    _state.update { it.copy(showHabitAddSheet = false) }
                }

                is OnToggleCompactView -> datastore.setCompactView(action.pref)

                is OnToggleEditState -> _state.update { it.copy(editState = action.pref) }

                is OnTransientHabitReorder -> {
                    val currentList = _state.value.habitsWithAnalytics.toMutableList()
                    currentList.add(action.to, currentList.removeAt(action.from))
                    _state.update { it.copy(habitsWithAnalytics = currentList) }
                }

                is FetchCompletedHabitsForDate -> {
                    completedHabitsFetchJob?.cancel()
                    completedHabitsFetchJob = launch {
                        if (action.date == null) {
                            _state.update {
                                it.copy(
                                    overallAnalytics =
                                        it.overallAnalytics.copy(completedHabits = null)
                                )
                            }
                            return@launch
                        }

                        val completedHabits = repo.getCompletedHabitsForDate(action.date)

                        _state.update { habitState ->
                            habitState.copy(
                                overallAnalytics =
                                    habitState.overallAnalytics.copy(
                                        completedHabits = action.date to completedHabits
                                    )
                            )
                        }
                    }
                }

                OnHabitsOpened -> {
                    analytics.trackEvent(AnalyticsEvent.HABITS_OPENED, emptyMap())
                }

                OnOverallAnalyticsViewed -> {
                    analytics.trackEvent(AnalyticsEvent.OVERALL_ANALYTICS_VIEWED, emptyMap())
                }

                HabitsAction.DismissMotivationPopup -> {
                    _state.update { it.copy(motivationPopup = null) }
                }

                HabitsAction.DismissFreezeNotice -> {
                    freezeNotice.value?.let { activityRepo.markFreezesSeen(it.frozenDays) }
                }

                is HabitsAction.InsertMeasuredStatus -> {
                    insertHabitStatus(action.habit, action.date, action.value)
                }
            }
        }
    }

    private fun observeHabitStatuses() {
        habitStatusJob?.cancel()
        habitStatusJob =
            viewModelScope.launch {
                combine(repo.getHabitsWithAnalytics(), repo.getCompletedHabitIds()) {
                        habits,
                        completedHabits ->
                        _state.update {
                            it.copy(
                                habitsWithAnalytics = habits,
                                completedHabitIds = completedHabits,
                            )
                        }
                    }
                    .launchIn(this)
            }
    }

    private fun observeOverallAnalytics() {
        overallAnalyticsJob?.cancel()
        overallAnalyticsJob =
            repo
                .getOverallAnalytics()
                .onEach { overallAnalytics ->
                    _state.update { it.copy(overallAnalytics = overallAnalytics) }
                }
                .launchIn(viewModelScope)
    }

    private fun observeActivity() {
        activityJob?.cancel()
        activityJob =
            activityRepo
                .getActivityReport()
                .onEach { report -> _state.update { it.copy(activity = report) } }
                .launchIn(viewModelScope)
    }

    private fun observeDataStore() {
        observeDatastoreJob?.cancel()
        observeDatastoreJob =
            viewModelScope.launch {
                datastore
                    .getCompactViewPref()
                    .onEach { pref -> _state.update { it.copy(compactHabitView = pref) } }
                    .launchIn(this)

                datastore
                    .getStartOfTheWeekPref()
                    .onEach { pref -> _state.update { it.copy(startingDay = pref) } }
                    .launchIn(this)

                datastore
                    .getIs24Hr()
                    .onEach { pref -> _state.update { it.copy(is24Hr = pref) } }
                    .launchIn(this)
            }
    }

    private suspend fun rescheduleAllHabits() {
        repo.getHabits().forEach { habit -> scheduler.schedule(habit) }
    }

    private suspend fun upsertHabit(habit: Habit) {
        val newId = repo.upsertHabit(habit)
        scheduler.schedule(habit.copy(id = newId))
    }

    private suspend fun deleteHabit(habit: Habit) {
        repo.deleteHabit(habit.id)
        scheduler.cancel(habit)
    }

    private suspend fun insertHabitStatus(habit: Habit, date: LocalDate) {
        val isHabitCompleted =
            _state.value.habitsWithAnalytics
                .find { it.habit == habit }
                ?.statuses
                ?.any { it.date == date } ?: false

        if (isHabitCompleted) {
            repo.deleteHabitStatus(habit.id, date)
        } else {
            analytics.trackEvent(AnalyticsEvent.HABIT_COMPLETED, emptyMap())
            val firstOfDay = !isTodayActive()
            repo.insertHabitStatus(HabitStatus(habitId = habit.id, date = date))
            showMotivationPopup(habit, date, firstOfDay)
        }
    }

    private suspend fun insertHabitStatus(habit: Habit, date: LocalDate, value: Double) {
        val isStatusAdded =
            _state.value.habitsWithAnalytics
                .find { it.habit == habit }
                ?.statuses
                ?.any { it.date == date } ?: false

        if (isStatusAdded) repo.deleteHabitStatus(habit.id, date)
        if (value <= 0.0) {
            repo.deleteHabitStatus(habit.id, date)
            return
        }
        analytics.trackEvent(AnalyticsEvent.HABIT_COMPLETED, emptyMap())
        val firstOfDay = !isTodayActive()
        repo.insertHabitStatus(HabitStatus(habitId = habit.id, date = date, value = value))
        // only the first value of a day is a check-in, corrections stay silent
        if (!isStatusAdded) showMotivationPopup(habit, date, firstOfDay)
    }

    /** Whether today already has a check-in, read before saving the next one. */
    private suspend fun isTodayActive(): Boolean =
        activityRepo.getActivityReport().first().days[LocalDate.now()] == DayStatus.ACTIVE

    /**
     * Reads the streak after a check-in from the repository, the same number the habit card shows.
     * Only check-ins for today or yesterday are celebrated; catching up on older days stays silent,
     * as does a check-in that does not extend an active streak (streak 0), and every check-in while
     * the popup is turned off in the settings. The first check-in of the day ([firstOfDay]) also
     * shows the activity streak that it extended, as it earns the day's energy.
     */
    private suspend fun showMotivationPopup(habit: Habit, date: LocalDate, firstOfDay: Boolean) {
        if (!motivationSettings.getPopupEnabled().first()) return
        if (date < LocalDate.now().minus(1, DateTimeUnit.DAY)) return
        val streak =
            repo.getHabitsWithAnalytics().first().find { it.habit.id == habit.id }?.currentStreak
        if (streak == null || streak < 1) return
        val quote = GritQuoteEngine.getQuote(streak, previous = lastQuoteId)
        lastQuoteId = quote.id
        val activityStreak =
            if (firstOfDay) activityRepo.getActivityReport().first().currentStreak else null
        _state.update {
            it.copy(
                motivationPopup = MotivationPopupState(habit.title, streak, quote, activityStreak)
            )
        }
    }
}
