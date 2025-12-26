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

package org.cangnova.cangjie.references

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.cangnova.cangjie.FrontendInternals
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.quickDoc.cdoc.CDocLinkResolutionService
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjQualifiedExpression
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.caches.CangJieCacheService
import org.cangnova.cangjie.resolve.calls.util.CallType
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolver
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.source.PsiSourceElement
import org.cangnova.cangjie.utils.Printer
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.isExtension
import org.cangnova.cangjie.utils.substituteExtensionIfCallable


abstract class CDocReference(element: CDocName) : CjMultiReference<CDocName>(element) {
    override fun getRangeInElement(): TextRange = element.getNameTextRange()

    override fun canRename(): Boolean = true

    override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.element

    override fun getCanonicalText(): String = element.getNameText()

    override val resolvesByNames: Collection<Name> get() = listOf(Name.identifier(element.getNameText()))
}

class CangJieCDocReference(element: CDocName) : CDocReference(element) {

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> =
        CjReferenceResolutionHelper.getInstance().resolveCDocLink(element)


}

private fun resolveParamLink(
    fromDescriptor: DeclarationDescriptor,
    qualifiedName: List<String>
): List<DeclarationDescriptor> {
    val name = qualifiedName.singleOrNull() ?: return emptyList()
    return getParamDescriptors(fromDescriptor).filter { it.name.asString() == name }
}

fun getParamDescriptors(fromDescriptor: DeclarationDescriptor): List<DeclarationDescriptor> {
    // TODO resolveName parameters of functions passed as parameters
    when (fromDescriptor) {
        is CallableDescriptor -> {
            return fromDescriptor.valueParameters + fromDescriptor.typeParameters
        }

        is ClassifierDescriptor -> {
            val typeParams = fromDescriptor.typeConstructor.parameters
            if (fromDescriptor is ClassDescriptor) {
                val constructorDescriptor = fromDescriptor.unsubstitutedPrimaryConstructor
                if (constructorDescriptor != null) {
                    return typeParams + constructorDescriptor.valueParameters
                }
            }
            return typeParams
        }

        else -> {
            return emptyList()
        }
    }
}

private fun resolveLocal(
    nameToResolve: String,
    contextScope: LexicalScope,
    fromDescriptor: DeclarationDescriptor
): List<DeclarationDescriptor> {
    val shortName = Name.identifier(nameToResolve)

    val descriptorsByName = SmartList<DeclarationDescriptor>()
    descriptorsByName.addIfNotNull(contextScope.findClassifier(shortName, NoLookupLocation.FROM_IDE))
    descriptorsByName.addIfNotNull(contextScope.findPackage(shortName))
    descriptorsByName.addAll(contextScope.collectFunctions(shortName, NoLookupLocation.FROM_IDE))
    descriptorsByName.addAll(contextScope.collectVariables(shortName, NoLookupLocation.FROM_IDE))

    if (fromDescriptor is FunctionDescriptor && shortName.asString() == "this") {
        return listOfNotNull(fromDescriptor.extensionReceiverParameter)
    }

    // Try to find a matching local descriptor (parameter or type parameter) first
    val localDescriptors = descriptorsByName.filter { it.containingDeclaration == fromDescriptor }
    if (localDescriptors.isNotEmpty()) return localDescriptors

    return descriptorsByName
}

private fun getPackageInnerScope(descriptor: PackageFragmentDescriptor): MemberScope {
    return descriptor.containingDeclaration.getPackage(descriptor.fqName).memberScope
}

private fun getOuterScope(
    descriptor: DeclarationDescriptorWithSource,
    resolutionFacade: ResolutionFacade
): LexicalScope {
    val parent = descriptor.containingDeclaration!!
    if (parent is PackageFragmentDescriptor) {
        val containingFile =
            (descriptor.source as? PsiSourceElement)?.psi?.containingFile as? CjFile ?: return LexicalScope.Base(
                ImportingScope.Empty,
                parent
            )
        val kotlinCacheService = containingFile.project.serviceOrNull<CangJieCacheService>()
        val facadeToUse = kotlinCacheService?.getResolutionFacade(containingFile) ?: resolutionFacade
        return facadeToUse.getFileResolutionScope(containingFile)
    } else {
        return getCDocLinkResolutionScope(resolutionFacade, parent)
    }
}

