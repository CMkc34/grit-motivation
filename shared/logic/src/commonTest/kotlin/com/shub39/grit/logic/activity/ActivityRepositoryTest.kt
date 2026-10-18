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
package com.shub39.grit.logic.activity

import com.shub39.grit.core.activity.ActivityData
import com.shub39.grit.core.activity.ActivityStore
import com.shub39.grit.core.now
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

private class FakeActivityStore(initial: ActivityData = ActivityData()) : ActivityStore {
    val data = MutableStateFlow(initial)

    override fun getActivity(): Flow<ActivityData> = data

    override suspend fun ensureStartDate(day: LocalDate) {
        data.update { if (it.startDate == null) it.copy(startDate = day) else it }
    }

    override suspend fun markActive(day: LocalDate) {
        data.update { it.copy(activeDays = it.activeDays + day) }
    }

    override suspend fun markFreezesSeen(days: Set<LocalDate>) {
        data.update { it.copy(seenFreezes = it.seenFreezes + days) }
    }
}

class ActivityRepositoryTest {

    // relative to the real date, like the repository's "today"
    private val today = LocalDate.now()

    private fun daysAgo(n: Int) = today.minus(n, DateTimeUnit.DAY)

    @Test
    fun `report evaluates the stored active days`() = runTest {
        val store =
            FakeActivityStore(
                ActivityData(
                    startDate = daysAgo(2),
                    activeDays = setOf(daysAgo(2), daysAgo(1), today),
                )
            )

        val report = ActivityRepository(store).getActivityReport().first()

        assertEquals(3, report.currentStreak)
        assertEquals(3, report.energy)
    }

    @Test
    fun `reading the report sets the start date on first use`() = runTest {
        val store = FakeActivityStore()

        ActivityRepository(store).getActivityReport().first()

        assertEquals(today, store.data.value.startDate)
    }
}
