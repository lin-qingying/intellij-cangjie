package com.huawei.cangjie.ide.formatter.cjfmt.action

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.ide.formatter.cjfmt.*
import com.huawei.cangjie.ide.run.cjpm.runconfig.ExecuteResult
import com.intellij.codeInsight.actions.ReformatCodeAction
import com.intellij.lang.LanguageFormatting
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import java.util.*

class CangJieFormatCodeAction : ReformatCodeAction() {

    private var shouldExecuteFormatAction = false
    private var shouldExecuteOldReformatAction = false
    private var hasSelectionIncludedDir = false

    companion object {

        val LOG = Logger.getInstance(CangJieFormatCodeAction::class.java)
        fun containsOnlyFiles(files: Array<VirtualFile>): Boolean {


            if (files.isEmpty()) {
                return false
            } else {


                for (element in files) {
                    if (element.isDirectory) {
                        return false
                    }
                }

                return true
            }
        }
    }

    init {
        this.isEnabledInModalContext = true
        templatePresentation.text = CangJieBundle.message("title.cjfmt")
        templatePresentation.setDescription(CangJieBundle.message("title.cjfmt"))
    }

    override fun actionPerformed(event: AnActionEvent) {
        if (this.shouldExecuteFormatAction) {
            this.executeFormat(event)
        }

        if (this.shouldExecuteOldReformatAction) {
            super.actionPerformed(event)
        }
    }

    private fun isOldReformatActionAvailable(event: AnActionEvent): Boolean {


        val dataContext = event.dataContext
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        if (project == null) {
            return false
        } else {
            val editor = CommonDataKeys.EDITOR.getData(dataContext)
            val virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
            val files: Queue<VirtualFile> = ArrayDeque()



            for (index in 0 until virtualFiles!!.size) {
                val virtualFile = virtualFiles[index]
                val isSuccess = files.offer(virtualFile)
                if (!isSuccess) {
                    LOG.warn("add virtualFile failed")
                }
            }

            if (editor != null) {
                val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
                if (file == null || file.virtualFile == null) {
                    return false
                }

                if (LanguageFormatting.INSTANCE.forContext(file) != null) {
                    return true
                }
            } else if (virtualFiles.size != 1 && !containsOnlyFiles(
                    virtualFiles
                )
            ) {
                if (LangDataKeys.MODULE_CONTEXT.getData(dataContext) == null && PlatformDataKeys.PROJECT_CONTEXT.getData(
                        dataContext
                    ) == null
                ) {
                    val element = CommonDataKeys.PSI_ELEMENT.getData(dataContext)
                        ?: return false

                    if (element !is PsiDirectory) {
                        val file = element.containingFile
                        return file != null && LanguageFormatting.INSTANCE.forContext(file) != null
                    }
                }
            } else if (!this.isSelectedValidForOldAction(project, files)) {
                return false
            }

            return true
        }
    }

    private fun isSelectedValidForOldAction(project: Project, files: Queue<VirtualFile>): Boolean {
        while (true) {
            if (!files.isEmpty()) {
                val virtualFile = files.poll() as VirtualFile
                if (virtualFile.isDirectory) {
                    if (virtualFile.children != null && virtualFile.children.isNotEmpty()) {
                        this.offerVirtualFile(files, virtualFile)
                    }
                    continue
                }

                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return false

                LanguageFormatting.INSTANCE.forContext(psiFile) ?: continue

                return true
            }

            return false
        }
    }

    private fun offerVirtualFile(files: Queue<VirtualFile>, file: VirtualFile) {
        val children = file.children
        val size = children.size

        for (index in 0 until size) {
            val virtualFile = children[index]
            val isSuccess = files.offer(virtualFile)
            if (!isSuccess) {
                LOG.warn("add child file failed")
            }
        }
    }

    private fun isSelectedValid(virtualFiles: Array<VirtualFile>): Boolean {
        val files: Queue<VirtualFile> = ArrayDeque()
        this.hasSelectionIncludedDir = false
        var hasSelectionIncludedFiles = false
        var dirCount = 0


        for (index in virtualFiles.indices) {
            val virtualFile = virtualFiles[index]
            val isSuccess = files.offer(virtualFile)
            if (!isSuccess) {
                LOG.warn("add virtualFile failed")
            }

            if (virtualFile.isDirectory) {
                this.hasSelectionIncludedDir = true
                ++dirCount
            } else {
                hasSelectionIncludedFiles = true
            }
        }

        if ((!this.hasSelectionIncludedDir || !hasSelectionIncludedFiles) && dirCount <= 1) {
            while (!files.isEmpty()) {
                val file = files.poll()
                if (file != null) {
                    if (file.isDirectory) {
                        if (file.children != null && file.children.isNotEmpty()) {
                            this.offerVirtualFile(files, file)
                        }
                    } else if (checkFile(file)) {
                        return true
                    }
                }
            }

            return false
        } else {
            return false
        }
    }

