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

package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderUtil.rawTokenText
import com.intellij.lang.WhitespacesBinders
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.parsing.NameParsingMode.*
import org.cangnova.cangjie.psi.CallingConvention
import org.cangnova.cangjie.psi.CjBuiltInAnnotation
import org.cangnova.cangjie.psi.CjBuiltInAnnotation.*
import org.cangnova.cangjie.psi.CjNodeTypes.*
import org.cangnova.cangjie.psi.OverflowStrategy
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.ANNOTATION
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.ANNOTATIONS
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.CONSTRUCTOR_CALLEE


class CangJieParsing private constructor(
    builder: SemanticWhitespaceAwarePsiBuilder, isTopLevel: Boolean, isLazy: Boolean
) : AbstractCangJieParsing(
    builder, isLazy
) {

    companion object {
        val PARAMETER_NAME_RECOVERY_SET = TokenSet.create(COLON, EQ, COMMA, RPAR)
        private val GT_COMMA_COLON_SET = TokenSet.create(GT, COMMA, COLON)
        private val LOG = Logger.getInstance(CangJieParsing::class.java)
        private val TOP_LEVEL_DECLARATION_FIRST = TokenSet.create(
            INTERFACE_KEYWORD, CLASS_KEYWORD, FUNC_KEYWORD, LET_KEYWORD, VAR_KEYWORD, CONST_KEYWORD, PACKAGE_KEYWORD
        )
        private val TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET =
            TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(SEMICOLON))
        private val LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET =
            TokenSet.orSet(TokenSet.create(LT, EQ, SEMICOLON), TOP_LEVEL_DECLARATION_FIRST)

        private val CLASS_NAME_RECOVERY_SET =
            TokenSet.orSet(TokenSet.create(LT, LPAR, LTCOLON, LBRACE), TOP_LEVEL_DECLARATION_FIRST)
        private val TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(WHERE_KEYWORD, LPAR, COLON, LBRACE, GT)
        private val PACKAGE_NAME_RECOVERY_SET = TokenSet.create(DOT, EOL_OR_SEMICOLON)
        private val IMPORT_RECOVERY_SET = TokenSet.create(AS_KEYWORD, DOT, EOL_OR_SEMICOLON)
        private val TYPE_REF_FIRST = TokenSet.create(LBRACKET, IDENTIFIER, LPAR, HASH)
        private val LBRACE_RBRACE_TYPE_REF_FIRST_SET = TokenSet.orSet(TokenSet.create(LBRACE, RBRACE), TYPE_REF_FIRST)
        private val LTCOLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET =
            TokenSet.orSet(TokenSet.create(LTCOLON, COMMA, LBRACE, RBRACE), TYPE_REF_FIRST)
        private val RECEIVER_TYPE_TERMINATORS = TokenSet.create(DOT)
        private val VALUE_PARAMETER_FIRST = TokenSet.orSet(
            TokenSet.create(IDENTIFIER, LBRACKET, LET_KEYWORD, CONST_KEYWORD, VAR_KEYWORD),
            TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUNC_KEYWORD))
        )
        private val LAMBDA_VALUE_PARAMETER_FIRST = TokenSet.orSet(
            TokenSet.create(IDENTIFIER, LBRACKET), TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUNC_KEYWORD))
        )

        private val BLOCK_DOC_COMMENT_SET = TokenSet.create(BLOCK_COMMENT, DOC_COMMENT)
        private val SEMICOLON_SET = TokenSet.create(SEMICOLON)
        private val COMMA_COLON_GT_SET = TokenSet.create(COMMA, COLON, GT)
        private val IDENTIFIER_RBRACKET_LBRACKET_SET = TokenSet.create(IDENTIFIER, RBRACKET, LBRACKET)
        private val LBRACE_RBRACE_SET = TokenSet.create(LBRACE, RBRACE)
        private val COMMA_SEMICOLON_RBRACE_SET = TokenSet.create(COMMA, SEMICOLON, RBRACE)
        private val VALUE_ARGS_RECOVERY_SET = TokenSet.create(LBRACE, SEMICOLON, RPAR, EOL_OR_SEMICOLON, RBRACE)
        private val PROPERTY_NAME_FOLLOW_SET =
            TokenSet.create(COLON, EQ, LBRACE, RBRACE, SEMICOLON, LET_KEYWORD, VAR_KEYWORD, FUNC_KEYWORD, CLASS_KEYWORD)
        private val PROPERTY_NAME_FOLLOW_MULTI_DECLARATION_RECOVERY_SET =
            TokenSet.orSet(PROPERTY_NAME_FOLLOW_SET, PARAMETER_NAME_RECOVERY_SET)
        private val PROPERTY_NAME_FOLLOW_FUNCTION_OR_PROPERTY_RECOVERY_SET =
            TokenSet.orSet(PROPERTY_NAME_FOLLOW_SET, LBRACE_RBRACE_SET, TOP_LEVEL_DECLARATION_FIRST)
        private val IDENTIFIER_EQ_COLON_SEMICOLON_SET = TokenSet.create(IDENTIFIER, EQ, COLON, SEMICOLON)
        private val COMMA_RPAR_COLON_EQ_SET = TokenSet.create(COMMA, RPAR, COLON, EQ)
        private val ACCESSOR_FIRST_OR_PROPERTY_END =
            TokenSet.orSet(MODIFIER_KEYWORDS, TokenSet.create(GET_KEYWORD, SET_KEYWORD, EOL_OR_SEMICOLON, RBRACE))
        private val RPAR_IDENTIFIER_COLON_LBRACE_EQ_SET = TokenSet.create(RPAR, IDENTIFIER, COLON, LBRACE, EQ)
        private val COMMA_COLON_RPAR_SET = TokenSet.create(COMMA, COLON, RPAR)
        private val RPAR_COLON_LBRACE_EQ_SET = TokenSet.create(RPAR, COLON, LBRACE, EQ)
        private val LBRACKET_LBRACE_RBRACE_LPAR_SET = TokenSet.create(LBRACKET, LBRACE, RBRACE, LPAR)
        private val FUNCTION_NAME_FOLLOW_SET = TokenSet.create(LT, LPAR, RPAR, COLON, EQ)
        private val FUNCTION_NAME_RECOVERY_SET =
            TokenSet.orSet(TokenSet.create(LT, LPAR, RPAR, COLON, EQ), LBRACE_RBRACE_SET, TOP_LEVEL_DECLARATION_FIRST)
        private val VALUE_PARAMETERS_FOLLOW_SET = TokenSet.create(EQ, LBRACE, RBRACE, SEMICOLON, RPAR)
        private val LPAR_VALUE_PARAMETERS_FOLLOW_SET =
            TokenSet.orSet(TokenSet.create(LPAR), VALUE_PARAMETERS_FOLLOW_SET)
        private val LPAR_LBRACE_COLON_CONSTRUCTOR_KEYWORD_SET = TokenSet.create(LPAR, LBRACE, COLON, INIT_KEYWORD)
        private val definitelyOutOfReceiverSet =
            TokenSet.orSet(TokenSet.create(EQ, COLON, LBRACE, RBRACE), TOP_LEVEL_DECLARATION_FIRST)
        private val EOL_OR_SEMICOLON_RBRACE_SET = TokenSet.create(EOL_OR_SEMICOLON, RBRACE)
        private val CLASS_INTERFACE_SET = TokenSet.create(CLASS_KEYWORD, INTERFACE_KEYWORD)
        private val CLASS_INTERFACE_STRUCT_ENUM_EXTEND_SET =
            TokenSet.create(CLASS_KEYWORD, INTERFACE_KEYWORD, STRUCT_KEYWORD, ENUM_KEYWORD, EXTEND_KEYWORD)

        private val IDENTIFIER_DOT_MUL_SET = TokenSet.create(IDENTIFIER, DOT, MUL)

        // 包的访问修饰符
        private val PACKAGE_ACCESS_MODIFIER_SET = TokenSet.create(PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD)

        // 导入语句访问修饰符
        private val IMPORT_ACCESS_MODIFIER_SET =
            TokenSet.create(PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD)

        private val DOT_MUL_SET = TokenSet.create(DOT, MUL)
        private val DECLARATION_FIRST =
            TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(INIT_KEYWORD, GET_KEYWORD, SET_KEYWORD))
        private val MUT_PROP_SET = TokenSet.create(MUT_KEYWORD, PROP_KEYWORD)


        private fun createForByClause(builder: SemanticWhitespaceAwarePsiBuilder, isLazy: Boolean): CangJieParsing {
            return CangJieParsing(SemanticWhitespaceAwarePsiBuilderForByClause(builder), false, isLazy)
        }

        fun createForTopLevel(builder: SemanticWhitespaceAwarePsiBuilder): CangJieParsing {
            return CangJieParsing(builder, true, true)
        }

        fun createForTopLevelNonLazy(builder: SemanticWhitespaceAwarePsiBuilder): CangJieParsing {
            return CangJieParsing(builder, true, false)
        }

    }

    val expressionParsing: CangJieExpressionParsing =
        if (isTopLevel) CangJieExpressionParsing(builder, this, isLazy) else object :
            CangJieExpressionParsing(builder, this@CangJieParsing, isLazy) {
            public override fun create(builder: SemanticWhitespaceAwarePsiBuilder): CangJieParsing {
                return CangJieParsing.createForByClause(builder, super.isLazy)
            }
        }


    enum class ModifierKind {
        ABSTRACT, MUT, PUBLIC, PRIVATE, PROTECTED, OPERATOR, FOREIGN, CONST, UNSAFE, SEALED, REDEF, OPEN, STATIC
    }

    public class ModifierDetector : ((IElementType?) -> Unit) {
        private val modifiers = mutableSetOf<ModifierKind>()

        val count: Int get() = modifiers.size

        fun has(kind: ModifierKind): Boolean = modifiers.contains(kind)

        val isAbstractDetected: Boolean get() = has(ModifierKind.ABSTRACT)
        val isMutDetected: Boolean get() = has(ModifierKind.MUT)
        val isPublicDetected: Boolean get() = has(ModifierKind.PUBLIC)
        val isPrivateDetected: Boolean get() = has(ModifierKind.PRIVATE)
        val isProtectedDetected: Boolean get() = has(ModifierKind.PROTECTED)
        val isOperatorDetected: Boolean get() = has(ModifierKind.OPERATOR)
        val isForeignDetected: Boolean get() = has(ModifierKind.FOREIGN)
        val isConstDetected: Boolean get() = has(ModifierKind.CONST)
        val isUnsafeDetected: Boolean get() = has(ModifierKind.UNSAFE)
        val isSealedDetected: Boolean get() = has(ModifierKind.SEALED)
        val isRedefDetected: Boolean get() = has(ModifierKind.REDEF)
        val isOpenDetected: Boolean get() = has(ModifierKind.OPEN)
        val isStaticDetected: Boolean get() = has(ModifierKind.STATIC)

        @Deprecated("Use count property", ReplaceWith("count"))
        fun getSize(): Int = count

        override fun invoke(item: IElementType?) {
            when (item) {
                PUBLIC_KEYWORD -> modifiers.add(ModifierKind.PUBLIC)
                PRIVATE_KEYWORD -> modifiers.add(ModifierKind.PRIVATE)
                PROTECTED_KEYWORD -> modifiers.add(ModifierKind.PROTECTED)
                ABSTRACT_KEYWORD -> modifiers.add(ModifierKind.ABSTRACT)
                MUT_KEYWORD -> modifiers.add(ModifierKind.MUT)
                OPERATOR_KEYWORD -> modifiers.add(ModifierKind.OPERATOR)
                FOREIGN_KEYWORD -> modifiers.add(ModifierKind.FOREIGN)
                CONST_KEYWORD -> modifiers.add(ModifierKind.CONST)
                UNSAFE_KEYWORD -> modifiers.add(ModifierKind.UNSAFE)
                OPEN_KEYWORD -> modifiers.add(ModifierKind.OPEN)
                STATIC_KEYWORD -> modifiers.add(ModifierKind.STATIC)
                SEALED_KEYWORD -> modifiers.add(ModifierKind.SEALED)
                REDEF_KEYWORD -> modifiers.add(ModifierKind.REDEF)
            }
        }
    }


    private val lastDotAfterReceiverNotLParPattern = LastBefore(
        AtSet(RECEIVER_TYPE_TERMINATORS), object : AbstractTokenStreamPredicate() {
            override fun matching(topLevel: Boolean): Boolean {
                if (topLevel && (atSet(definitelyOutOfReceiverSet) || at(LPAR))) {
                    return true
                }
                if (topLevel && at(IDENTIFIER)) {
                    val lookahead = lookahead(1)
                    return lookahead != LT && lookahead != DOT && lookahead != QUEST
                }
                return false
            }
        })

    private val lastDotAfterReceiverLParPattern = FirstBefore(
        AtSet(RECEIVER_TYPE_TERMINATORS), object : AbstractTokenStreamPredicate() {
            override fun matching(topLevel: Boolean): Boolean {
                if (topLevel && atSet(definitelyOutOfReceiverSet)) {
                    return true
                }
                return topLevel && !at(QUEST) && !at(LPAR) && !at(RPAR)
            }
        })


    /**
     * 解析类型引用
     *
     * Grammar:
     * ```
     * typeReference
     *   : userType
     *   | functionType
     *   | basicType
     *   | optionalType
     *   | tupleType
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseTypeRef() {
        parseTypeRef(TokenSet.EMPTY, false)
    }

    /**
     * 解析不带交集类型的类型引用
     *
     * Grammar:
     * ```
     * typeReference
     *   : userType
     *   | functionType
     *   | basicType
     *   | optionalType
     *   | tupleType
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseTypeRefWithoutIntersections() {
        parseTypeRef(TokenSet.EMPTY)
    }


    public override fun create(builder: SemanticWhitespaceAwarePsiBuilder): CangJieParsing {
        return createForTopLevel(builder)
    }


    /**
     * 解析 this 或 super 关键字
     *
     * Grammar:
     * ```
     * constructorDelegationReference
     *   : "this" | "super"
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseThisOrSuper() {
        check(_at(THIS_KEYWORD) || _at(SUPER_KEYWORD))
        val mark = mark()

        advance() // THIS_KEYWORD | SUPER_KEYWORD

        mark.done(CONSTRUCTOR_DELEGATION_REFERENCE)
    }

    /**
     * 解析构造函数代码块
     *
     * Grammar:
     * ```
     * initBlock
     *   : "{" constructorDelegationCall? statement* "}"
     *   ;
     * ```
     */

    context(parseContext: ParsingContext)
    private fun parseInitFunctionBlock() {
        val lazyBlock = mark()

        builder.enableNewlines()

        // 恢复  init() xxxxxxx {}
        expect(LBRACE, "Expecting '{'  ")

        val delegationCall = mark()
        if ((at(THIS_KEYWORD) || at(SUPER_KEYWORD)) && rawLookup(1) == LPAR) {
            parseThisOrSuper()
            expressionParsing.parseValueArgumentList()
            delegationCall.done(CONSTRUCTOR_DELEGATION_CALL)
        } else {
            mark().done(CONSTRUCTOR_DELEGATION_REFERENCE)
            delegationCall.done(CONSTRUCTOR_DELEGATION_CALL)
        }

        expressionParsing.parseStatements()
        expect(RBRACE, "Expecting '}'")

        builder.restoreNewlinesState()
        lazyBlock.done(INIT_BLOCK)
    }

    /**
     * 解析代码块
     *
     * Grammar:
     * ```
     * block
     *   : "{" statement* "}"
     *   ;
     * ```
     *
     * @param collapse 是否折叠代码块以提升解析性能
     */
    context(parseContext: ParsingContext)
    fun parseBlock(collapse: Boolean = true) {
        val lazyBlock = mark()

        builder.enableNewlines()

        val hasOpeningBrace = expect(LBRACE, "Expecting '{'  ")
        val canCollapse = collapse && hasOpeningBrace && isLazy

        if (canCollapse) {
            advanceBalancedBlock()
        } else {
            expressionParsing.parseStatements()
            expect(RBRACE, "Expecting '}'")
        }

        builder.restoreNewlinesState()

        if (canCollapse) {
            lazyBlock.collapse(BLOCK)
        } else {
            lazyBlock.done(BLOCK)
        }
    }


    /**
     * 解析块表达式
     *
     * 解析一个代码块作为表达式，不进行折叠。
     *
     * Grammar:
     * ```
     * blockExpression
     *   : block
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseBlockExpression() {
        parseBlock(false)
    }

    /**
     * 解析包名
     *
     * Grammar:
     * ```
     * packageName
     *   : simpleName ("." simpleName)*
     *   ;
     * ```
     */

