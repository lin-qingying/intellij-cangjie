package com.huawei.cangjie.context

import com.huawei.cangjie.storage.ExceptionTracker
import com.huawei.cangjie.storage.LockBasedStorageManager
import com.huawei.cangjie.storage.StorageManager
import com.intellij.openapi.project.Project

interface GlobalContext {
    val storageManager: StorageManager
    val exceptionTracker: ExceptionTracker
}
interface ProjectContext : GlobalContext {
    val project: Project
}
class ProjectContextImpl(
    override val project: Project,
    private val globalContext: GlobalContext
) : ProjectContext, GlobalContext by globalContext

open class GlobalContextImpl(
    storageManager: LockBasedStorageManager,
    exceptionTracker: ExceptionTracker
) : SimpleGlobalContext(storageManager, exceptionTracker) {
    override val storageManager: LockBasedStorageManager = super.storageManager as LockBasedStorageManager
}

fun GlobalContext.withProject(project: Project): ProjectContext = ProjectContextImpl(project, this)


open class SimpleGlobalContext(
    override val storageManager: StorageManager,
    override val exceptionTracker: ExceptionTracker
) : GlobalContext
