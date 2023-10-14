package com.huawei.cangjie.lang.core.psi

import com.huawei.cangjie.lang.CjFileType
import com.huawei.cangjie.lang.core.psi.CjPsiManager.Companion.isIgnorePsiEvents
import com.intellij.ProjectTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.*
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.messages.Topic
import com.huawei.cangjie.lang.core.psi.CjPsiTreeChangeEvent.*
import com.huawei.cangjie.lang.core.psi.ext.findModificationTrackerOwner
import com.intellij.openapi.project.DumbService


/**不直接订阅，也不通过plugin.xml惰性监听器订阅。使用[CjPsiManager.subscribeCangJieStructureChange]*/
private val CANGJIE_STRUCTURE_CHANGE_TOPIC: Topic<CangJieStructureChangeListener> = Topic.create(
    "CANGJIE_STRUCTURE_CHANGE_TOPIC",
    CangJieStructureChangeListener::class.java,
    Topic.BroadcastDirection.TO_PARENT
)

/**不直接订阅，也不通过plugin.xml惰性监听器订阅。使用[CjPsiManager.subscribeCangJieStructureChange]*/
private val CANGJIE_PSI_CHANGE_TOPIC: Topic<CangJiePsiChangeListener> = Topic.create(
    "CANGJIE_PSI_CHANGE_TOPIC",
    CangJiePsiChangeListener::class.java,
    Topic.BroadcastDirection.TO_PARENT
)

interface CjPsiManager {
    /**
     *项目-全局修改跟踪器，在可能影响的每个PSI更改上递增。
     *名称解析或类型推理。它将随着大多数类型的更改而递增。
     *不包括函数体的PSI元素(表达式和语句)。
     */
    val cangjieStructureModificationTracker: ModificationTracker


    val cangjieStructureModificationTrackerInDependencies: SimpleModificationTracker

    fun incCangJieStructureModificationCount()

    /**这是一个实例方法，因为[CjPsiManager]应该在事件订阅之前创建*/
    fun subscribeCangJieStructureChange(connection: MessageBusConnection, listener: CangJieStructureChangeListener) {
        connection.subscribe(CANGJIE_STRUCTURE_CHANGE_TOPIC, listener)
    }

    /**这是一个实例方法，因为[CjPsiManager]应该在事件订阅之前创建*/
    fun subscribeCangJiePsiChange(connection: MessageBusConnection, listener: CangJiePsiChangeListener) {
        connection.subscribe(CANGJIE_PSI_CHANGE_TOPIC, listener)
    }

    companion object {
        private val IGNORE_PSI_EVENTS: Key<Boolean> = Key.create("IGNORE_PSI_EVENTS")

        fun <T> withIgnoredPsiEvents(psi: PsiFile, f: () -> T): T {
            setIgnorePsiEvents(psi, true)
            try {
                return f()
            } finally {
                setIgnorePsiEvents(psi, false)
            }
        }


        fun isIgnorePsiEvents(psi: PsiFile): Boolean =
            psi.getUserData(IGNORE_PSI_EVENTS) == true

        private fun setIgnorePsiEvents(psi: PsiFile, ignore: Boolean) {
            psi.putUserData(IGNORE_PSI_EVENTS, if (ignore) true else null)
        }
    }
}



class CjPsiManagerImpl(val project: Project) : CjPsiManager, Disposable {

    override val cangjieStructureModificationTracker = SimpleModificationTracker()
    override val cangjieStructureModificationTrackerInDependencies = SimpleModificationTracker()
    override fun incCangJieStructureModificationCount() =
        incCangJieStructureModificationCount(null, null)


    private fun incCangJieStructureModificationCount(file: PsiFile? = null, psi: PsiElement? = null) {
        cangjieStructureModificationTracker.incModificationCount()
        if (!isWorkspaceFile(file)) {
            cangjieStructureModificationTrackerInDependencies.incModificationCount()
        }
        project.messageBus.syncPublisher(CANGJIE_STRUCTURE_CHANGE_TOPIC).cangjieStructureChanged(file, psi)
    }
    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(CacheInvalidator(), this)
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                incCangJieStructureModificationCount()
            }
        })