//    context(parseContext: ParsingContext)
//    private fun parsePackageName() {
//        var qualifiedExpression = mark()
//        var simpleName = true
//
//        while (true) {
//            if (builder.newlineBeforeCurrentToken()) {
//                errorWithRecovery(
//                    "Package name must be a '.'-separated identifier list placed on a single line",
//                    PACKAGE_NAME_RECOVERY_SET
//                )
//                break
//            }
//
//            if (at(DOT)) {
//                advance() // DOT
//                qualifiedExpression.error(CangJieParsingBundle.message("parsing.error.package.name.separator"))
//                qualifiedExpression = mark()
//                continue
//            }
//
//            val nsName = mark()
//            val simpleNameFound = expect(
//                IDENTIFIER, "Package name must be a '.'-separated identifier list", PACKAGE_NAME_RECOVERY_SET
//            )
//            if (simpleNameFound) {
//                nsName.done(REFERENCE_EXPRESSION)
//            } else {
//                nsName.drop()
//            }
//
//            if (!simpleName) {
//                val precedingMarker = qualifiedExpression.precede()
//                qualifiedExpression.done(DOT_QUALIFIED_EXPRESSION)
//                qualifiedExpression = precedingMarker
//            }
//
//            if (at(DOT)) {
//                advance() // DOT
//
//                if (simpleName && !simpleNameFound) {
//                    qualifiedExpression.drop()
//                    qualifiedExpression = mark()
//                } else {
//                    simpleName = false
//                }
//            } else {
//                break
//            }
//        }
//
//        qualifiedExpression.drop()
//    }


    context(parseContext: ParsingContext)
    private fun parsePackageName() {
        // 先解析第一个标识符
        if (builder.newlineBeforeCurrentToken()) {
            errorWithRecovery(
                "Package name must be a '.'-separated identifier list placed on a single line",
                PACKAGE_NAME_RECOVERY_SET
            )
            return
        }

        // 第一个片段
        val firstName = mark()
        val firstFound = expect(
            IDENTIFIER,
            "Package name must be a '.'-separated identifier list",
            PACKAGE_NAME_RECOVERY_SET
        )
        if (firstFound) {
            firstName.done(REFERENCE_EXPRESSION)
        } else {
            firstName.drop()
            return
        }

        // 没有后续 DOT，就是单段包名，直接结束
        if (!at(DOT)) return

        // 有多段，构建左递归 DOT_QUALIFIED_EXPRESSION 树
        // 初始：wrap 第一个 REFERENCE_EXPRESSION
        var lhs = firstName.precede()
        // 此时 lhs 包裹着已 done 的 firstName

        while (at(DOT)) {
            advance() // DOT

            if (builder.newlineBeforeCurrentToken()) {
                lhs.done(DOT_QUALIFIED_EXPRESSION)
                errorWithRecovery(
                    "Package name must be a '.'-separated identifier list placed on a single line",
                    PACKAGE_NAME_RECOVERY_SET
                )
                return
            }

            val nextName = mark()
            val nextFound = expect(
                IDENTIFIER,
                "Package name must be a '.'-separated identifier list",
                PACKAGE_NAME_RECOVERY_SET
            )
            if (nextFound) {
                nextName.done(REFERENCE_EXPRESSION)
            } else {
                nextName.drop()
                lhs.done(DOT_QUALIFIED_EXPRESSION)
                return
            }

            // 把 lhs 收进新的 DOT_QUALIFIED_EXPRESSION，再向外扩展
            val newLhs = lhs.precede()
            lhs.done(DOT_QUALIFIED_EXPRESSION)
            lhs = newLhs
        }

        // 最外层多套了一层，drop 掉
        lhs.drop()
    }

    context(parseContext: ParsingContext)
    private fun parsePreamble() {
        // 解析包声明
        parsePackageDirective()
        // 解析导入列表
        parseImportDirectives()
    }

    context(parseContext: ParsingContext)
    private fun parsePackageDirective() {
        // 先探测是否有包声明，决定走哪条路径
        val hasAccessModifier = atSet(PACKAGE_ACCESS_MODIFIER_SET)
                && (lookahead(1) == MACRO_KEYWORD || lookahead(1) == PACKAGE_KEYWORD)
        val hasMacroKeyword = at(MACRO_KEYWORD)
        val hasPackageKeyword = when {
            hasAccessModifier && lookahead(1) == MACRO_KEYWORD -> lookahead(2) == PACKAGE_KEYWORD
            hasAccessModifier -> lookahead(1) == PACKAGE_KEYWORD
            hasMacroKeyword -> lookahead(1) == PACKAGE_KEYWORD
            else -> at(PACKAGE_KEYWORD)
        }

        if (!hasAccessModifier && !hasMacroKeyword && !hasPackageKeyword) {
            // 没有包声明，直接插入空的 PACKAGE_DIRECTIVE 占位
            val packageDirective = mark()
            packageDirective.done(PACKAGE_DIRECTIVE)
            packageDirective.setCustomEdgeTokenBinders(BindFirstShebangWithWhitespaceOnly, null)
            return
        }

        // 有包声明，正常解析
        val packageDirective = mark()

        if (atSet(PACKAGE_ACCESS_MODIFIER_SET)
            && (lookahead(1) == MACRO_KEYWORD || lookahead(1) == PACKAGE_KEYWORD)
        ) {
            advance() // 访问修饰符
        }

        if (at(MACRO_KEYWORD)) {
            advance() // MACRO_KEYWORD
        }

        if (at(PACKAGE_KEYWORD)) {
            advance() // PACKAGE_KEYWORD
            parsePackageName()
            consumeIf(SEMICOLON)
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.keyword", "package"))
        }

        packageDirective.done(PACKAGE_DIRECTIVE)
    }
    /**
     * 解析文件前导部分
     *
     * Grammar:
     * ```
     * preamble
     *   : fileAnnotationList? packageDirective? importDirective*
     *   ;
     * ```
     */
