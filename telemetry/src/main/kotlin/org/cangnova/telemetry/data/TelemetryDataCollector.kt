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

package org.cangnova.telemetry.data

import org.cangnova.telemetry.TelemetrySettings
import org.cangnova.telemetry.api.TelemetryEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 遥测数据收集器，负责收集和缓存遥测事件
 */
@Service
class TelemetryDataCollector {
    private val logger = Logger.getInstance(TelemetryDataCollector::class.java)
    private val settings = TelemetrySettings.getInstance()
    private val events = ConcurrentLinkedQueue<TelemetryEvent>()
    private val isCollecting = AtomicBoolean(false)

    /**
     * 收集一个遥测事件
     * @param event 要收集的事件
     * @return 如果事件被成功收集，则返回true；否则返回false
     */
    fun collect(event: TelemetryEvent): Boolean {
        // 如果遥测功能被禁用，则不收集数据
        if (!settings.telemetryEnabled) {
            return false
        }

        try {
            events.add(event)
            logger.debug("Collected telemetry event: ${event.id}")
            return true
        } catch (e: Exception) {
            logger.warn("Failed to collect telemetry event", e)
            return false
        }
    }

    /**
     * 获取并清空收集到的所有事件
     * @return 收集到的事件列表
     */
    fun getAndClearEvents(): List<TelemetryEvent> {
        val result = mutableListOf<TelemetryEvent>()

        // 使用原子布尔值确保线程安全
        if (isCollecting.compareAndSet(false, true)) {
            try {
                var event = events.poll()
                while (event != null) {
                    result.add(event)
                    event = events.poll()
                }
            } finally {
                isCollecting.set(false)
            }
        }

        return result
    }
    
    /**
     * 获取当前缓存的事件数量
     * @return 缓存的事件数量
     */
    fun getPendingEventsCount(): Int {
        return events.size
    }
    
    /**
     * 检查遥测功能是否已启用
     * @return 如果遥测功能已启用，则返回true；否则返回false
     */
    fun isEnabled(): Boolean {
        return settings.telemetryEnabled
    }

    companion object {
        @JvmStatic
        fun getInstance(): TelemetryDataCollector = service()
    }
} 