package com.huawei.cangjie.ide

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.extensions.CangJieIndicesHelperExtension
import com.huawei.cangjie.ide.imports.importableFqName
import com.huawei.cangjie.ide.projectStructure.languageVersionSettings
import com.huawei.cangjie.ide.stubindex.*
import com.huawei.cangjie.ide.util.substituteExtensionIfCallable
import com.huawei.cangjie.incremental.CangJieLookupLocation
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.caches.resolveImportReference
import com.huawei.cangjie.resolve.calls.util.receiverTypes
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver
import com.huawei.cangjie.resolve.frontendService
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.isError
import com.huawei.cangjie.utils.CallTypeAndReceiver
import com.huawei.cangjie.utils.CangJieExceptionWithAttachments
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor


class CangJieIndicesHelper(
    private val resolutionFacade: ResolutionFacade,
    private val scope: GlobalSearchScope,
    visibilityFilter: (DeclarationDescriptor) -> Boolean,
    applicabilityFilter: (DeclarationDescriptor) -> Boolean = { true },
    private val declarationTranslator: (CjDeclaration) -> CjDeclaration? = { it },
    applyExcludeSettings: Boolean = true,
    private val filterOutPrivate: Boolean = true,
    private val file: CjFile? = null
)
{
    private val moduleDescriptor = resolutionFacade.moduleDescriptor

    private val project = resolutionFacade.project
    @OptIn(FrontendInternals::class)
    private val descriptorFilter: (DeclarationDescriptor) -> Boolean = filter@{ descriptor ->
        if (!applicabilityFilter(descriptor)) return@filter false
        if (resolutionFacade.frontendService<DeprecationResolver>().isHiddenInResolution(descriptor)) return@filter false
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
    private fun MutableSet<CjNamedDeclaration>.addTopLevelNonExtensionCallablesByName(
        helper: CangJieStringStubIndexHelper<out CjNamedDeclaration>,
        name: String
    ) {
        helper.processElements(name, project, scope) {
            ProgressManager.checkCanceled()
            if (it.parent is CjFile && it is CjCallableDeclaration && it.receiverTypeReference == null) {
                add(it)
            }
            true
        }
    }



    fun getCangJieEnumsByName(name: String): Collection<DeclarationDescriptor> {
        val enumEntries = CangJieClassShortNameIndex.getAllElements(name, project, scope) {
            it is CjEnumEntry
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
        declarations.addTopLevelNonExtensionCallablesByName(CangJiePropertyShortNameIndex, name)
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
            getCallableTopLevelExtensions(callTypeAndReceiver, receiverTypes, nameFilter)
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
        CangJieTopLevelExtensionsByReceiverTypeIndex.processSuitableExtensions(
            receiverTypes,
            nameFilter,
            declarationFilter,
            callTypeAndReceiver,
            processor = suitableTopLevelExtensions::add
        )

        val additionalDescriptors = ArrayList<CallableDescriptor>()

        val lookupLocation = this.file?.let { CangJieLookupLocation(it) } ?: NoLookupLocation.FROM_IDE
        for (extension in CangJieIndicesHelperExtension.getInstances(project)) {
            extension.appendExtensionCallables(additionalDescriptors, moduleDescriptor, receiverTypes, nameFilter, lookupLocation)
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

    private fun CangJieExtensionsByReceiverTypeStubIndexHelper.processSuitableExtensions(
        receiverTypes: Collection<CangJieType>,
        nameFilter: (String) -> Boolean,
        declarationFilter: (CjDeclaration) -> Boolean,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        processor: (CallableDescriptor) -> Unit
    ) {
        val receiverTypeNames = collectAllNamesOfTypes(receiverTypes)

        val callType = callTypeAndReceiver.callType
        val processed = HashSet<CallableDescriptor>()

        val declarationProcessor = Processor<CjCallableDeclaration> { callableDeclaration ->
            // Check that function or property with the given qualified name can be resolved in given scope and called on given receiver
            ProgressManager.checkCanceled()
            if (declarationFilter(callableDeclaration)) {
                callableDeclaration.resolveToDescriptors<CallableDescriptor>().forEach { descriptor ->
                    if (descriptor.extensionReceiverParameter != null && descriptorFilter(descriptor)) {
                        for (callableDescriptor in descriptor.substituteExtensionIfCallable(receiverTypes, callType)){
                            if (processed.add(callableDescriptor)) processor(callableDescriptor)
                        }
                    }
                }
            }
            true
        }
        processAllElements(
            project,
            scope,
            { receiverTypeNameFromKey(it) in receiverTypeNames && nameFilter(callableNameFromKey(it)) },
            declarationProcessor
        )
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
        CangJieFullClassNameIndex.processAllElements(project, scope, { nameFilter(it.substringAfterLast('.')) }, classOrObjectProcessor)
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
   return false}
