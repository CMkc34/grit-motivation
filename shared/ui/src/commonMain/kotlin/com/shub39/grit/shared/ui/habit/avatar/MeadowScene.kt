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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
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
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** The scene is twice as wide as high; the plant fills the square in the middle. */
const val MEADOW_ASPECT_RATIO = 2f

// seconds after which the motion of the decorations starts over
private const val LOOP_SECONDS = 60f

private val woodLight = Color(0xFFE2BC8E)
private val woodBench = Color(0xFFC98B4F)
private val metal = Color(0xFF4E5B52)
private val lanternGreen = Color(0xFF3E4F45)
private val lanternLight = Color(0xFFFFE79A)
private val grassDark = Color(0xFF6FB24F)
private val water = Color(0xFF6EC6E8)
private val waterLight = Color(0xFFA7DFF2)
private val pink = Color(0xFFFF8FA0)
private val pinkLight = Color(0xFFFFB3C1)

/**
 * The plant in the middle of a wide meadow with the decorations the user shows. Every decoration
 * has its own fixed spot left or right of the plant, chosen so that even the mighty tree with its
 * arms never covers one. Animals sleep with the plant when it is tired; the small movements follow
 * the system's animation setting like the plant's sway.
 */
@Composable
fun MeadowScene(
    stage: PlantStage,
    mood: PlantMood,
    modifier: Modifier = Modifier,
    potColor: String = ShopCatalog.DEFAULT_POT_COLOR,
    accessories: Set<String> = emptySet(),
    decorations: Set<String> = emptySet(),
    animate: Boolean = true,
) {
    val sway = if (animate) rememberSway() else 0f
    val time = if (animate) rememberMeadowTime() else 0f
    val tired = mood == PlantMood.TIRED
    val shown = decorationsInOrder.filter { it.id in decorations }

    Canvas(modifier = modifier.aspectRatio(MEADOW_ASPECT_RATIO)) {
        // everything below is drawn in a 2 x 1 rectangle
        scale(scale = size.height, pivot = Offset.Zero) {
            val painter = DecorationPainter(tired = tired, time = time, moving = animate && !tired)
            with(painter) { drawGround() }
            shown.filter { !it.inFront }.forEach { it.draw(this, painter) }
            translate(left = 0.5f) { drawPlantAvatar(stage, mood, potColor, accessories, sway) }
            shown.filter { it.inFront }.forEach { it.draw(this, painter) }
        }
    }
}

/** Seconds from 0 to [LOOP_SECONDS], then again. */
@Composable
private fun rememberMeadowTime(): Float {
    val transition = rememberInfiniteTransition(label = "meadow")
    val time by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = LOOP_SECONDS,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 60_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "time",
        )
    return time
}

// the meadow is the top of this ellipse, so it falls off towards both edges
private const val GROUND_Y = 1.07f
private const val GROUND_RX = 1.12f
private const val GROUND_RY = 0.22f

/** Height of the meadow's edge at [x]; decorations stand on it so none floats in the air. */
private fun groundY(x: Float): Float {
    val dx = (x - 1f) / GROUND_RX
    return GROUND_Y - GROUND_RY * sqrt(max(0f, 1f - dx * dx))
}

/** One decoration: [inFront] ones stand before the plant's hill, the others behind the plant. */
private class Decoration(
    val id: String,
    val inFront: Boolean,
    val draw: DrawScope.(DecorationPainter) -> Unit,
)

private fun ellipsePath(center: Offset, rx: Float, ry: Float) =
    Path().apply { addOval(Rect(center.x - rx, center.y - ry, center.x + rx, center.y + ry)) }

private fun roundRectPath(x: Float, y: Float, w: Float, h: Float, r: Float) =
    Path().apply { addRoundRect(RoundRect(Rect(Offset(x, y), Size(w, h)), CornerRadius(r))) }

/** Drawing helpers for the decorations, in the 2 x 1 rectangle of [MeadowScene]. */
private class DecorationPainter(val tired: Boolean, val time: Float, val moving: Boolean) {

    fun Color.mood(): Color = if (tired) lerp(this, tiredGray, 0.4f) else this

    /** A bump from 0 to 1 and back that lasts [length] seconds and comes every [every] seconds. */
    fun pulse(every: Float, length: Float, offset: Float = 0f): Float {
        if (!moving) return 0f
        val k = ((time + offset) % every) / length
        return if (k < 1f) sin(PI.toFloat() * k) else 0f
    }

