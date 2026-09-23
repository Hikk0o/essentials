/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandDevicesBatteryBottomSheet.kt
 * Description: Bottom sheet for picking and ordering Bluetooth devices shown as the Island battery complication.
 */

package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.BluetoothPairedDevicesUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandDevicesBatteryBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    val paired = remember { BluetoothPairedDevicesUtil.getPairedDevices(context) }
    val names = remember(paired) { paired.associate { it.address to it.name } }
    val order = viewModel.islandDevicesBatteryOrder.value.filter { it in names }
    val unselected = paired.filterNot { it.address in order }.sortedBy { it.name.lowercase() }

    fun save(list: List<String>) {
        HapticUtil.performVirtualKeyHaptic(view)
        viewModel.setIslandDevicesBatteryOrder(list)
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        val current = viewModel.islandDevicesBatteryOrder.value.filter { it in names }
        val fromIndex = current.indexOf(fromKey)
        val toIndex = current.indexOf(toKey)
        if (fromIndex < 0 || toIndex < 0) return@rememberReorderableLazyListState
        viewModel.setIslandDevicesBatteryOrder(current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) })
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.island_devices_battery_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )
            Text(
                text = stringResource(R.string.island_devices_battery_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item(key = "options") {
                    Column(Modifier.padding(bottom = 16.dp)) {
                        IslandBatteryOptions(
                            viewModel = viewModel,
                            onlyLow = viewModel.isIslandDevicesBatteryOnlyLow.value,
                            onOnlyLowChange = viewModel::setIslandDevicesBatteryOnlyLow,
                        )
                    }
                }

                item(key = "devices_header") {
                    Text(
                        text = stringResource(R.string.island_devices_battery_section),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    )
                }

                if (paired.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.island_devices_battery_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                itemsIndexed(order, key = { _, address -> address }) { index, address ->
                    ReorderableItem(reorderState, key = address) { isDragging ->
                        DeviceRow(
                            name = names[address].orEmpty(),
                            checked = true,
                            shape = groupShape(index, order.size),
                            dragging = isDragging,
                            onCheckedChange = { save(order - address) },
                            dragHandle = {
                                IconButton(
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = { hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                                        onDragStopped = { hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd) },
                                    ),
                                    onClick = {},
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_drag_handle_24),
                                        contentDescription = stringResource(R.string.content_desc_drag_reorder),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                        )
                    }
                }

                if (order.isNotEmpty() && unselected.isNotEmpty()) {
                    item(key = "gap") { Spacer(Modifier.height(16.dp)) }
                }

                itemsIndexed(unselected, key = { _, device -> device.address }) { index, device ->
                    DeviceRow(
                        name = device.name,
                        checked = false,
                        shape = groupShape(index, unselected.size),
                        dragging = false,
                        onCheckedChange = { save(order + device.address) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    name: String,
    checked: Boolean,
    shape: Shape,
    dragging: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (dragging) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceBright)
            .clickable(onClick = onCheckedChange)
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_bluetooth_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        dragHandle?.invoke()
        Switch(checked = checked, onCheckedChange = { onCheckedChange() })
    }
}

private fun groupShape(index: Int, size: Int): RoundedCornerShape {
    val outer = 24.dp
    val inner = 4.dp
    val top = if (index == 0) outer else inner
    val bottom = if (index == size - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}
