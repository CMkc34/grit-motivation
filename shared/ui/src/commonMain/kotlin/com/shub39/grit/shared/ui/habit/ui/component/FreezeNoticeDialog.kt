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
package com.shub39.grit.shared.ui.habit.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.window.DialogProperties
import com.shub39.grit.core.activity.FreezeNotice
import com.shub39.grit.shared.ui.GritPreviewWrapper
import com.shub39.grit.shared.ui.components.GritDialog
import com.shub39.grit.shared.ui.theme.flexFontEmphasis
import grit.shared.ui.generated.resources.Res
import grit.shared.ui.generated.resources.freeze_notice_button
import grit.shared.ui.generated.resources.freeze_notice_days
import grit.shared.ui.generated.resources.freeze_notice_text
import grit.shared.ui.generated.resources.freeze_notice_title
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Tells the user that freezes saved the activity streak. Like the motivation popup it closes via
 * the button or the back key only, so the notice is not lost by accident.
 */
@Composable
fun FreezeNoticeDialog(notice: FreezeNotice, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    GritDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        Text(
            text = stringResource(Res.string.freeze_notice_title),
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = flexFontEmphasis(),
                    fontWeight = FontWeight.Bold,
                ),
            textAlign = TextAlign.Center,
        )
        Text(
            text =
                pluralStringResource(
                    Res.plurals.freeze_notice_text,
                    notice.streak,
                    notice.streak,
                    notice.energySpent,
                    notice.energyLeft,
                ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text =
                pluralStringResource(
                    Res.plurals.freeze_notice_days,
                    notice.frozenDays.size,
                    notice.frozenDays.size,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(Res.string.freeze_notice_button))
        }
    }
}

@PreviewWrapper(GritPreviewWrapper::class)
@Preview
@Composable
private fun OneFreezePreview() {
    FreezeNoticeDialog(
        notice =
            FreezeNotice(streak = 12, frozenDays = setOf(LocalDate(2026, 10, 20)), energyLeft = 7),
        onDismiss = {},
    )
}

@PreviewWrapper(GritPreviewWrapper::class)
@Preview
@Composable
private fun TwoFreezesPreview() {
    FreezeNoticeDialog(
        notice =
            FreezeNotice(
                streak = 30,
                frozenDays = setOf(LocalDate(2026, 10, 19), LocalDate(2026, 10, 20)),
                energyLeft = 3,
            ),
        onDismiss = {},
    )
}
