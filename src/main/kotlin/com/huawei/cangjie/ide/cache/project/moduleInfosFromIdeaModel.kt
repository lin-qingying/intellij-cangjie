package com.huawei.cangjie.ide.cache.project


import com.huawei.cangjie.analyzer.CangJieModuleInfo
import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.ide.cache.trackers.CangJieCodeBlockModificationListener
import com.huawei.cangjie.utils.CangJieExceptionWithAttachments
import com.intellij.java.workspace.entities.JavaModuleSettingsEntity
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.platform.backend.workspace.WorkspaceModelChangeListener
import com.intellij.platform.backend.workspace.WorkspaceModelTopics
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.EntityChange
import com.intellij.platform.workspace.storage.VersionedStorageChange
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.serviceContainer.AlreadyDisposedException
import com.intellij.util.messages.MessageBusConnection
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModule
import org.jetbrains.jps.model.java.JavaSourceRootType

/** null-platform means that we should get all modules */
fun getModuleInfosFromIdeaModel(project: Project ): List<ModuleInfo> {
    return runReadAction {
        val ideaModelInfosCache = getIdeaModelInfosCache(project)

            ideaModelInfosCache.allModules()

    }
}
fun getIdeaModelInfosCache(project: Project): ModelInfosCache = project.service()
interface ModelInfosCache {
//    fun forPlatform(platform: TargetPlatform): List<IdeaModuleInfo>

    fun allModules(): List<ModuleInfo>

//    fun getModuleInfosForModule(module: Module): Collection<ModuleSourceInfo>
//    fun getLibraryInfosForLibrary(library: Library): Collection<LibraryInfo>
//    fun getSdkInfoForSdk(sdk: Sdk): SdkInfo?

}
class FineGrainedIdeaModelInfosCache(private val project: Project) : ModelInfosCache, Disposable{

    private val modules: CachedValue<List<ModuleInfo>>
    private val moduleCache = ModuleCache()
    private val modificationTracker = SimpleModificationTracker()
//    private val libraries: CachedValue<Collection<LibraryInfo>>

    init {
        val cachedValuesManager = CachedValuesManager.getManager(project)

        modules = cachedValuesManager.createCachedValue {
            val ideaModuleInfos = moduleCache.fetchValues().flatten().also {
                it.checkValidity { "modulesAndSdk: modules calculation" }
            }
            CachedValueProvider.Result.create(ideaModuleInfos, modificationTracker)
        }

    }
    inner class ModuleCache : AbstractCache<Module, List<ModuleInfo>>(
        initializer = {
            project.modules().forEach(it::get)
        }) {

        override fun calculate(key: Module): List<ModuleInfo> = key.moduleInfos

        override fun checkKeyValidity(key: Module) {
                key.checkValidity()
        }

        override fun modelChanged(event: VersionedStorageChange) {
            val storageBefore = event.storageBefore
            val storageAfter = event.storageAfter

            val moduleChanges = event.getChanges<ModuleEntity>()
            val sourceRootChanges = event.getChanges<SourceRootEntity>()
            val moduleSettingChanges = event.getChanges<JavaModuleSettingsEntity>()

            if (moduleChanges.isEmpty() && sourceRootChanges.isEmpty() && moduleSettingChanges.isEmpty()) {
                return
            }

            val modulesToRegister = LinkedHashSet<Module>()
            val modulesToRemove = LinkedHashSet<Module>()

            fun Module.scheduleRegister() = modulesToRegister.add(this)
            fun Module.scheduleRemove() = modulesToRemove.add(this)

            for (moduleChange in moduleChanges) {
                moduleChange.oldEntity()?.findModule(storageBefore)?.scheduleRemove()
                moduleChange.newEntity()?.findModule(storageAfter)?.scheduleRegister()
            }

            val changedModules = mutableSetOf<Module>()
            for (sourceRootChange in sourceRootChanges) {
                sourceRootChange.oldEntity()?.contentRoot?.module?.findModule(storageBefore)?.let {
                    changedModules.add(it)
                }
                sourceRootChange.newEntity()?.contentRoot?.module?.findModule(storageAfter)?.let {
                    changedModules.add(it)
                }
            }

            for (moduleSettingChange in moduleSettingChanges) {
                moduleSettingChange.oldEntity()?.module?.findModule(storageBefore)?.let {
                    changedModules.add(it)
                }
                moduleSettingChange.newEntity()?.module?.findModule(storageAfter)?.let {
                    changedModules.add(it)
                }
            }

            for (module in changedModules) {
                if (module in modulesToRemove && module !in modulesToRegister) {
                    // The module itself is gone. No need in updating it because of source root modification.
                    // Note that on module deletion, both module and source root deletion events arrive.
                    continue
                }

                module.scheduleRemove()
                module.scheduleRegister()
            }

            invalidateKeys(modulesToRemove)
            modulesToRegister.forEach { get(it) }

            incModificationCount()
        }

    }
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

    override fun allModules(): List<ModuleInfo>   =  (modules.value /*+ libraries.value*/).also {
        it.checkValidity { "allModules" }
    }
    private fun incModificationCount() {
        modificationTracker.incModificationCount()
      CangJieCodeBlockModificationListener.getInstance(project).incModificationCount()
    }
    override fun dispose() = Unit


}

fun Collection<ModuleInfo>.checkValidity(lazyMessage: () -> String) {
    val disposed = filter {
        when (it) {
            is CangJieModuleInfo -> it.module.isDisposed
//            is LibraryInfo -> it.library.isDisposed
            else -> false
        }
    }
    if (disposed.isNotEmpty()) {
        throw CangJieExceptionWithAttachments(lazyMessage())
            .withAttachment("disposedInfos.txt", disposed.joinToString("\n") { it.name.asString() })
    }
}

fun Project.modules(): Array<out Module> = runReadAction { ModuleManager.getInstance(this).modules }
fun Module.checkValidity() {
    if (isDisposed) {
        throw AlreadyDisposedException("Module '${name}' is already disposed")
    }
}
val Module.moduleInfos: List<ModuleInfo>
    get() = listOfNotNull( cangjieModuleInfo)
val Module.cangjieModuleInfo: CangJieModuleInfo?
    get() {
//        val hasProductionRoots = hasRootsOfType(setOf(JavaSourceRootType.SOURCE, SourceKotlinRootType))
//                || (isNewMultiPlatformModule && cangjieSourceRootType == SourceKotlinRootType)
//
//        return if (hasProductionRoots) CangJieModuleInfo(this) else null
        return CangJieModuleInfo(this)
    }
inline fun <reified T : WorkspaceEntity> VersionedStorageChange.getChanges(): List<EntityChange<T>> =
    getChanges(T::class.java)
