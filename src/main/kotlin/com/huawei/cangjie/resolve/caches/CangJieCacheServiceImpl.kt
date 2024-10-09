package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.analyzer.LibraryInfo
import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.analyzer.ModuleSourceInfo
import com.huawei.cangjie.analyzer.ResolverForProject.Companion.resolverForLibrariesName
import com.huawei.cangjie.analyzer.ResolverForProject.Companion.resolverForModulesName
import com.huawei.cangjie.analyzer.ResolverForProject.Companion.resolverForSpecialInfoName
import com.huawei.cangjie.analyzer.isLibraryClasses
import com.huawei.cangjie.context.GlobalContext
import com.huawei.cangjie.context.GlobalContextImpl
import com.huawei.cangjie.ide.base.projectStructure.RootKindFilter
import com.huawei.cangjie.ide.base.projectStructure.matches
import com.huawei.cangjie.ide.cache.SLRUCache
import com.huawei.cangjie.ide.cache.project.getDependentModules
import com.huawei.cangjie.ide.projectStructure.moduleInfo
import com.huawei.cangjie.ide.projectStructure.moduleInfo.NotUnderContentRootModuleInfo
import com.huawei.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import com.huawei.cangjie.psi.CjCodeFragment
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.NotNullableUserDataProperty
import com.huawei.cangjie.psi.psiUtil.contains
import com.huawei.cangjie.resolve.ModuleResolutionFacadeImpl
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.storage.ExceptionTracker
import com.huawei.cangjie.storage.LockBasedStorageManager
import com.huawei.cangjie.utils.CangJieExceptionWithAttachments
import com.huawei.cangjie.utils.sumByLong
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
                    this.put(key, newValue)
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
        private val context = GlobalContext("cjpm", project)
//        private val sdkContext = GlobalContext(resolverForSdkName)
//        private val moduleFilters = GlobalFacadeModuleFilters(project)
//        val facadeForSdk = ProjectResolutionFacade(
//            "facadeForSdk", "$resolverForSdkName with settings=$settings",
//            project, sdkContext, settings,
//            moduleFilter = moduleFilters::sdkFacadeFilter,
//            dependencies = listOf(

