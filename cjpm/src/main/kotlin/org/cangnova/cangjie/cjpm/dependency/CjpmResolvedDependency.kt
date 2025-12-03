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

import org.cangnova.cangjie.model.CjDependency
import org.cangnova.cangjie.model.CjLibrary
import org.cangnova.cangjie.model.CjPackage
import org.cangnova.cangjie.model.CjResolvedDependency

/**
 * CJPM 已解析依赖实现
 */
data class CjpmResolvedDependency(
    override val dependency: CjDependency,
    override val resolvedPackage: CjPackage? = null,
    override val resolvedLibrary: CjLibrary? = null,
    override val transitiveDependencies: List<CjDependency> = emptyList(),
    override val errorMessage: String? = null
) : CjResolvedDependency {
    /**
     * 构建库的唯一名称
     *
     * 格式：group:name:version 或 name:version
     */
    override fun buildLibraryName(dependency: CjDependency): String {
        return when (dependency) {
            is CjDependency.Library -> {
                if (dependency.group != null) {
                    "Cjpm: ${dependency.group}:${dependency.name}:${dependency.version}"
                } else {
                    "Cjpm: ${dependency.name}:${dependency.version}"
                }
            }

            is CjDependency.Git -> {
                "Cjpm: ${dependency.name}:${dependency.version} (git)"
            }

            is CjDependency.System -> {
                "${dependency.name}:${dependency.version} (system)"
            }

            is CjDependency.Stdlib -> {
                dependency.id
            }

            else -> {
                "Cjpm: ${dependency.name}:${dependency.version}"
            }
        }
    }

    companion object {
        /**
         * 创建一个解析成功的依赖（包）
         */
        fun resolvedAsPackage(
            dependency: CjDependency,
            pkg: CjPackage,
            transitive: List<CjDependency> = emptyList()
        ): CjpmResolvedDependency {
            return CjpmResolvedDependency(
                dependency = dependency,
                resolvedPackage = pkg,
                transitiveDependencies = transitive
            )
        }

        /**
         * 创建一个解析成功的依赖（库）
         */
        fun resolvedAsLibrary(
            dependency: CjDependency,
            library: CjLibrary,
            transitive: List<CjDependency> = emptyList()
        ): CjpmResolvedDependency {
            return CjpmResolvedDependency(
                dependency = dependency,
                resolvedLibrary = library,
                transitiveDependencies = transitive
            )
        }

        /**
         * 创建一个解析失败的依赖
         */
        fun failed(
            dependency: CjDependency,
            errorMessage: String
        ): CjpmResolvedDependency {
            return CjpmResolvedDependency(
                dependency = dependency,
                errorMessage = errorMessage
            )
        }
    }
}