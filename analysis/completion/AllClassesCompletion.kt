/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.getDescriptorsFiltered
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.PrefixMatcher
import org.cangnova.cangjie.indices.CangJieIndicesHelper


class AllClassesCompletion(
    private val parameters: CompletionParameters,
    private val cangjieIndicesHelper: CangJieIndicesHelper,
    private val prefixMatcher: PrefixMatcher,
    private val resolutionFacade: ResolutionFacade,
    private val kindFilter: (ClassKind) -> Boolean,
    private val includeTypeAliases: Boolean,

    ) {
    fun collect(classifierDescriptorCollector: (ClassifierDescriptorWithTypeParameters) -> Unit ) {

        //TODO: this is a temporary solution until we have built-ins in indices
        // we need only nested classes because top-level built-ins are all added through default imports
            collectClassesFromScope(resolutionFacade.moduleDescriptor.builtIns.BASIC_SCOPE) {
                if (it.containingDeclaration is ClassDescriptor) {
                    classifierDescriptorCollector(it)
                }
        }

        cangjieIndicesHelper.processCangJieClasses(
            { prefixMatcher.prefixMatches(it) },
            kindFilter = kindFilter,
            processor = classifierDescriptorCollector
        )

        if (includeTypeAliases) {
            cangjieIndicesHelper.processTopLevelTypeAliases(prefixMatcher.asStringNameFilter(), classifierDescriptorCollector)
        }


    }

    private fun collectClassesFromScope(scope: MemberScope, collector: (ClassDescriptor) -> Unit) {
        for (descriptor in scope.getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS)) {
            if (descriptor is ClassDescriptor) {
                if (kindFilter(descriptor.kind) && prefixMatcher.prefixMatches(descriptor.name.asString())) {
                    collector(descriptor)
                }

                collectClassesFromScope(descriptor.unsubstitutedMemberScope, collector)
            }
        }
    }




}
