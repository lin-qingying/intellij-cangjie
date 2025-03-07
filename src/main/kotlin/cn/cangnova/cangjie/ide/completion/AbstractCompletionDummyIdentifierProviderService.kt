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

package cn.cangnova.cangjie.ide.completion

import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.*
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.*
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import kotlin.math.max

abstract class AbstractCompletionDummyIdentifierProviderService : CompletionDummyIdentifierProviderService {
    override fun correctPositionForStringTemplateEntry(context: CompletionInitializationContext): Boolean {
        val offset = context.startOffset
        val psiFile = context.file
        val tokenBefore = psiFile.findElementAt(max(0, offset - 1))

        if (offset > 0 && tokenBefore!!.node.elementType == CjTokens.REGULAR_STRING_PART && tokenBefore.text.startsWith(".")) {
            val prev = tokenBefore.parent.prevSibling
            if (prev != null && prev is CjSimpleNameStringTemplateEntry) {
                val expression = prev.expression
                if (expression != null) {
                    val prefix = tokenBefore.text.substring(0, offset - tokenBefore.startOffset)
                    context.dummyIdentifier = "{" + expression.text + prefix + CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "}"
                    context.offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, expression.startOffset)
                    return true
                }
            }
        }
        return false
    }

    override fun correctPositionForParameter(context: CompletionInitializationContext) {
        val offset = context.startOffset
        val psiFile = context.file
        val tokenAt = psiFile.findElementAt(max(0, offset)) ?: return

        // IDENTIFIER when 'f<caret>oo: Foo'
        // COLON when 'foo<caret>: Foo'
        if (tokenAt.node.elementType == CjTokens.IDENTIFIER || tokenAt.node.elementType == CjTokens.COLON) {
            val parameter = tokenAt.parent as? CjParameter
            if (parameter != null) {
                context.replacementOffset = parameter.endOffset
            }
        }
    }

    override fun provideDummyIdentifier(context: CompletionInitializationContext): String {
        val psiFile = context.file
        if (psiFile !is CjFile) {
            error("CompletionDummyIdentifierProviderService.providerDummyIdentifier should not be called for non CjFile")
        }

        val offset = context.startOffset
        val tokenBefore = psiFile.findElementAt(max(0, offset - 1))

        return when {
            context.completionType == CompletionType.SMART -> DEFAULT_DUMMY_IDENTIFIER

            // TODO package completion

            isInClassHeader(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER // do not add '$' to not interrupt class declaration parsing

            isInUnclosedSuperQualifier(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + ">"

            isInSimpleStringTemplate(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED

            else -> specialLambdaSignatureDummyIdentifier(tokenBefore)
                ?: specialExtensionReceiverDummyIdentifier(tokenBefore)
                ?: specialInTypeArgsDummyIdentifier(tokenBefore)
                ?: specialInArgumentListDummyIdentifier(tokenBefore)
                ?: specialInNameWithQuotes(tokenBefore)
                ?: specialInBinaryExpressionDummyIdentifier(tokenBefore)
                ?: isInValueOrTypeParametersList(tokenBefore)
                ?: handleDefaultCase(context)
                ?: isInAnnotationEntry(tokenBefore)
                ?: DEFAULT_DUMMY_IDENTIFIER
        }
    }

    private fun isInAnnotationEntry(tokenBefore: PsiElement?): String? {
        if (tokenBefore == null) return null

        val typeReference = tokenBefore.parentOfType<CjTypeReference>(true) ?: return null
        return if (typeReference.parentOfType<CjAnnotationEntry>() != null) {
            CompletionUtilCore.DUMMY_IDENTIFIER
        } else {
            null
        }
    }

    protected open fun handleDefaultCase(context: CompletionInitializationContext): String? = null

    private fun isInValueOrTypeParametersList(tokenBefore: PsiElement?): String? {
        if (tokenBefore == null) return null
        if (tokenBefore.parents.any { it is CjTypeParameterList || it is CjParameterList }) {
            return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
        }
        return null
    }

    private fun specialLambdaSignatureDummyIdentifier(tokenBefore: PsiElement?): String? {
        var leaf = tokenBefore
        while (leaf is PsiWhiteSpace || leaf is PsiComment) {
            leaf = leaf.prevLeaf(true)
        }

        val lambda = leaf?.parents?.firstOrNull { it is CjFunctionLiteral } ?: return null

        val lambdaChild = leaf.parents.takeWhile { it != lambda }.lastOrNull()

        return if (lambdaChild is CjParameterList)
            CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
        else
            null

    }

    private fun isInClassHeader(tokenBefore: PsiElement?): Boolean {
        val classOrObject = tokenBefore?.parents?.firstIsInstanceOrNull<CjTypeStatement>() ?: return false
        val name = classOrObject.nameIdentifier ?: return false
        val headerEnd = classOrObject.body?.startOffset ?: classOrObject.endOffset
        val offset = tokenBefore.startOffset
        return name.endOffset <= offset && offset <= headerEnd
    }

    private fun specialInBinaryExpressionDummyIdentifier(tokenBefore: PsiElement?): String? {
        if (tokenBefore.elementType == CjTokens.IDENTIFIER && tokenBefore?.context?.context is CjBinaryExpression)
            return CompletionUtilCore.DUMMY_IDENTIFIER
        return null
    }

    private fun specialInNameWithQuotes(tokenBefore: PsiElement?): String? {
        val badCharacterBefore = when (tokenBefore?.elementType) {
            TokenType.BAD_CHARACTER -> tokenBefore
            CjTokens.IDENTIFIER -> tokenBefore?.prevLeaf(skipEmptyElements = true)?.takeIf { it.elementType == TokenType.BAD_CHARACTER }
            else -> null
        }
        val quote = "`"
        if (badCharacterBefore?.text == quote) return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + quote + "$"
        return null
    }

    private fun isInUnclosedSuperQualifier(tokenBefore: PsiElement?): Boolean {
        if (tokenBefore == null) return false
        val tokensToSkip = TokenSet.orSet(TokenSet.create(CjTokens.IDENTIFIER, CjTokens.DOT), CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET)
        val tokens = generateSequence(tokenBefore) { it.prevLeaf() }
        val ltToken = tokens.firstOrNull { it.node.elementType !in tokensToSkip } ?: return false
        if (ltToken.node.elementType != CjTokens.LT) return false
        val superToken = ltToken.prevLeaf { it !is PsiWhiteSpace && it !is PsiComment }
        return superToken?.node?.elementType == CjTokens.SUPER_KEYWORD
    }

    private fun isInSimpleStringTemplate(tokenBefore: PsiElement?): Boolean {
        return tokenBefore?.parents?.firstIsInstanceOrNull<CjStringTemplateExpression>()?.isPlain() ?: false
    }


    private fun specialExtensionReceiverDummyIdentifier(tokenBefore: PsiElement?): String? {
        var token = tokenBefore ?: return null
        var ltCount = 0
        var gtCount = 0
        val builder = StringBuilder()
        while (true) {
            val tokenType = token.node!!.elementType
            if (tokenType in declarationKeywords) {
                val balance = ltCount - gtCount
                if (balance < 0) return null
                builder.append(token.text!!.reversed())
                builder.reverse()

                var tail = "X" + ">".repeat(balance) + ".f"
                if (tokenType == CjTokens.FUNC_KEYWORD) {
                    tail += "()"
                }
                builder.append(tail)

                val text = builder.toString()
                val file = CjPsiFactory(tokenBefore.project).createFile(text)
                val declaration = file.declarations.singleOrNull() ?: return null
                if (declaration.textLength != text.length) return null
                val containsErrorElement = !PsiTreeUtil.processElements(file) { it !is PsiErrorElement }
                return if (containsErrorElement) null else "$tail$"
            }
            if (tokenType !in declarationTokens) return null
            if (tokenType == CjTokens.LT) ltCount++
            if (tokenType == CjTokens.GT) gtCount++
            builder.append(token.text!!.reversed())
            token = PsiTreeUtil.prevLeaf(token) ?: return null
        }
    }

    private fun specialInTypeArgsDummyIdentifier(tokenBefore: PsiElement?): String? {
        if (tokenBefore == null) return null

        if (tokenBefore.getParentOfType<CjTypeArgumentList>(true) != null) { // already parsed inside type argument list
            return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED // do not insert '$' to not break type argument list parsing
        }

        val pair = unclosedTypeArgListNameAndBalance(tokenBefore) ?: return null
        val (nameToken, balance) = pair
        assert(balance > 0)

        val nameRef = nameToken.parent as? CjNameReferenceExpression ?: return null
        return if (allTargetsAreFunctionsOrClasses(nameRef)) {
            CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + ">".repeat(balance) + "$"
        } else {
            null
        }
    }

    protected abstract fun allTargetsAreFunctionsOrClasses(nameReferenceExpression: CjNameReferenceExpression): Boolean

    private fun unclosedTypeArgListNameAndBalance(tokenBefore: PsiElement): Pair<PsiElement, Int>? {
        val nameToken = findCallNameTokenIfInTypeArgs(tokenBefore) ?: return null
        val pair = unclosedTypeArgListNameAndBalance(nameToken)
        return if (pair == null) {
            Pair(nameToken, 1)
        } else {
            Pair(pair.first, pair.second + 1)
        }
    }

    private val callTypeArgsTokens = TokenSet.orSet(
        TokenSet.create(
            CjTokens.IDENTIFIER, CjTokens.LT, CjTokens.GT,
            CjTokens.COMMA, CjTokens.DOT, CjTokens.QUEST, CjTokens.COLON,
            CjTokens.LPAR, CjTokens.RPAR, CjTokens.ARROW
        ),
        CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
    )

    // if the leaf could be located inside type argument list of a call (if parsed properly)
    // then it returns the call name reference this type argument list would belong to
    private fun findCallNameTokenIfInTypeArgs(leaf: PsiElement): PsiElement? {
        var current = leaf
        while (true) {
            val tokenType = current.node!!.elementType
            if (tokenType !in callTypeArgsTokens) return null

            if (tokenType == CjTokens.LT) {
                val nameToken = current.prevLeaf(skipEmptyElements = true) ?: return null
                if (nameToken.node!!.elementType != CjTokens.IDENTIFIER) return null
                return nameToken
            }

            if (tokenType == CjTokens.GT) { // pass nested type argument list
                val prev = current.prevLeaf(skipEmptyElements = true) ?: return null
                val typeRef = findCallNameTokenIfInTypeArgs(prev) ?: return null
                current = typeRef
                continue
            }

            current = current.prevLeaf(skipEmptyElements = true) ?: return null
        }
    }


    private fun specialInArgumentListDummyIdentifier(tokenBefore: PsiElement?): String? {
        // If we insert `$` in the argument list of a delegation specifier, this will break parsing
        // and the following block will not be attached as a body to the constructor. Therefore
        // we need to use a regular identifier.
        val argumentList = tokenBefore?.getNonStrictParentOfType<CjValueArgumentList>() ?: return null
        if (argumentList.parent is CjConstructorDelegationCall) return CompletionUtil.DUMMY_IDENTIFIER_TRIMMED
        // If there is = in the argument list after caret, then breaking parsing with just $ prevents K2 from resolving function call,
        // i.e. `f ($ = )` is resolved to variable assignment and left part `f ($` is resolved to erroneous name reference,
        // so we need to use `$,` to avoid resolving to variable assignment
        return CompletionUtil.DUMMY_IDENTIFIER_TRIMMED + "$,"
    }

    private companion object {
        private const val DEFAULT_DUMMY_IDENTIFIER: String =
            CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "$" // add '$' to ignore context after the caret

        private val declarationKeywords = TokenSet.create(CjTokens.FUNC_KEYWORD, CjTokens.LET_KEYWORD, CjTokens.VAR_KEYWORD)
        private val declarationTokens = TokenSet.orSet(
            TokenSet.create(
                CjTokens.IDENTIFIER, CjTokens.LT, CjTokens.GT,
                CjTokens.COMMA, CjTokens.DOT, CjTokens.QUEST, CjTokens.COLON,
                CjTokens.IN_KEYWORD,
                CjTokens.LPAR, CjTokens.RPAR, CjTokens.ARROW,
                TokenType.ERROR_ELEMENT
            ),
            CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
        )
    }
}
