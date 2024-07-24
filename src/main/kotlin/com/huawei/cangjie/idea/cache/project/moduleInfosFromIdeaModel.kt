package com.huawei.cangjie.idea.cache.project

import com.huawei.cangjie.idea.projectStructure.moduleInfo.IdeaModuleInfo
import com.intellij.java.workspace.entities.JavaModuleSettingsEntity
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.libraries.Library
import com.intellij.platform.backend.workspace.WorkspaceModelChangeListener
import com.intellij.platform.backend.workspace.WorkspaceModelTopics
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.VersionedStorageChange
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.messages.MessageBusConnection

/** null-platform means that we should get all modules */
fun getModuleInfosFromIdeaModel(project: Project ): List<IdeaModuleInfo> {
    return runReadAction {
        val ideaModelInfosCache = getIdeaModelInfosCache(project)

            ideaModelInfosCache.allModules()

    }
}
fun getIdeaModelInfosCache(project: Project): IdeaModelInfosCache = project.service()
interface IdeaModelInfosCache {
//    fun forPlatform(platform: TargetPlatform): List<IdeaModuleInfo>

    fun allModules(): List<IdeaModuleInfo>

//    fun getModuleInfosForModule(module: Module): Collection<ModuleSourceInfo>
//    fun getLibraryInfosForLibrary(library: Library): Collection<LibraryInfo>
//    fun getSdkInfoForSdk(sdk: Sdk): SdkInfo?

}
class FineGrainedIdeaModelInfosCache(private val project: Project) : IdeaModelInfosCache, Disposable{

//    private val modulesAndSdk: CachedValue<List<IdeaModuleInfo>>
    abstract inner class AbstractCache<Key : Any, Value : Any>(initializer: (AbstractCache<Key, Value>) -> Unit) :
        SynchronizedFineGrainedEntityCache<Key, Value>(project),
        WorkspaceModelChangeListener {

        @Volatile
        private var initializerRef: ((AbstractCache<Key, Value>) -> Unit)? = initializer
        private val initializerLock = Any()

        init {
            initialize()
        }

        override fun subscribe() {
            val connection = project.messageBus.connect(this)
            connection.subscribe(WorkspaceModelTopics.CHANGED, this)
            subscribe(connection)
        }

        protected open fun subscribe(connection: MessageBusConnection) = Unit

        fun fetchValues(): Collection<Value> {
            if (initializerRef != null) {
                synchronized(initializerLock) {
                    initializerRef?.let { it(this) }
                    initializerRef = null
                }
            }
            return values()
        }

        fun applyIfPossible(action: () -> Unit) {
            if (initializerRef != null) return

            action()
        }

        final override fun changed(event: VersionedStorageChange) {
            applyIfPossible {
                modelChanged(event)
            }
        }

        abstract fun modelChanged(event: VersionedStorageChange)
    }
//    inner class ModuleCache : AbstractCache<Module, List<ModuleSourceInfo>>(
//        initializer = {
//            project.ideaModules().forEach(it::get)
//        }) {
//
//        override fun calculate(key: Module): List<ModuleSourceInfo> = key.sourceModuleInfos
//
//        override fun checkKeyValidity(key: Module) {
//            key.checkValidity()
//        }
//
//        override fun modelChanged(event: VersionedStorageChange) {
//            val storageBefore = event.storageBefore
//            val storageAfter = event.storageAfter
//
//            val moduleChanges = event.getChanges<ModuleEntity>()
//            val sourceRootChanges = event.getChanges<SourceRootEntity>()
//            val moduleSettingChanges = event.getChanges<JavaModuleSettingsEntity>()
//
//            if (moduleChanges.isEmpty() && sourceRootChanges.isEmpty() && moduleSettingChanges.isEmpty()) {
//                return
//            }
//
//            val modulesToRegister = LinkedHashSet<Module>()
//            val modulesToRemove = LinkedHashSet<Module>()
//
//            fun Module.scheduleRegister() = modulesToRegister.add(this)
//            fun Module.scheduleRemove() = modulesToRemove.add(this)
//
//            for (moduleChange in moduleChanges) {
//                moduleChange.oldEntity()?.findModule(storageBefore)?.scheduleRemove()
//                moduleChange.newEntity()?.findModule(storageAfter)?.scheduleRegister()
//            }
//
//            val changedModules = mutableSetOf<Module>()
//            for (sourceRootChange in sourceRootChanges) {
//                sourceRootChange.oldEntity()?.contentRoot?.module?.findModule(storageBefore)?.let {
//                    changedModules.add(it)
//                }
//                sourceRootChange.newEntity()?.contentRoot?.module?.findModule(storageAfter)?.let {
//                    changedModules.add(it)
//                }
//            }
//
//            for (moduleSettingChange in moduleSettingChanges) {
//                moduleSettingChange.oldEntity()?.module?.findModule(storageBefore)?.let {
//                    changedModules.add(it)
//                }
//                moduleSettingChange.newEntity()?.module?.findModule(storageAfter)?.let {
//                    changedModules.add(it)
//                }
//            }
//
//            for (module in changedModules) {
//                if (module in modulesToRemove && module !in modulesToRegister) {
//                    // The module itself is gone. No need in updating it because of source root modification.
//                    // Note that on module deletion, both module and source root deletion events arrive.
//                    continue
//                }
//
//                module.scheduleRemove()
//                module.scheduleRegister()
//            }
//
//            invalidateKeys(modulesToRemove)
//            modulesToRegister.forEach { get(it) }
//
//            incModificationCount()
//        }
//    }
//    init {
//        val cachedValuesManager = CachedValuesManager.getManager(project)
//
//        modulesAndSdk = cachedValuesManager.createCachedValue {
//            val ideaModuleInfos = moduleCache.fetchValues().flatten().also {
//                it.checkValidity { "modulesAndSdk: modules calculation" }
//            } + sdkCache.fetchValues().also {
//                it.checkValidity { "modulesAndSdk: sdks calculation" }
//            }
//            CachedValueProvider.Result.create(ideaModuleInfos, modificationTracker)
//        }
//
//    }
    override fun allModules(): List<IdeaModuleInfo> {
        return emptyList()
//        TODO("Not yet implemented")
    }

    override fun dispose() = Unit


}
