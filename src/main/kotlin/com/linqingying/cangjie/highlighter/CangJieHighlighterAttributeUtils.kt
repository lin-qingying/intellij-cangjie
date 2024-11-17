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

package com.linqingying.cangjie.highlighter

import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.isAbstract
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.psi.PsiElement

fun textAttributesKeyForCjElement(element: PsiElement): HighlightInfoType? {
    return sequence {
        yield(textAttributesKeyForTypeDeclaration(element))
        yield(textAttributesKeyForCjFunction(element))
        yield(textAttributesKeyForPropertyDeclaration(element))
    }.firstOrNull { it != null }
}

fun textAttributesForCjVariableDeclaration(variable: CjVariable): HighlightInfoType = when {

    variable.isLocal -> CangJieHighlightInfoTypeSemanticNames.LOCAL_VARIABLE
    variable.isTopLevel ->

        CangJieHighlightInfoTypeSemanticNames.PACKAGE_PROPERTY

    else ->

        CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY

}

private fun CjProperty.isCustomPropertyDeclaration(): Boolean {
    return getter?.bodyExpression != null || setter?.bodyExpression != null
}

fun textAttributesForCjPropertyDeclaration(property: CjProperty): HighlightInfoType = when {

    property.isLocal -> CangJieHighlightInfoTypeSemanticNames.LOCAL_VARIABLE

    else -> when {
        property.isCustomPropertyDeclaration() -> CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
        else -> CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY
    }
}

fun textAttributesForCjParameterDeclaration(parameter: CjParameter): HighlightInfoType = when {
    parameter.letOrVarKeyword != null -> CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY
    else -> CangJieHighlightInfoTypeSemanticNames.PARAMETER
}

fun textAttributesKeyForPropertyDeclaration(declaration: PsiElement): HighlightInfoType? = when (declaration) {
    is CjProperty -> textAttributesForCjPropertyDeclaration(declaration)
    is CjParameter -> textAttributesForCjParameterDeclaration(declaration)
    is CjVariable -> textAttributesForCjVariableDeclaration(declaration)

    else -> null
}


fun textAttributesKeyForCjFunction(function: PsiElement): HighlightInfoType? = when (function) {
    is CjFunction -> CangJieHighlightInfoTypeSemanticNames.FUNCTION_DECLARATION
    else -> null
}

fun textAttributesKeyForTypeDeclaration(declaration: PsiElement): HighlightInfoType? = when {
    declaration is CjTypeParameter -> CangJieHighlightInfoTypeSemanticNames.TYPE_PARAMETER
    declaration is CjTypeAlias -> CangJieHighlightInfoTypeSemanticNames.TYPE_ALIAS
    declaration is CjTypeStatement -> when {
//        declaration.isAnnotation() -> CangJieHighlightInfoTypeSemanticNames.ANNOTATION
        else -> textAttributesForClass(declaration)
    }

//    declaration is CjPrimaryConstructor && declaration.containingTypeStatement?.isAnnotation() == true -> CangJieHighlightInfoTypeSemanticNames.ANNOTATION


    else -> null
}

fun textAttributesForClass(cclass: CjTypeStatement): HighlightInfoType = when {
    cclass is CjInterface -> CangJieHighlightInfoTypeSemanticNames.TRAIT
//    cclass.isAnnotation() -> CangJieHighlightInfoTypeSemanticNames.ANNOTATION
    cclass.isEnum() -> CangJieHighlightInfoTypeSemanticNames.ENUM
    cclass is CjEnumEntry -> CangJieHighlightInfoTypeSemanticNames.ENUM_ENTRY
    cclass.isAbstract() -> CangJieHighlightInfoTypeSemanticNames.ABSTRACT_CLASS
    else -> CangJieHighlightInfoTypeSemanticNames.CLASS
}
