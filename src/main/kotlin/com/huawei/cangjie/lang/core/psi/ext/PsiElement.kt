package com.huawei.cangjie.lang.core.psi.ext

import com.huawei.cangjie.lang.core.psi.CjFile
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.PsiElement
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement
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
inline fun <reified T : PsiElement> PsiElement.descendantOfTypeStrict(): T? =
    PsiTreeUtil.findChildOfType(this, T::class.java, /* strict */ true)

@Suppress("UNCHECKED_CAST")
inline val <T : StubElement<*>> StubBasedPsiElement<T>.greenStub: T?
    get() = (this as? StubBasedPsiElementBase<T>)?.greenStub
