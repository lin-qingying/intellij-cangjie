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
import org.cangnova.cangjie.project.service.CjProjectBuildSystemService
import org.cangnova.cangjie.project.wizard.CjModuleBuilder

/**
 * 模块构建器提供者接口
 *
 * 用于提供创建新项目的 ModuleBuilder 实例。
 * 不同的构建系统（如 CJPM）可以实现此接口来提供自己的 ModuleBuilder。
 */
interface CjModuleBuilderProvider {

    companion object {
        val EP_NAME = ExtensionPointName<CjModuleBuilderProvider>(
            "org.cangnova.cangjie.project.moduleBuilderProvider"
        )


        fun getModuleBuilderProvider(): CjModuleBuilderProvider? {

            val buildSystemId = CjProjectBuildSystemService.getInstance().getBuildSystem()?.id


            return EP_NAME.extensionList.find {

                it.getBuildSystemId().id == buildSystemId
            }

        }

    }

    /**
     * 获取此解析器关联的构建系统 ID
     *
     * @return 构建系统 ID
     */
    fun getBuildSystemId(): ProjectBuildSystemId

    /**
     * 提供者名称
     */
    val providerName: String


    /**
     * 创建 ModuleBuilder 实例
     *
     * @return ModuleBuilder 实例，如果无法创建则返回 null
     */
    fun createModuleBuilder(): CjModuleBuilder?
}