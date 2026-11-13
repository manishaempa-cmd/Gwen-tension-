package com.example.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenUiElement(
    val text: String,
    val className: String,
    val clickable: Boolean,
    val scrollable: Boolean
)

class GwenAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        refreshScreenNodes()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            refreshScreenNodes()
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        _isConnected.value = false
    }

    fun refreshScreenNodes(): List<ScreenUiElement> {
        val root = rootInActiveWindow ?: return emptyList()
        val elements = mutableListOf<ScreenUiElement>()
        collectNodes(root, elements, maxNodes = 60)
        _screenElements.value = elements
        return elements
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo?,
        out: MutableList<ScreenUiElement>,
        maxNodes: Int
    ) {
        if (node == null || out.size >= maxNodes) return
        val label = node.text?.toString()?.trim()
            ?: node.contentDescription?.toString()?.trim()
            ?: ""
        if (label.isNotBlank()) {
            out.add(
                ScreenUiElement(
                    text = label.take(120),
                    className = node.className?.toString()?.substringAfterLast('.') ?: "View",
                    clickable = node.isClickable,
                    scrollable = node.isScrollable
                )
            )
        }
        for (i in 0 until node.childCount) {
            if (out.size >= maxNodes) break
            collectNodes(node.getChild(i), out, maxNodes)
        }
    }

    fun performClickByText(query: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val matches = root.findAccessibilityNodeInfosByText(query)
        for (node in matches) {
            var target: AccessibilityNodeInfo? = node
            while (target != null && !target.isClickable) {
                target = target.parent
            }
            if (target != null && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
        }
        return false
    }

    fun performScroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findScrollableNode(root)
        if (scrollable != null) {
            val action = if (forward) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            if (scrollable.performAction(action)) return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                if (forward) {
                    moveTo(540f, 1500f)
                    lineTo(540f, 500f)
                } else {
                    moveTo(540f, 500f)
                    lineTo(540f, 1500f)
                }
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 260))
                .build()
            return dispatchGesture(gesture, null, null)
        }
        return false
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findScrollableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    fun tapCoordinates(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    companion object {
        @Volatile
        var instance: GwenAccessibilityService? = null
            private set

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _screenElements = MutableStateFlow<List<ScreenUiElement>>(emptyList())
        val screenElements: StateFlow<List<ScreenUiElement>> = _screenElements.asStateFlow()
    }
}
