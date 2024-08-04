package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.psiUtil.getContentRange
import com.linqingying.cangjie.psi.psiUtil.isSingleQuoted
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import gnu.trove.TIntArrayList
import kotlin.math.min



class CangJieStringLiteralTextEscaper(host: CjStringTemplateExpression) : LiteralTextEscaper<CjStringTemplateExpression>(host) {
    private var sourceOffsets: IntArray? = null

    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        val sourceOffsetsList = mutableListOf<Int>()
        var sourceOffset = 0

        for (child in myHost.entries) {
            val childRange = TextRange.from(child.startOffsetInParent, child.textLength)
            if (rangeInsideHost.endOffset <= childRange.startOffset) {
                break
            }
            if (childRange.endOffset <= rangeInsideHost.startOffset) {
                continue
            }
            when (child) {
                is CjEscapeStringTemplateEntry -> {
                    if (!rangeInsideHost.contains(childRange)) {

                        sourceOffsetsList.add(sourceOffset)
                        sourceOffsets = sourceOffsetsList.toIntArray()
                        return false
                    }
                    val unescaped = child.unescapedValue
                    outChars.append(unescaped)
                    repeat(unescaped.length) {
                        sourceOffsetsList.add(sourceOffset)
                    }
                    sourceOffset += child.getTextLength()
                }
                else -> {
                    val textRange = rangeInsideHost.intersection(childRange)!!.shiftRight(-childRange.startOffset)
                    outChars.append(child.text, textRange.startOffset, textRange.endOffset)
                    repeat(textRange.length) {
                        sourceOffsetsList.add(sourceOffset++)
                    }
                }
            }
        }
        sourceOffsetsList.add(sourceOffset)
        sourceOffsets = sourceOffsetsList.toIntArray()
        return true
    }

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
        val offsets = sourceOffsets
        if (offsets == null || offsetInDecoded >= offsets.size) return -1
        return min(offsets[offsetInDecoded], rangeInsideHost.length) + rangeInsideHost.startOffset
    }

    override fun getRelevantTextRange(): TextRange {
        return myHost.getContentRange()
    }

    override fun isOneLine(): Boolean {
        return myHost.isSingleQuoted()
    }
}
