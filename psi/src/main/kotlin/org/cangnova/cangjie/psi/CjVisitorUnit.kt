/*
 * Copyright 2025 LinQingYing. and contributors.
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

open class CjVisitorUnit : CjVisitor<Unit, Unit?>() {

    open fun visitCjElement(element: CjElement) {
        super.visitCjElement(element, Unit)
    }

    open fun visitTupleType(type: CjTupleType) {
        super.visitTupleType(type, Unit)
    }

    open fun visitFunctionType(type: CjFunctionType) {
        super.visitFunctionType(type, Unit)
    }

    override fun visitTupleType(tupleType: CjTupleType, data: Unit?) {
        visitTupleType(tupleType)

    }

    override fun visitFunctionType(type: CjFunctionType, data: Unit?): Unit {
        visitFunctionType(type)

    }

    override fun visitParenthesizedType(parenthesizedType: CjParenthesizedType, data: Unit?): Unit {
        visitParenthesizedType(parenthesizedType)

    }

    open fun visitParenthesizedType(parenthesizedType: CjParenthesizedType) {
        super.visitParenthesizedType(parenthesizedType, Unit)
    }

    override fun visitOptionType(optionType: CjOptionType, data: Unit?) {
        visitOptionType(optionType)

    }

    open fun visitOptionType(optionType: CjOptionType) {
        super.visitOptionType(optionType, Unit)
    }

    open fun visitProperty(property: CjProperty) {
        super.visitProperty(property, Unit)
    }

    open fun visitPatternVariable(variable: CjPatternVariable) {
        super.visitPatternVariable(variable, Unit)
    }

    open fun visitVariable(variable: CjVariable<*>) {
        super.visitVariable(variable, Unit)
    }

    open fun visitFieldVariable(field: CjFieldVariable) {
        super.visitFieldVariable(field, Unit)
    }

    open fun visitTypeStatement(typeStatement: CjTypeStatement) {
        super.visitTypeStatement(typeStatement, Unit)
    }

    open fun visitClass(cclass: CjClass) {
        super.visitClass(cclass, Unit)
    }

    override fun visitExtend(extend: CjExtend, data: Unit?): Unit {
        visitExtend(extend)

    }

    open fun visitExtend(extend: CjExtend) {
        super.visitExtend(extend, Unit)
    }

    override fun visitMainFunction(mainFunction: CjMainFunction, data: Unit?): Unit {
        visitMainFunction(mainFunction)

    }

    open fun visitMainFunction(mainFunction: CjMainFunction) {
        super.visitMainFunction(mainFunction, Unit)
    }

    open fun visitStruct(cstruct: CjStruct) {
        super.visitStruct(cstruct, Unit)
    }

    open fun visitEnum(cenum: CjEnum) {
        super.visitEnum(cenum, Unit)
    }

    open fun visitInterface(cinterface: CjInterface) {
        super.visitInterface(cinterface, Unit)
    }

    open fun visitDeclaration(dcl: CjDeclaration) {
        super.visitDeclaration(dcl, Unit)
    }

    open fun visitTypeAlias(typeAlias: CjTypeAlias) {
        super.visitTypeAlias(typeAlias, Unit)
    }

    open fun visitEndSecondaryConstructor(constructor: CjEndSecondaryConstructor) {
        super.visitEndSecondaryConstructor(constructor, Unit)
    }

    open fun visitSecondaryConstructor(constructor: CjSecondaryConstructor) {
        super.visitSecondaryConstructor(constructor, Unit)
    }

    open fun visitPrimaryConstructor(constructor: CjPrimaryConstructor) {
        super.visitPrimaryConstructor(constructor, Unit)
    }

    open fun visitMacroDeclaration(function: CjMacroDeclaration) {
        super.visitMacroDeclaration(function, Unit)
    }

    open fun visitNamedFunction(function: CjNamedFunction) {
        super.visitNamedFunction(function, Unit)
    }

    open fun visitCjFile(file: CjFile) {
        super.visitCjFile(file, Unit)
    }

    open fun visitImportAlias(importAlias: CjImportAlias) {
        super.visitImportAlias(importAlias, Unit)
    }

    open fun visitImportDirective(importDirective: CjImportDirective) {
        super.visitImportDirective(importDirective, Unit)
    }


    open fun visitImportList(importList: CjImportList) {
        super.visitImportList(importList, Unit)
    }

    open fun visitClassBody(classBody: CjAbstractClassBody) {
        super.visitClassBody(classBody, Unit)
    }

    open fun visitModifierList(list: CjModifierList) {
        super.visitModifierList(list, Unit)
    }

    fun visitConstructorCalleeExpression(constructorCalleeExpression: CjConstructorCalleeExpression) {
        super.visitConstructorCalleeExpression(constructorCalleeExpression, Unit)
    }

    fun visitTypeParameterList(list: CjTypeParameterList) {
        super.visitTypeParameterList(list, Unit)
    }

    open fun visitTypeParameter(parameter: CjTypeParameter) {
        super.visitTypeParameter(parameter, Unit)
    }

    open fun visitEnumConstructor(enumEntry: CjEnumConstructor) {
        super.visitEnumConstructor(enumEntry, Unit)
    }

    open fun visitParameterList(list: CjParameterList) {
        super.visitParameterList(list, Unit)
    }

    open fun visitParameter(parameter: CjParameter) {
        super.visitParameter(parameter, Unit)
    }

    open fun visitSuperTypeList(list: CjSuperTypeList) {
        super.visitSuperTypeList(list, Unit)
    }

    fun visitSuperTypeListEntry(specifier: CjSuperTypeListEntry) {
        super.visitSuperTypeListEntry(specifier, Unit)
    }

    open fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry) {
        super.visitSuperTypeCallEntry(call, Unit)
    }

    open fun visitSuperTypeEntry(specifier: CjSuperTypeEntry) {
        super.visitSuperTypeEntry(specifier, Unit)
    }


    open fun visitConstructorDelegationCall(call: CjConstructorDelegationCall) {
        super.visitConstructorDelegationCall(call, Unit)
    }

    open fun visitTypeReference(typeReference: CjTypeReference) {
        super.visitTypeReference(typeReference, Unit)
    }

    open fun visitValueArgumentList(list: CjValueArgumentList) {
        super.visitValueArgumentList(list, Unit)
    }

    open fun visitExpression(expression: CjExpression) {
        super.visitExpression(expression, Unit)
    }

    fun visitLoopExpression(loopExpression: CjLoopExpression) {
        super.visitLoopExpression(loopExpression, Unit)
    }

    open fun visitConstantExpression(expression: CjConstantExpression) {
        super.visitConstantExpression(expression, Unit)
    }

    open fun visitSimpleNameExpression(expression: CjSimpleNameExpression) {
        super.visitSimpleNameExpression(expression, Unit)
    }

    fun visitReferenceExpression(expression: CjReferenceExpression) {
        super.visitReferenceExpression(expression, Unit)
    }

    open fun visitPrefixExpression(expression: CjPrefixExpression) {
        super.visitPrefixExpression(expression, Unit)
    }

    open fun visitPostfixExpression(expression: CjPostfixExpression) {
        super.visitPostfixExpression(expression, Unit)
    }

    open fun visitUnaryExpression(expression: CjUnaryExpression) {
        super.visitUnaryExpression(expression, Unit)
    }

    open fun visitBinaryExpression(expression: CjBinaryExpression) {
        super.visitBinaryExpression(expression, Unit)
    }

    open fun visitReturnExpression(expression: CjReturnExpression) {
        super.visitReturnExpression(expression, Unit)
    }

    open fun visitExpressionWithLabel(expression: CjExpressionWithLabel) {
        super.visitExpressionWithLabel(expression, Unit)
    }

    open fun visitThrowExpression(expression: CjThrowExpression) {
        super.visitThrowExpression(expression, Unit)
    }

    open fun visitBreakExpression(expression: CjBreakExpression) {
        super.visitBreakExpression(expression, Unit)
    }

    open fun visitContinueExpression(expression: CjContinueExpression) {
        super.visitContinueExpression(expression, Unit)
    }

    open fun visitIfExpression(expression: CjIfExpression) {
        super.visitIfExpression(expression, Unit)
    }

    open fun visitMatchExpression(expression: CjMatchExpression) {
        super.visitMatchExpression(expression, Unit)
    }

    fun visitCollectionLiteralExpression(expression: CjCollectionLiteralExpression) {
        super.visitCollectionLiteralExpression(expression, Unit)
    }

    open fun visitTryExpression(expression: CjTryExpression) {
        super.visitTryExpression(expression, Unit)
    }

    open fun visitForExpression(expression: CjForExpression) {
        super.visitForExpression(expression, Unit)
    }

    open fun visitWhileExpression(expression: CjWhileExpression) {
        super.visitWhileExpression(expression, Unit)
    }

    open fun visitDoWhileExpression(expression: CjDoWhileExpression) {
        super.visitDoWhileExpression(expression, Unit)
    }

    open fun visitCallExpression(expression: CjCallExpression) {
        super.visitCallExpression(expression, Unit)
    }

    open fun visitArrayAccessExpression(expression: CjArrayAccessExpression) {
        super.visitArrayAccessExpression(expression, Unit)
    }

    open fun visitQualifiedExpression(expression: CjQualifiedExpression) {
        super.visitQualifiedExpression(expression, Unit)
    }

    open fun visitLambdaExpression(lambdaExpression: CjLambdaExpression) {
        super.visitLambdaExpression(lambdaExpression, Unit)
    }

    override fun visitLambdaExpression(expression: CjLambdaExpression, data: Unit?): Unit {
        visitLambdaExpression(expression)

    }

    open fun visitDotQualifiedExpression(expression: CjDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression, Unit)
    }

    open fun visitBlockExpression(expression: CjBlockExpression) {
        super.visitBlockExpression(expression, Unit)
    }

    fun visitCatchSection(catchClause: CjCatchClause) {
        super.visitCatchSection(catchClause, Unit)
    }

    fun visitFinallySection(finallySection: CjFinallySection) {
        super.visitFinallySection(finallySection, Unit)
    }

    fun visitTypeArgumentList(typeArgumentList: CjTypeArgumentList) {
        super.visitTypeArgumentList(typeArgumentList, Unit)
    }

    open fun visitThisExpression(expression: CjThisExpression) {
        super.visitThisExpression(expression, Unit)
    }

    open fun visitSuperExpression(expression: CjSuperExpression) {
        super.visitSuperExpression(expression, Unit)
    }

    open fun visitParenthesizedExpression(expression: CjParenthesizedExpression) {
        super.visitParenthesizedExpression(expression, Unit)
    }


    fun visitTypeConstraintList(list: CjTypeConstraintList) {
        super.visitTypeConstraintList(list, Unit)
    }

    fun visitTypeConstraint(constraint: CjTypeConstraint) {
        super.visitTypeConstraint(constraint, Unit)
    }

    open fun visitUserType(type: CjUserType) {
        super.visitUserType(type, Unit)
    }

    open fun visitVArrayType(type: CjVArrayType) {
        super.visitVArrayType(type, Unit)
    }

    open fun visitThisType(type: CjThisType) {
        super.visitThisType(type, Unit)
    }

    open fun visitBasicType(type: CjBasicType) {
        super.visitBasicType(type, Unit)
    }

    open fun visitBinaryWithTypeRHSExpression(expression: CjBinaryExpressionWithTypeRHS) {
        super.visitBinaryWithTypeRHSExpression(expression, Unit)
    }

    open fun visitStringTemplateExpression(expression: CjStringTemplateExpression) {
        super.visitStringTemplateExpression(expression, Unit)
    }

    open fun visitNamedDeclaration(declaration: CjNamedDeclaration) {
        super.visitNamedDeclaration(declaration, Unit)
    }

    open fun visitTypeProjection(typeProjection: CjTypeProjection) {
        super.visitTypeProjection(typeProjection, Unit)
    }

    fun visitWhenEntry(matchEntry: CjMatchEntry) {
        super.visitMatchEntry(matchEntry, Unit)
    }

    open fun visitIsExpression(expression: CjIsExpression) {
        super.visitIsExpression(expression, Unit)
    }

    fun visitStringTemplateEntry(entry: CjStringTemplateEntry) {
        super.visitStringTemplateEntry(entry, Unit)
    }

    open fun visitStringTemplateEntryWithExpression(entry: CjStringTemplateEntryWithExpression) {
        super.visitStringTemplateEntryWithExpression(entry, Unit)
    }

    fun visitBlockStringTemplateEntry(entry: CjBlockStringTemplateEntry) {
        super.visitBlockStringTemplateEntry(entry, Unit)
    }

    fun visitSimpleNameStringTemplateEntry(entry: CjSimpleNameStringTemplateEntry) {
        super.visitSimpleNameStringTemplateEntry(entry, Unit)
    }

    open fun visitLiteralStringTemplateEntry(entry: CjLiteralStringTemplateEntry) {
        super.visitLiteralStringTemplateEntry(entry, Unit)
    }

    open fun visitEscapeStringTemplateEntry(entry: CjEscapeStringTemplateEntry) {
        super.visitEscapeStringTemplateEntry(entry, Unit)
    }

    open fun visitPackageDirective(directive: CjPackageDirective) {
        super.visitPackageDirective(directive, Unit)
    }

    // hidden methods
    override fun visitCjElement(element: CjElement, data: Unit?): Unit {
        visitCjElement(element)

    }

    override fun visitDeclaration(dcl: CjDeclaration, data: Unit?): Unit {
        visitDeclaration(dcl)

    }

    override fun visitProperty(property: CjProperty, data: Unit?): Unit {
        visitProperty(property)

    }

    override fun visitPatternVariable(variable: CjPatternVariable, data: Unit?) {
        visitPatternVariable(variable)
    }

    override fun visitFieldVariable(field: CjFieldVariable, data: Unit?): Unit {
        visitFieldVariable(field)
    }

    override fun visitEndSecondaryConstructor(constructor: CjEndSecondaryConstructor, data: Unit?): Unit {
        visitEndSecondaryConstructor(constructor)

    }

    override fun visitSecondaryConstructor(constructor: CjSecondaryConstructor, data: Unit?): Unit {
        visitSecondaryConstructor(constructor)

    }

    override fun visitPrimaryConstructor(constructor: CjPrimaryConstructor, data: Unit?): Unit {
        visitPrimaryConstructor(constructor)

    }

    override fun visitMacroExpression(expression: CjMacroExpression, data: Unit?) {
        visitMacroExpression(expression)
    }

    open fun visitMacroExpression(expression: CjMacroExpression) {
        super.visitMacroExpression(expression, Unit)
    }

    override fun visitMacroDeclaration(macroDeclaration: CjMacroDeclaration, data: Unit?): Unit {
        visitMacroDeclaration(macroDeclaration)

    }

    override fun visitNamedFunction(function: CjNamedFunction, data: Unit?): Unit {
        visitNamedFunction(function)

    }

    override fun visitTypeAlias(typeAlias: CjTypeAlias, data: Unit?): Unit {
        visitTypeAlias(typeAlias)

    }

    override fun visitCjFile(file: CjFile, data: Unit?): Unit {
        visitCjFile(file)

    }


    override fun visitImportDirective(importDirective: CjImportDirective, data: Unit?): Unit {
        visitImportDirective(importDirective)

    }

    override fun visitImportList(importList: CjImportList, data: Unit?): Unit {
        visitImportList(importList)

    }

    override fun visitClassBody(classBody: CjAbstractClassBody, data: Unit?): Unit {
        visitClassBody(classBody)

    }

    override fun visitModifierList(list: CjModifierList, data: Unit?): Unit {
        visitModifierList(list)

    }

    override fun visitConstructorCalleeExpression(
        constructorCalleeExpression: CjConstructorCalleeExpression,
        data: Unit?,
    ): Unit {
        visitConstructorCalleeExpression(constructorCalleeExpression)

    }

    override fun visitTypeParameterList(list: CjTypeParameterList, data: Unit?): Unit {
        visitTypeParameterList(list)

    }

    override fun visitTypeParameter(parameter: CjTypeParameter, data: Unit?): Unit {
        visitTypeParameter(parameter)

    }

    override fun visitEnumConstructor(enumConstructor: CjEnumConstructor, data: Unit?): Unit {
        visitEnumConstructor(enumConstructor)

    }

    override fun visitParameterList(parameterList: CjParameterList, data: Unit?): Unit {
        visitParameterList(parameterList)

    }

    override fun visitParameter(parameter: CjParameter, data: Unit?): Unit {
        visitParameter(parameter)

    }

    override fun visitSuperTypeList(list: CjSuperTypeList, data: Unit?): Unit {
        visitSuperTypeList(list)

    }

    override fun visitSuperTypeListEntry(specifier: CjSuperTypeListEntry, data: Unit?): Unit {
        visitSuperTypeListEntry(specifier)

    }

    override fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry, data: Unit?): Unit {
        visitSuperTypeCallEntry(call)

    }

    override fun visitSuperTypeEntry(specifier: CjSuperTypeEntry, data: Unit?): Unit {
        visitSuperTypeEntry(specifier)

    }

    override fun visitConstructorDelegationCall(
        call: CjConstructorDelegationCall,
        data: Unit?,
    ): Unit {
        visitConstructorDelegationCall(call)

    }

    override fun visitTypeReference(typeReference: CjTypeReference, data: Unit?): Unit {
        visitTypeReference(typeReference)

    }

    override fun visitValueArgumentList(list: CjValueArgumentList, data: Unit?): Unit {
        visitValueArgumentList(list)

    }

    override fun visitExpression(expression: CjExpression, data: Unit?): Unit {
        visitExpression(expression)

    }

    override fun visitLoopExpression(loopExpression: CjLoopExpression, data: Unit?): Unit {
        visitLoopExpression(loopExpression)

    }

    override fun visitConstantExpression(expression: CjConstantExpression, data: Unit?): Unit {
        visitConstantExpression(expression)

    }

    override fun visitSimpleNameExpression(expression: CjSimpleNameExpression, data: Unit?): Unit {
        visitSimpleNameExpression(expression)

    }

    override fun visitReferenceExpression(
        expression: CjReferenceExpression,
        data: Unit?,
    ): Unit {
        visitReferenceExpression(expression)

    }

    override fun visitPrefixExpression(expression: CjPrefixExpression, data: Unit?): Unit {
        visitPrefixExpression(expression)

    }

    override fun visitPostfixExpression(expression: CjPostfixExpression, data: Unit?): Unit {
        visitPostfixExpression(expression)

    }

    override fun visitUnaryExpression(expression: CjUnaryExpression, data: Unit?): Unit {
        visitUnaryExpression(expression)

    }

    override fun visitBinaryExpression(expression: CjBinaryExpression, data: Unit?): Unit {
        visitBinaryExpression(expression)

    }

    override fun visitReturnExpression(expression: CjReturnExpression, data: Unit?): Unit {
        visitReturnExpression(expression)

    }

    override fun visitExpressionWithLabel(expression: CjExpressionWithLabel, data: Unit?): Unit {
        visitExpressionWithLabel(expression)

    }

    override fun visitThrowExpression(expression: CjThrowExpression, data: Unit?): Unit {
        visitThrowExpression(expression)

    }

    override fun visitBreakExpression(expression: CjBreakExpression, data: Unit?): Unit {
        visitBreakExpression(expression)

    }

    override fun visitContinueExpression(expression: CjContinueExpression, data: Unit?): Unit {
        visitContinueExpression(expression)

    }

    override fun visitIfExpression(expression: CjIfExpression, data: Unit?): Unit {
        visitIfExpression(expression)

    }

    override fun visitMatchExpression(expression: CjMatchExpression, data: Unit?): Unit {
        visitMatchExpression(expression)

    }

    override fun visitCollectionLiteralExpression(
        expression: CjCollectionLiteralExpression,
        data: Unit?,
    ): Unit {
        visitCollectionLiteralExpression(expression)

    }

    override fun visitTryExpression(expression: CjTryExpression, data: Unit?): Unit {
        visitTryExpression(expression)

    }

    override fun visitForExpression(expression: CjForExpression, data: Unit?): Unit {
        visitForExpression(expression)

    }

    override fun visitWhileExpression(expression: CjWhileExpression, data: Unit?): Unit {
        visitWhileExpression(expression)

    }

    override fun visitDoWhileExpression(expression: CjDoWhileExpression, data: Unit?): Unit {
        visitDoWhileExpression(expression)

    }

    override fun visitCallExpression(expression: CjCallExpression, data: Unit?): Unit {
        visitCallExpression(expression)

    }

    override fun visitArrayAccessExpression(
        expression: CjArrayAccessExpression,
        data: Unit?,
    ): Unit {
        visitArrayAccessExpression(expression)

    }

    override fun visitQualifiedExpression(expression: CjQualifiedExpression, data: Unit?): Unit {
        visitQualifiedExpression(expression)

    }

    override fun visitDotQualifiedExpression(expression: CjDotQualifiedExpression, data: Unit?): Unit {
        visitDotQualifiedExpression(expression)

    }

    override fun visitBlockExpression(expression: CjBlockExpression, data: Unit?): Unit {
        visitBlockExpression(expression)

    }

    override fun visitCatchSection(catchClause: CjCatchClause, data: Unit?): Unit {
        visitCatchSection(catchClause)

    }

    override fun visitFinallySection(finallySection: CjFinallySection, data: Unit?): Unit {
        visitFinallySection(finallySection)

    }

    override fun visitTypeArgumentList(
        typeArgumentList: CjTypeArgumentList,
        data: Unit?,
    ): Unit {
        visitTypeArgumentList(typeArgumentList)

    }

    override fun visitThisExpression(expression: CjThisExpression, data: Unit?): Unit {
        visitThisExpression(expression)

    }

    override fun visitSuperExpression(expression: CjSuperExpression, data: Unit?): Unit {
        visitSuperExpression(expression)

    }

    override fun visitParenthesizedExpression(
        expression: CjParenthesizedExpression,
        data: Unit?,
    ): Unit {
        visitParenthesizedExpression(expression)

    }


    override fun visitTypeConstraintList(list: CjTypeConstraintList, data: Unit?): Unit {
        visitTypeConstraintList(list)

    }

    override fun visitTypeConstraint(constraint: CjTypeConstraint, data: Unit?): Unit {
        visitTypeConstraint(constraint)

    }

    override fun visitUserType(type: CjUserType, data: Unit?): Unit {
        visitUserType(type)

    }

    override fun visitVArrayType(optionType: CjVArrayType, data: Unit?): Unit {
        visitVArrayType(optionType)

    }

    override fun visitThisType(thisType: CjThisType, data: Unit?): Unit {
        visitThisType(thisType)

    }

    override fun visitBasicType(basicType: CjBasicType, data: Unit?): Unit {
        visitBasicType(basicType)

    }

    override fun visitBinaryWithTypeRHSExpression(
        expression: CjBinaryExpressionWithTypeRHS,
        data: Unit?,
    ): Unit {
        visitBinaryWithTypeRHSExpression(expression)

    }

    override fun visitStringTemplateExpression(
        expression: CjStringTemplateExpression,
        data: Unit?,
    ): Unit {
        visitStringTemplateExpression(expression)

    }

    override fun visitNamedDeclaration(declaration: CjNamedDeclaration, data: Unit?): Unit {
        visitNamedDeclaration(declaration)

    }

    override fun visitTypeProjection(typeProjection: CjTypeProjection, data: Unit?): Unit {
        visitTypeProjection(typeProjection)

    }

    fun visitMatchEntry(matchEntry: CjMatchEntry) {
        super.visitMatchEntry(matchEntry, Unit)
    }

    override fun visitMatchEntry(matchEntry: CjMatchEntry, data: Unit?): Unit {
        visitMatchEntry(matchEntry)

    }

    override fun visitIsExpression(expression: CjIsExpression, data: Unit?): Unit {
        visitIsExpression(expression)

    }

    override fun visitStringTemplateEntry(entry: CjStringTemplateEntry, data: Unit?): Unit {
        visitStringTemplateEntry(entry)

    }

    override fun visitStringTemplateEntryWithExpression(
        entry: CjStringTemplateEntryWithExpression,
        data: Unit?,
    ): Unit {
        visitStringTemplateEntryWithExpression(entry)

    }

    override fun visitBlockStringTemplateEntry(
        entry: CjBlockStringTemplateEntry,
        data: Unit?,
    ): Unit {
        visitBlockStringTemplateEntry(entry)

    }

    override fun visitSimpleNameStringTemplateEntry(
        entry: CjSimpleNameStringTemplateEntry,
        data: Unit?,
    ): Unit {
        visitSimpleNameStringTemplateEntry(entry)

    }

    override fun visitLiteralStringTemplateEntry(
        entry: CjLiteralStringTemplateEntry,
        data: Unit?,
    ): Unit {
        visitLiteralStringTemplateEntry(entry)

    }

    override fun visitEscapeStringTemplateEntry(
        entry: CjEscapeStringTemplateEntry,
        data: Unit?,
    ): Unit {
        visitEscapeStringTemplateEntry(entry)

    }

    override fun visitPackageDirective(packageDirective: CjPackageDirective, data: Unit?): Unit {
        visitPackageDirective(packageDirective)

    }

    override fun visitTypeStatement(typeStatement: CjTypeStatement, data: Unit?): Unit {
        visitTypeStatement(typeStatement)

    }

    override fun visitClass(cclass: CjClass, data: Unit?): Unit {
        visitClass(cclass)

    }

    override fun visitStruct(cstruct: CjStruct, data: Unit?): Unit {
        visitStruct(cstruct)

    }

    override fun visitEnum(cenum: CjEnum, data: Unit?): Unit {
        visitEnum(cenum)

    }

    override fun visitInterface(cinterface: CjInterface, data: Unit?): Unit {
        visitInterface(cinterface)

    }

    override fun visitCasePattern(element: CjCasePatternElement, data: Unit?): Unit {
        visitCasePattern(element)

    }

    open fun visitCasePattern(element: CjCasePatternElement) {
        super.visitCasePattern(element, Unit)
    }

    override fun visitPatternByBinding(element: CjBindingPattern, data: Unit?): Unit {
        visitPatternByBinding(element)

    }

    open fun visitPatternByBinding(element: CjBindingPattern) {
        super.visitPatternByBinding(element, Unit)
    }

    override fun visitPatternByConstant(element: CjConstantPattern, data: Unit?): Unit {
        visitPatternByConstant(element)

    }

    open fun visitPatternByConstant(element: CjConstantPattern) {
        super.visitPatternByConstant(element, Unit)
    }

    override fun visitMatchConditionWithExpression(condition: CjMatchConditionWithExpression, data: Unit?): Unit {
        visitMatchConditionWithExpression(condition)

    }

    override fun visitPatternByEnum(element: CjEnumPattern, data: Unit?): Unit {
        visitPatternByEnum(element)

    }

    open fun visitMatchConditionWithExpression(element: CjMatchConditionWithExpression) {
        super.visitMatchConditionWithExpression(element, Unit)
    }

    open fun visitPatternByEnum(element: CjEnumPattern) {
        super.visitPatternByEnum(element, Unit)
    }

    override fun visitPatternByTuple(element: CjTuplePattern, data: Unit?): Unit {
        visitPatternByTuple(element)

    }

    open fun visitPatternByTuple(element: CjTuplePattern) {
        super.visitPatternByTuple(element, Unit)
    }

    override fun visitPatternByWildcard(element: CjWildcardPattern, data: Unit?): Unit {
        visitPatternByWildcard(element)

    }

    override fun visitPatternByType(element: CjTypePattern, data: Unit?): Unit {
        visitPatternByType(element)

    }

    open fun visitPatternByType(element: CjTypePattern) {
        super.visitPatternByType(element, Unit)
    }

    open fun visitPatternByWildcard(element: CjWildcardPattern) {
        super.visitPatternByWildcard(element, Unit)
    }
}
