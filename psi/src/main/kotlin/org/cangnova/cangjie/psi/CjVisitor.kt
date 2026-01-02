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

import com.intellij.psi.PsiElementVisitor

/**
 * 仓颉语言 PSI 树访问者基类
 *
 * 实现访问者模式，用于遍历和处理仓颉语言的程序结构接口（PSI）树。
 *
 * ## 类型参数
 * - `R`: 访问方法的返回值类型
 * - `D`: 访问过程中传递的数据类型
 *
 * ## 使用方式
 * 继承此类并重写需要处理的特定节点类型的 visit 方法。访问器遵循层次结构，
 * 子节点类型的默认实现会调用父节点类型的方法。
 *
 * ## 示例
 * ```kotlin
 * class MyVisitor : CjVisitor<Unit, MyData>() {
 *     override fun visitClass(klass: CjClass, data: MyData): Unit? {
 *         // 处理类声明
 *         return super.visitClass(klass, data)
 *     }
 * }
 * ```
 *
 * @see CjElement
 * @see PsiElementVisitor
 */
open class CjVisitor<R, D> : PsiElementVisitor() {

    /**
     * 访问仓颉语言的 PSI 元素
     *
     * 这是所有仓颉语言元素的基础访问方法，其他 visit 方法最终都会委托到这里。
     *
     * @param element 要访问的仓颉元素
     * @param data 传递给访问器的上下文数据
     * @return 访问结果，默认返回 null
     */
    open fun visitCjElement(element: CjElement, data: D): R? {
        visitElement(element)
        return null
    }

    open fun visitQuoteInterpolate(expression: CjQuoteInterpolate, data: D): R? {
        return visitCjElement(expression, data)
    }

    open fun visitScript(script: CjScript, data: D): R? {
        return visitDeclaration(script, data)
    }

    /**
     * 访问通用仓颉文件
     *
     * 用于支持 [CjCommonFile] 的实现。通常不应直接使用此方法，
     * 因为 [CjCommonFile] 本身不应该被直接使用。
     *
     * @param file 仓颉通用文件
     * @return 访问结果，默认返回 null
     */
    @Suppress("DEPRECATION")
    fun visitCjCommonFile(file: CjCommonFile): R? {
        visitFile(file)
        return null
    }

