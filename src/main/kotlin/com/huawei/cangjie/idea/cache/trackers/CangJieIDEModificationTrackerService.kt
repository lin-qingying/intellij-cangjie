package com.huawei.cangjie.idea.cache.trackers

import com.huawei.cangjie.analyzer.CangJieModificationTrackerService
import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.annotations.TestOnly

private val PER_FILE_MODIFICATION_TRACKER = Key<SimpleModificationTracker>("FILE_OUT_OF_BLOCK_MODIFICATION_COUNT")

val CjFile.perFileModificationTracker: ModificationTracker
    get() = putUserDataIfAbsent(PER_FILE_MODIFICATION_TRACKER, SimpleModificationTracker())

class CangJieIDEModificationTrackerService(project: Project) : CangJieModificationTrackerService() {
    override val modificationTracker: ModificationTracker = PsiModificationTracker.getInstance(project)

    override val outOfBlockModificationTracker: ModificationTracker =
        CangJieCodeBlockModificationListener.getInstance(project).cangjieOutOfCodeBlockTracker

    override fun fileModificationTracker(file: CjFile): ModificationTracker =
        file.perFileModificationTracker

    companion object {
        @TestOnly
        fun invalidateCaches(project: Project) {
            project.getService(CangJieModificationTrackerService::class.java).apply {
                (outOfBlockModificationTracker as SimpleModificationTracker).incModificationCount()

                @Suppress("DEPRECATION")
                (modificationTracker as PsiModificationTrackerImpl).incCounter()
            }
        }
    }
}