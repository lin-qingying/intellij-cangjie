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
import org.cangnova.cangjie.build.model.CjBuildConfiguration

/**
 * 构建配置提供者扩展点
 *
 * 用于从不同来源加载构建配置
 */
interface CjBuildConfigurationProvider {
    companion object {
        val EP_NAME = ExtensionPointName<CjBuildConfigurationProvider>(
            "org.cangnova.cangjie.build.buildConfigurationProvider"
        )
    }

    /**
     * 提供者名称
     */
    val providerName: String


    /**
     * 检查是否可以处理指定的配置文件
     *
     * @param configFile 配置文件
     * @return 如果可以处理返回 true
     */
    fun canHandle(configFile: VirtualFile): Boolean

    /**
     * 加载构建配置
     *
     * @param configFile 配置文件
     * @param project IntelliJ 项目实例
     * @return 构建配置，如果无法加载返回 null
     */
    fun loadConfiguration(configFile: VirtualFile, project: Project): CjBuildConfiguration?
}