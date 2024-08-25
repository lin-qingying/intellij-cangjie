package com.huawei.cangjie.ide.projectStructure

import com.huawei.cangjie.analyzer.LibraryInfo
import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.analyzer.ModuleSourceInfo
import com.huawei.cangjie.cjpm.project.workspace.CjpmLibrary
import com.huawei.cangjie.ide.base.projectStructure.RootKindFilter
import com.huawei.cangjie.ide.base.projectStructure.matches

import com.huawei.cangjie.ide.cache.project.LibraryInfoCache
import com.huawei.cangjie.ide.cache.project.cangjieModuleInfo
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.utils.*
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

@DslMarker
private annotation class ModuleInfoDsl

@ModuleInfoDsl
fun SeqScope<Result<ModuleInfo>>.register(moduleInfo: ModuleInfo) = yield { Result.success(moduleInfo) }

@ModuleInfoDsl
fun SeqScope<Result<ModuleInfo>>.reportError(error: Throwable) = yield { Result.failure(error) }

@Service(Service.Level.PROJECT)
class ModuleInfoProvider(private val project: Project) {
    data class Configuration(
        val createSourceLibraryInfoForLibraryBinaries: Boolean = false,
        val preferModulesFromExtensions: Boolean = false,
        val contextualModuleInfo: ModuleInfo? = null,
    ) {
        companion object {
            val Default = Configuration()
        }
    }

    companion object {
        internal val LOG = Logger.getInstance(ModuleInfoProvider::class.java)

        fun getInstance(project: Project): ModuleInfoProvider = project.service()

        fun findAnchorElement(element: PsiElement): PsiElement? = when {
            element is PsiDirectory -> element
            element !is CjLightElement<*, *> -> element.containingFile
            /**
             * We shouldn't unwrap decompiled classes
             * @see [ModuleInfoProvider.collectByLightElement]
             */
//            element.getNonStrictParentOfType<CjLightClassForDecompiledDeclaration>() != null -> null
//            element is CjLightClassForFacade -> element.files.first()
//            else -> element.cangjieOrigin?.let(::findAnchorElement)
            else -> {
                element.cangjieOrigin?.let(::findAnchorElement)

            }
        }
    }

    private val fileIndex by lazy { ProjectFileIndex.getInstance(project) }

    private inline fun callExtensions(block: ModuleInfoProviderExtension.() -> Unit) {
        for (extension in ModuleInfoProviderExtension.EP_NAME.extensionList) {
            with(extension, block)
        }
    }

    private inline fun withCallExtensions(
        config: Configuration,
        extensionBlock: ModuleInfoProviderExtension.() -> Unit,
        block: () -> Unit,
    ) {
        if (config.preferModulesFromExtensions) {
            callExtensions(extensionBlock)
        }

        block()

//        if (!config.preferModulesFromExtensions) {
//            callExtensions(extensionBlock)
//        }
    }

    //    private fun isLibrarySource(containingCjFile: CjFile, config: Configuration): Boolean {
//        val isCompiled = containingCjFile.isCompiled
//        return if (config.createSourceLibraryInfoForLibraryBinaries) isCompiled else !isCompiled
//    }
    private fun SeqScope<Result<ModuleInfo>>.collectByElement(element: PsiElement, config: Configuration) {
        val containingFile = element.containingFile

        if (containingFile != null) {
            val moduleInfo = containingFile.forcedModuleInfo
            if (moduleInfo is ModuleInfo) {
                register(moduleInfo)
            }
        }


        if (element is PsiDirectory) {
            collectByFile(element.virtualFile, isLibrarySource = false, config)
            return
        }


        if (containingFile == null) {
            val message = "Analyzing element of type ${element::class.java} with no containing file"
            reportError(CangJieExceptionWithAttachments(message).withAttachment("element.kt", element.text))
        }

        val containingCjFile = containingFile as? CjFile
        if (containingCjFile != null) {
//            @OptIn(CjModuleStructureInternals::class, Frontend10ApiUsage::class)
//            containingFile.virtualFile?.analysisExtensionFileContextModule?.let { module ->
//                register(module.moduleInfo)
//            }

            val analysisContext = containingCjFile.analysisContext
            if (analysisContext != null) {
                collectByElement(analysisContext, config)
            }

            if (containingCjFile.doNotAnalyze != null) {
                return
            }

            val explicitModuleInfo =
                containingCjFile.forcedModuleInfo ?: (containingCjFile.originalFile as? CjFile)?.forcedModuleInfo
            if (explicitModuleInfo is ModuleInfo) {
                register(explicitModuleInfo)
            }

            if (containingCjFile is CjCodeFragment) {
                val context = containingCjFile.getContext()
                if (context != null) {
                    collectByElement(context, config)
                } else {
                    val message = "Analyzing code fragment of type ${containingCjFile::class.java} with no context"
                    val error =
                        CangJieExceptionWithAttachments(message).withAttachment("file.kt", containingCjFile.text)
                    reportError(error)
                }
            }
        }
        if (containingFile != null) {
            val virtualFile = containingFile.originalFile.virtualFile
            if (virtualFile != null) {
                withCallExtensions(
                    config = config,
                    extensionBlock = { collectByElement(element, containingFile, virtualFile) },
                ) {
//                    val isLibrarySource = if (containingCjFile != null) isLibrarySource(containingCjFile, config) else false
                    collectByFile(virtualFile, isLibrarySource = true, config)
                }
            } else {
                val message =
                    "Analyzing element of type ${element::class.java} in non-physical file of type ${containingFile::class.java}"
                reportError(CangJieExceptionWithAttachments(message).withAttachment("file.kt", containingFile.text))
            }
        }
    }


