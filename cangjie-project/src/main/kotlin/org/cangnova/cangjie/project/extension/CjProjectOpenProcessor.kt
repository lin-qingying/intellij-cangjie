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

/**
 * 项目打开处理器扩展点
 *
 * 负责处理仓颉项目的打开逻辑
 */
interface CjProjectOpenProcessor {
    companion object {
        val EP_NAME = ExtensionPointName<CjProjectOpenProcessor>(
            "org.cangnova.cangjie.project.projectOpenProcessor"
        )
    }

    /**
     * 处理器优先级 (数值越小优先级越高)
     */
    val priority: Int
        get() = 100

    /**
     * 判断是否可以处理该项目
     *
     * @param projectDir 项目目录
     * @return 如果可以处理返回 true
     */
    fun canOpenProject(projectDir: VirtualFile): Boolean

    /**
     * 打开项目
     *
     * @param projectDir 项目目录
     * @param project IntelliJ 项目实例
     */
    fun doOpenProject(projectDir: VirtualFile, project: Project)

    /**
     * 处理器名称
     */
    val processorName: String
}