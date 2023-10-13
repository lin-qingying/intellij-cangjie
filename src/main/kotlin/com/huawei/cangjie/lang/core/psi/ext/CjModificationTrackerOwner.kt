package com.huawei.cangjie.lang.core.psi.ext

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement

interface CjModificationTrackerOwner : CjElement {
    val modificationTracker: ModificationTracker


    fun incModificationCount(element: PsiElement): Boolean
}
