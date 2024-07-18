package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.context.GlobalContext
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.ResolutionFacadeImpl
import com.huawei.cangjie.utils.CangJieExceptionWithAttachments
import com.intellij.execution.Platform
import com.intellij.execution.target.TargetPlatform
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.SLRUCache

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

    private val globalFacadesPerPlatformAndSdk: SLRUCache<PlatformAnalysisSettings, GlobalFacade> =
        SLRUCache.slruCache(2 * 3 * 2, 2 * 3 * 2) { GlobalFacade(it) }

    private fun facadeForModules(settings: PlatformAnalysisSettings) =
        getOrBuildGlobalFacade(settings).facadeForModules


    @Synchronized
    private fun getOrBuildGlobalFacade(settings: PlatformAnalysisSettings) =
        globalFacadesPerPlatformAndSdk[settings]

    private inner class GlobalFacade(settings: PlatformAnalysisSettings) {
        private val context = GlobalContext("sdk")

        val facadeForModules = ProjectResolutionFacade(
            "facadeForModules", "sdk with settings=$settings",
            project, context,

            dependencies = listOf(ProjectRootModificationTracker.getInstance(project)),
            invalidateOnOOCB = true
        )
    }


    private fun getFacadeToAnalyzeFile(file: CjFile, settings: PlatformAnalysisSettings): ResolutionFacade {


        val projectFacade = facadeForModules(settings)

        return ResolutionFacadeImpl(projectFacade).createdFor(emptyList(),/* moduleInfo,*/ settings)
    }

    override fun getResolutionFacade(elements: List<CjElement>): ResolutionFacade {
        TODO("Not yet implemented")
    }

    private fun getFilesForElements(elements: List<CjElement>): List<CjFile> {
        return elements.map {
            it.fileForElement()
        }.distinct()
    }


    private fun CjElement.fileForElement() = try {
        // in theory `containingKtFile` is `@NotNull` but in practice EA-114080
        @Suppress("USELESS_ELVIS")
        getContainingCjFile() ?: throw IllegalStateException("containingKtFile was null for $this of ${this.javaClass}")
    } catch (e: Exception) {
        if (e is ControlFlowException) throw e
        throw CangJieExceptionWithAttachments("Couldn't get containingKtFile for ktElement", e)
            .withPsiAttachment("element", this)
            .withPsiAttachment("file", this.containingFile)
            .withAttachment("original", e.message)
    }
}
