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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.shub39.grit.core.avatar.AvatarData
import com.shub39.grit.core.avatar.AvatarState
import com.shub39.grit.core.avatar.PlantMood
import com.shub39.grit.core.avatar.PlantStage
import com.shub39.grit.core.avatar.PlantState
import com.shub39.grit.core.avatar.Purchase
import com.shub39.grit.core.avatar.ShopItem
import com.shub39.grit.shared.ui.GritPreviewWrapper
import com.shub39.grit.shared.ui.LocalWindowSizeClass
import com.shub39.grit.shared.ui.WindowSize.Companion.isExpanded
import com.shub39.grit.shared.ui.habit.HabitsAction
import com.shub39.grit.shared.ui.habit.ui.sections.EnergyBalance
import com.shub39.grit.shared.ui.theme.flexFontEmphasis
import grit.shared.ui.generated.resources.Res
import grit.shared.ui.generated.resources.avatar_tab
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * Own tab for the plant: energy balance on top (so the shop shows what can be spent), then the
 * plant with its name and progress, then the shop. The meadow around the plant can take decorations
 * later (#82).
 */
@Composable
fun AvatarPage(
    avatar: AvatarState?,
    onAction: (HabitsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    var showRename by remember { mutableStateOf(false) }
    var itemToBuy by remember { mutableStateOf<ShopItem?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            colors =
                TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = Color.Transparent,
                    containerColor = Color.Transparent,
                ),
            title = {
                Text(text = stringResource(Res.string.avatar_tab), fontFamily = flexFontEmphasis())
            },
            windowInsets =
                if (windowSizeClass.isExpanded()) {
                    WindowInsets(0)
                } else {
                    TopAppBarDefaults.windowInsets
                },
        )

        if (avatar == null) return@Column

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { EnergyBalance(energy = avatar.energy) }

            item { AvatarCard(avatar = avatar, onRenameClick = { showRename = true }) }

            item {
                AvatarShop(
                    avatar = avatar,
                    onBuyClick = { itemToBuy = it },
                    onWear = { item, worn -> onAction(HabitsAction.WearShopItem(item.id, worn)) },
                )
            }
        }
    }

    if (avatar != null && showRename) {
        RenameAvatarDialog(
            currentName = avatar.data.name,
            onSave = {
                onAction(HabitsAction.RenameAvatar(it))
                showRename = false
            },
            onDismiss = { showRename = false },
        )
    }

    val buying = itemToBuy
    if (avatar != null && buying != null) {
        BuyItemDialog(
            avatar = avatar,
            item = buying,
            onConfirm = {
                onAction(HabitsAction.BuyShopItem(buying.id))
                itemToBuy = null
            },
            onDismiss = { itemToBuy = null },
        )
    }

    avatar?.newStage?.let { stage ->
        NewStageDialog(
            avatar = avatar,
            stage = stage,
            onDismiss = { onAction(HabitsAction.DismissNewStage) },
        )
    }
}

@PreviewWrapper(GritPreviewWrapper::class)
@Preview
@Composable
private fun AvatarPageFreshInstallPreview() {
    AvatarPage(
        avatar =
            AvatarState(
                plant = PlantState(activeDays = 0, stage = PlantStage.SEED, mood = PlantMood.TIRED),
                energy = 0,
                data = AvatarData(),
            ),
        onAction = {},
    )
}

@PreviewWrapper(GritPreviewWrapper::class)
@Preview
@Composable
private fun AvatarPageWithPurchasesPreview() {
    AvatarPage(
        avatar =
            AvatarState(
                plant =
                    PlantState(
                        activeDays = 32,
                        stage = PlantStage.YOUNG_PLANT,
                        mood = PlantMood.HAPPY,
                    ),
                energy = 30,
                data =
                    AvatarData(
                        name = "Fridolin",
                        purchases =
                            listOf(
                                Purchase("pot_blue", LocalDate(2026, 10, 1), 10),
                                Purchase("acc_ladybug", LocalDate(2026, 10, 2), 15),
                            ),
                        potColor = "pot_blue",
                        accessories = setOf("acc_ladybug"),
                    ),
            ),
        onAction = {},
    )
}
