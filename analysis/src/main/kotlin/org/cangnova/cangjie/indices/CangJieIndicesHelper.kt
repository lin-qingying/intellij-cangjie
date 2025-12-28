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

package org.cangnova.cangjie.indices

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.extensions.CangJieIndicesHelperExtension
import org.cangnova.cangjie.incremental.CangJieLookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.caches.resolveImportReference
import org.cangnova.cangjie.resolve.calls.util.receiverTypes
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import org.cangnova.cangjie.resolve.fqNameSafe
import org.cangnova.cangjie.resolve.frontendService
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import org.cangnova.cangjie.FrontendInternals
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.util.languageVersionSettings
import org.cangnova.cangjie.resolve.isStatic
import org.cangnova.cangjie.stubindex.CangJieClassShortNameIndex
import org.cangnova.cangjie.stubindex.CangJieFullClassNameIndex
import org.cangnova.cangjie.stubindex.CangJieFunctionShortNameIndex
import org.cangnova.cangjie.stubindex.CangJieStringStubIndexHelper
import org.cangnova.cangjie.stubindex.CangJieTopLevelFunctionFqNameIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelTypeAliasFqNameIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelVariableFqNameIndex
import org.cangnova.cangjie.stubindex.CangJieTypeAliasByExpansionShortNameIndex
import org.cangnova.cangjie.stubindex.CangJieVariableShortNameIndex
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachments
import org.cangnova.cangjie.utils.importableFqName
import org.cangnova.cangjie.utils.isExtension
import kotlin.collections.filter