    private fun updateOldFormatAction(event: AnActionEvent) {


        this.shouldExecuteOldReformatAction = this.isOldReformatActionAvailable(event)
        val presentation = event.presentation
        presentation.isEnabledAndVisible = shouldExecuteOldReformatAction
    }

    override fun update(event: AnActionEvent) {
        val presentation = event.presentation
        val project = event.project
        if (this.isProjectAbnormal(project)) {
            presentation.isEnabled = false
            this.shouldExecuteFormatAction = false
        } else {
            val virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext) as Array<VirtualFile>?
            if (!virtualFiles.isNullOrEmpty()) {
                if (!this.isSelectedValid(virtualFiles)) {
                    presentation.isEnabledAndVisible = false
                    this.shouldExecuteFormatAction = false
                    this.updateOldFormatAction(event)
                } else {
                    this.shouldExecuteFormatAction = true
                    if ((!this.hasSelectionIncludedDir || virtualFiles.size != 1) && virtualFiles.size <= 1) {
                        this.shouldExecuteOldReformatAction = false
                    } else {
                        this.shouldExecuteOldReformatAction = this.isOldReformatActionAvailable(event)
                        event.place
                    }
                }
            } else {
                presentation.isEnabledAndVisible = false
                this.shouldExecuteFormatAction = false
            }
        }
    }

    private fun reformatFilesCode(
        files: Array<VirtualFile>,
        project: Project,
        reformatCodeService: CangJieFormatService
    ) {


        for (element in files) {
            if (checkFile(element) || element.isDirectory) {
                val resultOptional: Optional<ExecuteResult> = reformatCodeService.reformatCode(element, project)
                if (resultOptional.isEmpty) {
                    notifyResult(false, project)
                    break
                }

                if (resultOptional.get().exitCode !== 0) {
                    showWithProject(
                        resultOptional.get().executeOut,
                        "Reformat Code",
                        Type.INFO,
                        project
                    )
                    break
                }
            }
        }
    }

    private fun isProjectAbnormal(project: Project?): Boolean {
        var isAbnormal = project == null
        isAbnormal = isAbnormal || project!!.isDefault
        isAbnormal = isAbnormal || !project!!.isInitialized
        isAbnormal = isAbnormal || project!!.isDisposed
        isAbnormal = isAbnormal || !project!!.isOpen
        return isAbnormal
    }

    private fun executeFormat(event: AnActionEvent) {
        val dataContext = event.dataContext
        val project = event.project ?: return

        if (!isProjectAbnormal(project)) {


            val editor = CommonDataKeys.EDITOR.getData(dataContext)
            var file: PsiFile? = null
            var hasSelection = false
            FileDocumentManager.getInstance().saveAllDocuments()
            val files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext) ?: return

            if (files.isNotEmpty()) {
                val reformatCodeService = CangJieFormatService.getInstance(project)
                val actionManager = ActionManager.getInstance()
                val dir: PsiDirectory?

                if (editor != null) {
                    file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
                    if (file == null) return

                    dir = file.containingDirectory
                    hasSelection = editor.selectionModel.hasSelection()
                } else {
                    if (containsOnlyFiles(files)) {
                        reformatFilesCode(files, project, reformatCodeService)
                        actionManager.getAction("SynchronizeCurrentFile").actionPerformed(event)
                        return
                    }

                    if (PlatformCoreDataKeys.PROJECT_CONTEXT.getData(dataContext) != null ||
                        LangDataKeys.MODULE_CONTEXT.getData(dataContext) != null
                    ) {
                        reformatFilesCode(files, project, reformatCodeService)
                        actionManager.getAction("SynchronizeCurrentFile").actionPerformed(event)
                        return
                    }

                    val element = CommonDataKeys.PSI_ELEMENT.getData(dataContext) ?: return

                    dir = when (element) {
                        is PsiDirectoryContainer -> element.directories.firstOrNull()
                        is PsiDirectory -> element
                        else -> {
                            file = element.containingFile ?: return
                            file.containingDirectory
                        }
                    }
                }

                if (file == null && dir != null) {
                    executeReformatDirectoryCode(dir, project, actionManager, reformatCodeService, event)
                } else if (file != null && editor != null) {
                    PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                    reformatCodeService.reformatFileCode(file, hasSelection, project, editor)
                    actionManager.getAction("SynchronizeCurrentFile").actionPerformed(event)
                }
            }
        }
    }

    private fun executeReformatDirectoryCode(
        dir: PsiDirectory,
        project: Project,
        actionManager: ActionManager,
        reformatCodeService: CangJieFormatService,
        event: AnActionEvent
    ) {

        val resultOptional: Optional<ExecuteResult> =
            reformatCodeService.reformatDirectoryCode(dir.virtualFile, project, "-d")
        if (resultOptional.isEmpty) {
            notifyResult(false, project)
        } else {
            if (resultOptional.get().exitCode !== 0) {
                showWithProject(
                    resultOptional.get().executeOut,
                    "Reformat Code",
                    Type.INFO,
                    project
                )
            }

            actionManager.getAction("SynchronizeCurrentFile").actionPerformed(event)
        }
    }
}
