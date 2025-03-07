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

package cn.cangnova.cangjie.ide.completion

import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.ide.CangJieIndicesHelper
import cn.cangnova.cangjie.ide.codeinsight.collectSyntheticStaticMembersAndConstructors
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.resolve.ImportedFromObjectCallableDescriptor
import cn.cangnova.cangjie.resolve.ResolutionFacade
import cn.cangnova.cangjie.resolve.descriptorUtil.targetDescriptors
import cn.cangnova.cangjie.resolve.isExtension
import cn.cangnova.cangjie.resolve.scopes.DescriptorKindExclude
import cn.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import cn.cangnova.cangjie.resolve.scopes.getDescriptorsFiltered
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.utils.CallType
import cn.cangnova.cangjie.utils.CallTypeAndReceiver
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import java.util.ArrayList
import java.util.HashSet

class StaticMembersCompletion(
    private val prefixMatcher: PrefixMatcher,
    private val resolutionFacade: ResolutionFacade,
    private val lookupElementFactory: LookupElementFactory,
    alreadyAdded: Collection<DeclarationDescriptor>,

    ) {
    private val alreadyAdded = alreadyAdded.mapTo(HashSet()) {
        if (it is ImportedFromObjectCallableDescriptor<*>) it.callableFromObject else it
    }

    fun decoratedLookupElementFactory(itemPriority: ItemPriority): AbstractLookupElementFactory =
        object : AbstractLookupElementFactory {
            override fun createStandardLookupElementsForDescriptor(
                descriptor: DeclarationDescriptor,
                useReceiverTypes: Boolean
            ): Collection<LookupElement> {
                if (!useReceiverTypes) return emptyList()
                return lookupElementFactory.createLookupElement(descriptor, useReceiverTypes = false)
                    .decorateAsStaticMember(descriptor, classNameAsLookupString = false)
                    ?.assignPriority(itemPriority)
                    ?.suppressAutoInsertion()
                    .let(::listOfNotNull)
            }

            override fun createLookupElement(
                descriptor: DeclarationDescriptor, useReceiverTypes: Boolean,
                qualifyNestedClasses: Boolean, includeClassTypeArguments: Boolean,
                parametersAndTypeGrayed: Boolean
            ): LookupElement? = null
        }

    fun membersFromImports(file: CjFile): Collection<DeclarationDescriptor> {
        val containers = file.importDirectivesItem.filter { !it.isAllUnder }.mapNotNull {
            it.targetDescriptors(resolutionFacade).map { descriptor ->
                descriptor.containingDeclaration
            }.distinct().singleOrNull() as? ClassDescriptor
        }.toSet()

        val result = ArrayList<DeclarationDescriptor>()

        val kindFilter = DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions
        val nameFilter = prefixMatcher.asNameFilter()
        for (container in containers) {
            val memberScope = container.staticScope
            val members =
                memberScope.getDescriptorsFiltered(
                    kindFilter,
                    nameFilter
                ) + memberScope.collectSyntheticStaticMembersAndConstructors(
                    resolutionFacade,
                    kindFilter,
                    nameFilter
                )
            members.filterTo(result) { it is CallableDescriptor && it !in alreadyAdded }
        }
        return result
    }

    //TODO: filter out those that are accessible from SmartCompletion.additionalItems
    //TODO: what about enum members?
    //TODO: better presentation for lookup elements from imports too
    //TODO: from the same file

    fun processMembersFromIndices(indicesHelper: CangJieIndicesHelper, processor: (DeclarationDescriptor) -> Unit) {
        val descriptorKindFilter = DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions
        val nameFilter: (String) -> Boolean = { prefixMatcher.prefixMatches(it) }



        indicesHelper.processStaticMembers(descriptorKindFilter, nameFilter) {
            if (it !in alreadyAdded) {
                processor(it)
            }
        }

    }

    fun completeFromImports(file: CjFile, collector: LookupElementsCollector) {
        val factory = decoratedLookupElementFactory(ItemPriority.STATIC_MEMBER_FROM_IMPORTS)
        membersFromImports(file).forEach { descriptor ->
            factory.createStandardLookupElementsForDescriptor(descriptor, useReceiverTypes = true)
                .forEach(collector::addElement)
            collector.flushToResultSet()
        }
    }

    /**
     * Collects extensions declared as members in objects into [collector], for example:
     *
     * ```
     * object Obj {
     *     fun String.foo() {}
     * }
     * ```
     *
     * `foo` here is object member extension.
     */
    fun completeObjectMemberExtensionsFromIndices(
        indicesHelper: CangJieIndicesHelper,
        receiverTypes: Collection<CangJieType>,
        callTypeAndReceiver: CallTypeAndReceiver<*, CallType<out CjElement?>>,
        collector: LookupElementsCollector
    ) {
        val factory = decoratedLookupElementFactory(ItemPriority.STATIC_MEMBER)

        indicesHelper.processCallableExtensionsDeclaredInObjects(
            callTypeAndReceiver,
            receiverTypes,
            nameFilter = { prefixMatcher.prefixMatches(it) },
            processor = { descriptor ->
                if (descriptor !in alreadyAdded) {
                    factory.createStandardLookupElementsForDescriptor(descriptor, useReceiverTypes = true)
                        .forEach(collector::addElement)
                    collector.flushToResultSet()
                }
            }
        )
    }

    /**
     * Find all extension methods and properties declared in objects or inherited by objects
     * from their superclasses, and add them to the collector.
     *
     * @param indicesHelper an instance of indices helper to look up for candidate objects
     * @param receiverTypes the receiver types at the completion site
     * @param callTypeAndReceiver the type of call
     * @param collector a collector for candidates
     */
    fun completeExplicitAndInheritedMemberExtensionsFromIndices(
        indicesHelper: CangJieIndicesHelper,
        receiverTypes: Collection<CangJieType>,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        collector: LookupElementsCollector
    ) {
        val factory = decoratedLookupElementFactory(ItemPriority.STATIC_MEMBER)

        indicesHelper.processAllCallablesFromSubclassObjects(
            callTypeAndReceiver,
            receiverTypes,
            nameFilter = { prefixMatcher.prefixMatches(it) },
            processor = { callableDescriptor ->
                if (callableDescriptor.isExtension && callableDescriptor !in alreadyAdded) {
                    factory.createStandardLookupElementsForDescriptor(callableDescriptor, useReceiverTypes = true)
                        .forEach(collector::addElement)
                    collector.flushToResultSet()
                }
            }
        )
    }

    fun completeFromIndices(indicesHelper: CangJieIndicesHelper, collector: LookupElementsCollector) {
        val factory = decoratedLookupElementFactory(ItemPriority.STATIC_MEMBER)
        processMembersFromIndices(indicesHelper) { descriptor ->
            factory.createStandardLookupElementsForDescriptor(descriptor, useReceiverTypes = true)
                .forEach(collector::addElement)
            collector.flushToResultSet()
        }
    }
}
