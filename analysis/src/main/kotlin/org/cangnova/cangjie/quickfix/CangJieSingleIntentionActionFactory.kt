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

package org.cangnova.cangjie.quickfix
import org.cangnova.cangjie.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement


abstract class CangJieSingleIntentionActionFactory : CangJieIntentionActionsFactory() {
    protected abstract fun createAction(diagnostic: Diagnostic): IntentionAction?

    final override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> =
        listOfNotNull(createAction(diagnostic))

    companion object {
        inline fun <reified PSI : PsiElement> createFromQuickFixesPsiBasedFactory(
            psiBasedFactory: QuickFixesPsiBasedFactory<PSI>
        ): CangJieSingleIntentionActionFactory = object : CangJieSingleIntentionActionFactory() {
            override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                val factories = psiBasedFactory.createQuickFix(diagnostic.psiElement as PSI)
                return when (factories.size) {
                    0 -> null
                    1 -> factories.single()
                    else -> error("To convert QuickFixesPsiBasedFactory to CangJieSingleIntentionActionFactory, it should always return one or zero quickfixes")
                }
            }
        }
    }
}
