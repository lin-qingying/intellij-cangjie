package com.huawei.cangjie.lang.core.psi.ext

import com.huawei.cangjie.lang.core.psi.CjFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil


inline fun <reified T : PsiElement> PsiElement.contextStrict(): T? =
    PsiTreeUtil.getContextOfType(this, T::class.java, /* strict */ true)
val PsiElement.containingCjFileSkippingCodeFragments: CjFile?
    get() {
        var containingFile = containingFile.originalFile

        while (containingFile !is CjFile) {
            containingFile = containingFile.context?.containingFile?.originalFile ?: break
        }
        return containingFile as? CjFile
    }