    fun collect(
        virtualFile: VirtualFile,
        isLibrarySource: Boolean = false,
        config: Configuration = Configuration.Default,
    ): Sequence<Result<ModuleInfo>> {
        return seq {
            collectByFile(virtualFile, isLibrarySource, config)
        }
    }

//    private inline fun withCallExtensions(
//        config: Configuration,
//        extensionBlock: ModuleInfoProviderExtension.() -> Unit,
//        block: () -> Unit,
//    ) {
//        if (config.preferModulesFromExtensions) {
//            callExtensions(extensionBlock)
//        }
//
//        block()
//
//        if (!config.preferModulesFromExtensions) {
//            callExtensions(extensionBlock)
//        }
//    }

    private fun SeqScope<Result<ModuleInfo>>.collectSourceRelatedByFile(
        virtualFile: VirtualFile,
        config: Configuration
    ) {
        yieldAll(object : Iterable<Result<ModuleInfo>> {
            override fun iterator(): Iterator<Result<ModuleInfo>> {

                val modules = seq {
                    withCallExtensions(
                        config = config,
                        extensionBlock = { findContainingModules(project, virtualFile) },
                    ) {
                        runReadAction { fileIndex.getModuleForFile(virtualFile) }?.let { module ->

                            yield { module }


                        }
                    }
                }
                val iterator = modules.iterator()

                return MappingIterator(iterator) { module ->
                    if (module.isDisposed) return@MappingIterator null
//                    val projectFileIndex = ProjectFileIndex.getInstance(project)
//                    val sourceRootType: CangJieSourceRootType? = projectFileIndex.getCangJieSourceRootType(virtualFile)
//                    module.asSourceInfo(sourceRootType)?.let(Result.Companion::success)
//module.moduleInfos
                    Result.success(module.cangjieModuleInfo)
                }
            }
        })
//
//        val fileOrigin = getOutsiderFileOrigin(project, virtualFile)
//        if (fileOrigin != null) {
//            collectSourceRelatedByFile(fileOrigin, config)
//        }
    }

    private fun contextByContextualModule(
        virtualFile: VirtualFile,
        isLibrarySource: Boolean,
        visited: HashSet<ModuleInfo>,
        config: Configuration
    ): ModuleInfo? {
        val contextualModuleInfo = config.contextualModuleInfo ?: return null

//        val contentScope = when (contextualModuleInfo) {
//            is CjpmLibraryInfo -> contextualModuleInfo.contentScope
////            is LibrarySourceInfo -> contextualModuleInfo.sourceScope()
//            else -> null
//        }

//        if (contentScope == null || virtualFile !in contentScope) {
//            return null
//        }
//
//        return when (contextualModuleInfo) {
//            is CjpmLibraryInfo -> contextualModuleInfo.library?.let {
//                collectByLibrary(
//                    virtualFile,
//                    it,
//                    isLibrarySource,
//                    visited,
//                    config
//                )
//            }
////            is LibrarySourceInfo -> collectByLibrary(virtualFile, contextualModuleInfo.library, isLibrarySource, visited, config)
////            is SdkInfo -> collectBySdk(contextualModuleInfo.sdk, visited)
//            else -> null
//        }


        val contentScope = when (contextualModuleInfo) {
            is LibraryInfo -> contextualModuleInfo.contentScope
//            is LibrarySourceInfo -> contextualModuleInfo.sourceScope()
            else -> null
        }

        if (contentScope == null || virtualFile !in contentScope) {
            return null
        }

        return when (contextualModuleInfo) {
            is LibraryInfo -> collectByLibrary(
                virtualFile,
                contextualModuleInfo.library,
                isLibrarySource,
                visited,
                config
            )
//            is LibrarySourceInfo -> collectByLibrary(virtualFile, contextualModuleInfo.library, isLibrarySource, visited, config)
//            is SdkInfo -> collectBySdk(contextualModuleInfo.sdk, visited)
            else -> null
        }
    }

