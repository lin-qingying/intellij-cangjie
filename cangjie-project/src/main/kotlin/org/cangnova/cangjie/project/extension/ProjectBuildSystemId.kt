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

import com.intellij.openapi.extensions.ExtensionPointName

/**
 * 构建系统标识符接口
 *
 * 用于定义构建系统、项目模块和依赖管理的统一标识符。
 * 每个构建系统（如 CJPM）应提供唯一的 ID 和显示名称。
 */
interface ProjectBuildSystemId {

    companion object {
        /**
         * 扩展点名称，用于注册构建系统标识符
         */
        val EP_NAME: ExtensionPointName<ProjectBuildSystemId> =
            ExtensionPointName.create("org.cangnova.cangjie.project.buildSystemId")


    }

    /**
     * 构建系统的唯一标识符
     *
     * 应使用小写字母和连字符，例如 "cjpm", "gradle", "maven"
     * @return 构建系统的唯一 ID
     */
    val id: String

    /**
     * 构建系统的显示名称
     *
     * 用于在 UI 中展示，例如 "CJPM", "Gradle", "Maven"
     * @return 构建系统的显示名称
     */
    val displayName: String

    /**
     * 构建系统的描述信息
     *
     * 简要描述该构建系统的功能和特点
     * @return 构建系统的描述
     */
    val description: String get() = ""

    /**
     * 构建系统的版本信息（可选）
     *
     * @return 构建系统的版本号，如果不适用则返回 null
     */
    val version: String? get() = null
}