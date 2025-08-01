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

package cn.cangnova.cangjie.resolve.deprecation

import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.FunctionDescriptor
import cn.cangnova.cangjie.psi.Call
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.storage.MemoizedFunctionToNotNull


class DeprecationResolver(
    storageManager: StorageManager,

    private val deprecationSettings: DeprecationSettings
) {

    private val deprecations: MemoizedFunctionToNotNull<DeclarationDescriptor, DeprecationInfo> =
        storageManager.createMemoizedFunction { descriptor ->
            computeDeprecation(descriptor)
        }

    private fun DeclarationDescriptor.getOwnDeprecations(): List<DescriptorBasedDeprecationInfo> {
        return emptyList()
        // The problem is that declaration `mod` in built-ins has @Deprecated annotation but actually it was deprecated only in version 1.1
//        if (isBuiltInOperatorMod && !shouldWarnAboutDeprecatedModFromBuiltIns(languageVersionSettings)) {
//            return emptyList()
//        }
//

//
//        val result = SmartList<DescriptorBasedDeprecationInfo>()
//
//        addDeprecationIfPresent(result)
//
//        when (this) {
//            is TypeAliasDescriptor -> expandedType.deprecationsByConstituentTypes().mapTo(result) { deprecation ->
//                when (deprecation) {
//                    is DeprecatedByAnnotation -> DeprecatedTypealiasByAnnotation(this, deprecation)
//                    else -> deprecation
//                }
//            }
//
//            is DescriptorDerivedFromTypeAlias ->
//                result.addAll(typeAliasDescriptor.getOwnDeprecations())
//
//            is PropertyAccessorDescriptor ->
//                correspondingProperty.addDeprecationIfPresent(result)
//        }
//
//        return result.distinct()
    }

    private fun computeDeprecation(descriptor: DeclarationDescriptor): DeprecationInfo {
        val deprecations = descriptor.getOwnDeprecations()
        return when {
            deprecations.isNotEmpty() -> DeprecationInfo(deprecations, hasInheritedDeprecations = false)

            else -> DeprecationInfo.EMPTY
        }
    }

    private data class DeprecationInfo(
        val deprecations: List<DescriptorBasedDeprecationInfo>,
        val hasInheritedDeprecations: Boolean,
        val hiddenInheritedDeprecations: List<DescriptorBasedDeprecationInfo> = emptyList()
    ) {
        companion object {
            val EMPTY = DeprecationInfo(emptyList(), hasInheritedDeprecations = false, emptyList())
        }
    }

    fun isHiddenInResolution(
        descriptor: DeclarationDescriptor,
        callElement: CjElement?,
        bindingContext: BindingContext?,
        isSuperCall: Boolean,
        fromImportingScope: Boolean
    ): Boolean {
        if (descriptor is FunctionDescriptor) {
            if (descriptor.isHiddenToOvercomeSignatureClash) return true
            if (descriptor.isHiddenForResolutionEverywhereBesideSupercalls && !isSuperCall) return true
        }

//        val sinceCangJieAccessibility = isHiddenBecauseOfCangJieVersionAccessibility(descriptor.original)
//        if (sinceCangJieAccessibility is SinceCangJieAccessibility.NotAccessible) return true
//
//        if (sinceCangJieAccessibility is SinceCangJieAccessibility.NotAccessibleButWasExperimental) {
//            return if (callElement != null && bindingContext != null) {
//                with(OptInUsageChecker) {
//                    sinceCangJieAccessibility.markerClasses.any { classDescriptor ->
//                        !callElement.isOptInAllowed(classDescriptor.fqNameSafe, languageVersionSettings, bindingContext)
//                    }
//                }
//            } else {
//                // We need a softer check for descriptors from importing scope as there is no access to PSI elements
//                // It's fine to return false here as there will be additional checks for accessibility later
//                !fromImportingScope
//            }
//        }

        return isDeprecatedHidden(descriptor)
    }

    fun getDeprecations(descriptor: DeclarationDescriptor): List<DescriptorBasedDeprecationInfo> =
        deprecations(descriptor.original).deprecations

    fun isDeprecatedHidden(descriptor: DeclarationDescriptor): Boolean =
        getDeprecations(descriptor).any { it.deprecationLevel == DeprecationLevelValue.HIDDEN }


    @JvmOverloads
    fun isHiddenInResolution(
        descriptor: DeclarationDescriptor,
        call: Call? = null,
        bindingContext: BindingContext? = null,
        isSuperCall: Boolean = false,
        fromImportingScope: Boolean = false
    ): Boolean =
        isHiddenInResolution(descriptor, call?.callElement, bindingContext, isSuperCall, fromImportingScope)

}

