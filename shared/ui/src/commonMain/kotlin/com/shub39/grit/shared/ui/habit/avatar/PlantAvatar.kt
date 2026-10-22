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
package com.shub39.grit.shared.ui.habit.avatar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.shub39.grit.core.avatar.PlantMood
import com.shub39.grit.core.avatar.PlantStage
import com.shub39.grit.core.avatar.ShopCatalog
import com.shub39.grit.core.avatar.ShopItemKind
import com.shub39.grit.shared.ui.GritPreviewWrapper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// fixed palette, the plant looks the same in light and dark theme
private val leafGreen = Color(0xFF4CAF50)
private val leafDark = Color(0xFF2E7D32)
private val trunkBrown = Color(0xFF795548)
private val soilBrown = Color(0xFF5D4037)
private val seedBrown = Color(0xFF8D6E63)
private val meadowGreen = Color(0xFF7CB342)
private val blossomPink = Color(0xFFF48FB1)
private val fruitRed = Color(0xFFE53935)
private val lightYellow = Color(0xFFFFEB3B)
private val tiredGray = Color(0xFF9E9E9E)

/** Color of a pot color item; unknown ids fall back to terracotta. */
fun potColorOf(itemId: String): Color =
    when (itemId) {
        "pot_blue" -> Color(0xFF4A7BD0)
        "pot_yellow" -> Color(0xFFE8C547)
        "pot_green" -> Color(0xFF5FA55A)
        "pot_purple" -> Color(0xFF8E5CC2)
        "pot_gold" -> Color(0xFFD4AF37)
        else -> Color(0xFFC8693E) // ShopCatalog.DEFAULT_POT_COLOR
    }

/**
 * The plant avatar, drawn in code (no image assets). It grows through the 12 [PlantStage]s from a
 * seed in a pot to a mighty tree on a meadow. A tired plant droops a little and looks paler. The
 * gentle sway follows the system's animation setting.
 */
@Composable
fun PlantAvatar(
    stage: PlantStage,
    mood: PlantMood,
    modifier: Modifier = Modifier,
    potColor: String = ShopCatalog.DEFAULT_POT_COLOR,
    accessories: Set<String> = emptySet(),
    animate: Boolean = true,
) {
    val sway = if (animate) rememberSway() else 0f

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val plant =
            PlantPainter(
                scope = this,
                stage = stage,
                tired = mood == PlantMood.TIRED,
                potColor = potColorOf(potColor),
                accessories = accessories,
            )
        plant.draw(swayDegrees = sway * plant.maxSway)
    }
}

/** -1..1, back and forth every few seconds. */
@Composable
private fun rememberSway(): Float {
    val transition = rememberInfiniteTransition(label = "plant sway")
    val sway by
        transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 2800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "sway",
        )
    return sway
}

