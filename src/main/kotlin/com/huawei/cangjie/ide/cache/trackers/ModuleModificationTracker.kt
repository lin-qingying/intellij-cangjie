package com.huawei.cangjie.ide.cache.trackers

import com.huawei.cangjie.ide.cache.project.getChanges
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.platform.backend.workspace.WorkspaceModelChangeListener
import com.intellij.platform.backend.workspace.WorkspaceModelTopics
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.VersionedStorageChange

@Service(Service.Level.PROJECT)
class ModuleModificationTracker(project: Project) :
    SimpleModificationTracker(), WorkspaceModelChangeListener, Disposable {

    init {
        project.messageBus.connect(this).subscribe(WorkspaceModelTopics.CHANGED, this)
    }

    override fun changed(event: VersionedStorageChange) {
        event.getChanges<ModuleEntity>().ifEmpty { return }
        incModificationCount()
    }

    override fun dispose() = Unit

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ModuleModificationTracker = project.service()
    }
}