//        project.messageBus.connect().subscribe(CjpmProjectsService.CARGO_PROJECTS_TOPIC, CjpmProjectsListener { _, _ ->
//            incCangJieStructureModificationCount()
//        })
    }
    private fun isWorkspaceFile(file: PsiFile?): Boolean {
        if (file !is CjFile) return false
//        val virtualFile = file.virtualFile ?: return false
//        val crates =
//            if (virtualFile.fileSystem is MacroExpansionFileSystem) {
//            val crateId = project.macroExpansionManagerIfCreated?.getCrateForExpansionFile(virtualFile) ?: return false
//            listOf(crateId)
//        } else {
//            project.defMapService.findCrates(file)
//        }
//        if (crates.isEmpty()) return false
//        val crateGraph =  project.crateGraph
//        if (crates.any { crateGraph.findCrateById(it)?.origin != PackageOrigin.WORKSPACE }) return false

        return true
    }



    override fun dispose() {

    }


    inner class CacheInvalidator : CjPsiTreeChangeAdapter() {
        override fun handleEvent(event: CjPsiTreeChangeEvent) {
            val element = when (event) {
                is ChildRemoval.Before -> event.child
                is ChildRemoval.After -> event.parent
                is ChildReplacement.Before -> event.oldChild
                is ChildReplacement.After -> event.newChild
                is ChildAddition.After -> event.child
                is ChildMovement.After -> event.child
                is ChildrenChange.After -> if (!event.isGenericChange) event.parent else return
                is PropertyChange.After -> {
                    when (event.propertyName) {
                        PsiTreeChangeEvent.PROP_UNLOADED_PSI, PsiTreeChangeEvent.PROP_FILE_TYPES -> {
                            incCangJieStructureModificationCount()
                            return
                        }
                        PsiTreeChangeEvent.PROP_WRITABLE -> return
                        else -> event.element ?: return
                    }
                }
                else -> return
            }

            val file = event.file

            // if file is null, this is an event about VFS changes
            if (file == null) {
//                val isStructureModification = element is CjFile && !isIgnorePsiEvents(element)
//                        || element is PsiDirectory && project.cargoProjects.findPackageForFile(element.virtualFile) != null
//                if (isStructureModification) {
//                    incCangJieStructureModificationCount(element as? CjFile, element as? CjFile)
//                }
            } else {
                if (file.fileType != CjFileType) return
                if (isIgnorePsiEvents(file)) return

                val isWhitespaceOrComment = element is PsiComment || element is PsiWhiteSpace
//            if (isWhitespaceOrComment && !isMacroExpansionModeNew) {
//                // Whitespace/comment changes are meaningful if new macro expansion engine is used
//                return
//            }

                // Most of events means that some element *itself* is changed, but ChildrenChange means
                // that changed some of element's children, not the element itself. In this case
                // we should look up for ModificationTrackerOwner a bit differently
                val isChildrenChange = event is ChildrenChange || event is ChildRemoval.After

                updateModificationCount(file, element, isChildrenChange, isWhitespaceOrComment)
            }
        }

    }


    private fun updateModificationCount(
        file: PsiFile,
        psi: PsiElement,
        isChildrenChange: Boolean,
        isWhitespaceOrComment: Boolean
    ) {

        val owner = if (DumbService.isDumb(project)) null else psi.findModificationTrackerOwner(!isChildrenChange)


        val isStructureModification = owner == null || !owner.incModificationCount(psi)

//        if (!isStructureModification && owner is CjMacroCall &&
//            (!isMacroExpansionModeNew || !owner.isTopLevelExpansion)) {
//            return updateModificationCount(file, owner, isChildrenChange = false, isWhitespaceOrComment = false)
//        }

        if (isStructureModification) {
            incCangJieStructureModificationCount(file, psi)
        }
        project.messageBus.syncPublisher(CANGJIE_PSI_CHANGE_TOPIC).cangjiePsiChanged(file, psi, isStructureModification)
    }
}


interface CangJieStructureChangeListener {
    fun cangjieStructureChanged(file: PsiFile?, changedElement: PsiElement?)
}

interface CangJiePsiChangeListener {
    fun cangjiePsiChanged(file: PsiFile, element: PsiElement, isStructureModification: Boolean)
}

val Project.cangjiePsiManager: CjPsiManager get() = service()
val Project.cangjieStructureModificationTracker: ModificationTracker
    get() = cangjiePsiManager.cangjieStructureModificationTracker
