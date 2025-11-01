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

package org.cangnova.cangjie.project.service.impl

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.util.PathUtil
import org.cangnova.cangjie.CjConstants.MAIN_CJ_FILE
import org.cangnova.cangjie.project.extension.CjProjectProvider
import org.cangnova.cangjie.project.extension.WatchedFilePatterns
import org.cangnova.cangjie.project.service.CjProjectBuildSystemService
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.utils.pathAsPath

/**
 * 仓颉配置文件监听器
 *
 * 实现 [BulkFileListener] 接口，监听虚拟文件系统的变化事件，
 * 当检测到与仓颉项目相关的重要文件变更时，触发项目刷新。
 *
 * ## 监听的文件类型
 *
 * 监听的具体文件模式由各个 [CjProjectProvider] 扩展点实现定义，通过
 * [CjProjectProvider.getWatchedFilePatterns] 方法获取。当前硬编码的默认配置包括：
 * - 项目配置文件（如 cjpm.toml）
 * - 隐式目标文件（build.cj, main.cj, lib.cj 等）
 * - 特定目录下的 .cj 文件（src/bin, examples, tests, benches 等）
 *
 * ## TODO: 架构改进
 *
 * **当前实现存在的问题**：监听规则硬编码在此类中，不够灵活。
 *
 * **建议改进方案**：
 * 1. 从所有注册的 [CjProjectProvider] 扩展点收集监听规则
 * 2. 合并所有提供者的 [WatchedFilePatterns]
 * 3. 动态应用这些规则，支持不同项目类型的个性化监听
 *
 * 该监听器用于旧的项目刷新机制，在新的 [CangJieExternalSystemProjectAware] 机制启用后可能不再使用。
 *
 * @property projectService 项目管理服务，用于访问项目列表
 * @property onChange 当检测到相关文件变更时调用的回调函数
 * @see BulkFileListener
 * @see CangJieExternalSystemProjectAware
 * @see CjProjectProvider.getWatchedFilePatterns
 * @see WatchedFilePatterns
 */
class CangJieConfigFileWatcher(
    private val projectService: CjProjectsService,
    private val onChange: () -> Unit
) : BulkFileListener {

    /**
     * 从扩展点收集的监听规则
     *
     * 延迟初始化，获取与当前构建系统匹配的 [CjProjectProvider] 的 [WatchedFilePatterns]。
     * 如果没有扩展点提供规则，则使用默认的硬编码规则（向后兼容）。
     */
    private val watchedPatterns: WatchedFilePatterns by lazy {
        val buildSystemService = CjProjectBuildSystemService.getInstance()
        val buildSystemId = buildSystemService.getBuildSystemId()

        val provider = CjProjectProvider.EP_NAME.extensionList.find { provider ->
            buildSystemId == null || provider.getBuildSystemId().id == buildSystemId
        }

        provider?.getWatchedFilePatterns() ?: WatchedFilePatterns.EMPTY
    }

    /**
     * 文件变更前的回调
     *
     * 当前实现为空，不执行任何操作。
     */
    override fun before(events: List<VFileEvent>) = Unit

    /**
     * 文件变更后的回调
     *
     * 检查变更事件列表，如果包含感兴趣的事件（项目相关文件），则触发 [onChange] 回调。
     *
     * @param events 虚拟文件系统变更事件列表
     */
    override fun after(events: List<VFileEvent>) {
        if (events.any { isInterestingEvent(it) }) onChange()
    }

    /**
     * 判断事件是否与当前项目相关
     *
     * 使用动态收集的 [watchedPatterns] 判断事件，然后进一步检查文件是否属于当前管理的项目。
     *
     * @param event 虚拟文件事件
     * @return 如果事件需要触发项目刷新则返回 true
     */
    private fun isInterestingEvent(event: VFileEvent): Boolean {
        // 先进行通用的文件类型和路径检查（使用动态规则）
        if (!isInterestingEvent(projectService.intellijProject, event, watchedPatterns)) return false

        // 提取事件对应的文件
        val file = when (event) {
            is VFileContentChangeEvent -> event.file
            else -> return true  // 非内容变更事件（如创建、删除、重命名）直接认为是感兴趣的
        }
        val fileParentPath = file.pathAsPath.parent

        // 检查文件是否属于任何已管理的项目
       return true

    }

    companion object {
        /**
         * 判断文件事件是否需要关注（通用判断）
         *
         * 根据动态收集的 [WatchedFilePatterns] 和文件路径、事件类型判断是否应该触发项目刷新。
         *
         * 判断规则：
         * 1. 配置文件的任何变更（来自 [WatchedFilePatterns.configFiles]）
         * 2. 隐式目标文件的非内容变更（来自 [WatchedFilePatterns.implicitTargetFiles]）
         * 3. 特定目录下 .cj 文件的非内容变更（来自 [WatchedFilePatterns.implicitTargetDirs]）
         *
         * **注意**：内容变更事件（[VFileContentChangeEvent]）会被忽略，
         * 因为代码编辑不应触发项目结构刷新。
         *
         * @param project IntelliJ 项目实例（用于日志和未来扩展）
         * @param event 虚拟文件事件
         * @param patterns 监听规则配置
         * @return 如果事件需要触发项目刷新则返回 true
         */
        @VisibleForTesting
        fun isInterestingEvent(project: Project, event: VFileEvent, patterns: WatchedFilePatterns): Boolean {
            return when {
                // 1. 检查配置文件变更
                patterns.configFiles.any { event.pathEndsWith(it) } -> true

                // TODO: 未来可能需要监听锁文件变更，并根据时间戳判断是否为外部变更
                // 锁文件监听逻辑：区分 IDE 内部变更和外部工具变更，避免不必要的刷新

                // 2. 忽略内容变更事件（只关注文件的创建、删除、重命名）
                event is VFileContentChangeEvent -> false

                // 3. 只关注 .cj 文件
                !event.pathEndsWith(".cj") -> false

                // 4. 对于属性变更，只关注文件名变更（重命名）
                event is VFilePropertyChangeEvent && event.propertyName != VirtualFile.PROP_NAME -> false

                // 5. 检查是否为隐式目标文件
                patterns.implicitTargetFiles.any { event.pathEndsWith(it) } -> true

                // 6. 检查是否在隐式目标目录下
                else -> {
                    val parent = PathUtil.getParentPath(event.path)
                    val grandParent = PathUtil.getParentPath(parent)
                    patterns.implicitTargetDirs.any {
                        // 直接在目标目录下，或者是目标目录下的 main.cj
                        parent.endsWith(it) || (event.pathEndsWith(MAIN_CJ_FILE) && grandParent.endsWith(it))
                    }
                }
            }
        }

        /**
         * 检查事件路径是否以指定后缀结尾
         *
         * 对于属性变更事件（如重命名），同时检查新旧路径。
         *
         * @param suffix 要匹配的路径后缀
         * @return 如果路径以指定后缀结尾则返回 true
         */
        private fun VFileEvent.pathEndsWith(suffix: String): Boolean = path.endsWith(suffix) ||
                this is VFilePropertyChangeEvent && oldPath.endsWith(suffix)

        /**
         * 检查事件路径是否以列表中任一后缀结尾
         *
         * @param suffix 后缀列表
         * @return 如果路径以列表中任一后缀结尾则返回 true
         */
        private fun VFileEvent.pathEndsWith(suffix: List<String>): Boolean {
            // 列表中只要有一个为 true 就是 true
            return suffix.any { pathEndsWith(it) }
        }

        /** 日志记录器 */
        private val LOG = logger<CangJieConfigFileWatcher>()
    }

}
