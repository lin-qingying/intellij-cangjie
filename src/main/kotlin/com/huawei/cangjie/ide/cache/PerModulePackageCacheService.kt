package com.huawei.cangjie.ide.cache

import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.ide.cache.PerModulePackageCacheService.Companion.DEBUG_LOG_ENABLE_PerModulePackageCache
import com.huawei.cangjie.ide.indices.CangJiePackageIndexUtils
import com.huawei.cangjie.ide.stubindex.resolve.isUnitTestMode
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.NotNullableUserDataProperty
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.containers.CollectionFactory
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.indexing.DumbModeAccessType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicReference


@Service(Service.Level.PROJECT)

class PerModulePackageCacheService(private val project: Project) : Disposable {
    companion object {
        const val FULL_DROP_THRESHOLD = 1000
        private val LOG = Logger.getInstance(this::class.java)

        fun getInstance(project: Project): PerModulePackageCacheService = project.service()

        var Project.DEBUG_LOG_ENABLE_PerModulePackageCache: Boolean
                by NotNullableUserDataProperty<Project, Boolean>(Key.create("debug.PerModulePackageCache"), false)
    }
    private val pendingVFileChanges: MutableSet<VFileEvent> = mutableSetOf()
    private val pendingCtFileChanges: MutableSet<CjFile> = mutableSetOf()
    private val cacheInstance  =
        AtomicReference<ConcurrentMap<Module, ConcurrentMap<ModuleInfo, ConcurrentMap<FqName, Boolean>>>>()
    private val useStrongMapForCaching = Registry.`is`("cangjie.cache.packages.strong.map", false)

    override fun dispose() {
        clear()

    }

    private fun cache(): ConcurrentMap<Module, ConcurrentMap<ModuleInfo, ConcurrentMap<FqName, Boolean>>> {
        cacheInstance.get()?.let { return it }
        val map =
            ContainerUtil.createConcurrentWeakMap<Module, ConcurrentMap<ModuleInfo, ConcurrentMap<FqName, Boolean>>>()
        return if (cacheInstance.compareAndSet(null, map)) {
            map
        } else {
            cacheInstance.get()!!
        }
    }

    fun packageExists(packageFqName: FqName, moduleInfo: ModuleInfo): Boolean {


        val module = moduleInfo.module
//        checkPendingChanges()
//
        val perSourceInfoCache = cache().getOrPut(module) {
            if (useStrongMapForCaching) ConcurrentHashMap() else CollectionFactory.createConcurrentSoftMap()
        }
        val cacheForCurrentModuleInfo = perSourceInfoCache.getOrPut(moduleInfo) {
            if (useStrongMapForCaching) ConcurrentHashMap() else CollectionFactory.createConcurrentSoftMap()
        }
////
        return try {
            cacheForCurrentModuleInfo.getOrPut(packageFqName) {
                val packageExists = CangJiePackageIndexUtils.packageExists(packageFqName, moduleInfo.contentScope)
                LOG.debugIfEnabled(project) { "Computed cache value for $packageFqName in $moduleInfo is $packageExists" }
                packageExists
            }
        } catch (e: IndexNotReadyException) {
            DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode(ThrowableComputable {
                CangJiePackageIndexUtils.packageExists(packageFqName, moduleInfo.contentScope)
            })
        }
    }

    private fun clear() {
        synchronized(this) {
            pendingVFileChanges.clear()
            pendingCtFileChanges.clear()
//            cacheInstance.set(null)
//            implicitPackagePrefixCache.clear()
        }
    }

}
private fun Logger.debugIfEnabled(project: Project, withCurrentTrace: Boolean = false, message: () -> String) {
    if (isUnitTestMode() && project.DEBUG_LOG_ENABLE_PerModulePackageCache) {
        val msg = message()
        if (withCurrentTrace) {
            val e = Exception().apply { fillInStackTrace() }
            this.debug(msg, e)
        } else {
            this.debug(msg)
        }
    }
}
