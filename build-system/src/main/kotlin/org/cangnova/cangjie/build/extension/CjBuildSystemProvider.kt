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

package org.cangnova.cangjie.build.extension

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.build.model.CjBuildSystem

/**
 * 构建系统提供者扩展点
 *
 * 用于提供不同的构建系统实现（如 CJPM、Gradle 等）
 */
interface CjBuildSystemProvider {
    companion object {
        val EP_NAME = ExtensionPointName<CjBuildSystemProvider>(
            "org.cangnova.cangjie.build.buildSystemProvider"
        )
    }

    /**
     * 提供者名称
     */
    val providerName: String

    /**
     * 提供者优先级
     */
    val priority: Int
        get() = 100

    /**
     * 检查是否可以处理指定的项目
     *
     * @param projectDir 项目根目录
     * @return 如果可以处理返回 true
     */
    fun canHandle(projectDir: VirtualFile): Boolean

    /**
     * 创建构建系统实例
     *
     * @param projectDir 项目根目录
     * @param project IntelliJ 项目实例
     * @return 构建系统实例，如果无法创建返回 null
     */
    fun createBuildSystem(projectDir: VirtualFile, project: Project): CjBuildSystem?

    /**
     * 获取构建系统版本
     *
     * @param projectDir 项目根目录
     * @return 构建系统版本，如果无法获取返回 null
     */
    fun getBuildSystemVersion(projectDir: VirtualFile): String?
}