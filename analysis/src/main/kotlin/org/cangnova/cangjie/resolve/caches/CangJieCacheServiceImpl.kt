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

package org.cangnova.cangjie.resolve.caches

import com.intellij.execution.Platform
import com.intellij.execution.target.TargetPlatform
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.cangnova.cangjie.ExceptionTracker
import org.cangnova.cangjie.context.GlobalContext
import org.cangnova.cangjie.context.GlobalContextImpl
import org.cangnova.cangjie.descriptors.AnalysisContext
import org.cangnova.cangjie.descriptors.AnalysisContextProvider
import org.cangnova.cangjie.descriptors.NotUnderContentRootModuleInfo
import org.cangnova.cangjie.descriptors.analysisContext
import org.cangnova.cangjie.descriptors.isLibraryContext
import org.cangnova.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.cangnova.cangjie.projectStructure.RootKindFilter
import org.cangnova.cangjie.projectStructure.matches
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.NotNullableUserDataProperty
import org.cangnova.cangjie.psi.psiUtil.contains
import org.cangnova.cangjie.resolve.ModuleResolutionFacadeImpl
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.ResolverForProject.Companion.resolverForLibrariesName
import org.cangnova.cangjie.resolve.ResolverForProject.Companion.resolverForModulesName
import org.cangnova.cangjie.resolve.ResolverForProject.Companion.resolverForSpecialInfoName
import org.cangnova.cangjie.storage.LockBasedStorageManager
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachmentsImpl
import org.cangnova.cangjie.utils.sumByLong


internal val LOG = Logger.getInstance(CangJieCacheService::class.java)

data class PlatformAnalysisSettingsImpl(
    val platform: TargetPlatform,

    ) : PlatformAnalysisSettings

class CangJieCacheServiceImpl(val project: Project) : CangJieCacheService {
    override fun getResolutionFacade(element: CjElement): ResolutionFacade {
        val file = element.fileForElement()

        return CachedValuesManager.getCachedValue(file) {
            val settings = PlatformAnalysisSettingsImpl(TargetPlatform(Platform.WINDOWS))

            CachedValueProvider.Result(
                getFacadeToAnalyzeFile(file, settings),

                ProjectRootModificationTracker.getInstance(project),
            )
        }
    }

    private fun <K, V> SLRUCache<K, V>.getOrCreateValue(key: K): V =
        synchronized(this) {
            this.getIfCached(key)
        } ?: run {
            // do actual value calculation out of any locks
            // trade-off: several instances could be created, but only one would be used
            val newValue = this.createValue(key)
            synchronized(this) {
                val cached = this.getIfCached(key)
                cached ?: run {
                    newValue?.let { this.put(key, it) }
                    newValue
                }
            }
        }

    private val globalFacadesPerPlatformAndSdk: SLRUCache<String, GlobalFacade> =
        SLRUCache.slruCache(2 * 3 * 2, 2 * 3 * 2) { GlobalFacade() }

    private fun facadeForModules(/*settings: PlatformAnalysisSettings*/) =
        getOrBuildGlobalFacade(/*settings*/).facadeForModules

    private fun librariesFacade() =
        getOrBuildGlobalFacade().facadeForLibraries


    @Synchronized
    private fun getOrBuildGlobalFacade(/*settings: PlatformAnalysisSettings*/) =
        globalFacadesPerPlatformAndSdk["CangJie"]


