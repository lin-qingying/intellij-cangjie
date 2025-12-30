/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi.packgae

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
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

    /**
     * 根据当前编辑的文件，建议最合适的目录
     * 通过分析当前编辑器状态，确定最合适的目录范围
     *
     * @param psiPackage CangJiePackage对象，代表一个 PSI 包
     * @return 返回一个PsiDirectory数组，表示建议的最合适的目录
     */
    @VisibleForTesting
    fun suggestMostAppropriateDirectories(psiPackage: CangJiePackage): Array<PsiDirectory> {
        // 获取当前PSI包所属的项目
        val project: Project = psiPackage.project
        var directories: Array<PsiDirectory>? = null

        // 获取当前项目中选中的编辑器
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            // 从编辑器获取文档
            val document = editor.document
            // 根据文档获取PsiFile对象
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
            if (psiFile != null) {
                // 查找PsiFile所属的模块
                val module = ModuleUtilCore.findModuleForPsiElement(psiFile)
                if (module != null) {
                    // 获取PsiFile对应的虚拟文件
                    val virtualFile = PsiUtilCore.getVirtualFile(psiFile)
                    if (virtualFile != null) {
                        // 判断虚拟文件是否在测试源码内容中
                        if (ModuleRootManager.getInstance(module).fileIndex.isInTestSourceContent(virtualFile)) {
                            // 如果在测试源码中，获取对应模块及其依赖的测试目录
                            directories =
                                psiPackage.getDirectories(GlobalSearchScope.moduleTestsWithDependentsScope(module))
                        }

                        // 如果未找到合适的目录，尝试在内容根目录中查找
                        if (directories.isNullOrEmpty()) {
                            val contentRootForFile =
                                ProjectRootManager.getInstance(project).fileIndex.getContentRootForFile(virtualFile)
                            if (contentRootForFile != null) {
                                // 获取项目中与内容根目录相关的目录
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

                    // 如果仍然没有合适的目录，扩展搜索范围到模块及其依赖和库
                    if (directories.isNullOrEmpty()) {
                        directories =
                            psiPackage.getDirectories(GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))
                    }
                } else {
                    // 如果没有找到模块，将搜索范围限定在项目范围之外
                    directories =
                        psiPackage.getDirectories(GlobalSearchScope.notScope(GlobalSearchScope.projectScope(project)))
                }
            }
        }

        // 如果没有找到任何目录，返回PSI包的默认目录
        if (directories.isNullOrEmpty()) {
            directories = psiPackage.directories
        }
        return directories
    }

    fun navigate(psiPackage: AbstractCangJiePackage, requestFocus: Boolean) {
        val project: Project = psiPackage.project
        val window = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW)
        window?.activate(null)
        val projectView = ProjectView.getInstance(project)
        val directories: Array<PsiDirectory> =
            suggestMostAppropriateDirectories(psiPackage)
        if (directories.isEmpty()) return

        TODO("导航打开文件")
    }
}
