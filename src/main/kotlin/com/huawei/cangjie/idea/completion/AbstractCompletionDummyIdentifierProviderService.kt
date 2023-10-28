package com.huawei.cangjie.idea.completion

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.*
import com.huawei.cangjie.utils.getNonStrictParentOfType
import com.huawei.cangjie.utils.parents
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.*
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.prevLeaf
import org.jetbrains.annotations.ApiStatus
import kotlin.math.max


@ApiStatus.Internal
abstract class AbstractCompletionDummyIdentifierProviderService : CompletionDummyIdentifierProviderService {
    override fun correctPositionForStringTemplateEntry(context: CompletionInitializationContext): Boolean {
        val offset = context.startOffset
        val psiFile = context.file
        val tokenBefore = psiFile.findElementAt(max(0, offset - 1))

        if (offset > 0 && tokenBefore!!.node.elementType == CjTokens.REGULAR_STRING_PART && tokenBefore.text.startsWith(
                "."
            )
        ) {
            val prev = tokenBefore.parent.prevSibling
            if (prev != null && prev is CjSimpleNameStringTemplateEntry) {
                val expression = prev.expression
                if (expression != null) {
                    val prefix = tokenBefore.text.substring(0, offset - tokenBefore.startOffset)
                    context.dummyIdentifier =
                        "{" + expression.text + prefix + CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "}"
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



//            isInClassHeader(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER

//            isInUnclosedSuperQualifier(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + ">"

//            isInSimpleStringTemplate(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED

            else -> specialLambdaSignatureDummyIdentifier(tokenBefore)
                ?: specialExtensionReceiverDummyIdentifier(tokenBefore)
                ?: specialInTypeArgsDummyIdentifier(tokenBefore)
                ?: specialInArgumentListDummyIdentifier(tokenBefore)
                ?: specialInNameWithQuotes(tokenBefore)
                ?: specialInBinaryExpressionDummyIdentifier(tokenBefore)
                ?: isInValueOrTypeParametersList(tokenBefore)
                ?: handleDefaultCase(context)

                ?: DEFAULT_DUMMY_IDENTIFIER
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

    /**
     * lambda 表达式生成虚拟标识符
     */
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
        val classOrStruct = tokenBefore?.parents?.firstIsInstanceOrNull<CjClassOrStruct>() ?: return false
        val name = classOrStruct.nameIdentifier ?: return false
        val headerEnd = classOrStruct.body?.startOffset ?: classOrStruct.endOffset
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
            CjTokens.IDENTIFIER -> tokenBefore?.prevLeaf(skipEmptyElements = true)
                ?.takeIf { it.elementType == TokenType.BAD_CHARACTER }

            else -> null
        }
        val quote = "`"
        if (badCharacterBefore?.text == quote) return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + quote + "$"
        return null
    }

    private fun isInUnclosedSuperQualifier(tokenBefore: PsiElement?): Boolean {
        if (tokenBefore == null) return false
        val tokensToSkip =
            TokenSet.orSet(TokenSet.create(CjTokens.IDENTIFIER, CjTokens.DOT), CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET)
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

        if (tokenBefore.getParentOfType<CjTypeArgumentList>(true) != null) {
            return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
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

        val argumentList = tokenBefore?.getNonStrictParentOfType<CjValueArgumentList>() ?: return null
        if (argumentList.parent is CjConstructorDelegationCall) return CompletionUtil.DUMMY_IDENTIFIER_TRIMMED

        return CompletionUtil.DUMMY_IDENTIFIER_TRIMMED + "$,"
    }

    private companion object {
        private const val DEFAULT_DUMMY_IDENTIFIER: String =
            CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "$" // add '$' to ignore context after the caret

        private val declarationKeywords =
            TokenSet.create(CjTokens.FUNC_KEYWORD, CjTokens.LET_KEYWORD, CjTokens.VAR_KEYWORD)
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
