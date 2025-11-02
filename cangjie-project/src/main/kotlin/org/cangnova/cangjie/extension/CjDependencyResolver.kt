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

package org.cangnova.cangjie.extension

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.model.CjDependency
import org.cangnova.cangjie.model.CjResolvedDependency
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId

/**
 * 依赖解析器扩展点
 *
 * 负责解析特定类型的依赖
 */
interface CjDependencyResolver {
    companion object {
        val EP_NAME = ExtensionPointName<CjDependencyResolver>(
            "org.cangnova.cangjie.dependency.dependencyResolver"
        )
    }

    /**
     * 获取此解析器关联的构建系统 ID
     *
     * @return 构建系统 ID
     */
    fun getBuildSystemId(): ProjectBuildSystemId



    /**
     * 判断是否可以解析该依赖
     *
     * @param dependency 待解析的依赖
     * @return 如果可以解析返回 true
     */
    fun canResolve(dependency: CjDependency): Boolean

    /**
     * 解析依赖，返回依赖的详细信息
     *
     * @param dependency 待解析的依赖
     * @param project IntelliJ 项目实例
     * @return 解析后的依赖，如果解析失败返回 null
     */
    fun resolve(dependency: CjDependency, project: Project): CjResolvedDependency?

    /**
     * 解析传递依赖
     *
     * @param dependency 待解析的依赖
     * @param project IntelliJ 项目实例
     * @return 传递依赖列表
     */
    fun resolveTransitive(dependency: CjDependency, project: Project): List<CjDependency> {
        return emptyList()
    }

    /**
     * 解析器名称
     */
    val resolverName: String
}