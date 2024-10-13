package com.huawei.cangjie.ide.util

import com.huawei.cangjie.ide.everythingScopeExcludeFileTypes
import com.huawei.cangjie.lang.CangJieFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope

fun SearchScope.excludeCangJieSources(project: Project): SearchScope = excludeFileTypes(project, CangJieFileType.INSTANCE)

fun SearchScope.excludeFileTypes(project: Project, vararg fileTypes: FileType): SearchScope {
    return if (this is GlobalSearchScope) {
        this.intersectWith(project.everythingScopeExcludeFileTypes(*fileTypes))
    } else {
        this as LocalSearchScope
        val filteredElements = scope.filter { it.containingFile.fileType !in fileTypes }
        if (filteredElements.isNotEmpty())
            LocalSearchScope(filteredElements.toTypedArray())
        else
            LocalSearchScope.EMPTY
    }
}
