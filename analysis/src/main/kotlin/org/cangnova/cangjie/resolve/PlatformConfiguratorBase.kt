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

import org.cangnova.cangjie.container.*
import org.cangnova.cangjie.resolve.caches.DeclarationChecker
import org.cangnova.cangjie.resolve.calls.checkers.AdditionalTypeChecker
import org.cangnova.cangjie.resolve.calls.checkers.AssignmentChecker
import org.cangnova.cangjie.resolve.calls.checkers.CallChecker
import org.cangnova.cangjie.resolve.calls.checkers.configureDefaultCheckers
import org.cangnova.cangjie.resolve.lazy.DelegationFilter
import org.cangnova.cangjie.types.DynamicTypesSettings

fun createContainer(
    id: String,
    analyzerServices: PlatformDependentAnalyzerServices,
    init: StorageComponentContainer.() -> Unit
) =
    composeContainer(id, analyzerServices.platformConfigurator.platformSpecificContainer, init)

//根据参数可扩展
abstract class PlatformConfiguratorBase(
    private val dynamicTypesSettings: DynamicTypesSettings? = null,
    private val additionalDeclarationCheckers: List<DeclarationChecker> = emptyList(),
    private val additionalCallCheckers: List<CallChecker> = emptyList(),
    private val additionalAssignmentCheckers: List<AssignmentChecker> = emptyList(),
    private val additionalTypeCheckers: List<AdditionalTypeChecker> = emptyList(),
//    private val additionalClassifierUsageCheckers: List<ClassifierUsageChecker> = emptyList(),
//    private val additionalAnnotationCheckers: List<AdditionalAnnotationChecker> = emptyList(),
    private val additionalClashResolvers: List<PlatformExtensionsClashResolver<*>> = emptyList(),
    private val identifierChecker: IdentifierChecker? = null,
    private val overloadFilter: OverloadFilter? = null,
//    private val platformToCangJieClassMapper: PlatformToCangJieClassMapper? = null,
//    private val platformSpecificCastChecker: PlatformSpecificCastChecker? = null,
    private val delegationFilter: DelegationFilter? = null,
    private val overridesBackwardCompatibilityHelper: OverridesBackwardCompatibilityHelper? = null,
    private val declarationReturnTypeSanitizer: DeclarationReturnTypeSanitizer? = null
) : PlatformConfigurator {

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
//        container.useImpl<OptInMarkerDeclarationAnnotationChecker>()
    }


    fun configureExtensionsAndCheckers(container: StorageComponentContainer) {
        with(container) {
            useInstanceIfNotNull(dynamicTypesSettings)
            additionalDeclarationCheckers.forEach { useInstance(it) }
            additionalCallCheckers.forEach { useInstance(it) }
            additionalAssignmentCheckers.forEach { useInstance(it) }
            additionalTypeCheckers.forEach { useInstance(it) }
//            additionalClassifierUsageCheckers.forEach { useInstance(it) }
//            additionalAnnotationCheckers.forEach { useInstance(it) }
            additionalClashResolvers.forEach { useClashResolver(it) }
            useInstanceIfNotNull(identifierChecker)
            useInstanceIfNotNull(overloadFilter)
//            useInstanceIfNotNull(platformToCangJieClassMapper)
//            useInstanceIfNotNull(platformSpecificCastChecker)
            useInstanceIfNotNull(delegationFilter)
            useInstanceIfNotNull(overridesBackwardCompatibilityHelper)
            useInstanceIfNotNull(declarationReturnTypeSanitizer)
        }
    }

    override val platformSpecificContainer = composeContainer(this::class.java.simpleName) {
        configureDefaultCheckers()
        configureExtensionsAndCheckers(this)
    }

}

object CangJiePlatformConfigurator : PlatformConfiguratorBase() {

    override fun configureModuleComponents(container: StorageComponentContainer) {

    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
//        container.useImpl<OptInMarkerDeclarationAnnotationChecker>()
    }


}
