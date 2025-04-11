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

package cn.cangnova.cangjie.ide.editor.folding

import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.cdoc.lexer.CDocTokens
import cn.cangnova.cangjie.psi.psiUtil.*
import cn.cangnova.cangjie.psi.stubs.elements.CjFunctionElementType
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

/**
 * CangJieFoldingBuilder 类继承了 CustomFoldingBuilder 和 DumbAware 接口，用于构建仓颉语言的代码折叠区域。
 * 该类主要负责根据特定的语法结构，如导入语句、函数体、类体等，创建折叠描述符，以实现代码的折叠功能。
 */
class CangJieFoldingBuilder : CustomFoldingBuilder(), DumbAware {

    // 存储集合工厂函数名称的集合，目前为空。
    private val collectionFactoryFunctionsNames: Set<String> = setOf()

    /**
     * 构建语言特定折叠区域的方法。
     *
     * @param descriptors 用于存储折叠描述符的列表。
     * @param root PSI元素的根，用于从该根开始构建折叠区域。
     * @param document 文档对象，用于获取文本内容。
     * @param quick 表示是否快速构建折叠区域的标志。
     */
    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement,
        document: Document,
        quick: Boolean
    ) {
        // 如果根元素不是 CjFile 类型，则直接返回。
        if (root !is CjFile) {
            return
        }

        // 处理导入语句的折叠。
        val importList = root.importList
        if (importList != null) {
            val firstImport = importList.imports.firstOrNull()
            if (firstImport != null && importList.imports.size > 1) {
                val importKeyword = firstImport.firstChild

                val startOffset = importKeyword.endOffset + 1
                val endOffset = importList.endOffset

                descriptors.add(FoldingDescriptor(importList, TextRange(startOffset, endOffset)).apply {
                    setCanBeRemovedWhenCollapsed(true)
                })
            }
        }

        // 递归添加描述符。
        appendDescriptors(root.node, document, descriptors)
    }

    /**
     * 判断是否需要折叠的方法。
     *
     * @param node AST节点，用于检查该节点是否需要折叠。
     * @param document 文档对象，用于获取文本内容。
     * @return 如果需要折叠则返回 true，否则返回 false。
     */
    private fun needFolding(node: ASTNode, document: Document): Boolean {
        val type = node.elementType
        val parentType = node.treeParent?.elementType

        // 检查函数体是否需要折叠。
        if (type is CjFunctionElementType) {
            val bodyExpression = (node.psi as? CjNamedFunction)?.bodyExpression
            if (bodyExpression != null && bodyExpression !is CjBlockExpression) return true
        }

        // 检查其他需要折叠的类型。
        return type == CjNodeTypes.FUNCTION_LITERAL ||
                (type == CjNodeTypes.BLOCK && parentType != CjNodeTypes.FUNCTION_LITERAL) ||
                type == CjNodeTypes.CLASS_BODY || type == CjTokens.BLOCK_COMMENT || type == CDocTokens.CDOC ||
                type == CjNodeTypes.STRING_TEMPLATE || type == CjNodeTypes.PRIMARY_CONSTRUCTOR || type == CjNodeTypes.MATCH ||
                node.shouldFoldCollection(document)
    }

    /**
     * 判断集合调用是否需要折叠的方法。
     *
     * @param document 文档对象，用于获取文本内容。
     * @return 如果需要折叠则返回 true，否则返回 false。
     */
    private fun ASTNode.shouldFoldCollection(document: Document): Boolean {
        val call = psi as? CjCallExpression ?: return false
        if (DumbService.isDumb(call.project)) return false

        if (call.valueArguments.size < 2) return false

        // 检查调用是否在同一行，如果是则不需要折叠。
        if (call.startLine(document) == call.endLine(document)) return false

        val reference = call.referenceExpression() ?: return false

        return false
//        return !reference.mainReference.resolvesByNames.any { name ->
//            name.isSpecial || name.identifier !in collectionFactoryFunctionsNames
//        }
    }

    /**
     * 递归添加折叠描述符的方法。
     *
     * @param node AST节点，用于从该节点开始添加折叠描述符。
     * @param document 文档对象，用于获取文本内容。
     * @param descriptors 用于存储折叠描述符的列表。
     */
    private fun appendDescriptors(node: ASTNode, document: Document, descriptors: MutableList<FoldingDescriptor>) {
        if (needFolding(node, document)) {
            val textRange = getRangeToFold(node, document)
            val relativeRange = textRange.shiftRight(-node.textRange.startOffset)
            val foldRegionText = node.chars.subSequence(relativeRange.startOffset, relativeRange.endOffset)
            if (StringUtil.countNewLines(foldRegionText) > 0) {
                descriptors.add(FoldingDescriptor(node, textRange))
            }
        }

        var child = node.firstChildNode
        while (child != null) {
            appendDescriptors(child, document, descriptors)
            child = child.treeNext
        }
    }

    /**
     * 获取需要折叠的范围。
     *
     * @param node AST节点，用于获取该节点的折叠范围。
     * @param document 文档对象，用于获取文本内容。
     * @return 返回折叠的文本范围。
     */
    private fun getRangeToFold(node: ASTNode, document: Document): TextRange {
        // 根据不同的节点类型，返回相应的折叠范围。
        if (node.elementType is CjFunctionElementType) {
            val function = node.psi as? CjNamedFunction
            val funKeyword = function?.funKeyword
            val bodyExpression = function?.bodyExpression
            if (funKeyword != null && bodyExpression != null && bodyExpression !is CjBlockExpression) {
                if (funKeyword.startLine(document) != bodyExpression.startLine(document)) {
                    val lineBreak =
                        bodyExpression.siblings(forward = false, withItself = false).firstOrNull { "\n" in it.text }
                    if (lineBreak != null) {
                        return TextRange(lineBreak.startOffset, bodyExpression.endOffset)
                    }
                }
                return bodyExpression.textRange
            }
        }

        // 处理函数字面量的折叠范围。
        if (node.elementType == CjNodeTypes.FUNCTION_LITERAL) {
            val psi = node.psi as? CjFunctionLiteral
            val lbrace = psi?.lBrace
            val rbrace = psi?.rBrace
            if (lbrace != null && rbrace != null) {
                return TextRange(lbrace.startOffset, rbrace.endOffset)
            }
        }

        // 处理调用表达式的折叠范围。
        if (node.elementType == CjNodeTypes.CALL_EXPRESSION) {
            val valueArgumentList = (node.psi as? CjCallExpression)?.valueArgumentList
            val leftParenthesis = valueArgumentList?.leftParenthesis
            val rightParenthesis = valueArgumentList?.rightParenthesis
            if (leftParenthesis != null && rightParenthesis != null) {
                return TextRange(leftParenthesis.startOffset, rightParenthesis.endOffset)
            }
        }

        // 处理匹配表达式的折叠范围。
        if (node.elementType == CjNodeTypes.MATCH) {
            val whenExpression = node.psi as? CjMatchExpression
            val openBrace = whenExpression?.openBrace
            val closeBrace = whenExpression?.closeBrace
            if (openBrace != null && closeBrace != null) {
                return TextRange(openBrace.startOffset, closeBrace.endOffset)
            }
        }

        // 默认返回节点的文本范围。
        return node.textRange
    }

    /**
     * 获取注释内容的方法。
     *
     * @param line 注释行字符串。
     * @return 返回处理后的注释内容。
     */
    private fun getCommentContents(line: String): String {
        return line.trim()
            .removePrefix("/**")
            .removePrefix("/*")
            .removePrefix("*/")
            .removePrefix("*")
            .trim()
    }

    /**
     * 获取注释第一行内容的方法。
     *
     * @param node AST节点，用于获取该节点的注释第一行内容。
     * @return 返回注释的第一行内容。
     */
    private fun getFirstLineOfComment(node: ASTNode): String {
        val targetCommentLine = node.text.split("\n").firstOrNull {
            getCommentContents(it).isNotEmpty()
        } ?: return ""
        return " ${getCommentContents(targetCommentLine)} "
    }

    /**
     * 获取字符串模板第一行内容的方法。
     *
     * @param node AST节点，用于获取该节点的字符串模板第一行内容。
     * @return 返回字符串模板的第一行内容。
     */
    private fun getTrimmedFirstLineOfString(node: ASTNode): String {
        val lines = node.text.split("\n")
        val firstLine = lines.asSequence().map { it.replace("\"\"\"", "").trim() }.firstOrNull(String::isNotEmpty)
        return firstLine ?: ""
    }

    /**
     * 添加空格的方法。
     *
     * @return 如果需要在字符串末尾添加空格，则返回添加空格后的字符串，否则返回原字符串。
     */
    private fun String.addSpaceIfNeeded(): String {
        if (isEmpty() || endsWith(" ")) return this
        return "$this "
    }

    /**
     * 获取语言特定占位符文本的方法。
     *
     * @param node AST节点，用于获取该节点的占位符文本。
     * @param range 折叠的文本范围。
     * @return 返回占位符文本。
     */
    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String = when {
        node.elementType == CjTokens.BLOCK_COMMENT -> "/${getFirstLineOfComment(node)}.../"
        node.elementType == CDocTokens.CDOC -> "/**${getFirstLineOfComment(node)}...*/"
        node.elementType == CjNodeTypes.STRING_TEMPLATE -> "\"\"\"${getTrimmedFirstLineOfString(node).addSpaceIfNeeded()}...\"\"\""
        node.elementType == CjNodeTypes.PRIMARY_CONSTRUCTOR || node.elementType == CjNodeTypes.CALL_EXPRESSION -> "(...)"
        node.psi is CjImportList -> "..."
        else -> "{...}"
    }

    /**
     * 判断是否是文件中第一个元素的方法。
     *
     * @param element PSI元素，用于检查该元素是否是文件中的第一个非空白元素。
     * @return 如果是则返回 true，否则返回 false。
     */
    private fun isFirstElementInFile(element: PsiElement): Boolean {
        val parent = element.parent
        if (parent is CjFile) {
            val firstNonWhiteSpace = parent.allChildren.firstOrNull {
                it.textLength != 0 && it !is PsiWhiteSpace
            }

            return element == firstNonWhiteSpace
        }

        return false
    }

    /**
     * 判断区域是否默认折叠的方法。
     *
     * @param node AST节点，用于检查该节点是否默认折叠。
     * @return 如果默认折叠则返回 true，否则返回 false。
     */


    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean {
//        val settings = CangJieEditorOptions.getInstance()

//        if (node.psi is CjImportList) {
//            return settings.isCollapseImports
//        }
//
//        val type = node.elementType
//        if (type == CjTokens.BLOCK_COMMENT || type == CDocTokens.CDOC) {
//            if (isFirstElementInFile(node.psi)) {
//                return settings.isCollapseFileHeader
//            }
//        }

        return false
    }
}
