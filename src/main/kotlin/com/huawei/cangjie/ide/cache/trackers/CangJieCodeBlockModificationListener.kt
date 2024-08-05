package com.huawei.cangjie.ide.cache.trackers

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor


/**
 * Tested in [OutOfBlockModificationTestGenerated]
 */
@Service(Service.Level.PROJECT)
class CangJieCodeBlockModificationListener(project: Project) : PsiTreeChangePreprocessor, Disposable {

    private val cangjieOutOfCodeBlockTrackerImpl = SimpleModificationTracker()

    val cangjieOutOfCodeBlockTracker: ModificationTracker = cangjieOutOfCodeBlockTrackerImpl

    companion object {
        fun getInstance(project: Project): CangJieCodeBlockModificationListener = project.service()
    }

    override fun treeChanged(event: PsiTreeChangeEventImpl) {
        if (!PsiModificationTrackerImpl.canAffectPsi(event)) {
            return
        }

        // Copy logic from PsiModificationTrackerImpl.treeChanged(). Some out-of-code-block events are written to language modification
        // tracker in PsiModificationTrackerImpl but don't have correspondent PomModelEvent. Increase kotlinOutOfCodeBlockTracker
        // manually if needed.
        val outOfCodeBlock = when (event.code) {
            PsiTreeChangeEventImpl.PsiEventType.PROPERTY_CHANGED ->
                event.propertyName === PsiTreeChangeEvent.PROP_UNLOADED_PSI || event.propertyName === PsiTreeChangeEvent.PROP_ROOTS

            PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED -> event.oldParent is PsiDirectory || event.newParent is PsiDirectory
            else -> event.parent is PsiDirectory
        }

        if (outOfCodeBlock) {
            incModificationCount()
        }
    }

    fun incModificationCount() {
        cangjieOutOfCodeBlockTrackerImpl.incModificationCount()
    }

    override fun dispose() = Unit
}
