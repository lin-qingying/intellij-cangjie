package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.firstIsInstanceOrNull
import com.huawei.cangjie.psi.psiUtil.siblings
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace

fun setTypeReference(declaration: CjCallableDeclaration, addAfter: PsiElement?, typeRef: CjTypeReference?): CjTypeReference? {
    val oldTypeRef = getTypeReference(declaration)
    if (typeRef != null) {
        return if (oldTypeRef != null) {
            oldTypeRef.replace(typeRef) as CjTypeReference
        } else {
            val anchor = addAfter
                ?: declaration.nameIdentifier?.siblings(forward = true)?.firstOrNull { it is PsiErrorElement }
                ?: (declaration as? CjParameter)?.destructuringDeclaration
            val newTypeRef = declaration.addAfter(typeRef, anchor) as CjTypeReference
            declaration.addAfter(CjPsiFactory(declaration.project).createColon(), anchor)
            newTypeRef
        }
    } else {
        if (oldTypeRef != null) {
            val colon = declaration.colon!!
            val removeFrom = colon.prevSibling as? PsiWhiteSpace ?: colon
            declaration.deleteChildRange(removeFrom, oldTypeRef)
        }
        return null
    }
}
fun getTypeReference(declaration: CjCallableDeclaration): CjTypeReference? {
    return declaration.firstChild!!.siblings(forward = true)
        .dropWhile { it.node!!.elementType != CjTokens.COLON }
        .firstIsInstanceOrNull<CjTypeReference>()
}
