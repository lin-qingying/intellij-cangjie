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

package cn.cangnova.cangjie.diagnostics

import cn.cangnova.cangjie.diagnostics.PositioningStrategy
import cn.cangnova.cangjie.diagnostics.hasSyntaxErrors
import cn.cangnova.cangjie.diagnostics.markElement
import cn.cangnova.cangjie.diagnostics.markRange
import cn.cangnova.cangjie.lexer.CjModifierKeywordToken
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

/**
 * 定位策略工具类
 * 
 * 提供用于标记诊断位置的各种策略，用于在编辑器中准确地高亮显示错误或警告
 */
object PositioningStrategies {

    /**
     * 声明头部定位策略
     * 
     * 用于定位声明的头部部分，通常包括修饰符、名称和类型引用
     *
     * @param T 声明类型
     */
    open class DeclarationHeader<T : CjDeclaration> : PositioningStrategy<T>() {
        override fun isValid(element: T): Boolean {
            if (element is CjNamedDeclaration &&

                element !is CjSecondaryConstructor &&
                element !is CjFunction
            ) {
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
     * 用于定位导入语句中的别名部分
     */
    @JvmField
    val IMPORT_ALIAS: PositioningStrategy<CjImportDirectiveItem> = object : PositioningStrategy<CjImportDirectiveItem>() {
        override fun mark(element: CjImportDirectiveItem): List<TextRange> {
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
     * 无用Elvis运算符定位策略
     * 
     * 用于定位不必要的Elvis运算符(?:)
     */
    @JvmField
    val USELESS_ELVIS: PositioningStrategy<CjBinaryExpression> = object : PositioningStrategy<CjBinaryExpression>() {
        override fun mark(element: CjBinaryExpression): List<TextRange> {
            return listOf(TextRange(element.operationReference.startOffset, element.endOffset))
        }
    }

    /**
     * 数组访问定位策略
     * 
     * 用于定位数组访问表达式中的索引部分
     */
    @JvmField
    val ARRAY_ACCESS: PositioningStrategy<CjArrayAccessExpression> = object : PositioningStrategy<CjArrayAccessExpression>() {
        override fun mark(element: CjArrayAccessExpression): List<TextRange> {
            return markElement(element.indicesNode)
        }
    }
    
    /**
     * 带标签的返回表达式定位策略
     * 
     * 用于定位带标签的return表达式
     */
    @JvmField
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
     * 创建投影位置定位策略
     * 
     * 用于定位类型参数投影修饰符（in、out）
     *
     * @return 用于定位投影修饰符的策略
     */
    @JvmStatic
    fun projectionPosition(): PositioningStrategy<CjModifierListOwner> {
        return object : PositioningStrategy<CjModifierListOwner>() {
            override fun mark(element: CjModifierListOwner): List<TextRange> {


                throw IllegalStateException("None of the modifiers is found: in, out")
            }
        }
    }

    /**
     * 变型修饰符定位策略
     * 
     * 用于定位类型参数的变型修饰符（in、out）
     */
    @JvmField
    val VARIANCE_MODIFIER: PositioningStrategy<CjModifierListOwner> = projectionPosition()
    
    /**
     * open修饰符定位策略
     * 
     * 用于定位声明中的open修饰符
     */
    @JvmField
    val OPEN_MODIFIER: PositioningStrategy<CjModifierListOwner> =
        ModifierSetBasedPositioningStrategy(CjTokens.OPEN_KEYWORD)

    /**
     * 声明签名定位策略
     * 
     * 用于定位声明的签名部分
     */
    @JvmField
    val DECLARATION_SIGNATURE: PositioningStrategy<CjDeclaration> = object : DeclarationHeader<CjDeclaration>() {

    }
    
    /**
     * private修饰符定位策略
     * 
     * 用于定位声明中的private修饰符
     */
    @JvmField
    val PRIVATE_MODIFIER: PositioningStrategy<CjModifierListOwner> =
        ModifierSetBasedPositioningStrategy(CjTokens.PRIVATE_KEYWORD)

    /**
     * abstract修饰符定位策略
     * 
     * 用于定位声明中的abstract修饰符
     */
    @JvmField
    val ABSTRACT_MODIFIER: PositioningStrategy<CjModifierListOwner> =
        ModifierSetBasedPositioningStrategy(CjTokens.ABSTRACT_KEYWORD)

    /**
     * 字符引号剪切定位策略
     * 
     * 用于定位字符常量，但不包括引号
     */
    @JvmField
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
     * 安全访问定位策略
     * 
     * 用于定位安全访问运算符(?.)
     */
    @JvmField
    val SAFE_ACCESS: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return markElement(element.node.findChildByType(CjTokens.SAFE_ACCESS)?.psi ?: element)
        }
    }
    
    /**
     * 限定表达式选择器定位策略
     * 
     * 用于定位限定表达式中的选择器部分
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
            if (element is CjImportDirectiveItem) {
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
     * 获取类型元素引用的表达式
     *
     * @return 引用的类型表达式
     */
    private fun CjTypeElement.getReferencedTypeExpression(): CjElement? {
        return when (this) {
            is CjUserType -> referenceExpression
            is CjOptionType -> getInnerType()?.getReferencedTypeExpression()
            else -> null
        }
    }

    /**
     * 带点的调用元素定位策略
     * 
     * 用于定位包含点运算符的调用表达式
     */
    @JvmField
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
     * 用于定位声明的签名，如果不是声明则使用默认策略
     */
    @JvmField
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
     * 重声明定位策略
     * 
     * 用于定位重复声明的元素
     */
    @JvmField
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
     * 用于定位函数调用表达式
     */
    @JvmField
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
     * 用于定位声明的可见性修饰符（如private、public等）
     */
    @JvmField
    val VISIBILITY_MODIFIER: PositioningStrategy<CjModifierListOwner> =
        ModifierSetBasedPositioningStrategy(CjTokens.VISIBILITY_MODIFIERS)

    /**
     * 参数默认值定位策略
     * 
     * 用于定位函数参数的默认值
     */
    @JvmField
    val PARAMETER_DEFAULT_VALUE: PositioningStrategy<CjParameter> = object : PositioningStrategy<CjParameter>() {
        override fun mark(element: CjParameter): List<TextRange> {
            return markNode(element.defaultValue!!.node)
        }
    }

    /**
     * let或var关键字定位策略
     * 
     * 用于定位变量声明中的let或var关键字
     */
    @JvmField
    val LET_OR_VAR_NODE: PositioningStrategy<CjDeclaration> = object : PositioningStrategy<CjDeclaration>() {
        override fun mark(element: CjDeclaration): List<TextRange> {
            return when (element) {
                is CjParameter -> markElement(element.letOrVarKeyword ?: element)
                is CjProperty -> markElement(element.letOrVarKeyword ?: element)
                is CjVariable -> markElement(element.letOrVarKeyword)
                is CjDestructuringDeclaration -> markElement(element.letOrVarKeyword ?: element)
                else -> error("Declaration is neither a parameter nor a property: " + element.getElementTextWithContext())
            }
        }
    }

    /**
     * override修饰符定位策略
     * 
     * 用于定位声明中的override或redef修饰符
     */
    @JvmField
    val OVERRIDE_MODIFIER: PositioningStrategy<CjModifierListOwner> =
        ModifierSetBasedPositioningStrategy(CjTokens.OVERRIDE_KEYWORD,CjTokens.REDEF_KEYWORD)

    /**
     * 声明返回类型定位策略
     * 
     * 用于定位函数或属性声明的返回类型
     */
    @JvmField
    val DECLARATION_RETURN_TYPE: PositioningStrategy<CjDeclaration> = object : PositioningStrategy<CjDeclaration>() {
        override fun mark(element: CjDeclaration): List<TextRange> {
            return markElement(getElementToMark(element))
        }

        override fun isValid(element: CjDeclaration): Boolean {
            return !hasSyntaxErrors(getElementToMark(element))
        }

        /**
         * 获取要标记的元素
         * 
         * @param declaration 声明元素
         * @return 要标记的PSI元素
         */
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
     * 用于定位可选类型中的问号
     */
    @JvmField
    val OPTIONAL_TYPE: PositioningStrategy<CjOptionType> = object : PositioningStrategy<CjOptionType>() {
        override fun mark(element: CjOptionType): List<TextRange> {
            return markNode(element.getQuestionMarkNode())
        }
    }

    /**
     * 类型参数或声明签名定位策略
     * 
     * 用于定位类型参数列表，如果不存在则定位声明签名
     */
    @JvmField
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
     * 带主体的声明定位策略
     * 
     * 用于定位带有代码块主体的声明
     */
    @JvmField
    val DECLARATION_WITH_BODY: PositioningStrategy<CjDeclarationWithBody> = object : PositioningStrategy<CjDeclarationWithBody>() {
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
     * 用于定位声明的名称标识符
     */
    @JvmField
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
     * else分支定位策略
     * 
     * 用于定位match表达式中的else分支
     */
    @JvmField
    val ELSE_ENTRY: PositioningStrategy<CjMatchEntry> = object : PositioningStrategy<CjMatchEntry>() {
        override fun mark(element: CjMatchEntry): List<TextRange> {
            return markElement(element.elseKeyword!!)
        }
    }

    /**
     * match表达式定位策略
     * 
     * 用于定位match表达式的match关键字
     */
    @JvmField
    val MATCH_EXPRESSION: PositioningStrategy<CjMatchExpression> = object : PositioningStrategy<CjMatchExpression>() {
        override fun mark(element: CjMatchExpression): List<TextRange> {
            return markElement(element.matchKeyword)
        }
    }
    
    /**
     * 值参数定位策略
     * 
     * 用于定位函数调用的参数列表
     */
    @JvmField
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
     * 次级构造函数委托调用定位策略
     * 
     * 用于定位次级构造函数中的委托调用
     */
    @JvmField
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
     * 投影中的变型定位策略
     * 
     * 用于定位类型投影中的变型修饰符
     */
    @JvmField
    val VARIANCE_IN_PROJECTION: PositioningStrategy<CjTypeProjection> =
        object : PositioningStrategy<CjTypeProjection>() {
            override fun mark(element: CjTypeProjection): List<TextRange> {
                return markElement(element.projectionToken!!)
            }
        }

    /**
     * 调用元素定位策略
     * 
     * 用于定位函数调用的被调用元素
     */
    @JvmField
    val CALL_ELEMENT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return markElement((element as? CjCallElement)?.calleeExpression ?: element)
        }
    }

    /**
     * 未解析引用定位策略
     * 
     * 用于定位无法解析的引用表达式
     */
    @JvmField
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
     * 用于没有特定策略时的默认定位
     */
    @JvmField
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
     * 用于定位特定修饰符集合
     *
     * @property modifierSet 修饰符标记集合
     */
    private open class ModifierSetBasedPositioningStrategy(private val modifierSet: TokenSet) :
        PositioningStrategy<CjModifierListOwner>() {
        /**
         * 构造函数
         *
         * @param tokens 修饰符标记数组
         */
        constructor(vararg tokens: IElementType) : this(TokenSet.create(*tokens))

        /**
         * 标记修饰符
         * 
         * @param element 拥有修饰符列表的元素
         * @return 修饰符文本范围列表，如果没有找到则返回null
         */
        protected fun markModifier(element: CjModifierListOwner?): List<TextRange>? =
            modifierSet.types.mapNotNull {
                element?.modifierList?.getModifier(it as CjModifierKeywordToken)?.textRange
            }.takeIf { it.isNotEmpty() }

        override fun mark(element: CjModifierListOwner): List<TextRange> {
            val result = markModifier(element)
            if (result != null) return result

            // Try to resolve situation when there's no visibility modifiers written before element
            if (element is PsiNameIdentifierOwner) {
                val nameIdentifier = element.nameIdentifier
                if (nameIdentifier != null) {
                    return markElement(nameIdentifier)
                }
            }

            val elementToMark = when (element) {

                is CjPropertyAccessor -> element.namePlaceholder
                is CjAnonymousInitializer, is CjPrimaryConstructor -> element
                else -> throw IllegalArgumentException(
                    "Can't find text range for element '${element::class.java.canonicalName}' with the text '${element.text}'"
                )
            }
            return markElement(elementToMark)
        }
    }
}

/**
 * 标记AST节点
 * 
 * @param node 要标记的AST节点
 * @return 节点的文本范围列表
 */
fun markNode(node: ASTNode): List<TextRange> {
    return markElement(node.psi)
}
