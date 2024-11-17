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

package com.linqingying.cangjie.ide.projectView

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.psi.*
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement


class CjDeclarationTreeNode private constructor(
    project: Project?,
    val declaration: CjDeclaration?,
    viewSettings: ViewSettings?
) : AbstractPsiBasedNode<CjDeclaration >(project, declaration!!, viewSettings) {
    companion object {
        private val CLASS_INITIALIZER = "<" + CangJieBundle.message("project.view.class.initializer") + ">"
        private val EXPRESSION = "<" + CangJieBundle.message("project.view.expression") + ">"
        private val ERROR_NAME = "<" + CangJieBundle.message("project.view.class.error.name") + ">"
        private fun String?.orErrorName() = if (!isNullOrBlank()) this else ERROR_NAME
        fun create(project: Project?, ktDeclaration: CjDeclaration, viewSettings: ViewSettings): CjDeclarationTreeNode =
            CjDeclarationTreeNode(project, ktDeclaration, viewSettings)

        @NlsSafe
        fun tryGetRepresentableText(declaration: CjDeclaration, renderArguments: Boolean = true): String {
            fun CjVariable.presentableText() = buildString {
                append(name.orErrorName())
                typeReference?.text?.let { reference ->
                    append(": ")
                    append(reference)
                }
            }

            fun CjProperty.presentableText() = buildString {
                append(name.orErrorName())
                typeReference?.text?.let { reference ->
                    append(": ")
                    append(reference)
                }
            }

            fun CjFunction.presentableText() = buildString {
                receiverTypeReference?.text?.let { receiverReference ->
                    append(receiverReference)
                    append('.')
                }
                append(name.orErrorName())
                if (renderArguments) {
                    append("(")
                    val valueParameters = valueParameters
                    valueParameters.forEachIndexed { index, parameter ->
                        parameter.name?.let { parameterName ->
                            append(parameterName)
                            append(": ")
                            Unit
                        }
                        parameter.typeReference?.text?.let { typeReference ->
                            append(typeReference)
                        }
                        if (index != valueParameters.size - 1) {
                            append(", ")
                        }
                    }
                    append(")")
                }

                typeReference?.text?.let { returnTypeReference ->
                    append(": ")
                    append(returnTypeReference)
                }
            }



            return when (declaration) {
                is CjVariable -> declaration.presentableText()
                is CjProperty -> declaration.presentableText()
                is CjFunction -> declaration.presentableText()


                is CjAnonymousInitializer -> CLASS_INITIALIZER

                else -> declaration.name.orErrorName()
            }
        }
    }

    override fun extractPsiFromValue(): PsiElement? = value
    override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> = emptyList()

    override fun updateImpl(data: PresentationData) {
        val declaration = value ?: return
        data.presentableText = tryGetRepresentableText(declaration)
    }
}
