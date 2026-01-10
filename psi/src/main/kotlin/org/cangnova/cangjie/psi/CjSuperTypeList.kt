/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.EditCommaSeparatedListHelper.addItem
import org.cangnova.cangjie.psi.EditCommaSeparatedListHelper.removeItem
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import java.util.concurrent.atomic.AtomicLong

class CjSuperTypeList : CjElementImplStub<CangJiePlaceHolderStub<CjSuperTypeList>> {
    private val _modificationStamp = AtomicLong()

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjSuperTypeList>) : super(stub, CjStubElementTypes.SUPER_TYPE_LIST)

    override fun toString(): String {
        return node.elementType.toString()
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitSuperTypeList(this, data)
    }

    fun addEntry(entry: CjSuperTypeListEntry): CjSuperTypeListEntry {
        return addItem(
            this,
            entries,
            entry,
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
            PsiComment::class.java,
        )
        if (left == null || left.node.elementType !== CjTokens.COLON) left = this
        parent.deleteChildRange(left, this)
    }

    val entries: List<CjSuperTypeListEntry>
        get() = listOf(
            *getStubOrPsiChildren(
                CjTokenSets.SUPER_TYPE_LIST_ENTRIES,
                CjSuperTypeListEntry.ARRAY_FACTORY,
            ),
        )

    override fun subtreeChanged() {
        super.subtreeChanged()
        _modificationStamp.getAndIncrement()
    }

    val modificationStamp: Long get() =
        _modificationStamp.get()
}
