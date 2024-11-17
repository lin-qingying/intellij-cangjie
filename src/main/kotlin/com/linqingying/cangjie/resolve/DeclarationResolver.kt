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

package com.linqingying.cangjie.resolve

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.enumd.EnumEntryDescriptor
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.diagnostics.Errors.REDECLARATION
import com.linqingying.cangjie.diagnostics.reportOnDeclaration
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjPackageDirective
import com.linqingying.cangjie.resolve.lazy.TopLevelDescriptorProvider
import com.linqingying.cangjie.utils.addIfNotNull

class DeclarationResolver(
    private val annotationResolver: AnnotationResolver,
    private val trace: BindingTrace
) {
    private fun getTopLevelDescriptorsByFqName(
        topLevelDescriptorProvider: TopLevelDescriptorProvider,
        fqName: FqName,
        location: LookupLocation
    ): Set<DeclarationDescriptor> {
        val descriptors = HashSet<DeclarationDescriptor>()

        descriptors.addIfNotNull(topLevelDescriptorProvider.getPackageFragment(fqName))
        descriptors.addAll(topLevelDescriptorProvider.getTopLevelClassifierDescriptors(fqName, location))
        return descriptors
    }

    private fun reportRedeclarationsWithClassifiers(descriptorMap: Multimap<Name, DeclarationDescriptor>) {
        for (name in descriptorMap.keySet()) {
            val descriptors = descriptorMap[name]
            if (descriptors.size > 1 && descriptors.any { it is ClassifierDescriptor }) {
                for (descriptor in descriptors) {
                    reportOnDeclaration(trace, descriptor) { REDECLARATION.on(it, descriptors) }
                }
            }
        }
    }

    fun checkRedeclarations(c: TopDownAnalysisContext) {
        for (classDescriptor in c.declaredClasses.values) {
            val descriptorMap = HashMultimap.create<Name, DeclarationDescriptor>()
            for (desc in classDescriptor.unsubstitutedMemberScope.getContributedDescriptors()) {
                if ((desc is ClassifierDescriptor || desc is PropertyDescriptor || desc is VariableDescriptor) && desc !is EnumEntryDescriptor) {
                    descriptorMap.put(desc.name, desc)
                }
            }

            reportRedeclarationsWithClassifiers(descriptorMap)
        }
    }

    fun checkRedeclarationsInPackages(
        topLevelDescriptorProvider: TopLevelDescriptorProvider,
        topLevelFqNames: Multimap<FqName, CjElement>
    ) {
        for ((fqName, declarationsOrPackageDirectives) in topLevelFqNames.asMap()) {
            if (fqName.isRoot) continue

            // TODO: report error on expected class and actual val, or vice versa
            val (expected, actual) =
                getTopLevelDescriptorsByFqName(
                    topLevelDescriptorProvider,
                    fqName,
                    NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS
                )
                    .partition { it is MemberDescriptor && it.isExpect }

            for (descriptors in listOf(expected, actual)) {
                if (descriptors.size > 1) {
                    for (directive in declarationsOrPackageDirectives) {
                        val reportAt = (directive as? CjPackageDirective)?.nameIdentifier ?: directive
                        trace.report(
                            Errors.PACKAGE_OR_CLASSIFIER_REDECLARATION.on(
                                reportAt,
                                fqName.shortName().asString()
                            )
                        )
                    }
                }
            }
        }
    }
}
