package com.huawei.cangjie.psi.psiUtil

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil

/**
 * Deletes this single PSI element from the PSI tree without removing other elements.
 * This method is mostly used as a substitute for [PsiElement.delete] because [PsiElement.delete] can delete more than the element it is
 * called on. When for example calling it on a [KtClassOrObject], [PsiElement.delete] will delete the class or object but when this class
 * or object is the only declaration in the file, it will also delete the file. On the contrary [PsiElement.deleteSingle] will only delete
 * the class or object here, but not the file.
 */
fun PsiElement.deleteSingle() {
    CodeEditUtil.removeChild(parent?.node ?: return, node ?: return)
}
