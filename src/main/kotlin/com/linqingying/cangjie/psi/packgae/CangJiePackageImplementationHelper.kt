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
