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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.shub39.grit.core.activity.ActivityPlanner
import com.shub39.grit.core.avatar.AvatarData
import com.shub39.grit.core.avatar.AvatarState
import com.shub39.grit.core.avatar.PlantMood
import com.shub39.grit.core.avatar.PlantStage
import com.shub39.grit.core.avatar.PlantState
import com.shub39.grit.core.avatar.Purchase
import com.shub39.grit.core.avatar.PurchaseCheck
import com.shub39.grit.core.avatar.ShopCatalog
import com.shub39.grit.core.avatar.ShopItem
import com.shub39.grit.core.avatar.ShopItemKind
import com.shub39.grit.shared.ui.GritPreviewWrapper
import com.shub39.grit.shared.ui.components.GritDialog
import com.shub39.grit.shared.ui.theme.flexFontRounded
import grit.shared.ui.generated.resources.Res
import grit.shared.ui.generated.resources.avatar_cancel
import grit.shared.ui.generated.resources.shop_accessories
import grit.shared.ui.generated.resources.shop_balance
import grit.shared.ui.generated.resources.shop_buy
import grit.shared.ui.generated.resources.shop_buy_for
import grit.shared.ui.generated.resources.shop_choose
import grit.shared.ui.generated.resources.shop_chosen
import grit.shared.ui.generated.resources.shop_confirm_text
import grit.shared.ui.generated.resources.shop_confirm_title
import grit.shared.ui.generated.resources.shop_decorations
import grit.shared.ui.generated.resources.shop_from_stage
import grit.shared.ui.generated.resources.shop_item_acc_bird
import grit.shared.ui.generated.resources.shop_item_acc_birdhouse
import grit.shared.ui.generated.resources.shop_item_acc_blossoms
import grit.shared.ui.generated.resources.shop_item_acc_butterfly
import grit.shared.ui.generated.resources.shop_item_acc_fairy_lights
import grit.shared.ui.generated.resources.shop_item_acc_fruits
import grit.shared.ui.generated.resources.shop_item_acc_ladybug
import grit.shared.ui.generated.resources.shop_item_acc_swing
import grit.shared.ui.generated.resources.shop_item_acc_watering_can
import grit.shared.ui.generated.resources.shop_item_deco_bench
import grit.shared.ui.generated.resources.shop_item_deco_fence
import grit.shared.ui.generated.resources.shop_item_deco_hedgehog
import grit.shared.ui.generated.resources.shop_item_deco_lantern
import grit.shared.ui.generated.resources.shop_item_deco_mushrooms
import grit.shared.ui.generated.resources.shop_item_deco_pond
import grit.shared.ui.generated.resources.shop_item_deco_rabbit
import grit.shared.ui.generated.resources.shop_item_deco_snail
import grit.shared.ui.generated.resources.shop_item_pot_blue
import grit.shared.ui.generated.resources.shop_item_pot_gold
import grit.shared.ui.generated.resources.shop_item_pot_green
import grit.shared.ui.generated.resources.shop_item_pot_purple
import grit.shared.ui.generated.resources.shop_item_pot_terracotta
import grit.shared.ui.generated.resources.shop_item_pot_yellow
import grit.shared.ui.generated.resources.shop_low_energy
import grit.shared.ui.generated.resources.shop_pot_colors
import grit.shared.ui.generated.resources.shop_put_away
import grit.shared.ui.generated.resources.shop_put_on
import grit.shared.ui.generated.resources.shop_set_up
import grit.shared.ui.generated.resources.shop_take_off
import grit.shared.ui.generated.resources.shop_title
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private fun ShopItem.nameRes(): StringResource? =
    when (id) {
        "pot_terracotta" -> Res.string.shop_item_pot_terracotta
        "pot_blue" -> Res.string.shop_item_pot_blue
        "pot_yellow" -> Res.string.shop_item_pot_yellow
        "pot_green" -> Res.string.shop_item_pot_green
        "pot_purple" -> Res.string.shop_item_pot_purple
        "pot_gold" -> Res.string.shop_item_pot_gold
        "acc_watering_can" -> Res.string.shop_item_acc_watering_can
        "acc_ladybug" -> Res.string.shop_item_acc_ladybug
        "acc_butterfly" -> Res.string.shop_item_acc_butterfly
        "acc_blossoms" -> Res.string.shop_item_acc_blossoms
        "acc_bird" -> Res.string.shop_item_acc_bird
        "acc_birdhouse" -> Res.string.shop_item_acc_birdhouse
        "acc_fruits" -> Res.string.shop_item_acc_fruits
        "acc_fairy_lights" -> Res.string.shop_item_acc_fairy_lights
        "acc_swing" -> Res.string.shop_item_acc_swing
        "deco_mushrooms" -> Res.string.shop_item_deco_mushrooms
        "deco_fence" -> Res.string.shop_item_deco_fence
        "deco_snail" -> Res.string.shop_item_deco_snail
        "deco_hedgehog" -> Res.string.shop_item_deco_hedgehog
        "deco_rabbit" -> Res.string.shop_item_deco_rabbit
        "deco_bench" -> Res.string.shop_item_deco_bench
        "deco_pond" -> Res.string.shop_item_deco_pond
        "deco_lantern" -> Res.string.shop_item_deco_lantern
        else -> null
    }

