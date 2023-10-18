package com.huawei.cangjie1.psi.psiUtil

import com.huawei.cangjie1.doc.psi.CDoc
import com.huawei.cangjie1.psi.CjFile
import com.huawei.cangjie1.psi.CjDeclaration
import com.huawei.cangjie1.psi.CjDeclarationModifierList


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