//    context(parseContext: ParsingContext)
//    private fun parsePreamble() {
//        val firstEntry = mark()
//
//        /*
//         * TODO fileAnnotationList Ko
//         * 文档注释 : fileAnnotations*
//         */
//
//        /**
//         * 解析包声明
//         *
//         * Grammar:
//         * ```
//         * packageDirective
//         *   : modifier* "package" qualifiedName (";")?
//         *   ;
//         * ```
//         */
//        var packageDirective = mark()
//
//        // 是否有修饰符
//        var isPackageAccessModifier = false
//        if (atSet(PACKAGE_ACCESS_MODIFIER_SET) && (lookahead(1) == MACRO_KEYWORD || lookahead(1) == PACKAGE_KEYWORD)) {
//            advance() // 修饰符
//            isPackageAccessModifier = true
//        }
//
//        if (at(MACRO_KEYWORD)) {
//            advance() // MARCO_KEYWORD 宏声明
//            isPackageAccessModifier = true
//        }
//
//        if (at(PACKAGE_KEYWORD)) {
//            if (at(PACKAGE_KEYWORD)) {
//                advance() // PACKAGE_KEYWORD
//            } else if (isPackageAccessModifier) {
//                error(CangJieParsingBundle.message("parsing.error.expecting.keyword", "package"))
//            }
//
//
//            parsePackageName()
//
//            firstEntry.drop()
//
//            consumeIf(SEMICOLON)
//
//            packageDirective.done(PACKAGE_DIRECTIVE)
//        } else {
//            // 忽略 package 指令时不应报错，将位置回滚
//            firstEntry.rollbackTo()
//
//            // TODO 解析文件注解列表
//            // parseFileAnnotationList(FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED)
//            packageDirective = mark()
//            packageDirective.done(PACKAGE_DIRECTIVE)
//            packageDirective.setCustomEdgeTokenBinders(
//                BindFirstShebangWithWhitespaceOnly, null
//            )
//
//            // TODO 仓颉包中必须有包名，但单文件可无
//        }
//
//        parseImportDirectives()
//    }


    /**
     * 解析导入指令列表
     *
     * Grammar:
     * ```
     * importList
     *   : importDirective*
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseImportDirectives() {
        val importList = mark()

//        if (!(at(IMPORT_KEYWORD) || atSet(IMPORT_ACCESS_MODIFIER_SET) && lookahead(1) == IMPORT_KEYWORD)) {
//            // 允许注释绑定到首个声明
//            importList.setCustomEdgeTokenBinders(DoNotBindAnything, null)
//        }

        while (at(IMPORT_KEYWORD) || atSet(IMPORT_ACCESS_MODIFIER_SET) && lookahead(1) == IMPORT_KEYWORD || isWhenAnnotation()) {
            if (!parseImportDirective()) {
                break
            }
        }

        importList.done(IMPORT_LIST)
    }

    fun isWhenAnnotation(): Boolean {
        if (at(AT) && CjBuiltInAnnotation.fromName(
                rawTokenText(builder, 1).toString()
            ) == CjBuiltInAnnotation.WHEN
        ) {
            return true
        }
        return false
    }

    /**
     * 解析 @When 条件编译注解
     *
     * @When 注解用于条件编译,可以出现在 import 语句之前或其他声明之前。
     *
     * Grammar:
     * ```
     * whenAnnotation
     *   : "@" "When" "[" expression "]"
     *   ;
     * ```
     *
     * 示例:
     * ```cangjie
     * @When[DEBUG]
     * import some.debug.Module
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseWhenAnnotation(


        isCreateAnnotations: Boolean = false

    ) {
        assert(at(AT))

        val annotationMark = mark()


        val mark = mark()
        advance() // 消耗 @

        // 解析 When 标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        val annotationName = builder.tokenText
        if (annotationName != "When") {
            error(CangJieParsingBundle.message("parsing.error.expecting.annotation", "When"))
            mark.done(ANNOTATION)
            return
        }

        parseConstructorCallee()// 消耗 When


        expect(LBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "["))

        // 使用专用节点类型封装 When 条件
        val whenConditionMark = mark()
        expressionParsing.parseExpression()
        whenConditionMark.done(ANNOTATION_WHEN_CONDITION)

        expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))


//        if (at(LBRACKET)) {
//            expressionParsing.parseValueArgumentList(LBRACKET, RBRACKET)
//        }

        mark.done(ANNOTATION)

        if (isCreateAnnotations) {
            annotationMark.done(ANNOTATIONS)
        } else {
            annotationMark.drop()
        }
    }

    context(parseContext: ParsingContext)
    private fun parseAttributeAnnotation(


        isCreateAnnotations: Boolean = false

    ) {
        assert(at(AT))

        val annotationMark = mark()


        val mark = mark()
        advance() // 消耗 @

        // 解析 Attribute 标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        val annotationName = builder.tokenText
        if (annotationName != "Attribute") {
            error(CangJieParsingBundle.message("parsing.error.expecting.annotation", "Attribute"))
            mark.done(ANNOTATION)
            return
        }

        parseConstructorCallee()// 消耗 Attribute


        expect(LBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "["))

        val attr = mark()


        // 处理标识符或字符串，使用逗号分隔，允许多个连续逗号
        // 例如: @Attribute[State, aaa,,,,,,sdfsd,,,,,,,ssss]
        builder.disableNewlines()

        while (true) {
            // 跳过多余的逗号
            while (at(COMMA)) {
                advance()
            }

            // 如果到达右括号，结束解析
            if (at(RBRACKET)) {
                break
            }

            // 解析标识符或字符串
            if (at(IDENTIFIER)) {
                advance()
            } else if (at(OPEN_QUOTE)) {
                with(parseContext.copy(processStringInterpolation = false)) {
                    expressionParsing.parseStringTemplate()
                }

            } else {
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.identifier.or.string"))

            }

            // 跳过逗号
            while (at(COMMA)) {
                advance()
            }

            // 如果到达右括号，结束解析
            if (at(RBRACKET)) {
                break
            }
            if (eof()) {
                break
            }
        }

        builder.enableNewlines()
        attr.done(ANNTATION_ATTR_ATTRIBUTE)
        expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))

        mark.done(ANNOTATION)

        if (isCreateAnnotations) {
            annotationMark.done(ANNOTATIONS)
        } else {
            annotationMark.drop()
        }
    }

    /**
     * 关闭导入指令并处理换行错误
     *
     * @param importDirective 导入指令标记
     * @param importAlias 导入别名标记
     * @param errorMessage 错误消息
     * @return 是否处理了错误
     */
    context(parseContext: ParsingContext)
    private fun closeImportWithErrorIfNewline(
        importDirective: PsiBuilder.Marker?, importAlias: PsiBuilder.Marker?, errorMessage: String
    ): Boolean {
        if (builder.newlineBeforeCurrentToken()) {
            importAlias?.done(IMPORT_ALIAS)
            error(errorMessage)
            importDirective?.done(IMPORT_DIRECTIVE)
            return true
        }
        return false
    }

    /**
     * 解析导入指令项
     *
     * Grammar:
     * ```
     * importDirectiveItem
     *   : qualifiedName ("as" simpleName)?
     *   | qualifiedName "." "*"
     *   ;
     * ```
     *
     * @param isTopLevel 是否为顶层导入
     * @return 是否成功解析
     */

    /**
     * 解析单个导入项
     *
     * 支持:
     * - a.b.c
     * - a.b.*
     * - a.b as AB
     */
    context(parseContext: ParsingContext)
    private fun parseImportItem() {
        val item = mark()

        if (!at(IDENTIFIER)) {
            error(
                CangJieParsingBundle.message(
                    "parsing.error.package.name.after.dot",
                    builder.tokenText ?: "<unknown>"
                )
            )
            item.done(IMPORT_ITEM)
            return
        }

        var qualifiedName = mark()
        var reference = mark()
        advance() // IDENTIFIER
        reference.done(REFERENCE_EXPRESSION)

        // 解析点号分隔的标识符链
        while (at(DOT) && lookahead(1) != MUL && lookahead(1) != LBRACE) {
            advance() // DOT

            reference = mark()
            if (expect(
                    IDENTIFIER,
                    "Qualified name must be a '.'-separated identifier list",
                    IMPORT_RECOVERY_SET
                )
            ) {
                reference.done(REFERENCE_EXPRESSION)
            } else {
                reference.drop()
            }

            val precede = qualifiedName.precede()
            qualifiedName.done(DOT_QUALIFIED_EXPRESSION)
            qualifiedName = precede
        }

        qualifiedName.drop()

        // 处理通配符或别名
        when {
            at(DOT) && lookahead(1) == MUL -> {
                advance() // DOT
                advance() // MUL

                if (at(AS_KEYWORD)) {
                    errorAndAdvance(
                        CangJieParsingBundle.message("parsing.error.aliases.not.allowed.for.all.imports")
                    )
                }
            }

            at(AS_KEYWORD) -> {
                val alias = mark()
                advance() // AS_KEYWORD
                expect(IDENTIFIER, "Expecting identifier", SEMICOLON_SET)
                alias.done(IMPORT_ALIAS)
            }
        }

        item.done(IMPORT_ITEM)
    }

    /**
     * 解析导入项列表 (逗号分隔)
     *
     * 用于:
     * - import {a.b, c.d, ...}
     * - import a.{b, c, ...}
     */
    context(parseContext: ParsingContext)
    private fun parseImportItemList() {
        parseImportItem()

        while (at(COMMA)) {
            advance() // COMMA
            parseImportItem()
        }
    }

    /**
     * 解析导入指令
     *
     * 支持的语法形式:
     * 1. import a.b
     * 2. import a.{b, c}
     * 3. import {a.b, a.c}
     * 4. import {a.*, b.c}
     * 5. import {a.*, b.c as BC}
     * 6. import a.*
     * 7. public/internal/protected import ...
     *
     * Grammar:
     * ```
     * importDirective
     *   : modifier* "import" (importItem | "{" importItemList "}" | qualifiedName "." "{" importItemList "}") (";")?
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseImportDirective(): Boolean {
        assert(_at(IMPORT_KEYWORD) || _atSet(IMPORT_ACCESS_MODIFIER_SET) || isWhenAnnotation())

        val importDirective = mark()

        // 1. 解析注解 (when annotation)
        if (isWhenAnnotation()) {
            parseWhenAnnotation(true)
        }

        // 2. 解析访问修饰符 (public/internal/protected/private)
        if (_atSet(IMPORT_ACCESS_MODIFIER_SET)) {
            advance() // 访问修饰符
        }

        // 3. 检查 import 关键字
        if (!at(IMPORT_KEYWORD)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.keyword", "import"))
            importDirective.rollbackTo()
            return false
        }

        advance() // IMPORT_KEYWORD

        // 4. 检查换行错误
        if (closeImportWithErrorIfNewline(importDirective, null, "Expecting qualified name")) {
            return true
        }

        // 5. 解析导入项
        if (at(LBRACE)) {
            // 形式: import {a.b, c.d, ...}
            advance() // LBRACE
            parseImportItemList()
            expect(RBRACE, "Expecting '}'")
        } else {
            // 需要先解析限定名，然后判断是单项导入还是同包多项导入
            // 解析限定名
            if (!at(IDENTIFIER)) {
                error("Expecting qualified name")
                importDirective.done(IMPORT_DIRECTIVE)
                importDirective.setCustomEdgeTokenBinders(null, TrailingCommentsBinder)
                return true
            }

            var qualifiedName = mark()
            var reference = mark()
            advance() // IDENTIFIER
            reference.done(REFERENCE_EXPRESSION)

            // 解析点号分隔的标识符链
            while (at(DOT) && lookahead(1) == IDENTIFIER) {
                advance() // DOT

                reference = mark()
                advance() // IDENTIFIER
                reference.done(REFERENCE_EXPRESSION)

                val precede = qualifiedName.precede()
                qualifiedName.done(DOT_QUALIFIED_EXPRESSION)
                qualifiedName = precede
            }

            // 检查是否为同包多项导入 import a.{b, c}
            if (at(DOT) && lookahead(1) == LBRACE) {
                // 保留 qualifiedName 作为基础路径，让 CjImportDirective.importedReference 可以访问
                qualifiedName.drop()  // 虽然 drop，但表达式已经在 AST 中
                advance() // DOT
                advance() // LBRACE
                parseImportItemList()
                expect(RBRACE, "Expecting '}'")
            } else {
                // 单项导入: import a.b 或 import a.b.*
                // 需要将表达式放入 IMPORT_ITEM 中
                val item = qualifiedName.precede()
                qualifiedName.drop()

                // 处理通配符
                if (at(DOT) && lookahead(1) == MUL) {
                    advance() // DOT
                    advance() // MUL

                    if (at(AS_KEYWORD)) {
                        errorAndAdvance(
                            CangJieParsingBundle.message("parsing.error.aliases.not.allowed.for.all.imports")
                        )
                    }
                } else if (at(AS_KEYWORD)) {
                    // 处理别名
                    val alias = mark()
                    advance() // AS_KEYWORD
                    expect(IDENTIFIER, "Expecting identifier", SEMICOLON_SET)
                    alias.done(IMPORT_ALIAS)
                }

                item.done(IMPORT_ITEM)
            }
        }

        consumeIf(SEMICOLON)
        importDirective.done(IMPORT_DIRECTIVE)
        importDirective.setCustomEdgeTokenBinders(null, TrailingCommentsBinder)
        return true
    }


    /**
     * 解析脚本文件
     *
     * Grammar:
     * ```
     * cangJieScript
     *   : statement*
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseScript() {
        val fileMarker = mark()
        fileMarker.done(CJ_SCRIPT)
    }

    /**
     * 解析仓颉源文件
     *
     * 文件解析的主入口方法。解析完整的仓颉源文件，包括包声明、导入语句和顶级声明。
     *
     * Grammar:
     * ```
     * cangJieFile
     *   : preamble topLevelDeclaration*
     *   ;
     * ```
     */
    fun parseFile() {

        with(ParsingContext.DEFAULT) {
            val fileMarker = mark()

            // 处理开头 package
            parsePreamble()

            // 处理声明式语句
            while (!eof()) {
                parseTopLevelDeclaration()
            }

            checkUnclosedBlockComment()
            fileMarker.done(CJ_FILE)
        }

    }


    /**
     * 检查未关闭的块注释
     *
     * 检查文件末尾是否有未关闭的块注释，并报告错误。
     */
    context(parseContext: ParsingContext)
    private fun checkUnclosedBlockComment() {
        if (BLOCK_DOC_COMMENT_SET.contains(builder.rawLookup(-1))) {
            val startOffset = builder.rawTokenTypeStart(-1)
            val endOffset = builder.rawTokenTypeStart(0)
            val tokenChars = builder.originalText.subSequence(startOffset, endOffset)

            if (!(tokenChars.length > 2 && tokenChars.subSequence(tokenChars.length - 2, tokenChars.length)
                    .toString() == "*/")
            ) {
                val marker = builder.mark()
                marker.error(CangJieParsingBundle.message("parsing.error.unclosed.comment"))
                marker.setCustomEdgeTokenBinders(WhitespacesBinders.GREEDY_RIGHT_BINDER, null)
            }
        }
    }


    /**
     * 解析顶层声明（无参数版本）
     *
     * 调用带参数的版本，默认不解析宏。
     */
    context(parseContext: ParsingContext)
    private fun parseTopLevelDeclaration() {
        parseTopLevelDeclaration(false)
    }


    /**
     * 解析顶层声明
     *
     * Grammar:
     * ```
     * topLevelDeclaration
     *   : classDeclaration
     *   | interfaceDeclaration
     *   | structDeclaration
     *   | enumDeclaration
     *   | extendDeclaration
     *   | functionDeclaration
     *   | variableDeclaration
     *   | typeAliasDeclaration
     *   | foreignDeclaration
     *   | macroDeclaration
     *   ;
     * ```
     *
     * @param parseMacro 是否解析宏表达式
     */
    context(parseContext: ParsingContext)
    private fun parseTopLevelDeclaration(parseMacro: Boolean) {
        if (at(SEMICOLON)) {
            advance() // SEMICOLON
            return
        }

        val decl = mark()

        // 如果有导入语句
        if (at(PUBLIC_KEYWORD) && lookahead(1) == IMPORT_KEYWORD) {
            // error("imports are only allowed in the beginning of file");
            parseImportDirectives()
            decl.drop()
            return
        }

        parseAnnotations()

        val detector = ModifierDetector()

        parseModifierList(detector, TokenSet.EMPTY, parseMacro)
        val declType = parseCommonDeclaration(detector, NameParsingMode.REQUIRED, DeclarationParsingMode.TOPLEVEL)


        if (declType == null) {
            errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element", "a top level declaration"))
            // decl.error("Expecting a top level declaration")
            decl.drop()
        } else {
            closeDeclarationWithCommentBinders(decl, declType, true)
        }
    }


    /**
     * 尝试解析修饰符
     *
     * @param tokenConsumer 修饰符令牌消费者
     * @param noModifiersBefore 禁止修饰符前的令牌集合
     * @param modifierKeywords 修饰符关键字集合
     * @return 是否成功解析修饰符
     */
    context(parseContext: ParsingContext)
    private fun tryParseModifier(
        tokenConsumer: ((IElementType) -> Unit)?, noModifiersBefore: TokenSet, modifierKeywords: TokenSet
    ): Boolean {
        val marker = mark()

        if (atSet(modifierKeywords)) {
            val lookahead = lookahead(1)

            if (at(FUNC_KEYWORD) && lookahead != INTERFACE_KEYWORD) {
                marker.rollbackTo()
                return false
            }

            if (lookahead != null && !noModifiersBefore.contains(lookahead)) {
                val tt = tt() ?: return false
                tokenConsumer?.invoke(tt)
                advance() // MODIFIER
                marker.collapse(tt)
                return true
            }
        } else if (at(CONST_KEYWORD) && lookahead(2) != EQ && lookahead(2) != COLON) {
            // 处理特殊的const修饰的
            advance() // MODIFIER
            tokenConsumer?.invoke(CONST_KEYWORD)
            marker.collapse(CONST_KEYWORD)
            return true
        } else if (at(UNSAFE_KEYWORD) && lookahead(1) != LBRACE) {
            advance()
            tokenConsumer?.invoke(UNSAFE_KEYWORD)
            marker.collapse(UNSAFE_KEYWORD)
            return true
        } else if (at(FOREIGN_KEYWORD) && lookahead(1) != LBRACE) {
            advance()
            tokenConsumer?.invoke(FOREIGN_KEYWORD)
            marker.collapse(FOREIGN_KEYWORD)
            return true
        }

        marker.rollbackTo()
        return false
    }


    /**
     * 解析修饰符列表主体
     *
     * @param tokenConsumer 修饰符令牌消费者
     * @param modifierKeywords 修饰符关键字集合
     * @param noModifiersBefore 禁止修饰符前的令牌集合
     * @param isParseMacro 是否解析宏
     * @return 是否为空列表
     */
    context(parseContext: ParsingContext)
    private fun doParseModifierListBody(
        tokenConsumer: ((IElementType) -> Unit)?,
        modifierKeywords: TokenSet,
        noModifiersBefore: TokenSet,
        isParseMacro: Boolean = false
    ): Boolean {
        var empty = true

        while (!eof()) {/*
            if (at(AT) && isParseMacro) {
                val beforeAnnotationMarker = mark()
                val type = parseAnnotation(null)
                if (type == null || type == MACRO_EXPRESSION) {
                    beforeAnnotationMarker.rollbackTo()
                    break
                } else {
                    tokenConsumer?.invoke(type)
                    beforeAnnotationMarker.done(type)
                }
            } else
            */
            if (!tryParseModifier(tokenConsumer, noModifiersBefore, modifierKeywords)) {
                // modifier not advanced
                break
            }
            empty = false
        }
        return empty
    }

    /**
     * 解析 Lambda 表达式
     *
     * Grammar:
     * ```
     * lambdaExpression
     *   : "{" parameter* "->" statement* "}"
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseLambdaExpression() {
        with(
            parseContext.copy(preferBlock = false, collapse = false, isDoubleArrow = false)
        ) {
            expressionParsing.parseFunctionLiteral()
        }
    }

    /**
     * 解析注解或宏表达式
     *
     * Grammar:
     * ```
     * annotation
     *   : "@" (annotationUseSiteTarget ":" )? unescapedAnnotation
     *   ;
     *
     * unescapedAnnotation
     *   : qualifiedName typeArguments? valueArguments?
     *   ;
     *
     * macroExpression
     *   : "@" qualifiedName "[" macroArguments? "]"
     *   ;
     * ```
     *
     * @param detector 修饰符检测器
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    fun parseAnnotation() {

        if (isBuiltInAnnotation()) {
            parseBuiltAnnotation()
            return
        }

        if (parseContext.disableMacroParsing) {
            if (!_atSet(AT, ATEXCL)) return

        } else {
            if (!_atSet(ATEXCL)) return
        }

        val mark = mark()

        advance() //消耗


//        注解名称

        parseConstructorCallee()

        if (at(LBRACKET)) {
            expressionParsing.parseValueArgumentList(LBRACKET, RBRACKET)
        }

        mark.done(ANNOTATION)
    }

    context(parseContext: ParsingContext)
            /**
             * 解析带可选参数的注解
             *
             * 解析标准格式的注解，支持可选的参数列表（使用方括号 `[...]`）。
             * 适用于多种内置注解，如 @Deprecated, @Java, @C, @Annotation 等。
             *
             * Grammar:
             * ```
             * annotationWithOptionalArguments
             *   : "@" IDENTIFIER ("[" valueArguments "]")?
             *   ;
             * ```
             *
             * 支持的注解示例：
             * ```cangjie
             * @Deprecated                           // 无参数
             * @Deprecated["Use newMethod instead"]  // 带参数
             * @Java                                 // 无参数
             * @Annotation                           // 无参数
             * ```
             */
    fun parseAnnotationWithOptionalArguments() {
        assert(_at(AT))

        val mark = mark()
        advance() // 消耗 @

        // 解析注解名称标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        parseConstructorCallee() // 消耗注解名称

        // 解析可选的参数列表 [...]
        if (at(LBRACKET)) {
            expressionParsing.parseValueArgumentList(LBRACKET, RBRACKET)
        }

        mark.done(ANNOTATION)
    }

    context(parseContext: ParsingContext)
    fun parseAnnotations() {
        val set = if (parseContext.disableMacroParsing) {
            TokenSet.create(AT, ATEXCL)

        } else {
            TokenSet.create(ATEXCL)
        }
        val mark = mark()
        while (_atSet(set) || isBuiltInAnnotation()) {
            parseAnnotation()
        }
        mark.done(ANNOTATIONS)
    }

    /**
     * 检查是否是内置注解
     *
     * 检查当前位置的注解是否为仓颉语言的内置注解。
     *
     * 内置注解包括:
     * - FFI相关: @Java, @C, @JavaMirror, @JavaImpl, @ObjCMirror, @ObjCImpl, @ForeignName, @CallingConv
     * - 编译器指令: @Attribute, @NumericOverflow, @Intrinsic, @When, @FastNative, @ConstSafe
     * - 语义标记: @Deprecated, @Frozen
     * - 元注解: @Annotation
     * - 测试相关: @EnsurePreparedToMock
     *
     * @return 如果当前注解为内置注解返回true,否则返回false
     * @see org.cangnova.cangjie.psi.CjBuiltInAnnotation
     */
    fun isBuiltInAnnotation(): Boolean {
        if (!_atSet(AT)) return false

        val lookahead = lookahead(1)
        if (lookahead != IDENTIFIER) return false

        val annotationName = rawTokenText(builder, 1).toString()

        return CjBuiltInAnnotation.isBuiltIn(annotationName)
    }

    /**
     * 解析内置注解
     *
     * 解析仓颉语言的内置注解,如 @Java, @C, @When 等。
     * 内置注解的语法格式:
     * ```
     * builtInAnnotation
     *   : "@" builtInAnnotationName ("[" argumentList "]")?
     *   ;
     * ```
     *
     *
     * 示例:
     * ```cangjie
     * @Java[package="java.lang", name="String"]
     * @Deprecated[message:"Use newMethod instead"]
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseBuiltAnnotation() {
        assert(_atSet(AT))

        val builtInAnnotation = CjBuiltInAnnotation.fromName(rawTokenText(builder, 1).toString())

        if (builtInAnnotation == null) {
            error(
                CangJieParsingBundle.message(
                    "parsing.error.unknown.built.in.annotation",
                    rawTokenText(builder, 1).toString()
                )
            )

            return
        }


        when (builtInAnnotation) {
            C, FAST_NATIVE, INTRINSIC, CONST_SAFE, FROZEN, ENSURE_PREPARED_TO_MOCK -> {
//                不需要参数的注解
                parseBuiltNonArgAnnotation(builtInAnnotation)
            }

            CALLING_CONV -> {
//                @CallingConv[CDECL] 或 @CallingConv[STDCALL]
                parseCallingConvAnnotation()
            }

            ATTRIBUTE -> {
                parseAttributeAnnotation()
            }

            OVERFLOW_THROWING, OVERFLOW_WRAPPING, OVERFLOW_SATURATING -> {
                parseOverflowAnnotation(builtInAnnotation)
            }

            WHEN ->
                parseWhenAnnotation()


            CjBuiltInAnnotation.ANNOTATION, JAVA_IMPL, OBJ_C_MIRROR, OBJ_C_IMPL, JAVA_MIRROR, FOREIGN_NAME,
            JAVA, DEPRECATED -> {
                parseAnnotationWithOptionalArguments()
            }


        }

    }


    /**
     * 解析 @CallingConv 调用约定注解
     *
     * @CallingConv 注解用于指定函数的调用约定,支持 CDECL 和 STDCALL。
     * CDECL 是 clang C 编译器在不同平台上默认使用的调用约定。
     * STDCALL 是 Win32 API 使用的调用约定。
     *
     * Grammar:
     * ```
     * callingConvAnnotation
     *   : "@" "CallingConv" "[" ("CDECL" | "STDCALL") "]"
     *   ;
     * ```
     *
     * 示例:
     * ```cangjie
     * @CallingConv[CDECL]
     * foreign func rand(): Int32
     *
     * @CallingConv[STDCALL]
     * foreign func WinApiFunction(): Int32
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseCallingConvAnnotation() {
        assert(_atSet(AT))

        val mark = mark()
        advance() // 消耗 @

        // 解析 CallingConv 标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        val annotationName = builder.tokenText
        if (annotationName != "CallingConv") {
            error(CangJieParsingBundle.message("parsing.error.expecting.annotation", "CallingConv"))
            mark.done(ANNOTATION)
            return
        }

        parseConstructorCallee()// 消耗 CallingConv

        // 解析参数: [CDECL] 或 [STDCALL]
        if (!expect(LBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "["))) {
            mark.done(ANNOTATION)
            return
        }

        // 使用专用节点类型封装调用约定参数
        val callingConvMark = mark()

        // 解析调用约定标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.calling.convention"))
            callingConvMark.drop()
            mark.done(ANNOTATION)
            return
        }

        val callingConv = builder.tokenText?.toString()
        if (callingConv != null && !CallingConvention.isValid(callingConv)) {
            error(CangJieParsingBundle.message("parsing.error.invalid.calling.convention", callingConv))
        }

        advance() // 消耗调用约定标识符

        callingConvMark.done(ANNOTATION_CALLING_CONV)

        expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))

        mark.done(ANNOTATION)
    }

    /**
     * 解析 @Java 注解
     *
     * @Java 注解用于与Java代码进行互操作,只接受一个字符串参数。
     *
     * Grammar:
     * ```
     * javaAnnotation
     *   : "@" "Java" ("[" stringLiteral "]")?
     *   ;
     * ```
     *
     * 示例:
     * ```cangjie
     * @Java["java.lang.String"]
     * @Java
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseJavaAnnotation(

    ) {
        assert(_atSet(AT))

        val mark = mark()
        advance() // 消耗 @

        //
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        val annotationName = builder.tokenText
        if (annotationName != "Java") {
            error(CangJieParsingBundle.message("parsing.error.expecting.annotation", "Java"))
            mark.done(ANNOTATION)
            return
        }

        advance() // 消耗 Java

        // 检查是否有参数
        if (!at(LBRACKET)) {
            // @Java 注解可以不带参数
            mark.done(ANNOTATION)
            return
        }

        advance() // 消耗 [

        builder.disableNewlines()

        // @Java 注解只接受一个字符串参数
        if (at(OPEN_QUOTE)) {
            with(parseContext.copy(processStringInterpolation = false)) {
                expressionParsing.parseStringTemplate()
            }
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.string.literal"))
        }

        builder.enableNewlines()

        expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))

        mark.done(ANNOTATION)
    }


    context(parseContext: ParsingContext)
    fun parseConstructorCallee() {
        val reference = mark()
        parseTypeRef()
        reference.done(CONSTRUCTOR_CALLEE)
    }


    /**
     * 解析溢出注解
     *
     * 解析 @OverflowThrowing, @OverflowWrapping, @OverflowSaturating 注解。
     * 这些注解可以带可选参数来指定具体的溢出策略。
     *
     * Grammar:
     * ```
     * overflowAnnotation
     *   : "@" ("OverflowThrowing" | "OverflowWrapping" | "OverflowSaturating") ("[" identifier "]")?
     *   ;
     * ```
     *
     * 示例:
     * ```cangjie
     * @OverflowThrowing
     * @OverflowThrowing[checked]
     * @OverflowWrapping[wrapping]
     * @OverflowSaturating[saturating]
     * ```
     *
     * @param builtInAnnotation 内置注解类型
     */
    context(parseContext: ParsingContext)
    private fun parseOverflowAnnotation(
        builtInAnnotation: CjBuiltInAnnotation
    ) {
        assert(_atSet(AT))

        val mark = mark()
        advance() // 消耗 @

        // 解析注解标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        val annotationName = builder.tokenText
        if (annotationName != builtInAnnotation.annotationName) {
            error(CangJieParsingBundle.message("parsing.error.expecting.annotation", builtInAnnotation.annotationName))
            mark.done(ANNOTATION)
            return
        }

        parseConstructorCallee()// 消耗注解名称

        // 检查是否有参数
        if (at(LBRACKET)) {
            advance() // 消耗 [
            // 使用专用节点类型封装调用约定参数
            val overflow = mark()
            builder.disableNewlines()

            // 解析溢出策略标识符
            if (at(IDENTIFIER)) {
                val strategyName = builder.tokenText?.toString()
                if (strategyName != null && !OverflowStrategy.isValid(strategyName)) {
                    error(
                        CangJieParsingBundle.message(
                            "parsing.error.invalid.overflow.strategy",
                            strategyName
                        )
                    )
                }
                advance() // 消耗策略标识符
            } else {
                error(CangJieParsingBundle.message("parsing.error.expecting.overflow.strategy"))
            }

            builder.enableNewlines()
            overflow.done(ANNOTATION_OVERFLOW_STRATEGY)
            expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))
        }
        // 如果没有参数,从注解名称中提取策略(如 OverflowThrowing -> throwing)

        mark.done(ANNOTATION)
    }

    /**
     * 解析不需要参数的内置注解
     *
     * 解析简单的内置注解,这些注解不需要任何参数,如 @C, @FastNative, @Intrinsic 等。
     * 语法格式:
     * ```
     * nonArgAnnotation
     *   : "@" annotationName
     *   ;
     * ```
     *
     * @param builtInAnnotation 内置注解类型
     *
     * 示例:
     * ```cangjie
     * @C
     * @FastNative
     * @Intrinsic
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseBuiltNonArgAnnotation(
        builtInAnnotation: CjBuiltInAnnotation
    ) {
        assert(_atSet(AT))

        val mark = mark()
        advance() // 消耗 @

        // 解析注解标识符
        if (!at(IDENTIFIER)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
            mark.done(ANNOTATION)
            return
        }

        val annotationName = builder.tokenText
        if (annotationName != builtInAnnotation.annotationName) {
            error(CangJieParsingBundle.message("parsing.error.expecting.annotation", builtInAnnotation.annotationName))
            mark.done(ANNOTATION)
            return
        }
        parseConstructorCallee()


        mark.done(ANNOTATION)
    }

    /**
     * (modifier )*
     *
     * 如果不为空，则将修饰符(非批注)馈送到传递的使用者
     *
     * @param noModifiersBefore 是一个令牌集，其中包含指示何时满足这些元素的元素。
     *                          必须将前一个令牌解析为标识符，而不是修饰符
     */
    context(parseContext: ParsingContext)
    fun parseModifierList(
        tokenConsumer: ((IElementType) -> Unit)?, noModifiersBefore: TokenSet, isParseMacro: Boolean = false
    ): Boolean {

        return doParseModifierList(tokenConsumer, MODIFIER_KEYWORDS, noModifiersBefore, isParseMacro)
    }


    /**
     * 私有的修饰符列表解析方法
     *
     * @param tokenConsumer 修饰符令牌消费者
     * @param modifierKeywords 修饰符关键字集合
     * @param noModifiersBefore 禁止修饰符前的令牌集合
     * @param isParseMacro 是否解析宏
     * @return 是否解析到修饰符
     */
    context(parseContext: ParsingContext)
    private fun doParseModifierList(
        tokenConsumer: ((IElementType) -> Unit)?,
        modifierKeywords: TokenSet,
        noModifiersBefore: TokenSet,
        isParseMacro: Boolean = false
    ): Boolean {
        val list = mark()

        val empty = doParseModifierListBody(tokenConsumer, modifierKeywords, noModifiersBefore, isParseMacro)

        // 始终创建 MODIFIER_LIST 占位符，保持与反编译 Stub 结构一致
        list.done(MODIFIER_LIST)
        return !empty
    }


    /**
     * 解析类通用声明
     *
     * 处理类内部的通用声明，包括函数、属性、变量等。
     *
     * @param tokenId 令牌 ID
     * @param classdetector 类修饰符检测器
     * @param detector 修饰符检测器
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    private fun parseClassCommonDeclaration(
        tokenId: Int?, classdetector: ModifierDetector, detector: ModifierDetector
    ): IElementType? {
        return when (getTokenId()) {
            AT_Id -> with(parseContext.copy(backToken = true)) {
                expressionParsing.parseMacroExpression()
            }

            FUNC_KEYWORD_Id -> if (tokenId != null) {
                if (tokenId == INTERFACE_KEYWORD_Id) {
                    parseFunction(true, classdetector, detector, topTokenId = tokenId)
                } else {
                    parseFunction(classdetector = classdetector, detector = detector, topTokenId = tokenId)
                }
            } else {
                parseFunction(detector = detector, topTokenId = tokenId)
            }

            PROP_KEYWORD_Id -> if (tokenId != null && tokenId == INTERFACE_KEYWORD_Id) {
                parseProperty(true, classdetector, detector)
            } else {
                parseProperty(classdetector = classdetector, detector = detector)
            }

            LET_KEYWORD_Id, VAR_KEYWORD_Id, CONST_KEYWORD_Id -> parseField(classdetector)

            CLASS_KEYWORD_Id, INTERFACE_KEYWORD_Id, STRUCT_KEYWORD_Id, ENUM_KEYWORD_Id, EXTEND_KEYWORD_Id -> {

                with(parseContext.copy(shouldReportError = false)) {


                    parseClass(detector)
                }

            }

            else -> null
        }
    }


    /**
     * 解析类初始化器（占位方法）
     *
     * 当前未实现，返回 null。
     *
     * @return 总是返回 null
     */
    context(parseContext: ParsingContext)
    private fun parseClassInitializer(): IElementType? {
        return null
    }


    /**
     * 解析属性委托或赋值
     *
     * Grammar:
     * ```
     * propertyDelegateOrAssignment
     *   : "=" expression
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    context(parseContext: ParsingContext)
    private fun parsePropertyDelegateOrAssignment(): Boolean {
        if (at(EQ)) {
            advance() // consume EQ token

            expressionParsing.parseExpression()

            return true
        }
        return false
    }


    /**
     * 寻找接收者后的最后一个点
     *
     * 用于处理接收者类型的解析。
     *
     * @return 最后一个点的位置
     */
    context(parseContext: ParsingContext)
    private fun lastDotAfterReceiver(): Int {
        val pattern = if (at(LPAR)) lastDotAfterReceiverLParPattern else lastDotAfterReceiverNotLParPattern
        pattern.reset()
        return matchTokenStreamPredicate(pattern)
    }


    /**
     * 解析接收者类型
     *
     * @param title 标题名称
     * @param nameFollow 名称跟随的令牌集合
     * @return 是否存在接收者类型
     */
    context(parseContext: ParsingContext)
    private fun parseReceiverType(
        title: String,
        nameFollow: TokenSet
    ): Boolean {
        val lastDot = lastDotAfterReceiver()
        val receiverPresent = lastDot != -1

        if (!receiverPresent) return false

        createTruncatedBuilder(lastDot).parseTypeRef()

        if (atSet(RECEIVER_TYPE_TERMINATORS)) {
            advance() // expectation
        } else {
            errorWithRecovery(CangJieParsingBundle.message("parsing.error.expecting.symbol", "."), nameFollow)
        }
        return true
    }


    /**
     * 解析类成员字段声明
     *
     * field
     *   : modifiers ("let" | "var" | "const") SimpleName (":" type) ("=" expression)?
     *   | modifiers ("let" | "var" | "const") SimpleName (":" type)? "=" expression
     *   ;
     *
     * 成员字段使用简单标识符，不支持模式匹配
     * 成员字段必须有类型声明或初始化表达式
     */
    context(parseContext: ParsingContext)
    fun parseField(
        classdetector: ModifierDetector
    ): IElementType {
        assert(at(LET_KEYWORD) || at(VAR_KEYWORD) || at(CONST_KEYWORD))
        advance()

        // 成员字段使用简单标识符
        parseIdentifierByTitle("field", PROPERTY_NAME_FOLLOW_SET, true)

        var hasTypeRef = false
        var hasInitializer = false

        if (at(COLON)) {
            advance() // COLON
            parseTypeRef()
            hasTypeRef = true
        }

        if (at(EQ)) {
            advance() // EQ
            expressionParsing.parseExpression()
            hasInitializer = true
        }

        // 成员字段必须有类型声明或初始化表达式
        if (!hasTypeRef && !hasInitializer) {
            error(CangJieParsingBundle.message("parsing.error.field.requires.type.or.initializer"))
        }

        return FIELD
    }

    /**
     * 解析变量声明（支持模式匹配）
     *
     * variableDeclaration
     *   : modifiers ("let" | "var" | "const") pattern (":" type)? ("=" expression)?
     *   ;
     *
     * 用于顶层变量和局部变量，支持解构赋值如 let (a, b) = tuple
     */
    context(parseContext: ParsingContext)
    fun parseVariable(
        classdetector: ModifierDetector
    ): IElementType {
        assert(at(LET_KEYWORD) || at(VAR_KEYWORD) || at(CONST_KEYWORD))
        advance()

        // 变量声明使用模式匹配
        expressionParsing.parsePattern(
            CangJieExpressionParsing.PatternParseContext.VARIABLE_DECL
        )

        if (at(COLON)) {
            advance() // COLON
            parseTypeRef()
        }

        if (at(EQ)) {
            advance() // EQ
            expressionParsing.parseExpression()
        }

        return VARIABLE
    }

    /**
     * 解析表达式代码片段
     *
     * 用于调试和代码片段解析。
     */
    context(parseContext: ParsingContext)
    fun parseExpressionCodeFragment() {
        val marker = mark()
        expressionParsing.parseExpression()


        checkForUnexpectedSymbols()

        marker.done(EXPRESSION_CODE_FRAGMENT)
    }

    /**
     * 解析块代码片段
     *
     * 用于调试和代码片段解析。
     */
    context(parseContext: ParsingContext)
    fun parseBlockCodeFragment() {
        val marker = mark()
        val blockMarker = mark()

        if (at(PACKAGE_KEYWORD) || at(IMPORT_KEYWORD)) {
            val err = mark()
            parsePreamble()
            err.error(
                CangJieParsingBundle.message(
                    "parsing.error.not.allowed.context", "Package directive and imports", "code fragments"
                )
            )
        }


        expressionParsing.parseStatements()

        checkForUnexpectedSymbols()

        blockMarker.done(BLOCK)
        marker.done(BLOCK_CODE_FRAGMENT)
    }

    private interface DeclarationParser {
        fun isValidInScope(scope: DeclarationParsingMode): Boolean

        context(parseContext: ParsingContext)
        fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType?
    }

    private abstract class ScopedDeclarationParser(
        private val validScopes: Set<DeclarationParsingMode>
    ) : DeclarationParser {
        override fun isValidInScope(scope: DeclarationParsingMode): Boolean {
            return validScopes.contains(scope)
        }

        context(parseContext: ParsingContext)
        override fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            val type = doParse(parser, detector, nameParsingMode, scope)
            if (!isValidInScope(scope)) {
                return null
            }
            return type
        }

        context(parseContext: ParsingContext)
        protected abstract fun doParse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType?
    }

    private class TypeAliasParser : ScopedDeclarationParser(
        setOf(DeclarationParsingMode.ALL, DeclarationParsingMode.TOPLEVEL)
    ) {
        context(parseContext: ParsingContext)
        override fun doParse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseTypeAlias()
        }
    }

    private class ForeignParser : ScopedDeclarationParser(
        setOf(DeclarationParsingMode.ALL, DeclarationParsingMode.TOPLEVEL)
    ) {
        context(parseContext: ParsingContext)
        override fun doParse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseForeign()
        }
    }

    private class MainFuncParser : ScopedDeclarationParser(
        setOf(DeclarationParsingMode.ALL, DeclarationParsingMode.TOPLEVEL)
    ) {
        context(parseContext: ParsingContext)
        override fun doParse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseMainFunc()
        }
    }

    private class ClassParser : ScopedDeclarationParser(
        setOf(DeclarationParsingMode.ALL, DeclarationParsingMode.TOPLEVEL)
    ) {
        context(parseContext: ParsingContext)
        override fun doParse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseClass(detector)
        }


    }

    private class MacroParser : DeclarationParser {
        override fun isValidInScope(scope: DeclarationParsingMode): Boolean = true

        context(parseContext: ParsingContext)
        override fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType {
            return with(parseContext.copy(disableMacroParsing = false, allowParseAnnotationsInValueParameter = false)) {
                parser.parseMacro()
            }

        }
    }

    private class FunctionParser : DeclarationParser {
        override fun isValidInScope(scope: DeclarationParsingMode): Boolean = true

        context(parseContext: ParsingContext)
        override fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseFunction(detector = detector)
        }
    }

    private class VariableParser : DeclarationParser {
        override fun isValidInScope(scope: DeclarationParsingMode): Boolean =
            scope != DeclarationParsingMode.MEMBER // 成员字段由 FieldParser 处理

        context(parseContext: ParsingContext)
        override fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseVariable(detector)
        }
    }

    private class FieldParser : DeclarationParser {
        override fun isValidInScope(scope: DeclarationParsingMode): Boolean =
            scope == DeclarationParsingMode.MEMBER

        context(parseContext: ParsingContext)
        override fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {
            return parser.parseField(detector)
        }
    }

    private class MacroExpressionParser(private val expressionParsing: CangJieExpressionParsing) : DeclarationParser {
        override fun isValidInScope(scope: DeclarationParsingMode): Boolean = true

        context(parseContext: ParsingContext)
        override fun parse(
            parser: CangJieParsing,
            detector: ModifierDetector,
            nameParsingMode: NameParsingMode,
            scope: DeclarationParsingMode
        ): IElementType? {


            return with(parseContext.copy(backToken = true)) {
                expressionParsing.parseMacroExpression()
            }
        }
    }

    private val declarationParsers: Map<Int, DeclarationParser> by lazy {
        mapOf(
            TYPE_KEYWORD_Id to TypeAliasParser(),
            AT_Id to MacroExpressionParser(expressionParsing),
            FOREIGN_KEYWORD_Id to ForeignParser(),
            MACRO_KEYWORD_Id to MacroParser(),
            FUNC_KEYWORD_Id to FunctionParser(),
            MAIN_KEYWORD_Id to MainFuncParser(),
            EXTEND_KEYWORD_Id to ClassParser(),
            ENUM_KEYWORD_Id to ClassParser(),
            STRUCT_KEYWORD_Id to ClassParser(),
            INTERFACE_KEYWORD_Id to ClassParser(),
            CLASS_KEYWORD_Id to ClassParser(),
            LET_KEYWORD_Id to VariableParser(),
            VAR_KEYWORD_Id to VariableParser(),
            CONST_KEYWORD_Id to VariableParser()
        )
    }

    /**
     * 解析通用声明
     *
     * 根据令牌类型选择合适的解析器进行声明解析。
     *
     * @param detector 修饰符检测器
     * @param nameParsingMode 名称解析模式
     * @param declarationParsingMode 声明解析模式
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    fun parseCommonDeclaration(
        detector: ModifierDetector, nameParsingMode: NameParsingMode, declarationParsingMode: DeclarationParsingMode
    ): IElementType? {
        val tokenId = getTokenId() ?: return null
        val parser = declarationParsers[tokenId] ?: return null
//        先处理
        val type = parser.parse(this, detector, nameParsingMode, declarationParsingMode)
//        TODO 先处理还是先返回INVALID_DECLARATION？
        if (!parser.isValidInScope(declarationParsingMode)) {

            return INVALID_DECLARATION
        }


        return type

    }


    /**
     * 解析类型别名
     *
     * Grammar:
     * ```
     * typeAliasDeclaration
     *   : modifier* "type" simpleName typeParameters? "=" typeReference
     *   ;
     * ```
     *
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    private fun parseTypeAlias(): IElementType {
        assert(_at(TYPE_KEYWORD))

        advance() // TYPE_KEYWORD

        expect(IDENTIFIER, "Type name expected", LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET)

        parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET)

        if (at(WHERE_KEYWORD)) {
            val error = mark()
            parseTypeConstraints()
            error.error(CangJieParsingBundle.message("parsing.error.not.allowed", "Type alias parameters bounds"))
        }

        expect(EQ, "Expecting '='", TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET)

        parseTypeRef()

        consumeIf(SEMICOLON)

        return TYPEALIAS
    }


    /**
     * 解析属性声明
     *
     * Grammar:
     * ```
     * propertyDeclaration
     *   : modifier* "prop" simpleName ":" typeReference propertyBody?
     *   ;
     * ```
     *
     * @param isInterface 是否在接口中
     * @param classdetector 类修饰符检测器
     * @param detector 修饰符检测器
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    fun parseProperty(
        isInterface: Boolean = false, classdetector: ModifierDetector? = null, detector: ModifierDetector? = null
    ): IElementType {
        assert(_at(PROP_KEYWORD))

        // advance past 'prop' keyword
        advance()

        parseIdentifierByTitle(" prop ")

        parseByType()

        if (isDeclarationsFile) {
            if (at(LBRACE)) {
                val body = mark()

                val tokenSet = TokenSet.orSet(
                    KEYWORDS, TokenSet.create(OPEN_KEYWORD, ABSTRACT_KEYWORD, SEALED_KEYWORD)
                )
                while (!atSet(tokenSet) && !eof()) {
                    advance()
                }
                error(
                    CangJieParsingBundle.message(
                        "parsing.error.not.allowed.context", "Property body", "declarations file"
                    )
                )
            }
            return PROPERTY
        }

        if (at(LBRACE)) {
            parsePropertyBody(detector)
        } else if (!isInterface) {
            if (classdetector != null && !classdetector.isAbstractDetected) {
                error(CangJieParsingBundle.message("parsing.error.unimplemented.abstract.property"))
                error(CangJieParsingBundle.message("parsing.error.missing", "prop body. Expecting '{''"))
            }
        }

        return PROPERTY
    }


    /**
     * 解析类型注解
     *
     * Grammar:
     * ```
     * typeAnnotation
     *   : ":" typeReference
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseByType() {
        if (at(COLON)) {
            advance() // COLON
            parseTypeRef()
        } else {
            error(CangJieParsingBundle.message("parsing.error.missing", "type. Expecting ':' type"))
        }
    }


    /**
     * 解析属性 getter 访问器
     *
     * Grammar:
     * ```
     * propertyGetter
     *   : "get" "(" ")" block
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parsePropertyGet() {
        assert(_at(GET_KEYWORD))

        val get = mark()
        advance() // GET_KEYWORD

        if (expect(LPAR, "Expecting '('")) {
            if (expect(RPAR, "Expecting ')'")) {
                if (at(LBRACE)) {
                    parseBlock()
                } else {
                    error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))
                }
            }
        }

        get.done(PROPERTY_ACCESSOR)
    }


    /**
     * 解析属性 setter 访问器
     *
     * Grammar:
     * ```
     * propertySetter
     *   : "set" "(" parameter ")" block
     *   ;
     * ```
     *
     * @param detector 修饰符检测器
     */
    context(parseContext: ParsingContext)
    private fun parsePropertySet(detector: ModifierDetector?) {
        assert(_at(SET_KEYWORD))
        val set = mark()

        // if(detector?.isMutDetected() == true) {
        advance() // SET_KEYWORD

        if (expect(LPAR, "Expecting '('")) {
            val plist = mark()
            val value = mark()

            if (!expect(UNDERLINE)) {
                expect(IDENTIFIER, "Expecting identifier")
            }

            value.done(VALUE_PARAMETER)
            plist.done(VALUE_PARAMETER_LIST)

            if (expect(RPAR, "Expecting ')'")) {
                if (at(LBRACE)) {
                    parseBlock()
                } else {
                    error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))
                }
            }
        }
        // } else {
        //     error("immutable property cannot have setter")
        //     set.drop()
        //     return
        // }

        // if (detector?.isMutDetected() != true) {
        //     set.error("immutable property cannot have setter")
        //     return
        // }

        set.done(PROPERTY_ACCESSOR)
    }


    /**
     * 解析属性体
     *
     * Grammar:
     * ```
     * propertyBody
     *   : "{" propertyAccessor* "}"
     *   ;
     *
     * propertyAccessor
     *   : propertyGetter | propertySetter
     *   ;
     * ```
     *
     * @param detector 修饰符检测器
     */
    context(parseContext: ParsingContext)
    private fun parsePropertyBody(detector: ModifierDetector?) {
        assert(_at(LBRACE))

        val body = mark()
        advance() // LBRACE

        // var isGet = false
        // var isSet = false
        // while (at(GET_KEYWORD) || at(SET_KEYWORD)) {
        //     if (at(GET_KEYWORD) && !isGet) {
        //         isGet = true
        //         parsePropertyGet()
        //     }
        //     if (at(SET_KEYWORD) && !isSet) {
        //         isSet = true
        //         parsePropertySet(detector)
        //     }
        // }
        //
        // if (!isGet) {
        //     error("Get accessor should be implemented")
        // }
        //
        // if (!isSet && detector?.isMutDetected() == true) {
        //     error("Set accessor should be implemented")
        // }

        while (at(GET_KEYWORD) || at(SET_KEYWORD)) {
            parsePropertyAccessor()
        }

        expect(RBRACE, "Expecting '}'")

        body.done(PROPERTY_BODY)
    }


    context(parseContext: ParsingContext)
    private fun parsePropertyAccessor() {
        if (at(GET_KEYWORD)) {
            parsePropertyGet()
        }
        if (at(SET_KEYWORD)) {
            parsePropertySet(null)
        }
    }


    /**
     * 解析枚举类型
     *
     * Grammar:
     * ```
     * enumDeclaration
     *   : modifier* "enum" simpleName "{" "|" enumEntry ("|" enumEntry)* memberDeclaration* "}"
     *   ;
     * ```
     *
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    private fun parseEnum(): IElementType {
        assert(_at(ENUM_KEYWORD))
        advance()

        parseIdentifierByTitle("enum", IDENTIFIER_RBRACKET_LBRACKET_SET, false)

        parseEnumBody()

        return ENUM
    }


    context(parseContext: ParsingContext)
    private fun parseEnumBody() {
        val body = mark()
        if (at(LBRACE)) {
            advance() // LBRACE

            expect(OR)

            if (at(IDENTIFIER)) {
                parseEnumList()
            } else {
                error(CangJieParsingBundle.message("parsing.error.expecting.element", "enum constructor"))
            }

            parseMembers(null, null)

            expect(RBRACE, "Expecting '}'")
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))
        }
        body.done(ENUM_BODY)
    }


    context(parseContext: ParsingContext)
    private fun parseEnumList() {
        while (true) {
            // 检查是否为非穷举枚举的省略号
            if (at(ELLIPSIS)) {
                advance() // ELLIPSIS
                // 省略号必须是最后一个构造器
                break
            }

            parseEnumEntry()
            when {
                at(RBRACE) -> break
                at(OR) -> advance()
                else -> break
            }
        }
    }

    context(parseContext: ParsingContext)
    fun parseTypeCodeFragment() {
        val marker = mark()
        parseTypeRef()

        checkForUnexpectedSymbols()

        marker.done(TYPE_CODE_FRAGMENT)
    }


    /**
     * 检查意外符号
     *
     * 在代码片段解析中检查是否有意外的符号。
     */
    context(parseContext: ParsingContext)
    private fun checkForUnexpectedSymbols() {
        while (!eof()) {
            errorAndAdvance(CangJieParsingBundle.message("parsing.error.unexpected", "symbol"))
        }
    }


    /**
     * 解析枚举项
     *
     * Grammar:
     * ```
     * enumEntry
     *   : simpleName ("(" typeList ")" )?
     *   ;
     * ```
     *
     * @param isCreateMark 是否创建标记
     * @return 是否成功解析
     */
    context(parseContext: ParsingContext)
    fun parseEnumEntry(isCreateMark: Boolean = true): Boolean {
        val entry = if (isCreateMark) mark() else null

        if (!expect(IDENTIFIER, "Expecting enum constructor name")) {
            entry?.drop()
            return false
        }

//    parseIdentifierByTitle("enum constructor", IDENTIFIER_RBRACKET_LBRACKET_SET)

//    处理泛型
//    parseTypeArgumentList()

        if (at(LPAR)) {
            advance() // LPAR
            parseTypeList()
            expect(RPAR, "Expecting ')'")
        }

        entry?.done(ENUM_CONSTRUCTOR)
        return true
    }

    /**
     * 解析类型列表
     *
     * Grammar:
     * ```
     * typeList
     *   : typeReference ("," typeReference)*
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseTypeList() {
        val list = mark()

        while (true) {
            parseTypeRef()
            if (!at(COMMA)) break
            advance() // COMMA
        }

        list.done(TYPE_LIST)
    }


    /**
     * 解析类型参数列表
     *
     * Grammar:
     * ```
     * typeParameterList
     *   : "<" typeParameter ("," typeParameter)* ">"
     *   ;
     * ```
     *
     * @param recoverySet 恢复伤口的token集合
     * @return 是否成功解析
     */
    context(parseContext: ParsingContext)
    private fun parseTypeParameterList(recoverySet: TokenSet): Boolean {
        var result = false
        if (at(LT)) {
            val list = mark()

            builder.disableNewlines()
            advance() // LT

            while (true) {
                if (at(COMMA)) errorAndAdvance(
                    CangJieParsingBundle.message(
                        "parsing.error.expecting.element", "type parameter declaration"
                    )
                )
                parseTypeParameter()
//          parseTypeRef(true)

                if (!at(COMMA)) break
                advance() // COMMA
                if (at(GT)) {
                    break
                }
            }

            expect(GT, "Missing '>'", recoverySet)
            builder.restoreNewlinesState()
            result = true

            list.done(TYPE_PARAMETER_LIST)
        }
        return result
    }


    /**
     * 解析类型参数
     *
     * Grammar:
     * ```
     * typeParameter
     *   : modifier* simpleName (":" userType)?
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseTypeParameter() {
        if (atSet(TYPE_PARAMETER_GT_RECOVERY_SET)) {
            error(CangJieParsingBundle.message("parsing.error.type.parameter.declaration.expected"))
            return
        }

        val mark = mark()

//  parseModifierList(GT_COMMA_COLON_SET)

        expect(IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY)

//  if (at(COLON)) {
//      advance() // COLON
//      parseTypeRef()
//  }

        mark.done(TYPE_PARAMETER)
    }


    /**
     * 解析继承说明符
     *
     * Grammar:
     * ```
     * delegationSpecifier
     *   : typeReference
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseDelegationSpecifier() {
        val delegator = mark()
        val reference = mark()
        parseTypeRef()

        reference.drop()
        delegator.done(SUPER_TYPE_ENTRY)
    }


    /**
     * 解析继承说明符列表
     *
     * Grammar:
     * ```
     * delegationSpecifierList
     *   : delegationSpecifier ("&" delegationSpecifier)*
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseDelegationSpecifierList() {
        val list = mark()

        while (true) {
            if (at(AND)) {
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.delegation.specifier"))
                continue
            }
            parseDelegationSpecifier()
            if (!at(AND)) break
            advance() // COMMA
        }

        list.done(SUPER_TYPE_LIST)
    }

    /**
     * (modifier)*
     */
    context(parseContext: ParsingContext)
    fun parseModifierList(noModifiersBefore: TokenSet): Boolean {
        return parseModifierList(null, noModifiersBefore)
    }

    /**
     * 解析类声明
     *
     * Grammar:
     * ```
     * classDeclaration
     *   : modifier* ("class" | "interface" | "struct" | "enum" | "extend")
     *     (typeParameters)? simpleName? ("<:" delegationSpecifierList)?
     *     typeConstraints? classBody?
     *   ;
     * ```
     *
     * @param detector 修饰符检测器
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    fun parseClass(
        detector: ModifierDetector, nameParsingMode: NameParsingMode = NameParsingMode.REQUIRED
    ): IElementType {
        val tokenId = getTokenId()
        val token = builder.tokenType

        // assert _atSet(CLASS_INTERFACE_STRUCT_ENUM_EXTEND_SET)
        assert(_atSet(CLASS_INTERFACE_STRUCT_ENUM_EXTEND_SET))
        advance()

        var typeParametersDeclared = false

        if (token == EXTEND_KEYWORD) {
            if (at(LT)) {
                parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET)
                typeParametersDeclared = true
            }
            parseTypeRef()
        } else {

            parseIdentifier(recoverySet = CLASS_NAME_RECOVERY_SET) // 类名


            typeParametersDeclared = parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET)
        }


        if (at(LTCOLON)) {
            advance() // COLON
            parseDelegationSpecifierList()
        }

        val whereMarker = OptionalMarker(false)
        parseTypeConstraintsGuarded(typeParametersDeclared)
        whereMarker.error(CangJieParsingBundle.message("parsing.error.where.clause.not.allowed"))

        if (at(LBRACE)) {
            when (tokenId) {
                ENUM_KEYWORD_Id -> parseEnumBody()
                EXTEND_KEYWORD_Id, STRUCT_KEYWORD_Id, INTERFACE_KEYWORD_Id, CLASS_KEYWORD_Id -> parseClassBody(
                    tokenId, detector
                )

                else -> parseClassBody(tokenId, detector)
            }
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.brace.or.inherit")) // 应该为 '{' 或者继承
        }

        return when (tokenId) {
            INTERFACE_KEYWORD_Id -> INTERFACE
            STRUCT_KEYWORD_Id -> STRUCT
            CLASS_KEYWORD_Id -> CLASS
            ENUM_KEYWORD_Id -> ENUM
            EXTEND_KEYWORD_Id -> EXTEND
            else -> CLASS
        }
    }


    /**
     * typeConstraints
     *   : ("where" typeConstraint{","})?
     *   ;
     */
    context(parseContext: ParsingContext)
    private fun parseTypeConstraintsGuarded(
        typeParameterListOccurred: Boolean
    ) {
        val error = mark()
        val constraints = parseTypeConstraints()
        errorIf(
            error,
            constraints && !typeParameterListOccurred,
            "Type constraints are not allowed when no type parameters declared"
        )
    }


    context(parseContext: ParsingContext)
    private fun parseTypeConstraints(): Boolean {
        if (at(WHERE_KEYWORD)) {
            parseTypeConstraintList()
            return true
        }
        return false
    }


    /**
     * 解析类型约束列表
     *
     * Grammar:
     * ```
     * typeConstraintList
     *   : "where" typeConstraint ("," typeConstraint)*
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseTypeConstraintList() {
        assert(_at(WHERE_KEYWORD))

        advance() // WHERE_KEYWORD

        val list = mark()

        while (true) {
            if (at(COMMA)) errorAndAdvance(CangJieParsingBundle.message("parsing.error.type.constraint.expected"))
            parseTypeConstraint()
            if (!at(COMMA)) break
            advance() // COMMA
        }

        list.done(TYPE_CONSTRAINT_LIST)
    }


    /**
     * 解析类型约束
     *
     * Grammar:
     * ```
     * typeConstraint
     *   : simpleName "<:" typeReference ("&" typeReference)*
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseTypeConstraint() {
        val constraint = mark()

        val reference = mark()
        if (expect(IDENTIFIER, "Expecting type parameter name", LTCOLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET)) {
            reference.done(REFERENCE_EXPRESSION)
        } else {
            reference.drop()
        }

        expect(LTCOLON, "Expecting '<:' before the upper bound", LBRACE_RBRACE_TYPE_REF_FIRST_SET)

        do {
            if (at(AND)) advance()
            parseTypeRef()
        } while (at(AND))

        constraint.done(TYPE_CONSTRAINT)
    }


    /**
     * 解析成员声明
     *
     * @param tokenId 令牌 ID
     * @param classdetector 类修饰符检测器
     * @param rollbackMacro 是否回滚宏
     */
    context(parseContext: ParsingContext)
    private fun parseMemberDeclaration(
        tokenId: Int?, classdetector: ModifierDetector, rollbackMacro: Boolean = false
    ) {
        if (at(SEMICOLON)) {
            advance() // SEMICOLON
            return
        }
        val decl = mark()


        parseAnnotations()

        val detector = ModifierDetector()
        parseModifierList(detector, TokenSet.EMPTY, rollbackMacro)

        val declType = parseMemberDeclarationRest(tokenId, classdetector, detector)


        when {
            declType == null -> {
                errorWithRecovery(
                    CangJieParsingBundle.message("parsing.error.expecting.member.declaration"), TokenSet.EMPTY
                )
                decl.drop()
            }

            declType == CLASS || declType == INTERFACE || declType == STRUCT || declType == ENUM || declType == EXTEND -> {
                val declTypeName = when (declType) {
                    CLASS -> "class"
                    INTERFACE -> "interface"
                    STRUCT -> "struct"
                    ENUM -> "enum"
                    EXTEND -> "extend"
                    else -> "class"
                }

                val containerName = when (tokenId) {
                    CLASS_KEYWORD_Id -> "class"
                    INTERFACE_KEYWORD_Id -> "interface"
                    STRUCT_KEYWORD_Id -> "struct"
                    ENUM_KEYWORD_Id -> "enum"
                    EXTEND_KEYWORD_Id -> "extend"
                    else -> "class"
                }

                val error = decl.precede()
                closeDeclarationWithCommentBinders(decl, declType, true)

                error(
                    error, CangJieParsingBundle.message(
                        "parsing.error.invalid.declaration.in.class", declTypeName, containerName
                    )
                )
            }

            else -> {
                closeDeclarationWithCommentBinders(decl, declType, true)
            }
        }
    }


    /**
     * 解析成员声明的剩余部分
     *
     * @param tokenId 令牌 ID
     * @param classdetector 类修饰符检测器
     * @param detector 修饰符检测器
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    private fun parseMemberDeclarationRest(
        tokenId: Int?, classdetector: ModifierDetector, detector: ModifierDetector
    ): IElementType? {
        var declType = parseClassCommonDeclaration(tokenId, classdetector, detector)

        if (declType != null) return declType

        if (tokenId != null && tokenId != INTERFACE_KEYWORD_Id) {

            // 注意：修饰符已在 parseMemberDeclaration 中解析，这里不需要再调用 parseModifierList

            when {
                at(INIT_KEYWORD) -> {
                    parseInitFunc()
                    declType = SECONDARY_CONSTRUCTOR
                }

                at(LBRACE) -> {
                    error(CangJieParsingBundle.message("parsing.error.expecting.member.declaration"))
                    parseBlock()
                    declType = FUNC
                }

                at(IDENTIFIER) && lookahead(1) == LPAR -> {
                    // 主构造函数
                    parsePrimaryInitFunc()
                    declType = PRIMARY_CONSTRUCTOR
                }

                at(TILDE) && lookahead(1) == INIT_KEYWORD -> {
                    advance() // TILDE ~
                    parseInitFunc()
                    // 析构函数
                    declType = END_SECONDARY_CONSTRUCTOR
                }
            }
        }

        return declType
    }

    /**
     * 解析主构造函数
     *
     * Grammar:
     * ```
     * primaryConstructor
     *   : simpleName "(" parameterList? ")" constructorBody?
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parsePrimaryInitFunc() {
        assert(_at(IDENTIFIER))
        advance() // IDENTIFIER

        if (at(RBRACE)) {
            error(CangJieParsingBundle.message("parsing.error.function.body.expected"))  // 应该为函数体
            return
        }

        builder.disableJoiningComplexTokens()
        // 类型参数
        if (at(LPAR)) {
            parsePrimaryInitFuncValueParameterList()
        } else {
            // error("Expecting '(' ")  // 应该为'('
            errorAndAdvance(
                CangJieParsingBundle.message(
                    "parsing.error.expecting.left.parenthesis.but.available", builder.tokenText ?: "<unknown>"
                )
            )
        }

        parseInitFunctionBody()
    }

    /**
     * 解析构造函数
     *
     * Grammar:
     * ```
     * constructor
     *   : "init" "(" parameterList? ")" constructorBody?
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseInitFunc() {
        assert(_at(INIT_KEYWORD))
        advance() // INIT_KEYWORD

        if (at(RBRACE)) {
            error(CangJieParsingBundle.message("parsing.error.function.body.expected"))  // 应该为函数体
            return
        }

        builder.disableJoiningComplexTokens()
        // 类型参数
        if (at(LPAR)) {
            parseValueParameterList()
        } else {
            // error("Expecting '(' ")  // 应该为'('
            errorAndAdvance(
                CangJieParsingBundle.message(
                    "parsing.error.expecting.left.parenthesis.but.available", builder.tokenText ?: "<unknown>"
                )
            )
        }

        if (isDeclarationsFile) {
            if (at(LBRACE)) {
                val body = mark()
                while (!atSet(KEYWORDALL) && !eof()) {
                    advance()
                }
                // parseFunctionBody()
                body.error(
                    CangJieParsingBundle.message(
                        "parsing.error.not.allowed.context", "Method bodies", "declaration files"
                    )
                )
            }
            return
        }

        parseInitFunctionBody()
    }


    /**
     * 解析成员列表
     *
     * Grammar:
     * ```
     * members
     *   : memberDeclaration*
     *   ;
     * ```
     *
     * @param tokenId 令牌 ID
     * @param detector 修饰符检测器
     */
    context(parseContext: ParsingContext)
    private fun parseMembers(
        tokenId: Int?,
        detector: ModifierDetector?
    ) {
        while (!eof() && !at(RBRACE)) {
            parseMemberDeclaration(tokenId, detector ?: ModifierDetector())
        }
    }


    /**
     * 解析类体
     *
     * Grammar:
     * ```
     * classBody
     *   : "{" memberDeclaration* "}"
     *   ;
     * ```
     *
     * @param tokenId 令牌 ID
     * @param detector 修饰符检测器
     */
    context(parseContext: ParsingContext)
    private fun parseClassBody(
        tokenId: Int,
        detector: ModifierDetector
    ) {
        val body = mark()

        builder.enableNewlines()

        if (expect(LBRACE, "Expecting a class body")) {
            parseMembers(tokenId, detector)
            expect(RBRACE, "Missing '}'")
        }

        builder.restoreNewlinesState()

        body.done(
            when (tokenId) {
                INTERFACE_KEYWORD_Id -> INTERFACE_BODY

                ENUM_KEYWORD_Id -> ENUM_BODY
                else -> CLASS_BODY
            }
        )
    }


    /**
     * 解析主函数
     *
     * Grammar:
     * ```
     * mainFunction
     *   : "main" "(" parameterList? ")" (":" typeReference)? functionBody
     *   ;
     * ```
     *
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    private fun parseMainFunc(): IElementType {
        assert(_at(MAIN_KEYWORD))
        advance()

        if (at(RBRACE)) {
            error(CangJieParsingBundle.message("parsing.error.function.body.expected"))  // 应该为函数体
            return MAIN_FUNC
        }

        builder.disableJoiningComplexTokens()
        // 类型参数
        if (at(LPAR)) {
            parseValueParameterList(false, VALUE_PARAMETERS_FOLLOW_SET)
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "("))
        }

        // 返回值类型
        if (at(COLON)) {
            advance() // COLON
            parseTypeRef()
        }

        // 函数体
        if (at(LBRACE)) {
            parseFunctionBody()
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))  // 应该为'{'
        }
        return MAIN_FUNC
    }

    /**
     * 解析函数声明
     *
     * Grammar:
     * ```
     * functionDeclaration
     *   : modifier* "func" (simpleName | operatorName) typeParameters?
     *     "(" parameterList? ")" (":" typeReference)? typeConstraints? functionBody?
     *   ;
     * ```
     *
     * @param isInterfaceMethod 是否为接口方法
     * @param classdetector 类修饰符检测器
     * @param detector 修饰符检测器
     * @param isForeign 是否为外部函数
     * @param topTokenId 顶层token ID
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    fun parseFunction(
        isInterfaceMethod: Boolean = false,
        classdetector: ModifierDetector? = null,
        detector: ModifierDetector? = null,
        isForeign: Boolean = false,
        topTokenId: Int? = 0
    ): IElementType {
        assert(_at(FUNC_KEYWORD))
        advance()

        var type: IElementType = FUNC


        if (at(RBRACE)) {
            error(CangJieParsingBundle.message("parsing.error.function.body.expected")) // 应该为函数体
            return type
        }

        builder.disableJoiningComplexTokens()

        if (detector?.isOperatorDetected == true) {
            // 运算符重载
            val operatorToken = getOperationTokenType()

            if (OPERATIONS_CAN_BE_OVERLOADED.contains(operatorToken)) {
                val operator = mark()
                advanceOperationToken(operatorToken)
                operator.done(OPERATION_NAME)
            } else {
                val mark = mark()
                advance()
                mark.error(
                    CangJieParsingBundle.message(
                        "parsing.error.should.be", "operator", "an overloaded operator"
                    )
                )
            }
        } else {
            // 函数名
            builder.tokenType
            parseIdentifier()
        }

        var typeParameterListOccurred = false
        if (at(LT)) {
            parseTypeParameterList(LBRACKET_LBRACE_RBRACE_LPAR_SET)
            typeParameterListOccurred = true
        }

        // 参数列表
        if (at(LPAR)) {
            parseValueParameterList(false, VALUE_PARAMETERS_FOLLOW_SET)
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "(")) // 应该为'('
        }

        // 返回值类型
        if (at(COLON)) {
            advance() // COLON
            parseTypeRef()
        }

        parseTypeConstraintsGuarded(typeParameterListOccurred)

        // 函数体
        if (isDeclarationsFile) {
            if (at(LBRACE)) {
                val body = mark()
                while (!atSet(KEYWORDALL) && !eof()) {
                    advance()
                }
                body.error(
                    CangJieParsingBundle.message(
                        "parsing.error.not.allowed.context", "Method bodies", "declaration files"
                    )
                )
            }
            return type
        }

        if (at(LBRACE)) {
            parseFunctionBody()
            if (isForeign) {
                error(CangJieParsingBundle.message("parsing.error.foreign.function.no.body"))
            }
        } else if (!(isInterfaceMethod || (classdetector?.isAbstractDetected == true)) && (detector?.isForeignDetected != true)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{")) // 应该为'{'
        }

        return type
    }

    /**
     * 解析外部函数声明
     *
     * Grammar:
     * ```
     * foreignFunction
     *   : modifier* "func" simpleName typeParameters? "(" parameterList? ")" (":" typeReference)? typeConstraints?
     *   ;
     * ```
     *
     * @param detector 修饰符检测器
     * @param topTokenId 顶层token ID
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    fun parseForeignFunction(
        detector: ModifierDetector, topTokenId: Int = 0
    ): IElementType {
        return parseFunction(
            isInterfaceMethod = false,
            classdetector = null,
            detector = detector,
            isForeign = true,
            topTokenId = topTokenId
        )
    }


    /**
     * 解析标识符
     *
     */
    context(parseContext: ParsingContext)
    private fun parseIdentifier(
        nameParsingMode: NameParsingMode = NameParsingMode.REQUIRED, recoverySet: TokenSet = TokenSet.EMPTY
    ) {

        if (atSet(KEYWORDS)) {
            error(CangJieParsingBundle.message("parsing.error.cannot.be.used", "Keywords")) // 关键字不能使用
            return
        }

        if (nameParsingMode == NameParsingMode.REQUIRED) {
            expect(
                IDENTIFIER, CangJieParsingBundle.message("parsing.error.expecting.cangjie.identifier"), recoverySet
            ) // 应该为标识符
        } else {
            if (at(IDENTIFIER)) {
                if (nameParsingMode == NameParsingMode.PROHIBITED) {
// TODO 禁止出现标识符，目前应该用不到

                } else {
                    assert(nameParsingMode == NameParsingMode.ALLOWED)
                    advance()
                }
            }
        }
    }


    /**
     * 按标题解析标识符
     *
     * @param title 标题名称，用于错误消息
     * @param recoverySet 恢复伤口的令牌集合
     * @param isUnderline 是否允许下划线
     */
    context(parseContext: ParsingContext)
    private fun parseIdentifierByTitle(
        title: String, recoverySet: TokenSet = TokenSet.EMPTY, isUnderline: Boolean = false
    ) {
        if (isUnderline && expect(CangJieExpressionParsing.IDENTIFIER_RECOVERY_SET)) {
            return
        }

        if (expect(IDENTIFIER)) {
            return
        }

        errorWithRecovery("Expecting $title name", recoverySet)
    }

    /**
     * 解析同步表达式
     *
     * Grammar:
     * ```
     * synchronizedExpression
     *   : "synchronized" "(" expression ")" block
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseSynchronizedExpression() {
        assert(_at(SYNCHRONIZED_KEYWORD))
        val synchronizedMarker = mark()
        advance()

        if (at(LPAR)) {
            advance()

            expressionParsing.parseExpression()

            expect(RPAR, "Expecting ')'")
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "("))
        }

        if (at(LBRACE)) {
            parseBlock()
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{")) // 应该为'{'
        }

        synchronizedMarker.done(SYNCHRONIZED_EXPRESSION)
    }


    /**
     * 解析外部声明块
     *
     * Grammar:
     * ```
     * foreignDeclaration
     *   : "foreign" "{" foreignFunction* "}"
     *   ;
     * ```
     *
     * @return 解析结果的节点类型
     */
    context(parseContext: ParsingContext)
    private fun parseForeign(): IElementType {
        assert(_at(FOREIGN_KEYWORD))
        advance()

        if (at(LBRACE)) {
            parseForeignBody()
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))
        }

        return FOREIGN
    }


    /**
     * 解析外部声明块体
     *
     * Grammar:
     * ```
     * foreignBody
     *   : "{" foreignFunction* "}"
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    private fun parseForeignBody() {
        assert(_at(LBRACE))
        val mark = mark()
        advance() // LBRACE

        while (!at(RBRACE) && !eof()) {
            val detector = ModifierDetector()

            parseModifierList(detector, TokenSet.EMPTY)

            if (at(FUNC_KEYWORD)) {
                detector(FOREIGN_KEYWORD)
                parseForeignFunction(detector)
            } else {
                errorWithRecovery("Expecting function declaration", TokenSet.create(FUNC_KEYWORD))
            }
        }

        expect(RBRACE, "Missing '}'")
        mark.done(FOREIGN_BODY)
    }

    /**
     * 解析宏声明
     *
     * Grammar:
     * ```
     * macroDeclaration
     *   : "macro" simpleName typeParameters? "(" parameterList? ")" (":" typeReference)?
     *     typeConstraints? functionBody?
     *   ;
     * ```
     *
     * @return 解析结果的节点类型
     */

    context(parseContext: ParsingContext)
    fun parseMacro(): IElementType {
        assert(_at(MACRO_KEYWORD))
        advance()

        if (at(RBRACE)) {
            error(CangJieParsingBundle.message("parsing.error.function.body.expected")) // 应该为函数体
            return MACRO
        }

        builder.disableJoiningComplexTokens()

        // 函数名
        parseIdentifier()
        // 类型参数
        var typeParameterListOccurred = false
        if (at(LT)) {
            parseTypeParameterList(LBRACKET_LBRACE_RBRACE_LPAR_SET)
            typeParameterListOccurred = true
        }


        if (at(LPAR)) {
            parseValueParameterList(false, VALUE_PARAMETERS_FOLLOW_SET)
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "(")) // 应该为'('
        }

        // 返回值类型
        if (at(COLON)) {
            advance() // COLON
            parseTypeRef()
        }

        parseTypeConstraintsGuarded(typeParameterListOccurred)

        if (isDeclarationsFile) {
            if (at(LBRACE)) {
                val body = mark()
                while (!atSet(KEYWORDALL) && !eof()) {
                    advance()
                }
                body.error(
                    CangJieParsingBundle.message(
                        "parsing.error.not.allowed.context", "Method bodies", "declaration files"
                    )
                )
            }
            return MACRO
        }

        // 函数体
        if (at(LBRACE)) {
            parseFunctionBody()
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{")) // 应该为'{'
        }
        return MACRO
    }

    /**
     * init 函数体恢复
     */
    context(parseContext: ParsingContext)
    fun parseInitFunctionBody() {
        if (at(COLON)) {
            val error = mark()
            while (!at(LBRACE)) advance()
            error.error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))
        }

        if (isDeclarationsFile) {
            if (at(LBRACE)) {
                val body = mark()
                while (!atSet(KEYWORDALL) && !eof()) {
                    advance()
                }
                body.error(
                    CangJieParsingBundle.message(
                        "parsing.error.not.allowed.context", "Method bodies", "declaration files"
                    )
                )
            }
            return
        }

        if (at(LBRACE)) {
            parseInitFunctionBlock()
        } else {
            error(CangJieParsingBundle.message("parsing.error.function.body.expected")) // 应该为函数体
        }
    }

    /**
     * 函数体解析，只支持块
     */
    context(parseContext: ParsingContext)
    fun parseFunctionBody() {
        if (at(LBRACE)) {
            parseBlock()
        } else {
            error(CangJieParsingBundle.message("parsing.error.function.body.expected")) // 应该为函数体
        }
    }

    /**
     * 尝试解析函数参数，类型是否必须由 typeRequired 决定
     */
    context(parseContext: ParsingContext)
    fun tryParseValueParameter(typeRequired: Boolean): Boolean {
        return parseValueParameter(true, typeRequired)
    }

    /**
     * 解析主构造函数的值参数列表
     *
     * 专门用于解析主构造函数的参数列表，支持注解、修饰符和属性声明（let/var）。
     * 这是一个便捷方法，内部调用 `parseValueParameterList` 并传入 `isPrimaryInitFunc=true`。
     *
     * Grammar:
     * ```
     * primaryConstructorParameterList
     *   : "(" (primaryConstructorParameter ("," primaryConstructorParameter)*)? ")"
     *   ;
     *
     * primaryConstructorParameter
     *   : annotations? modifiers? ("let" | "var") IDENTIFIER ":" type ("=" expression)?
     *   ;
     * ```
     *
     * 特点：
     * - 参数可以有注解（如 `@Serializable`, `@Deprecated`）
     * - 参数可以有访问修饰符（如 `public`, `private`, `protected`）
     * - 参数可以声明为属性（使用 `let` 或 `var`）
     * - 参数不能使用宏调用
     * - 所有参数必须有名称和类型
     *
     * 示例：
     * ```cangjie
     * class Person(
     *     // 公开的不可变属性
     *     public let name: String,
     *
     *     // 私有的可变属性，带默认值
     *     private var age: Int = 0,
     *
     *     // 带注解的属性
     *     @Deprecated("Use fullAddress instead")
     *     let address: String
     * )
     * ```
     *
     * @see parseValueParameterList 底层参数列表解析方法
     * @see parseValueParameter 单个参数解析方法
     */
    context(parseContext: ParsingContext)
    fun parsePrimaryInitFuncValueParameterList() {
        parseValueParameterList(false, VALUE_PARAMETERS_FOLLOW_SET, true)
    }

    /**
     * 解析值参数列表
     *
     * 解析函数或函数类型的参数列表，包括括号内的所有参数声明。
     * 支持两种模式：
     * 1. 函数类型内容模式（isFunctionTypeContents = true）：解析函数类型的参数列表，参数可以省略名称
     * 2. 普通函数模式（isFunctionTypeContents = false）：解析函数定义的参数列表，参数必须有名称
     *
     * Grammar:
     * ```
     * valueParameterList
     *   : "(" (valueParameter ("," valueParameter)*)? ")"
     *   ;
     *
     * valueParameter
     *   : annotations? modifiers? (("let" | "var"))? IDENTIFIER ":" type ("=" expression)?  // 普通函数参数
     *   | type                                                                               // 函数类型参数（可省略名称）
     *   ;
     * ```
     *
     * @param isFunctionTypeContents 是否为函数类型内容。
     *        - true: 解析函数类型的参数列表，例如 `(Int, String) -> Unit` 中的 `(Int, String)`
     *        - false: 解析函数定义的参数列表，例如 `func foo(x: Int, y: String)` 中的 `(x: Int, y: String)`
     * @param typeRequired 参数是否必须有类型注解。
     *        - true: 参数必须显式声明类型
     *        - false: 参数类型可以省略（根据上下文推断）
     * @param recoverySet 用于错误恢复的token集合。当遇到语法错误时，解析器会跳过token直到遇到此集合中的token
     * @param isPrimaryInitFunc 是否为主构造函数的参数列表。
     *        - true: 解析主构造函数参数，支持 `let`/`var` 关键字和注解
     *        - false: 解析普通函数参数
     *
     * 解析流程：
     * 1. 消费左括号 `(`
     * 2. 循环解析参数，参数之间用逗号分隔
     * 3. 对于函数类型内容模式，参数可以是简单的类型引用或完整的参数声明
     * 4. 对于普通函数模式，参数必须包含名称和类型
     * 5. 消费右括号 `)`
     * 6. 验证参数列表的一致性（不允许混合命名参数和匿名参数）
     *
     * 错误处理：
     * - 遇到连续的逗号会报错
     * - 右括号前的逗号会报错
     * - 缺少逗号或右括号会报错
     * - 混合使用命名参数和匿名参数会报错
     *
     * 示例：
     * ```cangjie
     * // 普通函数参数列表
     * func foo(x: Int, y: String = "default")
     *
     * // 主构造函数参数列表
     * class Person(public let name: String, var age: Int)
     *
     * // 函数类型参数列表
     * let f: (Int, String) -> Unit
     * ```
     */
    /**
     * 解析函数定义的值参数列表
     *
     * 用于解析普通函数或主构造函数的参数列表，所有参数必须有名称。
     *
     * Grammar:
     * ```
     * valueParameterList
     *   : "(" (valueParameter ("," valueParameter)*)? ")"
     *   ;
     *
     * valueParameter
     *   : annotations? modifiers? (("let" | "var"))? IDENTIFIER ":" type ("=" expression)?
     *   ;
     * ```
     *
     * @param typeRequired 参数是否必须有类型注解
     * @param recoverySet 用于错误恢复的token集合
     * @param isPrimaryInitFunc 是否为主构造函数的参数列表
     *
     * 示例：
     * ```cangjie
     * // 普通函数参数列表
     * func foo(x: Int, y: String = "default")
     *
     * // 主构造函数参数列表
     * class Person(public let name: String, var age: Int)
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseValueParameterList(
        typeRequired: Boolean = false,
        recoverySet: TokenSet = TokenSet.EMPTY,
        isPrimaryInitFunc: Boolean = false
    ) {
        parseParameterListCore(
            parseParameter = { parseValueParameter(false, typeRequired, isPrimaryInitFunc); null },
            checkMixedParameters = false,
            recoverySet = recoverySet
        )
    }

    /**
     * 解析函数类型的参数列表
     *
     * 用于解析函数类型声明中的参数列表，参���可以省略名称（仅类型）。
     * 不允许混合使用命名参数和匿名参数。
     *
     * Grammar:
     * ```
     * functionTypeParameterList
     *   : "(" (functionTypeParameter ("," functionTypeParameter)*)? ")"
     *   ;
     *
     * functionTypeParameter
     *   : IDENTIFIER ":" type    // 命名参数
     *   | type                   // 匿名参数
     *   ;
     * ```
     *
     * @param typeRequired 参数是否必须有类型注解
     *
     * 示例：
     * ```cangjie
     * // 只有类型（匿名参数）
     * let callback: (Int, String) -> Unit
     *
     * // 带名称（命名参数）
     * let handler: (count: Int, message: String) -> Bool
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseFunctionTypeParameterList(
        typeRequired: Boolean = true
    ) {
//   不允许处理注解和宏
        with(parseContext.copy(disableMacroParsing = true, allowParseAnnotationsInValueParameter = false)) {
            parseParameterListCore(
                parseParameter = { parseFunctionTypeParameter(typeRequired) },
                checkMixedParameters = true,
                recoverySet = TokenSet.EMPTY
            )
        }
    }

    /**
     * 参数列表解析的核心逻辑
     *
     * 提取公共的参数列表解析逻辑，避免代码重复。
     *
     * @param parseParameter 参数解析函数，返回 null（不检查混合）或 Boolean（是否为命名参数）
     * @param checkMixedParameters 是否检查混合命名/匿名参数
     * @param recoverySet 错误恢复token集合
     */
    context(parseContext: ParsingContext)
    private fun parseParameterListCore(
        parseParameter: () -> Boolean?,
        checkMixedParameters: Boolean,
        recoverySet: TokenSet
    ) {
        assert(at(LPAR))
        val parameters = mark()

        builder.disableNewlines()
        advance() // consume '('

        // 只在需要时追踪命名参数状态
        var hasNamedParameters: Boolean? = null
        var hasMixedParameters = false

        while (!at(RPAR) && !atSet(recoverySet) && !eof()) {
            // 处理连续逗号错误
            if (at(COMMA)) {
                errorAndAdvance(
                    CangJieParsingBundle.message("parsing.error.expecting", "parameter declaration")
                )
                continue
            }

            // 解析参数
            val isNamed = parseParameter()

            // 检测混合参数（仅当需要时）
            if (checkMixedParameters && isNamed != null && !hasMixedParameters) {
                when (hasNamedParameters) {
                    null -> hasNamedParameters = isNamed
                    isNamed -> { /* 一致，继续 */
                    }

                    else -> hasMixedParameters = true  // 检测到混合
                }
            }

            // 处理分隔符
            if (!parseParameterSeparator()) {
                break
            }
        }

        expect(RPAR, "Expecting ')'", recoverySet)
        builder.restoreNewlinesState()

        // 完成或报错
        if (hasMixedParameters) {
            parameters.error(CangJieParsingBundle.message("parsing.error.mixed.named.parameters"))
        } else {
            parameters.done(VALUE_PARAMETER_LIST)
        }
    }
