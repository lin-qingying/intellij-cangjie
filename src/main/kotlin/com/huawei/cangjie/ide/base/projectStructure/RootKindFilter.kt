package com.huawei.cangjie.ide.base.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

data class RootKindFilter(
    val includeProjectSourceFiles: Boolean,
    val includeLibraryClassFiles: Boolean,
    val includeLibrarySourceFiles: Boolean,
    val includeScriptDependencies: Boolean,
    val includeScriptsOutsideSourceRoots: Boolean,
    val includeResources: Boolean
) {


    companion object {
        @JvmField
        val projectSourcesAndLibraryClasses = RootKindFilter(
            includeProjectSourceFiles = true,
            includeLibraryClassFiles = true,
            includeLibrarySourceFiles = false,
            includeScriptDependencies = true,
            includeScriptsOutsideSourceRoots = false,
            includeResources = false
        )
        @JvmField
        val projectFiles = RootKindFilter(
            includeProjectSourceFiles = true,
            includeLibraryClassFiles = true,
            includeLibrarySourceFiles = false,
            includeScriptDependencies = false,
            includeScriptsOutsideSourceRoots = false,
            includeResources = false
        )
        @JvmField
        val projectSourcesAndResources = RootKindFilter(
            includeProjectSourceFiles = true,
            includeLibraryClassFiles = false,
            includeLibrarySourceFiles = false,
            includeScriptDependencies = false,
            includeScriptsOutsideSourceRoots = false,
            includeResources = true
        )

        @JvmField
        val librarySources = RootKindFilter(
            includeProjectSourceFiles = false,
            includeLibraryClassFiles = false,
            includeLibrarySourceFiles = true,
            includeScriptDependencies = true,
            includeScriptsOutsideSourceRoots = false,
            includeResources = false
        )

        @JvmField
        val everything = RootKindFilter(
            includeProjectSourceFiles = true,
            includeLibraryClassFiles = true,
            includeLibrarySourceFiles = true,
            includeScriptDependencies = true,
            includeScriptsOutsideSourceRoots = false,
            includeResources = false
        )
        @JvmField
        val libraryFiles = RootKindFilter(
            includeProjectSourceFiles = false,
            includeLibraryClassFiles = true,
            includeLibrarySourceFiles = true,
            includeScriptDependencies = true,
            includeScriptsOutsideSourceRoots = false,
            includeResources = false
        )
        @JvmField
        val libraryClasses = RootKindFilter(
            includeProjectSourceFiles = false,
            includeLibraryClassFiles = true,
            includeLibrarySourceFiles = false,
            includeScriptDependencies = true,
            includeScriptsOutsideSourceRoots = false,
            includeResources = false
        )

        @JvmField
        val projectSources = RootKindFilter(
            includeProjectSourceFiles = true,
            includeLibraryClassFiles = false,
            includeLibrarySourceFiles = false,
            includeScriptDependencies = false,
            includeScriptsOutsideSourceRoots = false,
            includeResources = false
        )
    }
}
fun RootKindFilter.matches(element: PsiElement): Boolean {
    return RootKindMatcher.matches(element, this)
}
fun RootKindFilter.matches(project: Project, virtualFile: VirtualFile): Boolean {
    return RootKindMatcher.matches(project, virtualFile, this)
}

interface RootKindMatcher {
    fun matches(filter: RootKindFilter, virtualFile: VirtualFile): Boolean

    companion object {
        @JvmStatic
        fun matches(project: Project, virtualFile: VirtualFile, filter: RootKindFilter): Boolean {
            val matcherService = project.service<RootKindMatcher>()
            return matcherService.matches(filter, virtualFile)
        }

        @JvmStatic
        fun matches(element: PsiElement, filter: RootKindFilter): Boolean {
            val virtualFile = when (element) {
                is PsiDirectory -> element.virtualFile
                is PsiFile -> element.virtualFile
                else -> element.containingFile?.virtualFile
            }

            return if (virtualFile != null) matches(element.project, virtualFile, filter) else false
        }
    }

}
