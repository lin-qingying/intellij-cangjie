package com.linqingying.cangjie.ide.cache.trackers

import com.linqingying.cangjie.ide.cache.cacheByClassInvalidatingOnRootModifications
import com.linqingying.cangjie.psi.CjFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.ModificationTracker
import com.intellij.util.Processors
import org.jetbrains.annotations.TestOnly



class CangJieModuleOutOfCodeBlockModificationTracker(private val module: Module) : ModificationTracker {
    private val cangjieOutOfCodeBlockTracker = CangJieCodeBlockModificationListener.getInstance(module.project).cangjieOutOfCodeBlockTracker

    private val updater
        get() = getUpdaterInstance(module.project)

    private val dependencies by lazy {
        // Avoid implicit capturing for this to make CachedValueStabilityChecker happy
        val module = module

        module.cacheByClassInvalidatingOnRootModifications(KeyForCachedDependencies::class.java) {
            val modules = HashSet<Module>()
            val processor = Processors.cancelableCollectProcessor(modules)
            ModuleRootManager.getInstance(module).orderEntries().recursively().forEachModule(processor)
            ModuleDependencyProviderExtension.getInstance(module.project).processAdditionalDependencyModules(module, processor)
            modules
        }
    }

    object KeyForCachedDependencies

    override fun getModificationCount(): Long {
        val currentGlobalCount = cangjieOutOfCodeBlockTracker.modificationCount

        if (updater.hasPerModuleModificationCounts()) {
            val selfCount = updater.getModificationCount(module)
            if (selfCount == currentGlobalCount) return selfCount

            var maxCount = selfCount
            for (dependency in dependencies) {
                val depCount = updater.getModificationCount(dependency)
                if (depCount == currentGlobalCount) return currentGlobalCount
                if (depCount > maxCount) maxCount = depCount
            }
            return maxCount
        }

        return currentGlobalCount
    }

    companion object {
        internal fun getUpdaterInstance(project: Project): Updater = project.service()

        fun incrementModificationCountForAllModules(project: Project) {
            getUpdaterInstance(project).incrementModificationCountForAllModules()
        }

        @TestOnly
        fun getModificationCount(module: Module): Long = getUpdaterInstance(module.project).getModificationCount(module)
    }

    @Service(Service.Level.PROJECT)
    class Updater(private val project: Project): Disposable {
        private val cangjieOfOfCodeBlockTracker
            get() =
                CangJieCodeBlockModificationListener.getInstance(project).cangjieOutOfCodeBlockTracker

        private val perModuleModCount = mutableMapOf<Module, Long>()

        private var lastAffectedModule: Module? = null

        private var lastAffectedModuleModCount = -1L

        // All modifications since that count are known to be single-module modifications reflected in
        // perModuleModCount map
        private var perModuleChangesHighWatermark: Long? = null

        internal fun getModificationCount(module: Module): Long {
            return perModuleModCount[module] ?: perModuleChangesHighWatermark ?: cangjieOfOfCodeBlockTracker.modificationCount
        }

        internal fun incrementModificationCountForAllModules() {
            perModuleModCount.replaceAll { _, count -> count + 1 }
        }

        internal fun hasPerModuleModificationCounts() = perModuleChangesHighWatermark != null

        internal fun onCangJiePhysicalFileOutOfBlockChange(cjFile: CjFile, immediateUpdatesProcess: Boolean) {
            lastAffectedModule = ModuleUtil.findModuleForPsiElement(cjFile)
            lastAffectedModuleModCount = cangjieOfOfCodeBlockTracker.modificationCount

            if (immediateUpdatesProcess) {
                onPsiModificationTrackerUpdate(0)
            }
        }

        internal fun onPsiModificationTrackerUpdate(customIncrement: Int = 0) {
            val newModCount = cangjieOfOfCodeBlockTracker.modificationCount
            val affectedModule = lastAffectedModule
            if (affectedModule != null && newModCount == lastAffectedModuleModCount + customIncrement) {
                if (perModuleChangesHighWatermark == null) {
                    perModuleChangesHighWatermark = lastAffectedModuleModCount
                }
                perModuleModCount[affectedModule] = newModCount
            } else {
                // Some updates were not processed in our code so they probably came from other languages. Invalidate all.
                clean()
            }
        }

        private fun clean() {
            perModuleChangesHighWatermark = null
            lastAffectedModule = null
            perModuleModCount.clear()
        }

        override fun dispose() = clean()
    }
}
