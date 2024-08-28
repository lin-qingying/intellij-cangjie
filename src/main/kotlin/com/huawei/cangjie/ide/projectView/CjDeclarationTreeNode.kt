package com.huawei.cangjie.ide.projectView

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.psi.*
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
) : AbstractPsiBasedNode<CjDeclaration?>(project, declaration!!, viewSettings) {
    companion object {
        private val CLASS_INITIALIZER = "<" + CangJieBundle.message("project.view.class.initializer") + ">"
        private val EXPRESSION = "<" + CangJieBundle.message("project.view.expression") + ">"
        private val ERROR_NAME = "<" + CangJieBundle.message("project.view.class.error.name") + ">"
        private fun String?.orErrorName() = if (!isNullOrBlank()) this else ERROR_NAME

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
