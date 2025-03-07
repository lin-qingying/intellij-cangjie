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

package cn.cangnova.cangjie.ide.codeinsight.hints

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import cn.cangnova.cangjie.CangJieBundle
import cn.cangnova.cangjie.ide.codeinsight.hints.declarative.*
import cn.cangnova.cangjie.ide.parameterInfo.*
import cn.cangnova.cangjie.ide.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import cn.cangnova.cangjie.ide.stubindex.resolve.isApplicationInternalMode
import cn.cangnova.cangjie.lexer.CjKeywordToken
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.endOffset
import cn.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import cn.cangnova.cangjie.psi.psiUtil.startOffset
import cn.cangnova.cangjie.resolve.caches.safeAnalyze
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode
import cn.cangnova.cangjie.utils.RangeCjExpressionType
import cn.cangnova.cangjie.utils.getRangeBinaryExpressionType
import cn.cangnova.cangjie.utils.isRangeExpression
import cn.cangnova.cangjie.utils.safeAs
import org.jetbrains.annotations.Nls
import kotlin.enums.EnumEntries

enum class HintType(
    @Nls private val description: String,
    @Nls @IntentionName val showDescription: String,
    @Nls @IntentionName val hideDescription: String,
    defaultEnabled: Boolean
) {

    VARIABLE_HINT(
        CangJieBundle.message("hints.settings.types.variable"),
        CangJieBundle.message("hints.settings.show.types.variable"),
        CangJieBundle.message("hints.settings.dont.show.types.variable"),
        false
    ) {
        override fun provideHintDetails(e: PsiElement): List<InlayInfoDetails> {
            return provideVariableTypeHint(e, SHOW_VARIABLE_TYPES)
        }

        override fun isApplicable(e: PsiElement): Boolean =
            e is CjVariable && e.getReturnTypeReference() == null && !e.isLocal
    },
    PATTERN_VARIABLE_HINT(
        CangJieBundle.message("hints.settings.types.pattern.variable"),
        CangJieBundle.message("hints.settings.show.types.pattern.variable"),
        CangJieBundle.message("hints.settings.dont.show.types.pattern.variable"),
        false
    ) {
        override fun provideHintDetails(e: PsiElement): List<InlayInfoDetails> {
            return provideVariableTypeHint(e, SHOW_PATTERN_VARIABLE_TYPES)
        }

        override fun isApplicable(e: PsiElement): Boolean =
            (e is CjBindingPattern && e.variable == null)
    },
    LOCAL_VARIABLE_HINT(
        CangJieBundle.message("hints.settings.types.local.variable"),
        CangJieBundle.message("hints.settings.show.types.local.variable"),
        CangJieBundle.message("hints.settings.dont.show.types.local.variable"),
        false
    ) {
        override fun provideHintDetails(e: PsiElement): List<InlayInfoDetails> {
            return provideVariableTypeHint(e, SHOW_LOCAL_VARIABLE_TYPES)
        }

        override fun isApplicable(e: PsiElement): Boolean =
            (e is CjVariable && e.getReturnTypeReference() == null && e.isLocal) ||
                    (e is CjParameter && e.isLoopParameter && e.typeReference == null) ||
                    (e is CjDestructuringDeclarationEntry && e.getReturnTypeReference() == null && e.name != "_")
    },
    MAIN_FUNCTION_HINT(
        CangJieBundle.message("hints.settings.types.main.return"),
        CangJieBundle.message("hints.settings.show.types.main.return"),
        CangJieBundle.message("hints.settings.dont.show.types.main.return"),
        false
    ) {
        override fun provideHintDetails(e: PsiElement): List<InlayInfoDetails> {
            e.safeAs<CjMainFunction>()?.let { mainFunction ->
                mainFunction.valueParameterList?.let { paramList ->
                    provideTypeHint(
                        mainFunction,
                        paramList.endOffset,
                        SHOW_MAIN_FUNCTION_RETURN_TYPES
                    )?.let { return listOf(it) }
                }
            }
            e.safeAs<CjExpression>()?.let { expression ->
                provideLambdaReturnTypeHints(expression)?.let { return listOf(it) }
            }
            return emptyList()
        }

        override fun isApplicable(e: PsiElement): Boolean {
            return e is CjMainFunction && !(e.hasDeclaredReturnType())

        }
    },

    FUNCTION_HINT(
        CangJieBundle.message("hints.settings.types.return"),
        CangJieBundle.message("hints.settings.show.types.return"),
        CangJieBundle.message("hints.settings.dont.show.types.return"),
        false
    ) {
        override fun provideHintDetails(e: PsiElement): List<InlayInfoDetails> {
            e.safeAs<CjNamedFunction>()?.let { namedFunction ->
                namedFunction.valueParameterList?.let { paramList ->
                    provideTypeHint(
                        namedFunction,
                        paramList.endOffset,
                        SHOW_FUNCTION_RETURN_TYPES
                    )?.let { return listOf(it) }
                }
            }
            e.safeAs<CjExpression>()?.let { expression ->
                provideLambdaReturnTypeHints(expression)?.let { return listOf(it) }
            }
            return emptyList()
        }

        override fun isApplicable(e: PsiElement): Boolean {
            return e is CjNamedFunction && !(/*e.hasBlockBody() || */e.hasDeclaredReturnType()) ||
                    Registry.`is`("cangjie.enable.inlay.hint.for.lambda.return.type") && e is CjExpression && e !is CjFunctionLiteral && !e.isNameReferenceInCall() && e.isLambdaReturnValueHintsApplicable(
                allowOneLiner = true
            )
        }
    },

    PARAMETER_TYPE_HINT(
        CangJieBundle.message("hints.settings.types.parameter"),
        CangJieBundle.message("hints.settings.show.types.parameter"),
        CangJieBundle.message("hints.settings.dont.show.types.parameter"),
        false
    ) {
        override fun provideHintDetails(e: PsiElement): List<InlayInfoDetails> {
            (e as? CjParameter)?.let { param ->
                param.nameIdentifier?.let { ident ->
                    provideTypeHint(param, ident.endOffset, SHOW_FUNCTION_PARAMETER_TYPES)?.let { return listOf(it) }
                }
            }
            return emptyList()
        }

        override fun isApplicable(e: PsiElement): Boolean =
            e is CjParameter && e.typeReference == null && !e.isLoopParameter
    },

    PARAMETER_HINT(
        CangJieBundle.message("hints.title.argument.name.enabled"),
        CangJieBundle.message("hints.title.show.argument.name.enabled"),
        CangJieBundle.message("hints.title.dont.show.argument.name.enabled"),
        true
    ) {
        override fun provideHints(e: PsiElement): List<InlayInfo> {
            val callElement = e.getStrictParentOfType<CjCallElement>() ?: return emptyList()
            return provideArgumentNameHints(callElement)
        }

        override fun isApplicable(e: PsiElement): Boolean = e is CjValueArgumentList
    },

    LAMBDA_RETURN_EXPRESSION(
        CangJieBundle.message("hints.settings.lambda.return"),
        CangJieBundle.message("hints.settings.show.lambda.return"),
        CangJieBundle.message("hints.settings.dont.show.lambda.return"),
        true
    ) {
        override fun isApplicable(e: PsiElement): Boolean {
            return e is CjExpression &&
                    e !is CjFunctionLiteral &&
                    !e.isNameReferenceInCall() &&
                    e.isLambdaReturnValueHintsApplicable()
        }

        override fun provideHintDetails(e: PsiElement): List<InlayInfoDetails> {
            e.safeAs<CjExpression>()?.let { expression ->
                provideLambdaReturnValueHints(expression)?.let { return listOf(it) }
            }
            return emptyList()
        }
    },

    LAMBDA_IMPLICIT_PARAMETER_RECEIVER(
        CangJieBundle.message("hints.settings.lambda.receivers.parameters"),
        CangJieBundle.message("hints.settings.show.lambda.receivers.parameters"),
        CangJieBundle.message("hints.settings.dont.show.lambda.receivers.parameters"),
        true
    ) {
        override fun isApplicable(e: PsiElement): Boolean {
            return e is CjFunctionLiteral && e.parent is CjLambdaExpression && (e.parent as CjLambdaExpression).leftCurlyBrace.isFollowedByNewLine()
        }

        override fun provideHintDetails(e: PsiElement): List<InlayInfoDetails> {
            e.safeAs<CjFunctionLiteral>()?.parent.safeAs<CjLambdaExpression>()?.let { expression ->
                provideLambdaImplicitHints(expression)?.let { return it }
            }
            return emptyList()
        }
    },

    SUSPENDING_CALL(
        CangJieBundle.message("hints.settings.suspending"),
        CangJieBundle.message("hints.settings.show.suspending"),
        CangJieBundle.message("hints.settings.dont.show.suspending"),
        false
    ) {
        override fun isApplicable(e: PsiElement) = e.isNameReferenceInCall() && isApplicationInternalMode()

        override fun provideHints(e: PsiElement): List<InlayInfo> {
            val callExpression = e.parent as? CjCallExpression ?: return emptyList()
            return emptyList()
        }
    },

    RANGES(
        CangJieBundle.message("hints.settings.ranges"),
        CangJieBundle.message("hints.settings.show.ranges"),
        CangJieBundle.message("hints.settings.dont.show.ranges"),
        true
    ) {
        override fun isApplicable(e: PsiElement): Boolean = e is CjBinaryExpression && e.isRangeExpression()

        override fun provideHintDetails(e: PsiElement): List<InlayInfoDetails> {
            val binaryExpression = e.safeAs<CjBinaryExpression>() ?: return emptyList()
            val leftExp = binaryExpression.left ?: return emptyList()
            val rightExp = binaryExpression.right ?: return emptyList()
            val operationReference: CjOperationReferenceExpression = binaryExpression.operationReference
            val context = lazy { binaryExpression.safeAnalyze(BodyResolveMode.PARTIAL) }
            val type = binaryExpression.getRangeBinaryExpressionType(context) ?: return emptyList()

//            if (!leftExp.isComparable(context.value) || !rightExp.isComparable(context.value)) return emptyList()

            val (leftText: String, rightText: String?) = when (type) {
                RangeCjExpressionType.RANGE_TO -> {
                    CangJieBundle.message("hints.ranges.lessOrEqual") to CangJieBundle.message("hints.ranges.lessOrEqual")
                }
//                RANGE_UNTIL -> {
//                    CangJieBundle.message("hints.ranges.lessOrEqual") to null
//                }
//                DOWN_TO -> {
//                    if (operationReference.hasIllegalLiteralPrefixOrSuffix()) return emptyList()
//
//                    CangJieBundle.message("hints.ranges.greaterOrEqual") to CangJieBundle.message("hints.ranges.greaterOrEqual")
//                }
//                UNTIL -> {
//                    if (operationReference.hasIllegalLiteralPrefixOrSuffix()) return emptyList()
//
//                    CangJieBundle.message("hints.ranges.lessOrEqual") to CangJieBundle.message("hints.ranges.less")
//                }
            }

            val leftInfo = InlayInfo(text = leftText, offset = leftExp.endOffset)
            val rightInfo = rightText?.let { InlayInfo(text = it, offset = rightExp.startOffset) }
            return listOfNotNull(
                InlayInfoDetails(leftInfo, listOf(TextInlayInfoDetail(leftText, smallText = false))),
                rightInfo?.let { InlayInfoDetails(it, listOf(TextInlayInfoDetail(rightText, smallText = false))) }
            )
        }
    };

    companion object {
        private val values: EnumEntries<HintType> = entries

        fun resolve(e: PsiElement): List<HintType> =
            values.filter { it.isApplicable(e) }

        private fun CjOperationReferenceExpression.hasIllegalLiteralPrefixOrSuffix(): Boolean {
            val prevLeaf = PsiTreeUtil.prevLeaf(this)
            val nextLeaf = PsiTreeUtil.nextLeaf(this)
            return prevLeaf?.illegalLiteralPrefixOrSuffix() == true || nextLeaf?.illegalLiteralPrefixOrSuffix() == true
        }

        private fun PsiElement.illegalLiteralPrefixOrSuffix(): Boolean {
            val elementType = this.node.elementType
            return (elementType === CjTokens.IDENTIFIER) ||
                    (elementType === CjTokens.INTEGER_LITERAL) ||
                    (elementType === CjTokens.FLOAT_LITERAL) ||
                    elementType is CjKeywordToken
        }

    }

    abstract fun isApplicable(e: PsiElement): Boolean
    open fun provideHints(e: PsiElement): List<InlayInfo> = emptyList()
    open fun provideHintDetails(e: PsiElement): List<InlayInfoDetails> =
        provideHints(e).map { InlayInfoDetails(it, listOf(TextInlayInfoDetail(it.text)), NoInlayInfoOption) }

    val option = Option("SHOW_${this.name}", { this.description }, defaultEnabled)
    val enabled
        get() = option.get()
}

data class InlayInfoDetails(
    val inlayInfo: InlayInfo,
    val details: List<InlayInfoDetail>,
    val option: InlayInfoOption? = NoInlayInfoOption
)

sealed class InlayInfoOption

object NoInlayInfoOption : InlayInfoOption()

class NamedInlayInfoOption(val name: String) : InlayInfoOption()

sealed class InlayInfoDetail(val text: String)

class TextInlayInfoDetail(text: String, val smallText: Boolean = true) : InlayInfoDetail(text) {
    override fun toString(): String = "[$text]"
}

class TypeInlayInfoDetail(text: String, val fqName: String?) : InlayInfoDetail(text) {
    override fun toString(): String = "[$text :$fqName]"
}

class PsiInlayInfoDetail(text: String, val element: PsiElement) : InlayInfoDetail(text) {
    override fun toString(): String = "[$text @ $element]"
}

