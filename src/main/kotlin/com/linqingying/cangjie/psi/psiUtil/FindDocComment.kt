package com.linqingying.cangjie.psi.psiUtil

import com.linqingying.cangjie.doc.psi.CDoc
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjDeclarationModifierList


fun findDocComment(declaration: CjDeclaration): CDoc? {
    val containingFile = declaration.containingFile
    if (containingFile is CjFile && containingFile.isCompiled) {
        //can't use containingCjFile due to non-physical code fragments, e.g. ssr
        return null
    }
    return declaration.allChildren
        .flatMap {
            if (it is CjDeclarationModifierList) {
                return@flatMap it.children.asSequence()
            }
            sequenceOf(it)
        }
        .dropWhile { it !is CDoc }
        .firstOrNull() as? CDoc
}
