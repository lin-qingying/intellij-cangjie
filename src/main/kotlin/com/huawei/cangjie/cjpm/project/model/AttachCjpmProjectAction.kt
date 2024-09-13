package com.huawei.cangjie.cjpm.project.model

import com.google.common.annotations.VisibleForTesting
import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.CjpmConstants
import com.huawei.cangjie.cjpm.findChild
import com.huawei.cangjie.cjpm.project.pathAsPath
import com.huawei.cangjie.cjpm.project.toolwindow.CjpmToolWindow
import com.huawei.cangjie.ide.notifications.CjEditorNotificationPanel
import com.huawei.cangjie.ide.notifications.isCjpmManifestFile
import com.huawei.cangjie.ide.run.cjpm.isUnitTestMode
import com.huawei.cangjie.ide.run.cjpm.runconfig.buildtool.saveAllDocuments
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

abstract class CjpmProjectActionBase : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

/**
 * Adds cjpm project to [CjpmProjectsService]
 *
 * It can be invoked from Project View, [CjpmToolWindow] and [CjEditorNotificationPanel]
 */
class AttachCjpmProjectAction : CjpmProjectActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        saveAllDocuments()

        val file = when (e.place) {
            CjpmToolWindow.CJPM_TOOLBAR_PLACE -> chooseFile(project, e)
            CjEditorNotificationPanel.NOTIFICATION_PANEL_PLACE -> {
                val file = e.getData(PlatformDataKeys.VIRTUAL_FILE)
                if (file?.isCjpmManifestFile == true) file else chooseFile(project, e)
            }
            else -> e.getData(PlatformDataKeys.VIRTUAL_FILE)
        } ?: return

        val cjpmJson = file.findCjpmToml() ?: return

        if (!project.cjpmProjects.attachCjpmProject(cjpmJson.pathAsPath)) {
            Messages.showErrorDialog(
                project,
                CangJieBundle.message("dialog.message.this.cjpm.package.already.part.attached.workspace"),
                CangJieBundle.message("dialog.title.unable.to.attach.cjpm.project")
            )
        }
    }

    private fun chooseFile(project: Project, event: AnActionEvent): VirtualFile? {
        return if (isUnitTestMode) {
            event.getData(MOCK_CHOSEN_FILE_KEY)
        } else {
            val chooser = FileChooserFactory.getInstance().createFileChooser(CjpmProjectChooserDescriptor, project, null)
            return chooser.choose(project).singleOrNull()
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        e.presentation.isEnabledAndVisible = isActionEnabled(e, project)
    }

    private fun isActionEnabled(e: AnActionEvent, project: Project): Boolean {
        return when (e.place) {
            CjpmToolWindow.CJPM_TOOLBAR_PLACE, CjEditorNotificationPanel.NOTIFICATION_PANEL_PLACE -> true
            else -> {
                // We need to use `ProjectFileIndex` to check if `Cjpm.toml` is in project content
                // so disable the action in dumb mode
                if (DumbService.isDumb(project)) return false
                val file = e.getData(PlatformDataKeys.VIRTUAL_FILE)
                val cjpmToml = file?.findCjpmToml() ?: return false

                canBeAttached(project, cjpmToml)
            }
        }
    }

    private fun VirtualFile.findCjpmToml(): VirtualFile? {
        return if (isDirectory) findChild(CjpmConstants.MANIFEST_FILE) else takeIf { it.isCjpmManifestFile }
    }

    companion object {
        @VisibleForTesting
        val MOCK_CHOSEN_FILE_KEY: DataKey<VirtualFile> = DataKey.create("MOCK_CHOSEN_FILE_KEY")

        fun canBeAttached(project: Project, cjpmToml: VirtualFile): Boolean {
            require(cjpmToml.isCjpmManifestFile)
            if (!ProjectFileIndex.getInstance(project).isInContent(cjpmToml)) return false

            val path = cjpmToml.pathAsPath


            return !project.cjpmProjects.allProjects.any { it.manifest == path }
        }


    }
}

object CjpmProjectChooserDescriptor : FileChooserDescriptor(true, true, false, false, false, false) {

    init {
        // The filter is not used for directories
        withFileFilter { it.isCjpmManifestFile }
        @Suppress("DialogTitleCapitalization")
        withTitle(CangJieBundle.message("dialog.title.select.cjpm.json"))
    }

    override fun isFileSelectable(file: VirtualFile?): Boolean {
        return super.isFileSelectable(file) && file != null && (!file.isDirectory || file.findChild(CjpmConstants.MANIFEST_FILE) != null)
    }
}

