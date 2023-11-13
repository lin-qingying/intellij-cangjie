package com.huawei.cangjie.lang.sdk

import com.huawei.cangjie.lang.CangJieFileType
import com.intellij.codeInsight.daemon.ProjectSdkSetupValidator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.EditorNotificationPanel
import javax.swing.event.HyperlinkEvent


class CangJieLanguageSdkSetupValidator : ProjectSdkSetupValidator {
    override fun isApplicableFor(project: Project, file: VirtualFile): Boolean {
        return file.fileType == CangJieFileType
    }

    override fun getFixHandler(project: Project, file: VirtualFile): EditorNotificationPanel.ActionHandler {
        val handler = object : EditorNotificationPanel.ActionHandler {
            override fun handlePanelActionClick(panel: EditorNotificationPanel, event: HyperlinkEvent) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Project SDKs")
            }

            override fun handleQuickFixClick(editor: Editor, psiFile: PsiFile) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Project SDKs")

            }
        }
        //打开sdk设置
        return handler
    }

    override fun getErrorMessage(project: Project, file: VirtualFile): String? {

        val sdk = CangJieSdkManager.getProjectSdk()
        if (sdk == null || sdk.sdkType !is CangJieSdkType) {

            ApplicationManager.getApplication().invokeLater {
                ApplicationManager.getApplication().runWriteAction {


                    ProjectRootManager.getInstance(project).projectSdk = null

                }
            }
            return "Please setup SDK for CangJie"
        }
        return null
    }

}
