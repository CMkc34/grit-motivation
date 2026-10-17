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

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuoteTranslationTest {

    private val resources = File("src/commonMain/composeResources")

    private fun quoteNames(valuesDir: String): Set<String> {
        val file = resources.resolve("$valuesDir/motivation_strings.xml")
        assertTrue(file.exists(), "missing ${file.path}")
        val strings =
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(file)
                .getElementsByTagName("string")
        return (0 until strings.length)
            .map { strings.item(it).attributes.getNamedItem("name").nodeValue }
            .toSet()
    }

    @Test
    fun `german and english offer the same quotes`() {
        assertEquals(quoteNames("values"), quoteNames("values-de"))
    }
}