    open fun visitSynchronizedExpression(expression: CjSynchronizedExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitPatternGuard(expression: CjPatternGuard, data: D): R? {
        return visitCjElement(expression, data)
    }

    open fun visitSafeQualifiedExpression(expression: CjSafeQualifiedExpression, data: D): R? {
        return visitQualifiedExpression(expression, data)
    }

    open fun visitMacroExpression(expression: CjMacroExpression, data: D): R? {
        return visitCjElement(expression, data)
    }

    open fun visitQuoteExpression(element: CjQuoteExpression, data: D): R? {
        return visitExpression(element, data)
    }

    open fun visitCasePattern(element: CjCasePatternElement, data: D): R? {
        return visitCjElement(element, data)
    }

    open fun visitPatternByConstant(element: CjConstantPattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    open fun visitPatternByBinding(element: CjBindingPattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    open fun visitMatchConditionWithExpression(condition: CjMatchConditionWithExpression, data: D): R? {
        return visitCasePattern(condition, data)
    }

    open fun visitPatternByType(element: CjTypePattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    open fun visitPatternByTuple(element: CjTuplePattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    open fun visitPatternByWildcard(element: CjWildcardPattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    open fun visitPatternByEnum(element: CjEnumPattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    open fun visitOptionType(optionType: CjOptionType, data: D): R? {
        return visitTypeElement(optionType, data)
    }

    open fun visitVArrayType(optionType: CjVArrayType, data: D): R? {
        return visitTypeElement(optionType, data)
    }

    open fun visitThisType(thisType: CjThisType, data: D): R? {
        return visitTypeElement(thisType, data)
    }

    open fun visitProperty(property: CjProperty, data: D): R? {
        return visitNamedDeclaration(property, data)
    }
    open fun visitPatternVariable(variable: CjPatternVariable, data: D): R? {
        return visitVariable(variable, data)
    }
    open fun visitFieldVariable(field: CjFieldVariable, data: D): R? {
        return visitVariable(field, data)
    }
    open fun visitVariable(variable: CjVariable<*>, data: D): R? {
        return visitNamedDeclaration(variable, data)
    }



    open fun visitCallableReferenceExpression(expression: CjCallableReferenceExpression, data: D): R? {
        return visitDoubleColonExpression(expression, data)
    }

    open fun visitDoubleColonExpression(expression: CjDoubleColonExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitRangeExpression(expression: CjRangeExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitSpawnExpression(expression: CjSpawnExpression, data: D): R? {
        return visitCallExpression(expression, data)
    }

    open fun visitLambdaExpression(expression: CjLambdaExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitUnsafeExpression(expression: CjUnsafeExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitTupleExpression(expression: CjTupleExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitTryResourceList(resource: CjTryResourceList, data: D): R? {
        return visitCjElement(resource, data)
    }

    open fun visitTryResource(resource: CjTryResource, data: D): R? {
        return visitCjElement(resource, data)
    }

    open fun visitTryExpression(expression: CjTryExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitCatchSection(catchClause: CjCatchClause, data: D): R? {
        return visitCjElement(catchClause, data)
    }

    open fun visitFinallySection(finallySection: CjFinallySection, data: D): R? {
        return visitCjElement(finallySection, data)
    }

    open fun visitMatchExpression(expression: CjMatchExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitLetExpression(expression: CjLetExpression, data: D): R? {
        return visitCjElement(expression, data)
    }

    open fun visitWhileExpression(expression: CjWhileExpression, data: D): R? {
        return visitLoopExpression(expression, data)
    }

    open fun visitDoWhileExpression(expression: CjDoWhileExpression, data: D): R? {
        return visitLoopExpression(expression, data)
    }

    open fun visitLoopExpression(loopExpression: CjLoopExpression, data: D): R? {
        return visitExpression(loopExpression, data)
    }

    open fun visitForExpression(expression: CjForExpression, data: D): R? {
        return visitLoopExpression(expression, data)
    }

    open fun visitIfExpression(expression: CjIfExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitContinueExpression(expression: CjContinueExpression, data: D): R? {
        return visitExpressionWithLabel(expression, data)
    }

    open fun visitBreakExpression(expression: CjBreakExpression, data: D): R? {
        return visitExpressionWithLabel(expression, data)
    }

    open fun visitCollectionLiteralExpression(expression: CjCollectionLiteralExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitThrowExpression(expression: CjThrowExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitReturnExpression(expression: CjReturnExpression, data: D): R? {
        return visitExpressionWithLabel(expression, data)
    }

    open fun visitParenthesizedExpression(expression: CjParenthesizedExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitStringTemplateExpression(expression: CjStringTemplateExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitSuperExpression(expression: CjSuperExpression, data: D): R? {
        return visitExpressionWithLabel(expression, data)
    }

    open fun visitThisExpression(expression: CjThisExpression, data: D): R? {
        return visitExpressionWithLabel(expression, data)
    }

    open fun visitExpressionWithLabel(expression: CjExpressionWithLabel, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitLiteralStringTemplateEntry(entry: CjLiteralStringTemplateEntry, data: D): R? {
        return visitStringTemplateEntry(entry, data)
    }

    open fun visitEscapeStringTemplateEntry(entry: CjEscapeStringTemplateEntry, data: D): R? {
        return visitStringTemplateEntry(entry, data)
    }

    open fun visitStringTemplateEntry(entry: CjStringTemplateEntry, data: D): R? {
        return visitCjElement(entry, data)
    }

    open fun visitStringTemplateEntryWithExpression(entry: CjStringTemplateEntryWithExpression, data: D): R? {
        return visitStringTemplateEntry(entry, data)
    }

    open fun visitBlockStringTemplateEntry(entry: CjBlockStringTemplateEntry, data: D): R? {
        return visitStringTemplateEntryWithExpression(entry, data)
    }

    open fun visitPostfixExpression(expression: CjPostfixExpression, data: D): R? {
        return visitUnaryExpression(expression, data)
    }

    open fun visitConstantExpression(expression: CjConstantExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitValueArgumentList(list: CjValueArgumentList, data: D): R? {
        return visitCjElement(list, data)
    }

    open fun visitArrayAccessExpression(expression: CjArrayAccessExpression, data: D): R? {
        return visitReferenceExpression(expression, data)
    }

    open fun visitSuperTypeEntry(specifier: CjSuperTypeEntry, data: D): R? {
        return visitSuperTypeListEntry(specifier, data)
    }

    open fun visitPrefixExpression(expression: CjPrefixExpression, data: D): R? {
        return visitUnaryExpression(expression, data)
    }

    open fun visitUnaryExpression(expression: CjUnaryExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitIsExpression(expression: CjIsExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitBinaryExpression(expression: CjBinaryExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry, data: D): R? {
        return visitSuperTypeListEntry(call, data)
    }

    open fun visitBinaryWithTypeRHSExpression(expression: CjBinaryExpressionWithTypeRHS, data: D): R? {
        return visitExpression(expression, data)
    }




    open fun visitSuperTypeListEntry(specifier: CjSuperTypeListEntry, data: D): R? {
        return visitCjElement(specifier, data)
    }

    open fun visitTypeProjection(typeProjection: CjTypeProjection, data: D): R? {
        return visitCjElement(typeProjection, data)
    }

    open fun visitTypeParameterList(list: CjTypeParameterList, data: D): R? {
        return visitCjElement(list, data)
    }

    open fun visitTypeParameter(parameter: CjTypeParameter, data: D): R? {
        return visitNamedDeclaration(parameter, data)
    }

    open fun visitCjFile(file: CjFile, data: D): R? {
        visitFile(file)
        return null
    }

    open fun visitBlockExpression(expression: CjBlockExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 访问表达式节点
     *
     * 这是所有表达式节点的基础访问方法，包括字面量、运算表达式、
     * 函数调用、控制流表达式等。
     *
     * @param expression 要访问的表达式
     * @param data 传递给访问器的上下文数据
     * @return 访问结果
     */
    open fun visitExpression(expression: CjExpression, data: D): R? {
        return visitCjElement(expression, data)
    }

    open fun visitClassBody(classBody: CjAbstractClassBody, data: D): R? {
        return visitCjElement(classBody, data)
    }

    open fun visitEnumBody(enumBody: CjEnumBody, data: D): R? {
        return visitCjElement(enumBody, data)
    }

    open fun visitModifierList(list: CjModifierList, data: D): R? {
        return visitCjElement(list, data)
    }



    open fun visitTypeReference(typeReference: CjTypeReference, data: D): R? {
        return visitCjElement(typeReference, data)
    }

    open fun visitSimpleNameExpression(expression: CjSimpleNameExpression, data: D): R? {
        return visitReferenceExpression(expression, data)
    }

    open fun visitReferenceExpression(expression: CjReferenceExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitTypeConstraintList(list: CjTypeConstraintList, data: D): R? {
        return visitCjElement(list, data)
    }

    open fun visitTypeConstraint(constraint: CjTypeConstraint, data: D): R? {
        return visitCjElement(constraint, data)
    }

    open fun visitParameterList(parameterList: CjParameterList, data: D): R? {
        return visitCjElement(parameterList, data)
    }

    open fun visitParameter(parameter: CjParameter, data: D): R? {
        return visitCjElement(parameter, data)
    }

    /**
     * 访问声明节点
     *
     * 这是所有声明节点的基础访问方法，包括类型声明、函数声明、
     * 变量声明等。
     *
     * @param dcl 要访问的声明
     * @param data 传递给访问器的上下文数据
     * @return 访问结果
     */
    open fun visitDeclaration(dcl: CjDeclaration, data: D): R? {
        return visitExpression(dcl, data)
    }

    open fun visitTypeAlias(typeAlias: CjTypeAlias, data: D): R? {
        return visitNamedDeclaration(typeAlias, data)
    }

    /**
     * 访问命名声明节点
     *
     * 命名声明是指所有具有名称标识符的声明，如类、函数、变量、
     * 类型参数等。
     *
     * @param declaration 要访问的命名声明
     * @param data 传递给访问器的上下文数据
     * @return 访问结果
     */
    open fun visitNamedDeclaration(declaration: CjNamedDeclaration, data: D): R? {
        return visitDeclaration(declaration, data)
    }

    open fun visitPrimaryConstructor(constructor: CjPrimaryConstructor, data: D): R? {
        return visitNamedDeclaration(constructor, data)
    }

    open fun visitMacroDeclaration(macroDeclaration: CjMacroDeclaration, data: D): R? {
        return visitNamedDeclaration(macroDeclaration, data)
    }

    open fun visitNamedFunction(function: CjNamedFunction, data: D): R? {
        return visitNamedDeclaration(function, data)
    }

    open fun visitCallExpression(expression: CjCallExpression, data: D): R? {
        return visitReferenceExpression(expression, data)
    }

    open fun visitEndSecondaryConstructor(constructor: CjEndSecondaryConstructor, data: D): R? {
        return visitNamedDeclaration(constructor, data)
    }

    open fun visitSecondaryConstructor(constructor: CjSecondaryConstructor, data: D): R? {
        return visitNamedDeclaration(constructor, data)
    }

    open fun visitConstructorDelegationCall(call: CjConstructorDelegationCall, data: D): R? {
        return visitCjElement(call, data)
    }

    open fun visitPackageDirective(packageDirective: CjPackageDirective, data: D): R? {
        return visitCjElement(packageDirective, data)
    }

    open fun visitQualifiedExpression(expression: CjQualifiedExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    open fun visitDotQualifiedExpression(expression: CjDotQualifiedExpression, data: D): R? {
        return visitQualifiedExpression(expression, data)
    }



    open fun visitImportItem(importItem: CjImportItem, data: D): R? {
        return visitCjElement(importItem, data)
    }

    open fun visitImportDirective(importDirective: CjImportDirective, data: D): R? {
        return visitCjElement(importDirective, data)
    }

    open fun visitUserType(type: CjUserType, data: D): R? {
        return visitCjElement(type, data)
    }

    open fun visitTypeArgumentList(typeArgumentList: CjTypeArgumentList, data: D): R? {
        return visitCjElement(typeArgumentList, data)
    }

    open fun visitImportAlias(importAlias: CjImportAlias, data: D): R? {
        return visitCjElement(importAlias, data)
    }

    open fun visitImportList(importList: CjImportList, data: D): R? {
        return visitCjElement(importList, data)
    }

    open fun visitSuperTypeList(list: CjSuperTypeList, data: D): R? {
        return visitCjElement(list, data)
    }

    open fun visitConstructorCalleeExpression(
        constructorCalleeExpression: CjConstructorCalleeExpression,
        data: D
    ): R? {
        return visitCjElement(constructorCalleeExpression, data)
    }

    open fun visitBasicType(basicType: CjBasicType, data: D): R? {
        return visitCjElement(basicType, data)
    }

    open fun visitKeyword(keyword: CjKeyword, data: D): R? {
        return visitCjElement(keyword, data)
    }

    open fun visitSimpleNameStringTemplateEntry(entry: CjSimpleNameStringTemplateEntry, data: D): R? {
        return visitStringTemplateEntryWithExpression(entry, data)
    }

    open fun visitEnumConstructor(enumConstructor: CjEnumConstructor, data: D): R? {
        return visitCjElement(enumConstructor, data)
    }

    open fun visitMatchEntry(matchEntry: CjMatchEntry, data: D): R? {
        return visitCjElement(matchEntry, data)
    }

    open fun visitTupleType(tupleType: CjTupleType, data: D): R? {
        return visitCjElement(tupleType, data)
    }

    open fun visitParenthesizedType(parenthesizedType: CjParenthesizedType, data: D): R? {
        return visitCjElement(parenthesizedType, data)
    }

    open fun visitFunctionType(type: CjFunctionType, data: D): R? {
        return visitTypeElement(type, data)
    }

    /**
     * 访问类型元素节点
     *
     * 类型元素表示类型表达式，如函数类型、可选类型、可变数组类型等。
     *
     * @param type 要访问的类型元素
     * @param data 传递给访问器的上下文数据
     * @return 访问结果
     */
    protected open fun visitTypeElement(type: CjTypeElement, data: D): R? {
        return visitCjElement(type, data)
    }

    open fun visitPropertyAccessor(accessor: CjPropertyAccessor, data: D): R? {
        return visitDeclaration(accessor, data)
    }

    open fun visitExtend(extend: CjExtend, data: D): R? {
        return visitTypeStatement(extend, data)
    }

    open fun visitClass(cclass: CjClass, data: D): R? {
        return visitTypeStatement(cclass, data)
    }

    open fun visitEnum(cenum: CjEnum, data: D): R? {
        return visitTypeStatement(cenum, data)
    }

    open fun visitStruct(cstruct: CjStruct, data: D): R? {
        return visitTypeStatement(cstruct, data)
    }

    open fun visitInterface(cinterface: CjInterface, data: D): R? {
        return visitTypeStatement(cinterface, data)
    }

    /**
     * 访问类型声明节点
     *
     * 类型声明包括类（class）、接口（interface）、结构体（struct）、
     * 枚举（enum）、扩展（extend）等顶层类型定义。
     *
     * @param typeStatement 要访问的类型声明
     * @param data 传递给访问器的上下文数据
     * @return 访问结果
     */
    open fun visitTypeStatement(typeStatement: CjTypeStatement, data: D): R? {
        return visitNamedDeclaration(typeStatement, data)
    }

    open fun visitAnnotation(annotation: CjAnnotations, data: D): R? {
        return visitCjElement(annotation, data)
    }

    open fun visitMainFunction(mainFunction: CjMainFunction, data: D): R? {
        return visitDeclaration(mainFunction, data)
    }
} 