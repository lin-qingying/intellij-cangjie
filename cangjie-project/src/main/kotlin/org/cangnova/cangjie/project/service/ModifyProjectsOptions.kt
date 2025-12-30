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

package org.cangnova.cangjie.project.service

/**
     * 项目更新选项
     *
     * @property lightweight 是否为轻量级更新（不触发完整的刷新事件和根目录变更）
     * @property publishRefreshEvents 是否发布刷新状态事件（onRefreshStarted/onRefreshFinished）
     * @property resetIndices 是否重置索引
     * @property updateRoots 是否更新项目根目录
     */
    data class ModifyProjectsOptions(
        val lightweight: Boolean = false,
        val publishRefreshEvents: Boolean = true,
        val resetIndices: Boolean = true,
        val updateRoots: Boolean = true
    ) {
        companion object {
            /**
             * 默认选项：完整更新流程
             */
            val DEFAULT = ModifyProjectsOptions()

            /**
             * 轻量级选项：适用于单个项目的增删操作
             * - 不发布刷新事件
             * - 不更新项目根目录（避免触发大规模索引重建）
             * - 仍然重置索引以保持一致性
             */
            val LIGHTWEIGHT = ModifyProjectsOptions(
                lightweight = true,
                publishRefreshEvents = false,
                updateRoots = false
            )

            /**
             * 初始化加载选项：适用于 loadState
             * - 不发布刷新事件（避免在启动时触发不必要的通知）
             * - 不更新根目录（启动后会单独刷新）
             * - 不重置索引（延迟到刷新时）
             * - 跳过文件类型关联和消息发布（避免阻塞服务初始化）
             */
            val LOAD_STATE = ModifyProjectsOptions(
                lightweight = true,
                publishRefreshEvents = false,
                resetIndices = false,
                updateRoots = false
            )
        }
    }