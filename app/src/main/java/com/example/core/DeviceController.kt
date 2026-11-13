package com.example.core

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.ContactsContract
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val label: String,
    val packageName: String
)

data class ContactItem(
    val name: String,
    val phoneNumber: String
)

data class DeviceTelemetry(
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val ramAvailableMb: Long,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val batteryTempC: Float,
    val mediaVolume: Int,
    val mediaMaxVolume: Int,
    val ringVolume: Int,
    val ringMaxVolume: Int,
    val brightnessPercent: Int,
    val isMusicActive: Boolean,
    val deviceModel: String
)

data class PermissionSnapshot(
    val recordAudio: Boolean,
    val readContacts: Boolean,
    val callPhone: Boolean,
    val camera: Boolean,
    val postNotifications: Boolean,
    val writeSettings: Boolean,
    val accessibilityEnabled: Boolean,
    val notificationListenerEnabled: Boolean,
    val ignoringBatteryOptimizations: Boolean
)

class DeviceController(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun getPermissionSnapshot(): PermissionSnapshot {
        val notifPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPerm(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
        val notifListener = NotificationManagerCompat.getEnabledListenerPackages(appContext)
            .contains(appContext.packageName)
        return PermissionSnapshot(
            recordAudio = hasPerm(Manifest.permission.RECORD_AUDIO),
            readContacts = hasPerm(Manifest.permission.READ_CONTACTS),
            callPhone = hasPerm(Manifest.permission.CALL_PHONE),
            camera = hasPerm(Manifest.permission.CAMERA),
            postNotifications = notifPost,
            writeSettings = Settings.System.canWrite(appContext),
            accessibilityEnabled = GwenAccessibilityService.isConnected.value,
            notificationListenerEnabled = notifListener,
            ignoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(appContext.packageName)
        )
    }

    private fun hasPerm(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun getDeviceTelemetry(): DeviceTelemetry {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024L * 1024L)
        val availMb = memInfo.availMem / (1024L * 1024L)
        val usedMb = (totalMb - availMb).coerceAtLeast(0L)

        val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 85
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 310) ?: 310

        val mediaVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val mediaMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val ringVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val ringMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)

        val brightnessRaw = try {
            Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Exception) {
            128
        }
        val brightnessPct = ((brightnessRaw / 255f) * 100f).toInt().coerceIn(5, 100)

        return DeviceTelemetry(
            ramUsedMb = usedMb,
            ramTotalMb = totalMb,
            ramAvailableMb = availMb,
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryTempC = tempTenths / 10f,
            mediaVolume = mediaVol,
            mediaMaxVolume = mediaMax,
            ringVolume = ringVol,
            ringMaxVolume = ringMax,
            brightnessPercent = brightnessPct,
            isMusicActive = audioManager.isMusicActive,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
    }

    suspend fun getLaunchableApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(intent, 0)
        resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == appContext.packageName) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                if (label.isBlank()) return@mapNotNull null
                InstalledAppItem(label = label, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = appContext.packageManager.getLaunchIntentForPackage(packageName) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun findAndLaunchAppByName(query: String): InstalledAppItem? {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return null
        val apps = getLaunchableApps()
        val match = apps.firstOrNull { it.label.lowercase() == clean }
            ?: apps.firstOrNull { it.label.lowercase().contains(clean) }
            ?: apps.firstOrNull { it.packageName.lowercase().contains(clean) }
        return if (match != null && launchApp(match.packageName)) match else null
    }

    fun setMediaVolume(level: Int) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            level.coerceIn(0, max),
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun setRingVolume(level: Int) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        audioManager.setStreamVolume(
            AudioManager.STREAM_RING,
            level.coerceIn(0, max),
            0
        )
    }

    fun setSystemBrightnessPercent(percent: Int): Boolean {
        if (!Settings.System.canWrite(appContext)) return false
        return try {
            val value = ((percent.coerceIn(5, 100) / 100f) * 255f).toInt().coerceIn(10, 255)
            Settings.System.putInt(
                appContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun dispatchMediaKey(keyCode: Int): Boolean {
        return try {
            val eventTime = SystemClock.uptimeMillis()
            val down = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            val up = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)
            audioManager.dispatchMediaKeyEvent(down)
            audioManager.dispatchMediaKeyEvent(up)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openYouTubeSearch(query: String): Boolean {
        return try {
            val url = if (query.isBlank()) {
                "https://www.youtube.com"
            } else {
                "https://www.youtube.com/results?search_query=${Uri.encode(query.trim())}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun searchContacts(query: String): List<ContactItem> = withContext(Dispatchers.IO) {
        if (!hasPerm(Manifest.permission.READ_CONTACTS)) return@withContext emptyList()
        val list = mutableListOf<ContactItem>()
        val selection = if (query.isBlank()) null else "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = if (query.isBlank()) null else arrayOf("%${query.trim()}%")
        try {
            appContext.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                selection,
                args,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext() && list.size < 25) {
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val num = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                    if (name.isNotBlank() && num.isNotBlank()) {
                        list.add(ContactItem(name = name, phoneNumber = num))
                    }
                }
            }
        } catch (_: Exception) {
        }
        list.distinctBy { "${it.name}|${it.phoneNumber}" }
    }

    fun initiateDial(phoneNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun initiateDirectCall(phoneNumber: String): Boolean {
        if (!hasPerm(Manifest.permission.CALL_PHONE)) {
            return initiateDial(phoneNumber)
        }
        return try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(phoneNumber)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openSmsDraft(phoneNumber: String, body: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phoneNumber)}")).apply {
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openSystemSettings(action: String, withPackageUri: Boolean = false) {
        try {
            val intent = Intent(action).apply {
                if (withPackageUri) {
                    data = Uri.parse("package:${appContext.packageName}")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(fallback)
            } catch (_: Exception) {
            }
        }
    }

    fun setWakeServiceRunning(enabled: Boolean) {
        val intent = Intent(appContext, GwenWakeWordService::class.java).apply {
            action = if (enabled) GwenWakeWordService.ACTION_START else GwenWakeWordService.ACTION_STOP
        }
        try {
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(appContext, intent)
            } else {
                appContext.startService(intent)
            }
        } catch (_: Exception) {
        }
    }
}
