package com.linqingying.cangjie.utils

import com.linqingying.cangjie.resolve.lazy.NoDescriptorForDeclarationException
import com.intellij.psi.PsiElement

/**
 * Best effort to analyze element:
 * - Best effort for file that is out of source root scope: NoDescriptorForDeclarationException could be swallowed
 * - Do not swallow NoDescriptorForDeclarationException during analysis for in source scope files
 */
inline fun <T> PsiElement.actionUnderSafeAnalyzeBlock(
    crossinline action: () -> T,
    crossinline fallback: () -> T
): T = try {
    action()
} catch (e: Exception) {
    e.returnIfNoDescriptorForDeclarationException(condition = {
        val file = containingFile
        it && (!file.isPhysical || !file.isUnderCangJieSourceRootTypes())
    }) { fallback() }
}


val Exception.isItNoDescriptorForDeclarationException: Boolean
    get() = this is NoDescriptorForDeclarationException || cause?.safeAs<Exception>()?.isItNoDescriptorForDeclarationException == true

inline fun <T> Exception.returnIfNoDescriptorForDeclarationException(
    crossinline condition: (Boolean) -> Boolean = { v -> v },
    crossinline computable: () -> T
): T =
    if (condition(this.isItNoDescriptorForDeclarationException)) {
        computable()
    } else {
        throw this
    }

