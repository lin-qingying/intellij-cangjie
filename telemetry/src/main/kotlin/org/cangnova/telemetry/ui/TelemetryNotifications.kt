/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.telemetry.ui

import org.cangnova.telemetry.TelemetryBundle
import org.cangnova.telemetry.TelemetrySettings
import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationListener.UrlOpeningListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.Consumer

object TelemetryNotifications {

    fun showNotification() {
        val group = NotificationGroupManager.getInstance().getNotificationGroup("Enable Telemetry")

        val notification = group.createNotification(
            TelemetryBundle.message("notification.group.cangjie.title"),
            TelemetryBundle.message(
                "notification.group.message",
                "<a href=\"https://telemetry.cangnova.cn/privacy-policy\">${TelemetryBundle.message("notification.group.privacy.link")}</a>",
                "<a href=\"\">${TelemetryBundle.message("notification.group.opt.out.link")}</a>"
            ),
            NotificationType.INFORMATION
        )
        notification.setListener(UrlOpeningListener(false))
        val accept = DumbAwareAction.create(
            TelemetryBundle.message("notification.group.accept"),
            Consumer { enableTelemetry(true, notification) })
        val deny = DumbAwareAction.create(
            TelemetryBundle.message("notification.group.deny"),
            Consumer { enableTelemetry(false, notification) })
        val later = DumbAwareAction.create(
            TelemetryBundle.message("notification.group.later"),
            Consumer { notification.expire() })
        notification
            .addAction(accept)
            .addAction(deny)
            .addAction(later)
            .setIcon(AllIcons.General.TodoQuestion)
            .notify(null)

    }

    private fun enableTelemetry(enabled: Boolean, notification: Notification) {
        // Update telemetry settings
        val settings = TelemetrySettings.getInstance()
        settings.telemetryEnabled = enabled
        
        // Close the notification
        notification.expire()
        
        // Show confirmation if enabled
        if (enabled) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Enable Telemetry")
                .createNotification(
                    TelemetryBundle.message("notification.group.cangjie.title"),
                    TelemetryBundle.message("notification.telemetry.enabled.message"),
                    NotificationType.INFORMATION
                )
                .setIcon(AllIcons.General.Information)
                .notify(null)
        }
    }
}