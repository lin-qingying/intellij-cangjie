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

package org.cangnova.cangjie.moduleinfo.provider

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import org.cangnova.cangjie.config.CangJieSourceRootTypes
import org.cangnova.cangjie.moduleinfo.*
import org.cangnova.cangjie.moduleinfo.cache.LibraryInfoCache
import org.cangnova.cangjie.moduleinfo.util.asSourceInfo
import org.cangnova.cangjie.moduleinfo.util.customLibrary
import org.cangnova.cangjie.moduleinfo.util.customSourceRootTypeId
import org.cangnova.cangjie.projectStructure.RootKindFilter
import org.cangnova.cangjie.projectStructure.matches
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachmentsImpl


suspend fun SequenceScope<Result<IdeaModuleInfo>>.register(moduleInfo: IdeaModuleInfo): Unit = yield(Result.success(moduleInfo))

suspend fun SequenceScope<Result<IdeaModuleInfo>>.register(block: () -> IdeaModuleInfo?) {
    block()?.let { register(it) }
}

suspend fun SequenceScope<Result<IdeaModuleInfo>>.reportError(error: Throwable): Unit = yield(Result.failure(error))

interface ModuleInfoProviderExtension {
    companion object {
        val EP_NAME: ExtensionPointName<ModuleInfoProviderExtension> =
            ExtensionPointName("org.cangnova.cangjie.moduleInfoProviderExtension")
    }

    suspend fun SequenceScope<Result<IdeaModuleInfo>>.collectByElement(element: PsiElement, file: PsiFile, virtualFile: VirtualFile)
    suspend fun SequenceScope<Result<IdeaModuleInfo>>.collectByFile(
        project: Project,
        virtualFile: VirtualFile,
        isLibrarySource: Boolean,
        config: ModuleInfoProvider.Configuration,
    )

    suspend fun SequenceScope<Module>.findContainingModules(project: Project, virtualFile: VirtualFile)
}

@Service(Service.Level.PROJECT)
class ModuleInfoProvider(private val project: Project) {
    companion object {
        internal val LOG = Logger.getInstance(ModuleInfoProvider::class.java)

        fun getInstance(project: Project): ModuleInfoProvider = project.service()
    }

    data class Configuration(
        val createSourceLibraryInfoForLibraryBinaries: Boolean = true,
        val preferModulesFromExtensions: Boolean = false,
        val contextualModuleInfo: IdeaModuleInfo? = null,
    ) {
        companion object {
            val Default = Configuration()
        }
    }

    private val fileIndex by lazy { ProjectFileIndex.getInstance(project) }
    private val libraryInfoCache by lazy { LibraryInfoCache.getInstance(project) }

    fun collect(element: PsiElement, config: Configuration = Configuration.Default): Sequence<Result<IdeaModuleInfo>> {
        return sequence {
            collectByElement(element, config)
        }
    }

    fun collect(
        virtualFile: VirtualFile,
        isLibrarySource: Boolean = false,
        config: Configuration = Configuration.Default,
    ): Sequence<Result<IdeaModuleInfo>> {
        return sequence {
            collectByFile(virtualFile, isLibrarySource, config)
        }
    }

    private suspend fun SequenceScope<Result<IdeaModuleInfo>>.collectByElement(element: PsiElement, config: Configuration) {
        PsiUtilCore.ensureValid(element)
        val containingFile = element.containingFile

        if (element is PsiDirectory) {
            collectByFile(element.virtualFile, isLibrarySource = false, config)
            return
        }

        collectByUserData(UserDataModuleContainer.ForElement(element))

        if (containingFile == null) {
            val message = "Analyzing element of type ${element::class.java} with no containing file"
            reportError(CangJieExceptionWithAttachmentsImpl(message).withAttachment("element.cj", element.text))
            return
        }

        val containingCjFile = containingFile as? CjFile
        if (containingCjFile != null) {
            if (containingCjFile is CjCodeFragment) {
                val context = containingCjFile.getContext()
                if (context != null) {
                    collectByElement(context, config)
                } else {
                    val message = "Analyzing code fragment of type ${containingCjFile::class.java} with no context"
                    val error = CangJieExceptionWithAttachmentsImpl(message).withAttachment("file.cj", containingCjFile.text)
                    reportError(error)
                }
            }
        }

        val virtualFile = containingFile.originalFile.virtualFile
        if (virtualFile != null) {
            withCallExtensions(
                config = config,
                extensionBlock = { collectByElement(element, containingFile, virtualFile) },
            ) {
                val isLibrarySource = containingCjFile != null && isLibrarySource(containingCjFile, config)
                collectByFile(virtualFile, isLibrarySource, config)
            }
        } else {
            val message = "Analyzing element of type ${element::class.java} in non-physical file of type ${containingFile::class.java}"
            reportError(CangJieExceptionWithAttachmentsImpl(message).withAttachment("file.cj", containingFile.text))
        }
    }

