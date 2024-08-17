package com.huawei.cangjie.ide.editor.folding

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.doc.lexer.CDocTokens
import com.huawei.cangjie.ide.editor.CangJieEditorOptions
import com.huawei.cangjie.ide.references.mainReference
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.*
import com.huawei.cangjie.psi.stubs.elements.CjFunctionElementType
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

class CangJieFoldingBuilder : CustomFoldingBuilder(), DumbAware {

    private val collectionFactoryFunctionsNames: Set<String> = setOf()
    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement,
        document: Document,
        quick: Boolean
    ) {
        if (root !is CjFile) {
            return
        }

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

        appendDescriptors(root.node, document, descriptors)
    }

    private fun needFolding(node: ASTNode, document: Document): Boolean {
        val type = node.elementType
        val parentType = node.treeParent?.elementType

        if (type is CjFunctionElementType) {
            val bodyExpression = (node.psi as? CjNamedFunction)?.bodyExpression
            if (bodyExpression != null && bodyExpression !is CjBlockExpression) return true
        }

        return type == CjNodeTypes.FUNCTION_LITERAL ||
                (type == CjNodeTypes.BLOCK && parentType != CjNodeTypes.FUNCTION_LITERAL) ||
                type == CjNodeTypes.CLASS_BODY || type == CjTokens.BLOCK_COMMENT || type == CDocTokens.CDOC ||
                type == CjNodeTypes.STRING_TEMPLATE || type == CjNodeTypes.PRIMARY_CONSTRUCTOR || type == CjNodeTypes.MATCH ||
                node.shouldFoldCollection(document)
    }

    private fun ASTNode.shouldFoldCollection(document: Document): Boolean {
        val call = psi as? CjCallExpression ?: return false
        if (DumbService.isDumb(call.project)) return false

        if (call.valueArguments.size < 2) return false

        // Similar check will be done latter, but we still use it here to avoid unnecessary resolve.
        if (call.startLine(document) == call.endLine(document)) return false

        val reference = call.referenceExpression() ?: return false
        return !reference.mainReference.resolvesByNames.any { name ->
            name.isSpecial || name.identifier !in collectionFactoryFunctionsNames
        }
    }

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

    private fun getRangeToFold(node: ASTNode, document: Document): TextRange {
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

        if (node.elementType == CjNodeTypes.FUNCTION_LITERAL) {
            val psi = node.psi as? CjFunctionLiteral
            val lbrace = psi?.lBrace
            val rbrace = psi?.rBrace
            if (lbrace != null && rbrace != null) {
                return TextRange(lbrace.startOffset, rbrace.endOffset)
            }
        }

        if (node.elementType == CjNodeTypes.CALL_EXPRESSION) {
            val valueArgumentList = (node.psi as? CjCallExpression)?.valueArgumentList
            val leftParenthesis = valueArgumentList?.leftParenthesis
            val rightParenthesis = valueArgumentList?.rightParenthesis
            if (leftParenthesis != null && rightParenthesis != null) {
                return TextRange(leftParenthesis.startOffset, rightParenthesis.endOffset)
            }
        }

        if (node.elementType == CjNodeTypes.MATCH) {
            val whenExpression = node.psi as? CjMatchExpression
            val openBrace = whenExpression?.openBrace
            val closeBrace = whenExpression?.closeBrace
            if (openBrace != null && closeBrace != null) {
                return TextRange(openBrace.startOffset, closeBrace.endOffset)
            }
        }

        return node.textRange
    }

    private fun getCommentContents(line: String): String {
        return line.trim()
            .removePrefix("/**")
            .removePrefix("/*")
            .removePrefix("*/")
            .removePrefix("*")
            .trim()
    }

    private fun getFirstLineOfComment(node: ASTNode): String {
        val targetCommentLine = node.text.split("\n").firstOrNull {
            getCommentContents(it).isNotEmpty()
        } ?: return ""
        return " ${getCommentContents(targetCommentLine)} "
    }

    private fun getTrimmedFirstLineOfString(node: ASTNode): String {
        val lines = node.text.split("\n")
        val firstLine = lines.asSequence().map { it.replace("\"\"\"", "").trim() }.firstOrNull(String::isNotEmpty)
        return firstLine ?: ""
    }
    private fun String.addSpaceIfNeeded(): String {
        if (isEmpty() || endsWith(" ")) return this
        return "$this "
    }
    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String = when {
        node.elementType == CjTokens.BLOCK_COMMENT -> "/${getFirstLineOfComment(node)}.../"
        node.elementType == CDocTokens.CDOC -> "/**${getFirstLineOfComment(node)}...*/"
        node.elementType == CjNodeTypes.STRING_TEMPLATE -> "\"\"\"${getTrimmedFirstLineOfString(node).addSpaceIfNeeded()}...\"\"\""
        node.elementType == CjNodeTypes.PRIMARY_CONSTRUCTOR || node.elementType == CjNodeTypes.CALL_EXPRESSION -> "(...)"
        node.psi is CjImportList -> "..."
        else -> "{...}"
    }
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
