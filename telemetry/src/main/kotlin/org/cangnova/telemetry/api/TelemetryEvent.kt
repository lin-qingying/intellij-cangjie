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

package org.cangnova.telemetry.api

import java.time.Instant

/**
 * 遥测事件接口，表示一个可以被发送到遥测服务器的事件
 * 这是遥测系统的核心API接口，所有遥测事件都应该实现这个接口
 */
interface TelemetryEvent {
    /**
     * 事件的唯一标识符
     */
    val id: String
    
    /**
     * 事件的类别
     */
    val category: String
    
    /**
     * 事件的名称
     */
    val name: String
    
    /**
     * 事件的值
     */
    val value: Any
    
    /**
     * 事件的时间戳
     */
    val timestamp: Instant
    
    /**
     * 事件的附加属性
     */
    val properties: Map<String, String>
}

/**
 * 基础遥测事件实现
 */
data class BaseTelemetryEvent(
    override val id: String,
    override val category: String,
    override val name: String,
    override val value: Any,
    override val timestamp: Instant = Instant.now(),
    override val properties: Map<String, String> = emptyMap()
) : TelemetryEvent 