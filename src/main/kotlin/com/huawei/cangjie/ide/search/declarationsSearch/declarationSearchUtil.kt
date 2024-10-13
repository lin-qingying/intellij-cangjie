package com.huawei.cangjie.ide.search.declarationsSearch

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
interface SearchRequestWithElement<T : PsiElement> : DeclarationSearchRequest<T> {
    val originalElement: T
    override val project: Project get() = originalElement.project
}
interface DeclarationSearchRequest<in T> {
    val project: Project
    val searchScope: SearchScope
}

class HierarchySearchRequest<T : PsiElement>(
    override val originalElement: T,
    override val searchScope: SearchScope,
    val searchDeeply: Boolean = true
) : SearchRequestWithElement<T> {
    fun <U : PsiElement> copy(newOriginalElement: U): HierarchySearchRequest<U> =
        HierarchySearchRequest(newOriginalElement, searchScope, searchDeeply)
}
