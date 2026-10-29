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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

// fixed palette, the plant looks the same in light and dark theme
internal val outline = Color(0xFF2B3A2C)
private val leafGreen = Color(0xFF6CCB5F)
private val leafLight = Color(0xFF93DB78)
private val stemGreen = Color(0xFF3FA34D)
private val trunkBrown = Color(0xFFA0704C)
private val soilBrown = Color(0xFF6B4A3A)
private val seedBrown = Color(0xFFD2A26E)
internal val meadowGreen = Color(0xFF9BD770)
private val flowerYellow = Color(0xFFFFD45C)
private val fruitRed = Color(0xFFF25C54)
private val blush = Color(0xFFFF8FA0)
private val lightYellow = Color(0xFFFFEB3B)
internal val tiredGray = Color(0xFFA3A79F)

/** Outline width in units of the avatar's size. */
internal const val OUTLINE = 0.013f

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
 * The plant avatar, drawn in code (no image assets) in a sticker look: every shape has a dark
 * outline, and the plant has a face, first on the seed, then on the pot, later on the trunk. It
 * grows through the 12 [PlantStage]s from a seed in a pot to a mighty tree on a meadow; from the
 * sapling on, branches grow out of the trunk and act as arms. A tired plant sleeps, droops its
 * leaves and arms and looks paler. The gentle sway follows the system's animation setting.
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
        // everything below is drawn in a 1 x 1 square
        scale(scaleX = size.width, scaleY = size.height, pivot = Offset.Zero) {
            drawPlantAvatar(stage, mood, potColor, accessories, sway)
        }
    }
}

/** Draws the plant into the 1 x 1 square at the origin; [sway] is -1..1 from [rememberSway]. */
internal fun DrawScope.drawPlantAvatar(
    stage: PlantStage,
    mood: PlantMood,
    potColor: String,
    accessories: Set<String>,
    sway: Float,
) {
    val plant =
        PlantPainter(
            stage = stage,
            tired = mood == PlantMood.TIRED,
            potColor = potColorOf(potColor),
            accessories = accessories,
        )
    with(plant) { draw(swayDegrees = sway * plant.maxSway) }
}

