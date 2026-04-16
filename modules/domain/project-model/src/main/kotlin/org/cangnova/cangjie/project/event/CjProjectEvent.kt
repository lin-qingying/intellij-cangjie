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

package org.cangnova.cangjie.project.event

import org.cangnova.cangjie.project.model.CjProject
import java.util.*

/**
 * 项目事件类型
 */
enum class CjProjectEventType {
    /**
     * 项目创建
     */
    CREATED,

    /**
     * 项目打开
     */
    OPENED,

    /**
     * 项目更新/刷新
     */
    UPDATED,

    /**
     * 项目删除
     */
    REMOVED,

    /**
     * 项目配置变更
     */
    CONFIG_CHANGED,

    /**
     * 项目同步完成（刷新成功）
     */
    SYNCED
}

/**
 * 仓颉项目事件
 *
 * 当项目发生变化时触发
 */
class CjProjectEvent(
    /**
     * 发生变化的项目
     */
    val project: CjProject,

    /**
     * 事件类型
     */
    val eventType: CjProjectEventType
) : EventObject(project) {

    override fun toString(): String {
        return "CjProjectEvent(project=${project.name}, type=$eventType)"
    }
}