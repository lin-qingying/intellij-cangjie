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

package org.cangnova.cangjie.project.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import org.cangnova.cangjie.project.model.*
import org.cangnova.cangjie.result.CjProcessResult


val Project.cangjieProjectService: CjProjectsService get() = CjProjectsService.getInstance(this)

/**
 * 仓颉项目管理服务接口
 *
 * ## 架构设计：Project Model 优先，Workspace Model 补充
 *
 * 该服务实现了"Project Model 优先，Workspace Model 补充"的架构：
 * - **Project Model（主）**：基于 CjProject/CjModule 的自定义模型，作为唯一数据源
 * - **Workspace Model（辅）**：仅在必要时同步，为 IDE 子系统提供补充元数据（当前已禁用）
 *
 * ## 核心职责
 * - 项目的发现、创建和生命周期管理
 * - 模块索引和快速查找（O(1) 文件到模块映射）
 * - 项目事件发布（CREATED、OPENED、UPDATED、REMOVED、CONFIG_CHANGED）
 * - 与外部系统框架集成（通过 CangJieExternalSystemProjectAware）
 * - 项目配置变更的监听和自动刷新
 *
 * ## 单项目模型
 * 每个 IntelliJ 项目对应一个仓颉项目（cjProject），项目内部可以包含多个模块。
 * 这简化了项目管理，与 IntelliJ 平台的项目模型保持一致。
 *
 * ## 项目自动导入机制
 * 服务支持两种项目导入模式：
 * - **新模式（推荐）**：基于 [ExternalSystemProjectTracker] 的自动导入，通过 Registry 配置
 *   `org.cangnova.cangjie.project.new.auto.import` 启用。支持配置文件变更自动刷新。
 * - **旧模式**：基于文件监听的手动刷新机制，需手动触发项目刷新。
 *
 * 服务作用域为 PROJECT 级别，每个 IntelliJ 项目对应一个服务实例。
 *
 * @see CjProjectsServiceImpl
 * @see CangJieExternalSystemProjectAware
 */

interface CjProjectsService {
    val intellijProject: Project
    /**
     * 无项目标记对象
     * 用作 LightDirectoryIndex 的默认值，表示文件不属于任何仓颉项目
     */
    public val noProjectMarker: CjProject
        get() {
            return object : CjProject {
                override val intellijProject: Project = this@CjProjectsService.intellijProject

                override val name: String = "<no project>"
                override val rootDir: VirtualFile
                    get() = intellijProject.guessProjectDir() ?: error("No project dir")
                override val isWorkspace: Boolean
                    get() = false
                override val module: CjModule = object : CjModule {
                    override val name: String = "<no module>"
                    override val rootDir: VirtualFile = intellijProject.guessProjectDir() ?: error("No project dir")
                    override val project: CjProject
                        get() = noProjectMarker
                    override val configFile: VirtualFile?
                        get() = null

                    override val sourceSets: List<CjSourceSet>
                        get() = emptyList()

                    override val metadata: CjPackageMetadata
                        get() = CjPackageMetadata.EMPTY

                    override fun <T : Any?> getUserData(key: Key<T?>): T? {
                        return null
                    }

                    override fun <T : Any?> putUserData(key: Key<T?>, value: T?) {

                    }

                }


                override val workspace: CjWorkspace? = null

                override val isValid: Boolean = false
                override val indexableDirectories: List<VirtualFile> = emptyList()

                @Throws(Exception::class)
                override fun refresh(onComplete: (() -> Unit)?) {
                }

                override fun findModule(name: String): CjModule? = null
            }
        }

    companion object {



        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): CjProjectsService {
            return project.getService(CjProjectsService::class.java)
        }


        val CANGJIE_PROJECTS_REFRESH_TOPIC: Topic<CangJieProjectsRefreshListener> = Topic(
            "CangJie refresh",
            CangJieProjectsRefreshListener::class.java
        )

    }

    enum class RefreshStatus {
        SUCCESS,
        FAILURE,
        CANCEL
    }

    interface CangJieProjectsRefreshListener {
        fun onRefreshStarted()
        fun onRefreshFinished(status: RefreshStatus)
    }

    fun interface CangJieProjectsListener {
        fun cangjieProjectsUpdated(service: CjProjectsService, projects: Collection<CjProject>)
    }


    val cjProject: CjProject


    /**
     * 发现并创建项目
     *
     * 通过扩展点机制尝试识别并创建项目
     *
     * @param rootDir 项目根目录
     * @return 创建的项目，如果无法识别返回 null
     */
    fun discoverProject(rootDir: VirtualFile)

    /**
     * 添加项目到缓存
     *
     * @param project 要添加的项目
     */
    fun addModule(module: CjModule)

    /**
     * 从缓存中移除项目
     *
     * @param project 要移除的项目
     */
    fun removeModule(module: CjModule)


    /**
     * 刷新指定项目
     *
     * @param project 要刷新的项目
     */
    fun refreshProject()


    /**
     * 根据文件查找所属模块
     *
     * @param file 要查找的文件
     * @return 文件所属的模块，如果不属于任何模块返回 null
     */
    fun findModuleForFile(file: VirtualFile): CjModule?

    val initialized: Boolean


    /**
     * 创建项目（通过工具链命令）
     */
    fun createProject(
        sdkId: String,
        owner: Disposable,
        directory: VirtualFile,
        projectType: String = "executable",
        name: String? = null,
    ): CjProcessResult<GeneratedFilesHolder>

    /**
     * 直接创建项目文件，不调用工具链命令
     *
     * 通过插件直接生成项目所需的文件和目录结构。
     * 适用于快速创建项目或工具链不可用的场景。
     *
     * @param directory 项目根目录
     * @param projectType 项目类型（如 "executable", "static" 等）
     * @param name 项目名称
     * @return 创建结果，包含生成的文件
     */
//    fun createProjectFilesDirectly(
//        directory: VirtualFile,
//        projectType: String = "executable",
//        name: String
//    ): CjProcessResult<GeneratedFilesHolder>


}


data class GeneratedFilesHolder(val manifest: VirtualFile, val sourceFiles: List<VirtualFile>)
