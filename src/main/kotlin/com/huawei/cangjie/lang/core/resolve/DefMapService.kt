package com.huawei.cangjie.lang.core.resolve

import com.huawei.cangjie.lang.core.create.Crate
import com.huawei.cangjie.lang.core.create.CratePersistentId
import com.huawei.cangjie.lang.core.psi.CjFile
import com.huawei.cangjie.lang.core.psi.cangjiePsiManager
import com.huawei.cangjie.openapiext.checkReadAccessAllowed
import com.huawei.cangjie.openapiext.fileId
import com.huawei.cangjie.openapiext.isUnitTestMode
import com.huawei.cangjie.stdext.HashCode
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.util.io.DigestUtil
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong



val RESOLVE_LOG: Logger = Logger.getInstance("com.huawei.cangjie.resolve")
class DefMapHolder(
    val crateId: CratePersistentId,
    private val structureModificationTracker: ModificationTracker,
) {


    @Volatile
    var defMap: CrateDefMap? = null
        private set


    private val defMapStamp: AtomicLong = AtomicLong(-1)

    fun hasLatestStamp(): Boolean = defMapStamp.get() == structureModificationTracker.modificationCount

    private fun setLatestStamp() {
        defMapStamp.set(structureModificationTracker.modificationCount)
    }

    fun checkHasLatestStamp() {
        if (defMap != null && !hasLatestStamp()) {
            RESOLVE_LOG.error(
                "DefMapHolder must have latest stamp right after DefMap($defMap) was updated. " +
                        "$defMapStamp vs ${structureModificationTracker.modificationCount}"
            )
        }
    }

    val modificationCount: Long get() = defMapStamp.get()


    @Volatile
    var shouldRebuild: Boolean = true
        set(value) {
            field = value
            if (value) {
                defMapStamp.decrementAndGet()
                shouldRecheck = false
                changedFiles.clear()
            }
        }


    @Volatile
    var shouldRecheck: Boolean = false
        set(value) {
            field = value
            if (value) {
                defMapStamp.decrementAndGet()
            }
        }

     val changedFiles: MutableSet<CjFile> = hashSetOf()
    fun addChangedFile(file: CjFile) {
        changedFiles += file
        defMapStamp.decrementAndGet()
    }

    fun setDefMap(defMap: CrateDefMap?) {
        this.defMap = defMap
        shouldRebuild = false
        setLatestStamp()
    }



    override fun toString(): String = "DefMapHolder($defMap, stamp=$defMapStamp)"
}





@Service
class DefMapService(val project: Project) : Disposable {


    companion object {
        private val nextNonCargoCrateId: AtomicInteger = AtomicInteger(-1)
        fun getNextNonCargoCrateId(): Int = nextNonCargoCrateId.decrementAndGet()
    }
    private val structureModificationTracker: ModificationTracker =
        project.cangjiePsiManager.cangjieStructureModificationTracker
    override fun dispose() {

    }
    @Volatile
    private var allDefMapsUpdatedStamp: Long = -1
    fun areAllDefMapsUpToDate(): Boolean = allDefMapsUpdatedStamp == structureModificationTracker.modificationCount

}

val Project.defMapService: DefMapService
    get() = service()