    private fun SeqScope<Result<ModuleInfo>>.collectByFile(
        virtualFile: VirtualFile,
        isLibrarySource: Boolean,
        config: Configuration,
    ) {

        withCallExtensions(
            config = config,
            extensionBlock = { collectByFile(project, virtualFile, isLibrarySource, config) }
        ) {
            collectSourceRelatedByFile(virtualFile, config)

            val visited = hashSetOf<ModuleInfo>()

            yield {
                // Several libraries may include the same JAR files.
                // Below, we use an index for getting order entries for a file, but entries come in an arbitrary order.
                // So if we are already inside a library, we scan it first.

                val contextualModuleResult =
                    contextByContextualModule(virtualFile, isLibrarySource, visited, config)
                contextualModuleResult?.let(Result.Companion::success)
            }

            yieldAll(object : Iterable<Result<ModuleInfo>> {
                override fun iterator(): Iterator<Result<ModuleInfo>> {

//                    val librarys =
//                        runReadAction { CjAdditionalLibraryRootsProvider.findLibrarysByCjFile(project, virtualFile) }
//
//                    val iterator = librarys.iterator()
//                    return MappingIterator(iterator) { orderEntry ->
//                        collectByOrderEntry(
//                            virtualFile,
//                            orderEntry,
//                            isLibrarySource,
//                            visited,
//                            config
//                        )?.let(Result.Companion::success)
//                    }



                    val orderEntries = runReadAction { fileIndex.getOrderEntriesForFile(virtualFile) }
                    val iterator = orderEntries.iterator()
                    return MappingIterator(iterator) { orderEntry ->
                        collectByOrderEntry(
                            virtualFile,
                            orderEntry,
                            isLibrarySource,
                            visited,
                            config
                        )?.let(Result.Companion::success)
                    }
                }
            })
        }

    }

//    private fun collectByOrderEntry(
//        virtualFile: VirtualFile,
//        library: SyntheticLibrary,
//        isLibrarySource: Boolean,
//        visited: HashSet<ModuleInfo>,
//        config: Configuration,
//    ): ModuleInfo? {
//        if (library !is CjpmLibrary) {
//            return null
//        }
//        ProgressManager.checkCanceled()
//
//
//        return collectByLibrary(virtualFile, library, isLibrarySource, visited, config)
//
//    }

    private fun collectByOrderEntry(
        virtualFile: VirtualFile,
        orderEntry: OrderEntry,
        isLibrarySource: Boolean,
        visited: HashSet<ModuleInfo>,
        config: Configuration,
    ): ModuleInfo? {
        if (orderEntry is ModuleOrderEntry) {
            // Module-related entries are covered in 'collectModuleRelatedModuleInfosByFile()'
            return null
        }
        ProgressManager.checkCanceled()

        if (!orderEntry.isValid) {
            return null
        }
        if (orderEntry is LibraryOrderEntry) {
            val library = orderEntry.library
            if (library != null) {
                return collectByLibrary(virtualFile, library, isLibrarySource, visited, config)
            }
        }


        return null
    }

    private val libraryInfoCache by lazy { LibraryInfoCache.getInstance(project) }
//    private val cjpmLibraryInfoCache by lazy { CjpmLibraryInfoCache.getInstance(project) }


//    private fun collectByLibrary(
//        virtualFile: VirtualFile,
//        library: CjpmLibrary,
//        isLibrarySource: Boolean,
//        visited: HashSet<ModuleInfo>,
//        config: Configuration,
//    ): ModuleInfo? {
//        for (libraryInfo in cjpmLibraryInfoCache[library]) {
//            if (visited.add(libraryInfo)) {
////                    if (libraryInfo.isApplicable(sourceContext)) {
//                return libraryInfo
////                    }
//            }
//        }
//        return null
//    }