/** -1..1, back and forth every few seconds. */
@Composable
internal fun rememberSway(): Float {
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

/** Size of a tree: trunk [height] and [trunk] half width at the ground, [crown] radius. */
private class TreeShape(val height: Float, val trunk: Float, val crown: Float, val lobes: Int)

private fun treeShapeOf(stage: PlantStage): TreeShape? =
    when (stage) {
        PlantStage.SAPLING -> TreeShape(height = 0.3f, trunk = 0.03f, crown = 0.13f, lobes = 3)
        PlantStage.YOUNG_TREE -> TreeShape(height = 0.36f, trunk = 0.07f, crown = 0.17f, lobes = 5)
        PlantStage.TREE -> TreeShape(height = 0.41f, trunk = 0.08f, crown = 0.2f, lobes = 6)
        PlantStage.BIG_TREE -> TreeShape(height = 0.46f, trunk = 0.09f, crown = 0.24f, lobes = 7)
        PlantStage.BLOSSOMING_TREE ->
            TreeShape(height = 0.48f, trunk = 0.095f, crown = 0.25f, lobes = 7)
        PlantStage.MIGHTY_TREE -> TreeShape(height = 0.5f, trunk = 0.1f, crown = 0.29f, lobes = 8)
        else -> null
    }

/** [from] moved by ([dx], [dy]) after turning that offset clockwise by [degrees]. */
private fun turned(from: Offset, degrees: Float, dx: Float, dy: Float): Offset {
    val a = degrees * PI.toFloat() / 180f
    val s = sin(a)
    val c = cos(a)
    return Offset(from.x + dx * c - dy * s, from.y + dx * s + dy * c)
}

private fun circlePath(center: Offset, radius: Float) =
    Path().apply { addOval(Rect(center = center, radius = radius)) }

/** A round leaf growing from [from]; [degrees] is measured from pointing straight up. */
private fun leafPath(from: Offset, degrees: Float, length: Float): Path {
    fun p(dx: Float, dy: Float) = turned(from, degrees, dx * length, dy * length)
    val width = 0.48f
    return Path().apply {
        moveTo(from.x, from.y)
        val a = p(width * 1.1f, -0.2f)
        val b = p(width * 0.9f, -0.95f)
        val tip = p(0f, -1f)
        cubicTo(a.x, a.y, b.x, b.y, tip.x, tip.y)
        val c = p(-width * 0.9f, -0.95f)
        val d = p(-width * 1.1f, -0.2f)
        cubicTo(c.x, c.y, d.x, d.y, from.x, from.y)
        close()
    }
}

/**
 * A branch along a quadratic curve from [start] over [control] to [end] that tapers from
 * [startWidth] to [endWidth] and ends round.
 */
private fun branchPath(
    start: Offset,
    control: Offset,
    end: Offset,
    startWidth: Float,
    endWidth: Float,
): Path {
    fun at(t: Float): Offset {
        val u = 1 - t
        return Offset(
            u * u * start.x + 2 * u * t * control.x + t * t * end.x,
            u * u * start.y + 2 * u * t * control.y + t * t * end.y,
        )
    }
    val steps = 14
    val left = mutableListOf<Offset>()
    val right = mutableListOf<Offset>()
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val p = at(t)
        val before = at(max(0f, t - 0.01f))
        val after = at(min(1f, t + 0.01f))
        val dx = after.x - before.x
        val dy = after.y - before.y
        val length = hypot(dx, dy).takeIf { it > 0f } ?: 1f
        val half = (startWidth + (endWidth - startWidth) * t) / 2
        left += Offset(p.x - dy / length * half, p.y + dx / length * half)
        right += Offset(p.x + dy / length * half, p.y - dx / length * half)
    }
    val tipAngle = atan2(end.y - at(0.97f).y, end.x - at(0.97f).x) * 180f / PI.toFloat()
    return Path().apply {
        moveTo(left.first().x, left.first().y)
        left.forEach { lineTo(it.x, it.y) }
        arcTo(Rect(center = end, radius = endWidth / 2), tipAngle + 90f, -180f, false)
        right.asReversed().forEach { lineTo(it.x, it.y) }
        close()
    }
}