private fun getClassInnerScope(outerScope: LexicalScope, descriptor: ClassDescriptor): LexicalScope {

    val headerScope = LexicalScopeImpl(
        outerScope, descriptor, false, descriptor.thisAsReceiverParameter,
        descriptor.contextReceivers, LexicalScopeKind.SYNTHETIC
    ) {
        descriptor.declaredTypeParameters.forEach { addClassifierDescriptor(it) }
        descriptor.constructors.forEach { addFunctionDescriptor(it) }
    }

    return LexicalChainedScope.create(
        headerScope, descriptor, false, null, emptyList(), LexicalScopeKind.SYNTHETIC,
        descriptor.defaultType.memberScope,
        descriptor.staticScope,
        null
    )
}

fun getCDocLinkResolutionScope(
    resolutionFacade: ResolutionFacade,
    contextDescriptor: DeclarationDescriptor
): LexicalScope {
    return when (contextDescriptor) {
        is PackageFragmentDescriptor ->
            LexicalScope.Base(getPackageInnerScope(contextDescriptor).memberScopeAsImportingScope(), contextDescriptor)

        is PackageViewDescriptor ->
            LexicalScope.Base(contextDescriptor.memberScope.memberScopeAsImportingScope(), contextDescriptor)

        is ClassDescriptor ->
            getClassInnerScope(getOuterScope(contextDescriptor, resolutionFacade), contextDescriptor)

        is FunctionDescriptor -> FunctionDescriptorUtil.getFunctionInnerScope(
            getOuterScope(contextDescriptor, resolutionFacade),
            contextDescriptor, LocalRedeclarationChecker.DO_NOTHING
        )

        is PropertyDescriptor ->
            ScopeUtils.makeScopeForPropertyHeader(getOuterScope(contextDescriptor, resolutionFacade), contextDescriptor)

        is DeclarationDescriptorNonRoot ->
            getOuterScope(contextDescriptor, resolutionFacade)

        else -> throw IllegalArgumentException("Cannot find resolution scope for root $contextDescriptor")
    }
}

fun resolveCDocLink(
    context: BindingContext,
    resolutionFacade: ResolutionFacade,
    fromDescriptor: DeclarationDescriptor,
    contextElement: CjElement,
    fromSubjectOfTag: CDocTag?,
    qualifiedName: List<String>
): Collection<DeclarationDescriptor> {
    val tag = fromSubjectOfTag?.knownTag
    if (tag == CDocKnownTag.PARAM) {
        return resolveParamLink(fromDescriptor, qualifiedName)
    }
    val contextScope = getCDocLinkResolutionScope(resolutionFacade, fromDescriptor)
    if (qualifiedName.size == 1) {
        val localDescriptors = resolveLocal(qualifiedName.single(), contextScope, fromDescriptor)
        if (localDescriptors.isNotEmpty()) {
            return localDescriptors
        }
    }
    val isMarkdownLink = tag == null
    if (tag == CDocKnownTag.SAMPLE || tag == CDocKnownTag.SEE || isMarkdownLink) {
        val cdocService = resolutionFacade.project.serviceOrNull<CDocLinkResolutionService>()
        val declarationDescriptors =
            cdocService?.resolveCDocLink(context, fromDescriptor, resolutionFacade, qualifiedName) ?: emptyList()
        if (declarationDescriptors.isNotEmpty()) {
            return declarationDescriptors
        }
    }

    return resolveDefaultCDocLink(context, resolutionFacade, contextElement, qualifiedName, contextScope)
}

