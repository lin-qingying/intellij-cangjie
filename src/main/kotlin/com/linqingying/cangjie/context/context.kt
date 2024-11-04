package com.linqingying.cangjie.context

//import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.storage.ExceptionTracker
import com.linqingying.cangjie.storage.LockBasedStorageManager
import com.linqingying.cangjie.storage.StorageManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project

interface GlobalContext {


    val storageManager: StorageManager
    val exceptionTracker: ExceptionTracker
}
interface ProjectContext : GlobalContext {
        val project: Project
}
fun ProjectContext(project: Project, debugName: String): ProjectContext = ProjectContextImpl(project, GlobalContext(debugName,project))

interface ModuleContext : ProjectContext {
    val module: ModuleDescriptor
}
fun ProjectContext.withModule(module: ModuleDescriptor): ModuleContext = ModuleContextImpl(module, this)
class ModuleContextImpl(
    override val module: ModuleDescriptor,
    projectContext: ProjectContext
) : ModuleContext, ProjectContext by projectContext

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



fun GlobalContext(debugName: String,project: Project): GlobalContextImpl {
    val tracker = ExceptionTracker()
    return GlobalContextImpl(LockBasedStorageManager.createWithExceptionHandling(project,debugName, tracker, {
        ProgressManager.checkCanceled()
    }, { throw ProcessCanceledException(it) }), tracker)
}
fun GlobalContext(debugName: String): GlobalContextImpl {
    val tracker = ExceptionTracker()
    return GlobalContextImpl(LockBasedStorageManager.createWithExceptionHandling(debugName, tracker, {
        ProgressManager.checkCanceled()
    }, { throw ProcessCanceledException(it) }), tracker)
}
