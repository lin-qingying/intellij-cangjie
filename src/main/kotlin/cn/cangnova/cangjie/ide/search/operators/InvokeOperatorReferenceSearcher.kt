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

package cn.cangnova.cangjie.ide.search.operators

import cn.cangnova.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions
import cn.cangnova.cangjie.psi.CjCallExpression
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.psi.psiUtil.isExtensionDeclaration
import cn.cangnova.cangjie.references.CjInvokeFunctionReference
import cn.cangnova.cangjie.utils.firstIsInstance
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor

class InvokeOperatorReferenceSearcher(
    targetFunction: PsiElement,
    searchScope: SearchScope,
    consumer: Processor<in PsiReference>,
    optimizer: SearchRequestCollector,
    options: CangJieReferencesSearchOptions
) : OperatorReferenceSearcher<CjCallExpression>(targetFunction, searchScope, consumer, optimizer, options, wordsToSearch = emptyList()) {

    override fun processPossibleReceiverExpression(expression: CjExpression) {
        val callExpression = expression.parent as? CjCallExpression ?: return
        processReferenceElement(callExpression)
    }

    override fun isReferenceToCheck(ref: PsiReference): Boolean  = ref is CjInvokeFunctionReference

    override fun extractReference(element: CjElement): PsiReference? {
        val callExpression = element as? CjCallExpression ?: return null


        return callExpression.references.firstIsInstance<CjInvokeFunctionReference>()
    }

}
