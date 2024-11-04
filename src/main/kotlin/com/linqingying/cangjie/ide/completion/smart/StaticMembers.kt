package com.linqingying.cangjie.ide.completion.smart

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.ide.ExpectedInfo
import com.linqingying.cangjie.ide.completion.LookupElementFactory
import com.linqingying.cangjie.ide.completion.decorateAsStaticMember
import com.linqingying.cangjie.ide.multipleFuzzyTypes
import com.linqingying.cangjie.psi.CjSimpleNameExpression
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.ResolutionFacade
import com.linqingying.cangjie.resolve.isVisible
import com.linqingying.cangjie.types.TypeSubstitutor
import com.linqingying.cangjie.types.fuzzyReturnType
import com.intellij.codeInsight.lookup.LookupElement

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

                DescriptorUtils.isEnumEntry(descriptor) && !enumEntriesToSkip.contains(descriptor) -> {
                    /* we do not need to check type of enum entry because it's taken from proper enum */
                    { ExpectedInfoMatch.match(TypeSubstitutor.EMPTY) }
                }

                else -> return
            }

            val priority = when {
                DescriptorUtils.isEnumEntry(descriptor) -> SmartCompletionItemPriority.ENUM_ENTRIES
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