    private inner class GlobalFacade {
        private val context = GlobalContext("cangjie", project)

        /**
         * 获取所有分析上下文（包括模块和依赖）
         *
         * 从 AnalysisContextProvider 获取项目中的所有上下文，包括：
         * - 源码模块上下文
         * - 库依赖上下文（包括 stdlib）
         * - 其他类型的上下文
         *
         * 这个列表会被传递给 ProjectResolutionFacade，最终填充到 AbstractResolverForProject.allModules 中。
         */
        private val allAnalysisContexts: Collection<AnalysisContext> by lazy {
            AnalysisContextProvider.getAllContexts(project)
        }

        private val librariesContext = context.contextWithCompositeExceptionTracker(project, resolverForLibrariesName)

        val facadeForLibraries = ProjectResolutionFacade(
            "facadeForLibraries", "$resolverForLibrariesName  ",
            project, context,
            reuseDataFrom = null,
            moduleFilter = { it.isLibraryContext },
            invalidateOnOOCB = false,
            dependencies = listOf(
                ProjectRootModificationTracker.getInstance(project)
            ),
            allModules = allAnalysisContexts  // ✅ 传入所有上下文（包括 stdlib）
        )

        private val modulesContext =
            librariesContext.contextWithCompositeExceptionTracker(project, resolverForModulesName)

        val facadeForModules = ProjectResolutionFacade(
            "facadeForModules", resolverForModulesName,
            project, modulesContext,
            reuseDataFrom = facadeForLibraries,
            moduleFilter = { true },
            dependencies = listOf(ProjectRootModificationTracker.getInstance(project)),
            invalidateOnOOCB = true,
            allModules = allAnalysisContexts  // ✅ 传入所有上下文（包括 stdlib）
        )
    }


    private fun getFacadeToAnalyzeFile(file: CjFile, settings: PlatformAnalysisSettings): ResolutionFacade {
        val analysisContext = file.analysisContext
        val specialFile = filterNotInProjectSource(file, analysisContext)

        if (specialFile != null) {
            val specialFiles = setOf(specialFile)
            val projectFacade = getFacadeForSpecialFiles(specialFiles, settings)
            return ModuleResolutionFacadeImpl(projectFacade, analysisContext).createdFor(specialFiles, analysisContext)
        }
        return getResolutionFacadeByModuleInfo(analysisContext /*, settings*/).createdFor(
            emptyList(),
            analysisContext/*, settings*/
        )

    }


    override fun getResolutionFacadeByModuleInfo(context: AnalysisContext): ResolutionFacade {
//        val settings = moduleInfo.platformSettings(platform)
        val projectFacade = facadeForModules(/*settings*/)

        return ModuleResolutionFacadeImpl(projectFacade, context)
    }

    override fun getResolutionFacade(elements: List<CjElement>): ResolutionFacade {

        val files = getFilesForElements(elements)
        if (files.size == 1) return getResolutionFacade(files.single())


        return getFacadeToAnalyzeFiles(files/*, settings*/)

    }

    private fun CjCodeFragment.getContextFile(): CjFile? {
        val contextElement = context ?: return null
        val contextFile = (contextElement as? CjElement)?.getContainingCjFile()
            ?: throw AssertionError("Analyzing cangjie code fragment of type ${this::class.java} with java context of type ${contextElement::class.java}")
        return if (contextFile is CjCodeFragment) contextFile.getContextFile() else contextFile
    }

    private fun Collection<CjFile>.filterNotInProjectSource(context: AnalysisContext): Set<CjFile> =
        mapNotNullTo(mutableSetOf()) { filterNotInProjectSource(it, context) }

    private fun filterNotInProjectSource(file: CjFile, context: AnalysisContext): CjFile? {
        val fileToAnalyze = when (file) {
            is CjCodeFragment -> file.getContextFile()
            else -> file
        }

        if (fileToAnalyze == null) {
            return null
        }

        val isInProjectSource = RootKindFilter.projectSources.matches(fileToAnalyze)
                && context.scope.contains(fileToAnalyze)

        return if (!isInProjectSource) fileToAnalyze else null
    }

    private fun getFacadeForSpecialFiles(
        files: Set<CjFile>,
        settings: PlatformAnalysisSettings
    ): ProjectResolutionFacade {
        val cachedValue: SLRUCache<Pair<Set<CjFile>, PlatformAnalysisSettings>, ProjectResolutionFacade> =
            CachedValuesManager.getManager(project).getCachedValue(project, specialFilesCacheProvider)

        // In Upsource, we create multiple instances of KotlinCacheService, which all access the same CachedValue instance (UP-8046)
        // This is so because class name of provider is used as a key when fetching cached value, see CachedValueManager.getKeyForClass.
        // To avoid race conditions, we can't use any local lock to access the cached value contents.
        return cachedValue.getOrCreateValue(files to settings)
    }

