/*
 * Copyright 2024 LinQingYing. and contributors.
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
