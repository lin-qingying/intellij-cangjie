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

package com.linqingying.cangjie.references

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjImportDirective
import com.linqingying.cangjie.psi.CjNameReferenceExpression
import com.linqingying.cangjie.psi.CjPackageDirective
import com.linqingying.cangjie.psi.CjUserType
import com.linqingying.cangjie.psi.psiUtil.parents
import com.intellij.psi.PsiReference


class CangJieReferenceContributor : CangJieReferenceProviderContributor {
    override fun registerReferenceProviders(registrar: CangJiePsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(factory = ::CjSimpleNameReference)
            registerProvider(factory = ::CjInvokeFunctionReference)
            registerProvider(factory = ::CjConstructorDelegationReference)
            registerProvider(factory = ::CjArrayAccessReference)
            registerProvider(factory = ::CangJieCDocReference)

//            registerProvider(factory = ::CjPatternEnumReference)
//
            registerMultiProvider<CjNameReferenceExpression> { nameReferenceExpression ->

                if (nameReferenceExpression.referencedNameElementType != CjTokens.IDENTIFIER) {
                    return@registerMultiProvider PsiReference.EMPTY_ARRAY
                }
                if (nameReferenceExpression.parents.any { it is CjImportDirective || it is CjPackageDirective || it is CjUserType }) {
                    return@registerMultiProvider PsiReference.EMPTY_ARRAY
                }
                when (nameReferenceExpression.readWriteAccess(useResolveForReadWrite = false)) {
                    ReferenceAccess.READ ->
                        arrayOf(CangJieSyntheticPropertyAccessorReference(nameReferenceExpression, getter = true))

                    ReferenceAccess.WRITE -> arrayOf(
                        CangJieSyntheticPropertyAccessorReference(
                            nameReferenceExpression,
                            getter = false
                        )
                    )

                    ReferenceAccess.READ_WRITE -> arrayOf(
                        CangJieSyntheticPropertyAccessorReference(nameReferenceExpression, getter = true),
                        CangJieSyntheticPropertyAccessorReference(nameReferenceExpression, getter = false)
                    )
                }
            }
        }
    }
}