//    context(parseContext: ParsingContext) fun parseValueParameterList(
//        isFunctionTypeContents: Boolean,
//        typeRequired: Boolean,
//        recoverySet: TokenSet = TokenSet.EMPTY,
//        isPrimaryInitFunc: Boolean = false
//    ) {
//        assert(at(LPAR))
//        val parameters = mark()
//
//
//        builder.disableNewlines()
//        advance() // consume '('
//
//        val isNamedParameters = mutableListOf<Boolean>()
//
//        while (!at(RPAR) && !atSet(recoverySet) && !eof()) {
//            if (at(COMMA)) {
//                errorAndAdvance(
//                    CangJieParsingBundle.message(
//                        "parsing.error.expecting", "parameter declaration"
//                    )
//                ) // 应该为参数声明
//            }
//
//            if (isFunctionTypeContents) {
//                if (!tryParseValueParameter(typeRequired)) {
//                    val valueParameter = mark()
//                    // 你原来注释掉的parseFunctionTypeValueParameterModifierList()可根据需求添加
//                    parseTypeRef()
//                    closeDeclarationWithCommentBinders(valueParameter, VALUE_PARAMETER, false)
//                    isNamedParameters.add(false)
//                } else {
//                    isNamedParameters.add(true)
//                }
//            } else {
//                parseValueParameter(false, typeRequired, isPrimaryInitFunc)
//            }
//
//            if (at(COMMA)) {
//                advance() // consume ','
//
//                if (at(RPAR)) {
//                    error(CangJieParsingBundle.message("parsing.error.expecting", "a parameter declaration")) // 应该为参数声明
//                }
//            } else {
//                if (!at(RPAR)) {
//                    errorAndAdvance(
//                        CangJieParsingBundle.message(
//                            "parsing.error.expecting.found", "',' or ')'", "${builder.tokenText}"
//                        )
//                    )
//                }
//            }
//        }
//
//        expect(RPAR, "Expecting ')'", recoverySet)
//        builder.restoreNewlinesState()
//
//        if (isNamedParameters.contains(true) && isNamedParameters.contains(false)) {
//            // 要么全为 true,要么全为 false,混合是不允许的
//            parameters.error(CangJieParsingBundle.message("parsing.error.mixed.named.parameters"))
//        } else {
//            parameters.done(VALUE_PARAMETER_LIST)
//        }
//    }

    /**
     * 解析函数类型参数
     *
     * @param typeRequired 是否要求类型注解
     * @return true 如果是命名参数，false 如果是匿名参数（仅类型）
     */
    context(parseContext: ParsingContext)
    private fun parseFunctionTypeParameter(typeRequired: Boolean): Boolean {
        return if (!tryParseValueParameter(typeRequired)) {
            // 匿名参数：只有类型
            val valueParameter = mark()
            parseTypeRef()
            closeDeclarationWithCommentBinders(valueParameter, VALUE_PARAMETER, false)
            false
        } else {
            // 命名参数
            true
        }
    }

    /**
     * 解析参数分隔符（逗号）
     *
     * @return true 如果应该继续解析下一个参数，false 如果应该结束
     */
    context(parseContext: ParsingContext)
    private fun parseParameterSeparator(): Boolean {
        if (at(COMMA)) {
            advance() // consume ','

            if (at(RPAR)) {
                error(CangJieParsingBundle.message("parsing.error.expecting", "a parameter declaration"))
                return false
            }
            return true
        } else if (!at(RPAR)) {
            errorAndAdvance(
                CangJieParsingBundle.message(
                    "parsing.error.expecting.found", "',' or ')'", "${builder.tokenText}"
                )
            )
            return false
        }
        return false
    }


    /**
     * 解析单个值参数
     *
     * 解析函数或构造函数的单个参数声明，包括注解、修饰符、参数名、类型和默认值。
     * 根据上下文不同，支持普通函数参数和主构造函数参数两种模式。
     *
     * Grammar:
     * ```
     * valueParameter
     *   : annotations? modifiers? (("let" | "var"))? IDENTIFIER ("!" | ":")? type? ("=" expression)?
     *   ;
     * ```
     *
     * @param rollbackOnFailure 解析失败时是否回滚。
     *        - true: 如果解析失败，回滚到参数开始位置并返回 false（用于尝试性解析）
     *        - false: 即使解析失败也完成参数节点并返回 true（默认行为）
     * @param typeRequired 是否必须有类型注解。
     *        - true: 参数必须显式声明类型，否则报错
     *        - false: 参数类型可以省略（根据上下文推断）
     * @param isPrimaryInitFunc 是否为主构造函数参数。
     *        - true: 解析主构造函数参数，支持：
     *            - 注解（如 `@Serializable`）
     *            - 访问修饰符（如 `public`, `private`）
     *            - 属性关键字（`let` 或 `var`）
     *            - 禁止宏调用
     *        - false: 解析普通函数参数
     * @return 是否成功解析参数
     *        - true: 解析成功或虽有错误但不回滚
     *        - false: 仅当 rollbackOnFailure=true 且解析失败时返回
     *
     * 解析流程：
     * 1. 如果是主构造函数参数（isPrimaryInitFunc=true）：
     *    - 解析注解（如 `@Deprecated`）
     *    - 解析修饰符（如 `public`, `private`, `protected`）
     *    - 期望 `let` 或 `var` 关键字（如果有修饰符）
     * 2. 解析参数的剩余部��：
     *    - 参数名
     *    - 类型注解（可选，取决于 typeRequired）
     *    - 默认值（可选）
     * 3. 完成参数节点或回滚
     *
     * 示例：
     * ```cangjie
     * // 普通函数参数（isPrimaryInitFunc=false）
     * func foo(x: Int, y: String = "default")
     *         ^^^^^^  ^^^^^^^^^^^^^^^^^^^^
     *         参数1    参数2（带默认值）
     *
     * // 主构造函数参数（isPrimaryInitFunc=true）
     * class Person(
     *     @Serializable public let name: String,
     *     ^^^^^^^^^^^^^ ^^^^^^ ^^^ ^^^^^^^^^^^^
     *     注解          修饰符  let  参数声明
     *
     *     private var age: Int = 0
     *     ^^^^^^^ ^^^ ^^^^^^^^^^^
     *     修饰符   var  参数声明（带默认值）
     * )
     *
     * // 可选类型参数（typeRequired=false）
     * func infer(x) { ... }  // 类型由上下文推断
     *
     * // 必需类型参数（typeRequired=true）
     * func explicit(x: Int) { ... }  // 必须显式声明类型
     * ```
     *
     * 错误处理：
     * - 缺少参数名：报错但继续解析
     * - 主构造函数中缺少 let/var：报错
     * - 类型声明错误：根据 rollbackOnFailure 决定是否回滚
     */
    context(parseContext: ParsingContext)
    fun parseValueParameter(
        rollbackOnFailure: Boolean = false, typeRequired: Boolean = false, isPrimaryInitFunc: Boolean = false
    ): Boolean {
        val parameter = mark()
        if (!parseContext.disableMacroParsing && at(AT)) {
            expressionParsing.parseMacroExpression()
            return true
        }
        //            可以注解
        if (parseContext.allowParseAnnotationsInValueParameter) {
            parseAnnotations()
        }
        if (isPrimaryInitFunc) {
//            主构造方法不可以宏调用

            val detector = ModifierDetector()
            parseModifierList(detector, TokenSet.EMPTY, true)

            when {
                at(LET_KEYWORD) || at(VAR_KEYWORD) -> advance()
                detector.count > 0 -> error(CangJieParsingBundle.message("parsing.error.missing.variable.declaration.symbol"))
            }
        }

        if (!parseFunctionParameterRest(typeRequired) && rollbackOnFailure) {
            parameter.rollbackTo()
            return false
        }

        closeDeclarationWithCommentBinders(parameter, VALUE_PARAMETER, false)

        return true
    }


    /**
     * 解析函数参数的剩余部分
     *
     * 处理参数名称、类型注解和默认值。
     *
     * @param typeRequired 是否必须有类型注解
     * @return 是否成功解析
     */

    context(parseContext: ParsingContext)
    private fun parseFunctionParameterRest(
        typeRequired: Boolean
    ): Boolean {
        var noErrors = true
        var isDefault = false

        // 恢复 'func foo(Array<String>) {}' 和 'func foo(: Int) {}' 这种情况
        if ((at(IDENTIFIER) && lookahead(1) == LT) || at(COLON)) {
            error(CangJieParsingBundle.message("parsing.error.missing.parameter.name")) // 缺少参数名称
            if (at(COLON)) {
                // 保留 noErrors == true 以避免函数类型解析中回滚
                advance() // :
            } else {
                noErrors = false
            }
            parseTypeRef()
        } else {
            expect(
                CangJieExpressionParsing.IDENTIFIER_RECOVERY_SET, "Missing parameter name", PARAMETER_NAME_RECOVERY_SET
            )

            if (expect(EXCL)) {
                // 可以有默认值
                isDefault = true
            }

            if (at(COLON)) {
                advance() // :

                if (at(IDENTIFIER) && lookahead(1) == COLON) {
                    // 恢复 "func foo(x: y: Int)" 中错误的类型引用
                    error(CangJieParsingBundle.message("parsing.error.type.reference.expected"))
                    return false
                }

                parseTypeRef()
            } else if (typeRequired) {
                errorWithRecovery(
                    CangJieParsingBundle.message("parsing.error.parameters.must.have.type.annotation"),
                    PARAMETER_NAME_RECOVERY_SET
                )
                noErrors = false
            } else {
                errorWithoutAdvancing(
                    CangJieParsingBundle.message(
                        "parsing.error.expecting.found", "':'", "Missing type declaration"
                    )
                ) // 应该为 ':'
                noErrors = false
            }
        }

        if (at(EQ)) {
            if (isDefault) {
                advance()
            } else {
                error(CangJieParsingBundle.message("parsing.error.default.value.non.named.parameters"))
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.found", "',' or ')'", "='"))
                noErrors = false
            }


            expressionParsing.parseExpression()

        }

        return noErrors
    }


    /**
     * 在平台类型上恢复括号包围的单词
     *
     * 用于处理平台类型的特殊格式。
     *
     * @param offset 偏移量
     * @param word 期望的单词
     * @param consume 是否消费token
     * @return 是否成功处理
     */

    context(parseContext: ParsingContext)
    private fun recoverOnParenthesizedWordForPlatformTypes(
        offset: Int, word: String, consume: Boolean
    ): Boolean {
        // 形如 Array<(out) Foo>! 或 (Mutable)List<Bar>! 的恢复
        if (lookahead(offset) == LPAR && lookahead(offset + 1) == IDENTIFIER && lookahead(offset + 2) == RPAR && lookahead(
                offset + 3
            ) == IDENTIFIER
        ) {
            val error = mark()

            advance(offset)
            advance() // LPAR

            if (word != builder.tokenText) {
                // 不是预期的 "out" 或 "Mutable"
                error.rollbackTo()
                return false
            } else {
                advance() // IDENTIFIER ('out')
                advance() // RPAR

                if (consume) {
                    error.error(CangJieParsingBundle.message("parsing.error.unexpected", "tokens"))
                } else {
                    error.rollbackTo()
                }

                return true
            }
        }
        return false
    }


    /**
     * 解析类型参数列表
     *
     * Grammar:
     * ```
     * typeArgumentList
     *   : "<" typeArgument ("," typeArgument)* ">"
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    context(parseContext: ParsingContext)
    private fun parseTypeArgumentList(): Boolean {
        if (!at(LT)) return false

        val list = mark()
        tryParseTypeArgumentList(TokenSet.EMPTY)
        list.done(TYPE_ARGUMENT_LIST)

        return true
    }


    /**
     * 在平台类型后缀上恢复
     *
     * 处理平台类型的感叹号后缀。
     */
    context(parseContext: ParsingContext)
    private fun recoverOnPlatformTypeSuffix() {
        // 平台类型的恢复，遇到感叹号报错
        if (at(EXCL)) {
            val error = mark()
            advance() // EXCL
            error.error(CangJieParsingBundle.message("parsing.error.unexpected", "token"))
        }
    }


    /**
     * 解析函数类型
     *
     * Grammar:
     * ```
     * functionType
     *   : "(" parameterList? ")" "->" typeReference
     *   ;
     * ```
     *
     * @param functionType 函数类型标记
     */
    context(parseContext: ParsingContext)
    private fun parseFunctionType(functionType: PsiBuilder.Marker) {
        parseFunctionTypeContents(functionType).done(FUNCTION_TYPE)
    }


    /**
     * 解析函数类型内容
     *
     * @param functionType 函数类型标记
     * @return 处理后的标记
     */
    context(parseContext: ParsingContext)

    private fun parseFunctionTypeContents(functionType: PsiBuilder.Marker): PsiBuilder.Marker {
        assert(_at(LPAR)) { tt()!! }

        parseFunctionTypeParameterList(typeRequired = true)

        expect(ARROW, "Expecting '->' to specify return type of a function type", TYPE_REF_FIRST)
        parseTypeRef()

        return functionType
    }


    /**
     * 解析元组类型
     *
     * Grammar:
     * ```
     * tupleType
     *   : "(" typeReference ("," typeReference)* ")"
     *   ;
     * ```
     *
     * @return 元组中类型的数量
     */
    context(parseContext: ParsingContext)
    private fun parseTupleType(): Int {
        assert(_at(LPAR))
        var count = 0

        advance() // LPAR

        if (!at(RPAR)) {
            while (true) {
                parseTypeRef()
                count++
                if (!at(COMMA)) break
                advance() // COMMA
            }
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting", "type"))
        }

        expect(RPAR, "Expecting ')'")

        return count
    }


    /**
     * 解析 VArray 类型
     *
     * Grammar:
     * ```
     * varrayType
     *   : "VArray" "<" typeReference "," "$" integerLiteral ">"
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    context(parseContext: ParsingContext)

    private fun parseVArrayType(): Boolean {
        if (at(VARRAY_KEYWORD)) {
            val typeRefMarker = mark()
            advance()

            if (at(LT)) {
                advance()
                val list = mark()
                val projection = mark()

                parseTypeRef(TokenSet.EMPTY)

                projection.done(TYPE_PROJECTION)
                list.done(TYPE_ARGUMENT_LIST)

                expect(COMMA, "Should be ','")
                expect(DOLLAR, "Should be '$'")

                expect(INTEGER_LITERAL, "Should be integer literal")

                expect(GT, "Should be '>'")

            } else {
                error(CangJieParsingBundle.message("parsing.error.expecting", "type parameters after 'VArray' keyword"))
            }

            typeRefMarker.done(VARRAY_TYPE)
            return true
        }

        return false
    }

    /**
     * 解析用户类型
     *
     * Grammar:
     * ```
     * userType
     *   : simpleUserType ("." simpleUserType)*
     *   ;
     *
     * simpleUserType
     *   : simpleName typeArguments?
     *   ;
     * ```
     *
     * @return 是否包含类型参数列表
     */
    context(parseContext: ParsingContext)
    fun parseUserType(): Boolean {
        var isTypeArgumentList = false

        var userType = mark()

        var reference = mark()

        while (true) {
            recoverOnParenthesizedWordForPlatformTypes(0, "Mutable", consume = true)
            if (expect(
                    IDENTIFIER, "Expecting type name", TokenSet.orSet(
                        CangJieExpressionParsing.EXPRESSION_FIRST,
                        CangJieExpressionParsing.EXPRESSION_FOLLOW,
                        DECLARATION_FIRST
                    )
                )
            ) {
                reference.done(REFERENCE_EXPRESSION)
            } else {
                reference.drop()
                break
            }

            isTypeArgumentList = parseTypeArgumentList()

//      recoverOnPlatformTypeSuffix()  // 如果需要恢复平台类型后缀，可以取消注释

            if (!at(DOT)) {
                break
            }

            val precede = userType.precede()
            userType.done(USER_TYPE)
            userType = precede

            advance() // DOT
            reference = mark()
        }

        userType.done(USER_TYPE)
        return isTypeArgumentList
    }


    /**
     * 解析 This 类型
     *
     * Grammar:
     * ```
     * thisType
     *   : "This"
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    fun parseThisType(): Boolean {
        if (at(THIS_KEYWORD_UPPER)) {
            val typeRefMarker = mark()
            advance()
            typeRefMarker.done(THIS_TYPE)
            return true
        }
        return false
    }

    /**
     * 解析基本类型
     *
     * Grammar:
     * ```
     * basicType
     *   : "Bool" | "Int8" | "Int16" | "Int32" | "Int64"
     *   | "UInt8" | "UInt16" | "UInt32" | "UInt64"
     *   | "Float32" | "Float64" | "Rune" | "Unit"
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    fun parseBasicType(): Boolean {
        if (atSet(BASICTYPES)) {
            val typeRefMarker = mark()
            advance()
            typeRefMarker.done(BASIC_TYPE)
            return true
        }
        return false
    }

    /**
     * 解析类型参数列表
     */
    context(parseContext: ParsingContext)
    fun tryParseTypeArgumentList(extraRecoverySet: TokenSet): Boolean {
        builder.disableNewlines()
        advance() // LT

        do {
            if (at(COMMA)) {
                advance()
            }

            val projection = mark()

            parseTypeRef(extraRecoverySet)

            projection.done(TYPE_PROJECTION)

            if (at(GT)) break
        } while (at(COMMA))

        val atGT = at(GT)
        if (!atGT) {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", ">"))
        } else {
            advance() // GT
        }
        builder.restoreNewlinesState()
        return atGT
    }


    /**
     * 解析可选类型
     *
     * Grammar:
     * ```
     * optionalType
     *   : "?" typeReference
     *   ;
     * ```
     */
    context(parseContext: ParsingContext)
    fun parseOptionType() {
        assert(_at(QUEST))

        val optionTypeMarker = mark()

        // 注释掉的 SAFE_CALL 处理部分暂时不处理，如果需要我可以帮你恢复
        /*
        if (at(SAFE_CALL)) {
            builder.remapCurrentToken(LPAR)
            parseTupleOrFunctionType()
            optionTypeMarker.done(OPTIONAL_TYPE)
            return
        }
        */

        advance()

        parseTypeRefContents()

        optionTypeMarker.done(OPTIONAL_TYPE)
    }

    /**
     * 解析类型引用内容
     *
     * 根据当前token类型选择合适的类型解析方法。
     *
     * @param extraRecoverySet 额外的恢复伤口token集合
     * @param isConstraint 是否为约束类型
     */
    context(parseContext: ParsingContext)
    fun parseTypeRef(
        extraRecoverySet: TokenSet = TokenSet.EMPTY, isConstraint: Boolean = false
    ) {
        val typeRefMarker = mark()

        if (!isConstraint) {
            parseTypeRefContents()
        } else {
            parseIdentifier()
        }

        typeRefMarker.done(TYPE_REFERENCE)
    }


    /**
     * 解析元组或函数类型
     *
     * 根据上下文决定是解析为元组还是函数类型。
     */
    context(parseContext: ParsingContext)
    private fun parseTupleOrFunctionType() {
        var oType = mark()

        val count = parseTupleType()

        if (at(ARROW) || at(COLON)) {
            oType.rollbackTo()
            oType = mark()
            parseFunctionType(oType)
        } else {
            if (count <= 1) {
                oType.done(PARENTHESIZED_TYPE)
            } else {
                oType.done(TUPLE_TYPE)
            }
        }
    }


    /**
     * 解析类型引用内容的具体实现
     *
     * 根据当前token类型选择合适的类型解析方法。
     */

    context(parseContext: ParsingContext)
    private fun parseTypeRefContents() {
        when {
            parseVArrayType() -> return
            parseThisType() -> return
            parseBasicType() -> return
            at(IDENTIFIER) -> parseUserType()
            at(LPAR) -> parseTupleOrFunctionType()
            at(QUEST) || at(SAFE_CALL) -> parseOptionType()
            else -> error(
                CangJieParsingBundle.message(
                    "parsing.error.expecting.found", "type name", "${builder.tokenText}"
                )
            )
        }
    }


}

enum class MacroType {
    MACRO_CALL, ANNOTATION,
}

enum class DeclarationParsingMode(
    val destructuringAllowed: Boolean, val accessorsAllowed: Boolean, val canBeEnumUsedAsSoftKeyword: Boolean
) {
    ALL(false, true, true), TOPLEVEL(false, true, true), MEMBER(false, true, true), MEMBER_OR_TOPLEVEL(
        false, true, true
    ),
    LOCAL(true, false, false)
    // SCRIPT_TOPLEVEL(true, true, false) // 如需使用可以取消注释
}

/**
 * 控制在解析声明时对名称标识符的处理方式。
 *
 * - [REQUIRED]：必须出现标识符，缺失时会报错。
 * - [ALLOWED]：标识符可选，存在则消费，不存在也不会报错。
 * - [PROHIBITED]：禁止出现标识符，出现时会报错并前进。
 */
enum class NameParsingMode {
    REQUIRED, ALLOWED, PROHIBITED
}
