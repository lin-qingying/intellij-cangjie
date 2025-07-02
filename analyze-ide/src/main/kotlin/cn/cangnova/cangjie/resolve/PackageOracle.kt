/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.descriptors.CangJieModuleInfo
import cn.cangnova.cangjie.descriptors.ModuleInfo
import cn.cangnova.cangjie.descriptors.ModuleOrigin
import cn.cangnova.cangjie.descriptors.projectSourceModules
import cn.cangnova.cangjie.ide.cache.PerModulePackageCacheService
import cn.cangnova.cangjie.name.FqName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project


interface PackageOracle {
    fun packageExists(fqName: FqName): Boolean

    object Optimistic : PackageOracle {
        override fun packageExists(fqName: FqName): Boolean = true
    }
}

interface PackageOracleFactory {
    fun createOracle(moduleInfo: ModuleInfo): PackageOracle

    object OptimisticFactory : PackageOracleFactory {
        override fun createOracle(moduleInfo: ModuleInfo) = PackageOracle.Optimistic
    }
}
@Service(Service.Level.PROJECT)
class IdePackageOracleFactory(val project: Project) : PackageOracleFactory {
    override fun createOracle(moduleInfo: ModuleInfo): PackageOracle {
        if (moduleInfo !is CangJieModuleInfo) return PackageOracle.Optimistic

        return when (moduleInfo.moduleOrigin) {
            ModuleOrigin.MODULE -> CangJieSourceFilesOracle(moduleInfo, project)
            else -> PackageOracle.Optimistic // binaries for non-jvm platform need some oracles based on their structure
        }

    }


    private class CangJieSourceFilesOracle(moduleInfo: ModuleInfo, private val project: Project) : PackageOracle {
        private val cacheService: PerModulePackageCacheService = project.service()
        private val sourceModules = moduleInfo.projectSourceModules()
        override fun packageExists(fqName: FqName): Boolean {
            return sourceModules.any { cacheService.packageExists(fqName, it) }
        }
    }


}
