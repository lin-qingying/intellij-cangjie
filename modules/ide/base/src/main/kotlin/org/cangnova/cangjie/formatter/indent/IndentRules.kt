/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.formatter.indent

import org.cangnova.cangjie.formatter.*
import org.cangnova.cangjie.formatter.NodeIndentStrategy.Companion.strategy
import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.CjNodeTypes.*
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens
import org.cangnova.cangjie.psi.psiUtil.getNextSiblingIgnoringWhitespace
import com.intellij.formatting.Indent
import com.intellij.psi.PsiComment


/**
 * 定义所有的缩进规则
 */
object IndentRules {

    /**
     * 获取所有缩进规则
     */
    fun getIndentRules(): Array<NodeIndentStrategy> = arrayOf(
        // 不对块中的右大括号和左大括号进行缩进
        strategy("No indent for braces in blocks")
            .within(BLOCK, CLASS_BODY, FUNCTION_LITERAL, ENUM_BODY)
            .forType(RBRACE, LBRACE)
            .set(Indent.getNoneIndent()),

        // 对块内容进行正常缩进
        strategy("Indent for block content")
            .within(BLOCK, CLASS_BODY, FUNCTION_LITERAL, PROPERTY_BODY, ENUM_BODY)
            .notForType(RBRACE, LBRACE, BLOCK, ENUM_CONSTRUCTOR)
            .set(Indent.getNormalIndent()),

        // 对模板内容进行正常缩进
        strategy("Indent for template content")
            .within(LONG_STRING_TEMPLATE_ENTRY)
            .notForType(LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END)
            .set(Indent.getNormalIndent()),

        // 对模板中的大括号不进行缩进
        strategy("No indent for braces in template")
            .within(LONG_STRING_TEMPLATE_ENTRY)
            .forType(LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END)
            .set(Indent.getNoneIndent()),


        // 对 quote 表达式中的括号不进行缩进
        strategy("No indent for parentheses in quote expression")
            .within(QUOTE_EXPRESSION)
            .forType(LPAR, RPAR)
            .set(Indent.getAbsoluteNoneIndent()),


        // 对属性访问器进行正常缩进
        strategy("Indent for property accessors")
            .within(PROPERTY)
            .forType(PROPERTY_BODY)
            .set(Indent.getNormalIndent()),

        // 对 'for' 循环中的单一语句进行正常缩进
        strategy("For a single statement in 'for'")
            .within(BODY)
            .notForType(BLOCK)
            .set(Indent.getNormalIndent()),

        // 对 'THEN' 和 'ELSE' 中的单一语句进行正常缩进
        strategy("For single statement in THEN and ELSE")
            .within(THEN, ELSE)
            .notForType(BLOCK)
            .set(Indent.getNormalIndent()),

        // 对表达式体应用缩进
        strategy("Expression body")
            .within(FUNC)
            .notForType(OPERATION_REFERENCE)
            .forElement { it.psi is CjExpression && it.psi !is CjBlockExpression }
            .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true),

