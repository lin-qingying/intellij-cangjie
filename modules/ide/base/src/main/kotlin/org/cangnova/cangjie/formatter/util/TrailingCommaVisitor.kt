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


package org.cangnova.cangjie.formatter.util

import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjTreeVisitorUnit
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement


abstract class TrailingCommaVisitor : CjTreeVisitorUnit() {
    override fun visitCjElement(element: CjElement) {
        super.visitCjElement(element)
        // because CjFunctionLiteral contains CjParameterList
        if (element !is CjFunctionLiteral && element.canAddTrailingComma()) {
            runProcessIfApplicable(element)
        }
    }

    override fun visitElement(element: PsiElement) {
        ProgressIndicatorProvider.checkCanceled()

        if (recursively) super.visitElement(element)
    }

    private fun runProcessIfApplicable(element: CjElement) {
        val context = TrailingCommaContext.create(element)
        if (context.state != TrailingCommaState.NOT_APPLICABLE) {
            process(context)
        }
    }


    protected abstract fun process(trailingCommaContext: TrailingCommaContext)

    protected open val recursively: Boolean = true
}
