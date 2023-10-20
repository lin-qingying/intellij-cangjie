package com.huawei.cangjie.parsing

import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType



interface SemanticWhitespaceAwarePsiBuilder : PsiBuilder {

    /**
     * 检查当前标记之前是否有换行符
     */
    fun newlineBeforeCurrentToken(): Boolean

    /**
     * 禁用换行符
     */
    fun disableNewlines()

    /**
     * 启用换行符
     */
    fun enableNewlines()

    /**
     * 恢复换行符状态
     */
    fun restoreNewlinesState()

    /**
     * 恢复复杂标记的连接
     */
    fun restoreJoiningComplexTokensState()

    /**
     * 启用复杂标记的连接
     */
    fun enableJoiningComplexTokens()

    /**
     * 禁用复杂标记的连接
     */
    fun disableJoiningComplexTokens()

    /**
     * 检查当前标记是否是空白符或注释
     */
    override fun isWhitespaceOrComment(elementType: IElementType): Boolean
}
