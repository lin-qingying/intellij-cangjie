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

package org.cangnova.cangjie.project.extension

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.service.GeneratedFilesHolder
import org.cangnova.cangjie.result.CjProcessResult

/**
 * 项目提供者扩展点
 *
 * 负责识别和创建特定类型的仓颉项目 (如 CJPM、KCJPM 等)
 * 具体实现由各个包管理器模块提供
 */
interface CjProjectProvider {
    companion object {
        val EP_NAME = ExtensionPointName<CjProjectProvider>(
            "org.cangnova.cangjie.project.projectProvider"
        )
    }

    /**
     * 获取此提供者关联的构建系统 ID
     *
     * @return 构建系统 ID
     */
    fun getBuildSystemId(): ProjectBuildSystemId

    /**
     * 判断是否可以处理该目录
     *
     * @param dir 待检测的目录
     * @return 如果可以处理返回 true
     */
    fun canHandle(dir: VirtualFile): Boolean

    /**
     * 创建项目实例
     *
     * @param dir 项目根目录
     * @param project IntelliJ 项目实例
     * @return 创建的仓颉项目，如果无法创建返回 null
     */
    fun createProject(dir: VirtualFile, project: Project): CjProject?

    /**
     * 物理文件中创建项目，它和createProject是有区别的，createProject是基于已经存在的文件夹创建项目
     * 而这个方法是基于物理文件创建项目文件夹，然后在创建（通过工具链命令创建）
     */
    fun createProjectFromPhysicalFile(
        sdkId: String,
        project: Project, owner: Disposable,
        directory: VirtualFile,
        projectType: String,
        name: String? = null
    ): CjProcessResult<GeneratedFilesHolder>

    /**
     * 直接创建项目文件，不调用工具链命令
     *
     * 通过插件直接生成项目所需的文件和目录结构，避免调用外部工具链。
     * 适用于快速创建项目或工具链不可用的场景。
     *
     * @param project IntelliJ 项目实例
     * @param directory 项目根目录
     * @param projectType 项目类型（如 "executable", "static" 等）
     * @param name 项目名称
     * @return 创建结果，包含生成的文件
     */
//    fun createProjectFilesDirectly(
//        project: Project,
//        directory: VirtualFile,
//        projectType: String,
//        name: String
//    ): CjProcessResult<GeneratedFilesHolder>

    /**
     * 项目提供者名称
     */
    val providerName: String

    /**
     * 获取需要监听的文件模式
     *
     * 返回此项目类型需要监听的配置文件和关键文件列表。
     * 当这些文件发生变更时，会触发项目刷新。
     *
     * 文件模式支持：
     * - 绝对路径匹配：如 "/cjpm.toml", "/build.cj"
     * - 相对路径匹配：如 "/src/main.cj", "/examples/main.cj"
     *
     * @return 需要监听的文件路径模式列表
     *
     * @see CangJieConfigFileWatcher
     */
    fun getWatchedFilePatterns(): WatchedFilePatterns {
        // 默认实现：返回空列表，由具体的项目提供者覆盖
        return WatchedFilePatterns.EMPTY
    }

    /**
     * 获取项目需要索引的目录列表
     *
     * 返回指定项目中所有需要建立目录索引的目录，用于快速查找文件所属的项目。
     * 这些目录通常包括：
     * - 项目根目录
     * - 源码目录
     * - 输出目录
     * - 依赖库目录
     * - 其他需要索引的自定义目录
     *
     * @param project 仓颉项目实例
     * @return 需要建立索引的目录列表
     */
    fun getIndexableDirectories(project: CjProject): List<VirtualFile> {
        // 默认实现：只索引项目根目录
        return listOfNotNull(project.rootDir)
    }
}

/**
 * 需要监听的文件模式配置
 *
 * @property configFiles 配置文件列表（如 cjpm.toml, package.json）
 * @property implicitTargetFiles 隐式目标文件列表（如 build.cj, main.cj）
 * @property implicitTargetDirs 隐式目标目录列表（如 /src/bin, /examples）
 */
data class WatchedFilePatterns(
    val configFiles: List<String> = emptyList(),
    val implicitTargetFiles: List<String> = emptyList(),
    val implicitTargetDirs: List<String> = emptyList()
) {
    companion object {
        val EMPTY = WatchedFilePatterns()
    }
}