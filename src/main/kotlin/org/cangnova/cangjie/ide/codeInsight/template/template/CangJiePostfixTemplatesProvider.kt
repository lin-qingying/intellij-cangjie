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

package org.cangnova.cangjie.ide.codeInsight.template.template

import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.endOffset
import org.cangnova.cangjie.psi.psiUtil.getParentOfType
import org.cangnova.cangjie.psi.psiUtil.parentsWithSelf
import org.cangnova.cangjie.utils.isUnitTestMode
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateExpressionSelector
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.Function
import org.jetbrains.annotations.TestOnly


/**
 * 创建一个Postfix模板表达式选择器
 *
 * @param collector 收集器函数，接收PsiFile和偏移量，返回符合条件的CjExpression序列
 * @return PostfixTemplateExpressionSelector 实例
 *
 * 该函数会：
 * 1. 实现getExpressions方法，通过collector获取所有表达式
 * 2. 实现hasExpression方法，检查是否存在任何表达式
 * 3. 实现getRenderer方法，提供PSI元素的文本渲染方式
 */
private fun selector(collector: (PsiFile, Int) -> Sequence<CjExpression>): PostfixTemplateExpressionSelector {
    return object : PostfixTemplateExpressionSelector {
        override fun getExpressions(context: PsiElement, document: Document, offset: Int): List<PsiElement> {
            return collector(context.containingFile, offset).toList()
        }

        override fun hasExpression(context: PsiElement, copyDocument: Document, newOffset: Int): Boolean {
            return collector(context.containingFile, newOffset).any()
        }

        override fun getRenderer(): com.intellij.util.Function<PsiElement, String> {
            return Function(PsiElement::getText)
        }
    }
}

/**
 * 收集指定偏移量处的所有CangJie表达式
 *
 * @param file 要搜索的PSI文件
 * @param offset 文件中的字符偏移量
 * @return 返回一个Sequence<CjExpression>，包含所有符合条件的表达式
 *
 * 该函数会：
 * 1. 获取指定偏移量处的PSI元素及其所有父元素
 * 2. 过滤出CjExpression类型的元素
 * 3. 确保表达式结束位置与偏移量匹配
 * 4. 应用一系列过滤条件排除不合适的表达式类型
 */
private fun collectExpressions(file: PsiFile, offset: Int): Sequence<CjExpression> {
    return PsiUtilCore.getElementAtOffset(file, offset - 1).parentsWithSelf.filterIsInstance<CjExpression>()
        .filter { it.endOffset == offset }.filter { expression ->
            val parent = expression.parent
            when {
                expression is CjBlockExpression -> false
                expression is CjFunctionLiteral -> false // 使用包含的'CjLambdaExpression'代替
                expression is CjLambdaExpression && parent is CjLambdaArgument -> false
                parent is CjThisExpression -> false
                parent is CjQualifiedExpression && expression == parent.selectorExpression -> false
                expression.node.elementType == CjNodeTypes.OPERATION_REFERENCE -> false
                expression.getParentOfType<CjTypeElement>(strict = false) != null -> false
                else -> true
            }
        }
}

/**
 * 收集并过滤所有符合条件的CangJie表达式，返回一个Postfix模板表达式选择器
 *
 * @param filters 可变参数，包含一组过滤条件，每个条件都是一个接受CjExpression并返回Boolean的函数
 * @return PostfixTemplateExpressionSelector 实例，用于选择和过滤表达式
 *
 * 该函数会：
 * 1. 收集指定位置的所有CjExpression
 * 2. 应用所有传入的过滤器进行筛选
 * 3. 在单元测试模式下，会记录被建议的表达式文本
 */
fun allExpressions(vararg filters: (CjExpression) -> Boolean): PostfixTemplateExpressionSelector {
    return selector { file, offset ->
        collectExpressions(file, offset).filter { expression ->
            filters.all { it(expression) }
        }.also { expressions ->
            if (isUnitTestMode) {
                val expressionTexts = expressions.toList().map { it.text }
                if (expressionTexts.size > 1) {
                    @Suppress("TestOnlyProblems") with(CangJiePostfixTemplateInfo) {
                        file.suggestedExpressions = expressionTexts
                    }
                }
            }
        }
    }
}

@TestOnly
object CangJiePostfixTemplateInfo {
    /**
     * In tests only one expression should be suggested, so in case there are many of them, save relevant items.
     */
    var PsiFile.suggestedExpressions: List<String> by NotNullableUserDataProperty(
        Key("CANGJIE_POSTFIX_TEMPLATE_EXPRESSIONS"),
        defaultValue = emptyList(),
    )
}

internal object NonPackageAndNonImportFilter : (CjExpression) -> Boolean {
    override fun invoke(expression: CjExpression): Boolean {
        val parent = expression.parent
        return parent !is CjPackageDirective && parent !is CjImportDirective && parent !is CjImportDirectiveItem
    }
}

internal object ValuedFilter : (CjExpression) -> Boolean {
    override fun invoke(expression: CjExpression): Boolean {
        val isAnonymousFunction =
            expression is CjFunctionLiteral || (expression is CjNamedFunction && expression.name == null)

        return when {
            CjPsiUtil.isAssignment(expression) -> false
            expression is CjNamedDeclaration && !isAnonymousFunction -> false
            expression is CjLoopExpression -> false
            expression is CjReturnExpression -> false
            expression is CjBreakExpression -> false
            expression is CjContinueExpression -> false
            expression is CjIfExpression && expression.`else` == null -> false
            else -> true
        }
    }
}


/**
 * 后缀模板提供者，用于提供CangJie的后缀模板
 */
class CangJiePostfixTemplateProvider : PostfixTemplateProvider {

    private val templateSet: Set<PostfixTemplate> by lazy {
        setOf(
            CangJieValPostfixTemplate(this),
            CangJieVarPostfixTemplate(this),
        )
    }

    override fun getTemplates(): Set<PostfixTemplate> = templateSet

    override fun isTerminalSymbol(currentChar: Char): Boolean = currentChar == '.'

    override fun preCheck(copyFile: PsiFile, realEditor: Editor, currentOffset: Int): PsiFile = copyFile

    override fun preExpand(file: PsiFile, editor: Editor) {}
    override fun afterExpand(file: PsiFile, editor: Editor) {}
}