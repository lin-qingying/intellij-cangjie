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

package org.cangnova.cangjie.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.model.CjDependency
import org.cangnova.cangjie.model.CjResolvedDependency

/**
 * 依赖服务接口
 *
 * 提供依赖解析和管理功能
 */

interface CjDependencyService {
    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(): CjDependencyService {
            return ApplicationManager.getApplication()
                .getService(CjDependencyService::class.java)
        }
    }

    /**
     * 解析单个依赖
     *
     * @param dependency 待解析的依赖
     * @param project IntelliJ 项目实例
     * @return 解析后的依赖
     */
    fun resolveDependency(dependency: CjDependency, project: Project): CjResolvedDependency?

    /**
     * 解析依赖列表
     *
     * @param dependencies 待解析的依赖列表
     * @param project IntelliJ 项目实例
     * @return 解析后的依赖列表
     */
    fun resolveDependencies(dependencies: List<CjDependency>, project: Project): List<CjResolvedDependency>

    /**
     * 解析依赖并包含传递依赖
     *
     * @param dependency 待解析的依赖
     * @param project IntelliJ 项目实例
     * @return 包含传递依赖的解析结果列表
     */
    fun resolveDependencyWithTransitive(dependency: CjDependency, project: Project): List<CjResolvedDependency>
}