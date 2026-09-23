package com.sameerasw.essentials.island.plugins.devices

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.components.BatteryGlyph
import com.sameerasw.essentials.island.ui.components.BatteryRing
import com.sameerasw.essentials.island.ui.components.IslandIcon

class DevicesPlugin : BaseIslandPlugin() {
    override val id = "devices"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_DEVICES,
        SettingsRepository.KEY_ISLAND_LINE_STAGE_ENABLED,
        SettingsRepository.KEY_ISLAND_BATTERY_STYLE,
    )

    private class DeviceEvent(val address: String, val connected: Boolean, val name: String, val iconRes: Int, val battery: Int)

    private var event: DeviceEvent? = null
    private var registered = false
    private val clearEvent = Runnable {
        event = null
        render()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val device = intent?.deviceExtra() ?: return
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> announce(device, connected = true)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> announce(device, connected = false)
                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val current = event ?: return
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (current.address != device.address || !current.connected || level < 0) return
                    event = DeviceEvent(current.address, true, current.name, current.iconRes, level)
                    render()
                }
            }
        }
    }

    override fun onStart() {
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(ACTION_BATTERY_LEVEL_CHANGED)
            }
            context.registerReceiver(receiver, filter)
            registered = true
        } catch (_: Exception) {
        }
    }

    override fun onStop() {
        ctx?.mainHandler?.removeCallbacks(clearEvent)
        event = null
        if (registered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
            registered = false
        }
    }

    override fun refresh() = render()

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun announce(device: BluetoothDevice, connected: Boolean) {
        val c = ctx ?: return
        if (!settings.isIslandShowDevicesEnabled() || !settings.isIslandLineStageEnabled() || !hasPermission()) return
        val name = try {
            device.alias ?: device.name
        } catch (_: SecurityException) {
            null
        } ?: context.getString(R.string.island_devices_unknown)
        val battery = if (connected) batteryOf(device) else -1
        event = DeviceEvent(device.address, connected, name, iconFor(device, name), battery)
        val duration = settings.getIslandPeekDurationMs()
        render()
        c.request(PluginRequest.Peek(ITEM_KEY, duration))
        c.mainHandler.removeCallbacks(clearEvent)
        c.mainHandler.postDelayed(clearEvent, duration + 500L)
    }

    private fun batteryOf(device: BluetoothDevice): Int = try {
        device.javaClass.getMethod("getBatteryLevel").invoke(device) as Int
    } catch (_: Exception) {
        -1
    }

    @SuppressLint("MissingPermission")
    private fun iconFor(device: BluetoothDevice, name: String): Int {
        val major = try {
            device.bluetoothClass?.majorDeviceClass
        } catch (_: SecurityException) {
            null
        }
        return when {
            name.contains("watch", true) || name.contains("gear", true) || name.contains("fit", true) ||
                major == BluetoothClass.Device.Major.WEARABLE -> R.drawable.rounded_watch_24
            name.contains("bud", true) || name.contains("pod", true) || name.contains("head", true) ||
                name.contains("audio", true) || name.contains("sound", true) ||
                major == BluetoothClass.Device.Major.AUDIO_VIDEO -> R.drawable.rounded_headphones_24
            name.contains("keyboard", true) -> R.drawable.rounded_keyboard_24
            major == BluetoothClass.Device.Major.COMPUTER -> R.drawable.rounded_laptop_mac_24
            else -> R.drawable.rounded_bluetooth_24
        }
    }

    private fun render() {
        val e = event
        if (ctx == null || e == null || !settings.isIslandShowDevicesEnabled()) {
            publish(null)
            return
        }
        val tint = if (e.connected) null else Color.White.copy(alpha = 0.6f)
        val battery = e.battery
        val iconStyle = settings.getIslandBatteryStyle() == SettingsRepository.ISLAND_BATTERY_STYLE_ICON
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.DEVICES,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("devices.icon") { IslandIcon(e.iconRes, size = 18.dp, tint = tint ?: MaterialTheme.colorScheme.primary) },
                ),
                line = LineContent(
                    icon = { IslandIcon(e.iconRes, tint = tint ?: MaterialTheme.colorScheme.primary) },
                    start = e.name,
                    end = context.getString(if (e.connected) R.string.island_devices_connected else R.string.island_devices_disconnected),
                    endSlot = if (battery >= 0) {
                        {
                            if (iconStyle) {
                                BatteryGlyph(battery, MaterialTheme.colorScheme.primary, showLevel = true)
                            } else {
                                BatteryRing(battery, MaterialTheme.colorScheme.primary, showLevel = true)
                            }
                        }
                    } else {
                        null
                    },
                ),
            ),
        )
    }

    @Suppress("DEPRECATION")
    private fun Intent.deviceExtra(): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

    companion object {
        const val ITEM_KEY = "devices"
        private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }
}
