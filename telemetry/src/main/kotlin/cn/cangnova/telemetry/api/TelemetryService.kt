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

package cn.cangnova.telemetry.api

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.time.Instant
import java.util.*

/**
 * 遥测服务接口，提供发送遥测事件的功能
 * 这是遥测系统的核心服务接口，所有需要发送遥测数据的模块都应该使用这个接口
 */
@Service
interface TelemetryService {
    /**
     * 发送遥测事件
     * @param event 要发送的遥测事件
     * @return 如果事件被成功接收，则返回true；否则返回false
     */
    fun sendEvent(event: TelemetryEvent): Boolean

    /**
     * 创建并发送遥测事件
     * @param category 事件类别
     * @param name 事件名称
     * @param value 事件值
     * @param properties 事件附加属性
     * @return 如果事件被成功接收，则返回true；否则返回false
     */
    fun sendEvent(
        category: String,
        name: String,
        value: Any,
        properties: Map<String, String> = emptyMap()
    ): Boolean {
        val event = BaseTelemetryEvent(
            id = "${category}_${name}_${UUID.randomUUID()}",
            category = category,
            name = name,
            value = value,
            timestamp = Instant.now(),
            properties = properties
        )
        return sendEvent(event)
    }

    /**
     * 检查遥测功能是否已启用
     * @return 如果遥测功能已启用，则返回true；否则返回false
     */
    fun isEnabled(): Boolean

    /**
     * 立即发送所有已收集的遥测数据到服务器
     * @return 如果发送成功，则返回true；否则返回false
     */
    fun sendImmediately(): Boolean

    companion object {
        /**
         * 获取遥测服务实例
         * @return 遥测服务实例
         */
        @JvmStatic
        fun getInstance(): TelemetryService = service()
    }
} 