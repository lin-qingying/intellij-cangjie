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

package cn.cangnova.cangjie.ide.quickfix

import cn.cangnova.cangjie.diagnostics.Diagnostic
import cn.cangnova.cangjie.psi.CjCallExpression
import cn.cangnova.cangjie.psi.CjSimpleNameExpression
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.psi.PsiElement

internal class ImportFix(expression: CjSimpleNameExpression) : AbstractImportFix(expression, MyFactory) {
    override fun elementsToCheckDiagnostics(): Collection<PsiElement> {
        val expression = element ?: return emptyList()
        return listOfNotNull(expression, expression.parent?.takeIf { it is CjCallExpression })
    }

    companion object MyFactory : FactoryWithUnresolvedReferenceQuickFix() {
        override fun areActionsAvailable(diagnostic: Diagnostic): Boolean {
            val expression = expression(diagnostic)
            return expression != null && expression.references.isNotEmpty()
        }

        override fun createImportAction(diagnostic: Diagnostic): ImportFix? =
            expression(diagnostic)?.let(::ImportFix)

        private fun expression(diagnostic: Diagnostic): CjSimpleNameExpression? =
            when (val element = diagnostic.psiElement) {
                is CjSimpleNameExpression -> element
                is CjCallExpression -> element.calleeExpression
                else -> null
            } as? CjSimpleNameExpression
    }
}
