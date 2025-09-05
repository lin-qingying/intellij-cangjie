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

package org.cangnova.cangjie.ide.refactoring.introduce

import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.ElementKind
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

interface CangJieIntroduceVariableService {
    fun findElement(
        file: CjFile,
        startOffset: Int,
        endOffset: Int,
        failOnNoExpression: Boolean,
        elementKind: ElementKind
    ): PsiElement?

    fun getContainersForExpression(expression: CjExpression): List<CangJieIntroduceVariableHelper.Containers>
    fun findOccurrences(expression: CjExpression, occurrenceContainer: CjElement): List<CjExpression>

    fun doRefactoringWithContainer(
        editor: Editor?,
        expressionToExtract: CjExpression,
        container: CjElement,
        occurrencesToReplace: List<CjExpression>?,
    )

    fun hasUnitType(element: CjExpression): Boolean


    object DEFAULT : CangJieIntroduceVariableService {
        override fun findElement(
            file: CjFile,
            startOffset: Int,
            endOffset: Int,
            failOnNoExpression: Boolean,
            elementKind: ElementKind
        ): PsiElement? {
            return null
        }

        override fun getContainersForExpression(expression: CjExpression): List<CangJieIntroduceVariableHelper.Containers> {
            return emptyList()
        }

        override fun findOccurrences(
            expression: CjExpression,
            occurrenceContainer: CjElement
        ): List<CjExpression> {
            return emptyList()
        }

        override fun doRefactoringWithContainer(
            editor: Editor?,
            expressionToExtract: CjExpression,
            container: CjElement,
            occurrencesToReplace: List<CjExpression>?
        ) {

        }

        override fun hasUnitType(element: CjExpression): Boolean {
            return false
        }

    }
}