//                ProjectRootModificationTracker.getInstance(project)
//            ),
//            invalidateOnOOCB = false,
//            reuseDataFrom = null
//        )

        //        private val moduleFilters = GlobalFacadeModuleFilters(project)
        private val librariesContext = context.contextWithCompositeExceptionTracker(project, resolverForLibrariesName)

        val facadeForLibraries = ProjectResolutionFacade(
            "facadeForLibraries", "",
            project, context,
            reuseDataFrom = null,
            moduleFilter = { true },
            invalidateOnOOCB = false,
            dependencies = listOf(

                ProjectRootModificationTracker.getInstance(project)
            )
        )
        private val modulesContext =
            librariesContext.contextWithCompositeExceptionTracker(project, resolverForModulesName)

        val facadeForModules = ProjectResolutionFacade(
            "facadeForModules", /*"sdk with settings=$settings"*/"",
            project, modulesContext,
            reuseDataFrom = facadeForLibraries,
//            moduleFilter = moduleFilters::sdkFacadeFilter,
            moduleFilter = {
                true
            },

            dependencies = listOf(ProjectRootModificationTracker.getInstance(project)),
            invalidateOnOOCB = true
        )
    }


    private fun getFacadeToAnalyzeFile(file: CjFile, settings: PlatformAnalysisSettings): ResolutionFacade {

        val moduleInfo = file.moduleInfo
        val specialFile = filterNotInProjectSource(file, moduleInfo)

//        val projectFacade = facadeForModules(settings)
//
//        return ResolutionFacadeImpl(projectFacade).createdFor(emptyList(),/* moduleInfo,*/ settings)
        if (specialFile != null) {
            val specialFiles = setOf(specialFile)
            val projectFacade = getFacadeForSpecialFiles(specialFiles, settings)
            return ModuleResolutionFacadeImpl(projectFacade, moduleInfo).createdFor(specialFiles, moduleInfo )
        }
        return getResolutionFacadeByModuleInfo (moduleInfo /*, settings*/).createdFor(
            emptyList(),
            moduleInfo/*, settings*/
        )

    }



    override fun getResolutionFacadeByModuleInfo(moduleInfo: ModuleInfo): ResolutionFacade {
//        val settings = moduleInfo.platformSettings(platform)
        val projectFacade = facadeForModules(/*settings*/)

        return ModuleResolutionFacadeImpl(projectFacade, moduleInfo)
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

    private fun Collection<CjFile>.filterNotInProjectSource(moduleInfo: ModuleInfo): Set<CjFile> =
        mapNotNullTo(mutableSetOf()) { filterNotInProjectSource(it, moduleInfo) }

    private fun filterNotInProjectSource(file: CjFile, moduleInfo: ModuleInfo): CjFile? {
        val fileToAnalyze = when (file) {
            is CjCodeFragment -> file.getContextFile()
            else -> file
        }

        if (fileToAnalyze == null) {
            return null
        }

        val isInProjectSource = RootKindFilter.projectSources.matches(fileToAnalyze)
                && moduleInfo.contentScope.contains(fileToAnalyze)

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
        val moduleInfo = files.first().moduleInfo
        val specialFiles = files.filterNotInProjectSource(moduleInfo)


        if (specialFiles.isNotEmpty()) {
            val projectFacade = getFacadeForSpecialFiles(specialFiles, DefaultPlatformAnalysisSettings)
            return ModuleResolutionFacadeImpl(projectFacade, moduleInfo).createdFor(specialFiles, moduleInfo)
        }

        return getResolutionFacadeByModuleInfo (moduleInfo/*, settings*/).createdFor(
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

        val specialModuleInfo = files.map { it.moduleInfo }.toSet().single()
//        val settings = explicitSettings ?: specialModuleInfo.platformSettings(specialModuleInfo.platform)

        // Dummy files created e.g. by J2K do not receive events.
        val dependencyTrackerForSyntheticFileCache = if (files.all { it.originalFile != it }) {
            ModificationTracker { files.sumByLong { it.outOfBlockModificationCount } }
        } else ModificationTracker { files.sumByLong { it.modificationStamp } }

        val resolverDebugName =
            "$resolverForSpecialInfoName $specialModuleInfo for files ${files.joinToString { it.name }} "

        fun makeProjectResolutionFacade(
            debugName: String,
            globalContext: GlobalContextImpl,
            reuseDataFrom: ProjectResolutionFacade? = null,
            moduleFilter: (ModuleInfo) -> Boolean = { true },
            allModules: Collection<ModuleInfo>? = null
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


            specialModuleInfo is LibraryInfo || specialModuleInfo is NotUnderContentRootModuleInfo -> {
                val librariesFacade = librariesFacade()
                val debugName = "facadeForSpecialModuleInfo (LibrarySourceInfo or NotUnderContentRootModuleInfo)"
                val globalContext =
                    librariesFacade.globalContext.contextWithCompositeExceptionTracker(project, debugName)
                makeProjectResolutionFacade(
                    debugName,
                    globalContext,
                    reuseDataFrom = librariesFacade,
                    moduleFilter = { it == specialModuleInfo }
                )
            }

            specialModuleInfo is ModuleSourceInfo -> {
                val dependentModules = specialModuleInfo.getDependentModules()
                val modulesFacade = facadeForModules()
                val globalContext =
                    modulesFacade.globalContext.contextWithCompositeExceptionTracker(
                        project,
                        "facadeForSpecialModuleInfo (ModuleSourceInfo)"
                    )
                makeProjectResolutionFacade(
                    "facadeForSpecialModuleInfo (ModuleSourceInfo)",
                    globalContext,
                    reuseDataFrom = modulesFacade,
                    moduleFilter = { it in dependentModules }
                )
            }

            specialModuleInfo.isLibraryClasses() -> {
                //NOTE: this code should not be called for sdk or library classes
                // currently the only known scenario is when we cannot determine that file is a library source
                // (file under both classes and sources root)
                LOG.warn("Creating cache with synthetic files ($files) in classes of library $specialModuleInfo")
                val globalContext =
                    GlobalContext("facadeForSpecialModuleInfo for file under both classes and root", project)
                makeProjectResolutionFacade(
                    "facadeForSpecialModuleInfo for file under both classes and root",
                    globalContext
                )
            }

            else -> throw IllegalStateException("Unknown IdeaModuleInfo ${specialModuleInfo::class.java}")
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
        throw CangJieExceptionWithAttachments("Couldn't get containingCjFile for cjElement", e)
            .withPsiAttachment("element", this)
            .withPsiAttachment("file", this.containingFile)
            .withAttachment("original", e.message)
    }
}

private fun GlobalContextImpl.contextWithCompositeExceptionTracker(debugName: String): GlobalContextImpl {
    val newExceptionTracker = CompositeExceptionTracker(this.exceptionTracker)
    return GlobalContextImpl(
        storageManager.replaceExceptionHandling(this.storageManager.project, debugName, newExceptionTracker),
        newExceptionTracker
    )
}

internal interface ModuleFilters {
    fun sdkFacadeFilter(module: ModuleInfo): Boolean
    fun libraryFacadeFilter(module: ModuleInfo): Boolean
    fun moduleFacadeFilter(module: ModuleInfo): Boolean
}

//internal class GlobalFacadeModuleFilters(project: Project) : ModuleFilters {
//    private val impl = when (IdeBuiltInsLoadingState.state) {
//        IdeBuiltInsLoadingState.IdeBuiltInsLoading.FROM_CLASSLOADER -> ClassLoaderBuiltInsModuleFilters
//        IdeBuiltInsLoadingState.IdeBuiltInsLoading.FROM_DEPENDENCIES_JVM -> DependencyBuiltinsModuleFilters(project)
//    }
//
//    override fun sdkFacadeFilter(module: ModuleInfo): Boolean = impl.sdkFacadeFilter(module)
//    override fun libraryFacadeFilter(module: ModuleInfo): Boolean = impl.libraryFacadeFilter(module)
//    override fun moduleFacadeFilter(module: ModuleInfo): Boolean = impl.moduleFacadeFilter(module)
//}
internal fun GlobalContextImpl.contextWithCompositeExceptionTracker(
    project: Project,
    debugName: String
): GlobalContextImpl =
//    if (project.useCompositeAnalysis || project.useLibraryToSourceAnalysis) {
//        this.contextWithCompositeExceptionTracker(debugName)
//    } else {
    this.contextWithNewLockAndCompositeExceptionTracker(debugName)
//    }

private fun GlobalContextImpl.contextWithNewLockAndCompositeExceptionTracker(debugName: String): GlobalContextImpl {
    val newExceptionTracker = CompositeExceptionTracker(this.exceptionTracker)
    return GlobalContextImpl(
        LockBasedStorageManager.createWithExceptionHandling(
            this.storageManager.project,
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