    private fun collectByLibrary(
        virtualFile: VirtualFile,
        library: Library,
        isLibrarySource: Boolean,
        visited: HashSet<ModuleInfo>,
        config: Configuration,
    ): ModuleInfo? {

        val sourceContext = config.contextualModuleInfo as? ModuleSourceInfo

        if (!isLibrarySource && RootKindFilter.libraryClasses.matches(project, virtualFile)) {
            for (libraryInfo in libraryInfoCache[library]) {
                if (visited.add(libraryInfo)) {
                    if (libraryInfo.isApplicable(sourceContext)) {
                        return libraryInfo
                    }
                }
            }
        } else if (isLibrarySource || RootKindFilter.libraryFiles.matches(project, virtualFile)) {
            for (libraryInfo in libraryInfoCache[library]) {


//                val moduleInfo = libraryInfo.sourcesModuleInfo
                if (visited.add(libraryInfo)) {
                    if (libraryInfo.isApplicable(sourceContext)) {
                        return libraryInfo
                    }
                }
            }
        }

        return null
    }

    private fun LibraryInfo.isApplicable(contextualModuleInfo: ModuleSourceInfo?): Boolean {
        if (contextualModuleInfo == null) return true

        val service = project.service<LibraryUsageIndex>()
        return service.hasDependentModule(this, contextualModuleInfo.module)
    }

    fun collect(element: PsiElement, config: Configuration = Configuration.Default): Sequence<Result<ModuleInfo>> {
        return seq {
            collectByElement(element, config)
        }
    }

}

fun Sequence<Result<ModuleInfo>>.unwrap(
    errorHandler: (String, Throwable) -> Unit,
    stopOnErrors: Boolean = true
): Sequence<ModuleInfo> {
    val originalSequence = this
    return object : Sequence<ModuleInfo> {
        override fun iterator(): Iterator<ModuleInfo> {
            return object : TransformingIterator<ModuleInfo>() {
                private var iterator: Iterator<Result<ModuleInfo>>? = originalSequence.iterator()
                override fun calculateHasNext(): Boolean = iterator?.hasNext() == true

                override fun calculateNext(): ModuleInfo? {
                    val iter = iterator ?: return null
                    val result = iter.next()
                    result.getOrNull()?.let { return it }

                    val error = result.exceptionOrNull()
                    if (error != null) {
                        errorHandler("Could not find correct module information", error)
                        if (stopOnErrors) {
                            iterator = null
                        }
                    }
                    return null
                }
            }
        }
    }
}

interface ModuleInfoProviderExtension {
    companion object {
        val EP_NAME: ExtensionPointName<ModuleInfoProviderExtension> =
            ExtensionPointName("com.huawei.cangjie.moduleinfo.moduleInfoProviderExtension")


    }

    fun SeqScope<Result<ModuleInfo>>.collectByElement(element: PsiElement, file: PsiFile, virtualFile: VirtualFile)
    fun SeqScope<Result<ModuleInfo>>.collectByFile(
        project: Project,
        virtualFile: VirtualFile,
        isLibrarySource: Boolean,
        config: ModuleInfoProvider.Configuration,
    )

    fun SeqScope<Module>.findContainingModules(project: Project, virtualFile: VirtualFile)
}

internal class DefaultModuleInfoProviderExtension : ModuleInfoProviderExtension {
    override fun SeqScope<Result<ModuleInfo>>.collectByElement(
        element: PsiElement,
        file: PsiFile,
        virtualFile: VirtualFile
    ) {

    }

    override fun SeqScope<Result<ModuleInfo>>.collectByFile(
        project: Project,
        virtualFile: VirtualFile,
        isLibrarySource: Boolean,
        config: ModuleInfoProvider.Configuration
    ) {

    }

    override fun SeqScope<Module>.findContainingModules(project: Project, virtualFile: VirtualFile) {
//        yield {
//            if (ScratchFileService.getInstance().getRootType(virtualFile) is ScratchRootType) {
//                ScriptRelatedModuleNameFile[project, virtualFile]?.let { scratchModuleName ->
//                    val moduleManager = ModuleManager.getInstance(project)
//                    moduleManager.findModuleByName(scratchModuleName)
//                }
//            } else {
//                null
//            }
//        }
    }
}