class CangJieIndicesHelper(
    private val resolutionFacade: ResolutionFacade,
    private val scope: GlobalSearchScope,
    visibilityFilter: (DeclarationDescriptor) -> Boolean,
    applicabilityFilter: (DeclarationDescriptor) -> Boolean = { true },
    private val declarationTranslator: (CjDeclaration) -> CjDeclaration? = { it },
    applyExcludeSettings: Boolean = true,
    private val filterOutPrivate: Boolean = true,
    private val file: CjFile? = null
) {
    private val moduleDescriptor = resolutionFacade.moduleDescriptor

    private val project = resolutionFacade.project

    @OptIn(FrontendInternals::class)
    private val descriptorFilter: (DeclarationDescriptor) -> Boolean = filter@{ descriptor ->
        if (!applicabilityFilter(descriptor)) return@filter false
        if (resolutionFacade.frontendService<DeprecationResolver>()
                .isHiddenInResolution(descriptor)
        ) return@filter false
        if (!visibilityFilter(descriptor)) return@filter false
        if (applyExcludeSettings) {
            val fqName = descriptor.importableFqName
            if (fqName != null && fqName.isExcludedFromAutoImport(project, file)) {
                return@filter false
            }
        }
        return@filter true
    }

    private inline fun <reified TDescriptor : Any> CjNamedDeclaration.resolveToDescriptors(): Collection<TDescriptor> {
        return resolveToDescriptorsWithHack { true }.filterIsInstance<TDescriptor>()
    }

    fun processTopLevelCallables(nameFilter: (String) -> Boolean, processor: (CallableDescriptor) -> Unit) {
        val callableDeclarationProcessor = Processor<CjCallableDeclaration> { declaration ->
            if (filterOutPrivate && declaration.hasModifier(CjTokens.PRIVATE_KEYWORD)) return@Processor true
            ProgressManager.checkCanceled()
            declaration.resolveToDescriptors<CallableDescriptor>().forEach { descriptor ->
                if (descriptorFilter(descriptor)) {
                    processor(descriptor)
                }
            }
            true
        }

        val filter: (String) -> Boolean = { key -> nameFilter(key.substringAfterLast('.', key)) }

        CangJieTopLevelFunctionFqNameIndex.processAllElements(project, scope, filter, callableDeclarationProcessor)
        CangJieTopLevelVariableFqNameIndex.processAllElements(project, scope, filter, callableDeclarationProcessor)
    }

    fun processStaticMembers(
        descriptorKindFilter: DescriptorKindFilter,
        nameFilter: (String) -> Boolean,
        processor: (DeclarationDescriptor) -> Unit
    ) {

    }

    private fun MutableSet<CjNamedDeclaration>.addTopLevelNonExtensionCallablesByName(
        helper: CangJieStringStubIndexHelper<out CjNamedDeclaration>,
        name: String
    ) {
        helper.processElements(name, project, scope) {
            ProgressManager.checkCanceled()
            if (it.parent is CjFile && it is CjCallableDeclaration ) {
                add(it)
            }
            true
        }
    }

    fun processCallableExtensionsDeclaredInObjects(
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        receiverTypes: Collection<CangJieType>,
        nameFilter: (String) -> Boolean,
        declarationFilter: (CjDeclaration) -> Boolean = { true },
        processor: (CallableDescriptor) -> Unit
    ) {
        if (receiverTypes.isEmpty()) return
//
//       CangJieExtensionsInObjectsByReceiverTypeIndex.processSuitableExtensions(
//            receiverTypes,
//            nameFilter,
//            declarationFilter,
//            callTypeAndReceiver,
//            processor
//        )
    }

    fun processAllCallablesFromSubclassObjects(
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        receiverTypes: Collection<CangJieType>,
        nameFilter: (String) -> Boolean,
        processor: (CallableDescriptor) -> Unit
    ) {

    }

    fun resolveTypeAliasesUsingIndex(type: CangJieType, originalTypeName: String): Set<TypeAliasDescriptor> {
        val typeConstructor = type.constructor

        val out = LinkedHashMap<FqName, TypeAliasDescriptor>()

        fun searchRecursively(typeName: String) {
            ProgressManager.checkCanceled()
            CangJieTypeAliasByExpansionShortNameIndex[typeName, project, scope].asSequence()
                .flatMap { it.resolveToDescriptors<TypeAliasDescriptor>().asSequence() }
                .filter { it.expandedType.constructor == typeConstructor }
                .filter { out.putIfAbsent(it.fqNameSafe, it) == null }
                .map { it.name.asString() }
                .forEach(::searchRecursively)
        }

        searchRecursively(originalTypeName)
        return out.values.toSet()
    }

    fun getMemberOperatorsByName(name: String): Collection<FunctionDescriptor> {
        return CangJieFunctionShortNameIndex.getAllElements(name, project, scope) {
            it.parent is CjAbstractClassBody && it.hasModifier(CjTokens.OPERATOR_KEYWORD)
        }
            .flatMap {
                ProgressManager.checkCanceled()
                it.resolveToDescriptors<FunctionDescriptor>()
            }
            .filter { descriptorFilter(it) && !it.isExtension }
            .distinct()
    }

    fun getTopLevelExtensionOperatorsByName(name: String): Collection<FunctionDescriptor> {
        return CangJieFunctionShortNameIndex.getAllElements(name, project, scope) {
            it.parent is CjFile  && it.hasModifier(CjTokens.OPERATOR_KEYWORD)
        }
            .flatMap {
                ProgressManager.checkCanceled()
                it.resolveToDescriptors<FunctionDescriptor>()
            }
            .filter { descriptorFilter(it) && it.isExtension }
            .distinct()
    }


    fun getCangJieEnumsByName(name: String): Collection<DeclarationDescriptor> {
        val enumEntries = CangJieClassShortNameIndex.getAllElements(name, project, scope) {
            it is CjEnumConstructor
        }
        val result = HashSet<DeclarationDescriptor>(enumEntries.size)
        for (enumEntry in enumEntries) {
            ProgressManager.checkCanceled()
            val descriptorsWithHack = enumEntry.resolveToDescriptorsWithHack { true }
            for (descriptor in descriptorsWithHack) {
                if (descriptorFilter(descriptor)) result += descriptor
            }
        }
        return result
    }

    fun getTopLevelCallablesByName(name: String): Collection<CallableDescriptor> {
        val declarations = mutableSetOf<CjNamedDeclaration>()
        declarations.addTopLevelNonExtensionCallablesByName(CangJieFunctionShortNameIndex, name)
        declarations.addTopLevelNonExtensionCallablesByName(CangJieVariableShortNameIndex, name)

        return declarations
            .flatMap { it.resolveToDescriptors<CallableDescriptor>() }
            .filter { descriptorFilter(it) }
    }

    fun processTopLevelTypeAliases(nameFilter: (String) -> Boolean, processor: (TypeAliasDescriptor) -> Unit) {
        val typeAliasProcessor = Processor<CjTypeAlias> { typeAlias ->
            typeAlias.resolveToDescriptors<TypeAliasDescriptor>().forEach {
                ProgressManager.checkCanceled()
                if (descriptorFilter(it)) {
                    processor(it)
                }
            }
            true
        }
        CangJieTopLevelTypeAliasFqNameIndex
            .processAllElements(project, scope, { nameFilter(it.substringAfterLast('.')) }, typeAliasProcessor)
    }


    fun getCallableTopLevelExtensions(
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        position: CjExpression,
        bindingContext: BindingContext,
        receiverTypeFromDiagnostic: CangJieType?,
        nameFilter: (String) -> Boolean
    ): Collection<CallableDescriptor> {
        val receiverTypes = callTypeAndReceiver.receiverTypes(
            bindingContext, position, moduleDescriptor, resolutionFacade, stableSmartCastsOnly = false
        )

        return if (receiverTypes == null || receiverTypes.all { it.isError }) {
            if (receiverTypeFromDiagnostic != null)
                getCallableTopLevelExtensions(callTypeAndReceiver, listOf(receiverTypeFromDiagnostic), nameFilter)
            else
                emptyList()
        } else {
            getCallableTopLevelExtensions(callTypeAndReceiver, receiverTypes, nameFilter).filter {
                val isStatic = (callTypeAndReceiver.receiver as? CjExpression)?.let{
                    bindingContext[BindingContext.QUALIFIER, it] != null

                } == true
                if (isStatic) {
                    return@filter it.isStatic()

                }
                true
            }
        }
    }

    fun getCallableTopLevelExtensions(
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        receiverTypes: Collection<CangJieType>,
        nameFilter: (String) -> Boolean,
        declarationFilter: (CjDeclaration) -> Boolean = { true }
    ): Collection<CallableDescriptor> {

        if (receiverTypes.isEmpty()) return emptyList()

        val suitableTopLevelExtensions = mutableListOf<CallableDescriptor>()


        val additionalDescriptors = ArrayList<CallableDescriptor>()

        val lookupLocation = this.file?.let { CangJieLookupLocation(it) } ?: NoLookupLocation.FROM_IDE
        for (extension in CangJieIndicesHelperExtension.getInstances(project)) {
            extension.appendExtensionCallables(
                additionalDescriptors,
                moduleDescriptor,
                receiverTypes,
                nameFilter,
                lookupLocation
            )
        }

        return if (additionalDescriptors.isNotEmpty()) {
            suitableTopLevelExtensions + additionalDescriptors
        } else {
            suitableTopLevelExtensions
        }
    }

    private fun collectAllNamesOfTypes(types: Collection<CangJieType>): HashSet<String> {
        val receiverTypeNames = HashSet<String>()
        types.forEach { receiverTypeNames.addTypeNames(it) }
        return receiverTypeNames
    }

    private fun MutableCollection<String>.addTypeNames(type: CangJieType) {
        val constructor = type.constructor
        constructor.declarationDescriptor?.name?.asString()?.let { typeName ->
            add(typeName)
            addAll(possibleTypeAliasExpansionNames(typeName))
        }
        constructor.supertypes.forEach { addTypeNames(it) }
    }

    private fun possibleTypeAliasExpansionNames(originalTypeName: String): Set<String> {
        val out = mutableSetOf<String>()

        fun searchRecursively(typeName: String) {
            ProgressManager.checkCanceled()
            CangJieTypeAliasByExpansionShortNameIndex[typeName, project, scope].asSequence()
                .mapNotNull(CjTypeAlias::getName)
                .filter(out::add)
                .forEach(::searchRecursively)
        }

        searchRecursively(originalTypeName)
        return out
    }



    private fun CjNamedDeclaration.resolveToDescriptorsWithHack(
        psiFilter: (CjDeclaration) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val cjFile = containingFile
        if (cjFile !is CjFile) {
            // https://ea.jetbrains.com/browser/ea_problems/219256
            LOG.error(
                CangJieExceptionWithAttachments("CjElement not inside CjFile ($cjFile, is valid: ${cjFile.isValid})")
                    .withAttachment("file", cjFile)
                    .withAttachment("virtualFile", containingFile.virtualFile)

                    .withAttachment("element", this)
                    .withAttachment("type", javaClass)
                    .withPsiAttachment("file.cj", cjFile)
            )

            return emptyList()
        }

        if (cjFile.isCompiled) { //TODO: it's temporary while resolveToDescriptor does not work for compiled declarations
            val fqName = fqName ?: return emptyList()
            return resolutionFacade.resolveImportReference(moduleDescriptor, fqName)
        } else {
            val translatedDeclaration = declarationTranslator(this) ?: return emptyList()
            if (!psiFilter(translatedDeclaration)) return emptyList()

            return listOfNotNull(resolutionFacade.resolveToDescriptor(translatedDeclaration))
        }
    }

    fun processCangJieClasses(
        nameFilter: (String) -> Boolean,
        psiFilter: (CjDeclaration) -> Boolean = { true },
        kindFilter: (ClassKind) -> Boolean = { true },
        processor: (ClassDescriptor) -> Unit
    ) {
        val classOrObjectProcessor = Processor<CjTypeStatement> { classOrObject ->
            ProgressManager.checkCanceled()
            classOrObject.resolveToDescriptorsWithHack(psiFilter).forEach {
                val descriptor = it as? ClassDescriptor ?: return@forEach
                ProgressManager.checkCanceled()
                if (kindFilter(descriptor.kind) && descriptorFilter(descriptor)) {
                    processor(descriptor)
                }
            }
            true
        }
        CangJieFullClassNameIndex.processAllElements(
            project,
            scope,
            { nameFilter(it.substringAfterLast('.')) },
            classOrObjectProcessor
        )
    }

    companion object {
        private val LOG = Logger.getInstance(CangJieIndicesHelper::class.java)
    }
}

fun FqName.isExcludedFromAutoImport(
    project: Project,
    contextFile: CjFile?,
    languageVersionSettings: LanguageVersionSettings? = contextFile?.languageVersionSettings
): Boolean {
    return false
}