    private inline fun callExtensions(block: ModuleInfoProviderExtension.() -> Unit) {
        for (extension in project.extensionArea.getExtensionPoint(ModuleInfoProviderExtension.EP_NAME).extensionList) {
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

        if (!config.preferModulesFromExtensions) {
            callExtensions(extensionBlock)
        }
    }

    private fun isLibrarySource(containingCjFile: CjFile, config: Configuration): Boolean {
        val isCompiled = containingCjFile.isCompiled
        return if (config.createSourceLibraryInfoForLibraryBinaries) isCompiled else !isCompiled
    }

    private suspend fun SequenceScope<Result<IdeaModuleInfo>>.collectByFile(
        virtualFile: VirtualFile,
        isLibrarySource: Boolean,
        config: Configuration,
    ) {
        collectByUserData(UserDataModuleContainer.ForVirtualFile(virtualFile, project))
        withCallExtensions(
            config = config,
            extensionBlock = { collectByFile(project, virtualFile, isLibrarySource, config) }
        ) {
            collectSourceRelatedByFile(virtualFile, config)

            val visited = hashSetOf<IdeaModuleInfo>()
            val collectionRequest = VirtualFileCollectionRequest(virtualFile, isLibrarySource, config, visited, project)

            // Several libraries may include the same JAR files.
            // Below, we use an index for getting order entries for a file, but entries come in an arbitrary order.
            // So if we are already inside a library, we scan it first.

            val contextualModuleResult = contextByContextualBinaryModule(collectionRequest)
            contextualModuleResult?.let { yield(Result.success(it)) }

            val orderEntries = runReadAction { fileIndex.getOrderEntriesForFile(virtualFile) }
            yieldAll(
                orderEntries.asSequence().mapNotNull { orderEntry ->
                    collectByOrderEntry(collectionRequest, orderEntry)?.let(Result.Companion::success)
                }
            )
        }
    }

    private class VirtualFileCollectionRequest(
        val virtualFile: VirtualFile,
        val isLibrarySource: Boolean,
        val config: Configuration,
        val visited: HashSet<IdeaModuleInfo>,
        val project: Project,
    ) {
        val hasLibraryClassesRootKind: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
            RootKindFilter.libraryClasses.matches(project, virtualFile)
        }

        val hasLibraryFilesRootKind: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
            RootKindFilter.libraryFiles.matches(project, virtualFile)
        }
    }

    private fun contextByContextualBinaryModule(collectionRequest: VirtualFileCollectionRequest): IdeaModuleInfo? {
        val contextualModuleInfo = collectionRequest.config.contextualModuleInfo ?: return null

        val contentScope = when (contextualModuleInfo) {
            is LibraryInfo -> contextualModuleInfo.contentScope
            is LibrarySourceInfo -> contextualModuleInfo.sourceScope()
            else -> null
        }

        val virtualFile = collectionRequest.virtualFile
        if (contentScope == null || virtualFile !in contentScope) {
            return null
        }

        return when (contextualModuleInfo) {
            is LibraryInfo -> collectByLibrary(collectionRequest, contextualModuleInfo.library)
            is LibrarySourceInfo -> collectByLibrary(collectionRequest, contextualModuleInfo.library)
            else -> null
        }
    }

    private suspend fun SequenceScope<Result<IdeaModuleInfo>>.collectSourceRelatedByFile(virtualFile: VirtualFile, config: Configuration) {
        val modules = sequence {
            withCallExtensions(
                config = config,
                extensionBlock = { findContainingModules(project, virtualFile) },
            ) {
                runReadAction { fileIndex.getModuleForFile(virtualFile) }?.let { module ->
                    yield(module)
                }
            }
        }
        yieldAll(
            modules.mapNotNull { module ->
                if (module.isDisposed) return@mapNotNull null
                val projectFileIndex = ProjectFileIndex.getInstance(project)
                val sourceRootTypeId: String? = projectFileIndex.getCangJieSourceRootTypeId(virtualFile)
                module.asSourceInfo(sourceRootTypeId)?.let(Result.Companion::success)
            }
        )
    }

    /**
     * 获取文件对应的仓颉源根类型标识符
     */
    private fun ProjectFileIndex.getCangJieSourceRootTypeId(virtualFile: VirtualFile): String? {
        return when {
            isInTestSourceContent(virtualFile) -> CangJieSourceRootTypes.TEST
            isInSourceContent(virtualFile) -> CangJieSourceRootTypes.SOURCE
            else -> null
        }
    }

    private fun collectByOrderEntry(
        collectionRequest: VirtualFileCollectionRequest,
        orderEntry: OrderEntry,
    ): IdeaModuleInfo? {
        if (orderEntry is ModuleOrderEntry) {
            return null
        }

        ProgressManager.checkCanceled()

        if (!orderEntry.isValid) {
            return null
        }

        if (orderEntry is LibraryOrderEntry) {
            val library = orderEntry.library
            if (library != null) {
                return collectByLibrary(collectionRequest, library)
            }
        }

        return null
    }

    private fun collectByLibrary(
        collectionRequest: VirtualFileCollectionRequest,
        library: Library,
    ): IdeaModuleInfo? {
        val config = collectionRequest.config
        val sourceContext = config.contextualModuleInfo as? ModuleSourceInfo
        val useLibrarySource = collectionRequest.isLibrarySource || (config.contextualModuleInfo as? LibrarySourceInfo)?.library == library

        if (!useLibrarySource && collectionRequest.hasLibraryClassesRootKind) {
            for (libraryInfo in libraryInfoCache[library]) {
                val isNew = collectionRequest.visited.add(libraryInfo)
                if (isNew && libraryInfo.isApplicable(sourceContext)) {
                    return libraryInfo
                }
            }
        } else if (useLibrarySource || collectionRequest.hasLibraryFilesRootKind) {
            for (libraryInfo in libraryInfoCache[library]) {
                val sourceInfo = libraryInfo.sourcesModuleInfo
                val isNew = collectionRequest.visited.add(sourceInfo)
                if (isNew && libraryInfo.isApplicable(sourceContext)) {
                    return sourceInfo
                }
            }
        }

        return null
    }

    private fun LibraryInfo.isApplicable(contextualModuleInfo: ModuleSourceInfo?): Boolean {
        if (contextualModuleInfo == null) return true
        return contextualModuleInfo.module.hasLibraryInTransitiveDependencies(library)
    }

    private fun Module.hasLibraryInTransitiveDependencies(library: Library): Boolean {
        var result = false
        ModuleRootManager.getInstance(this).orderEntries()
            .librariesOnly()
            .recursively().exportedOnly()
            .forEachLibrary {
                if (it == library) {
                    result = true
                    return@forEachLibrary false
                }
                true
            }
        return result
    }

    private suspend fun SequenceScope<Result<IdeaModuleInfo>>.collectByUserData(container: UserDataModuleContainer) {
        register {
            val sourceRootTypeId = container.customSourceRootTypeId
            if (sourceRootTypeId == null) return@register null

            container.module?.asSourceInfo(sourceRootTypeId)?.let { moduleInfo ->
                return@register moduleInfo
            }
            null
        }

        container.customLibrary?.let { library ->
            for (libraryInfo in libraryInfoCache[library]) {
                register(libraryInfo)
            }
        }
    }
}

