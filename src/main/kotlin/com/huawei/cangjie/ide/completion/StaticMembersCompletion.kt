package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.ClassKind
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.ide.CangJieIndicesHelper
import com.huawei.cangjie.ide.codeinsight.collectSyntheticStaticMembersAndConstructors
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjNamedDeclaration
import com.huawei.cangjie.resolve.ImportedFromObjectCallableDescriptor
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.descriptorUtil.targetDescriptors
import com.huawei.cangjie.resolve.isExtension
import com.huawei.cangjie.resolve.scopes.DescriptorKindExclude
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.getDescriptorsFiltered
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.utils.CallType
import com.huawei.cangjie.utils.CallTypeAndReceiver
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
        val containers = file.importDirectives.filter { !it.isAllUnder }.mapNotNull {
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
