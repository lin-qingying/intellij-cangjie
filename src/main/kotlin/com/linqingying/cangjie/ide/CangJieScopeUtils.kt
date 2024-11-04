package com.linqingying.cangjie.ide

import com.linqingying.cangjie.lang.CangJieFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
fun PsiFile.fileScope(): GlobalSearchScope = GlobalSearchScope.fileScope(this)

fun PsiElement.useScope(): SearchScope = PsiSearchHelper.getInstance(project).getUseScope(this)
fun GlobalSearchScope.restrictToCangJieSources() = restrictByFileType(CangJieFileType.INSTANCE)
fun GlobalSearchScope.restrictByFileType(fileType: FileType) = GlobalSearchScope.getScopeRestrictedByFileTypes(this, fileType)
fun Project.everythingScopeExcludeFileTypes(vararg fileTypes: FileType): GlobalSearchScope {
    return GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.everythingScope(this), *fileTypes).not()
}
operator fun GlobalSearchScope.not(): GlobalSearchScope = GlobalSearchScope.notScope(this)
fun SearchScope.restrictToCangJieSources() = restrictByFileType(CangJieFileType.INSTANCE)
fun SearchScope.restrictByFileType(fileType: FileType): SearchScope = when (this) {
    is GlobalSearchScope -> restrictByFileType(fileType)
    is LocalSearchScope -> {
        val elements = scope.filter { it.containingFile.fileType == fileType }
        when (elements.size) {
            0 -> LocalSearchScope.EMPTY
            scope.size -> this
            else -> LocalSearchScope(elements.toTypedArray())
        }
    }
    else -> this
}
fun Project.allScope(): GlobalSearchScope = GlobalSearchScope.allScope(this)
