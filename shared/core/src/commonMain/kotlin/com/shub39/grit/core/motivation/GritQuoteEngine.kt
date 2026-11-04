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

import kotlin.random.Random

/** A picked [quote] and the updated list of [shown] quote ids to store for the next pick. */
data class QuotePick(val quote: MotivationQuote, val shown: List<String>)

object GritQuoteEngine {

    /**
     * Picks a random quote for the tier of [activeDays] that is not in [shown], so every quote of a
     * tier appears once before any repeats (#146). When all quotes of the tier were shown, the
     * tier's ids are cleared from the list and a new round starts, skipping the last shown quote so
     * it never comes twice in a row. Ids no longer in [quotes] are dropped from the list. Without
     * any fitting quote, [QuoteCatalog.fallback] is returned and [shown] stays as it is.
     */
    fun pick(
        activeDays: Int,
        shown: List<String> = emptyList(),
        random: Random = Random.Default,
        quotes: List<MotivationQuote> = QuoteCatalog.all,
    ): QuotePick {
        val tier = StreakTier.forStreak(activeDays)
        val candidates = quotes.filter { it.tier == tier }
        if (candidates.isEmpty()) return QuotePick(QuoteCatalog.fallback, shown)

        val known = quotes.map { it.id }.toSet()
        val tierIds = candidates.map { it.id }.toSet()
        val history = shown.filter { it in known }
        val unseen = candidates.filter { it.id !in history }

        if (unseen.isNotEmpty()) {
            val quote = unseen.random(random)
            return QuotePick(quote, history + quote.id)
        }

        val last = history.lastOrNull { it in tierIds }
        val quote = candidates.filter { it.id != last }.ifEmpty { candidates }.random(random)
        return QuotePick(quote, history.filter { it !in tierIds } + quote.id)
    }
}
