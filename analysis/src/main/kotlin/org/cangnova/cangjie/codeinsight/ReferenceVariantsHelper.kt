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

package org.cangnova.cangjie.codeinsight

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.CangJieLookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.parentsWithSelf
import org.cangnova.cangjie.psi.psiUtil.startOffset
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.calls.smartcasts.SmartCastManager
import org.cangnova.cangjie.resolve.calls.util.getSmartCastVariantsWithLessSpecificExcluded
import org.cangnova.cangjie.resolve.calls.util.receiverTypes
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.resolve.calls.util.CallType
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import org.cangnova.cangjie.utils.getImplicitReceiversWithInstance
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.FrontendInternals
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.getDataFlowInfoBefore
import org.cangnova.cangjie.utils.ShadowedDeclarationsFilter
import org.cangnova.cangjie.utils.isExtension
import org.cangnova.cangjie.utils.substituteExtensionIfCallable

fun DeclarationDescriptor.findPsi(): PsiElement? {
    val psi = (this as? DeclarationDescriptorWithSource)?.source?.getPsi()
    return if (psi == null && this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        overriddenDescriptors.mapNotNull { it.findPsi() }.firstOrNull()
    } else {
        psi
    }
}

@OptIn(FrontendInternals::class)
class ReferenceVariantsHelper(
    private val bindingContext: BindingContext,
    private val resolutionFacade: ResolutionFacade,
    private val moduleDescriptor: ModuleDescriptor,
    private val visibilityFilter: (DeclarationDescriptor) -> Boolean,
    private val notProperties: Set<FqNameUnsafe> = setOf()
) {
    fun getReferenceVariants(
        expression: CjSimpleNameExpression,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,

        filterOutShadowed: Boolean = true,
        excludeNonInitializedVariable: Boolean = true,
        useReceiverType: CangJieType? = null
    ): Collection<DeclarationDescriptor> = getReferenceVariants(
        expression, CallTypeAndReceiver.detect(expression),
        kindFilter, nameFilter, filterOutShadowed, excludeNonInitializedVariable, useReceiverType
    )

    fun getReferenceVariants(
        contextElement: PsiElement,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,

        filterOutShadowed: Boolean = true,
        excludeNonInitializedVariable: Boolean = true,
        useReceiverType: CangJieType? = null
    ): Collection<DeclarationDescriptor> {
        var variants: Collection<DeclarationDescriptor> =
            getReferenceVariantsNoVisibilityFilter(
                contextElement,
                kindFilter,
                nameFilter,
                callTypeAndReceiver,
                useReceiverType
            )
                .filter {
                    !resolutionFacade.frontendService<DeprecationResolver>()
                        .isHiddenInResolution(it) && visibilityFilter(it)
                }

        if (filterOutShadowed) {
            ShadowedDeclarationsFilter.create(bindingContext, resolutionFacade, contextElement, callTypeAndReceiver)
                ?.let {
                    variants = it.filter(variants)
                }
        }


        if (excludeNonInitializedVariable && kindFilter.kindMask.and(DescriptorKindFilter.VARIABLES_MASK) != 0) {
            variants = excludeNonInitializedVariable(variants, contextElement)
        }

        return variants
    }

    private fun getVariantsForImportOrPackageDirective(
        receiverExpression: CjExpression?,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (receiverExpression != null) {
            val qualifier = bindingContext[BindingContext.QUALIFIER, receiverExpression] ?: return emptyList()
            val staticDescriptors = qualifier.staticScope.collectStaticMembers(resolutionFacade, kindFilter, nameFilter)

//            val objectDescriptor =
//                (qualifier as? ClassQualifier)?.descriptor?.takeIf { it.kind == ClassKind.OBJECT } ?: return staticDescriptors

            return staticDescriptors /*+ objectDescriptor.defaultType.memberScope.getDescriptorsFiltered(kindFilter, nameFilter)*/
        } else {
            val rootPackage = resolutionFacade.moduleDescriptor.getPackage(FqName.ROOT)
            return rootPackage.memberScope.getDescriptorsFiltered(kindFilter, nameFilter)
        }
    }


    private fun getReferenceVariantsNoVisibilityFilter(
        contextElement: PsiElement,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        useReceiverType: CangJieType?
    ): Collection<DeclarationDescriptor> {
        val callType = callTypeAndReceiver.callType

        @Suppress("NAME_SHADOWING")
        val kindFilter = kindFilter.intersect(callType.descriptorKindFilter)

        val receiverExpression: CjExpression?
        when (callTypeAndReceiver) {
            is CallTypeAndReceiver.IMPORT_DIRECTIVE -> {
                return getVariantsForImportOrPackageDirective(callTypeAndReceiver.receiver, kindFilter, nameFilter)
            }

            is CallTypeAndReceiver.PACKAGE_DIRECTIVE -> {
                return getVariantsForImportOrPackageDirective(callTypeAndReceiver.receiver, kindFilter, nameFilter)
            }

            is CallTypeAndReceiver.TYPE -> {
                return getVariantsForUserType(callTypeAndReceiver.receiver, contextElement, kindFilter, nameFilter)
            }

            is CallTypeAndReceiver.ANNOTATION -> {
                return getVariantsForUserType(callTypeAndReceiver.receiver, contextElement, kindFilter, nameFilter)
            }

            is CallTypeAndReceiver.CALLABLE_REFERENCE -> {
                return emptyList()
            }

            is CallTypeAndReceiver.DEFAULT -> receiverExpression = null
            is CallTypeAndReceiver.DOT -> receiverExpression = callTypeAndReceiver.receiver
            is CallTypeAndReceiver.SUPER_MEMBERS -> receiverExpression = callTypeAndReceiver.receiver
            is CallTypeAndReceiver.SAFE -> receiverExpression = callTypeAndReceiver.receiver

            is CallTypeAndReceiver.OPERATOR -> return emptyList()
            is CallTypeAndReceiver.UNKNOWN -> return emptyList()
            else -> throw RuntimeException()
        }

        val resolutionScope = contextElement.getResolutionScope(bindingContext, resolutionFacade)
        val dataFlowInfo = bindingContext.getDataFlowInfoBefore(contextElement)
        val containingDeclaration = resolutionScope.ownerDescriptor

        val smartCastManager = resolutionFacade.frontendService<SmartCastManager>()
        val languageVersionSettings = resolutionFacade.frontendService<LanguageVersionSettings>()

        val implicitReceiverTypes = resolutionScope.getImplicitReceiversWithInstance(

        ).flatMap {
            smartCastManager.getSmartCastVariantsWithLessSpecificExcluded(
                it.value,
                bindingContext,
                containingDeclaration,
                dataFlowInfo,
                languageVersionSettings,
                resolutionFacade.frontendService()
            )
        }.toSet()

        val descriptors = LinkedHashSet<DeclarationDescriptor>()

        val filterWithoutExtensions = kindFilter exclude DescriptorKindExclude.Extensions
        if (receiverExpression != null) {
            val qualifier = bindingContext[BindingContext.QUALIFIER, receiverExpression]
            if (qualifier != null) {
                descriptors.addAll(
                    qualifier.staticScope.collectStaticMembers(
                        resolutionFacade,
                        filterWithoutExtensions,
                        nameFilter
                    )
                )
            } else {

                val explicitReceiverTypes = if (useReceiverType != null) {
                    listOf(useReceiverType)
                } else {
                    callTypeAndReceiver.receiverTypes(
                        bindingContext,
                        contextElement,
                        moduleDescriptor,
                        resolutionFacade,
                        stableSmartCastsOnly = false
                    )!!
                }

                descriptors.processAll(
                    implicitReceiverTypes,
                    explicitReceiverTypes,
                    resolutionScope,
                    callType,
                    kindFilter,
                    nameFilter
                )
            }

        } else {
            assert(useReceiverType == null) { "'useReceiverType' parameter is not supported for implicit receiver" }

            descriptors.processAll(
                implicitReceiverTypes,
                implicitReceiverTypes,
                resolutionScope,
                callType,
                kindFilter,
                nameFilter
            )

            // add non-instance members
            descriptors.addAll(
                resolutionScope.collectDescriptorsFiltered(
                    filterWithoutExtensions,
                    nameFilter,
                    changeNamesForAliased = true
                )
            )
            descriptors.addAll(resolutionScope.collectAllFromMeAndParent { scope ->
                scope.collectSyntheticStaticMembersAndConstructors(resolutionFacade, kindFilter, nameFilter)
            })
        }

        if (callType == CallType.SUPER_MEMBERS) { // we need to unwrap fake overrides in case of "super." because ShadowedDeclarationsFilter does not work correctly
            return descriptors.flatMapTo(LinkedHashSet<DeclarationDescriptor>()) {
                if (it is CallableMemberDescriptor && it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
                    it.overriddenDescriptors
                else
                    listOf(it)
            }
        }

        return descriptors
    }

    private fun getVariantsForUserType(
        receiverExpression: CjExpression?,
        contextElement: PsiElement,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (receiverExpression != null) {
            val qualifier = bindingContext[BindingContext.QUALIFIER, receiverExpression] ?: return emptyList()
            return qualifier.staticScope.collectStaticMembers(resolutionFacade, kindFilter, nameFilter)
        } else {
            val scope = contextElement.getResolutionScope(bindingContext, resolutionFacade)
            return scope.collectDescriptorsFiltered(kindFilter, nameFilter, changeNamesForAliased = true)
        }
    }

    private fun MutableSet<DeclarationDescriptor>.addScopeAndSyntheticExtensions(
        scope: LexicalScope,
        receiverTypes: Collection<CangJieType>,
        callType: CallType<*>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ) {
        if (kindFilter.excludes.contains(DescriptorKindExclude.Extensions)) return
        if (receiverTypes.isEmpty()) return

        fun process(extensionOrSyntheticMember: CallableDescriptor) {
            if (kindFilter.accepts(extensionOrSyntheticMember) && nameFilter(extensionOrSyntheticMember.name)) {
                if (extensionOrSyntheticMember.isExtension) {
                    addAll(extensionOrSyntheticMember.substituteExtensionIfCallable(receiverTypes, callType))
                } else {
                    add(extensionOrSyntheticMember)
                }
            }
        }

        for (descriptor in scope.collectDescriptorsFiltered(
            kindFilter exclude DescriptorKindExclude.NonExtensions,
            nameFilter,
            changeNamesForAliased = true
        )) {
            // todo: sometimes resolution scope here is LazyJavaClassMemberScope. see ea.jetbrains.com/browser/ea_problems/72572
            process(descriptor as CallableDescriptor)
        }

        val syntheticScopes = resolutionFacade.getFrontendService(SyntheticScopes::class.java).forceEnableSamAdapters()
        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            val lookupLocation =
                (scope.ownerDescriptor.toSourceElement.getPsi() as? CjElement)?.let { CangJieLookupLocation(it) }
                    ?: NoLookupLocation.FROM_IDE

            for (extension in syntheticScopes.collectSyntheticExtensionProperties(receiverTypes, lookupLocation)) {
                process(extension)
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            for (syntheticMember in syntheticScopes.collectSyntheticMemberFunctions(receiverTypes)) {
                process(syntheticMember)
            }
        }
    }

    private fun MutableSet<DeclarationDescriptor>.processAll(
        implicitReceiverTypes: Collection<CangJieType>,
        receiverTypes: Collection<CangJieType>,
        resolutionScope: LexicalScope,
        callType: CallType<*>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ) {
        addNonExtensionMembers(receiverTypes, kindFilter, nameFilter, constructorFilter = { false })
        addMemberExtensions(implicitReceiverTypes, receiverTypes, callType, kindFilter, nameFilter)
        addScopeAndSyntheticExtensions(resolutionScope, receiverTypes, callType, kindFilter, nameFilter)

        filtration()

    }

    private fun MutableSet<DeclarationDescriptor>.filtration() {
//        过滤掉操作符函数
        removeIf { it is FunctionDescriptor && it.isOperator }
    }

    private fun MutableSet<DeclarationDescriptor>.addNonExtensionCallablesAndConstructors(
        scope: HierarchicalScope,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        constructorFilter: (ClassDescriptor) -> Boolean,
        classesOnly: Boolean
    ) {
        var filterToUse =
            DescriptorKindFilter(kindFilter.kindMask and DescriptorKindFilter.CALLABLES.kindMask).exclude(
                DescriptorKindExclude.Extensions
            )

        // should process classes if we need constructors
        if (filterToUse.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            filterToUse = filterToUse.withKinds(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK)
        }

        for (descriptor in scope.collectDescriptorsFiltered(filterToUse, nameFilter, changeNamesForAliased = true)) {
            if (descriptor is ClassDescriptor) {
                if (descriptor.modality == Modality.ABSTRACT || descriptor.modality == Modality.SEALED) continue
                if (!constructorFilter(descriptor)) continue
                descriptor.constructors.filterTo(this) { kindFilter.accepts(it) }
            } else if (!classesOnly && kindFilter.accepts(descriptor)) {
                this.add(descriptor)
            }
        }
    }

    private fun MutableSet<DeclarationDescriptor>.addNonExtensionMembers(
        memberScope: MemberScope,
        typeConstructor: TypeConstructor,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        constructorFilter: (ClassDescriptor) -> Boolean
    ) {
        addNonExtensionCallablesAndConstructors(
            memberScope.memberScopeAsImportingScope(),
            kindFilter, nameFilter, constructorFilter,
            false
        )
        typeConstructor.supertypes.forEach {
            addNonExtensionCallablesAndConstructors(
                it.memberScope.memberScopeAsImportingScope(),
                kindFilter, nameFilter, constructorFilter,
                true
            )
        }
    }

    private fun MutableSet<DeclarationDescriptor>.addNonExtensionMembers(
        receiverTypes: Collection<CangJieType>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        constructorFilter: (ClassDescriptor) -> Boolean
    ) {
        for (receiverType in receiverTypes) {
            addNonExtensionMembers(
                receiverType.memberScope,
                receiverType.constructor,
                kindFilter,
                nameFilter,
                constructorFilter
            )
//           TODO 是否需要添加静态成员
//            addNonExtensionMembers(receiverType.staticMemberScope, receiverType.constructor, kindFilter, nameFilter, constructorFilter)


        }
    }

    private fun MutableSet<DeclarationDescriptor>.addMemberExtensions(
        dispatchReceiverTypes: Collection<CangJieType>,
        extensionReceiverTypes: Collection<CangJieType>,
        callType: CallType<*>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ) {
        val memberFilter = kindFilter exclude DescriptorKindExclude.NonExtensions
        for (dispatchReceiverType in dispatchReceiverTypes) {
            for (member in dispatchReceiverType.memberScope.getDescriptorsFiltered(memberFilter, nameFilter)) {
                addAll((member as CallableDescriptor).substituteExtensionIfCallable(extensionReceiverTypes, callType))
            }
        }
    }


    // filters out variable inside its initializer
    fun excludeNonInitializedVariable(
        variants: Collection<DeclarationDescriptor>,
        contextElement: PsiElement
    ): Collection<DeclarationDescriptor> {
        for (element in contextElement.parentsWithSelf) {
            val parent = element.parent
            if (parent is CjVariableDeclaration && element == parent.initializer) {
                return variants.filter { it.findPsi() != parent }
            } else if (element is CjParameter) {
                // Filter out parameters initialized after the current parameter. For example
                // ```
                // fun test(a: Int = <caret>, b: Int) {}
                // ```
                // `b` should not show up in completion.
                return variants.filter {
                    val candidatePsi = it.findPsi()
                    if (candidatePsi is CjParameter && candidatePsi.parent == parent) {
                        return@filter candidatePsi.startOffset < element.startOffset
                    }
                    true
                }
            }
            if (element is CjDeclaration) break // we can use variable inside lambda or anonymous object located in its initializer
        }
        return variants
    }

}

@OptIn(FrontendInternals::class)
fun ResolutionScope.collectSyntheticStaticMembersAndConstructors(
    resolutionFacade: ResolutionFacade,
    kindFilter: DescriptorKindFilter,
    nameFilter: (Name) -> Boolean
): List<FunctionDescriptor> {
    val syntheticScopes = resolutionFacade.getFrontendService(SyntheticScopes::class.java)

    val functionDescriptors =
        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK))
            getContributedDescriptors(DescriptorKindFilter.FUNCTIONS, nameFilter)
        else
            emptyList()

    val classifierDescriptors =
        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK))
            getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, nameFilter)
        else
            emptyList()

    return (syntheticScopes.forceEnableSamAdapters().collectSyntheticStaticFunctions(functionDescriptors) +
            syntheticScopes.collectSyntheticConstructors(classifierDescriptors))
        .filter { kindFilter.accepts(it) && nameFilter(it.name) }
}

fun SyntheticScopes.forceEnableSamAdapters(): SyntheticScopes {
    return this

}

private fun MemberScope.collectStaticMembers(
    resolutionFacade: ResolutionFacade,
    kindFilter: DescriptorKindFilter,
    nameFilter: (Name) -> Boolean
): Collection<DeclarationDescriptor> {
    return getDescriptorsFiltered(kindFilter, nameFilter) + collectSyntheticStaticMembersAndConstructors(
        resolutionFacade,
        kindFilter,
        nameFilter
    )
}

val DeclarationDescriptor.toSourceElement: SourceElement
    get() = if (this is DeclarationDescriptorWithSource) source else SourceElement.NO_SOURCE