private class PlantPainter(
    private val scope: DrawScope,
    private val stage: PlantStage,
    private val tired: Boolean,
    private val potColor: Color,
    private val accessories: Set<String>,
) {
    private val w = scope.size.width
    private val h = scope.size.height
    private val onMeadow = stage >= PlantStage.YOUNG_TREE
    private val isTree = stage >= PlantStage.SAPLING

    /** Where the plant leaves the ground. */
    private val base = Offset(w * 0.5f, if (onMeadow) h * 0.86f else h * 0.74f)

    // small plants sway more than big trees
    val maxSway: Float = if (isTree) 1.2f else 3f

    private fun Color.mood(): Color = if (tired) lerp(this, tiredGray, 0.35f) else this

    private fun x(f: Float) = w * f

    private fun y(f: Float) = h * f

    fun draw(swayDegrees: Float) {
        with(scope) {
            if (onMeadow) drawMeadow() else drawPot()
            rotate(degrees = swayDegrees, pivot = base) { drawPlant() }
            drawGroundAccessories()
        }
    }

    // ground

    private fun DrawScope.drawPot() {
        val top = y(0.76f)
        val bottom = y(0.97f)
        val pot =
            Path().apply {
                moveTo(x(0.29f), top)
                lineTo(x(0.71f), top)
                lineTo(x(0.65f), bottom)
                lineTo(x(0.35f), bottom)
                close()
            }
        drawPath(pot, potColor)
        drawRoundRect(
            color = lerp(potColor, Color.Black, 0.12f),
            topLeft = Offset(x(0.26f), y(0.72f)),
            size = Size(x(0.48f), y(0.06f)),
            cornerRadius = CornerRadius(x(0.02f)),
        )
        drawOval(soilBrown, topLeft = Offset(x(0.3f), y(0.715f)), size = Size(x(0.4f), y(0.04f)))
    }

    private fun DrawScope.drawMeadow() {
        // the hill is cut at the bottom edge so it never covers the name below the plant
        clipRect {
            drawOval(
                color = meadowGreen.mood(),
                topLeft = Offset(x(-0.1f), y(0.82f)),
                size = Size(x(1.2f), y(0.3f)),
            )
        }
        // the pot color lives on as a little sign next to the tree
        drawLine(trunkBrown, Offset(x(0.16f), y(0.9f)), Offset(x(0.16f), y(0.78f)), x(0.015f))
        drawRoundRect(
            color = potColor,
            topLeft = Offset(x(0.08f), y(0.74f)),
            size = Size(x(0.16f), y(0.07f)),
            cornerRadius = CornerRadius(x(0.015f)),
        )
    }

    // plant

    private fun DrawScope.drawPlant() {
        when (stage) {
            PlantStage.SEED -> drawSeed()
            PlantStage.GERM -> {
                drawSeed()
                drawStem(height = 0.06f, width = 0.012f)
                drawLeaf(stemTop(0.06f), angle = -50f, length = 0.05f)
            }
            PlantStage.SEEDLING -> drawHerb(height = 0.13f, leafPairs = 1, leafLength = 0.08f)
            PlantStage.SPROUT -> drawHerb(height = 0.2f, leafPairs = 2, leafLength = 0.09f)
            PlantStage.PLANTLET -> drawHerb(height = 0.27f, leafPairs = 3, leafLength = 0.1f)
            PlantStage.YOUNG_PLANT -> drawHerb(height = 0.33f, leafPairs = 4, leafLength = 0.11f)
            PlantStage.SAPLING -> drawTree(height = 0.36f, trunk = 0.025f, crown = 0.12f, lobes = 3)
            PlantStage.YOUNG_TREE ->
                drawTree(height = 0.4f, trunk = 0.035f, crown = 0.15f, lobes = 4)
            PlantStage.TREE -> drawTree(height = 0.45f, trunk = 0.045f, crown = 0.19f, lobes = 5)
            PlantStage.BIG_TREE -> drawTree(height = 0.5f, trunk = 0.055f, crown = 0.23f, lobes = 6)
            PlantStage.BLOSSOMING_TREE ->
                drawTree(height = 0.52f, trunk = 0.06f, crown = 0.25f, lobes = 7)
            PlantStage.MIGHTY_TREE ->
                drawTree(height = 0.55f, trunk = 0.07f, crown = 0.29f, lobes = 8)
        }
        drawPlantAccessories()
    }

    private fun stemTop(height: Float) = Offset(base.x, base.y - y(height))

    private fun DrawScope.drawSeed() {
        drawOval(
            color = seedBrown.mood(),
            topLeft = Offset(base.x - x(0.035f), base.y - y(0.03f)),
            size = Size(x(0.07f), y(0.045f)),
        )
    }

    private fun DrawScope.drawStem(height: Float, width: Float, color: Color = leafDark) {
        val top = stemTop(height)
        val stem =
            Path().apply {
                moveTo(base.x, base.y)
                // a tired plant bends over a bit
                quadraticTo(
                    base.x + x(if (tired) 0.05f else 0.015f),
                    (base.y + top.y) / 2,
                    top.x + x(if (tired) 0.04f else 0f),
                    top.y + y(if (tired) 0.02f else 0f),
                )
            }
        drawPath(stem, color.mood(), style = Stroke(width = x(width), cap = StrokeCap.Round))
    }

    /** A leaf growing from [from]; [angle] is measured from pointing straight up. */
    private fun DrawScope.drawLeaf(
        from: Offset,
        angle: Float,
        length: Float,
        color: Color = leafGreen,
    ) {
        val droop = if (tired) 25f * (if (angle < 0) -1 else 1) else 0f
        val len = x(length)
        val leaf =
            Path().apply {
                moveTo(from.x, from.y)
                quadraticTo(from.x + len * 0.35f, from.y - len * 0.5f, from.x, from.y - len)
                quadraticTo(from.x - len * 0.35f, from.y - len * 0.5f, from.x, from.y)
                close()
            }
        rotate(degrees = angle + droop, pivot = from) {
            drawPath(leaf, color.mood())
            drawLine(
                leafDark.mood(),
                from,
                Offset(from.x, from.y - len * 0.85f),
                strokeWidth = x(0.004f),
            )
        }
    }

    private fun DrawScope.drawHerb(height: Float, leafPairs: Int, leafLength: Float) {
        drawStem(height = height, width = 0.012f + height * 0.02f)
        for (pair in 1..leafPairs) {
            val at = height * pair / (leafPairs + 0.6f)
            val from = stemTop(at)
            val size = leafLength * (1f - 0.12f * (leafPairs - pair))
            drawLeaf(from, angle = -60f, length = size)
            drawLeaf(from, angle = 60f, length = size)
        }
        drawLeaf(stemTop(height), angle = -10f, length = leafLength * 0.8f)
    }

    private fun DrawScope.drawTree(height: Float, trunk: Float, crown: Float, lobes: Int) {
        val top = stemTop(height)
        val trunkPath =
            Path().apply {
                moveTo(base.x - x(trunk), base.y)
                quadraticTo(
                    base.x - x(trunk * 0.4f),
                    base.y - y(height * 0.5f),
                    top.x - x(trunk * 0.35f),
                    top.y,
                )
                lineTo(top.x + x(trunk * 0.35f), top.y)
                quadraticTo(
                    base.x + x(trunk * 0.4f),
                    base.y - y(height * 0.5f),
                    base.x + x(trunk),
                    base.y,
                )
                close()
            }
        drawPath(trunkPath, trunkBrown.mood())
        if (stage == PlantStage.MIGHTY_TREE) {
            // roots
            drawLine(
                trunkBrown.mood(),
                base,
                Offset(base.x - x(0.12f), base.y + y(0.03f)),
                x(0.02f),
                StrokeCap.Round,
            )
            drawLine(
                trunkBrown.mood(),
                base,
                Offset(base.x + x(0.13f), base.y + y(0.025f)),
                x(0.02f),
                StrokeCap.Round,
            )
        }

        // crown: a ring of overlapping circles around a big one, tired crowns sag a little
        val center = Offset(top.x, top.y + y(if (tired) 0.02f else 0f))
        val radius = x(crown)
        drawCircle(leafDark.mood(), radius = radius, center = center)
        for (i in 0 until lobes) {
            val angle = 2 * PI * i / lobes
            val lobeCenter =
                Offset(
                    center.x + (radius * 0.7f * cos(angle)).toFloat(),
                    center.y + (radius * 0.55f * sin(angle)).toFloat(),
                )
            drawCircle(leafGreen.mood(), radius = radius * 0.55f, center = lobeCenter)
        }
        drawCircle(leafGreen.mood(), radius = radius * 0.6f, center = center)

        if (stage == PlantStage.BLOSSOMING_TREE || stage == PlantStage.MIGHTY_TREE) {
            crownSpots(center, radius, count = 9, seed = 1).forEach {
                drawCircle(blossomPink.mood(), radius = radius * 0.08f, center = it)
            }
        }
    }

    /** Evenly spread points inside the crown, the same on every frame. */
    private fun crownSpots(center: Offset, radius: Float, count: Int, seed: Int): List<Offset> =
        (0 until count).map { i ->
            val angle = 2 * PI * (i + seed * 0.37) / count
            val distance = radius * (0.35f + 0.45f * ((i * 7 + seed * 3) % 5) / 4f)
            Offset(
                center.x + (distance * cos(angle)).toFloat(),
                center.y + (distance * 0.8f * sin(angle)).toFloat(),
            )
        }

    // accessories

    private val crownCenter: Offset
        get() =
            when {
                isTree -> stemTop(treeHeight())
                else -> stemTop(herbHeight() * 0.7f)
            }

    private fun treeHeight() =
        when (stage) {
            PlantStage.SAPLING -> 0.36f
            PlantStage.YOUNG_TREE -> 0.4f
            PlantStage.TREE -> 0.45f
            PlantStage.BIG_TREE -> 0.5f
            PlantStage.BLOSSOMING_TREE -> 0.52f
            else -> 0.55f
        }

    private fun herbHeight() =
        when (stage) {
            PlantStage.SEED -> 0.02f
            PlantStage.GERM -> 0.06f
            PlantStage.SEEDLING -> 0.13f
            PlantStage.SPROUT -> 0.2f
            PlantStage.PLANTLET -> 0.27f
            else -> 0.33f
        }

    private fun crownRadius() =
        when (stage) {
            PlantStage.SAPLING -> x(0.12f)
            PlantStage.YOUNG_TREE -> x(0.15f)
            PlantStage.TREE -> x(0.19f)
            PlantStage.BIG_TREE -> x(0.23f)
            PlantStage.BLOSSOMING_TREE -> x(0.25f)
            PlantStage.MIGHTY_TREE -> x(0.29f)
            else -> x(0.08f)
        }

    private fun DrawScope.drawPlantAccessories() {
        val center = crownCenter
        val radius = crownRadius()

        if ("acc_blossoms" in accessories) {
            crownSpots(center, radius, count = 6, seed = 2).forEach {
                drawCircle(Color.White, radius = radius * 0.12f, center = it)
                drawCircle(lightYellow, radius = radius * 0.05f, center = it)
            }
        }
        if ("acc_fruits" in accessories) {
            crownSpots(center, radius, count = 7, seed = 4).forEach {
                drawCircle(fruitRed, radius = radius * 0.09f, center = it)
            }
        }
        if ("acc_fairy_lights" in accessories) {
            (0..8).forEach { i ->
                val t = i / 8f
                val light =
                    Offset(
                        center.x - radius * 0.9f + radius * 1.8f * t,
                        center.y + radius * 0.25f + radius * 0.25f * sin(PI * t).toFloat(),
                    )
                drawCircle(lightYellow, radius = radius * 0.05f, center = light)
            }
        }
        if ("acc_ladybug" in accessories) {
            val bug = Offset(center.x + radius * 0.45f, center.y - radius * 0.1f)
            drawCircle(fruitRed, radius = x(0.022f), center = bug)
            drawCircle(Color.Black, radius = x(0.008f), center = bug.copy(y = bug.y - x(0.018f)))
            drawCircle(Color.Black, radius = x(0.005f), center = bug.copy(x = bug.x - x(0.008f)))
            drawCircle(Color.Black, radius = x(0.005f), center = bug.copy(x = bug.x + x(0.008f)))
        }
        if ("acc_bird" in accessories && isTree) {
            drawBird(Offset(center.x - radius * 0.3f, center.y - radius * 0.95f))
        }
        if ("acc_birdhouse" in accessories && isTree) {
            drawBirdhouse(Offset(base.x, (base.y + center.y) / 2 + y(0.02f)))
        }
        if ("acc_swing" in accessories && isTree) {
            val branch = Offset(center.x + radius * 0.55f, center.y + radius * 0.35f)
            val seatY = base.y - y(0.08f)
            drawLine(trunkBrown, branch, Offset(branch.x - x(0.03f), seatY), x(0.006f))
            drawLine(
                trunkBrown,
                branch.copy(x = branch.x + x(0.06f)),
                Offset(branch.x + x(0.09f), seatY),
                x(0.006f),
            )
            drawRoundRect(
                color = trunkBrown,
                topLeft = Offset(branch.x - x(0.045f), seatY),
                size = Size(x(0.15f), y(0.02f)),
                cornerRadius = CornerRadius(x(0.006f)),
            )
        }
        if ("acc_butterfly" in accessories) {
            drawButterfly(Offset(center.x - radius - x(0.08f), center.y - y(0.04f)))
        }
    }

    private fun DrawScope.drawGroundAccessories() {
        if ("acc_watering_can" in accessories) {
            val can = Offset(x(0.8f), if (onMeadow) y(0.86f) else y(0.92f))
            val color = Color(0xFF78909C)
            drawRoundRect(
                color = color,
                topLeft = Offset(can.x - x(0.06f), can.y - y(0.09f)),
                size = Size(x(0.12f), y(0.09f)),
                cornerRadius = CornerRadius(x(0.015f)),
            )
            drawLine(
                color,
                Offset(can.x - x(0.05f), can.y - y(0.06f)),
                Offset(can.x - x(0.12f), can.y - y(0.12f)),
                x(0.015f),
                StrokeCap.Round,
            )
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(can.x - x(0.035f), can.y - y(0.14f)),
                size = Size(x(0.07f), y(0.08f)),
                style = Stroke(width = x(0.01f)),
            )
        }
        if ("acc_bird" in accessories && !isTree) {
            drawBird(Offset(x(0.24f), if (onMeadow) y(0.84f) else y(0.7f)))
        }
        if ("acc_birdhouse" in accessories && !isTree) {
            drawBirdhouse(Offset(x(0.82f), y(0.6f)))
        }
    }

    private fun DrawScope.drawBird(at: Offset) {
        val body = Color(0xFF42A5F5)
        drawOval(
            body,
            topLeft = Offset(at.x - x(0.035f), at.y - y(0.025f)),
            size = Size(x(0.07f), y(0.05f)),
        )
        drawCircle(body, radius = x(0.02f), center = Offset(at.x + x(0.03f), at.y - y(0.025f)))
        drawCircle(
            Color.Black,
            radius = x(0.004f),
            center = Offset(at.x + x(0.035f), at.y - y(0.03f)),
        )
        val beak =
            Path().apply {
                moveTo(at.x + x(0.048f), at.y - y(0.03f))
                lineTo(at.x + x(0.065f), at.y - y(0.022f))
                lineTo(at.x + x(0.048f), at.y - y(0.018f))
                close()
            }
        drawPath(beak, Color(0xFFFFA000))
    }

    private fun DrawScope.drawBirdhouse(at: Offset) {
        val wood = Color(0xFFA1887F)
        drawRect(
            wood,
            topLeft = Offset(at.x - x(0.04f), at.y - y(0.05f)),
            size = Size(x(0.08f), y(0.07f)),
        )
        val roof =
            Path().apply {
                moveTo(at.x - x(0.055f), at.y - y(0.05f))
                lineTo(at.x, at.y - y(0.09f))
                lineTo(at.x + x(0.055f), at.y - y(0.05f))
                close()
            }
        drawPath(roof, Color(0xFFD84315))
        drawCircle(soilBrown, radius = x(0.013f), center = Offset(at.x, at.y - y(0.02f)))
    }

    private fun DrawScope.drawButterfly(at: Offset) {
        val wing = Color(0xFFFFB300)
        drawOval(
            wing,
            topLeft = Offset(at.x - x(0.035f), at.y - y(0.03f)),
            size = Size(x(0.035f), y(0.035f)),
        )
        drawOval(wing, topLeft = Offset(at.x, at.y - y(0.03f)), size = Size(x(0.035f), y(0.035f)))
        drawOval(wing, topLeft = Offset(at.x - x(0.025f), at.y), size = Size(x(0.025f), y(0.025f)))
        drawOval(wing, topLeft = Offset(at.x, at.y), size = Size(x(0.025f), y(0.025f)))
        drawLine(
            Color.Black,
            Offset(at.x, at.y - y(0.03f)),
            Offset(at.x, at.y + y(0.025f)),
            x(0.006f),
            StrokeCap.Round,
        )
    }
}

