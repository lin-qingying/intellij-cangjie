package com.huawei.cangjie.resolve

import com.huawei.cangjie.psi.CjElement




/**
 * ScopeEntry is some PsiElement visible in some code scope.
 *
 * [ScopeEntry] handles the two case:
 *   * aliases (that's why we need a [name] property)
 *   * lazy resolving of actual elements (that's why [element] can return `null`)
 */
//interface ScopeEntry {
//    val name: String
//    val element: CjElement
//    val namespaces: Set<Namespace>
//    val subst: Substitution get() = emptySubstitution
//    fun doCopyWithNs(namespaces: Set<Namespace>): ScopeEntry
//}