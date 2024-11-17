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

package com.linqingying.cangjie.psi.packgae

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.psi.impl.file.PsiDirectoryOrFile
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.annotations.VisibleForTesting

@Service(Service.Level.APP)
object CangJiePackageImplementationHelper {

    fun getDirectoryCachedValueDependencies(psiPackage: CangJiePackage): Array<Any> {
        return arrayOf(
            PsiModificationTracker.MODIFICATION_COUNT,
            ProjectRootManager.getInstance(psiPackage.project)
        )
    }

    @VisibleForTesting
    fun suggestMostAppropriateDirectories(psiPackage: CangJiePackage): Array<PsiDirectory> {
        val project: Project = psiPackage.project
        var directories: Array<PsiDirectory>? = null
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            val document = editor.document
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
            if (psiFile != null) {
                val module = ModuleUtilCore.findModuleForPsiElement(psiFile)
                if (module != null) {
                    val virtualFile = PsiUtilCore.getVirtualFile(psiFile)
                    if (virtualFile != null) {
                        if (ModuleRootManager.getInstance(module).fileIndex.isInTestSourceContent(virtualFile)) {
                            directories =
                                psiPackage.getDirectories(GlobalSearchScope.moduleTestsWithDependentsScope(module))
                        }

                        if (directories.isNullOrEmpty()) {
                            val contentRootForFile =
                                ProjectRootManager.getInstance(project).fileIndex.getContentRootForFile(virtualFile)
                            if (contentRootForFile != null) {
                                directories = psiPackage.getDirectories(
                                    GlobalSearchScopes.directoriesScope(
                                        project,
                                        true,
                                        contentRootForFile
                                    )
                                )
                            }
                        }
                    }

                    if (directories.isNullOrEmpty()) {
                        directories =
                            psiPackage.getDirectories(GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))
                    }
                } else {
                    directories =
                        psiPackage.getDirectories(GlobalSearchScope.notScope(GlobalSearchScope.projectScope(project)))
                }
            }
        }

        if (directories.isNullOrEmpty()) {
            directories = psiPackage.directories
        }
        return directories
    }

    fun navigate(psiPackage: AbstractCangJiePackage, requestFocus: Boolean) {
        val project: Project = psiPackage.getProject()
        val window = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW)
        window?.activate(null)
        val projectView = ProjectView.getInstance(project)
        val directories: Array<PsiDirectory> =
            suggestMostAppropriateDirectories(psiPackage)
        if (directories.isEmpty()) return

        if(directories[0].isDirectory){
            projectView.select(directories[0], directories[0].virtualFile, requestFocus)

        }else if( directories[0] is PsiDirectoryOrFile && !directories[0].isDirectory ){
//            打开文件
            FileEditorManager.getInstance(project).openFile(directories[0].virtualFile, true)
        }
    }
}
