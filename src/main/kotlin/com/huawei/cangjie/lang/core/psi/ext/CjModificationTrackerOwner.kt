package com.huawei.cangjie.lang.core.psi.ext

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement

interface CjModificationTrackerOwner : CjElement {
    val modificationTracker: ModificationTracker

    /**
     * Increments local modification counter if needed.
     *
     * If and only if false returned,
     * [RsPsiManager.rustStructureModificationTracker]
     * will be incremented.
     *
     * @param element the changed psi element
     * @see org.rust.lang.core.psi.RsPsiManagerImpl.updateModificationCount
     */
    fun incModificationCount(element: PsiElement): Boolean
}
