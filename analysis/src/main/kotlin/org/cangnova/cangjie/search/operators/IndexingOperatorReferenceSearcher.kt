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

package org.cangnova.cangjie.search.operators

import org.cangnova.cangjie.search.ideExtensions.CangJieReferencesSearchOptions
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.references.CjArrayAccessReference
import org.cangnova.cangjie.references.readWriteAccess
import org.cangnova.cangjie.utils.firstIsInstance
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor
import org.cangnova.cangjie.psi.CjArrayAccessExpression


class IndexingOperatorReferenceSearcher(
    targetFunction: PsiElement,
    searchScope: SearchScope,
    consumer: Processor<in PsiReference>,
    optimizer: SearchRequestCollector,
    options: CangJieReferencesSearchOptions,
    private val isSet: Boolean
) : OperatorReferenceSearcher<CjArrayAccessExpression>(
    targetFunction,
    searchScope,
    consumer,
    optimizer,
    options,
    wordsToSearch = listOf("[")
) {

    override fun processPossibleReceiverExpression(expression: CjExpression) {
        val accessExpression = expression.parent as? CjArrayAccessExpression ?: return
        if (expression != accessExpression.arrayExpression) return
        if (!checkAccessExpression(accessExpression)) return
        processReferenceElement(accessExpression)
    }

    override fun isReferenceToCheck(ref: PsiReference) =
        ref is CjArrayAccessReference && checkAccessExpression(ref.element)

    override fun extractReference(element: CjElement): PsiReference? {
        val accessExpression = element as? CjArrayAccessExpression ?: return null
        if (!checkAccessExpression(accessExpression)) return null
        return accessExpression.references.firstIsInstance<CjArrayAccessReference>()
    }

    private fun checkAccessExpression(accessExpression: CjArrayAccessExpression): Boolean {
        val readWriteAccess = accessExpression.readWriteAccess(useResolveForReadWrite = false)
        return if (isSet) readWriteAccess.isWrite else readWriteAccess.isRead
    }
}
