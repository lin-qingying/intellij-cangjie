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

package org.cangnova.cangjie.project.extension

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.project.model.CjProject

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
     * 提供者优先级 (数值越小优先级越高)
     */
    val priority: Int
        get() = 100

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
     * 项目提供者名称
     */
    val providerName: String
}