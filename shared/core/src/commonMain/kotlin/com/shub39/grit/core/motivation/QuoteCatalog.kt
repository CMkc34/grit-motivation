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
package com.shub39.grit.core.motivation

import com.shub39.grit.core.motivation.QuoteTone.MOTIVATING
import com.shub39.grit.core.motivation.StreakTier.STARTER

/** All quotes the engine can pick from. Texts live in the UI's string resources, keyed by id. */
object QuoteCatalog {
    /** Shown when no quote fits the streak, so a check-in never ends without a message. */
    val fallback = MotivationQuote("quote_fallback", MOTIVATING, STARTER)

    /** Quotes per tier and tone (#146): 7 own lines, 5 film-style lines, 3 historical quotes. */
    const val QUOTES_PER_POOL = 15

    /**
     * Every quote, ids `quote_<tier>_<tone>_<n>` with n from 1 to [QUOTES_PER_POOL]; the ids are
     * the string resource names in shared:ui.
     */
    val all: List<MotivationQuote> =
        StreakTier.entries.flatMap { tier ->
            QuoteTone.entries.flatMap { tone ->
                (1..QUOTES_PER_POOL).map { n ->
                    MotivationQuote(
                        "quote_${tier.name.lowercase()}_${tone.name.lowercase()}_$n",
                        tone,
                        tier,
                    )
                }
            }
        }
}
