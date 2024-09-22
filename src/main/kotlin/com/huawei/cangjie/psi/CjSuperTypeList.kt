package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.EditCommaSeparatedListHelper.addItem
import com.huawei.cangjie.psi.EditCommaSeparatedListHelper.removeItem
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.huawei.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class CjSuperTypeList : CjElementImplStub<CangJiePlaceHolderStub<CjSuperTypeList >  > {
    private val _modificationStamp = AtomicLong()

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjSuperTypeList >) : super(stub, CjStubElementTypes.SUPER_TYPE_LIST)

    override fun toString(): String {
        return node.elementType.toString()
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitSuperTypeList(this, data)
    }

    fun addEntry(entry: CjSuperTypeListEntry): CjSuperTypeListEntry {
        return addItem(
            this,
            entries, entry
        )
    }

    fun removeEntry(entry: CjSuperTypeListEntry) {
        removeItem(entry)
        if (entries.isEmpty()) {
            delete()
        }
    }

    @Throws(IncorrectOperationException::class)
    override fun delete() {
        var left = PsiTreeUtil.skipSiblingsBackward(
            this,
            PsiWhiteSpace::class.java,
            PsiComment::class.java
        )
        if (left == null || left.node.elementType !== CjTokens.COLON) left = this
        parent.deleteChildRange(left, this)
    }

    val entries: List<CjSuperTypeListEntry>
        get() = listOf(
            *getStubOrPsiChildren(
                CjTokenSets.SUPER_TYPE_LIST_ENTRIES,
                CjSuperTypeListEntry.ARRAY_FACTORY
            )
        )


    override fun subtreeChanged() {
        super.subtreeChanged()
        _modificationStamp.getAndIncrement()
    }

    val modificationStamp : Long get() =
          _modificationStamp.get()

}
