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

package cn.cangnova.telemetry

import cn.cangnova.telemetry.sender.TelemetryDataSender
import cn.cangnova.telemetry.ui.TelemetryNotifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * 遥测启动活动，负责：
 * 1. 初始化遥测系统和数据发送器
 * 2. 检查遥测设置并显示通知
 */
class TelemetryStartupActivity : ProjectActivity {
    private val logger = Logger.getInstance(TelemetryStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        runActivity(project)
    }

    fun runActivity(project: Project) {
        // 只在应用启动时执行一次，不是每个项目都执行
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return
        }

        logger.info("Initializing telemetry system")

        // 初始化数据发送器
        val dataSender = TelemetryDataSender.getInstance()
        dataSender.initialize()

        logger.info("Checking telemetry settings")

        // 检查是否已经做出了遥测选择
        val settings = TelemetrySettings.getInstance()

        // 如果用户还没有做出选择（设置中没有明确的值），则显示通知
        if (!settings.hasUserMadeChoice()) {
            TelemetryNotifications.showNotification()
        }
    }
} 