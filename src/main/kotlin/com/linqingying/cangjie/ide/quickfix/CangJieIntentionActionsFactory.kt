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

package com.linqingying.cangjie.ide.quickfix

import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.psi.CjCodeFragment
import com.intellij.codeInsight.intention.IntentionAction


abstract class CangJieIntentionActionsFactory : QuickFixFactory {
    protected open fun isApplicableForCodeFragment(): Boolean = false

    protected abstract fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction>

    open fun areActionsAvailable(diagnostic: Diagnostic): Boolean = createActions(diagnostic).isNotEmpty()

    protected open fun doCreateActionsForAllProblems(
        sameTypeDiagnostics: Collection<Diagnostic>
    ): List<IntentionAction> = emptyList()

    fun createActions(diagnostic: Diagnostic): List<IntentionAction> = createActions(listOf(diagnostic), false)

    fun createActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<IntentionAction> =
        createActions(sameTypeDiagnostics, true)

    private fun createActions(sameTypeDiagnostics: Collection<Diagnostic>, createForAll: Boolean): List<IntentionAction> {
        if (sameTypeDiagnostics.isEmpty()) return emptyList()
        val first = sameTypeDiagnostics.first()

        if (first.psiElement.containingFile is CjCodeFragment && !isApplicableForCodeFragment()) {
            return emptyList()
        }

        if (sameTypeDiagnostics.size > 1 && createForAll) {
            assert(sameTypeDiagnostics.all { it.psiElement == first.psiElement && it.factory == first.factory }) {
                "It's expected to be the list of diagnostics of same type and for same element"
            }

            return doCreateActionsForAllProblems(sameTypeDiagnostics)
        }

        return sameTypeDiagnostics.flatMapTo(arrayListOf()) { doCreateActions(it) }
    }
}
