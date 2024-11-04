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