        // 对表达式体位置的行注释应用缩进
        strategy("Line comment at expression body position")
            .forElement { node ->
                val psi = node.psi
                val parent = psi.parent
                if (psi is PsiComment && parent is CjDeclarationWithInitializer) {
                    psi.getNextSiblingIgnoringWhitespace() == parent.initializer
                } else {
                    false
                }
            }
            .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true),

        // 对 if 条件中的缩进类型进行动态设置
        strategy("If condition")
            .within(CONDITION)
            .set { settings ->
                val indentType =
                    if (settings.cangjieCustomSettings.CONTINUATION_INDENT_IN_IF_CONDITIONS) Indent.Type.CONTINUATION
                    else Indent.Type.NORMAL

                Indent.getIndent(indentType, false, true)
            },

        // 对属性访问器中的表达式体进行正常缩进
        strategy("Property accessor expression body")
            .within(PROPERTY_ACCESSOR)
            .forElement { it.psi is CjExpression && it.psi !is CjBlockExpression }
            .set(Indent.getNormalIndent()),

        // 对属性初始化器中的表达式进行连续缩进
        strategy("Property initializer")
            .within(PROPERTY)
            .forElement { it.psi is CjExpression }
            .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES),


        // 对赋值表达式的右侧部分应用缩进
        strategy("Assignment expressions")
            .within(BINARY_EXPRESSION)
            .within {
                val binaryExpression = it.psi as? CjBinaryExpression ?: return@within false
                ALL_ASSIGNMENTS.contains(binaryExpression.operationToken)
            }
            .forElement {
                val psi = it.psi
                val binaryExpression = psi?.parent as? CjBinaryExpression
                binaryExpression?.right == psi
            }
            .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES),

        // 对块和函数等部分进行缩进，排除某些特定类型
        strategy("Indent for parts")
            .within(
                PROPERTY,
                FUNC,

                PRIMARY_CONSTRUCTOR,
                SECONDARY_CONSTRUCTOR,
                FINALIZER
            )
            .notForType(
                BLOCK,
                FUNC_KEYWORD,
                CONST_KEYWORD,
                LET_KEYWORD,
                VAR_KEYWORD,
                INIT_KEYWORD,
                RPAR,
                EOL_COMMENT,

                MODIFIER_LIST
            )
            .set(Indent.getContinuationWithoutFirstIndent()),

        // 对链式调用进行缩进
        strategy("Chained calls")
            .within(QUALIFIED_EXPRESSIONS)
            .forType(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT)
            .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_CHAINED_CALLS),

        // 对继承列表进行缩进
        strategy("Delegation list")
            .within(SUPER_TYPE_LIST)
            .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_IN_SUPERTYPE_LISTS, indentFirst = true),

        // 对索引进行缩进，排除右中括号
        strategy("Indices")
            .within(INDICES)
            .notForType(RBRACKET)
            .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_IN_ARGUMENT_LISTS),

        // 对二元表达式进行缩进
        strategy("Binary expressions")
            .within(BINARY_EXPRESSIONS)
            .forElement { node -> !node.suppressBinaryExpressionIndent() }
            .set(Indent.getContinuationWithoutFirstIndent(false)),

        // 对括号中的表达式进行缩进
        strategy("Parenthesized expression")
            .within(PARENTHESIZED)
            .set(Indent.getContinuationWithoutFirstIndent(false)),

        // 对条件中的左括号应用缩进
        strategy("Opening parenthesis for conditions")
            .forType(LPAR)
            .within(IF, MATCH_ENTRY, WHILE, DO_WHILE, FOR, MATCH)
            .set(Indent.getContinuationWithoutFirstIndent(true)),

        // 对条件中的右括号应用不缩进
        strategy("Closing parenthesis for conditions")
            .forType(RPAR)
            .forElement { node -> !hasErrorElementBefore(node) || node.prev()?.textLength == 0 }
            .within(IF, MATCH_ENTRY, WHILE, DO_WHILE, FOR, MATCH)
            .set(Indent.getNoneIndent()),

        // 对不完整条件中的右括号应用连续缩进
        strategy("Closing parenthesis for incomplete conditions")
            .forType(RPAR)
            .forElement { node -> hasErrorElementBefore(node) }
            .within(IF, MATCH_ENTRY, WHILE, DO_WHILE, FOR, MATCH)
            .set(Indent.getContinuationWithoutFirstIndent()),

        // 对 MATCH 内容进行正常缩进
        strategy("For MATCH content")
            .within(MATCH)
            .notForType(RBRACE, LBRACE, MATCH_KEYWORD)
            .set(Indent.getNormalIndent()),

        // 对 CDoc 注释中的星号和结束标记进行缩进
        strategy("CDoc comment indent")
            .within(CDOC_CONTENT)
            .forType(CDocTokens.LEADING_ASTERISK, CDocTokens.END)
            .set(Indent.getSpaceIndent(CDOC_COMMENT_INDENT)),

        // 对参数列表中的元素应用缩进
        strategy("Parameter list")
            .within(VALUE_PARAMETER_LIST)
            .forElement { it.elementType == VALUE_PARAMETER && it.psi.prevSibling != null }
            .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_IN_PARAMETER_LISTS, indentFirst = true),

        // 对 WHERE 子句应用缩进
        strategy("Where clause")
            .within(CLASS, EXTEND, STRUCT, ENUM, INTERFACE, FUNC, PROPERTY)
            .forType(WHERE_KEYWORD)
            .set(Indent.getContinuationIndent()),

        // 对数组字面量中的元素进行缩进
        strategy("Array literals")
            .within(COLLECTION_LITERAL_EXPRESSION)
            .notForType(LBRACKET, RBRACKET)
            .set(Indent.getNormalIndent()),

        // 对默认参数值应用缩进
        strategy("Default parameter values")
            .within(VALUE_PARAMETER)
            .forElement { node -> node.psi != null && node.psi == (node.psi.parent as? CjParameter)?.defaultValue }
            .continuationIf(CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true)
    )
}

