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
import com.intellij.openapi.vfs.VirtualFile

/**
 * 项目配置解析器扩展点
 *
 * 负责解析特定格式的项目配置文件 (如 manifest.toml, cjpm.toml 等)
 */
interface CjProjectConfigParser {
    companion object {
        val EP_NAME = ExtensionPointName<CjProjectConfigParser>(
            "org.cangnova.cangjie.project.configParser"
        )
    }

    /**
     * 获取此解析器关联的构建系统 ID
     *
     * @return 构建系统 ID
     */
    fun getBuildSystemId(): ProjectBuildSystemId


    /**
     * 判断是否可以解析该配置文件
     *
     * @param configFile 配置文件
     * @return 如果可以解析返回 true
     */
    fun canParse(configFile: VirtualFile): Boolean

    /**
     * 解析配置文件
     *
     * @param configFile 配置文件
     * @return 解析后的配置对象，如果解析失败返回 null
     */
    fun parse(configFile: VirtualFile): Any?

    /**
     * 解析器名称
     */
    val parserName: String
}