private sealed class UserDataModuleContainer {
    abstract val module: Module?

    val customSourceRootTypeId: String?
        get() = holders.firstNotNullOfOrNull { it.customSourceRootTypeId }

    val customLibrary: Library?
        get() = holders.firstNotNullOfOrNull { it.customLibrary }

    abstract val holders: List<UserDataHolder>

    data class ForVirtualFile(val virtualFile: VirtualFile, val project: Project) : UserDataModuleContainer() {
        override val module: Module?
            get() = ModuleUtilCore.findModuleForFile(virtualFile, project)

        override val holders: List<UserDataHolder>
            get() = listOf(virtualFile)
    }

    data class ForElement(val psiElement: PsiElement) : UserDataModuleContainer() {
        override val module: Module?
            get() = ModuleUtilCore.findModuleForPsiElement(psiElement)

        override val holders: List<UserDataHolder> by lazy {
            val containingFile = psiElement.containingFile
            listOfNotNull(
                psiElement,
                containingFile,
                containingFile?.originalFile?.virtualFile
            )
        }
    }
}


fun Sequence<Result<IdeaModuleInfo>>.unwrap(
    errorHandler: (String, Throwable) -> Unit,
): Sequence<IdeaModuleInfo> = sequence {
    for (result in this@unwrap) {
        val successResult = result.getOrNull()
        if (successResult != null) {
            yield(successResult)
            continue
        }
        val error = result.exceptionOrNull()
        if (error != null) {
            errorHandler("Could not find correct module information", error)
            break
        }
    }
}
