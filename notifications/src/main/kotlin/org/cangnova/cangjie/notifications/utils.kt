package org.cangnova.cangjie.notifications

import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.util.NlsContexts.NotificationContent

fun showBalloonWithoutProject(@NotificationContent content: String, type: NotificationType) {
    val notification = CjNotifications.pluginNotifications().createNotification(content, type)
    Notifications.Bus.notify(notification)
}