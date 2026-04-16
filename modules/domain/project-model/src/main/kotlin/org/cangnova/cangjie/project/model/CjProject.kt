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

package org.cangnova.cangjie.project.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdk

/**
 * 获取项目的仓颉 SDK
 *
 * 这是一个便捷的扩展属性，用于快速访问当前项目配置的仓颉 SDK。
 *
 * 示例：
 * ```kotlin
 * val sdk = project.cjSdk
 * if (sdk != null) {
 *     val version = sdk.version
 *     val homePath = sdk.homePath
 * }
 * ```
 *
 * @receiver IntelliJ 项目
 * @return 仓颉 SDK 实例，如果未配置则返回 null
 */
val Project.cjSdk: CjSdk?
    get() {
        return CjProjectSdkConfig.getInstance(this).getProjectSdk()
    }

/**
 * 获取项目的仓颉项目模型
 *
 * 这是一个便捷的扩展属性，用于快速访问当前项目的仓颉项目模型。
 * 该属性总是返回非 null 值，如果项目不是有效的仓颉项目，
 * 会返回一个默认的空项目模型。
 *
 * 示例：
 * ```kotlin
 * val cjProject = project.cjProject
 * val modules = cjProject.workspace?.modules ?: listOf(cjProject.module)
 * ```
 *
 * @receiver IntelliJ 项目
 * @return 仓颉项目模型实例，不为 null
 */
val Project.cjProject: CjProject
    get() {
        return CjProjectsService.getInstance(this).cjProject
    }

/**
 * 仓颉项目抽象接口
 *
 * 代表一个仓颉项目的核心抽象，可以是 CJPM 项目、KCJPM 项目等。
 * 具体实现由各个包管理器模块提供。
 */
interface CjProject {
    /**
     * 项目名称
     */
    val name: String

    /**
     * 项目根目录
     */
    val rootDir: VirtualFile


    /**
     * 关联的 IntelliJ 项目
     */
    val intellijProject: Project


    /**
     * 项目类型标识：是否为工作空间项目
     * - true: 工作空间项目（多模块）
     * - false: 单模块项目
     */
    val isWorkspace: Boolean

    /**
     * 项目的主模块（单模块项目）或工作空间（多模块项目）
     * - 单模块项目：返回该项目的唯一模块
     * - 工作空间项目：返回 null（使用 workspace 访问所有模块）
     */
    val module: CjModule?

    /**
     * 所属工作空间 (如果有)
     */
    val workspace: CjWorkspace?


    /**
     * 项目是否有效
     */
    val isValid: Boolean

    /**
     * 获取所有需要索引的目录
     *
     * 返回项目中所有需要建立目录索引的目录列表，用于快速查找文件所属的项目。
     * 默认实现返回空列表，由具体的项目类型覆盖此属性。
     *
     * 这些目录通常包括：
     * - 项目根目录
     * - 源码目录
     * - 输出目录
     * - 依赖库目录
     */
    val indexableDirectories: List<VirtualFile>
        get() = emptyList()

    /**
     * 刷新项目模型
     *
     * 此方法用于在项目同步过程中执行刷新操作，当遇到错误时会抛出异常，
     * 允许调用方（如 CangJieProjectSyncTask）捕获并处理错误信息。
     *
     * 建议在以下场景使用此方法：
     * - 项目同步任务中 (CangJieProjectSyncTask)
     * - 需要向用户显示具体错误信息的UI操作
     * - 批量项目处理中的错误处理
     * - 自动化脚本或CI/CD流程中的错误检测
     *
     * @param onComplete 可选的完成回调,在所有异步操作完成后调用
     * @throws Exception 当项目刷新过程中遇到任何错误时抛出异常，
     *                    包括TOML解析错误、CJPM命令执行错误等
     */
    @Throws(Exception::class)
    fun refresh(onComplete: (() -> Unit)? = null)


    /**
     * 查找指定名称的模块
     */
    fun findModule(name: String): CjModule?



}