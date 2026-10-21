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
package com.shub39.grit.shared.ui.fakes

import com.shub39.grit.core.AnalyticsEvent
import com.shub39.grit.core.habits.Habit
import com.shub39.grit.core.habits.HabitRepo
import com.shub39.grit.core.habits.HabitStatus
import com.shub39.grit.core.habits.HabitWithAnalytics
import com.shub39.grit.core.habits.MeasurableTargets
import com.shub39.grit.core.habits.OverallAnalytics
import com.shub39.grit.core.habits.Target
import com.shub39.grit.core.interfaces.AlarmScheduler
import com.shub39.grit.core.interfaces.AnalyticsWrapper
import com.shub39.grit.core.interfaces.SettingsDatastore
import com.shub39.grit.core.motivation.MotivationSettings
import com.shub39.grit.core.settings.Sections
import com.shub39.grit.core.tasks.SubTask
import com.shub39.grit.core.tasks.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * In-memory [HabitRepo]. The current streak is not calculated: tests set it per habit in
 * [currentStreaks], so they check how the caller uses the repository's value.
 */
class FakeHabitRepo(habits: List<Habit>) : HabitRepo {
    private val habits = MutableStateFlow(habits)
    private val statuses = MutableStateFlow(emptyList<HabitStatus>())

    val currentStreaks = mutableMapOf<Long, Int>()

    override suspend fun upsertHabit(habit: Habit): Long {
        habits.update { list -> list.filterNot { it.id == habit.id } + habit }
        return habit.id
    }

    override suspend fun deleteHabit(habitId: Long) {
        habits.update { list -> list.filterNot { it.id == habitId } }
    }

    override suspend fun getHabits(): List<Habit> = habits.value

    override suspend fun getHabitById(id: Long): Habit? = habits.value.find { it.id == id }

    override suspend fun getHabitStatuses(): List<HabitStatus> = statuses.value

    override fun getHabitsWithAnalytics(): Flow<List<HabitWithAnalytics>> =
        statuses.map { all ->
            habits.value.map { habit ->
                withAnalytics(habit, all.filter { it.habitId == habit.id })
            }
        }

    override fun getCompletedHabitIds(): Flow<List<Long>> = flowOf(emptyList())

    override fun getOverallAnalytics(): Flow<OverallAnalytics> = flowOf(OverallAnalytics())

    override fun getHabitsWithStatus(): Flow<List<Pair<Habit, Boolean>>> = flowOf(emptyList())

    override suspend fun getStatusForHabit(id: Long): List<HabitStatus> =
        statuses.value.filter { it.habitId == id }

    override suspend fun insertHabitStatus(habitStatus: HabitStatus) {
        statuses.update { it + habitStatus }
    }

    override suspend fun deleteHabitStatus(habitId: Long, date: LocalDate) {
        statuses.update { list -> list.filterNot { it.habitId == habitId && it.date == date } }
    }

    override suspend fun getCompletedHabitsForDate(date: LocalDate): Map<Habit, Double?> =
        emptyMap()

    private fun withAnalytics(habit: Habit, statuses: List<HabitStatus>): HabitWithAnalytics {
        val noTarget = Target(progress = 0.0, target = 0.0)
        return HabitWithAnalytics(
            habit = habit,
            consistency = 0f,
            statuses = statuses,
            weeklyComparisonData = emptyList(),
            weekDayFrequencyData = emptyMap(),
            measurableTargets = MeasurableTargets(noTarget, noTarget, noTarget, noTarget, noTarget),
            currentStreak = currentStreaks[habit.id] ?: 0,
            bestStreak = 0,
            startedDaysAgo = 0,
        )
    }
}

class FakeSettingsDatastore : SettingsDatastore {
    override fun getStartOfTheWeekPref(): Flow<DayOfWeek> = flowOf(DayOfWeek.MONDAY)

    override suspend fun setStartOfWeek(day: DayOfWeek) {}

    override fun getStartingSectionPref(): Flow<Sections> = flowOf(Sections.Habits)

    override suspend fun setStartingPage(page: Sections) {}

    override fun getIs24Hr(): Flow<Boolean> = flowOf(true)

    override suspend fun setIs24Hr(pref: Boolean) {}

    override fun getNotificationsFlow(): Flow<Boolean> = flowOf(false)

    override suspend fun setNotifications(pref: Boolean) {}

    override fun getBiometricLockPref(): Flow<Boolean> = flowOf(false)

    override suspend fun setBiometricPref(pref: Boolean) {}

    override fun getTaskReorderPref(): Flow<Boolean> = flowOf(false)

    override suspend fun setTaskReorderPref(pref: Boolean) {}

    override fun getCompactViewPref(): Flow<Boolean> = flowOf(false)

    override suspend fun setCompactView(pref: Boolean) {}
}

class FakeMotivationSettings : MotivationSettings {
    val popupEnabled = MutableStateFlow(true)

    override fun getPopupEnabled(): Flow<Boolean> = popupEnabled

    override suspend fun setPopupEnabled(enabled: Boolean) {
        popupEnabled.value = enabled
    }
}

class FakeAlarmScheduler : AlarmScheduler {
    override fun schedule(habit: Habit) {}

    override fun schedule(task: Task) {}

    override fun schedule(subTask: SubTask) {}

    override fun cancel(habit: Habit) {}

    override fun cancel(task: Task) {}

    override fun cancel(subTask: SubTask) {}

    override fun cancelAll() {}
}

class FakeAnalytics : AnalyticsWrapper {
    override fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any>) {}
}
