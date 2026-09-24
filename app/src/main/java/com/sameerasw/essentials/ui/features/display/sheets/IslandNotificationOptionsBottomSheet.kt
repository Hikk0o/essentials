/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandNotificationOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring Island notification options.
 */

package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandNotificationOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
    highlightSetting: String? = null,
) {
    val view = LocalView.current

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.island_notification_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                AnimatedVisibility(
                    visible = viewModel.isIslandLineStageEnabled.value,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_notification_sound_24,
                        title = stringResource(R.string.island_notif_compact_heads_up_title),
                        description = stringResource(R.string.island_notif_compact_heads_up_desc),
                        isChecked = viewModel.isIslandNotifCompactHeadsUp.value,
                        onCheckedChange = { checked ->
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.setIslandNotifCompactHeadsUp(checked)
                        },
                        modifier = Modifier.highlight(highlightSetting == "island_notif_compact_heads_up"),
                    )
                }

                IconToggleItem(
                    iconRes = R.drawable.rounded_downloading_24,
                    title = stringResource(R.string.island_notif_keep_progress_title),
                    isChecked = viewModel.isIslandNotifKeepProgress.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandNotifKeepProgress(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_notif_keep_progress"),
                )

                IconToggleItem(
                    iconRes = R.drawable.outline_circle_notifications_24,
                    title = stringResource(R.string.island_notif_queue_title),
                    isChecked = viewModel.isIslandNotifQueue.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandNotifQueue(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_notif_queue"),
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_touch_app_24,
                    title = stringResource(R.string.island_notif_tap_to_open_title),
                    isChecked = viewModel.isIslandNotifTapToOpen.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandNotifTapToOpen(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_notif_tap_to_open"),
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_notifications_unread_24,
                    title = stringResource(R.string.island_catch_up_title),
                    description = stringResource(R.string.island_catch_up_desc),
                    isChecked = viewModel.isIslandCatchUpEnabled.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandCatchUpEnabled(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_catch_up"),
                )

                AnimatedVisibility(
                    visible = viewModel.isIslandCatchUpEnabled.value,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    ConfigSliderItem(
                        title = stringResource(R.string.island_catch_up_timeout_title),
                        value = (viewModel.islandCatchUpTimeoutMs.longValue / 1000f),
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setIslandCatchUpTimeoutMs((it * 1000).toLong())
                        },
                        valueRange = 5f..60f,
                        increment = 5f,
                        iconRes = R.drawable.rounded_timer_24,
                        valueFormatter = { "${it.toInt()}s" },
                        modifier = Modifier.highlight(highlightSetting == "island_catch_up_timeout"),
                    )
                }
            }
        }
    }
}