    private fun getFacadeToAnalyzeFiles(files: Collection<CjFile>/*, settings: PlatformAnalysisSettings*/): ResolutionFacade {
        val moduleInfo = files.first().analysisContext
        val specialFiles = files.filterNotInProjectSource(moduleInfo)


        if (specialFiles.isNotEmpty()) {
            val projectFacade = getFacadeForSpecialFiles(specialFiles, DefaultPlatformAnalysisSettings)
            return ModuleResolutionFacadeImpl(projectFacade, moduleInfo).createdFor(specialFiles, moduleInfo)
        }

        return getResolutionFacadeByModuleInfo(moduleInfo/*, settings*/).createdFor(
            emptyList(),
            moduleInfo/*, settings*/
        )
    }

    private val specialFilesCacheProvider = CachedValueProvider {
        // NOTE: computations inside createFacadeForFilesWithSpecialModuleInfo depend on project root structure
        // so we additionally drop the whole slru cache on change
        CachedValueProvider.Result(
            SLRUCache.slruCache<Pair<Set<CjFile>, PlatformAnalysisSettings>, ProjectResolutionFacade>(2, 3) {
                createFacadeForFilesWithSpecialModuleInfo(it.first, it.second)
            },

            ProjectRootModificationTracker.getInstance(project)
        )
    }

    // explicitSettings allows to override the "innate" settings of the files' moduleInfo
    // This can be useful, if the module is common, but we want to create a facade to
    private fun createFacadeForFilesWithSpecialModuleInfo(
        files: Set<CjFile>,
        explicitSettings: PlatformAnalysisSettings? = null
    ): ProjectResolutionFacade {
        // we assume that all files come from the same module
        val specialContext = files.map { it.analysisContext }.toSet().single()

        // Dummy files created e.g. by J2K do not receive events.
        val dependencyTrackerForSyntheticFileCache = if (files.all { it.originalFile != it }) {
            ModificationTracker { files.sumByLong { it.outOfBlockModificationCount } }
        } else ModificationTracker { files.sumByLong { it.modificationStamp } }

        val resolverDebugName =
            "$resolverForSpecialInfoName $specialContext for files ${files.joinToString { it.name }} "

        fun makeProjectResolutionFacade(
            debugName: String,
            globalContext: GlobalContextImpl,
            reuseDataFrom: ProjectResolutionFacade? = null,
            moduleFilter: (AnalysisContext) -> Boolean = { true },
            allModules: Collection<AnalysisContext>? = null
        ): ProjectResolutionFacade {
            return ProjectResolutionFacade(
                debugName,
                resolverDebugName,
                project,
                globalContext,
//                settings,
                syntheticFiles = files,
                reuseDataFrom = reuseDataFrom,
                moduleFilter = moduleFilter,
                dependencies = listOf(
                    dependencyTrackerForSyntheticFileCache,
                    ProjectRootModificationTracker.getInstance(project)
                ),
                invalidateOnOOCB = true,
                allModules = allModules
            )
        }

        return when {
            !specialContext.isSourceContext || specialContext is NotUnderContentRootModuleInfo -> {
                val librariesFacade = librariesFacade()
                val debugName = "facadeForSpecialContext (Library or NotUnderContentRoot)"
                val globalContext =
                    librariesFacade.globalContext.contextWithCompositeExceptionTracker(project, debugName)
                makeProjectResolutionFacade(
                    debugName,
                    globalContext,
                    reuseDataFrom = librariesFacade,
                    moduleFilter = { it == specialContext }
                )
            }

            specialContext.isSourceContext -> {
                // 获取依赖模块：当前上下文及其所有依赖
                val dependentModules = listOf(specialContext) + specialContext.dependencies
                val modulesFacade = facadeForModules()
                val globalContext =
                    modulesFacade.globalContext.contextWithCompositeExceptionTracker(
                        project,
                        "facadeForSpecialContext (SourceContext)"
                    )
                makeProjectResolutionFacade(
                    "facadeForSpecialContext (SourceContext)",
                    globalContext,
                    reuseDataFrom = modulesFacade,
                    moduleFilter = { it in dependentModules }
                )
            }

            specialContext.isLibraryContext -> {
                //NOTE: this code should not be called for sdk or library classes
                // currently the only known scenario is when we cannot determine that file is a library source
                // (file under both classes and sources root)
                LOG.warn("Creating cache with synthetic files ($files) in classes of library $specialContext")
                val globalContext =
                    GlobalContext("facadeForSpecialContext for file under both classes and root", project)
                makeProjectResolutionFacade(
                    "facadeForSpecialContext for file under both classes and root",
                    globalContext
                )
            }

            else -> throw IllegalStateException("Unknown AnalysisContext ${specialContext.javaClass}")
        }
    }

