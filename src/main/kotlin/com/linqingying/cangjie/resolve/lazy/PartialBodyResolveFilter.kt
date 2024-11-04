package com.linqingying.cangjie.resolve.lazy

import com.linqingying.cangjie.psi.CjBlockExpression
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjExpression

import com.linqingying.cangjie.psi.psiUtil.siblings
import com.linqingying.cangjie.resolve.StatementFilter
import com.linqingying.cangjie.psi.psiUtil.parentsWithSelf
import com.intellij.psi.PsiElement

class PartialBodyResolveFilter(
    elementsToResolve: Collection<CjElement>,
    private val declaration: CjDeclaration,
    forCompletion: Boolean
) : StatementFilter(){

    private enum class MarkLevel {
        NONE,
        TAKE,
        NEED_REFERENCE_RESOLVE,
        NEED_COMPLETION
    }

    private inner class StatementMarks {
        private val statementMarks = HashMap<CjExpression, MarkLevel>()
        private val blockLevels = HashMap<CjBlockExpression, MarkLevel>()

        fun mark(element: PsiElement, level: MarkLevel) {
            var e = element
            while (e != declaration) {
                if (e.isStatement()) {
                    markStatement(e as CjExpression, level)
                }
                e = e.parent!!
            }
        }

        private fun markStatement(statement: CjExpression, level: MarkLevel) {
            val currentLevel = statementMark(statement)
            if (currentLevel < level) {
                statementMarks[statement] = level

                val block = statement.parent as CjBlockExpression
                val currentBlockLevel = blockLevels[block] ?: MarkLevel.NONE
                if (currentBlockLevel < level) {
                    blockLevels[block] = level
                }
            }
        }

        fun statementMark(statement: CjExpression): MarkLevel = statementMarks[statement] ?: MarkLevel.NONE

        fun allMarkedStatements(): Collection<CjExpression> = statementMarks.keys

        fun lastMarkedStatement(block: CjBlockExpression, minLevel: MarkLevel): CjExpression? {
            val level = blockLevels[block] ?: MarkLevel.NONE
            if (level < minLevel) return null // optimization
            return block.lastChild.siblings(forward = false)
                .filterIsInstance<CjExpression>()
                .first { statementMark(it) >= minLevel }
        }
    }
    val allStatementsToResolve: Collection<CjExpression>
        get() = statementMarks.allMarkedStatements()
    private val statementMarks = StatementMarks()

    companion object{
        fun findStatementToResolve(element: CjElement, declaration: CjDeclaration): CjExpression? {
            return element.parentsWithSelf.takeWhile {
                it != declaration
            }.firstOrNull {
                it.isStatement()
            } as CjExpression?
        }

        private fun PsiElement.isStatement() = this is CjExpression && parent is CjBlockExpression

    }
}
