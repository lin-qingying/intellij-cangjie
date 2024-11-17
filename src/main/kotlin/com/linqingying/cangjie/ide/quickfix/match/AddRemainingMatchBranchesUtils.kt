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

package com.linqingying.cangjie.ide.quickfix.match

import com.intellij.psi.PsiWhiteSpace
import com.linqingying.cangjie.diagnostics.MatchMissingCase
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjMatchEntry
import com.linqingying.cangjie.psi.CjMatchExpression
import com.linqingying.cangjie.psi.CjPsiFactory
import com.linqingying.cangjie.renderer.render
import com.linqingying.cangjie.types.expressions.match.Pattern

object AddRemainingMatchBranchesUtils {
    open fun createNewMatchEntry(
        psiFactory: CjPsiFactory,
        context: CjElement,
        patterns: List<Pattern>
    ): List<CjMatchEntry> =
        psiFactory.createMatch(patterns, context).entries

    fun generateMatchPatternBranches(element: CjMatchExpression, patterns: List<Pattern>) {
        val psiFactory = CjPsiFactory(element.project)
        val entrys = createNewMatchEntry(psiFactory, element, patterns)

        val matchCloseBrace = element.closeBrace
        entrys.forEach {
            element.addBefore(it, matchCloseBrace)
        }

    }

    fun generateMatchBranches(element: CjMatchExpression, missingCases: List<MatchMissingCase>) {
        val psiFactory = CjPsiFactory(element.project)
        val matchCloseBrace = element.closeBrace ?: run {
            val craftingMaterials = psiFactory.createExpression("match(1){}") as CjMatchExpression
            if (element.rightParenthesis == null) {
                element.addAfter(
                    craftingMaterials.rightParenthesis!!,
                    element.subjectExpression
                        ?: throw AssertionError("caller should have checked the presence of subject expression.")
                )
            }
            if (element.openBrace == null) {
                element.addAfter(craftingMaterials.openBrace!!, element.rightParenthesis!!)
            }
            element.addAfter(craftingMaterials.closeBrace!!, element.entries.lastOrNull() ?: element.openBrace!!)
            element.closeBrace!!
        }
        val elseBranch = element.entries.find { it.isElse }
        (matchCloseBrace.prevSibling as? PsiWhiteSpace)?.replace(psiFactory.createNewLine())
        for (case in missingCases) {
            val branchConditionText = when (case) {
                MatchMissingCase.Unknown,
                MatchMissingCase.NullIsMissing,
                is MatchMissingCase.BooleanIsMissing,
                is MatchMissingCase.ConditionTypeIsExpect -> case.branchConditionText

                is MatchMissingCase.IsTypeCheckIsMissing ->
                    if (case.isSingleton) {
                        ""
                    } else {
                        "is "
                    } + case.classId.asSingleFqName().render()

                is MatchMissingCase.EnumCheckIsMissing -> case.callableId.asSingleFqName().render()
                is MatchMissingCase.OtherCheckIsMissing -> TODO()
                is MatchMissingCase.TupleCheckIsMissing -> TODO()
            }
            val entry = psiFactory.createMatchEntry("case $branchConditionText => TODO()")
            if (elseBranch != null) {
                element.addBefore(entry, elseBranch)
            } else {
                element.addBefore(entry, matchCloseBrace)
            }
        }
    }
}
