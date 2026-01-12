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

package org.cangnova.cangjie.completion.smart

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.indices.ExpectedInfo
import org.cangnova.cangjie.completion.LookupElementFactory
import org.cangnova.cangjie.completion.decorateAsStaticMember
import org.cangnova.cangjie.indices.multipleFuzzyTypes
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.types.DefaultTypeSubstitutor
import org.cangnova.cangjie.types.fuzzyReturnType
import com.intellij.codeInsight.lookup.LookupElement
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.deprecation.isVisible

class StaticMembers(
    private val bindingContext: BindingContext,
    private val lookupElementFactory: LookupElementFactory,
    private val resolutionFacade: ResolutionFacade,
    private val moduleDescriptor: ModuleDescriptor,
    private val descriptorFilter: (DeclarationDescriptor) -> Boolean,
) {
    fun addToCollection(
        collection: MutableCollection<LookupElement>,
        expectedInfos: Collection<ExpectedInfo>,
        context: CjSimpleNameExpression,
        enumEntriesToSkip: Set<DeclarationDescriptor>
    ) {
        val expectedInfosByClass = HashMap<ClassDescriptor, MutableList<ExpectedInfo>>()
        for (expectedInfo in expectedInfos) {
            for (fuzzyType in expectedInfo.multipleFuzzyTypes) {
                val classDescriptor = fuzzyType.type.constructor.declarationDescriptor as? ClassDescriptor ?: continue
                expectedInfosByClass.getOrPut(classDescriptor) { ArrayList() }.add(expectedInfo)
            }

//            if (expectedInfo.additionalData is PropertyDelegateAdditionalData) {
//                val delegatesClass =
//                    resolutionFacade.resolveImportReference(moduleDescriptor, FqName("kotlin.properties.Delegates")).singleOrNull()
//                if (delegatesClass is ClassDescriptor) {
//                    addToCollection(
//                        collection,
//                        delegatesClass,
//                        listOf(expectedInfo),
//                        context,
//                        enumEntriesToSkip,
//                        SmartCompletionItemPriority.DELEGATES_STATIC_MEMBER
//                    )
//                }
//            }
        }

        for ((classDescriptor, expectedInfosForClass) in expectedInfosByClass) {
            if (!classDescriptor.name.isSpecial) {
                addToCollection(
                    collection,
                    classDescriptor,
                    expectedInfosForClass,
                    context,
                    enumEntriesToSkip,
                    SmartCompletionItemPriority.STATIC_MEMBER
                )
            }
        }
    }

    private fun addToCollection(
        collection: MutableCollection<LookupElement>,
        classDescriptor: ClassDescriptor,
        expectedInfos: Collection<ExpectedInfo>,
        context: CjSimpleNameExpression,
        enumEntriesToSkip: Set<DeclarationDescriptor>,
        defaultPriority: SmartCompletionItemPriority
    ) {
        fun processMember(descriptor: DeclarationDescriptor) {
            if (!descriptorFilter(descriptor)) return

            if (descriptor is DeclarationDescriptorWithVisibility && !descriptor.isVisible(
                    context,
                    null,
                    bindingContext,
                    resolutionFacade
                )
            ) return

            val matcher: (ExpectedInfo) -> ExpectedInfoMatch = when {
                descriptor is CallableDescriptor -> {
                    val returnType = descriptor.fuzzyReturnType() ?: return
                    { expectedInfo -> returnType.matchExpectedInfo(expectedInfo) }
                }

                DescriptorUtils.isEnumConstructor(descriptor) && !enumEntriesToSkip.contains(descriptor) -> {
                    /* we do not need to check type of enum entry because it's taken from proper enum */
                    { ExpectedInfoMatch.match(DefaultTypeSubstitutor.EMPTY) }
                }

                else -> return
            }

            val priority = when {
                DescriptorUtils.isEnumConstructor(descriptor) -> SmartCompletionItemPriority.ENUM_ENTRIES
                else -> defaultPriority
            }

            collection.addLookupElements(descriptor, expectedInfos, matcher) { createLookupElements(it, priority) }
        }

        classDescriptor.staticScope.getContributedDescriptors().forEach(::processMember)


        val members = classDescriptor.defaultType.memberScope.getContributedDescriptors().filter { member ->
            when (classDescriptor.kind) {
                ClassKind.ENUM -> member is ClassDescriptor // enum member

                else -> member is ClassDescriptor
            }
        }
        members.forEach(::processMember)
    }

    private fun createLookupElements(
        memberDescriptor: DeclarationDescriptor,
        priority: SmartCompletionItemPriority
    ): Collection<LookupElement> {
        return lookupElementFactory.createStandardLookupElementsForDescriptor(
            memberDescriptor,
            useReceiverTypes = false
        )
            .map {
                it.decorateAsStaticMember(memberDescriptor, classNameAsLookupString = true)!!
                    .assignSmartCompletionPriority(priority)
            }
    }
}
