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

package com.linqingying.cangjie.ide.search.operators

import com.linqingying.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions
import com.linqingying.cangjie.lexer.CjSingleValueToken
import com.linqingying.cangjie.psi.CjBinaryExpression
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.references.CjSimpleNameReference
import com.linqingying.cangjie.utils.firstIsInstance
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor


class BinaryOperatorReferenceSearcher(
    targetFunction: PsiElement,
    private val operationTokens: List<CjSingleValueToken>,
    searchScope: SearchScope,
    consumer: Processor<in PsiReference>,
    optimizer: SearchRequestCollector,
    options: CangJieReferencesSearchOptions
) : OperatorReferenceSearcher<CjBinaryExpression>(
    targetFunction,
    searchScope,
    consumer,
    optimizer,
    options,
    wordsToSearch = operationTokens.map { it.value }) {

    override fun processPossibleReceiverExpression(expression: CjExpression) {
        val binaryExpression = expression.parent as? CjBinaryExpression ?: return
        if (binaryExpression.operationToken !in operationTokens) return
        if (expression != binaryExpression.left) return
        processReferenceElement(binaryExpression)
    }

    override fun isReferenceToCheck(ref: PsiReference): Boolean {
        if (ref !is CjSimpleNameReference) return false
        val element = ref.element
        if (element.parent !is CjBinaryExpression) return false
        return element.referencedNameElementType in operationTokens
    }

    override fun extractReference(element: CjElement): PsiReference? {
        val binaryExpression = element as? CjBinaryExpression ?: return null
        if (binaryExpression.operationToken !in operationTokens) return null
        return binaryExpression.operationReference.references.firstIsInstance<CjSimpleNameReference>()
    }
}
