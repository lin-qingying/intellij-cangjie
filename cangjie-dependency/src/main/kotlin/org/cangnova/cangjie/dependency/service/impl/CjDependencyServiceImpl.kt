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

package org.cangnova.cangjie.dependency.service.impl

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.dependency.extension.CjDependencyResolver
import org.cangnova.cangjie.dependency.model.CjDependency
import org.cangnova.cangjie.dependency.model.CjResolvedDependency
import org.cangnova.cangjie.dependency.service.CjDependencyService

/**
 * 依赖服务实现
 */
@Service(Service.Level.APP)
class CjDependencyServiceImpl : CjDependencyService {

    private val log = logger<CjDependencyServiceImpl>()

    override fun resolveDependency(dependency: CjDependency, project: Project): CjResolvedDependency? {
        // 获取所有依赖解析器，按优先级排序
        val resolvers = CjDependencyResolver.EP_NAME.extensionList
            .sortedBy { it.priority }

        // 尝试使用每个解析器解析依赖
        for (resolver in resolvers) {
            if (resolver.canResolve(dependency)) {
                log.info("Using resolver: ${resolver.resolverName} for dependency: ${dependency.name}")
                val resolved = resolver.resolve(dependency, project)
                if (resolved != null && resolved.isResolved) {
                    return resolved
                }
            }
        }

        log.warn("Failed to resolve dependency: ${dependency.name}")
        return null
    }

    override fun resolveDependencies(
        dependencies: List<CjDependency>,
        project: Project
    ): List<CjResolvedDependency> {
        return dependencies.mapNotNull { resolveDependency(it, project) }
    }

    override fun resolveDependencyWithTransitive(
        dependency: CjDependency,
        project: Project
    ): List<CjResolvedDependency> {
        val result = mutableListOf<CjResolvedDependency>()
        val visited = mutableSetOf<String>()

        fun resolveRecursive(dep: CjDependency) {
            val key = "${dep.group}:${dep.name}:${dep.version}"
            if (key in visited) return
            visited.add(key)

            val resolved = resolveDependency(dep, project)
            if (resolved != null) {
                result.add(resolved)
                // 递归解析传递依赖
                if (dep.transitive) {
                    resolved.transitiveDependencies.forEach { resolveRecursive(it) }
                }
            }
        }

        resolveRecursive(dependency)
        return result
    }
}