    /** -1..1 with one full wave every [period] seconds; [period] divides [LOOP_SECONDS]. */
    fun wave(period: Float): Float = if (moving) sin(2 * PI.toFloat() * time / period) else 0f

    // sticker look like the plant: a dark outline under every shape
    fun DrawScope.sticker(path: Path, color: Color) {
        drawPath(
            path,
            outline,
            style = Stroke(width = OUTLINE * 2, join = StrokeJoin.Round, cap = StrokeCap.Round),
        )
        drawPath(path, color)
    }

    fun DrawScope.stalk(from: Offset, to: Offset, width: Float, color: Color) {
        drawLine(outline, from, to, width + OUTLINE * 2, StrokeCap.Round)
        drawLine(color, from, to, width, StrokeCap.Round)
    }

    fun DrawScope.eye(center: Offset, r: Float) {
        if (tired) {
            drawArc(
                color = outline,
                startAngle = 25f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(center.x - r * 1.1f, center.y - r * 1.4f),
                size = Size(r * 2.2f, r * 2.2f),
                style = Stroke(width = r * 0.7f, cap = StrokeCap.Round),
            )
        } else {
            drawCircle(outline, radius = r, center = center)
            drawCircle(
                Color.White,
                radius = r * 0.35f,
                center = Offset(center.x + r * 0.35f, center.y - r * 0.35f),
            )
        }
    }

    fun DrawScope.drawGround() {
        clipRect(left = 0f, top = 0f, right = MEADOW_ASPECT_RATIO, bottom = 1f) {
            sticker(ellipsePath(Offset(1f, GROUND_Y), GROUND_RX, GROUND_RY), meadowGreen.mood())
            // a few tufts so the wide meadow does not look empty
            listOf(
                    Offset(0.05f, 0.96f),
                    Offset(0.62f, 0.93f),
                    Offset(1.38f, 0.925f),
                    Offset(1.95f, 0.97f),
                    Offset(0.9f, 0.985f),
                )
                .forEach { tuft ->
                    for (side in -1..1) {
                        drawLine(
                            grassDark.mood(),
                            tuft,
                            Offset(tuft.x + side * 0.008f, tuft.y - 0.02f + (side * side) * 0.006f),
                            0.005f,
                            StrokeCap.Round,
                        )
                    }
                }
        }
    }
}

private val decorationsInOrder: List<Decoration> =
    listOf(
        Decoration("deco_fence", inFront = false) { fence(it) },
        Decoration("deco_hedgehog", inFront = false) { hedgehog(it) },
        Decoration("deco_bench", inFront = false) { bench(it) },
        Decoration("deco_lantern", inFront = false) { lantern(it) },
        Decoration("deco_mushrooms", inFront = true) { mushrooms(it) },
        Decoration("deco_snail", inFront = true) { snail(it) },
        Decoration("deco_rabbit", inFront = true) { rabbit(it) },
        Decoration("deco_pond", inFront = true) { pond(it) },
    )

private fun DrawScope.fence(p: DecorationPainter) {
    with(p) {
        // pickets stand upright, their feet and the rails follow the slope of the meadow
        val wood = woodLight.mood()
        val start = 0.03f
        val end = 0.36f
        listOf(-0.085f, -0.045f).forEach { above ->
            val rail =
                Path().apply {
                    moveTo(start, groundY(start) + above)
                    lineTo(end, groundY(end) + above)
                    lineTo(end, groundY(end) + above + 0.018f)
                    lineTo(start, groundY(start) + above + 0.018f)
                    close()
                }
            sticker(rail, wood)
        }
        for (i in 0 until 5) {
            val x = 0.045f + i * 0.07f
            val foot = groundY(x + 0.016f) + 0.006f
            val top = foot - 0.125f
            val picket =
                Path().apply {
                    moveTo(x, foot)
                    lineTo(x, top + 0.02f)
                    lineTo(x + 0.016f, top)
                    lineTo(x + 0.032f, top + 0.02f)
                    lineTo(x + 0.032f, foot)
                    close()
                }
            sticker(picket, wood)
        }
    }
}

