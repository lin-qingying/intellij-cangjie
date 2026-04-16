/*
 * Copyright 2025 LinQingYing. and contributors.
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


package org.cangnova.cangjie.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange

class SyntheticCangJieBlock(
    private val node: ASTNode,
    private val subBlocks: List<ASTBlock>,
    private val alignment: Alignment?,
    private val indent: Indent?,
    private val wrap: Wrap?,
    private val spacingBuilder: CangJieSpacingBuilder,
    private val createParentSyntheticSpacingBlock: (ASTNode) -> ASTBlock
) : ASTBlock {

    private val textRange = TextRange(
        subBlocks.first().textRange.startOffset,
        subBlocks.last().textRange.endOffset
    )

    override fun getTextRange(): TextRange = textRange
    override fun getSubBlocks() = subBlocks
    override fun getWrap() = wrap
    override fun getIndent() = indent
    override fun getAlignment() = alignment
    override fun getChildAttributes(newChildIndex: Int) = ChildAttributes(getIndent(), null)
    override fun isIncomplete() = getSubBlocks().last().isIncomplete
    override fun isLeaf() = false
    override fun getNode() = node
    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingBuilder.getSpacing(createParentSyntheticSpacingBlock(node), child1, child2)
    }


    override fun toString(): String {
        var child = subBlocks.first()
        var treeNode: ASTNode? = null

        loop@
        while (treeNode == null) when (child) {
            is SyntheticCangJieBlock -> child = child.getSubBlocks().first()

            else -> treeNode = child.node
        }

        val textRange = getTextRange()
        val psi = treeNode.psi
        if (psi != null) {
            val file = psi.containingFile
            if (file != null) {
                return file.text!!.subSequence(textRange.startOffset, textRange.endOffset).toString() + " " + textRange
            }
        }

        return this::class.java.name + ": " + textRange
    }
}
