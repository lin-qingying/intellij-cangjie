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

package org.cangnova.cangjie.cjpm.dependency

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.dependency.extension.CjDependencyResolver
import org.cangnova.cangjie.dependency.model.CjDependency
import org.cangnova.cangjie.dependency.model.CjResolvedDependency

/**
 * CJPM 依赖解析器实现
 */
class CjpmDependencyResolver : CjDependencyResolver {

    override val resolverName: String = "CJPM Dependency Resolver"

    override val priority: Int = 100

    override fun canResolve(dependency: CjDependency): Boolean {
        // CJPM 可以解析所有类型的依赖
        return true
    }

    override fun resolve(dependency: CjDependency, project: Project): CjResolvedDependency? {
        // TODO: 实现真实的依赖解析逻辑
        // 这里需要：
        // 1. 根据依赖信息查找包
        // 2. 下载依赖（如果需要）
        // 3. 解析依赖的元数据
        // 4. 返回解析后的依赖
        return null
    }

    override fun resolveTransitive(dependency: CjDependency, project: Project): List<CjDependency> {
        // TODO: 解析传递依赖
        // 1. 解析当前依赖
        // 2. 读取依赖的 cjpm.toml
        // 3. 递归解析其依赖
        return emptyList()
    }
}