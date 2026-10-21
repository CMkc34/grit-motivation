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

import com.shub39.grit.core.activity.ActivityReport
import com.shub39.grit.shared.ui.fakes.FakeActivityRepo
import com.shub39.grit.shared.ui.fakes.FakeAlarmScheduler
import com.shub39.grit.shared.ui.fakes.FakeAnalytics
import com.shub39.grit.shared.ui.fakes.FakeHabitRepo
import com.shub39.grit.shared.ui.fakes.FakeMotivationSettings
import com.shub39.grit.shared.ui.fakes.FakeSettingsDatastore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModelActivityTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val activityRepo = FakeActivityRepo()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state follows the activity report`() =
        runTest(dispatcher) {
            val viewModel =
                HabitViewModel(
                    FakeAlarmScheduler(),
                    FakeHabitRepo(emptyList()),
                    FakeSettingsDatastore(),
                    FakeAnalytics(),
                    FakeMotivationSettings(),
                    activityRepo,
                )
            backgroundScope.launch { viewModel.state.collect {} }

            val report =
                ActivityReport(
                    currentStreak = 12,
                    longestStreak = 20,
                    energy = 14,
                    days = emptyMap(),
                )
            activityRepo.report.value = report

            assertEquals(report, viewModel.state.value.activity)
        }
}
