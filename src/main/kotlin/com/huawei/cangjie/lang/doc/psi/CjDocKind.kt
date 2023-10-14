package com.huawei.cangjie.lang.doc.psi

import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.INNER_BLOCK_DOC_COMMENT
import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.INNER_EOL_DOC_COMMENT
import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.OUTER_BLOCK_DOC_COMMENT
import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.OUTER_EOL_DOC_COMMENT
import com.intellij.psi.tree.IElementType
import kotlin.math.min


enum class CjDocKind {
    Attr {
        override val prefix: String = ""
        override val infix: String = ""

        override fun removeDecoration(lines: Sequence<CjDocLine>): Sequence<CjDocLine> =
            removeAttrDecoration(lines)
    },

    InnerBlock {
        override val prefix: String = "/*!"
        override val infix: String = "*"

        override fun removeDecoration(lines: Sequence<CjDocLine>): Sequence<CjDocLine> =
            removeBlockDecoration(lines)
    },

    OuterBlock {
        override val prefix: String = "/**"
        override val infix: String = "*"

        override fun removeDecoration(lines: Sequence<CjDocLine>): Sequence<CjDocLine> =
            removeBlockDecoration(lines)
    },

    InnerEol {
        override val infix: String = "//!"
        override val prefix: String = infix

        override fun removeDecoration(lines: Sequence<CjDocLine>): Sequence<CjDocLine> =
            removeEolDecoration(lines)
    },

    OuterEol {
        override val infix: String = "///"
        override val prefix: String = infix

        override fun removeDecoration(lines: Sequence<CjDocLine>): Sequence<CjDocLine> =
            removeEolDecoration(lines)
    };

    abstract val prefix: String
    abstract val infix: String
    val suffix: String get() = if (isBlock) "*/" else ""

    val isBlock: Boolean
        get() = this == InnerBlock || this == OuterBlock


    protected abstract fun removeDecoration(lines: Sequence<CjDocLine>): Sequence<CjDocLine>

    fun  removeDecorationToLines(text: CharSequence): Sequence<CjDocLine> =
        removeDecoration(CjDocLine.splitLines(text))

    fun removeDecoration(text: CharSequence): Sequence<CharSequence> =
        removeDecorationToLines(text).mapNotNull { if (it.isRemoved) null else it.content }

    protected fun removeEolDecoration(decoratedLines: Sequence<CjDocLine>, infix: String = this.infix): Sequence<CjDocLine> {
        return removeCommonIndent(decoratedLines.map { it.trimStart().removePrefix(infix) })
    }

    protected fun removeCommonIndent(decoratedLines: Sequence<CjDocLine>): Sequence<CjDocLine> {
        val lines = decoratedLines.toList()

        val minIndent = lines.fold(Int.MAX_VALUE) { minIndent, line ->
            if (line.isRemoved || line.content.isBlank()) {
                minIndent
            } else {
                min(minIndent, line.countStartWhitespace())
            }
        }

        return lines.asSequence().map { line ->
            line.substring(min(minIndent, line.contentLength))
        }
    }

    protected fun removeBlockDecoration(lines: Sequence<CjDocLine>): Sequence<CjDocLine> {
        val lines2 = lines.toMutableList()
        if (lines2.isEmpty()) return emptySequence()
        lines2[0] = lines2[0].removePrefix(prefix)
        lines2[lines2.lastIndex] = lines2[lines2.lastIndex].removeSuffix(suffix)
        return removeAttrDecoration(lines2.asSequence())
    }

    protected fun removeAttrDecoration(linesSequence: Sequence<CjDocLine>): Sequence<CjDocLine> {

        fun doVerticalTrim(lines: List<CjDocLine>): List<CjDocLine> {
            var start = 0
            var end = lines.size


            if (lines[0].content.all { it == '*' }) {
                start++
            }

            while (start < end && lines[start].content.isBlank()) {
                start++
            }

            if (end > start && (lines[end - 1].content.isEmpty() || lines[end - 1].content.substring(1).all { it == '*' })) {
                end--
            }

            while (end > start && lines[end - 1].content.isBlank()) {
                end--
            }

            val lines2 = lines.toMutableList()

            for (i in 0 until start) {
                lines2[i] = lines2[i].markRemoved()
            }

            for (i in end until lines.size) {
                lines2[i] = lines2[i].markRemoved()
            }

            return lines2
        }


        fun calculateCommonIndentBeforeAsterisk(lines: List<CjDocLine>): Int? {
            var indent = Int.MAX_VALUE
            var first = true

            for (line in lines) {
                if (line.isRemoved) continue
                for ((j, c) in line.content.withIndex()) {
                    if (j > indent || !"* \t".contains(c)) {
                        return null
                    }
                    if (c == '*') {
                        if (first) {
                            indent = j
                            first = false
                        } else if (indent != j) {
                            return null
                        }
                        break
                    }
                }
                if (indent >= line.contentLength) {
                    return null
                }
            }
            return indent
        }

        val lines = linesSequence.toList()
        if (lines.size <= 1) return lines.asSequence()
        val lines2 = doVerticalTrim(lines)
        val indent = calculateCommonIndentBeforeAsterisk(lines2)
        return removeCommonIndent(if (indent != null) {
            lines2.map { it.substring(min(indent + 1, it.contentLength)) }
        } else {
            lines2
        }.asSequence())
    }

    companion object {

        fun of(tokenType: IElementType): CjDocKind = when (tokenType) {
            INNER_BLOCK_DOC_COMMENT -> InnerBlock
            OUTER_BLOCK_DOC_COMMENT -> OuterBlock
            INNER_EOL_DOC_COMMENT -> InnerEol
            OUTER_EOL_DOC_COMMENT -> OuterEol
            else -> throw IllegalArgumentException("unsupported token type")
        }
    }
}
