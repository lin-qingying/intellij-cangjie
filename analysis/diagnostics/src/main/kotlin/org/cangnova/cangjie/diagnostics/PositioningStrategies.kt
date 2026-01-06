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

package org.cangnova.cangjie.diagnostics


import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.lexer.CjModifierKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.*

/**
 * 诊断定位策略集合
 *
 * 这是一个单例对象，包含了所有预定义的诊断定位策略。
 * 每个策略都针对特定类型的 PSI 元素和诊断场景，精确控制编辑器中错误/警告标记的显示位置。
 *
 * ## 主要用途
 * - **精确错误定位**：将编译器错误标记到代码的准确位置（如只标记函数名、只标记修饰符等）
 * - **提升用户体验**：避免整行标红，只标记问题的核心部分
 * - **快速修复支持**：为 IDE 的快速修复功能提供精确的目标范围
 *
 * ## 策略分类
 * - **声明相关**：DECLARATION_NAME, DECLARATION_SIGNATURE 等
 * - **修饰符相关**：VISIBILITY_MODIFIER, OVERRIDE_MODIFIER, ABSTRACT_MODIFIER 等
 * - **表达式相关**：CALL_EXPRESSION, ARRAY_ACCESS, SAFE_ACCESS 等
 * - **类型相关**：OPTIONAL_TYPE, DECLARATION_RETURN_TYPE 等
 * - **导入相关**：IMPORT_ALIAS 等
 *
 * @see PositioningStrategy 定位策略基类
 */
object PositioningStrategies {

    /**
     * 声明头部定位策略基类
     *
     * 用于标记声明（如函数、类、属性等）的头部区域。
     * 此策略会验证声明的有效性，确保命名声明具有名称标识符。
     *
     * @param T 声明类型，必须继承自 [CjDeclaration]
     */
    open class DeclarationHeader<T : CjDeclaration> : PositioningStrategy<T>() {
        override fun isValid(element: T): Boolean {
            if (element is CjNamedDeclaration &&
                element !is CjSecondaryConstructor &&
                element !is CjFunction
            ) {
                if(element is CjPatternVariable) return true
                if (element.nameIdentifier == null) {
                    return false
                }
            }
            return super.isValid(element)
        }
    }

    /**
     * 导入别名定位策略
     *
     * 用于标记 import 语句中的别名或导入的引用。
     * 优先级：别名标识符 > 选择器表达式 > 导入引用 > 整个元素
     *
     * @sample
     * ```kotlin
     * import foo.bar.MyClass as MC  // 标记 "MC"
     * import foo.bar.MyClass        // 标记 "MyClass"
     * ```
     */
    
    val IMPORT_ALIAS: PositioningStrategy<CjImportItem> =
        object : PositioningStrategy<CjImportItem>() {
            override fun mark(element: CjImportItem): List<TextRange> {
                element.alias?.nameIdentifier?.let { return markElement(it) }
                element.importedReference?.let {
                    if (it is CjQualifiedExpression) {
                        it.selectorExpression?.let { return markElement(it) }
                    }
                    return markElement(it)
                }
                return markElement(element)
            }
        }

    /**
     * 无用的 Elvis 操作符定位策略
     *
     * 用于标记不必要的 Elvis 操作符（?:）及其右侧表达式。
     * 从操作符位置开始标记到表达式结束。
     *
     * @sample
     * ```kotlin
     * val x = nonNullValue ?: defaultValue  // 标记 "?: defaultValue"
     * ```
     */
    
    val USELESS_ELVIS: PositioningStrategy<CjBinaryExpression> = object : PositioningStrategy<CjBinaryExpression>() {
        override fun mark(element: CjBinaryExpression): List<TextRange> {
            return listOf(TextRange(element.operationReference.startOffset, element.endOffset))
        }
    }

    /**
     * 数组访问定位策略
     *
     * 用于标记数组访问表达式中的索引部分（包括方括号和索引表达式）。
     *
     * @sample
     * ```kotlin
     * array[0]      // 标记 "[0]"
     * matrix[i][j]  // 标记 "[i]" 或 "[j]"，取决于错误位置
     * ```
     */
    
