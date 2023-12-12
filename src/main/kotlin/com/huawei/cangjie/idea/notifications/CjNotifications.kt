package com.huawei.cangjie.idea.notifications

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager


object CjNotifications {

    fun buildLogGroup(): NotificationGroup {
        return NotificationGroupManager.getInstance().getNotificationGroup("CangJie Build Log")
    }

    fun pluginNotifications(): NotificationGroup {
        return NotificationGroupManager.getInstance().getNotificationGroup("CangJie Plugin")
    }
}
