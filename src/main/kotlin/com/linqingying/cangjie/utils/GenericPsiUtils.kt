
@file:JvmName("GenericPsiUtils")
package com.linqingying.cangjie.utils

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.annotations.ApiStatus

val PsiElement.module: Module?
    get() = ModuleUtilCore.findModuleForPsiElement(this)


fun PsiElement.reformat(canChangeWhiteSpacesOnly: Boolean = false){
    CodeStyleManager.getInstance(project).reformat(this, canChangeWhiteSpacesOnly)
}


fun PsiElement.reformatted(canChangeWhiteSpacesOnly: Boolean = false): PsiElement {
    reformat(canChangeWhiteSpacesOnly = canChangeWhiteSpacesOnly)
    return this
}
