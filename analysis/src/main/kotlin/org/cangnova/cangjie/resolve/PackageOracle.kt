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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.AnalysisContext
import org.cangnova.cangjie.name.FqName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.resolve.caches.PerModulePackageCacheService


interface PackageOracle {
    fun packageExists(fqName: FqName): Boolean

    object Optimistic : PackageOracle {
        override fun packageExists(fqName: FqName): Boolean = true
    }
}

interface PackageOracleFactory {
    fun createOracle(context: AnalysisContext): PackageOracle

    object OptimisticFactory : PackageOracleFactory {
        override fun createOracle(context: AnalysisContext) = PackageOracle.Optimistic
    }
}

@Service(Service.Level.PROJECT)
class IdePackageOracleFactory(val project: Project) : PackageOracleFactory {
    override fun createOracle(context: AnalysisContext): PackageOracle {
        // 只为源码上下文创建详细的 Oracle
        return if (context.isSourceContext) {
            CangJieSourceFilesOracle(context, project)
        } else {
            // 对于库和其他类型，使用乐观策略
            PackageOracle.Optimistic
        }
    }


    private class CangJieSourceFilesOracle(
        private val context: AnalysisContext,
        private val project: Project
    ) : PackageOracle {
        private val cacheService: PerModulePackageCacheService = project.service()

        override fun packageExists(fqName: FqName): Boolean {
            // 在当前上下文及其依赖中查找包
            // TODO: 可能需要递归查找依赖
            return cacheService.packageExists(fqName, context)
        }
    }


}