@Composable
private fun itemName(item: ShopItem): String = item.nameRes()?.let { stringResource(it) } ?: item.id

private fun ShopItem.emoji(): String =
    when (id) {
        "acc_watering_can" -> "💧"
        "acc_ladybug" -> "🐞"
        "acc_butterfly" -> "🦋"
        "acc_blossoms" -> "🌸"
        "acc_bird" -> "🐦"
        "acc_birdhouse" -> "🏠"
        "acc_fruits" -> "🍎"
        "acc_fairy_lights" -> "✨"
        "acc_swing" -> "🪢"
        "deco_mushrooms" -> "🍄"
        "deco_fence" -> "🪵"
        "deco_snail" -> "🐌"
        "deco_hedgehog" -> "🦔"
        "deco_rabbit" -> "🐇"
        "deco_bench" -> "🪑"
        "deco_pond" -> "🪷"
        "deco_lantern" -> "🏮"
        else -> "🎁"
    }

/**
 * Pot colors, accessories and meadow decorations bought with energy. [onBuyClick] opens the
 * confirmation, [onWear] puts an owned item on or takes it off.
 */
@Composable
fun AvatarShop(
    avatar: AvatarState,
    onBuyClick: (ShopItem) -> Unit,
    onWear: (ShopItem, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.widthIn(max = 500.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.shop_title),
                fontFamily = flexFontRounded(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.shop_balance, avatar.energy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ShopKind.entries.forEach { kind ->
                Text(
                    text = stringResource(kind.title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                ShopCatalog.items
                    .filter { it.kind == kind.kind }
                    .forEach { item ->
                        ShopRow(
                            item = item,
                            check = avatar.check(item),
                            worn = avatar.isWorn(item),
                            onBuyClick = { onBuyClick(item) },
                            onWear = { onWear(item, it) },
                        )
                    }
            }
        }
    }
}

private enum class ShopKind(val kind: ShopItemKind, val title: StringResource) {
    POTS(ShopItemKind.POT_COLOR, Res.string.shop_pot_colors),
    ACCESSORIES(ShopItemKind.ACCESSORY, Res.string.shop_accessories),
    DECORATIONS(ShopItemKind.DECORATION, Res.string.shop_decorations),
}

@Composable
private fun ShopRow(
    item: ShopItem,
    check: PurchaseCheck,
    worn: Boolean,
    onBuyClick: () -> Unit,
    onWear: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ItemIcon(item)
        Text(
            text = itemName(item),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        when (check) {
            PurchaseCheck.ALREADY_OWNED ->
                when {
                    item.kind == ShopItemKind.POT_COLOR && worn ->
                        OutlinedButton(onClick = {}, enabled = false) {
                            Text(stringResource(Res.string.shop_chosen))
                        }
                    item.kind == ShopItemKind.POT_COLOR ->
                        FilledTonalButton(onClick = { onWear(true) }) {
                            Text(stringResource(Res.string.shop_choose))
                        }
                    item.kind == ShopItemKind.DECORATION && worn ->
                        OutlinedButton(onClick = { onWear(false) }) {
                            Text(stringResource(Res.string.shop_put_away))
                        }
                    item.kind == ShopItemKind.DECORATION ->
                        FilledTonalButton(onClick = { onWear(true) }) {
                            Text(stringResource(Res.string.shop_set_up))
                        }
                    worn ->
                        OutlinedButton(onClick = { onWear(false) }) {
                            Text(stringResource(Res.string.shop_take_off))
                        }
                    else ->
                        FilledTonalButton(onClick = { onWear(true) }) {
                            Text(stringResource(Res.string.shop_put_on))
                        }
                }
            PurchaseCheck.STAGE_TOO_LOW ->
                Text(
                    text = "🔒 " + stringResource(Res.string.shop_from_stage, item.minStage.number),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            PurchaseCheck.NOT_ENOUGH_ENERGY ->
                Button(onClick = {}, enabled = false) {
                    Text(stringResource(Res.string.shop_buy_for, item.price))
                }
            PurchaseCheck.OK ->
                Button(onClick = onBuyClick) {
                    Text(stringResource(Res.string.shop_buy_for, item.price))
                }
        }
    }
}

@Composable
private fun ItemIcon(item: ShopItem) {
    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        when (item.kind) {
            ShopItemKind.POT_COLOR ->
                Box(
                    modifier =
                        Modifier.size(24.dp).background(potColorOf(item.id), shape = CircleShape)
                )
            ShopItemKind.ACCESSORY,
            ShopItemKind.DECORATION ->
                Text(text = item.emoji(), style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Confirms a purchase and shows the plant with [item] on. Warns when less than one freeze worth of
 * energy would be left.
 */
@Composable
fun BuyItemDialog(
    avatar: AvatarState,
    item: ShopItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val balanceAfter = avatar.energy - item.price
    val preview = avatar.tryOn(item)

    GritDialog(onDismissRequest = onDismiss) {
        if (item.kind == ShopItemKind.DECORATION) {
            // a decoration is shown where it will stand on the meadow
            MeadowScene(
                stage = avatar.plant.stage,
                mood = avatar.plant.mood,
                potColor = preview.potColor,
                accessories = preview.accessories,
                decorations = preview.decorations,
                weather = avatar.plant.weather,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PlantAvatar(
                stage = avatar.plant.stage,
                mood = avatar.plant.mood,
                potColor = preview.potColor,
                accessories = preview.accessories,
                modifier = Modifier.widthIn(max = 180.dp).fillMaxWidth(),
            )
        }
        Text(
            text = stringResource(Res.string.shop_confirm_title, itemName(item)),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.shop_confirm_text, item.price, balanceAfter),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (balanceAfter < ActivityPlanner.FREEZE_COST) {
            Text(
                text = stringResource(Res.string.shop_low_energy, ActivityPlanner.FREEZE_COST),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.avatar_cancel)) }
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.shop_buy)) }
        }
    }
}

private val previewAvatar =
    AvatarState(
        plant = PlantState(activeDays = 32, stage = PlantStage.YOUNG_PLANT, mood = PlantMood.HAPPY),
        energy = 30,
        data =
            AvatarData(
                purchases =
                    listOf(
                        Purchase("pot_blue", LocalDate(2026, 10, 1), 10),
                        Purchase("acc_ladybug", LocalDate(2026, 10, 2), 15),
                    ),
                potColor = "pot_blue",
                accessories = setOf("acc_ladybug"),
            ),
    )

@PreviewWrapper(GritPreviewWrapper::class)
@Preview
@Composable
private fun AvatarShopPreview() {
    AvatarShop(avatar = previewAvatar, onBuyClick = {}, onWear = { _, _ -> })
}

@PreviewWrapper(GritPreviewWrapper::class)
@Preview
@Composable
private fun BuyItemDialogPreview() {
    BuyItemDialog(
        avatar = previewAvatar,
        item = ShopCatalog.byId("acc_blossoms")!!,
        onConfirm = {},
        onDismiss = {},
    )
}