private fun DrawScope.mushrooms(p: DecorationPainter) {
    with(p) {
        listOf(Triple(0.585f, 0.972f, 0.03f), Triple(0.64f, 0.982f, 0.022f)).forEach { (x, y, s) ->
            sticker(
                roundRectPath(x - s * 0.35f, y - s * 1.1f, s * 0.7f, s * 1.15f, s * 0.25f),
                Color(0xFFF3E3C3),
            )
            val cap =
                Path().apply {
                    moveTo(x - s, y - s * 0.95f)
                    quadraticTo(x - s, y - s * 2.1f, x, y - s * 2.1f)
                    quadraticTo(x + s, y - s * 2.1f, x + s, y - s * 0.95f)
                    quadraticTo(x, y - s * 1.15f, x - s, y - s * 0.95f)
                    close()
                }
            sticker(cap, Color(0xFFE5533D).mood())
            listOf(
                    Triple(-0.45f, -1.35f, 0.17f),
                    Triple(0.2f, -1.7f, 0.14f),
                    Triple(0.5f, -1.2f, 0.12f),
                )
                .forEach { (dx, dy, r) ->
                    drawCircle(Color.White, radius = r * s, center = Offset(x + dx * s, y + dy * s))
                }
        }
    }
}

private fun DrawScope.snail(p: DecorationPainter) {
    with(p) {
        val shell = Color(0xFFF2B84B).mood()
        val skin = Color(0xFFD8C7A0).mood()
        // crawls back and forth, turning at each end
        val x = 1.36f + 0.025f * wave(20f)
        val heading = if (moving && cos(2 * PI.toFloat() * time / 20f) < 0f) -1f else 1f
        translate(left = x, top = 0.955f) {
            scale(scaleX = heading, scaleY = 1f, pivot = Offset.Zero) {
                listOf(
                        Offset(0.038f, -0.04f) to Offset(0.03f, -0.068f),
                        Offset(0.047f, -0.038f) to Offset(0.055f, -0.064f),
                    )
                    .forEach { (from, to) ->
                        stalk(from, to, 0.004f, skin)
                        drawCircle(outline, radius = 0.006f, center = to)
                    }
                val body =
                    Path().apply {
                        moveTo(-0.045f, 0f)
                        quadraticTo(-0.05f, -0.018f, -0.02f, -0.02f)
                        lineTo(0.03f, -0.02f)
                        quadraticTo(0.045f, -0.045f, 0.052f, -0.03f)
                        quadraticTo(0.06f, -0.005f, 0.04f, 0f)
                        close()
                    }
                sticker(body, skin)
                sticker(ellipsePath(Offset(-0.005f, -0.04f), 0.03f, 0.028f), shell)
                val spiral =
                    Path().apply {
                        var a = 0f
                        while (a < 9f) {
                            val r = 0.004f + a * 0.0026f
                            val p = Offset(-0.005f + r * cos(a), -0.04f + r * sin(a))
                            if (a == 0f) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                            a += 0.25f
                        }
                    }
                drawPath(spiral, outline, style = Stroke(width = 0.005f, cap = StrokeCap.Round))
            }
        }
    }
}

private fun DrawScope.hedgehog(p: DecorationPainter) {
    with(p) {
        val x = 0.47f
        val y = groundY(x) - 0.001f
        val sniff = 0.006f * pulse(every = 2.6f, length = 0.5f)
        val spikes =
            Path().apply {
                moveTo(x - 0.055f, y)
                for (i in 0..10) {
                    val a = PI.toFloat() + i * PI.toFloat() / 10
                    val r = if (i % 2 == 1) 0.055f else 0.042f
                    lineTo(x - 0.005f + r * cos(a) * 1.05f, y + r * sin(a) * 0.9f)
                }
                close()
            }
        sticker(spikes, Color(0xFF7A5C4A).mood())
        val skin = Color(0xFFE8C9A0).mood()
        val face =
            Path().apply {
                moveTo(x + 0.02f, y - 0.03f)
                quadraticTo(x + 0.07f + sniff, y - 0.02f, x + 0.075f + sniff, y - 0.006f)
                quadraticTo(x + 0.06f, y + 0.004f, x + 0.02f, y)
                close()
            }
        sticker(face, skin)
        drawCircle(outline, radius = 0.007f, center = Offset(x + 0.077f + sniff, y - 0.009f))
        eye(Offset(x + 0.045f, y - 0.018f), 0.0055f)
        listOf(-0.03f, 0f).forEach {
            sticker(roundRectPath(x + it, y - 0.006f, 0.012f, 0.012f, 0.005f), skin)
        }
    }
}

