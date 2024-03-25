package com.huawei.cangjie.analyzer

import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker

open class CangJieModificationTrackerService {
    open val modificationTracker: ModificationTracker = ModificationTracker.NEVER_CHANGED
    open val outOfBlockModificationTracker: ModificationTracker = ModificationTracker.NEVER_CHANGED
    open fun fileModificationTracker(file: CjFile): ModificationTracker = ModificationTracker.NEVER_CHANGED

    companion object {
        private val NEVER_CHANGE_TRACKER_SERVICE = CangJieModificationTrackerService()

        @JvmStatic
        fun getInstance(project: Project): CangJieModificationTrackerService {
            return project.getService(CangJieModificationTrackerService::class.java) ?: NEVER_CHANGE_TRACKER_SERVICE
        }
    }
}