    val ARRAY_ACCESS: PositioningStrategy<CjArrayAccessExpression> =
        object : PositioningStrategy<CjArrayAccessExpression>() {
            override fun mark(element: CjArrayAccessExpression): List<TextRange> {
                return markElement(element.indicesNode)
            }
        }

    /**
     * 带标签的 return 定位策略
     *
     * 用于标记 return 语句，如果有标签则标记从 return 到标签，否则只标记 return 关键字。
     *
     * @sample
     * ```kotlin
     * return@label value  // 标记 "return@label"
     * return value        // 标记 "return"
     * ```
     */
    
    val RETURN_WITH_LABEL: PositioningStrategy<CjReturnExpression> =
        object : PositioningStrategy<CjReturnExpression>() {
            override fun mark(element: CjReturnExpression): List<TextRange> {
                val labeledExpression = element.labeledExpression
                if (labeledExpression != null) {
                    return markRange(element, labeledExpression)
                }

                return markElement(element.returnKeyword)
            }
        }

    /**
     * 投影位置定位策略工厂方法
     *
     * 创建用于标记泛型投影修饰符（in/out）的定位策略。
     * 注意：当前实现会抛出异常，因为未找到对应的修饰符。
     */
    
    fun projectionPosition(): PositioningStrategy<CjModifierListOwner> {
        return object : PositioningStrategy<CjModifierListOwner>() {
            override fun mark(element: CjModifierListOwner): List<TextRange> {
                throw IllegalStateException("None of the modifiers is found: in, out")
            }
        }
    }

    /**
     * 型变修饰符定位策略
     *
     * 用于标记泛型类型参数的型变修饰符（in/out）。
     */
    
    val VARIANCE_MODIFIER: PositioningStrategy<CjModifierListOwner> = projectionPosition()

    /**
     * open 修饰符定位策略
     *
     * 用于标记 open 关键字，表示类或成员可以被继承或重写。
     */
    
    val OPEN_MODIFIER: PositioningStrategy<CjModifierListOwner> =
        ModifierSetBasedPositioningStrategy(CjTokens.OPEN_KEYWORD)

    /**
     * 声明签名定位策略
     *
     * 用于标记整个声明的签名部分（不包括函数体）。
     * 适用于函数、类、属性等各种声明。
     */
    
    val DECLARATION_SIGNATURE: PositioningStrategy<CjDeclaration> = object : DeclarationHeader<CjDeclaration>() {

    }

    /**
     * private 修饰符定位策略
     *
     * 用于标记 private 可见性修饰符。
     */
    
    val PRIVATE_MODIFIER: PositioningStrategy<CjModifierListOwner> =
        ModifierSetBasedPositioningStrategy(CjTokens.PRIVATE_KEYWORD)

    /**
     * abstract 修饰符定位策略
     *
     * 用于标记 abstract 关键字，表示抽象类或抽象成员。
     */
    
    val ABSTRACT_MODIFIER: PositioningStrategy<CjModifierListOwner> =
        ModifierSetBasedPositioningStrategy(CjTokens.ABSTRACT_KEYWORD)

    /**
     * 字符常量去引号定位策略
     *
     * 用于标记字符常量时去除单引号，只标记实际的字符内容。
     *
     * @sample
     * ```kotlin
     * 'a'  // 标记 "a" 而不是 "'a'"
     * ```
     */
    
    val CUT_CHAR_QUOTES: PositioningStrategy<CjElement> = object : PositioningStrategy<CjElement>() {
        override fun mark(element: CjElement): List<TextRange> {
            if (element is CjConstantExpression) {
                if (element.node.elementType == CjNodeTypes.RUNE_CONSTANT) {
                    val elementTextRange = element.getTextRange()
                    return listOf(TextRange.create(elementTextRange.startOffset + 1, elementTextRange.endOffset - 1))
                }
            }
            return markElement(element)
        }
    }

