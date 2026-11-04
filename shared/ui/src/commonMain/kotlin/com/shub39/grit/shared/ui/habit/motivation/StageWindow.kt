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
package com.shub39.grit.shared.ui.habit.motivation

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

/** Dialog properties for [CelebrationStage]: full width, and on Android behind the system bars. */
internal expect fun stageDialogProperties(): DialogProperties

/**
 * Called inside the stage's dialog: on Android it lets the dialog window reach behind the status
 * and navigation bars with transparent bars and light icons, so the stage fills the whole screen
 * (#153). Does nothing on desktop.
 */
@Composable internal expect fun StageWindowSetup()
