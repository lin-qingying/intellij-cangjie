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
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.LightDirectoryIndex
import org.cangnova.cangjie.project.event.CjProjectEvent
import org.cangnova.cangjie.project.event.CjProjectListener
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.roots
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.utils.*
import java.util.*


/**
 * 仓颉模块索引
 *
 * 负责维护从 VirtualFile 到 CjModule 的快速映射，实现 O(1) 查找。
 * 该索引监听项目更新事件，自动重建索引以保持数据一致性。
 *
 * 设计思路：
 * - 为每个 CjProject 维护独立的 LightDirectoryIndex
 * - 索引模块的所有相关目录：模块根目录、源码目录、输出目录
 * - 使用 Optional 包装模块对象，以区分"未找到"和"无模块"两种情况
 * - 订阅 CANGJIE_PROJECTS_TOPIC，在项目更新时自动刷新索引
 *
 * @property intellijProject IntelliJ 项目实例
 * @property service 仓颉项目服务实例
 */
class CangJieModuleIndex(
    private val intellijProject: Project,
    private val service: CjProjectsService
) : CjProjectListener {
    /**
     * 模块索引
     * 使用 Optional 包装以区分"未找到"和"无模块"两种情况
     */
    private var moduleIndex: LightDirectoryIndex<Optional<CjModule>>? = null

    /**
     * 索引生命周期管理器
     */
    private var indexDisposable: Disposable? = null

    init {
        // 订阅仓颉项目更新事件
        intellijProject.messageBus.connect(intellijProject)
            .subscribe(CjProjectListener.TOPIC, this)
    }

    /**
     * 项目创建时的回调
     *
     * 在 EDT 线程上异步执行索引重建，避免在后台线程中执行写操作。
     */
    override fun projectCreated(event: CjProjectEvent) {
        scheduleIndexRebuild()
    }

    /**
     * 项目更新时的回调
     *
     * 在 EDT 线程上异步执行索引重建，避免在后台线程中执行写操作。
     */
    override fun projectUpdated(event: CjProjectEvent) {
        scheduleIndexRebuild()
    }

    /**
     * 项目删除时的回调
     *
     * 在 EDT 线程上异步执行索引重建，避免在后台线程中执行写操作。
     */
    override fun projectRemoved(event: CjProjectEvent) {
        scheduleIndexRebuild()
    }

    /**
     * 项目配置变更时的回调
     *
     * 在 EDT 线程上异步执行索引重建，避免在后台线程中执行写操作。
     */
    override fun projectConfigChanged(event: CjProjectEvent) {
        scheduleIndexRebuild()
    }

    /**
     * 调度索引重建
     *
     * 将索引重建任务提交到 EDT 线程执行。
     * 这是必要的，因为事件回调可能在后台线程中触发，
     * 而 `runWriteAction` 必须在 EDT 线程上执行。
     */
    private fun scheduleIndexRebuild() {
        ApplicationManager.getApplication().invokeLater {
            if (!intellijProject.isDisposed) {
                runWriteAction {
                    rebuildIndices()
                }
            }
        }
    }

    /**
     * 为文件查找所属模块
     *
     * @param file 要查找的文件
     * @return 文件所属的模块，如果不属于任何模块返回 null
     */
    fun findModuleForFile(file: VirtualFile): CjModule? {
        checkReadAccessAllowed()

        // 使用索引查找模块
        return moduleIndex?.getInfoForFile(file)?.orElse(null)
    }

    /**
     * 重建所有索引
     *
     * 该方法必须在写操作中调用，因为它会创建和注册 Disposable。
     * 重建过程：
     * 1. 清理旧的索引
     * 2. 为项目的每个模块建立目录映射
     * 3. 索引模块根目录、源码目录和输出目录
     */
    private fun rebuildIndices() {

        checkWriteAccessAllowed()

        // 清理旧索引
        resetIndex()

        // 创建新的 Disposable，用于管理索引生命周期
        val disposable = Disposer.newDisposable("CangJieModuleIndexDisposable")
        Disposer.register(intellijProject, disposable)

        // 构建模块索引
        val cjProject = service.cjProject
        moduleIndex = LightDirectoryIndex(disposable, Optional.empty()) { index ->
            // 先获取到局部变量，避免多次访问属性导致的竞态条件
            val workspace = cjProject.workspace
            val module = cjProject.module

            if (workspace != null) {
                // 遍历工作空间中的所有模块
                for (workspaceModule in workspace.modules) {
                    val moduleInfo = Optional.of(workspaceModule)

                    // 索引模块根目录
                    index.putInfo(workspaceModule.rootDir, moduleInfo)

                    // 索引所有源码集的根目录
                    for (sourceSet in workspaceModule.sourceSets) {
                        for (root in sourceSet.roots) {
                            index.putInfo(root, moduleInfo)
                        }
                    }

                    // 索引所有目标的输出目录

                }
            } else if (module != null) {
                val moduleInfo = Optional.of(module)

                // 索引模块根目录
                index.putInfo(module.rootDir, moduleInfo)

                // 索引所有源码集的根目录
                for (sourceSet in module.sourceSets) {
                    for (root in sourceSet.roots) {
                        index.putInfo(root, moduleInfo)
                    }
                }

                // 索引所有目标的输出目录

            }
        }

        indexDisposable = disposable
    }

    /**
     * 清理索引
     *
     * 释放所有索引占用的资源。
     */
    private fun resetIndex() {
        indexDisposable?.let { Disposer.dispose(it) }
        indexDisposable = null
        moduleIndex = null
    }
}