@PreviewWrapper(GritPreviewWrapper::class)
@Preview(widthDp = 400, heightDp = 300)
@Composable
private fun PlantStagesPreview() {
    PlantGrid(mood = PlantMood.HAPPY)
}

@PreviewWrapper(GritPreviewWrapper::class)
@Preview(widthDp = 400, heightDp = 300)
@Composable
private fun TiredPlantStagesPreview() {
    PlantGrid(mood = PlantMood.TIRED)
}

@PreviewWrapper(GritPreviewWrapper::class)
@Preview
@Composable
private fun PlantAccessoriesPreview() {
    Row {
        PlantAvatar(
            stage = PlantStage.PLANTLET,
            mood = PlantMood.HAPPY,
            potColor = "pot_purple",
            accessories = setOf("acc_watering_can", "acc_ladybug", "acc_butterfly", "acc_blossoms"),
            animate = false,
            modifier = Modifier.size(160.dp),
        )
        PlantAvatar(
            stage = PlantStage.MIGHTY_TREE,
            mood = PlantMood.HAPPY,
            potColor = "pot_gold",
            accessories =
                ShopCatalog.items
                    .filter { it.kind == ShopItemKind.ACCESSORY }
                    .map { it.id }
                    .toSet(),
            animate = false,
            modifier = Modifier.size(160.dp),
        )
    }
}

@Composable
private fun PlantGrid(mood: PlantMood) {
    Column {
        PlantStage.entries.chunked(4).forEach { row ->
            Row {
                row.forEach { stage ->
                    PlantAvatar(
                        stage = stage,
                        mood = mood,
                        animate = false,
                        modifier = Modifier.size(100.dp),
                    )
                }
            }
        }
    }
}