/** Draws in a 1 x 1 square, see [PlantAvatar]. */
private class PlantPainter(
    private val stage: PlantStage,
    private val tired: Boolean,
    private val potColor: Color,
    private val accessories: Set<String>,
) {
    private val inPot = stage <= PlantStage.SAPLING
    private val isTree = stage >= PlantStage.SAPLING
    private val tree = treeShapeOf(stage)

    /** Where the plant leaves the ground. */
    private val base = Offset(0.5f, if (inPot) 0.67f else 0.86f)

    // small plants sway more than big trees
    val maxSway: Float = if (isTree) 1.2f else 3f

    private fun Color.mood(): Color = if (tired) lerp(this, tiredGray, 0.4f) else this

    private val leaf = leafGreen.mood()
    private val leafHighlight = leafLight.mood()
    private val stem = stemGreen.mood()
    private val wood = trunkBrown.mood()

    // a tired plant droops
    private val sag = if (tired) 0.025f else 0f

    private fun stemTop(height: Float) = Offset(base.x, base.y - height)

    fun DrawScope.draw(swayDegrees: Float) {
        if (inPot) drawPotBody() else drawMeadow()
        rotate(degrees = swayDegrees, pivot = base) { drawPlant() }
        if (inPot) drawPotRim()
        drawGroundAccessories()
    }

    // sticker look: a dark outline under every shape

    private fun DrawScope.sticker(path: Path, color: Color) {
        drawPath(path, outline, style = Stroke(width = OUTLINE * 2, join = StrokeJoin.Round))
        drawPath(path, color)
    }

    private fun DrawScope.face(center: Offset, size: Float) {
        val (x, y) = center
        if (tired) {
            // asleep: closed eyes and a small frown
            for (side in listOf(-1f, 1f)) {
                val eye = Offset(x + side * 0.38f * size, y - 0.02f * size)
                val r = 0.13f * size
                drawArc(
                    color = outline,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(eye.x - r, eye.y - r),
                    size = Size(2 * r, 2 * r),
                    style = Stroke(width = size * 0.08f, cap = StrokeCap.Round),
                )
            }
            val mouth =
                Path().apply {
                    moveTo(x - 0.14f * size, y + 0.3f * size)
                    quadraticTo(x, y + 0.22f * size, x + 0.14f * size, y + 0.3f * size)
                }
            drawPath(mouth, outline, style = Stroke(width = size * 0.07f, cap = StrokeCap.Round))
        } else {
            for (side in listOf(-1f, 1f)) {
                val eye = Offset(x + side * 0.38f * size, y)
                drawCircle(outline, radius = 0.12f * size, center = eye)
                drawCircle(
                    Color.White,
                    radius = 0.04f * size,
                    center = Offset(eye.x + 0.04f * size, eye.y - 0.04f * size),
                )
                drawOval(
                    color = blush,
                    alpha = 0.7f,
                    topLeft = Offset(x + side * 0.66f * size - 0.15f * size, y + 0.14f * size),
                    size = Size(0.3f * size, 0.16f * size),
                )
            }
            val r = 0.2f * size
            drawArc(
                color = outline,
                startAngle = 25f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(x - r, y + 0.1f * size - r),
                size = Size(2 * r, 2 * r),
                style = Stroke(width = size * 0.08f, cap = StrokeCap.Round),
            )
        }
    }

    // ground

    private fun DrawScope.drawPotBody() {
        val pot =
            Path().apply {
                moveTo(0.31f, 0.72f)
                lineTo(0.69f, 0.72f)
                lineTo(0.645f, 0.92f)
                quadraticTo(0.64f, 0.955f, 0.6f, 0.955f)
                lineTo(0.4f, 0.955f)
                quadraticTo(0.36f, 0.955f, 0.355f, 0.92f)
                close()
            }
        sticker(pot, potColor)
        // seed and germ wear the face themselves
        if (stage >= PlantStage.SEEDLING) face(Offset(0.5f, 0.83f), size = 0.14f)
    }

    private fun DrawScope.drawPotRim() {
        val rim =
            Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(Offset(0.27f, 0.665f), Size(0.46f, 0.07f)),
                        cornerRadius = CornerRadius(0.03f),
                    )
                )
            }
        sticker(rim, lerp(potColor, Color.Black, 0.08f))
        drawOval(soilBrown, topLeft = Offset(0.31f, 0.659f), size = Size(0.38f, 0.032f))
    }

    private fun DrawScope.drawMeadow() {
        // the hill is cut at the bottom edge so it never covers the name below the plant
        clipRect(left = 0f, top = 0f, right = 1f, bottom = 1f) {
            val hill = Path().apply { addOval(Rect(Offset(-0.14f, 0.83f), Size(1.28f, 0.34f))) }
            sticker(hill, meadowGreen.mood())
            if (stage >= PlantStage.TREE) {
                listOf(Offset(0.84f, 0.91f), Offset(0.3f, 0.95f), Offset(0.64f, 0.97f)).forEach {
                    for (a in 0 until 5) {
                        drawCircle(
                            Color.White,
                            radius = 0.011f,
                            center = turned(it, a * 72f, 0f, -0.016f),
                        )
                    }
                    drawCircle(flowerYellow, radius = 0.009f, center = it)
                }
            }
        }
        // the pot color lives on as a little sign next to the tree
        drawLine(
            outline,
            Offset(0.16f, 0.88f),
            Offset(0.16f, 0.77f),
            0.015f + OUTLINE * 2,
            StrokeCap.Round,
        )
        drawLine(wood, Offset(0.16f, 0.88f), Offset(0.16f, 0.77f), 0.015f, StrokeCap.Round)
        val sign =
            Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(Offset(0.08f, 0.73f), Size(0.16f, 0.07f)),
                        cornerRadius = CornerRadius(0.015f),
                    )
                )
            }
        sticker(sign, potColor)
    }

    // plant

    private fun DrawScope.drawPlant() {
        when (stage) {
            PlantStage.SEED -> drawSeed(Offset(0.5f, 0.6f), 0.085f)
            PlantStage.GERM -> {
                val sprout =
                    Path().apply {
                        moveTo(0.5f, 0.51f)
                        quadraticTo(0.49f, 0.46f, 0.53f, 0.435f)
                    }
                drawPath(
                    sprout,
                    outline,
                    style = Stroke(width = 0.018f + OUTLINE * 2, cap = StrokeCap.Round),
                )
                drawPath(sprout, stem, style = Stroke(width = 0.018f, cap = StrokeCap.Round))
                drawLeaf(Offset(0.53f, 0.44f), degrees = 55f, length = 0.07f)
                drawSeed(Offset(0.5f, 0.6f), 0.085f)
            }
            PlantStage.SEEDLING -> {
                drawStem(height = 0.15f, width = 0.022f)
                drawLeafPair(0.15f, 55f, 0.085f, leafHighlight)
            }
            PlantStage.SPROUT -> {
                drawStem(height = 0.23f, width = 0.024f)
                drawLeafPair(0.1f, 62f, 0.08f, leafHighlight)
                drawLeafPair(0.23f, 30f, 0.11f)
            }
            PlantStage.PLANTLET -> {
                drawStem(height = 0.29f, width = 0.026f)
                drawLeafPair(0.08f, 65f, 0.09f)
                drawLeafPair(0.18f, 55f, 0.1f)
                drawLeafPair(0.29f, 20f, 0.11f)
            }
            PlantStage.YOUNG_PLANT -> {
                drawStem(height = 0.34f, width = 0.028f)
                drawLeafPair(0.08f, 65f, 0.1f)
                drawLeafPair(0.18f, 58f, 0.11f)
                drawLeafPair(0.27f, 50f, 0.1f)
                val top = stemTop(0.34f)
                drawFlower(
                    Offset(top.x + if (tired) 0.045f else 0f, top.y + if (tired) 0.03f else 0f),
                    0.03f,
                )
            }
            else -> drawTree(tree!!)
        }
        drawPlantAccessories()
    }

    private fun DrawScope.drawSeed(center: Offset, r: Float) {
        val (x, y) = center
        val seed =
            Path().apply {
                moveTo(x, y - r * 1.1f)
                cubicTo(x + r * 1.1f, y - r * 1.05f, x + r * 1.05f, y + r * 0.85f, x, y + r * 0.85f)
                cubicTo(x - r * 1.05f, y + r * 0.85f, x - r * 1.1f, y - r * 1.05f, x, y - r * 1.1f)
                close()
            }
        sticker(seed, seedBrown.mood())
        face(Offset(x, y - r * 0.02f), size = r * 1.12f)
    }

    private fun DrawScope.drawStem(height: Float, width: Float) {
        val top = stemTop(height)
        val path =
            Path().apply {
                moveTo(base.x, base.y)
                // a tired plant bends over a bit
                quadraticTo(
                    base.x + if (tired) 0.05f else 0.012f,
                    (base.y + top.y) / 2,
                    top.x + if (tired) 0.045f else 0f,
                    top.y + sag,
                )
            }
        drawPath(path, outline, style = Stroke(width = width + OUTLINE * 2, cap = StrokeCap.Round))
        drawPath(path, stem, style = Stroke(width = width, cap = StrokeCap.Round))
    }

    private fun DrawScope.drawLeaf(
        from: Offset,
        degrees: Float,
        length: Float,
        color: Color = leaf,
    ) {
        val droop = if (tired) 28f * (if (degrees == 0f) 1f else sign(degrees)) else 0f
        sticker(leafPath(from, degrees + droop, length), color)
    }

    private fun DrawScope.drawLeafPair(
        height: Float,
        degrees: Float,
        length: Float,
        color: Color = leaf,
    ) {
        val from = stemTop(height)
        drawLeaf(from, -degrees, length, color)
        drawLeaf(from, degrees, length, color)
    }

    private fun DrawScope.drawFlower(center: Offset, size: Float) {
        for (a in 0 until 5) {
            sticker(circlePath(turned(center, a * 72f, 0f, -size), size * 0.62f), Color.White)
        }
        sticker(circlePath(center, size * 0.55f), flowerYellow)
    }

    private fun DrawScope.drawStar(center: Offset, size: Float) {
        val star =
            Path().apply {
                for (i in 0 until 8) {
                    val point =
                        turned(center, i * 45f, 0f, -(if (i % 2 == 1) size * 0.35f else size))
                    if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                close()
            }
        sticker(star, flowerYellow)
    }

    private fun trunkPath(shape: TreeShape): Path {
        val w = shape.trunk
        val top = stemTop(shape.height)
        return Path().apply {
            moveTo(base.x - w, base.y + 0.01f)
            quadraticTo(base.x - w * 0.55f, base.y - shape.height * 0.5f, top.x - w * 0.4f, top.y)
            lineTo(top.x + w * 0.4f, top.y)
            quadraticTo(
                base.x + w * 0.55f,
                base.y - shape.height * 0.5f,
                base.x + w,
                base.y + 0.01f,
            )
            close()
        }
    }

    /**
     * Branches that double as arms: thick at the trunk, thin at the tip, where they split into two
     * twigs with a leaf each, like fingers. Sapling and young tree wave with one arm, bigger trees
     * cheer with two; a tired tree lets them hang. Returns the wood and a function drawing the
     * leaves.
     */
    private fun arms(shape: TreeShape): Pair<List<Path>, DrawScope.() -> Unit> {
        val grown = (stage.ordinal - PlantStage.SAPLING.ordinal).toFloat()
        val sides = if (stage < PlantStage.TREE) listOf(1f) else listOf(1f, -1f)
        val w = shape.trunk
        val y = base.y - shape.height * 0.5f
        val wood = mutableListOf<Path>()
        val hands = mutableListOf<Triple<Offset, Float, Offset>>()
        for (side in sides) {
            val x = base.x + side * w * 0.1f
            val reach = shape.crown * if (tired) 0.85f else 1.05f
            val end = Offset(x + side * reach, if (tired) y + 0.1f else y - 0.13f - 0.005f * grown)
            wood +=
                branchPath(
                    start = Offset(x, y + 0.02f),
                    control = Offset(x + side * reach * 0.55f, if (tired) y - 0.02f else y + 0.01f),
                    end = end,
                    startWidth = w * 0.8f,
                    endWidth = 0.014f,
                )
            val twig1 =
                if (tired) Offset(end.x + side * 0.015f, end.y + 0.04f)
                else Offset(end.x + side * 0.035f, end.y - 0.03f)
            val twig2 =
                if (tired) Offset(end.x + side * 0.04f, end.y + 0.015f)
                else Offset(end.x - side * 0.005f, end.y - 0.045f)
            wood +=
                branchPath(end, Offset(end.x + side * 0.01f, end.y - 0.005f), twig1, 0.013f, 0.008f)
            wood += branchPath(end, Offset(end.x, end.y - 0.01f), twig2, 0.013f, 0.008f)
            hands += Triple(twig1, side, twig2)
        }
        val leaves: DrawScope.() -> Unit = {
            hands.forEach { (twig1, side, twig2) ->
                drawLeaf(twig1, side * if (tired) 150f else 60f, 0.04f)
                drawLeaf(twig2, side * if (tired) 170f else 15f, 0.04f)
            }
        }
        return wood to leaves
    }

    private fun DrawScope.drawTree(shape: TreeShape) {
        // trunk and arms are one piece of wood: all outlines first, then all fills
        val (armWood, armLeaves) = arms(shape)
        val pieces = listOf(trunkPath(shape)) + armWood
        pieces.forEach {
            drawPath(it, outline, style = Stroke(width = OUTLINE * 2, join = StrokeJoin.Round))
        }
        pieces.forEach { drawPath(it, wood) }

        val center = crownCenter
        val radius = shape.crown
        drawCrown(center, radius, shape.lobes)
        if (stage >= PlantStage.BLOSSOMING_TREE) {
            val blossoms = if (stage == PlantStage.MIGHTY_TREE) 8 else 9
            crownSpots(center, radius, count = blossoms, seed = 1).forEach {
                drawFlower(it, radius * if (stage == PlantStage.MIGHTY_TREE) 0.06f else 0.065f)
            }
        }
        if (stage == PlantStage.MIGHTY_TREE) {
            crownSpots(center, radius, count = 6, seed = 4).forEach {
                sticker(circlePath(it, radius * 0.075f), fruitRed.mood())
            }
            if (!tired) {
                drawStar(Offset(center.x - radius * 1.05f, center.y - radius * 0.55f), 0.03f)
                drawStar(Offset(center.x + radius * 1.08f, center.y - radius * 0.2f), 0.025f)
                drawStar(Offset(center.x + radius * 0.5f, center.y - radius * 1.02f), 0.022f)
            }
        }
        armLeaves(this)
        if (!inPot) face(Offset(base.x, base.y - shape.height * 0.28f), size = shape.trunk * 1.54f)
    }

    /** A cloud of overlapping circles with one shared outline and a few highlights on top. */
    private fun DrawScope.drawCrown(center: Offset, radius: Float, lobes: Int) {
        val parts = buildList {
            add(center to radius * 0.66f)
            for (i in 0 until lobes) {
                val angle = -PI / 2 + 2 * PI * i / lobes
                add(
                    Offset(
                        center.x + (radius * 0.58f * cos(angle)).toFloat(),
                        center.y + (radius * 0.46f * sin(angle)).toFloat(),
                    ) to radius * 0.48f
                )
            }
        }
        parts.forEach { (c, r) ->
            drawCircle(outline, radius = r, center = c, style = Stroke(width = OUTLINE * 2))
        }
        parts.forEach { (c, r) -> drawCircle(leaf, radius = r, center = c) }
        parts.drop(1).forEach { (c, r) ->
            if (c.y < center.y) {
                drawCircle(
                    leafHighlight,
                    radius = r * 0.32f,
                    center = Offset(c.x - r * 0.25f, c.y - r * 0.3f),
                )
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
            if (tree != null) Offset(base.x, base.y - tree.height + sag)
            else stemTop(herbHeight() * 0.7f)

    private fun herbHeight() =
        when (stage) {
            PlantStage.SEED -> 0.07f
            PlantStage.GERM -> 0.16f
            PlantStage.SEEDLING -> 0.15f
            PlantStage.SPROUT -> 0.23f
            PlantStage.PLANTLET -> 0.29f
            else -> 0.34f
        }

    private fun crownRadius() = tree?.crown ?: 0.08f

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
            drawCircle(fruitRed, radius = 0.022f, center = bug)
            drawCircle(Color.Black, radius = 0.008f, center = bug.copy(y = bug.y - 0.018f))
            drawCircle(Color.Black, radius = 0.005f, center = bug.copy(x = bug.x - 0.008f))
            drawCircle(Color.Black, radius = 0.005f, center = bug.copy(x = bug.x + 0.008f))
        }
        if ("acc_bird" in accessories && isTree) {
            drawBird(Offset(center.x - radius * 0.3f, center.y - radius * 0.95f))
        }
        if ("acc_birdhouse" in accessories && isTree) {
            drawBirdhouse(Offset(base.x, (base.y + center.y) / 2))
        }
        if ("acc_swing" in accessories && isTree) {
            val branch = Offset(center.x + radius * 0.55f, center.y + radius * 0.35f)
            val seatY = base.y - 0.08f
            drawLine(trunkBrown, branch, Offset(branch.x - 0.03f, seatY), 0.006f)
            drawLine(
                trunkBrown,
                branch.copy(x = branch.x + 0.06f),
                Offset(branch.x + 0.09f, seatY),
                0.006f,
            )
            drawRoundRect(
                color = trunkBrown,
                topLeft = Offset(branch.x - 0.045f, seatY),
                size = Size(0.15f, 0.02f),
                cornerRadius = CornerRadius(0.006f),
            )
        }
        if ("acc_butterfly" in accessories) {
            drawButterfly(Offset(center.x - radius - 0.08f, center.y - 0.04f))
        }
    }

    private fun DrawScope.drawGroundAccessories() {
        if ("acc_watering_can" in accessories) {
            val can = Offset(0.8f, if (inPot) 0.92f else 0.86f)
            val color = Color(0xFF78909C)
            drawRoundRect(
                color = color,
                topLeft = Offset(can.x - 0.06f, can.y - 0.09f),
                size = Size(0.12f, 0.09f),
                cornerRadius = CornerRadius(0.015f),
            )
            drawLine(
                color,
                Offset(can.x - 0.05f, can.y - 0.06f),
                Offset(can.x - 0.12f, can.y - 0.12f),
                0.015f,
                StrokeCap.Round,
            )
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(can.x - 0.035f, can.y - 0.14f),
                size = Size(0.07f, 0.08f),
                style = Stroke(width = 0.01f),
            )
        }
        if ("acc_bird" in accessories && !isTree) {
            drawBird(Offset(0.24f, 0.62f))
        }
        if ("acc_birdhouse" in accessories && !isTree) {
            drawBirdhouse(Offset(0.82f, 0.56f))
        }
    }

    private fun DrawScope.drawBird(at: Offset) {
        val body = Color(0xFF42A5F5)
        drawOval(body, topLeft = Offset(at.x - 0.035f, at.y - 0.025f), size = Size(0.07f, 0.05f))
        drawCircle(body, radius = 0.02f, center = Offset(at.x + 0.03f, at.y - 0.025f))
        drawCircle(Color.Black, radius = 0.004f, center = Offset(at.x + 0.035f, at.y - 0.03f))
        val beak =
            Path().apply {
                moveTo(at.x + 0.048f, at.y - 0.03f)
                lineTo(at.x + 0.065f, at.y - 0.022f)
                lineTo(at.x + 0.048f, at.y - 0.018f)
                close()
            }
        drawPath(beak, Color(0xFFFFA000))
    }

    private fun DrawScope.drawBirdhouse(at: Offset) {
        val woodColor = Color(0xFFA1887F)
        drawRect(woodColor, topLeft = Offset(at.x - 0.04f, at.y - 0.05f), size = Size(0.08f, 0.07f))
        val roof =
            Path().apply {
                moveTo(at.x - 0.055f, at.y - 0.05f)
                lineTo(at.x, at.y - 0.09f)
                lineTo(at.x + 0.055f, at.y - 0.05f)
                close()
            }
        drawPath(roof, Color(0xFFD84315))
        drawCircle(soilBrown, radius = 0.013f, center = Offset(at.x, at.y - 0.02f))
    }

    private fun DrawScope.drawButterfly(at: Offset) {
        val wing = Color(0xFFFFB300)
        drawOval(wing, topLeft = Offset(at.x - 0.035f, at.y - 0.03f), size = Size(0.035f, 0.035f))
        drawOval(wing, topLeft = Offset(at.x, at.y - 0.03f), size = Size(0.035f, 0.035f))
        drawOval(wing, topLeft = Offset(at.x - 0.025f, at.y), size = Size(0.025f, 0.025f))
        drawOval(wing, topLeft = Offset(at.x, at.y), size = Size(0.025f, 0.025f))
        drawLine(
            Color.Black,
            Offset(at.x, at.y - 0.03f),
            Offset(at.x, at.y + 0.025f),
            0.006f,
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
