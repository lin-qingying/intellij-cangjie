package com.huawei.cangjie.lang.core.psi

import com.huawei.cangjie.lang.core.psi.ext.*
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil

object CjPsiImplUtil {

    fun crateRelativePath(element: CjNamedElement): String? {
        val name = element.name ?: return null
        val qualifier = element.containingMod.crateRelativePath ?: return null
        return "$qualifier::$name"
    }

    fun modCrateRelativePath(mod: CjMod): String? {
        val segments = mod.superMods.asReversed().drop(1).map {
            it.modName ?: return null
        }
        if (segments.isEmpty()) return ""
        return "." + segments.joinToString(".")
    }

}
