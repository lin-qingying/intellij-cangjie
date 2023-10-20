package com.huawei.cangjie.lang.doc.psi

import com.huawei.cangjie.lang.core.psi.CjTokenType
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.tree.ICompositeElementType
import com.intellij.psi.tree.IElementType

open class CjDocTokenType(debugName: String) : CjTokenType(debugName)

class CjDocCompositeTokenType(
    debugName: String,
    private val astFactory: (IElementType) -> CompositeElement
) : CjDocTokenType(debugName), ICompositeElementType {
    override fun createCompositeNode(): CompositeElement = astFactory(this)
}
