package com.huawei.cangjie.psi

import com.huawei.cangjie.doc.psi.CDoc
import com.intellij.util.ArrayFactory

interface CjDeclaration : CjExpression, CjModifierListOwner {
    val docComment: CDoc?


    val expression: CjExpression?


    companion object {
        val EMPTY_ARRAY: Array<CjDeclaration?> = arrayOfNulls(0)

        val ARRAY_FACTORY: ArrayFactory<CjDeclaration> =
            ArrayFactory { count: Int -> if (count == 0) EMPTY_ARRAY else arrayOfNulls(count) }
    }
}
