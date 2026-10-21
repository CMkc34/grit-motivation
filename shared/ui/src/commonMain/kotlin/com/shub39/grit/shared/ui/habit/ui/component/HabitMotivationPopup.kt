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
import com.shub39.grit.core.motivation.MotivationQuote
import com.shub39.grit.core.motivation.QuoteTone
import com.shub39.grit.core.motivation.StreakTier
import com.shub39.grit.shared.ui.GritPreviewWrapper
import com.shub39.grit.shared.ui.components.GritDialog
import com.shub39.grit.shared.ui.habit.motivation.textResource
import com.shub39.grit.shared.ui.theme.flexFontEmphasis
import grit.shared.ui.generated.resources.Res
import grit.shared.ui.generated.resources.energy_popup_line
import grit.shared.ui.generated.resources.motivation_button
import grit.shared.ui.generated.resources.motivation_header_motivating
import grit.shared.ui.generated.resources.motivation_header_sarcastic
import grit.shared.ui.generated.resources.motivation_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Shown after a habit check-in. Closes via the button or the back key, not by tapping outside, so
 * the quote is not lost by accident. [activityStreak] adds the "+1 ⚡" line for the first check-in
 * of the day.
 */
@Composable
fun HabitMotivationPopup(
    habitTitle: String,
    activeDays: Int,
    quote: MotivationQuote,
    activityStreak: Int?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GritDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        Text(
            text =
                stringResource(
                    when (quote.tone) {
                        QuoteTone.MOTIVATING -> Res.string.motivation_header_motivating
                        QuoteTone.SARCASTIC -> Res.string.motivation_header_sarcastic
                    }
                ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text =
                pluralStringResource(
                    Res.plurals.motivation_title,
                    activeDays,
                    habitTitle,
                    activeDays,
                ),
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = flexFontEmphasis(),
                    fontWeight = FontWeight.Bold,
                ),
            textAlign = TextAlign.Center,
        )
        activityStreak?.let {
            Text(
                text = pluralStringResource(Res.plurals.energy_popup_line, it, it),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = stringResource(quote.textResource(), activeDays),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(Res.string.motivation_button))
        }
    }
}

@PreviewWrapper(GritPreviewWrapper::class)
@Preview
@Composable
private fun MotivatingPreview() {
    HabitMotivationPopup(
        habitTitle = "Meditate",
        activeDays = 1,
        quote =
            MotivationQuote("quote_starter_motivating_1", QuoteTone.MOTIVATING, StreakTier.STARTER),
        activityStreak = 13,
        onDismiss = {},
    )
}

@PreviewWrapper(GritPreviewWrapper::class)
@Preview
@Composable
private fun SarcasticPreview() {
    HabitMotivationPopup(
        habitTitle = "Learn Spanish for 20 minutes every morning",
        activeDays = 12,
        quote =
            MotivationQuote(
                "quote_established_sarcastic_1",
                QuoteTone.SARCASTIC,
                StreakTier.ESTABLISHED,
            ),
        activityStreak = null,
        onDismiss = {},
    )
}
