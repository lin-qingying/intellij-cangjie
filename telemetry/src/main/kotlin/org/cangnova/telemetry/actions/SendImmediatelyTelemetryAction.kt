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

package org.cangnova.telemetry.actions

import org.cangnova.telemetry.TelemetryBundle
import org.cangnova.telemetry.api.TelemetryService
import org.cangnova.telemetry.data.TelemetryDataCollector
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 立即发送遥测数据的动作
 * 用于手动触发遥测数据发送，将所有缓存的遥测数据立即发送到服务器
 */
class SendImmediatelyTelemetryAction : AnAction(
    TelemetryBundle.message("action.telemetry.send.immediately.text"),
    TelemetryBundle.message("action.telemetry.send.immediately.description"),
    AllIcons.Actions.Upload
) {
    private val logger = Logger.getInstance(SendImmediatelyTelemetryAction::class.java)

    // 通知组ID，与telemetry.xml中定义的一致
    private val NOTIFICATION_GROUP_ID = "CangJie.Telemetry"

    override fun actionPerformed(e: AnActionEvent) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // 获取遥测服务
                val telemetryService = TelemetryService.getInstance()
                if (!telemetryService.isEnabled()) {
                    ApplicationManager.getApplication().invokeLater {
                        showNotification(
                            e,
                            TelemetryBundle.message("telemetry.send.immediately.disabled.message"),
                            NotificationType.WARNING
                        )
                    }
                    return@executeOnPooledThread
                }

                // 获取数据收集器
                val dataCollector = TelemetryDataCollector.getInstance()

                // 获取缓存数据数量
                val cachedCount = dataCollector.getPendingEventsCount()

                // 手动触发数据发送
                val sendResult = telemetryService.sendImmediately()

                ApplicationManager.getApplication().invokeLater {
                    // 显示结果通知
                    if (sendResult) {
                        if (cachedCount > 0) {
                            showNotification(
                                e,
                                TelemetryBundle.message("telemetry.send.immediately.success.with.count", cachedCount),
                                NotificationType.INFORMATION
                            )
                        } else {
                            showNotification(
                                e,
                                TelemetryBundle.message("telemetry.send.immediately.success.no.data"),
                                NotificationType.INFORMATION
                            )
                        }
                    } else {
                        showNotification(
                            e,
                            TelemetryBundle.message("telemetry.send.immediately.error"),
                            NotificationType.ERROR
                        )
                    }
                }
            } catch (ex: Exception) {
                logger.error("立即发送遥测数据时出错", ex)
                ApplicationManager.getApplication().invokeLater {
                    showNotification(
                        e,
                        TelemetryBundle.message("telemetry.send.immediately.exception", ex.message ?: ""),
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    /**
     * 显示通知
     */
    private fun showNotification(e: AnActionEvent, message: String, type: NotificationType) {
        val project = e.project
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(message, type)
            .notify(project)
    }
} 