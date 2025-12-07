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

package org.cangnova.cangjie.project.event

import com.intellij.util.messages.Topic
import java.util.*

/**
 * 仓颉项目监听器
 *
 * 监听项目的各种变化事件
 */
interface CjProjectListener : EventListener {
    companion object {
        /**
         * 消息总线主题
         */
        val TOPIC = Topic.create(
            "CangJie Project Events",
            CjProjectListener::class.java
        )
    }

    /**
     * 项目创建时调用
     */
    fun projectCreated(event: CjProjectEvent) {}

    /**
     * 项目打开时调用
     */
    fun projectOpened(event: CjProjectEvent) {}

    /**
     * 项目更新时调用
     */
    fun projectUpdated(event: CjProjectEvent) {}

    /**
     * 项目删除时调用
     */
    fun projectRemoved(event: CjProjectEvent) {}

    /**
     * 项目配置变更时调用
     */
    fun projectConfigChanged(event: CjProjectEvent) {}

    /**
     * 项目同步完成时调用（刷新成功后）
     *
     * 该事件在项目刷新完成后触发，此时项目模型已更新，
     * 依赖已解析，可以安全地重启依赖项目状态的服务（如 LSP）
     */
    fun projectSynced(event: CjProjectEvent) {}
}