    /**
     * 安全访问操作符定位策略
     *
     * 用于标记安全调用操作符（?.）。
     *
     * @sample
     * ```kotlin
     * obj?.method()  // 标记 "?."
     * ```
     */
    
    val SAFE_ACCESS: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return markElement(element.node.findChildByType(CjTokens.SAFE_ACCESS)?.psi ?: element)
        }
    }

    /**
     * 限定表达式选择器定位策略
     *
     * 用于标记限定表达式中的选择器部分（点号后面的部分）。
     * 处理多种情况：赋值表达式、限定表达式、导入、类型引用等。
     *
     * @sample
     * ```kotlin
     * obj.property     // 标记 "property"
     * pkg.Class.method // 标记 "method"
     * ```
     */
    val SELECTOR_BY_QUALIFIED: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is CjBinaryExpression && element.operationToken in CjTokens.ALL_ASSIGNMENTS) {
                element.left?.let { return mark(it) }
            }
            if (element is CjQualifiedExpression) {
                when (val selectorExpression = element.selectorExpression) {
                    is CjElement -> return mark(selectorExpression)
                }
            }
            if (element is CjImportItem) {
                element.alias?.nameIdentifier?.let { return mark(it) }
                element.importedReference?.let { return mark(it) }
            }
            if (element is CjTypeReference) {
                element.typeElement?.getReferencedTypeExpression()?.let { return mark(it) }
            }
            return super.mark(element)
        }
    }

    /**
     * 获取类型元素引用的类型表达式
     *
     * 从类型元素中提取实际的类型引用表达式。
     * 处理用户自定义类型和可选类型。
     */
    private fun CjTypeElement.getReferencedTypeExpression(): CjElement? {
        return when (this) {
            is CjUserType -> referenceExpression
            is CjOptionType -> getInnerType()?.getReferencedTypeExpression()
            else -> null
        }
    }

    /**
     * 带点号的调用元素定位策略
     *
     * 用于标记从点号到调用表达式结束的范围，适用于方法调用。
     *
     * @sample
     * ```kotlin
     * obj.method()  // 标记 ".method()"
     * ```
     */
    
    val CALL_ELEMENT_WITH_DOT: PositioningStrategy<CjQualifiedExpression> =
        object : PositioningStrategy<CjQualifiedExpression>() {
            override fun mark(element: CjQualifiedExpression): List<TextRange> {
                val callElementRanges = SELECTOR_BY_QUALIFIED.mark(element)
                val callElementRange = when (callElementRanges.size) {
                    1 -> callElementRanges.first()
                    else -> return callElementRanges
                }

                val dotRanges = SAFE_ACCESS.mark(element)
                val dotRange = when (dotRanges.size) {
                    1 -> dotRanges.first()
                    else -> return dotRanges
                }

                return listOf(TextRange(dotRange.startOffset, callElementRange.endOffset))
            }
        }

    /**
     * 声明签名或默认定位策略
     *
     * 智能选择定位策略：如果是声明则使用声明签名策略，否则使用默认策略。
     */
    
    val DECLARATION_SIGNATURE_OR_DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return if (element is CjDeclaration)
                DECLARATION_SIGNATURE.mark(element)
            else
                DEFAULT.mark(element)
        }

        override fun isValid(element: PsiElement): Boolean {
            return if (element is CjDeclaration)
                DECLARATION_SIGNATURE.isValid(element)
            else
                DEFAULT.isValid(element)
        }
    }

    /**
     * 重复声明定位策略
     *
     * 用于标记重复声明错误，优先标记名称标识符。
     * 适用于命名声明和包声明。
     */
    
    val FOR_REDECLARATION: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val nameIdentifier = when (element) {
                is CjNamedDeclaration -> element.nameIdentifier
                is CjFile -> element.packageDirective!!.nameIdentifier
                else -> null
            }

            return markElement(nameIdentifier ?: element)
        }
    }

    /**
     * 调用表达式定位策略
     *
     * 用于标记函数调用，包括函数名和类型参数（如果有）。
     *
     * @sample
     * ```kotlin
     * function<T>()  // 标记 "function<T>"
     * function()     // 标记 "function"
     * ```
     */
    
    val CALL_EXPRESSION: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is CjCallExpression) {
                return markRange(element, element.typeArgumentList ?: element.calleeExpression ?: element)
            }
            return markElement(element)
        }
    }

    /**
     * 可见性修饰符定位策略
     *
     * 用于标记可见性修饰符（public、private、protected、internal 等）。
     */
    
    val VISIBILITY_MODIFIER: PositioningStrategy<CjModifierListOwner> =
        ModifierSetBasedPositioningStrategy(CjTokens.VISIBILITY_MODIFIERS)

    /**
     * 参数默认值定位策略
     *
     * 用于标记函数参数的默认值表达式。
     *
     * @sample
     * ```kotlin
     * func foo(x: Int = 42)  // 标记 "42"
     * ```
     */
    
    val PARAMETER_DEFAULT_VALUE: PositioningStrategy<CjParameter> = object : PositioningStrategy<CjParameter>() {
        override fun mark(element: CjParameter): List<TextRange> {
            return markNode(element.defaultValue!!.node)
        }
    }

    /**
     * let/var 关键字定位策略
     *
     * 用于标记变量声明中的 let 或 var 关键字。
     * 适用于参数、属性、变量和解构声明。
     *
     * @sample
     * ```kotlin
     * let x = 10     // 标记 "let"
     * var y = 20     // 标记 "var"
     * ```
     */
    
    val LET_OR_VAR_NODE: PositioningStrategy<CjDeclaration> = object : PositioningStrategy<CjDeclaration>() {
        override fun mark(element: CjDeclaration): List<TextRange> {
            return when (element) {
                is CjParameter -> markElement(element.letOrVarKeyword ?: element)
                is CjProperty -> markElement(element.letOrVarKeyword ?: element)
                is CjVariable<*> -> markElement(element.letOrVarKeyword ?: element)
                else -> error("Declaration is neither a parameter nor a property: " + element.getElementTextWithContext())
            }
        }
    }

    /**
     * override 修饰符定位策略
     *
     * 用于标记 override 或 redef 关键字，表示重写父类成员。
     */
    
    val OVERRIDE_MODIFIER: PositioningStrategy<CjModifierListOwner> =
        ModifierSetBasedPositioningStrategy(CjTokens.OVERRIDE_KEYWORD, CjTokens.REDEF_KEYWORD)

    /**
     * 声明返回类型定位策略
     *
     * 用于标记声明的返回类型，适用于函数和属性访问器。
     * 优先级：返回类型引用 > 名称标识符 > 整个声明
     *
     * @sample
     * ```kotlin
     * func foo(): Int { }  // 标记 "Int"
     * get(): String { }    // 标记 "String"
     * ```
     */
    
    val DECLARATION_RETURN_TYPE: PositioningStrategy<CjDeclaration> = object : PositioningStrategy<CjDeclaration>() {
        override fun mark(element: CjDeclaration): List<TextRange> {
            return markElement(getElementToMark(element))
        }

        override fun isValid(element: CjDeclaration): Boolean {
            return !hasSyntaxErrors(getElementToMark(element))
        }

        private fun getElementToMark(declaration: CjDeclaration): PsiElement {
            val (returnTypeRef, nameIdentifierOrPlaceholder) = when (declaration) {
                is CjCallableDeclaration -> Pair(declaration.typeReference, declaration.nameIdentifier)
                is CjPropertyAccessor -> Pair(declaration.returnTypeReference, declaration.namePlaceholder)
                else -> Pair(null, null)
            }

            if (returnTypeRef != null) return returnTypeRef
            if (nameIdentifierOrPlaceholder != null) return nameIdentifierOrPlaceholder
            return declaration
        }
    }

    /**
     * 可选类型定位策略
     *
     * 用于标记可选类型的问号（?），只标记问号而不标记整个类型。
     *
     * @sample
     * ```kotlin
     * var x: String?  // 标记 "?"
     * ```
     */
    
    val OPTIONAL_TYPE: PositioningStrategy<CjOptionType> = object : PositioningStrategy<CjOptionType>() {
        override fun mark(element: CjOptionType): List<TextRange> {
            return markNode(element.getQuestionMarkNode())
        }
    }

    /**
     * 类型参数或声明签名定位策略
     *
     * 优先标记类型参数列表，如果没有则标记整个声明签名。
     * 用于泛型相关的诊断。
     *
     * @sample
     * ```kotlin
     * class Foo<T, U> { }  // 标记 "<T, U>"
     * func bar() { }       // 标记整个函数签名
     * ```
     */
    
    val TYPE_PARAMETERS_OR_DECLARATION_SIGNATURE: PositioningStrategy<CjDeclaration> =
        object : PositioningStrategy<CjDeclaration>() {
            override fun mark(element: CjDeclaration): List<TextRange> {
                if (element is CjTypeParameterListOwner) {
                    val cjTypeParameterList = element.typeParameterList
                    if (cjTypeParameterList != null) {
                        return markElement(cjTypeParameterList)
                    }
                }
                return DECLARATION_SIGNATURE.mark(element)
            }
        }

    /**
     * 带函数体的声明定位策略
     *
     * 用于标记函数体的最后一个大括号，常用于缺少返回值的错误提示。
     *
     * @sample
     * ```kotlin
     * func foo(): Int {
     *     // 缺少 return
     * }  // 标记这个 "}"
     * ```
     */
    
    val DECLARATION_WITH_BODY: PositioningStrategy<CjDeclarationWithBody> =
        object : PositioningStrategy<CjDeclarationWithBody>() {
            override fun mark(element: CjDeclarationWithBody): List<TextRange> {
                val lastBracketRange = element.bodyBlockExpression?.lastBracketRange
                return if (lastBracketRange != null)
                    markRange(lastBracketRange)
                else
                    markElement(element)
            }

            override fun isValid(element: CjDeclarationWithBody): Boolean {
                return super.isValid(element) && element.bodyBlockExpression?.lastBracketRange != null
            }
        }

    /**
     * 声明名称定位策略
     *
     * 用于标记声明的名称，特殊处理类型声明（包含关键字和名称）。
     *
     * @sample
     * ```kotlin
     * class MyClass { }      // 标记 "class MyClass"
     * func myFunction() { }  // 标记 "myFunction"
     * var myProperty = 0     // 标记 "myProperty"
     * ```
     */
    
    val DECLARATION_NAME: PositioningStrategy<CjNamedDeclaration> = object : DeclarationHeader<CjNamedDeclaration>() {
        override fun mark(element: CjNamedDeclaration): List<TextRange> {
            val nameIdentifier = element.nameIdentifier
            if (nameIdentifier != null) {
                if (element is CjTypeStatement) {
                    val startElement =
                        element.modifierList?.getModifier(CjTokens.ENUM_KEYWORD)
                            ?: element.node.findChildByType(
                                TokenSet.create(
                                    CjTokens.CLASS_KEYWORD,
                                    CjTokens.STRUCT_KEYWORD
                                )
                            )?.psi
                            ?: element

                    return markRange(startElement, nameIdentifier)
                }
                return markElement(nameIdentifier)
            }
            if (element is CjNamedFunction) {
                return DECLARATION_SIGNATURE.mark(element)
            }
            return DEFAULT.mark(element)
        }
    }

    /**
     * else 分支定位策略
     *
     * 用于标记 match 表达式中的 else 关键字。
     *
     * @sample
     * ```kotlin
     * match (value) {
     *     case 1 -> println("1")
     *     else -> println("other")  // 标记 "else"
     * }
     * ```
     */
    
    val ELSE_ENTRY: PositioningStrategy<CjMatchEntry> = object : PositioningStrategy<CjMatchEntry>() {
        override fun mark(element: CjMatchEntry): List<TextRange> {
            return markElement(element.elseKeyword!!)
        }
    }

    /**
     * match 表达式定位策略
     *
     * 用于标记 match 关键字。
     *
     * @sample
     * ```kotlin
     * match (value) {  // 标记 "match"
     *     case 1 -> println("1")
     * }
     * ```
     */
    
    val MATCH_EXPRESSION: PositioningStrategy<CjMatchExpression> = object : PositioningStrategy<CjMatchExpression>() {
        override fun mark(element: CjMatchExpression): List<TextRange> {
            return markElement(element.matchKeyword)
        }
    }

    /**
     * 值参数定位策略
     *
     * 用于标记函数调用的参数列表，智能处理不同情况。
     * 处理赋值表达式、限定表达式、类型声明等多种场景。
     *
     * @sample
     * ```kotlin
     * foo(a, b, c)  // 标记 "c)"（最后一个参数到右括号）
     * foo()         // 标记 "()"
     * ```
     */
    
    val VALUE_ARGUMENTS: PositioningStrategy<CjElement> = object : PositioningStrategy<CjElement>() {
        override fun mark(element: CjElement): List<TextRange> {
            if (element is CjBinaryExpression && element.operationToken in CjTokens.ALL_ASSIGNMENTS) {
                element.left.let { left ->
                    left.unwrapParenthesesLabelsAndAnnotations()?.let { return markElement(it) }
                }
            }
            val qualifiedAccess = when (element) {
                is CjQualifiedExpression -> element.selectorExpression ?: element
                is CjTypeStatement -> element.getSuperTypeList() ?: element
                else -> element
            }
            val argumentList = qualifiedAccess as? CjValueArgumentList
                ?: qualifiedAccess.getChildOfType()
            return when {
                argumentList != null -> {
                    val rightParenthesis = argumentList.rightParenthesis ?: return markElement(qualifiedAccess)
                    val lastArgument = argumentList.children.findLast { it is CjValueArgument }
                    if (lastArgument != null) {
                        markRange(lastArgument, rightParenthesis)
                    } else {
                        val leftParenthesis = argumentList.leftParenthesis
                        markRange(leftParenthesis ?: qualifiedAccess, rightParenthesis)
                    }
                }

                qualifiedAccess is CjCallExpression -> markElement(
                    qualifiedAccess.getChildOfType<CjNameReferenceExpression>() ?: qualifiedAccess
                )

                else -> markElement(qualifiedAccess)
            }
        }
    }

    /**
     * 次构造函数委托调用定位策略
     *
     * 用于标记次构造函数中的委托调用（this() 或 super()）。
     * 如果没有显式委托调用，则标记构造函数头部。
     *
     * @sample
     * ```kotlin
     * class Foo {
     *     init() { }
     *     init(x: Int) : this() { }  // 标记 "this()"
     * }
     * ```
     */
    
    val SECONDARY_CONSTRUCTOR_DELEGATION_CALL: PositioningStrategy<PsiElement> =
        object : PositioningStrategy<PsiElement>() {
            override fun mark(element: PsiElement): List<TextRange> {
                return when (element) {
                    is CjSecondaryConstructor -> {
                        val valueParameterList = element.valueParameterList ?: return markElement(element)
                        markRange(element.getConstructorKeyword(), valueParameterList.lastChild)
                    }

                    is CjConstructorDelegationCall -> {
                        if (element.isImplicit) {
                            // TODO: [VD] FIR collects for some reason implicit CjConstructorDelegationCall
                            // check(!element.isImplicit) { "Implicit CjConstructorDelegationCall should not be collected directly" }
                            val constructor = element.getStrictParentOfType<CjSecondaryConstructor>()!!
                            val valueParameterList = constructor.valueParameterList ?: return markElement(constructor)
                            return markRange(constructor.getConstructorKeyword(), valueParameterList.lastChild)
                        }
                        markElement(element.calleeExpression ?: element)
                    }

                    else -> markElement(element)
                }
            }
        }

    /**
     * 类型投影中的型变定位策略
     *
     * 用于标记类型参数的型变修饰符（in/out）。
     *
     * @sample
     * ```kotlin
     * List<out T>  // 标记 "out"
     * List<in T>   // 标记 "in"
     * ```
     */
    
    val VARIANCE_IN_PROJECTION: PositioningStrategy<CjTypeProjection> =
        object : PositioningStrategy<CjTypeProjection>() {
            override fun mark(element: CjTypeProjection): List<TextRange> {
                return markElement(element.projectionToken!!)
            }
        }

    /**
     * 调用元素定位策略
     *
     * 用于标记函数调用的被调用表达式（函数名部分）。
     *
     * @sample
     * ```kotlin
     * foo()        // 标记 "foo"
     * obj.bar()    // 标记 "bar"
     * ```
     */
    
    val CALL_ELEMENT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return markElement((element as? CjCallElement)?.calleeExpression ?: element)
        }
    }

    /**
     * 未解析引用定位策略
     *
     * 用于标记无法解析的引用，特殊处理数组访问表达式。
     *
     * @sample
     * ```kotlin
     * undefinedVariable  // 标记整个变量名
     * array[index]       // 标记 "[index]"
     * ```
     */
    
    val FOR_UNRESOLVED_REFERENCE: PositioningStrategy<CjReferenceExpression> =
        object : PositioningStrategy<CjReferenceExpression>() {
            override fun mark(element: CjReferenceExpression): List<TextRange> {
                if (element is CjArrayAccessExpression) {
                    val ranges = element.bracketRanges
                    if (ranges.isNotEmpty()) {
                        return ranges
                    }
                }
                return listOf(element.textRange)
            }
        }

    /**
     * 默认定位策略
     *
     * 当没有更具体的定位策略时使用。
     * 标记整个元素，排除前后的空白和注释。
     */
    
    val DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            when (element) {
                else -> {
                    return super.mark(element)
                }
            }
        }
    }

    /**
     * 基于修饰符集合的定位策略
     *
     * 这是一个私有基类，用于创建针对特定修饰符关键字的定位策略。
     * 如果找到目标修饰符则标记修饰符本身，否则标记名称标识符或整个元素。
     *
     * @property modifierSet 要查找的修饰符集合
     */
    private open class ModifierSetBasedPositioningStrategy(private val modifierSet: TokenSet) :
        PositioningStrategy<CjModifierListOwner>() {
        /**
         * 便利构造函数，接受可变数量的 token 类型
         */
        constructor(vararg tokens: IElementType) : this(TokenSet.create(*tokens))

        /**
         * 标记指定元素中的修饰符
         *
         * @return 如果找到修饰符返回其文本范围列表，否则返回 null
         */
        protected fun markModifier(element: CjModifierListOwner?): List<TextRange>? =
            modifierSet.types.mapNotNull {
                element?.modifierList?.getModifier(it as CjModifierKeywordToken)?.textRange
            }.takeIf { it.isNotEmpty() }

        override fun mark(element: CjModifierListOwner): List<TextRange> {
            val result = markModifier(element)
            if (result != null) return result

            // 尝试处理元素前没有写可见性修饰符的情况
            if (element is PsiNameIdentifierOwner) {
                val nameIdentifier = element.nameIdentifier
                if (nameIdentifier != null) {
                    return markElement(nameIdentifier)
                }
            }

            val elementToMark = when (element) {
                is CjPropertyAccessor -> element.namePlaceholder
          is CjPrimaryConstructor -> element
                else -> throw IllegalArgumentException(
                    "Can't find text range for element '${element::class.java.canonicalName}' with the text '${element.text}'"
                )
            }
            return markElement(elementToMark)
        }
    }

}

/**
 * 标记 AST 节点的文本范围
 *
 * 这是一个便利函数，将 AST 节点转换为 PSI 元素后标记其范围。
 *
 * @param node 要标记的 AST 节点
 * @return 标记的文本范围列表
 */
fun markNode(node: ASTNode): List<TextRange> {
    return markElement(node.psi)
}
