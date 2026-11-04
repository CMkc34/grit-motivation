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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GritQuoteEngineTest {

    private val starter = MotivationQuote("starter", QuoteTone.MOTIVATING, StreakTier.STARTER)
    private val building = MotivationQuote("building", QuoteTone.SARCASTIC, StreakTier.BUILDING)
    private val legend = MotivationQuote("legend", QuoteTone.MOTIVATING, StreakTier.LEGEND)
    private val oneQuotePerTier = listOf(starter, building, legend)

    private val buildingQuotes =
        listOf(
            MotivationQuote("b_motivating_1", QuoteTone.MOTIVATING, StreakTier.BUILDING),
            MotivationQuote("b_motivating_2", QuoteTone.MOTIVATING, StreakTier.BUILDING),
            MotivationQuote("b_sarcastic_1", QuoteTone.SARCASTIC, StreakTier.BUILDING),
            MotivationQuote("b_sarcastic_2", QuoteTone.SARCASTIC, StreakTier.BUILDING),
        )

    /**
     * Picks [count] quotes in a row for [activeDays], carrying the shown list like the app does.
     */
    private fun pickRow(
        count: Int,
        activeDays: Int,
        random: Random,
        quotes: List<MotivationQuote>,
        shown: List<String> = emptyList(),
    ): List<String> {
        var history = shown
        return List(count) {
            val pick = GritQuoteEngine.pick(activeDays, history, random, quotes)
            history = pick.shown
            pick.quote.id
        }
    }

    @Test
    fun `returns the quote of the tier that matches the streak`() {
        assertEquals(starter, GritQuoteEngine.pick(1, quotes = oneQuotePerTier).quote)
        assertEquals(building, GritQuoteEngine.pick(3, quotes = oneQuotePerTier).quote)
        assertEquals(legend, GritQuoteEngine.pick(40, quotes = oneQuotePerTier).quote)
    }

    @Test
    fun `same seed picks the same quotes`() {
        val first = pickRow(20, 3, Random(7), buildingQuotes)
        val second = pickRow(20, 3, Random(7), buildingQuotes)

        assertEquals(first, second)
    }

    @Test
    fun `both tones show up over many check-ins`() {
        val ids = pickRow(100, 3, Random(42), buildingQuotes)
        val tones = buildingQuotes.filter { it.id in ids }.map { it.tone }.toSet()

        assertEquals(setOf(QuoteTone.MOTIVATING, QuoteTone.SARCASTIC), tones)
    }

    @Test
    fun `shows every quote of a tier once before any repeats`() {
        repeat(30) { seed ->
            val ids = pickRow(12, 3, Random(seed), buildingQuotes)
            ids.chunked(buildingQuotes.size).forEach { round ->
                assertEquals(buildingQuotes.map { it.id }.toSet(), round.toSet(), "seed $seed")
            }
        }
    }

    @Test
    fun `never repeats the previous quote when a new round starts`() {
        repeat(50) { seed ->
            val ids = pickRow(20, 3, Random(seed), buildingQuotes)
            ids.zipWithNext().forEach { (previous, next) ->
                assertNotEquals(previous, next, "seed $seed")
            }
        }
    }

    @Test
    fun `a new round only clears the ids of its own tier`() {
        val shown = listOf("starter", "b_motivating_1", "b_motivating_2", "b_sarcastic_1")
        val pick = GritQuoteEngine.pick(3, shown, Random(1), buildingQuotes + starter)

        assertEquals("b_sarcastic_2", pick.quote.id)
        assertEquals(shown + "b_sarcastic_2", pick.shown)

        val next = GritQuoteEngine.pick(3, pick.shown, Random(1), buildingQuotes + starter)
        assertNotEquals("b_sarcastic_2", next.quote.id)
        assertEquals(listOf("starter", next.quote.id), next.shown)
    }

    @Test
    fun `drops ids of quotes that no longer exist`() {
        val pick = GritQuoteEngine.pick(1, listOf("removed_quote"), quotes = oneQuotePerTier)

        assertEquals(listOf("starter"), pick.shown)
    }

    @Test
    fun `repeats the only quote of a tier`() {
        val pick = GritQuoteEngine.pick(1, listOf("starter"), quotes = oneQuotePerTier)

        assertEquals(starter, pick.quote)
        assertEquals(listOf("starter"), pick.shown)
    }

    @Test
    fun `falls back to the default quote when no quote fits`() {
        assertEquals(QuoteCatalog.fallback, GritQuoteEngine.pick(3, quotes = emptyList()).quote)
        assertEquals(QuoteCatalog.fallback, GritQuoteEngine.pick(7, quotes = oneQuotePerTier).quote)
    }
}
