package com.huawei.cangjie.psi.psiUtil

import com.huawei.cangjie.doc.psi.CDoc
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjDeclarationModifierList


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
