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

package org.cangnova.cangjie.moduleinfo

import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.TestModuleProperties
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.SmartList
import org.cangnova.cangjie.cache.cacheByClassInvalidatingOnRootModifications
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.CangJieResolveScopeEnlarger
import org.cangnova.cangjie.utils.addIfNotNull

data class ModuleTestSourceInfo internal constructor(
    override val module: Module
) : ModuleSourceInfoWithExpectedBy(forProduction = false), IdeaModuleInfo {
    override val name: Name
        get() = Name.special("<test sources for module ${module.name}>")

    override val displayedName: String
        get() = KotlinBaseProjectStructureBundle.message("module.name.0.test", module.name)


    override val stableName: Name by lazy { module.stableName }

    override val contentScope: GlobalSearchScope
        get() = CangJieResolveScopeEnlarger.enlargeScope(module.kotlinTestSourceScope, module, isTestScope = true)

    private val Module.kotlinTestSourceScope: GlobalSearchScope
        get() = ModuleSourcesScope.tests(module)

    override fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> =
        module.cacheByClassInvalidatingOnRootModifications(KeyForModulesWhoseInternalsAreVisible::class.java) {
            val list = SmartList<ModuleInfo>()

            list.addIfNotNull(module.productionSourceInfo)

            TestModuleProperties.getInstance(module).productionModule?.let {
                list.addIfNotNull(it.productionSourceInfo)
            }

            list.addAll(module.additionalVisibleModules.mapNotNull { additionalVisibleModule ->
                additionalVisibleModule.productionSourceInfo ?:
                // we should consider `testFixture` as an additional visible module for test sources
                module.testSourceInfo?.let { additionalVisibleModule.testSourceInfo }
            })

            list.toHashSet()
        }

    private object KeyForModulesWhoseInternalsAreVisible



}

val Module.additionalVisibleModules: List<Module>
    get() = cacheInvalidatingOnRootModifications cache@{
        val facetSettings = facetSettings ?: return@cache emptyList()

        val modulesByLinkedKey = project.service<ModulesByLinkedKeyCache>()

        facetSettings.additionalVisibleModuleNames.mapNotNull { moduleName ->
            modulesByLinkedKey[moduleName]
        }
    }
