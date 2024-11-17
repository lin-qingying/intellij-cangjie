/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

//package com.linqingying.cangjie.lang.sdk
//
//import com.linqingying.cangjie.CangJieBundle
//import com.linqingying.cangjie.lang.CangJieFileType.INSTANCE
//import com.linqingying.cangjie.lang.CangJieLanguage
//import com.linqingying.cangjie.lang.lsp.CangJieLspServerManager
//import com.intellij.codeInsight.daemon.ProjectSdkSetupValidator
//import com.intellij.json.JsonLanguage
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.fileTypes.FileTypeRegistry
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.projectRoots.SdkTypeId
//import com.intellij.openapi.roots.ProjectRootManager
//import com.intellij.openapi.roots.ui.configuration.SdkPopupBuilder
//import com.intellij.openapi.roots.ui.configuration.SdkPopupFactory
//import com.intellij.openapi.vfs.VirtualFile
//import com.intellij.psi.PsiManager
//import com.intellij.ui.EditorNotificationPanel
//
//
//class CangJieLanguageSdkSetupValidator : ProjectSdkSetupValidator {
//    companion object {
//        private fun preparePopup(project: Project, file: VirtualFile): SdkPopupBuilder {
//            return SdkPopupFactory.newBuilder()
//                .withProject(project)
//                .withSdkTypeFilter { type: SdkTypeId? -> type is CangJieSdkType }
//                .updateSdkForFile(file)
//        }
//    }
//
//    override fun isApplicableFor(project: Project, file: VirtualFile): Boolean {
////        return file.fileType == CangJieFileType.INSTANCE
////        if (!FileTypeRegistry.getInstance()
////                .isFileOfType(file, CangJieFileType.INSTANCE)
////        ) {
//
//
////   如果扩展名是.cj 返回true
//        if (file.extension == "cj") {
//            return true
//        }
//        val psiFile = PsiManager.getInstance(project).findFile(file)
//        if (psiFile != null) {
//
//            if (psiFile.language is JsonLanguage) {
//                if (psiFile.virtualFile.name.startsWith("module")) {
//                    return true
//                }
//            }
//
//
//
//            return psiFile.language.isKindOf(CangJieLanguage)
//        }
////        }
//        return false
//    }
//
//    override fun getFixHandler(project: Project, file: VirtualFile): EditorNotificationPanel.ActionHandler {
////        val handler = object : EditorNotificationPanel.ActionHandler {
////            override fun handlePanelActionClick(panel: EditorNotificationPanel, event: HyperlinkEvent) {
////                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Project SDKs")
////            }
////
////            override fun handleQuickFixClick(editor: Editor, psiFile: PsiFile) {
////                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Project SDKs")
////
////            }
////        }
////        //打开sdk设置
////        return handler
//        return preparePopup(project, file).buildEditorNotificationPanelHandler()
//    }
//
//    override fun getErrorMessage(project: Project, file: VirtualFile): String? {
//
//        val sdk = CangJieSdkManager.getProjectSdk(project)
//        if (sdk == null || sdk.sdkType !is CangJieSdkType) {
//
////            ApplicationManager.getApplication().invokeLater {
////                ApplicationManager.getApplication().runWriteAction {
////
////
////                    ProjectRootManager.getInstance(project).projectSdk = null
////
////                }
////            }
//            CangJieLspServerManager.shutdownAllServers()
//            return CangJieBundle.message("cangjie.sdk.please.configure")
//        }
//        CangJieLspServerManager.restartLspServer()
//
//        return null
//    }
//
//}