private fun DrawScope.rabbit(p: DecorationPainter) {
    with(p) {
        val fur = Color(0xFFF4EFE7).mood()
        val hop = 0.03f * pulse(every = 5f, length = 0.45f)
        val twitch = 14f * pulse(every = 3f, length = 0.3f, offset = 1.2f)
        val x = 1.76f
        val y = groundY(x) - 0.001f - hop
        if (!tired) {
            drawOval(
                Color.Black.copy(alpha = 0.15f),
                topLeft = Offset(x - 0.045f + hop / 2, groundY(x) + 0.001f),
                size = Size(0.09f - hop, 0.016f),
            )
        }
        sticker(ellipsePath(Offset(x - 0.048f, y - 0.028f), 0.012f, 0.012f), Color.White)
        sticker(ellipsePath(Offset(x, y - 0.025f), 0.045f, 0.032f), fur)
        // the ears stand up and twitch, asleep they lie back
        listOf(-1f to 0f, 1f to twitch).forEach { (side, wiggle) ->
            val base = Offset(x + 0.035f + side * 0.009f, y - 0.072f)
            val degrees = if (tired) -100f - side * 8f else side * 10f - wiggle
            rotate(degrees = degrees, pivot = base) {
                val center = Offset(base.x, base.y - 0.036f)
                sticker(ellipsePath(center, 0.012f, 0.036f), fur)
                drawOval(
                    pinkLight,
                    topLeft = Offset(center.x - 0.005f, center.y - 0.024f),
                    size = Size(0.01f, 0.048f),
                )
            }
        }
        sticker(ellipsePath(Offset(x + 0.038f, y - 0.058f), 0.028f, 0.025f), fur)
        eye(Offset(x + 0.05f, y - 0.062f), 0.005f)
        drawCircle(pink, radius = 0.005f, center = Offset(x + 0.064f, y - 0.052f))
        drawOval(
            pink.copy(alpha = 0.6f),
            topLeft = Offset(x + 0.032f, y - 0.05f),
            size = Size(0.016f, 0.01f),
        )
        if (tired) {
            // a small "z" above the sleeping head
            listOf(Offset(x + 0.075f, y - 0.11f) to 0.02f, Offset(x + 0.1f, y - 0.135f) to 0.014f)
                .forEach { (at, s) ->
                    val z =
                        Path().apply {
                            moveTo(at.x, at.y)
                            lineTo(at.x + s, at.y)
                            lineTo(at.x, at.y + s)
                            lineTo(at.x + s, at.y + s)
                        }
                    drawPath(
                        z,
                        outline,
                        style =
                            Stroke(
                                width = s * 0.25f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                    )
                }
        }
    }
}

private fun DrawScope.bench(p: DecorationPainter) {
    with(p) {
        val wood = woodBench.mood()
        val x = 1.44f
        // the bench leans with the meadow so all four feet touch the ground
        val left = x + 0.028f
        val right = x + 0.248f
        val degrees = atan2(groundY(right) - groundY(left), right - left) * 180f / PI.toFloat()
        translate(left = left, top = groundY(left) + 0.004f) {
            rotate(degrees = degrees, pivot = Offset.Zero) {
                // drawn with its feet at y = 0.875
                translate(left = -left, top = -0.875f) {
                    listOf(x + 0.025f, x + 0.235f).forEach {
                        sticker(roundRectPath(it, 0.71f, 0.016f, 0.165f, 0.006f), metal)
                    }
                    sticker(roundRectPath(x, 0.72f, 0.275f, 0.024f, 0.008f), wood)
                    sticker(roundRectPath(x, 0.752f, 0.275f, 0.024f, 0.008f), wood)
                    sticker(roundRectPath(x - 0.01f, 0.8f, 0.295f, 0.024f, 0.008f), wood)
                    listOf(x + 0.02f, x + 0.24f).forEach {
                        sticker(roundRectPath(it, 0.815f, 0.016f, 0.06f, 0.006f), metal)
                    }
                }
            }
        }
    }
}

private fun DrawScope.pond(p: DecorationPainter) {
    with(p) {
        // reeds behind the water
        listOf(Triple(0.14f, 0.93f, 0.11f), Triple(0.165f, 0.935f, 0.085f)).forEach { (x, y, h) ->
            stalk(Offset(x, y), Offset(x + 0.004f, y - h), 0.006f, Color(0xFF3FA34D).mood())
            sticker(
                roundRectPath(x - 0.006f, y - h - 0.005f, 0.016f, 0.04f, 0.008f),
                Color(0xFF8A5A3B).mood(),
            )
        }
        sticker(ellipsePath(Offset(0.3f, 0.945f), 0.2f, 0.048f), water.mood())
        drawOval(waterLight.mood(), topLeft = Offset(0.14f, 0.915f), size = Size(0.24f, 0.044f))
        if (moving) {
            // a ripple that grows and fades every three seconds
            val k = (time % 3f) / 3f
            drawOval(
                Color.White.copy(alpha = 0.7f * (1 - k)),
                topLeft = Offset(0.36f - 0.015f - 0.05f * k, 0.955f - 0.005f - 0.016f * k),
                size = Size(2 * (0.015f + 0.05f * k), 2 * (0.005f + 0.016f * k)),
                style = Stroke(width = 0.004f),
            )
        }
        val pad =
            Path().apply {
                moveTo(0.22f, 0.95f)
                arcTo(Rect(Offset(0.22f, 0.95f), 0.028f), 20f, 320f, false)
                close()
            }
        scale(scaleX = 1f, scaleY = 0.45f, pivot = Offset(0.22f, 0.95f)) {
            sticker(pad, Color(0xFF58B368).mood())
        }
        val flower = Offset(0.226f, 0.943f)
        for (i in 0 until 5) {
            val a = i * 72f * PI.toFloat() / 180f
            drawCircle(
                Color(0xFFFFC2D1),
                radius = 0.007f,
                center = Offset(flower.x + 0.008f * sin(a), flower.y - 0.008f * cos(a)),
            )
        }
        drawCircle(Color(0xFFFFD45C), radius = 0.005f, center = flower)
    }
}

private fun DrawScope.lantern(p: DecorationPainter) {
    with(p) {
        val x = 1.94f
        // drawn with its foot at y = 0.885, then moved onto the meadow
        translate(top = groundY(x) - 0.885f) {
            val glow = if (moving) 0.75f + 0.25f * sin(time * 7f) * sin(time * 2.3f) else 0.9f
            drawCircle(
                Brush.radialGradient(
                    colors =
                        listOf(
                            lanternLight.copy(alpha = 0.75f * glow),
                            lanternLight.copy(alpha = 0f),
                        ),
                    center = Offset(x, 0.555f),
                    radius = 0.09f,
                ),
                radius = 0.09f,
                center = Offset(x, 0.555f),
            )
            sticker(roundRectPath(x - 0.008f, 0.58f, 0.016f, 0.3f, 0.006f), lanternGreen)
            sticker(roundRectPath(x - 0.022f, 0.875f, 0.044f, 0.016f, 0.006f), lanternGreen)
            val head =
                Path().apply {
                    moveTo(x - 0.026f, 0.585f)
                    lineTo(x - 0.032f, 0.53f)
                    lineTo(x + 0.032f, 0.53f)
                    lineTo(x + 0.026f, 0.585f)
                    close()
                }
            sticker(head, lanternLight)
            val roof =
                Path().apply {
                    moveTo(x - 0.045f, 0.532f)
                    lineTo(x, 0.5f)
                    lineTo(x + 0.045f, 0.532f)
                    close()
                }
            sticker(roof, lanternGreen)
            drawLine(lanternGreen, Offset(x, 0.53f), Offset(x, 0.585f), 0.004f)
        }
    }
}

@PreviewWrapper(GritPreviewWrapper::class)
@Preview(widthDp = 400, heightDp = 620)
@Composable
private fun MeadowScenePreview() {
    val all = ShopCatalog.items.filter { it.kind == ShopItemKind.DECORATION }.map { it.id }.toSet()
    Column {
        listOf(
                PlantStage.SEEDLING to PlantMood.HAPPY,
                PlantStage.TREE to PlantMood.HAPPY,
                PlantStage.MIGHTY_TREE to PlantMood.TIRED,
            )
            .forEach { (stage, mood) ->
                MeadowScene(
                    stage = stage,
                    mood = mood,
                    decorations = all,
                    animate = false,
                    modifier = Modifier.width(400.dp),
                )
            }
    }
}
