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

package com.linqingying.cangjie.ide.quickfix

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.ide.quickfix.RemoveOptionalFix.NullableKind.*
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls


class RemoveOptionalFix(element: CjOptionType, private val typeOfError: NullableKind) :
    CangJiePsiOnlyQuickFixAction<CjOptionType>(element) {
    enum class NullableKind(@Nls val message: String) {
        REDUNDANT(CangJieBundle.message("remove.redundant")),
        SUPERTYPE(CangJieBundle.message("text.remove.question")),
        USELESS(CangJieBundle.message("remove.useless")),
        PROPERTY(CangJieBundle.message("make.not.nullable")),
        VARIABLE(CangJieBundle.message("make.not.nullable")),


        //        TODO 恶搞警告
        REDUNDANT_DOLL("结束套娃的一生"),

    }

    override fun getFamilyName() = CangJieBundle.message("text.remove.question")

    override fun getText() = typeOfError.message

    override fun invoke(project: Project, editor: Editor?, file: CjFile) {
        val element = element ?: return
        val type = element.getInnerType()
            ?: error("No inner type ${element.text}, should have been rejected in createFactory()")
        element.replace(type)
    }

    companion object {

        //        TODO 恶搞警告
        val removeForRedundantDoll = createFactory(REDUNDANT_DOLL)

        val removeForRedundant = createFactory(REDUNDANT)


        val removeForSuperType = createFactory(SUPERTYPE)
        val removeForUseless = createFactory(USELESS)
        val removeForLateInitProperty = createFactory(PROPERTY)
        val removeForLateInitVariable = createFactory(VARIABLE)

        private fun createFactory(typeOfError: NullableKind): QuickFixesPsiBasedFactory<CjElement> {
            return quickFixesPsiBasedFactory { e ->
                when (typeOfError) {
                    REDUNDANT, SUPERTYPE, USELESS, REDUNDANT_DOLL -> {
                        val nullType: CjOptionType? = when (e) {
                            is CjTypeReference -> e.typeElement as? CjOptionType
                            else -> e.getNonStrictParentOfType()
                        }
                        if (nullType?.getInnerType() == null) return@quickFixesPsiBasedFactory emptyList()
                        listOf(RemoveOptionalFix(nullType, typeOfError))
                    }

                    PROPERTY -> {
                        val property = e as? CjProperty ?: return@quickFixesPsiBasedFactory emptyList()
                        val typeReference = property.typeReference ?: return@quickFixesPsiBasedFactory emptyList()
                        val typeElement =
                            typeReference.typeElement as? CjOptionType ?: return@quickFixesPsiBasedFactory emptyList()
                        if (typeElement.getInnerType() == null) return@quickFixesPsiBasedFactory emptyList()
                        listOf(RemoveOptionalFix(typeElement, PROPERTY))
                    }

                    VARIABLE -> {
                        val property = e as? CjVariable ?: return@quickFixesPsiBasedFactory emptyList()
                        val typeReference = property.typeReference ?: return@quickFixesPsiBasedFactory emptyList()
                        val typeElement =
                            typeReference.typeElement as? CjOptionType ?: return@quickFixesPsiBasedFactory emptyList()
                        if (typeElement.getInnerType() == null) return@quickFixesPsiBasedFactory emptyList()
                        listOf(RemoveOptionalFix(typeElement, PROPERTY))
                    }
                }
            }
        }
    }
}
