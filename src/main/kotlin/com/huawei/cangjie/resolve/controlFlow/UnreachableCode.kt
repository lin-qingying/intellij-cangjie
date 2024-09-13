package com.huawei.cangjie.resolve.controlFlow

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjPsiUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import java.util.ArrayList
import java.util.HashSet


interface UnreachableCode {
    val elements: Set<CjElement>
    val reachableElements: Set<CjElement>
    val unreachableElements: Set<CjElement>

    companion object {
        fun getUnreachableTextRanges(
            element: CjElement,
            reachableElements: Set<CjElement>,
            unreachableElements: Set<CjElement>
        ): List<TextRange> {
            return if (element.hasChildrenInSet(reachableElements)) {
                with(
                    element.getLeavesOrReachableChildren(reachableElements, unreachableElements)
                        .removeReachableElementsWithMeaninglessSiblings(reachableElements).mergeAdjacentTextRanges()
                ) {
                    if (isNotEmpty()) this
                    // Specific case like condition in when:
                    // element is dead but its only child is alive and has the same text range
                    else listOf(element.textRange.endOffset.let { TextRange(it, it) })
                }
            } else {
                listOf(element.textRange!!)
            }
        }

        private fun CjElement.hasChildrenInSet(set: Set<CjElement>): Boolean =
            PsiTreeUtil.collectElements(this) { it != this }.any { it in set }

        private fun CjElement.getLeavesOrReachableChildren(
            reachableElements: Set<CjElement>,
            unreachableElements: Set<CjElement>
        ): List<PsiElement> {
            val children = ArrayList<PsiElement>()
            acceptChildren(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    val isReachable =
                        element is CjElement && reachableElements.contains(element) && !element.hasChildrenInSet(unreachableElements)
                    if (isReachable || element.children.isEmpty()) {
                        children.add(element)
                    } else {
                        element.acceptChildren(this)
                    }
                }
            })
            return children
        }

        private fun List<PsiElement>.removeReachableElementsWithMeaninglessSiblings(reachableElements: Set<CjElement>): List<PsiElement> {
            fun PsiElement.isMeaningless() = this is PsiWhiteSpace
                    || this.node?.elementType == CjTokens.COMMA
                    || this is PsiComment

            val childrenToRemove = HashSet<PsiElement>()
            fun collectSiblingsIfMeaningless(elementIndex: Int, direction: Int) {
                val index = elementIndex + direction
                if (index !in 0 until size) return

                val element = this[index]
                if (element.isMeaningless()) {
                    childrenToRemove.add(element)
                    collectSiblingsIfMeaningless(index, direction)
                }
            }
            for ((index, element) in this.withIndex()) {
                if (reachableElements.contains(element)) {
                    childrenToRemove.add(element)
                    collectSiblingsIfMeaningless(index, -1)
                    collectSiblingsIfMeaningless(index, 1)
                }
            }
            return this.filter { it !in childrenToRemove }
        }


        private fun List<PsiElement>.mergeAdjacentTextRanges(): List<TextRange> {
            val result = ArrayList<TextRange>()
            val lastRange = fold(null as TextRange?) { currentTextRange, element ->

                val elementRange = element.textRange!!
                when {
                    currentTextRange == null -> {
                        elementRange
                    }
                    currentTextRange.endOffset == elementRange.startOffset -> {
                        currentTextRange.union(elementRange)
                    }
                    else -> {
                        result.add(currentTextRange)
                        elementRange
                    }
                }
            }
            if (lastRange != null) {
                result.add(lastRange)
            }
            return result
        }
    }

}

class UnreachableCodeImpl(
    override val reachableElements: Set<CjElement>,
    override val unreachableElements: Set<CjElement>
) : UnreachableCode {

    // This is needed in order to highlight only '1 < 2' and not '1', '<' and '2' as well
    override val elements: Set<CjElement> = CjPsiUtil.findRootExpressions(unreachableElements)

}
