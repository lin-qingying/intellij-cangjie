package com.linqingying.cangjie.psi

import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.linqingying.cangjie.name.Name

interface ValueArgument {
    @IfNotParsed
    fun getArgumentExpression(): CjExpression?

    fun getArgumentName(): ValueArgumentName?

    fun isNamed(): Boolean

    fun asElement(): CjElement

    /* 例如foo(*arr)中的‘*’，即将数组作为多个var arg参数传递*/
    fun getSpreadElement(): LeafPsiElement?

    /* 参数放在外部以调用元素*/
    fun isExternal(): Boolean

    companion object
}

interface ValueArgumentName {
    val asName: Name
    val referenceExpression: CjSimpleNameExpression?
}

interface LambdaArgument : ValueArgument {
    fun getLambdaExpression(): CjLambdaExpression?
}
