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

import kotlin.test.Test
import kotlin.test.assertEquals

class QuoteCatalogTest {

    @Test
    fun `every tier offers a motivating and a sarcastic quote`() {
        StreakTier.entries.forEach { tier ->
            val tones = QuoteCatalog.all.filter { it.tier == tier }.map { it.tone }.toSet()
            assertEquals(QuoteTone.entries.toSet(), tones, "tier $tier")
        }
    }

    @Test
    fun `every tier and tone offers QUOTES_PER_POOL quotes with their own ids`() {
        StreakTier.entries.forEach { tier ->
            QuoteTone.entries.forEach { tone ->
                val pool = QuoteCatalog.all.filter { it.tier == tier && it.tone == tone }
                assertEquals(QuoteCatalog.QUOTES_PER_POOL, pool.size, "$tier $tone")
            }
        }
        assertEquals(QuoteCatalog.all.size, QuoteCatalog.all.map { it.id }.toSet().size)
        assertEquals("quote_starter_motivating_1", QuoteCatalog.all.first().id)
    }
}
