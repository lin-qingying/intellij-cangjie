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

package org.cangnova.cangjie.project.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.project.service.CjProjectsService


val Project.allCjProject: List<CjProject>
    get() {
        return CjProjectsService.getInstance(this).allProjects
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
     * 项目配置文件 (如 cjpm.toml)
     */
    val configFile: VirtualFile?

    /**
     * 项目中的模块列表
     */
    val modules: List<CjModule>

    /**
     * 所属工作空间 (如果有)
     */
    val workspace: CjWorkspace?

    /**
     * 项目版本
     */
    val version: String?

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
     */
    fun refresh()

    /**
     * 查找指定名称的模块
     */
    fun findModule(name: String): CjModule?
}