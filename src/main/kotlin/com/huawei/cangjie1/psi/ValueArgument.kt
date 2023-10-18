package com.huawei.cangjie1.psi

import com.huawei.cangjie1.name.Name
import com.intellij.psi.impl.source.tree.LeafPsiElement

interface ValueArgument {
    @IfNotParsed
    fun getArgumentExpression(): CjExpression?

    fun getArgumentName(): ValueArgumentName?

    fun isNamed(): Boolean

    fun asElement(): CjElement

    /* The '*' in something like foo(*arr) i.e. pass an array as a number of vararg arguments */
    fun getSpreadElement(): LeafPsiElement?

    /* The argument is placed externally to call element, e.g. in 'when' condition with subject: 'when (a) { in c -> }' */
    fun isExternal(): Boolean
}
interface ValueArgumentName {
    val asName: Name
    val referenceExpression: CjSimpleNameExpression?
}
