/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.telemetry.impl

import org.cangnova.telemetry.TelemetrySettings
import org.cangnova.telemetry.api.TelemetryEvent
import org.cangnova.telemetry.api.TelemetryService
import org.cangnova.telemetry.data.TelemetryDataCollector
import org.cangnova.telemetry.sender.TelemetryDataSender
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger

/**
 * 默认的遥测服务实现
 */
@Service(Service.Level.APP)
class DefaultTelemetryService : TelemetryService {
    private val logger = Logger.getInstance(DefaultTelemetryService::class.java)
    private val settings = TelemetrySettings.getInstance()
    private val dataCollector = TelemetryDataCollector.getInstance()
    
    /**
     * 发送遥测事件
     * @param event 要发送的遥测事件
     * @return 如果事件被成功接收，则返回true；否则返回false
     */
    override fun sendEvent(event: TelemetryEvent): Boolean {
        if (!isEnabled()) {
            return false
        }
        
        try {
            // 直接收集事件，不需要转换
            return dataCollector.collect(event)
        } catch (e: Exception) {
            logger.warn("Failed to send telemetry event", e)
            return false
        }
    }
    
    /**
     * 检查遥测功能是否已启用
     * @return 如果遥测功能已启用，则返回true；否则返回false
     */
    override fun isEnabled(): Boolean {
        return settings.telemetryEnabled
    }
    
    /**
     * 立即发送所有已收集的遥测数据到服务器
     * @return 如果发送成功，则返回true；否则返回false
     */
    override fun sendImmediately(): Boolean {
        if (!isEnabled()) {
            logger.info("Telemetry is disabled, not sending data")
            return false
        }
        
        try {
            val dataSender = TelemetryDataSender.getInstance()
            val result = dataSender.sendDataToServer()
            
            if (result) {
                logger.info("Successfully sent telemetry data immediately")
            } else {
                logger.warn("Failed to send telemetry data immediately")
            }
            
            return result
        } catch (e: Exception) {
            logger.error("Error sending telemetry data immediately", e)
            return false
        }
    }
} 