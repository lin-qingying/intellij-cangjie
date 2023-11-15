package com.huawei.cangjie.lang.sdk

import com.huawei.cangjie.lang.CangJieFileType
import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.lang.lsp.CangJieLspServerManager
import com.intellij.codeInsight.daemon.ProjectSdkSetupValidator
import com.intellij.json.JsonLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.SdkPopupBuilder
import com.intellij.openapi.roots.ui.configuration.SdkPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel


class CangJieLanguageSdkSetupValidator : ProjectSdkSetupValidator {
    companion object {
        private fun preparePopup(project: Project, file: VirtualFile): SdkPopupBuilder {
            return SdkPopupFactory.newBuilder()
                .withProject(project)
                .withSdkTypeFilter { type: SdkTypeId? -> type is CangJieSdkType }
                .updateSdkForFile(file)
        }
    }

    override fun isApplicableFor(project: Project, file: VirtualFile): Boolean {
//        return file.fileType == CangJieFileType
//        if (!FileTypeRegistry.getInstance()
//                .isFileOfType(file, CangJieFileType)
//        ) {
        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (psiFile != null) {

            if (psiFile.language is JsonLanguage) {
                if (psiFile.virtualFile.name.startsWith("module")) {
                    return true
                }
            }

            return psiFile.language.isKindOf(CangJieLanguage)
        }
//        }
        return false
    }

    override fun getFixHandler(project: Project, file: VirtualFile): EditorNotificationPanel.ActionHandler {
//        val handler = object : EditorNotificationPanel.ActionHandler {
//            override fun handlePanelActionClick(panel: EditorNotificationPanel, event: HyperlinkEvent) {
//                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Project SDKs")
//            }
//
//            override fun handleQuickFixClick(editor: Editor, psiFile: PsiFile) {
//                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Project SDKs")
//
//            }
//        }
//        //打开sdk设置
//        return handler
        return preparePopup(project, file).buildEditorNotificationPanelHandler()
    }

    override fun getErrorMessage(project: Project, file: VirtualFile): String? {

        val sdk = CangJieSdkManager.getProjectSdk(project)
        if (sdk == null || sdk.sdkType !is CangJieSdkType) {

//            ApplicationManager.getApplication().invokeLater {
//                ApplicationManager.getApplication().runWriteAction {
//
//
//                    ProjectRootManager.getInstance(project).projectSdk = null
//
//                }
//            }
            return "Please setup SDK for CangJie"
        }

        CangJieLspServerManager.restartLspServer()
        return null
    }

}