private fun resolveDefaultCDocLink(
    context: BindingContext,
    resolutionFacade: ResolutionFacade,
    contextElement: PsiElement,
    qualifiedName: List<String>,
    contextScope: LexicalScope
): Collection<DeclarationDescriptor> {
    @OptIn(FrontendInternals::class)
    val qualifiedExpressionResolver = resolutionFacade.getFrontendService(QualifiedExpressionResolver::class.java)

    val factory = CjPsiFactory(resolutionFacade.project)
    // TODO escape identifiers
    val codeFragment = factory.createExpressionCodeFragment(qualifiedName.joinToString("."), contextElement)
    val qualifiedExpression =
        codeFragment.findElementAt(codeFragment.textLength - 1)?.getStrictParentOfType<CjQualifiedExpression>()
            ?: return emptyList()
    val (descriptor, memberName) = qualifiedExpressionResolver.resolveClassOrPackageInQualifiedExpression(
        qualifiedExpression,
        contextScope,
        context
    )
    if (descriptor == null) return emptyList()
    if (memberName != null) {
        val memberScope = getCDocLinkMemberScope(descriptor, contextScope)
        return memberScope.getContributedFunctions(memberName, NoLookupLocation.FROM_IDE) +
                memberScope.getContributedVariables(memberName, NoLookupLocation.FROM_IDE) +
                listOfNotNull(memberScope.getContributedClassifier(memberName, NoLookupLocation.FROM_IDE))
    }
    return listOf(descriptor)
}

fun getCDocLinkMemberScope(descriptor: DeclarationDescriptor, contextScope: LexicalScope): MemberScope {
    return when (descriptor) {
        is PackageFragmentDescriptor -> getPackageInnerScope(descriptor)

        is PackageViewDescriptor -> descriptor.memberScope

        is ClassDescriptor -> {
            ChainedMemberScope.create(
                "Member scope for CDoc resolveName", listOfNotNull(
                    descriptor.unsubstitutedMemberScope,
                    descriptor.staticScope,
                    null,
                    ExtensionsScope(descriptor, contextScope)
                )
            )
        }

        else -> MemberScope.Empty
    }
}

private class ExtensionsScope(
    private val receiverClass: ClassDescriptor,
    private val contextScope: LexicalScope
) : MemberScope {
    private val receiverTypes = listOf(receiverClass.defaultType)
    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return emptyList()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return contextScope.collectFunctions(name, location).flatMap {
            emptyList()
//            if (it is SimpleFunctionDescriptor && it.isExtension) {
//                it.substituteExtensionIfCallable(
//                    receiverTypes = receiverTypes,
//                    callType = CallType.DOT,
//                    ignoreTypeParameters = true,
//                )
//            } else {
//                emptyList()
//            }
        }
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return contextScope.collectVariables(name, location).flatMap {
            if (it is PropertyDescriptor && it.isExtension) {
                it.substituteExtensionIfCallable(
                    receiverTypes = receiverTypes,
                    callType = CallType.DOT,
                    ignoreTypeParameters = true,
                )
            } else {
                emptyList()
            }
        }
    }


    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return contextScope.collectVariables(name, location).flatMap {
            if (it.isExtension) {
                it.substituteExtensionIfCallable(
                    receiverTypes = receiverTypes,
                    callType = CallType.DOT,
                    ignoreTypeParameters = true,
                )
            } else {
                emptyList()
            }
        }
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null
    override val propertyNames : Set<Name> get() {
        return getContributedDescriptors(kindFilter = DescriptorKindFilter.PROPERTYS).map { it.name }.toSet()

    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (DescriptorKindExclude.Extensions in kindFilter.excludes) return emptyList()
        return contextScope.collectDescriptorsFiltered(
            kindFilter exclude DescriptorKindExclude.NonExtensions,
            nameFilter,
            changeNamesForAliased = true
        ).flatMap {
            if (it is CallableDescriptor && it.isExtension) {
                it.substituteExtensionIfCallable(
                    receiverTypes = receiverTypes,
                    callType = CallType.DOT,
                    ignoreTypeParameters = true,
                )
            } else {
                emptyList()
            }
        }
    }

    override val functionNames : Set<Name> get()=
        getContributedDescriptors(kindFilter = DescriptorKindFilter.FUNCTIONS).map { it.name }.toSet()

    override val variableNames : Set<Name> get()=
        getContributedDescriptors(kindFilter = DescriptorKindFilter.VARIABLES).map { it.name }.toSet()

    override val classifierNames get() = null

    override fun printScopeStructure(p: Printer) {
        p.println("Extensions for ${receiverClass.name} in:")
        contextScope.printStructure(p)
    }
}
