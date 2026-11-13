package com.example.core

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GwenNotificationItem(
    val key: String,
    val appPackage: String,
    val title: String,
    val text: String,
    val postTime: Long
)

class GwenNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    private fun refreshNotifications() {
        try {
            val active = activeNotifications.orEmpty().take(25).map { sbn ->
                val extras = sbn.notification.extras
                val title = extras?.getCharSequence("android.title")?.toString().orEmpty()
                val text = extras?.getCharSequence("android.text")?.toString().orEmpty()
                GwenNotificationItem(
                    key = sbn.key,
                    appPackage = sbn.packageName,
                    title = title.ifBlank { sbn.packageName.substringAfterLast('.') },
                    text = text,
                    postTime = sbn.postTime
                )
            }
            _notifications.value = active
        } catch (_: Exception) {
        }
    }

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _notifications = MutableStateFlow<List<GwenNotificationItem>>(emptyList())
        val notifications: StateFlow<List<GwenNotificationItem>> = _notifications.asStateFlow()
    }
}
