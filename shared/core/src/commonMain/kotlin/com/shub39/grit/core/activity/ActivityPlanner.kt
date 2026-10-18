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
package com.shub39.grit.core.activity

import kotlinx.datetime.LocalDate

enum class DayStatus {
    /** At least one check-in was made on this day. */
    ACTIVE,
    /** Missed, but bridged by a freeze. */
    FROZEN,
    MISSED,
    /** Today without a check-in yet. */
    OPEN,
}

/** Result of replaying the user's active days. */
data class ActivityReport(
    val currentStreak: Int,
    val longestStreak: Int,
    val energy: Int,
    /** Status of every day from the start date up to today, in order. */
    val days: Map<LocalDate, DayStatus>,
) {
    val frozenDays: Set<LocalDate>
        get() = days.filterValues { it == DayStatus.FROZEN }.keys
}

/** Derives the activity streak, energy and freezes from the days the user used the app. */
object ActivityPlanner {
    const val ENERGY_PER_ACTIVE_DAY = 1
    const val FREEZE_COST = 7
    const val MAX_FROZEN_DAYS_IN_A_ROW = 2

    /**
     * Replays every day from [startDate] to [today]. A gap of missed days is frozen only as a
     * whole: when it is at most [MAX_FROZEN_DAYS_IN_A_ROW] long and the energy covers all of it.
     * Otherwise the streak breaks and no energy is spent. [today] without activity does not break
     * the streak.
     */
    fun plan(activeDays: Set<LocalDate>, startDate: LocalDate, today: LocalDate): ActivityReport {
        var streak = 0
        var longest = 0
        var energy = 0
        val days = linkedMapOf<LocalDate, DayStatus>()
        val gap = mutableListOf<LocalDate>()

        fun closeGap() {
            if (gap.isEmpty()) return
            val cost = gap.size * FREEZE_COST
            if (streak > 0 && gap.size <= MAX_FROZEN_DAYS_IN_A_ROW && energy >= cost) {
                gap.forEach { days[it] = DayStatus.FROZEN }
                energy -= cost
            } else {
                streak = 0
            }
            gap.clear()
        }

        for (epochDay in startDate.toEpochDays()..today.toEpochDays()) {
            val day = LocalDate.fromEpochDays(epochDay)
            when {
                day in activeDays -> {
                    closeGap()
                    streak++
                    longest = maxOf(longest, streak)
                    energy += ENERGY_PER_ACTIVE_DAY
                    days[day] = DayStatus.ACTIVE
                }
                day < today -> {
                    gap += day
                    days[day] = DayStatus.MISSED
                }
                else -> days[day] = DayStatus.OPEN
            }
        }
        closeGap()

        return ActivityReport(
            currentStreak = streak,
            longestStreak = longest,
            energy = energy,
            days = days,
        )
    }
}
