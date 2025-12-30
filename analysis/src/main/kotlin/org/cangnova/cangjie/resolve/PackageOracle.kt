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

import org.cangnova.cangjie.name.FqName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo
import org.cangnova.cangjie.moduleinfo.ModuleInfo
import org.cangnova.cangjie.moduleinfo.ModuleOrigin
import org.cangnova.cangjie.moduleinfo.ModuleSourceInfo
import org.cangnova.cangjie.moduleinfo.util.projectSourceModules
import org.cangnova.cangjie.name.isSubpackageOf
import org.cangnova.cangjie.psi.CangJiePsiFacade
import org.cangnova.cangjie.resolve.caches.PerModulePackageCacheService


interface PackageOracle {
    fun packageExists(fqName: FqName): Boolean

    object Optimistic : PackageOracle {
        override fun packageExists(fqName: FqName): Boolean = true
    }
}

interface PackageOracleFactory {
    fun createOracle(context: ModuleInfo): PackageOracle

    object OptimisticFactory : PackageOracleFactory {
        override fun createOracle(context: ModuleInfo) = PackageOracle.Optimistic
    }
}

@Service(Service.Level.PROJECT)
class IdePackageOracleFactory(val project: Project) : PackageOracleFactory {
    override fun createOracle(context: ModuleInfo): PackageOracle {
        if (context !is IdeaModuleInfo) return PackageOracle.Optimistic

        return when (context.moduleOrigin) {
            ModuleOrigin.LIBRARY -> PackagesOracle(context, project)
            ModuleOrigin.MODULE -> SourceOracle(context, project)
            ModuleOrigin.OTHER -> PackageOracle.Optimistic
        }
    }

    private class SourceOracle(moduleInfo: IdeaModuleInfo, project: Project) : PackageOracle {
        private val packagesOracle = PackagesOracle(moduleInfo, project)
        private val sourceOracle = CangJieSourceFilesOracle(moduleInfo, project)

        override fun packageExists(fqName: FqName): Boolean =
            packagesOracle.packageExists(fqName)
                    || sourceOracle.packageExists(fqName)
        /*  || fqName.isSubpackageOf(ANDROID_SYNTHETIC_PACKAGE_PREFIX)*/
    }

    private class PackagesOracle(moduleInfo: IdeaModuleInfo, project: Project) : PackageOracle {
        private val scope: GlobalSearchScope = moduleInfo.contentScope
        private val facade: CangJiePsiFacade =CangJiePsiFacade.getInstance(project)

        override fun packageExists(fqName: FqName): Boolean = facade.findPackage(fqName, scope) != null
    }

    private class CangJieSourceFilesOracle(
        private val context: IdeaModuleInfo,
        private val project: Project
    ) : PackageOracle {
        private val cacheService: PerModulePackageCacheService = project.service()
        private val sourceModules: List<ModuleSourceInfo> = context.projectSourceModules()

        override fun packageExists(fqName: FqName): Boolean {
            return sourceModules.any { cacheService.packageExists(fqName, it) }
        }
    }


}
