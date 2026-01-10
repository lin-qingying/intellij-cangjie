/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.psi.CjNodeTypes.INIT_BLOCK
import com.intellij.openapi.diagnostic.Logger

class CjInitBlockExpression(text: CharSequence?) : CjBlockExpression(INIT_BLOCK, text) {
    companion object {
        val LOG = Logger.getInstance(CjInitBlockExpression::class.java)
    }

    fun replaceImplicitDelegationCallWithExplicit(isThis: Boolean): CjConstructorDelegationCall {
        val psiFactory = CjPsiFactory(project)
        val current = getDelegationCall()

        assert(current.isImplicit) { "Method should not be called with explicit delegation call: " + text }
        current.delete()
//        换行
        val whiteSpace = addAfter(psiFactory.createNewLine(), lBrace)

        val delegationName = if (isThis) "this" else "super"

        return addAfter(
            psiFactory.creareDelegatedSuperTypeEntry("$delegationName()"),
            whiteSpace.nextSibling,
        ) as CjConstructorDelegationCall
    }

    fun getDelegationCall(): CjConstructorDelegationCall =
        getDelegationCallOrNull()!!

    fun getDelegationCallOrNull(): CjConstructorDelegationCall? =
        findChildByClass(CjConstructorDelegationCall::class.java)
}
