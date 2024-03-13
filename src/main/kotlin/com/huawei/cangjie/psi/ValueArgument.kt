package com.huawei.cangjie.psi

import com.huawei.cangjie.name.Name
import com.intellij.psi.impl.source.tree.LeafPsiElement

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
}

interface ValueArgumentName {
    val asName: Name
    val referenceExpression: CjSimpleNameExpression?
}
interface LambdaArgument : ValueArgument {
    fun getLambdaExpression(): CjLambdaExpression?
}