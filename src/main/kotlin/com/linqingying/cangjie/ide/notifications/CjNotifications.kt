package com.linqingying.cangjie.ide.notifications

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey


object CjNotifications {

    fun buildLogGroup(): NotificationGroup {
        return NotificationGroupManager.getInstance().getNotificationGroup("CangJie Build Log")
    }

    fun pluginNotifications(): NotificationGroup {
        return NotificationGroupManager.getInstance().getNotificationGroup("CangJie Plugin")
    }



}
