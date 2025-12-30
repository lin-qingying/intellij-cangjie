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

package org.cangnova.cangjie.search.usages

import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.utils.collapseSpaces
import com.intellij.codeInsight.highlighting.HighlightUsagesDescriptionLocation
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.ElementDescriptionLocation
import com.intellij.psi.ElementDescriptionProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.RefactoringDescriptionLocation
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewShortNameLocation
import com.intellij.usageView.UsageViewTypeLocation
import org.cangnova.cangjie.messages.CangJieSearchBundle
import org.cangnova.cangjie.name.SpecialNames.NO_NAME_PROVIDED


open class CangJieElementDescriptionProviderBase : ElementDescriptionProvider {
    private tailrec fun CjNamedDeclaration.parentForFqName(): CjNamedDeclaration? {
        val parent = getStrictParentOfType<CjNamedDeclaration>() ?: return null
        if (parent is CjProperty && parent.isLocal) return parent.parentForFqName()
        return parent
    }

    private fun CjNamedDeclaration.name() = nameAsName ?: NO_NAME_PROVIDED

    private fun CjNamedDeclaration.fqName(): FqNameUnsafe {
        containingTypeStatement?.let {

            return FqNameUnsafe("${it.name()}.${name()}")
        }

        val internalSegments = generateSequence(this) { it.parentForFqName() }
            .map { it.name ?: "<no name provided>" }
            .toList()
            .asReversed()
        val packageSegments = containingCjFile.packageFqName.pathSegments()
        return FqNameUnsafe((packageSegments + internalSegments).joinToString("."))
    }

    private fun CjTypeReference.renderShort(): String {
        return accept(
            object : CjVisitor<String, Unit>() {
                private val visitor get() = this

                override fun visitTypeReference(typeReference: CjTypeReference, data: Unit): String {
                    val typeText = typeReference.typeElement?.accept(this, data) ?: "???"
                    return if (!typeReference.containingCjFile.isCompiled && typeReference.hasParentheses()) "($typeText)" else typeText
                }


//                override fun visitDynamicType(type: CjDynamicType, data: Unit) = type.text

                override fun visitFunctionType(type: CjFunctionType, data: Unit): String {
                    return buildString {
                        type.receiverTypeReference?.let { append(it.accept(visitor, data)).append('.') }
                        type.parameters.joinTo(this, prefix = "(", postfix = ")") { it.accept(visitor, data).toString() }
                        append(" -> ")
                        append(type.returnTypeReference?.accept(visitor, data) ?: "???")
                    }
                }

                override fun visitOptionType(nullableType: CjOptionType, data: Unit): String {
                    val innerTypeText = nullableType.getInnerType()?.accept(this, data) ?: return "???"
                    return "$innerTypeText?"
                }

//                override fun visitSelfType(type: CjSelfType, data: Unit) = type.text

                override fun visitUserType(type: CjUserType, data: Unit): String {
                    return buildString {
                        append(type.referencedName ?: "???")

                        val arguments = type.typeArguments
                        if (arguments.isNotEmpty()) {
                            arguments.joinTo(this, prefix = "<", postfix = ">") {
                                it.typeReference?.accept(visitor, data) ?: it.text
                            }
                        }
                    }
                }

                override fun visitParameter(parameter: CjParameter, data: Unit) =
                    parameter.typeReference?.accept(this, data) ?: "???"
            },
            Unit
        ) ?: ""
    }


    protected open val PsiElement.isRenameCangJiePropertyProcessor get() = false

    override fun getElementDescription(element: PsiElement, location: ElementDescriptionLocation): String? {
        val shouldUnwrap = location !is UsageViewShortNameLocation && location !is UsageViewLongNameLocation
        val targetElement = if (shouldUnwrap) element ?: element else element

        fun elementKind() = when (targetElement) {
            is CjInterface -> CangJieSearchBundle.message("find.usages.interface")
            is CjClass -> CangJieSearchBundle.message("find.usages.class")
            is CjStruct -> CangJieSearchBundle.message("find.usages.struct")
            is CjNamedFunction -> CangJieSearchBundle.message("find.usages.function")
            is CjPropertyAccessor -> CangJieSearchBundle.message(
                "find.usages.for.property",
                (if (targetElement.isGetter)
                    CangJieSearchBundle.message("find.usages.getter")
                else
                    CangJieSearchBundle.message("find.usages.setter"))
            ) + " "

            is CjFunctionLiteral -> CangJieSearchBundle.message("find.usages.lambda")
            is CjPrimaryConstructor, is CjSecondaryConstructor -> CangJieSearchBundle.message("find.usages.constructor")
            is CjProperty -> if (targetElement.isLocal)
                CangJieSearchBundle.message("find.usages.variable")
            else
                CangJieSearchBundle.message("find.usages.property")

            is CjTypeParameter -> CangJieSearchBundle.message("find.usages.type.parameter")
            is CjParameter -> CangJieSearchBundle.message("find.usages.parameter")
            is CjTypeAlias -> CangJieSearchBundle.message("find.usages.type.alias")

            is CjImportAlias -> CangJieSearchBundle.message("find.usages.import.alias")

            else -> {

                when {

                    targetElement.isRenameCangJiePropertyProcessor -> CangJieSearchBundle.message("find.usages.property.accessor")
                    else -> null
                }
            }
        }

        val namedElement = if (targetElement is CjPropertyAccessor) {
            targetElement.parent as? CjProperty
        } else targetElement as? PsiNamedElement

        if (namedElement == null) {
            return if (targetElement is CjElement) "'" + StringUtil.shortenTextWithEllipsis(
                targetElement.text.collapseSpaces(),
                53,
                0
            ) + "'" else null
        }

        if (namedElement.language != CangJieLanguage) return null

        return when (location) {
            is UsageViewTypeLocation -> elementKind()
            is UsageViewShortNameLocation, is UsageViewLongNameLocation -> namedElement.name
            is RefactoringDescriptionLocation -> {
                val kind = elementKind() ?: return null
                if (namedElement !is CjNamedDeclaration) return null
                val renderFqName = location.includeParent() &&
                        namedElement !is CjTypeParameter &&
                        namedElement !is CjParameter &&
                        namedElement !is CjConstructor<*>

                @Suppress("HardCodedStringLiteral")
                val desc = when (namedElement) {
                    is CjFunction -> {
                        val baseText = buildString {
                            append(namedElement.name ?: "")
                            namedElement.valueParameters.joinTo(this, prefix = "(", postfix = ")") {
                                (if (it.isVarArg) "vararg " else "") + (it.typeReference?.renderShort() ?: "")
                            }
                        }
                        val parentFqName = if (renderFqName) namedElement.fqName().parent() else null
                        if (parentFqName?.isRoot != false) baseText else "${parentFqName.asString()}.$baseText"
                    }

                    else -> (if (renderFqName) namedElement.fqName().asString() else namedElement.name) ?: ""
                }

                "$kind ${CommonRefactoringUtil.htmlEmphasize(desc)}"
            }

            is HighlightUsagesDescriptionLocation -> {
                val kind = elementKind() ?: return null
                if (namedElement !is CjNamedDeclaration) return null
                "$kind ${namedElement.name}"
            }

            else -> null
        }
    }
}

class CangJieElementDescriptionProvider : CangJieElementDescriptionProviderBase()