    private fun getFilesForElements(elements: List<CjElement>): List<CjFile> {
        return elements.map {
            it.fileForElement()
        }.distinct()
    }


    private fun CjElement.fileForElement() = try {

        @Suppress("USELESS_ELVIS")
        getContainingCjFile() ?: throw IllegalStateException("containingCjFile was null for $this of ${this.javaClass}")
    } catch (e: Exception) {
        if (e is ControlFlowException) throw e
        throw CangJieExceptionWithAttachmentsImpl("Couldn't get containingCjFile for cjElement", e)
            .withPsiAttachment("element", this)
            .withPsiAttachment("file", this.containingFile)
            .withAttachment("original", e.message)
    }
}

internal interface ModuleFilters {
    fun sdkFacadeFilter(module: AnalysisContext): Boolean
    fun libraryFacadeFilter(module: AnalysisContext): Boolean
    fun moduleFacadeFilter(module: AnalysisContext): Boolean
}

//internal class GlobalFacadeModuleFilters(project: Project) : ModuleFilters {
//    private val impl = when (IdeBuiltInsLoadingState.state) {
//        IdeBuiltInsLoadingState.IdeBuiltInsLoading.FROM_CLASSLOADER -> ClassLoaderBuiltInsModuleFilters
//        IdeBuiltInsLoadingState.IdeBuiltInsLoading.FROM_DEPENDENCIES_JVM -> DependencyBuiltinsModuleFilters(project)
//    }
//
//    override fun sdkFacadeFilter(module: AnalysisContext): Boolean = impl.sdkFacadeFilter(module)
//    override fun libraryFacadeFilter(module: AnalysisContext): Boolean = impl.libraryFacadeFilter(module)
//    override fun moduleFacadeFilter(module: AnalysisContext): Boolean = impl.moduleFacadeFilter(module)
//}
internal fun GlobalContextImpl.contextWithCompositeExceptionTracker(
    project: Project,
    debugName: String
): GlobalContextImpl =
//    if (project.useCompositeAnalysis || project.useLibraryToSourceAnalysis) {
//        this.contextWithCompositeExceptionTracker(name)
//    } else {
    this.contextWithNewLockAndCompositeExceptionTracker(project, debugName)
//    }

private fun GlobalContextImpl.contextWithNewLockAndCompositeExceptionTracker(
    project: Project,
    debugName: String
): GlobalContextImpl {
    val newExceptionTracker = CompositeExceptionTracker(this.exceptionTracker)
    return GlobalContextImpl(
        LockBasedStorageManager.createWithExceptionHandling(

            debugName,
            newExceptionTracker,
            {
                ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            },
            { throw ProcessCanceledException(it) }),
        newExceptionTracker
    )
}

private class CompositeExceptionTracker(val delegate: ExceptionTracker) : ExceptionTracker() {
    override fun getModificationCount(): Long {
        return super.getModificationCount() + delegate.modificationCount
    }
}

/**
 * Note that CangJie Resolution can work in a mode, when some operations are performed in COMPOSITE mode, and some in SEPARATE.
 * This specific property only shows global project-wide setting, so use it with a lot of caution.
 */
//val Project.useCompositeAnalysis: Boolean
//    get() = CangJieMultiplatformAnalysisModeComponent.getMode(this) == CangJieMultiplatformAnalysisModeComponent.Mode.COMPOSITE
private val FILE_OUT_OF_BLOCK_MODIFICATION_COUNT = Key<Long>("FILE_OUT_OF_BLOCK_MODIFICATION_COUNT")
val CjFile.outOfBlockModificationCount: Long by NotNullableUserDataProperty(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT, 0)
