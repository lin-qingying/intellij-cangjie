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

package com.linqingying.cangjie.utils

import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.SpecialNames
import com.linqingying.cangjie.psi.*

object CangJiePsiDeclarationRenderer {
    fun render(declaration: CjDeclaration): String? =
        when (declaration) {
            is CjTypeStatement ->
                buildString {
//                    if (declaration.isAnnotation()) {
//                        append("annotation ")
//                    }
                    if (declaration.isInterface()) {
                        append("interface")
                    }
                    else {
                        append("class")
                    }

                    append(" ")
                    append(declaration.name)
                    declaration.typeParameterList?.parameters?.let {
                        append("<")
                        for ((index, cjTypeParameter) in it.withIndex()) {
                            if (index != 0) append(", ")
                            append(cjTypeParameter.name)
                        }
                        append(">")
                    }
                    val superTypeListEntries = declaration.superTypeListEntries
                    val superClass = superTypeListEntries.filterIsInstance<CjSuperTypeCallEntry>().firstOrNull()
                    if (superClass != null) {
                        superClass.calleeExpression.constructorReferenceExpression?.getReferencedName()?.let {
                            append(" : ")
                            append(it)
                        }
                    }
                    else if (superTypeListEntries.isNotEmpty()) {
                        superTypeListEntries.first().typeReference?.referenceName()?.let {
                            append(" : ")
                            append(it)
                        }
                    }
                }
            is CjProperty ->
                buildString {
                    if (declaration.isVar) {
                        append("var")
                    }
                    else {
                        append("val")
                    }
                    append(" ")
                    declaration.receiverTypeReference?.let {
                        append(it.referenceName())
                        append(".")
                    }
                    append(declaration.name)
                    declaration.typeReference?.let {
                        append(": ")
                        append(it.referenceName())
                    }
                }
            is CjTypeParameter -> buildString {
                append("<")
                append(declaration.name)
                append(">")
            }
            is CjParameter -> buildString {
                appendCjParameter(declaration)
            }
            is CjConstructor<*> -> buildString {
                append("constructor")
                append(" ")
                append(declaration.name ?: ("`" + SpecialNames.NO_NAME_PROVIDED.asString() + "`"))
                appendValueParameters(declaration)
            }
            is CjNamedFunction -> buildString {

                if (declaration.hasModifier(CjTokens.OPERATOR_KEYWORD)) {
                    append("operator")
                    append(" ")
                }
                append("fun")
                append(" ")
                if (declaration.hasTypeParameterListBeforeFunctionName()) {
                    append("<")
                    for ((index, cjTypeParameter) in declaration.typeParameters.withIndex()) {
                        if (index != 0) append(", ")
                        append(cjTypeParameter.name)
                    }
                    append(">")
                    append(" ")
                }
                declaration.receiverTypeReference?.let {
                    append(it.referenceName())
                    append(".")
                }
                append(declaration.name)
                appendValueParameters(declaration)
                val typeReference = declaration.typeReference
                if (typeReference != null) {
                    append(": ")
                    append(typeReference.referenceName())
                } else if (declaration.hasBlockBody()) {
                    append(": ")
                    append(StandardNames.FqNames.unitUFqName.shortName())
                }
            }
            else -> null
        }

    private fun CjTypeReference.referenceName(): String? {
        val type = typeElement as? CjUserType ?: (typeElement as? CjOptionType)?.getInnerType() as? CjUserType ?: return null
        return buildString {
            append(type.referencedName)
            if (typeElement is CjOptionType) {
                append("?")
            }
            type.typeArgumentList?.arguments?.let {
                append("<")
                for ((index: Int, typeProjection: CjTypeProjection) in it.withIndex()) {
                    if (index != 0) append(", ")
                    when(typeProjection.projectionKind) {


                        else -> {}
                    }
                    append(typeProjection.typeReference?.referenceName() ?: "??")
                }
                append(">")
            }
        }
    }

    private fun StringBuilder.appendCjParameter(cjParameter: CjParameter, withName: Boolean = true) {
        if (cjParameter.isVarArg) append("vararg ")
        if (withName) {
            append(cjParameter.name)
            append(": ")
        }
        append(cjParameter.typeReference?.referenceName() ?: "??")
        if (cjParameter.defaultValue != null) {
            append(" = ...")
        }
    }

    private fun StringBuilder.appendValueParameters(declaration: CjCallableDeclaration) {
        append("(")
        for ((index, cjParameter: CjParameter) in declaration.valueParameters.withIndex()) {
            if (index != 0) append(", ")
            appendCjParameter(cjParameter, withName = false)
        }
        append(")")
    }


}
