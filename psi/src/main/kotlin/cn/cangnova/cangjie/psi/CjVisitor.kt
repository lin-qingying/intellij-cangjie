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

package cn.cangnova.cangjie.psi

import com.intellij.psi.PsiElementVisitor

/**
 * 访问者模式基类，用于遍历CangJie语言的语法树
 */
open class CjVisitor<R, D> : PsiElementVisitor() {

    open fun visitCjElement(element: CjElement, data: D?): R? {
        visitElement(element)
        return null
    }

    open fun visitQuoteInterpolate(expression: CjQuoteInterpolate, data: D?): R? {
        return visitCjElement(expression, data)
    }

    open fun visitScript(script: CjScript, data: D?): R? {
        return visitDeclaration(script, data)
    }

    /**
     * Required for [CjCommonFile] implementation.
     * It is not expected to be used, because [CjCommonFile] should not be used directly.
     */
    @Suppress("DEPRECATION")
    fun visitCjCommonFile(file: CjCommonFile): R? {
        visitFile(file)
        return null
    }

    open fun visitSynchronizedExpression(expression: CjSynchronizedExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitPatternGuard(expression: CjPatternGuard, data: D?): R? {
        return visitCjElement(expression, data)
    }

    open fun visitSafeQualifiedExpression(expression: CjSafeQualifiedExpression, data: D?): R? {
        return visitQualifiedExpression(expression, data)
    }

    open fun visitMacroExpression(expression: CjMacroExpression, data: D?): R? {
        return visitCjElement(expression, data)
    }

    open fun visitQuoteExpression(element: CjQuoteExpression, data: D?): R? {
        return visitExpression(element, data)
    }

    open fun visitCasePattern(element: CjCasePattern, data: D?): R? {
        return visitCjElement(element, data)
    }

    open fun visitPatternByConstant(element: CjConstantPattern, data: D?): R? {
        return visitCasePattern(element, data)
    }

    open fun visitPatternByBinding(element: CjBindingPattern, data: D?): R? {
        return visitCasePattern(element, data)
    }

    open fun visitMatchConditionWithExpression(condition: CjMatchConditionWithExpression, data: D?): R? {
        return visitCasePattern(condition, data)
    }

    open fun visitPatternByType(element: CjTypePattern, data: D?): R? {
        return visitCasePattern(element, data)
    }

    open fun visitPatternByTuple(element: CjTuplePattern, data: D?): R? {
        return visitCasePattern(element, data)
    }

    open fun visitPatternByWildcard(element: CjWildcardPattern, data: D?): R? {
        return visitCasePattern(element, data)
    }

    open fun visitPatternByEnum(element: CjEnumPattern, data: D?): R? {
        return visitCasePattern(element, data)
    }

    open fun visitOptionType(optionType: CjOptionType, data: D?): R? {
        return visitTypeElement(optionType, data)
    }

    open fun visitVArrayType(optionType: CjVArrayType, data: D?): R? {
        return visitTypeElement(optionType, data)
    }

    open fun visitThisType(optionType: CjThisType, data: D?): R? {
        return visitTypeElement(optionType, data)
    }

    open fun visitProperty(property: CjProperty, data: D?): R? {
        return visitNamedDeclaration(property, data)
    }

    open fun visitVariable(variable: CjVariable, data: D?): R? {
        return visitNamedDeclaration(variable, data)
    }

    open fun visitCallableReferenceExpression(expression: CjCallableReferenceExpression, data: D?): R? {
        return visitDoubleColonExpression(expression, data)
    }

    open fun visitDoubleColonExpression(expression: CjDoubleColonExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitRangeExpression(expression: CjRangeExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitSpawnExpression(expression: CjSpawnExpression, data: D?): R? {
        return visitCallExpression(expression, data)
    }

    open fun visitLambdaExpression(expression: CjLambdaExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitUnsafeExpression(expression: CjUnsafeExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitTupleExpression(expression: CjTupleExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitTryResourceList(resource: CjTryResourceList, data: D?): R? {
        return visitCjElement(resource, data)
    }

    open fun visitTryResource(resource: CjTryResource, data: D?): R? {
        return visitCjElement(resource, data)
    }

    open fun visitTryExpression(expression: CjTryExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitCatchSection(cjCatchClause: CjCatchClause, data: D?): R? {
        return visitCjElement(cjCatchClause, data)
    }

    open fun visitFinallySection(finallySection: CjFinallySection, data: D?): R? {
        return visitCjElement(finallySection, data)
    }

    open fun visitMatchExpression(expression: CjMatchExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitLetExpression(expression: CjLetExpression, data: D?): R? {
        return visitCjElement(expression, data)
    }

    open fun visitWhileExpression(expression: CjWhileExpression, data: D?): R? {
        return visitLoopExpression(expression, data)
    }

    open fun visitDoWhileExpression(expression: CjDoWhileExpression, data: D?): R? {
        return visitLoopExpression(expression, data)
    }

    open fun visitLoopExpression(loopExpression: CjLoopExpression, data: D?): R? {
        return visitExpression(loopExpression, data)
    }

    open fun visitForExpression(expression: CjForExpression, data: D?): R? {
        return visitLoopExpression(expression, data)
    }

    open fun visitIfExpression(expression: CjIfExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitContinueExpression(expression: CjContinueExpression, data: D?): R? {
        return visitExpressionWithLabel(expression, data)
    }

    open fun visitBreakExpression(expression: CjBreakExpression, data: D?): R? {
        return visitExpressionWithLabel(expression, data)
    }

    open fun visitCollectionLiteralExpression(expression: CjCollectionLiteralExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitThrowExpression(expression: CjThrowExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitReturnExpression(expression: CjReturnExpression, data: D?): R? {
        return visitExpressionWithLabel(expression, data)
    }

    open fun visitParenthesizedExpression(expression: CjParenthesizedExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitStringTemplateExpression(expression: CjStringTemplateExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitSuperExpression(expression: CjSuperExpression, data: D?): R? {
        return visitExpressionWithLabel(expression, data)
    }

    open fun visitThisExpression(expression: CjThisExpression, data: D?): R? {
        return visitExpressionWithLabel(expression, data)
    }

    open fun visitExpressionWithLabel(expression: CjExpressionWithLabel, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitLiteralStringTemplateEntry(entry: CjLiteralStringTemplateEntry, data: D?): R? {
        return visitStringTemplateEntry(entry, data)
    }

    open fun visitEscapeStringTemplateEntry(entry: CjEscapeStringTemplateEntry, data: D?): R? {
        return visitStringTemplateEntry(entry, data)
    }

    open fun visitStringTemplateEntry(entry: CjStringTemplateEntry, data: D?): R? {
        return visitCjElement(entry, data)
    }

    open fun visitStringTemplateEntryWithExpression(entry: CjStringTemplateEntryWithExpression, data: D?): R? {
        return visitStringTemplateEntry(entry, data)
    }

    open fun visitBlockStringTemplateEntry(entry: CjBlockStringTemplateEntry, data: D?): R? {
        return visitStringTemplateEntryWithExpression(entry, data)
    }

    open fun visitPostfixExpression(expression: CjPostfixExpression, data: D?): R? {
        return visitUnaryExpression(expression, data)
    }

    open fun visitConstantExpression(expression: CjConstantExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitValueArgumentList(list: CjValueArgumentList, data: D?): R? {
        return visitCjElement(list, data)
    }

    open fun visitArrayAccessExpression(expression: CjArrayAccessExpression, data: D?): R? {
        return visitReferenceExpression(expression, data)
    }

    open fun visitSuperTypeEntry(specifier: CjSuperTypeEntry, data: D?): R? {
        return visitSuperTypeListEntry(specifier, data)
    }

    open fun visitPrefixExpression(expression: CjPrefixExpression, data: D?): R? {
        return visitUnaryExpression(expression, data)
    }

    open fun visitUnaryExpression(expression: CjUnaryExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitIsExpression(expression: CjIsExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitBinaryExpression(expression: CjBinaryExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry, data: D?): R? {
        return visitSuperTypeListEntry(call, data)
    }

    open fun visitBinaryWithTypeRHSExpression(expression: CjBinaryExpressionWithTypeRHS, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitClassInitializer(initializer: CjClassInitializer, data: D?): R? {
        return visitAnonymousInitializer(initializer, data)
    }

    open fun visitAnonymousInitializer(initializer: CjAnonymousInitializer, data: D?): R? {
        return visitDeclaration(initializer, data)
    }

    open fun visitSuperTypeListEntry(specifier: CjSuperTypeListEntry, data: D?): R? {
        return visitCjElement(specifier, data)
    }

    open fun visitTypeProjection(typeProjection: CjTypeProjection, data: D?): R? {
        return visitCjElement(typeProjection, data)
    }

    open fun visitTypeParameterList(list: CjTypeParameterList, data: D?): R? {
        return visitCjElement(list, data)
    }

    open fun visitTypeParameter(parameter: CjTypeParameter, data: D?): R? {
        return visitNamedDeclaration(parameter, data)
    }

    open fun visitCjFile(file: CjFile, data: D?): R? {
        visitFile(file)
        return null
    }

    open fun visitBlockExpression(expression: CjBlockExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitExpression(expression: CjExpression, data: D?): R? {
        return visitCjElement(expression, data)
    }

    open fun visitClassBody(classBody: CjAbstractClassBody, data: D?): R? {
        return visitCjElement(classBody, data)
    }

    open fun visitEnumBody(enumBody: CjEnumBody, data: D?): R? {
        return visitCjElement(enumBody, data)
    }

    open fun visitModifierList(list: CjModifierList, data: D?): R? {
        return visitCjElement(list, data)
    }

    open fun visitContextReceiverList(contextReceiverList: CjContextReceiverList, data: D?): R? {
        return visitCjElement(contextReceiverList, data)
    }

    open fun visitTypeReference(cjTypeReference: CjTypeReference, data: D?): R? {
        return visitCjElement(cjTypeReference, data)
    }

    open fun visitSimpleNameExpression(expression: CjSimpleNameExpression, data: D?): R? {
        return visitReferenceExpression(expression, data)
    }

    open fun visitReferenceExpression(expression: CjReferenceExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitTypeConstraintList(cjTypeConstraintList: CjTypeConstraintList, data: D?): R? {
        return visitCjElement(cjTypeConstraintList, data)
    }

    open fun visitTypeConstraint(cjTypeConstraint: CjTypeConstraint, data: D?): R? {
        return visitCjElement(cjTypeConstraint, data)
    }

    open fun visitParameterList(cjParameterList: CjParameterList, data: D?): R? {
        return visitCjElement(cjParameterList, data)
    }

    open fun visitParameter(cjParameter: CjParameter, data: D?): R? {
        return visitCjElement(cjParameter, data)
    }

    open fun visitDestructuringDeclaration(cjDestructuringDeclaration: CjDestructuringDeclaration, data: D?): R? {
        return visitCjElement(cjDestructuringDeclaration, data)
    }

    open fun visitDestructuringDeclarationEntry(
        cjDestructuringDeclarationEntry: CjDestructuringDeclarationEntry,
        data: D?
    ): R? {
        return visitCjElement(cjDestructuringDeclarationEntry, data)
    }

    open fun visitDeclaration(dcl: CjDeclaration, data: D?): R? {
        return visitExpression(dcl, data)
    }

    open fun visitTypeAlias(typeAlias: CjTypeAlias, data: D?): R? {
        return visitNamedDeclaration(typeAlias, data)
    }

    open fun visitNamedDeclaration(declaration: CjNamedDeclaration, data: D?): R? {
        return visitDeclaration(declaration, data)
    }

    open fun visitPrimaryConstructor(constructor: CjPrimaryConstructor, data: D?): R? {
        return visitNamedDeclaration(constructor, data)
    }

    open fun visitMacroDeclaration(macroDeclaration: CjMacroDeclaration, data: D?): R? {
        return visitNamedDeclaration(macroDeclaration, data)
    }

    open fun visitNamedFunction(cjNamedFunction: CjNamedFunction, data: D?): R? {
        return visitNamedDeclaration(cjNamedFunction, data)
    }

    open fun visitCallExpression(cjCallExpression: CjCallExpression, data: D?): R? {
        return visitReferenceExpression(cjCallExpression, data)
    }

    open fun visitEndSecondaryConstructor(constructor: CjEndSecondaryConstructor, data: D?): R? {
        return visitNamedDeclaration(constructor, data)
    }

    open fun visitSecondaryConstructor(constructor: CjSecondaryConstructor, data: D?): R? {
        return visitNamedDeclaration(constructor, data)
    }

    open fun visitConstructorDelegationCall(cjConstructorDelegationCall: CjConstructorDelegationCall, data: D?): R? {
        return visitCjElement(cjConstructorDelegationCall, data)
    }

    open fun visitPackageDirective(cjPackageDirective: CjPackageDirective, data: D?): R? {
        return visitCjElement(cjPackageDirective, data)
    }

    open fun visitQualifiedExpression(expression: CjQualifiedExpression, data: D?): R? {
        return visitExpression(expression, data)
    }

    open fun visitDotQualifiedExpression(expression: CjDotQualifiedExpression, data: D?): R? {
        return visitQualifiedExpression(expression, data)
    }

    open fun visitImportDirectiveItem(importDirectiveItem: CjImportDirectiveItem, data: D?): R? {
        return visitCjElement(importDirectiveItem, data)
    }

    open fun visitImportDirective(cjImportDirective: CjImportDirective, data: D?): R? {
        return visitCjElement(cjImportDirective, data)
    }

    open fun visitUserType(cjUserType: CjUserType, data: D?): R? {
        return visitCjElement(cjUserType, data)
    }

    open fun visitTypeArgumentList(cjTypeArgumentList: CjTypeArgumentList, data: D?): R? {
        return visitCjElement(cjTypeArgumentList, data)
    }

    open fun visitImportAlias(importAlias: CjImportAlias, data: D?): R? {
        return visitCjElement(importAlias, data)
    }

    open fun visitImportList(cjImportList: CjImportList, data: D?): R? {
        return visitCjElement(cjImportList, data)
    }

    open fun visitSuperTypeList(list: CjSuperTypeList, data: D?): R? {
        return visitCjElement(list, data)
    }

    open fun visitConstructorCalleeExpression(
        constructorCalleeExpression: CjConstructorCalleeExpression,
        data: D?
    ): R? {
        return visitCjElement(constructorCalleeExpression, data)
    }

    open fun visitBasicType(cjBasicType: CjBasicType, data: D?): R? {
        return visitCjElement(cjBasicType, data)
    }

    open fun visitKeyword(cjKeyword: CjKeyword, data: D?): R? {
        return visitCjElement(cjKeyword, data)
    }

    open fun visitSimpleNameStringTemplateEntry(entry: CjSimpleNameStringTemplateEntry, data: D?): R? {
        return visitStringTemplateEntryWithExpression(entry, data)
    }

    open fun visitEnumEntry(cjEnumEntry: CjEnumEntry, data: D?): R? {
        return visitTypeStatement(cjEnumEntry, data)
    }

    open fun visitMatchEntry(cjMatchEntry: CjMatchEntry, data: D?): R? {
        return visitCjElement(cjMatchEntry, data)
    }

    open fun visitTupleType(cjTupleType: CjTupleType, data: D?): R? {
        return visitCjElement(cjTupleType, data)
    }

    open fun visitParenthesizedType(cjParenthesizedType: CjParenthesizedType, data: D?): R? {
        return visitCjElement(cjParenthesizedType, data)
    }

    open fun visitFunctionType(type: CjFunctionType, data: D?): R? {
        return visitTypeElement(type, data)
    }

    protected open fun visitTypeElement(type: CjTypeElement, data: D?): R? {
        return visitCjElement(type, data)
    }

    open fun visitPropertyAccessor(accessor: CjPropertyAccessor, data: D?): R? {
        return visitDeclaration(accessor, data)
    }

    open fun visitExtend(cjExtend: CjExtend, data: D?): R? {
        return visitTypeStatement(cjExtend, data)
    }

    open fun visitClass(klass: CjClass, data: D?): R? {
        return visitTypeStatement(klass, data)
    }

    open fun visitEnum(cenum: CjEnum, data: D?): R? {
        return visitTypeStatement(cenum, data)
    }

    open fun visitStruct(cstruct: CjStruct, data: D?): R? {
        return visitTypeStatement(cstruct, data)
    }

    open fun visitInterface(cinterface: CjInterface, data: D?): R? {
        return visitTypeStatement(cinterface, data)
    }

    // 类型声明
    open fun visitTypeStatement(typeStatement: CjTypeStatement, data: D?): R? {
        return visitNamedDeclaration(typeStatement, data)
    }

    open fun visitAnnotation(annotation: CjAnnotation, data: D?): R? {
        return visitCjElement(annotation, data)
    }

    open fun visitMainFunction(cjMainFunction: CjMainFunction, data: D?): R? {
        return visitDeclaration(cjMainFunction, data)
    }

    open fun visitClassInitFunction(cjClassInit: CjClassInit, data: D?): R? {
        return visitDeclaration(cjClassInit, data)
    }
} 