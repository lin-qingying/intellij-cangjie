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

package com.linqingying.cangjie.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.*

abstract class AbstractDiagnostic<E : PsiElement>(
    override val psiElement: E,
    override val factory: DiagnosticFactoryWithPsiElement<E, *>,
    override val severity: Severity
) :
    ParametrizedDiagnostic<E> {


    override val psiFile: PsiFile
        get() = psiElement.containingFile



    override val textRanges: List<TextRange>
        get() = factory.getTextRanges(this)

    override val isValid: Boolean
        get() {
            return factory.isValid(this)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as AbstractDiagnostic<*>
        return psiElement == that.psiElement && factory == that.factory && severity == that.severity
    }

    override fun hashCode(): Int {
        return Objects.hash(psiElement, factory, severity)
    }
}
