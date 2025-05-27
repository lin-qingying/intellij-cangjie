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

package cn.cangnova.cangjie.parsing

import cn.cangnova.cangjie.lexer.CjTokens.*
import cn.cangnova.cangjie.psi.CjNodeTypes.*
import cn.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.CONSTRUCTOR_CALLEE
import com.intellij.lang.PsiBuilder
import com.intellij.lang.WhitespacesBinders

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.Contract

class CangJieParsing private constructor(
    builder: SemanticWhitespaceAwarePsiBuilder,
    isTopLevel: Boolean,
    isLazy: Boolean
) : AbstractCangJieParsing(
    builder, isLazy
) {

    companion object {
        public val PARAMETER_NAME_RECOVERY_SET = TokenSet.create(COLON, EQ, COMMA, RPAR)
        private val GT_COMMA_COLON_SET = TokenSet.create(GT, COMMA, COLON)
        private val LOG = Logger.getInstance(CangJieParsing::class.java)
        private val TOP_LEVEL_DECLARATION_FIRST = TokenSet.create(
            INTERFACE_KEYWORD,
            CLASS_KEYWORD,
            FUNC_KEYWORD,
            LET_KEYWORD,
            VAR_KEYWORD,
            CONST_KEYWORD,
            PACKAGE_KEYWORD
        )
        private val TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET =
            TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(SEMICOLON))
        private val LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET =
            TokenSet.orSet(TokenSet.create(LT, EQ, SEMICOLON), TOP_LEVEL_DECLARATION_FIRST)

        private val CLASS_NAME_RECOVERY_SET =
            TokenSet.orSet(TokenSet.create(LT, LPAR, COLON, LBRACE), TOP_LEVEL_DECLARATION_FIRST)
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
            TokenSet.create(IDENTIFIER, LBRACKET),
            TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUNC_KEYWORD))
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
            protected override fun create(builder: SemanticWhitespaceAwarePsiBuilder): CangJieParsing {
                return CangJieParsing.createForByClause(builder, super.isLazy)
            }
        }


   public class ModifierDetector : ((IElementType?) -> Unit) {

          var isAbstractDetected = false
          var isMutDetected = false
          var isPublicDetected = false
          var isPrivateDetected = false
          var isProtectedDetected = false
          var isOperatorDetected = false
          var isForeignDetected = false
          var isConstDetected = false
          var isUnsafeDetected = false
          var isSealedDetected = false
          var isRedefDetected = false
          var isOpenDetected = false
          var isStaticDetected = false
        // private var annotationCount = 0

        /**
         * 返回修饰符的数量
         */
        fun getSize(): Int {
            return ModifierDetector::class.members
                .filterIsInstance<kotlin.reflect.KProperty1<ModifierDetector, *>>()
                .count { prop ->
                    prop.get(this) == true
                }
        }

        override fun invoke(item: IElementType?) {
            when (item) {
                PUBLIC_KEYWORD -> isPublicDetected = true
                PRIVATE_KEYWORD -> isPrivateDetected = true
                PROTECTED_KEYWORD -> isProtectedDetected = true
                ABSTRACT_KEYWORD -> isAbstractDetected = true
                MUT_KEYWORD -> isMutDetected = true
                OPERATOR_KEYWORD -> isOperatorDetected = true
                FOREIGN_KEYWORD -> isForeignDetected = true
                CONST_KEYWORD -> isConstDetected = true
                UNSAFE_KEYWORD -> isUnsafeDetected = true
                OPEN_KEYWORD -> isOpenDetected = true
                STATIC_KEYWORD -> isStaticDetected = true
                SEALED_KEYWORD -> isSealedDetected = true
                REDEF_KEYWORD -> isRedefDetected = true
                // ANNOTATION_ENTRY -> annotationCount++
            }
        }


    }


    private val lastDotAfterReceiverNotLParPattern = LastBefore(
        AtSet(RECEIVER_TYPE_TERMINATORS),
        object : AbstractTokenStreamPredicate() {
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
        }
    )

    private val lastDotAfterReceiverLParPattern = FirstBefore(
        AtSet(RECEIVER_TYPE_TERMINATORS),
        object : AbstractTokenStreamPredicate() {
            override fun matching(topLevel: Boolean): Boolean {
                if (topLevel && atSet(definitelyOutOfReceiverSet)) {
                    return true
                }
                return topLevel && !at(QUEST) && !at(LPAR) && !at(RPAR)
            }
        }
    )



    fun parseTypeRef() {
        parseTypeRef(TokenSet.EMPTY, false)
    }

    fun parseTypeRefWithoutIntersections() {
        parseTypeRef(TokenSet.EMPTY)
    }

    fun parseTest() {
        // val a = mark()
        advance()
        // a.done(IMPORT_LIST)
    }

    public override fun create(builder: SemanticWhitespaceAwarePsiBuilder): CangJieParsing {
        return createForTopLevel(builder)
    }



    private fun parseThisOrSuper() {
        check(_at(THIS_KEYWORD) || _at(SUPER_KEYWORD))
        val mark = mark()

        advance() // THIS_KEYWORD | SUPER_KEYWORD

        mark.done(CONSTRUCTOR_DELEGATION_REFERENCE)
    }

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
    /*
       * block
       *   : "{" (expressions)* "}"
       */
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




    fun parseBlockExpression() {
        parseBlock(false)
    }

    /*
     * preamble
     *  : fileAnnotationList? packageDirective?
     */
    private fun parsePreamble() {
        val firstEntry = mark()

        /*
         * TODO fileAnnotationList Ko
         * 文档注释 : fileAnnotations*
         */

        /*
         * packageDirective  包声明
         *   : modifiers "package" SimpleName{"."} SEMI?
         */
        var packageDirective = mark()

        // 是否有修饰符
        var isPackageAccessModifier = false
        if (atSet(PACKAGE_ACCESS_MODIFIER_SET) &&
            (lookahead(1) == MACRO_KEYWORD || lookahead(1) == PACKAGE_KEYWORD)
        ) {
            advance() // 修饰符
            isPackageAccessModifier = true
        }

        if (at(MACRO_KEYWORD)) {
            advance() // MARCO_KEYWORD 宏声明
            isPackageAccessModifier = true
        }

        if (at(PACKAGE_KEYWORD)) {
            if (at(PACKAGE_KEYWORD)) {
                advance() // PACKAGE_KEYWORD
            } else if (isPackageAccessModifier) {
                error("Expecting package keyword")
            }

            // TODO 处理包名
            parsePackageName()

            firstEntry.drop()

            consumeIf(SEMICOLON)

            packageDirective.done(PACKAGE_DIRECTIVE)
        } else {
            // 忽略 package 指令时不应报错，将位置回滚
            firstEntry.rollbackTo()

            // TODO 解析文件注解列表
            // parseFileAnnotationList(FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED)
            packageDirective = mark()
            packageDirective.done(PACKAGE_DIRECTIVE)
            packageDirective.setCustomEdgeTokenBinders(
                BindFirstShebangWithWhitespaceOnly,
                null
            )

            // TODO 仓颉0.53.4：包中必须有包名，但单文件可无
        }

        parseImportDirectives()
    }

    private fun parseImportDirectives() {
        val importList = mark()

        if (!(at(IMPORT_KEYWORD) ||
                    atSet(IMPORT_ACCESS_MODIFIER_SET) && lookahead(1) == IMPORT_KEYWORD)
        ) {
            // 允许注释绑定到首个声明
            importList.setCustomEdgeTokenBinders(DoNotBindAnything, null)
        }

        while (at(IMPORT_KEYWORD) ||
            atSet(IMPORT_ACCESS_MODIFIER_SET) && lookahead(1) == IMPORT_KEYWORD
        ) {
            parseImportDirective()
        }

        importList.done(IMPORT_LIST)
    }

    private fun closeImportWithErrorIfNewline(
        importDirective: PsiBuilder.Marker?,
        importAlias: PsiBuilder.Marker?,
        errorMessage: String
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
     * 处理 import 关键字后的单个导入项
     * : "import"
     * : SimpleName{"."} ("." "*" )? | ("as" SimpleName{"."} ("." "*"))? SEMI?
     */
    private fun parseImportDirectiveItem(isTopLevel: Boolean): Boolean {
        var importDirectiveItem = mark()

        if (!at(IDENTIFIER)) {
            error("expected a package name after '.' in qualified name, found '${builder.tokenText}'")
            importDirectiveItem.done(IMPORT_DIRECTIVE_ITEM)
            consumeIf(SEMICOLON)
            return true
        }

        var qualifiedName = mark()
        var reference = mark()
        advance() // IDENTIFIER
        reference.done(REFERENCE_EXPRESSION)

        while (at(DOT) && lookahead(1) != MUL) {
            advance() // DOT

            // 同一个包多个导入项
            if (at(LBRACE) && isTopLevel) {
                qualifiedName.rollbackTo()
                importDirectiveItem.rollbackTo()
                return false // parseImportDirectiveItem2() 将被调用
            } else {
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
        }

        qualifiedName.drop()

        when {
            at(DOT) -> {
                advance()
                assert(_at(MUL))
                advance()
                if (at(AS_KEYWORD)) {
                    errorAndAdvance("Aliases are not allowed for all imports")
                }
            }

            at(AS_KEYWORD) -> {
                val alias = mark()
                advance() // AS_KEYWORD
                expect(IDENTIFIER, "Expecting identifier", SEMICOLON_SET)
                alias.done(IMPORT_ALIAS)
            }
        }

        importDirectiveItem.done(IMPORT_DIRECTIVE_ITEM)
        return true
    }

    private fun parseImportDirectiveItem2() {
        if (!at(IDENTIFIER)) {
            error("expected a package name after '.' in qualified name, found '${builder.tokenText}'")
            consumeIf(SEMICOLON)
            return
        }

        var qualifiedName = mark()
        var reference = mark()
        advance() // IDENTIFIER
        reference.done(REFERENCE_EXPRESSION)

        while (at(DOT) && lookahead(1) != MUL && lookahead(1) != LBRACE) {
            advance()
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

        expect(DOT, "Expecting '.'")
        expect(LBRACE, "Expecting '{'")

        do {
            expect(COMMA)
            parseImportDirectiveItem(false)
        } while (at(COMMA))

        expect(RBRACE, "Expecting '}'")
    }

    private fun parseImportDirective() {
        assert(_at(IMPORT_KEYWORD) || _atSet(IMPORT_ACCESS_MODIFIER_SET))

        val doneType = IMPORT_DIRECTIVE
        val importDirective = mark()

        if (_atSet(IMPORT_ACCESS_MODIFIER_SET)) {
            advance() // PUBLIC_KEYWORD
        }

        if (!at(IMPORT_KEYWORD)) {
            error("Expecting 'import' keyword")
            importDirective.done(doneType)
            return
        }

        advance() // IMPORT_KEYWORD

        if (closeImportWithErrorIfNewline(importDirective, null, "Expecting qualified name")) {
            return
        }

        if (at(LBRACE)) {
            advance()
            parseImportDirectiveItem(false)

            // 多个导入语句
            while (at(COMMA)) {
                advance()
                parseImportDirectiveItem(false)
            }

            expect(RBRACE, "Expecting '}'")
        } else {
            if (!parseImportDirectiveItem(true)) {
                parseImportDirectiveItem2()
            }
        }

        consumeIf(SEMICOLON)
        importDirective.done(doneType)
        importDirective.setCustomEdgeTokenBinders(null, TrailingCommentsBinder)
    }

    /* SimpleName{"."} */
    private fun parsePackageName() {
        var qualifiedExpression = mark()
        var simpleName = true

        while (true) {
            if (builder.newlineBeforeCurrentToken()) {
                errorWithRecovery(
                    "Package name must be a '.'-separated identifier list placed on a single line",
                    PACKAGE_NAME_RECOVERY_SET
                )
                break
            }

            if (at(DOT)) {
                advance() // DOT
                qualifiedExpression.error("Package name must be a '.'-separated identifier list")
                qualifiedExpression = mark()
                continue
            }

            val nsName = mark()
            val simpleNameFound = expect(
                IDENTIFIER,
                "Package name must be a '.'-separated identifier list",
                PACKAGE_NAME_RECOVERY_SET
            )
            if (simpleNameFound) {
                nsName.done(REFERENCE_EXPRESSION)
            } else {
                nsName.drop()
            }

            if (!simpleName) {
                val precedingMarker = qualifiedExpression.precede()
                qualifiedExpression.done(DOT_QUALIFIED_EXPRESSION)
                qualifiedExpression = precedingMarker
            }

            if (at(DOT)) {
                advance() // DOT

                if (simpleName && !simpleNameFound) {
                    qualifiedExpression.drop()
                    qualifiedExpression = mark()
                } else {
                    simpleName = false
                }
            } else {
                break
            }
        }

        qualifiedExpression.drop()
    }

    fun parseScript() {
        val fileMarker = mark()
        fileMarker.done(CJ_SCRIPT)
    }

    // 入口
    fun parseFile() {
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

    fun parseLspFile() {
        val fileMarker = mark()

        // 将所有节点全都读取
        while (!eof()) {
            advance()
        }

        fileMarker.done(CJ_FILE)
    }

    private fun checkUnclosedBlockComment() {
        if (BLOCK_DOC_COMMENT_SET.contains(builder.rawLookup(-1))) {
            val startOffset = builder.rawTokenTypeStart(-1)
            val endOffset = builder.rawTokenTypeStart(0)
            val tokenChars = builder.originalText.subSequence(startOffset, endOffset)

            if (!(tokenChars.length > 2 &&
                        tokenChars.subSequence(tokenChars.length - 2, tokenChars.length).toString() == "*/")
            ) {
                val marker = builder.mark()
                marker.error("Unclosed comment")
                marker.setCustomEdgeTokenBinders(WhitespacesBinders.GREEDY_RIGHT_BINDER, null)
            }
        }
    }

    private fun parseTopLevelDeclaration() {
        parseTopLevelDeclaration(false)
    }

    /*
     * 顶层声明语句
     *   : function
     *   : class enum interface struct
     */
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

        val detector = ModifierDetector()

        parseModifierList(detector, TokenSet.EMPTY, parseMacro)
        val declType = parseCommonDeclaration(detector, NameParsingMode.REQUIRED, DeclarationParsingMode.TOPLEVEL)

        /*
        if (declType == ANNOTATION_ENTRY) {
            decl.rollbackTo() // 返回给文档流并重新解析
            // decl.drop()
            // 应该为注解，加入到修饰符中并重新解析声明
            parseTopLevelDeclaration(true)
        } else
        */
        if (declType == null) {
            errorAndAdvance("Expecting a top level declaration") // 期待一个顶层声明语句
            // decl.error("Expecting a top level declaration")
            decl.drop()
        } else {
            closeDeclarationWithCommentBinders(decl, declType, true)
        }
    }

    private fun tryParseModifier(
        tokenConsumer: ((IElementType) -> Unit)?,
        noModifiersBefore: TokenSet,
        modifierKeywords: TokenSet
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


    private fun doParseModifierListBody(
        tokenConsumer: ((IElementType) -> Unit)?,
        modifierKeywords: TokenSet,
        noModifiersBefore: TokenSet,
        isParseMacro: Boolean = false
    ): Boolean {
        var empty = true

        while (!eof()) {
            /*
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

    fun parseLambdaExpression() {
        expressionParsing.parseFunctionLiteral(preferBlock = false, collapse = false, false)
    }

    /*
     * TODO 注解与宏
     * annotation
     *   : "@" (annotationUseSiteTarget ":")? unescapedAnnotation
     *   ;
     *
     * unescapedAnnotation
     *   : SimpleName{"."} typeArguments? valueArguments?
     *   ;
     */
    fun parseAnnotation(detector: ModifierDetector?): IElementType? {
        assert(_at(AT))
        val nextRawToken = lookahead(1)

        val modifierSize = detector?.getSize() ?: 0

        if (nextRawToken == IDENTIFIER) {
            advance() // consume AT '@'

            val reference = mark()
            val typeReference = mark()
            parseUserType()
            typeReference.done(TYPE_REFERENCE)
            reference.done(CONSTRUCTOR_CALLEE)

            if (at(LBRACKET)) {
                expressionParsing.parseValueArgumentList(LBRACKET, RBRACKET)
            }
        } else {
            errorAndAdvance("Expected annotation identifier after '@'", 1)
            return null
        }

        if (at(LPAR)) {
            // TODO 处理宏调用
            advance() // consume '('

            if (at(RPAR)) {
                advance() // consume ')'
            } else {
                while (!eof()) {
                    if (at(COMMA)) {
                        advance()
                    }
                    expressionParsing.parseStatementByScope(DeclarationParsingMode.ALL)

                    if (at(RPAR)) {
                        break
                    }
                }
                expect(RPAR, "expected ')'")
            }
            // parseMacroInputExprWithParens();
            return MACRO_EXPRESSION
        } else {
            return if (modifierSize > 0) {
                error("Should call (..) for macros")
                MACRO_EXPRESSION
            } else {
                ANNOTATION_ENTRY
            }
        }
    }

    /**
     * (modifier )*
     *
     * 如果不为空，则将修饰符(非批注)馈送到传递的使用者
     *
     * @param noModifiersBefore 是一个令牌集，其中包含指示何时满足这些元素的元素。
     *                          必须将前一个令牌解析为标识符，而不是修饰符
     */
    fun parseModifierList(
        tokenConsumer: ((IElementType) -> Unit)?,
        noModifiersBefore: TokenSet,
        isParseMacro: Boolean = false
    ): Boolean {
        // 这里你可以继续调用你之前重写的 doParseModifierListBody
        return doParseModifierListBody(tokenConsumer, MODIFIER_KEYWORDS, noModifiersBefore, isParseMacro)
    }



    private fun doParseModifierList(
        tokenConsumer: ((IElementType) -> Unit)?,
        modifierKeywords: TokenSet,
        noModifiersBefore: TokenSet,
        isParseMacro: Boolean = false
    ): Boolean {
        val list = mark()

        val empty = doParseModifierListBody(tokenConsumer, modifierKeywords, noModifiersBefore, isParseMacro)

        if (empty) {
            list.drop()
        } else {
            list.done(MODIFIER_LIST)
        }
        return !empty
    }

    private fun parseClassCommonDeclaration(
        tokenId: Int?,
        classdetector: ModifierDetector,
        detector: ModifierDetector
    ): IElementType? {
        return when (getTokenId()) {
            AT_Id -> expressionParsing.parseMacroExpression(true)
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
                parseProperty(  classdetector = classdetector, detector=detector)
            }

            LET_KEYWORD_Id, VAR_KEYWORD_Id, CONST_KEYWORD_Id -> parseVariable(
                classdetector,
                DeclarationParsingMode.MEMBER
            )

            else -> null
        }
    }

    private fun parseClassInitializer(): IElementType? {
        return null
    }

    private fun parsePropertyDelegateOrAssignment(): Boolean {
        if (at(EQ)) {
            advance() // consume EQ token
            expressionParsing.parseExpression()
            return true
        }
        return false
    }

    private fun lastDotAfterReceiver(): Int {
        val pattern = if (at(LPAR)) lastDotAfterReceiverLParPattern else lastDotAfterReceiverNotLParPattern
        pattern.reset()
        return matchTokenStreamPredicate(pattern)
    }

    private fun parseReceiverType(title: String, nameFollow: TokenSet): Boolean {
        val lastDot = lastDotAfterReceiver()
        val receiverPresent = lastDot != -1

        if (!receiverPresent) return false

        createTruncatedBuilder(lastDot).parseTypeRef()

        if (atSet(RECEIVER_TYPE_TERMINATORS)) {
            advance() // expectation
        } else {
            errorWithRecovery("Expecting '.' before a $title name", nameFollow)
        }
        return true
    }


    /**
     * variableDeclarationEntry
     *   : SimpleName (":" ('?')?type)?
     *   ;
     *
     * property
     *   : modifiers ("let" | "var" | "const")
     *   ;
     */
    fun parseVariable(
        classdetector: ModifierDetector,
        declarationParsingMode: DeclarationParsingMode? = null
    ): IElementType {
        assert(at(LET_KEYWORD) || at(VAR_KEYWORD) || at(CONST_KEYWORD))
        advance()

        if (declarationParsingMode == DeclarationParsingMode.MEMBER) {
            parseIdentifierByTitle("variable", PROPERTY_NAME_FOLLOW_SET, true)
        } else {
            expressionParsing.parsePattern(
                PatternConfig(true),
                Pattern.Wildcard,
                Pattern.Binding,
                Pattern.Tuple,
                Pattern.Enum
            )
        }

        var noTypeReference = true

        if (at(COLON)) {
            advance() // COLON
            noTypeReference = false

            parseTypeRef()
        }

        if (at(EQ)) {
            advance() // EQ
            expressionParsing.parseExpression()
        }

        return VARIABLE
    }

    fun parseExpressionCodeFragment() {
        val marker = mark()
        expressionParsing.parseExpression()

        checkForUnexpectedSymbols()

        marker.done(EXPRESSION_CODE_FRAGMENT)
    }

    fun parseBlockCodeFragment() {
        val marker = mark()
        val blockMarker = mark()

        if (at(PACKAGE_KEYWORD) || at(IMPORT_KEYWORD)) {
            val err = mark()
            parsePreamble()
            err.error("Package directive and imports are forbidden in code fragments")
        }

        expressionParsing.parseStatements()

        checkForUnexpectedSymbols()

        blockMarker.done(BLOCK)
        marker.done(BLOCK_CODE_FRAGMENT)
    }

    fun parseCommonDeclaration(
        detector: ModifierDetector,
        nameParsingMode: NameParsingMode,
        declarationParsingMode: DeclarationParsingMode
    ): IElementType? {

        return when (getTokenId()) {
            TYPE_KEYWORD_Id -> when (declarationParsingMode) {
                DeclarationParsingMode.LOCAL,
                DeclarationParsingMode.MEMBER,
                DeclarationParsingMode.MEMBER_OR_TOPLEVEL -> {
                    parseTypeAlias()
                    INVALID_DECLARATION
                }

                DeclarationParsingMode.ALL,
                DeclarationParsingMode.TOPLEVEL -> parseTypeAlias()
            }

            AT_Id -> expressionParsing.parseMacroExpression(true)

            FOREIGN_KEYWORD_Id -> when (declarationParsingMode) {
                DeclarationParsingMode.ALL,
                DeclarationParsingMode.TOPLEVEL -> parseForeign()

                DeclarationParsingMode.MEMBER,
                DeclarationParsingMode.MEMBER_OR_TOPLEVEL,
                DeclarationParsingMode.LOCAL -> {
                    parseForeign()
                    INVALID_DECLARATION
                }
            }

            MACRO_KEYWORD_Id -> parseMacro()

            FUNC_KEYWORD_Id -> parseFunction(detector = detector)

            MAIN_KEYWORD_Id -> when (declarationParsingMode) {
                DeclarationParsingMode.ALL,
                DeclarationParsingMode.TOPLEVEL -> parseMainFunc()

                DeclarationParsingMode.MEMBER,
                DeclarationParsingMode.MEMBER_OR_TOPLEVEL,
                DeclarationParsingMode.LOCAL -> {
                    parseMainFunc()
                    INVALID_DECLARATION
                }
            }

            EXTEND_KEYWORD_Id, ENUM_KEYWORD_Id, STRUCT_KEYWORD_Id, INTERFACE_KEYWORD_Id, CLASS_KEYWORD_Id -> when (declarationParsingMode) {
                DeclarationParsingMode.ALL,
                DeclarationParsingMode.TOPLEVEL -> parseClass(detector)

                DeclarationParsingMode.MEMBER,
                DeclarationParsingMode.MEMBER_OR_TOPLEVEL,
                DeclarationParsingMode.LOCAL -> {
                    parseClass(detector)
                    INVALID_DECLARATION
                }
            }

            LET_KEYWORD_Id, VAR_KEYWORD_Id, CONST_KEYWORD_Id -> parseVariable(detector, declarationParsingMode)

            else -> null
        }
    }

    /**
     * typeAlias
     *   : modifiers "typealias" SimpleName typeParameters? "=" type
     *   ;
     */
    private fun parseTypeAlias(): IElementType {
        assert(_at(TYPE_KEYWORD))

        advance() // TYPE_KEYWORD

        expect(IDENTIFIER, "Type name expected", LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET)

        parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET)

        if (at(WHERE_KEYWORD)) {
            val error = mark()
            parseTypeConstraints()
            error.error("Type alias parameters can't have bounds")
        }

        expect(EQ, "Expecting '='", TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET)

        parseTypeRef()

        consumeIf(SEMICOLON)

        return TYPEALIAS
    }



    /*
     * prop
     *   :  "mnt"? prop Identifier :Type propBody
     *   ;
     */
    fun parseProperty(
        isInterface: Boolean = false,
        classdetector: ModifierDetector? = null,
        detector: ModifierDetector? = null
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
                    KEYWORDS,
                    TokenSet.create(OPEN_KEYWORD, ABSTRACT_KEYWORD, SEALED_KEYWORD)
                )
                while (!atSet(tokenSet) && !eof()) {
                    advance()
                }
                body.error("Property body is not allowed in declarations file")
            }
            return PROPERTY
        }

        if (at(LBRACE)) {
            parsePropertyBody(detector)
        } else if (!isInterface) {
            if (classdetector != null && !classdetector.isAbstractDetected) {
                error("unimplemented abstract property")
                error("Missing prop body Expecting '{'")
            }
        }

        return PROPERTY
    }

    /**
     * ":" type
     */
    private fun parseByType() {
        if (at(COLON)) {
            advance() // COLON
            parseTypeRef()
        } else {
            error("Missing type Expecting ':' type")
        }
    }

    /**
     * prop get
     * :  "get" "(" ")" block
     */
    private fun parsePropertyGet() {
        assert(_at(GET_KEYWORD))

        val get = mark()
        advance() // GET_KEYWORD

        if (expect(LPAR, "Expecting '('")) {
            if (expect(RPAR, "Expecting ')'")) {
                if (at(LBRACE)) {
                    parseBlock()
                } else {
                    error("Expecting '{'")
                }
            }
        }

        get.done(PROPERTY_ACCESSOR)
    }

    /**
     * prop set
     * :  "set" "(" Identifier ")" block
     */
    private fun parsePropertySet(detector: ModifierDetector?) {
        assert(_at(SET_KEYWORD))
        val set = mark()

        // if(detector?.isMutDetected() == true) {
        advance() // SET_KEYWORD

        if (expect(LPAR, "Expecting '('")) {
            val plist = mark()
            val value = mark()

            expect(IDENTIFIER, "Expecting identifier")
            value.done(VALUE_PARAMETER)
            plist.done(VALUE_PARAMETER_LIST)

            if (expect(RPAR, "Expecting ')'")) {
                if (at(LBRACE)) {
                    parseBlock()
                } else {
                    error("Expecting '{'")
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

    /*
     * propBody
     *   :  {
     *   ;   get(){
     *   ;        func
     *   ;        expr
     *   ;     }
     *   ;    is mut
     *   ;    set(value){
     *   ;       func
     *   ;       expr
     *   ;      }
     *   ;   }
     */
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

    private fun parsePropertyAccessor() {
        if (at(GET_KEYWORD)) {
            parsePropertyGet()
        }
        if (at(SET_KEYWORD)) {
            parsePropertySet(null)
        }
    }

    /*
     * enum
     *   : "enum" SimpleName ("{" enumEntry((Type)?){"|"}  "}")
     *   ;
     */
    private fun parseEnum(): IElementType {
        assert(_at(ENUM_KEYWORD))
        advance()

        parseIdentifierByTitle("enum", IDENTIFIER_RBRACKET_LBRACKET_SET, false)

        parseEnumBody()

        return ENUM
    }

    private fun parseEnumBody() {
        val body = mark()
        if (at(LBRACE)) {
            advance() // LBRACE

            expect(OR)

            if (at(IDENTIFIER)) {
                parseEnumList()
            } else {
                error("Expecting enum entry")
            }

            parseMembers(null, null)

            expect(RBRACE, "Expecting '}'")
        } else {
            error("Expecting '{'")
        }
        body.done(ENUM_BODY)
    }

    private fun parseEnumList() {
        while (true) {
            parseEnumEntry()
            when {
                at(RBRACE) -> break
                at(OR) -> advance()
                else -> break
            }
        }
    }

    fun parseTypeCodeFragment() {
        val marker = mark()
        parseTypeRef()

        checkForUnexpectedSymbols()

        marker.done(TYPE_CODE_FRAGMENT)
    }

    private fun checkForUnexpectedSymbols() {
        while (!eof()) {
            errorAndAdvance("Unexpected symbol")
        }
    }



    fun parseEnumEntry(isCreateMark: Boolean = true): Boolean {
        val entry = if (isCreateMark) mark() else null

        if (!expect(IDENTIFIER, "Expecting enum entry name")) {
            entry?.drop()
            return false
        }

//    parseIdentifierByTitle("enum entry", IDENTIFIER_RBRACKET_LBRACKET_SET)

//    处理泛型
//    parseTypeArgumentList()

        if (at(LPAR)) {
            advance() // LPAR
            parseTypeList()
            expect(RPAR, "Expecting ')'")
        }

        entry?.done(ENUM_ENTRY)
        return true
    }

    /**
     * typelist
     * : type{","}
     */
    fun parseTypeList() {
        val list = mark()

        while (true) {
            parseTypeRef()
            if (!at(COMMA)) break
            advance() // COMMA
        }

        list.done(TYPE_LIST)
    }

    /*
     * typeParameters
     *   : ("<" typeParameter{","} ">"
     *   ;
     */
    private fun parseTypeParameterList(recoverySet: TokenSet): Boolean {
        var result = false
        if (at(LT)) {
            val list = mark()

            builder.disableNewlines()
            advance() // LT

            while (true) {
                if (at(COMMA)) errorAndAdvance("Expecting type parameter declaration")
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

    /*
     * typeParameter
     *   : modifiers SimpleName (":" userType)?
     *   ;
     */
    private fun parseTypeParameter() {
        if (atSet(TYPE_PARAMETER_GT_RECOVERY_SET)) {
            error("Type parameter declaration expected")
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

    private fun parseDelegationSpecifier() {
        val delegator = mark()
        val reference = mark()
        parseTypeRef()

        reference.drop()
        delegator.done(SUPER_TYPE_ENTRY)
    }

    /*
     * delegationSpecifier{"&"}
     */
    private fun parseDelegationSpecifierList() {
        val list = mark()

        while (true) {
            if (at(AND)) {
                errorAndAdvance("Expecting a delegation specifier")
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
    fun parseModifierList(noModifiersBefore: TokenSet): Boolean {
        return parseModifierList(null, noModifiersBefore)
    }

    /**
     * class
     *   : "class" SimpleName (<: delegationSpecifier{"&"}) classBody
     *   ;
     */
    fun parseClass(detector: ModifierDetector): IElementType {
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
            parseIdentifier() // 类名
            typeParametersDeclared = parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET)
        }

        // TODO 继承
        if (at(LTCOLON)) {
            advance() // COLON
            parseDelegationSpecifierList()
        }

        val whereMarker = OptionalMarker(false)
        parseTypeConstraintsGuarded(typeParametersDeclared)
        whereMarker.error("Where clause is not allowed")

        if (at(LBRACE)) {
            when (tokenId) {
                ENUM_KEYWORD_Id -> parseEnumBody()
                EXTEND_KEYWORD_Id, STRUCT_KEYWORD_Id, INTERFACE_KEYWORD_Id, CLASS_KEYWORD_Id -> parseClassBody(
                    tokenId,
                    detector
                )

                else -> parseClassBody(tokenId, detector)
            }
        } else {
            error("Expecting '{' or Inherit") // 应该为 '{' 或者继承
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
    private fun parseTypeConstraintsGuarded(typeParameterListOccurred: Boolean) {
        val error = mark()
        val constraints = parseTypeConstraints()
        errorIf(
            error,
            constraints && !typeParameterListOccurred,
            "Type constraints are not allowed when no type parameters declared"
        )
    }

    private fun parseTypeConstraints(): Boolean {
        if (at(WHERE_KEYWORD)) {
            parseTypeConstraintList()
            return true
        }
        return false
    }

    /*
     * typeConstraint{","}
     */
    private fun parseTypeConstraintList() {
        assert(_at(WHERE_KEYWORD))

        advance() // WHERE_KEYWORD

        val list = mark()

        while (true) {
            if (at(COMMA)) errorAndAdvance("Type constraint expected")
            parseTypeConstraint()
            if (!at(COMMA)) break
            advance() // COMMA
        }

        list.done(TYPE_CONSTRAINT_LIST)
    }

    /*
     * typeConstraint
     *   :   SimpleName "<:" type
     *   ;
     */
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


    private fun parseMemberDeclaration(tokenId: Int?, classdetector: ModifierDetector, rollbackMacro: Boolean = false) {
        if (at(SEMICOLON)) {
            advance() // SEMICOLON
            return
        }
        val decl = mark()

        val detector = ModifierDetector()
        parseModifierList(detector, TokenSet.EMPTY, rollbackMacro)

        val declType = parseMemberDeclarationRest(tokenId, classdetector, detector)

        /*if (declType == ANNOTATION_ENTRY) {
            decl.rollbackTo()
            parseMemberDeclaration(tokenId, classdetector, true)
        } else */
        if (declType == null) {
            errorWithRecovery("Expecting member declaration", TokenSet.EMPTY)
            decl.drop()
        } else {
            closeDeclarationWithCommentBinders(decl, declType, true)
        }
    }

    private fun parseMemberDeclarationRest(
        tokenId: Int?,
        classdetector: ModifierDetector,
        detector: ModifierDetector
    ): IElementType? {
        var declType = parseClassCommonDeclaration(tokenId, classdetector, detector)

        if (declType != null) return declType

        if (tokenId != null && tokenId != INTERFACE_KEYWORD_Id) {

            parseModifierList(TokenSet.EMPTY)

            when {
                at(INIT_KEYWORD) -> {
                    parseInitFunc()
                    declType = SECONDARY_CONSTRUCTOR
                }

                at(LBRACE) -> {
                    error("Expecting member declaration")
                    parseBlock()
                    declType = FUNC
                }

                at(IDENTIFIER) && lookahead(1) == LPAR -> {
                    // 主构造函数
                    parseMainInitFunc()
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

    fun parseMainInitFunc() {
        assert(_at(IDENTIFIER))
        advance() // IDENTIFIER

        if (at(RBRACE)) {
            error("Function body expected")  // 应该为函数体
            return
        }

        builder.disableJoiningComplexTokens()
        // 类型参数
        if (at(LPAR)) {
            parseInitFuncValueParameterList()
        } else {
            // error("Expecting '(' ")  // 应该为'('
            errorAndAdvance("Expecting '(' but available " + builder.tokenText)
        }

        parseInitFunctionBody()
    }

    fun parseInitFunc() {
        assert(_at(INIT_KEYWORD))
        advance() // INIT_KEYWORD

        if (at(RBRACE)) {
            error("Function body expected")  // 应该为函数体
            return
        }

        builder.disableJoiningComplexTokens()
        // 类型参数
        if (at(LPAR)) {
            parseInitFuncValueParameterList()
        } else {
            // error("Expecting '(' ")  // 应该为'('
            errorAndAdvance("Expecting '(' but available " + builder.tokenText)
        }

        if (isDeclarationsFile) {
            if (at(LBRACE)) {
                val body = mark()
                while (!atSet(KEYWORDALL) && !eof()) {
                    advance()
                }
                // parseFunctionBody()
                body.error("Method bodies are not allowed in declaration files")
            }
            return
        }

        parseInitFunctionBody()
    }

    /**
     * members
     * : memberDeclaration*
     * ;
     */
    private fun parseMembers(tokenId: Int?, detector: ModifierDetector?) {
        while (!eof() && !at(RBRACE)) {
            parseMemberDeclaration(tokenId, detector ?: ModifierDetector())
        }
    }

    private fun parseClassBody(tokenId: Int?, detector: ModifierDetector) {
        val body = mark()

        builder.enableNewlines()

        if (expect(LBRACE, "Expecting a class body")) {
            parseMembers(tokenId, detector)
            expect(RBRACE, "Missing '}'")
        }

        builder.restoreNewlinesState()

        body.done(CLASS_BODY)
    }

    private fun parseMainFunc(): IElementType {
        assert(_at(MAIN_KEYWORD))
        advance()

        if (at(RBRACE)) {
            error("Function body expected")  // 应该为函数体
            return MAIN_FUNC
        }

        builder.disableJoiningComplexTokens()
        // 类型参数
        if (at(LPAR)) {
            parseValueParameterList(false, false, VALUE_PARAMETERS_FOLLOW_SET)
        } else {
            error("Expecting '(' ")  // 应该为'('
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
            error("Expecting '{' ")  // 应该为'{'
        }
        return MAIN_FUNC
    }

    @Contract("false -> !null")
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
        if (topTokenId == EXTEND_KEYWORD_Id) {
            type = FUNC_EXTEND
        }

        if (at(RBRACE)) {
            error("Function body expected") // 应该为函数体
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
                mark.error("Should be an overloaded operator")
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
            parseValueParameterList(false, false, VALUE_PARAMETERS_FOLLOW_SET)
        } else {
            error("Expecting '(' ") // 应该为'('
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
                body.error("Method bodies are not allowed in declaration files")
            }
            return type
        }

        if (at(LBRACE)) {
            parseFunctionBody()
            if (isForeign) {
                error("foreign function can not have body")
            }
        } else if (!(isInterfaceMethod || (classdetector?.isAbstractDetected == true)) && (detector?.isForeignDetected != true)) {
            error("Expecting '{' ") // 应该为'{'
        }

        return type
    }

    fun parseForeignFunction(detector: ModifierDetector, topTokenId: Int = 0): IElementType {
        return parseFunction(
            isInterfaceMethod = false,
            classdetector = null,
            detector = detector,
            isForeign = true,
            topTokenId = topTokenId
        )
    }

    private fun parseIdentifier() {
        if (expect(IDENTIFIER)) return

        if (atSet(KEYWORDS)) {
            error("Keywords cannot be used") // 关键字不能使用
            return
        }

        if (!at(LPAR)) {
            errorAndAdvance("Expecting a CangJie identifier")
            return
        }

        error("Expecting a CangJie identifier") // 应该为标识符
    }

    private fun parseIdentifierByTitle(
        title: String,
        recoverySet: TokenSet = TokenSet.EMPTY,
        isUnderline: Boolean = false
    ) {
        if (isUnderline && expect(CangJieExpressionParsing.IDENTIFIER_RECOVERY_SET)) {
            return
        }

        if (expect(IDENTIFIER)) {
            return
        }

        errorWithRecovery("Expecting $title name", recoverySet)
    }

    fun parseSynchronizedExpression() {
        assert(_at(SYNCHRONIZED_KEYWORD))
        val synchronizedMarker = mark()
        advance()

        if (at(LPAR)) {
            advance()
            expressionParsing.parseExpression()
            expect(RPAR, "Expecting ')'")
        } else {
            error("Expecting '('")
        }

        if (at(LBRACE)) {
            parseBlock()
        } else {
            error("Expecting '{'") // 应该为'{'
        }

        synchronizedMarker.done(SYNCHRONIZED_EXPRESSION)
    }

    /**
     * 外部函数声明块
     */
    private fun parseForeign(): IElementType {
        assert(_at(FOREIGN_KEYWORD))
        advance()

        if (at(LBRACE)) {
            parseForeignBody()
        } else {
            error("Expecting '{'")
        }

        return FOREIGN
    }

    /**
     * 外部函数声明块内容
     */
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

    fun parseMacro(): IElementType {
        assert(_at(MACRO_KEYWORD))
        advance()

        if (at(RBRACE)) {
            error("Function body expected") // 应该为函数体
            return MACRO
        }

        builder.disableJoiningComplexTokens()

        // 函数名
        parseIdentifier()

        var typeParameterListOccurred = false
        if (at(LT)) {
            parseTypeParameterList(LBRACKET_LBRACE_RBRACE_LPAR_SET)
            typeParameterListOccurred = true
        }

        // 类型参数
        if (at(LPAR)) {
            parseValueParameterList(false, false, VALUE_PARAMETERS_FOLLOW_SET)
        } else {
            error("Expecting '('") // 应该为'('
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
                body.error("Method bodies are not allowed in declaration files")
            }
            return MACRO
        }

        // 函数体
        if (at(LBRACE)) {
            parseFunctionBody()
        } else {
            error("Expecting '{'") // 应该为'{'
        }
        return MACRO
    }

    /**
     * init 函数体恢复
     */
    fun parseInitFunctionBody() {
        if (at(COLON)) {
            val error = mark()
            while (!at(LBRACE)) advance()
            error.error("Expecting '{'")
        }

        if (isDeclarationsFile) {
            if (at(LBRACE)) {
                val body = mark()
                while (!atSet(KEYWORDALL) && !eof()) {
                    advance()
                }
                body.error("Method bodies are not allowed in declaration files")
            }
            return
        }

        if (at(LBRACE)) {
            parseInitFunctionBlock()
        } else {
            error("Expecting function body") // 应该为函数体
        }
    }

    /**
     * 函数体解析，只支持块
     */
    fun parseFunctionBody() {
        if (at(LBRACE)) {
            parseBlock()
        } else {
            error("Expecting function body") // 应该为函数体
        }
    }

    /**
     * 尝试解析函数参数，类型是否必须由 typeRequired 决定
     */
    fun tryParseValueParameter(typeRequired: Boolean): Boolean {
        return parseValueParameter(true, typeRequired)
    }

    fun parseInitFuncValueParameterList() {
        parseValueParameterList(false, false, VALUE_PARAMETERS_FOLLOW_SET, true)
    }


    fun parseValueParameterList(
        isFunctionTypeContents: Boolean,
        typeRequired: Boolean,
        recoverySet: TokenSet,
        isInitFunc: Boolean = false
    ) {
        assert(at(LPAR))
        val parameters = mark()

        builder.disableNewlines()
        advance() // consume '('

        val isNamedParameters = mutableListOf<Boolean>()

        while (!at(RPAR) && !atSet(recoverySet) && !eof()) {
            if (at(COMMA)) {
                errorAndAdvance("Expecting a parameter declaration") // 应该为参数声明
            }
            if (isFunctionTypeContents) {
                if (!tryParseValueParameter(typeRequired)) {
                    val valueParameter = mark()
                    // 你原来注释掉的parseFunctionTypeValueParameterModifierList()可根据需求添加
                    parseTypeRef()
                    closeDeclarationWithCommentBinders(valueParameter, VALUE_PARAMETER, false)
                    isNamedParameters.add(false)
                } else {
                    isNamedParameters.add(true)
                }
            } else {
                parseValueParameter(false, typeRequired, isInitFunc)
            }

            if (at(COMMA)) {
                advance() // consume ','

                if (at(RPAR)) {
                    error("Expecting a parameter declaration") // 应该为参数声明
                }
            } else {
                if (!at(RPAR)) {
                    errorAndAdvance("Expecting ',' or ')', found '${builder.tokenText}'")
                }
            }
        }

        expect(RPAR, "Expecting ')'", recoverySet)
        builder.restoreNewlinesState()

        if (isNamedParameters.contains(true) && isNamedParameters.contains(false)) {
            // 要么全为 true，要么全为 false，混合是不允许的
            parameters.error("In a parameter type list, either all parameters must be named, or none of them; mixed is not allowed")
        } else {
            parameters.done(VALUE_PARAMETER_LIST)
        }
    }




      fun parseValueParameter(
        rollbackOnFailure: Boolean = false,
        typeRequired: Boolean = false,
        isInitFunc: Boolean = false
    ): Boolean {
        val parameter = mark()

        if (isInitFunc) {
            val detector = ModifierDetector()
            parseModifierList(detector, TokenSet.EMPTY, true)

            when {
                at(LET_KEYWORD) || at(VAR_KEYWORD) -> advance()
                detector.getSize() > 0 -> error("Missing variable declaration symbol let or var after modifier")
            }
        }

        if (!parseFunctionParameterRest(typeRequired) && rollbackOnFailure) {
            parameter.rollbackTo()
            return false
        }

        closeDeclarationWithCommentBinders(parameter, VALUE_PARAMETER, false)
        return true
    }

    private fun parseFunctionParameterRest(typeRequired: Boolean): Boolean {
        var noErrors = true
        var isDefault = false

        // 恢复 'func foo(Array<String>) {}' 和 'func foo(: Int) {}' 这种情况
        if ((at(IDENTIFIER) && lookahead(1) == LT) || at(COLON)) {
            error("Missing parameter name") // 缺少参数名称
            if (at(COLON)) {
                // 保留 noErrors == true 以避免函数类型解析中回滚
                advance() // :
            } else {
                noErrors = false
            }
            parseTypeRef()
        } else {
            expect(
                CangJieExpressionParsing.IDENTIFIER_RECOVERY_SET,
                "Missing parameter name",
                PARAMETER_NAME_RECOVERY_SET
            )

            if (expect(EXCL)) {
                // 可以有默认值
                isDefault = true
            }

            if (at(COLON)) {
                advance() // :

                if (at(IDENTIFIER) && lookahead(1) == COLON) {
                    // 恢复 "func foo(x: y: Int)" 中错误的类型引用
                    error("Type reference expected")
                    return false
                }

                parseTypeRef()
            } else if (typeRequired) {
                errorWithRecovery("Parameters must have type annotation", PARAMETER_NAME_RECOVERY_SET)
                noErrors = false
            } else {
                errorWithoutAdvancing("Expecting ':' Missing type declaration") // 应该为 ':'
                noErrors = false
            }
        }

        if (at(EQ)) {
            if (isDefault) {
                advance()
            } else {
                error("The default value cannot be set for non-named parameters")
                errorAndAdvance("Expecting ',' or ')', found '='")
                noErrors = false
            }

            expressionParsing.parseExpression()
        }

        return noErrors
    }

    private fun recoverOnParenthesizedWordForPlatformTypes(offset: Int, word: String, consume: Boolean): Boolean {
        // 形如 Array<(out) Foo>! 或 (Mutable)List<Bar>! 的恢复
        if (lookahead(offset) == LPAR &&
            lookahead(offset + 1) == IDENTIFIER &&
            lookahead(offset + 2) == RPAR &&
            lookahead(offset + 3) == IDENTIFIER
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
                    error.error("Unexpected tokens")
                } else {
                    error.rollbackTo()
                }

                return true
            }
        }
        return false
    }

    private fun parseTypeArgumentList(): Boolean {
        if (!at(LT)) return false

        val list = mark()
        tryParseTypeArgumentList(TokenSet.EMPTY)
        list.done(TYPE_ARGUMENT_LIST)

        return true
    }

    private fun recoverOnPlatformTypeSuffix() {
        // 平台类型的恢复，遇到感叹号报错
        if (at(EXCL)) {
            val error = mark()
            advance() // EXCL
            error.error("Unexpected token")
        }
    }

    /**
     * functionType
     *   : (type ".")? "(" parameter{","}? ")" "->" type?
     *   ;
     */
    private fun parseFunctionType(functionType: PsiBuilder.Marker) {
        parseFunctionTypeContents(functionType).done(FUNCTION_TYPE)
    }

    private fun parseFunctionTypeContents(functionType: PsiBuilder.Marker): PsiBuilder.Marker {
        assert(_at(LPAR)) { tt()!! }

        parseValueParameterList(isFunctionTypeContents = true, typeRequired = true, recoverySet = TokenSet.EMPTY)

        expect(ARROW, "Expecting '->' to specify return type of a function type", TYPE_REF_FIRST)
        parseTypeRef()

        return functionType
    }

    /** 返回元组的类型数量 */
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
            error("Expecting type")
        }

        expect(RPAR, "Expecting ')'")

        return count
    }

    /** 解析 VArray 类型 */
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
                error("expected type parameters after 'VArray' keyword")
            }

            typeRefMarker.done(VARRAY_TYPE)
            return true
        }

        return false
    }

    /**
     * userType
     *   : simpleUserType{"."}
     *   ;
     *
     *   recovers on platform types:
     *    - Foo!
     *    - (Mutable)List<Foo>!
     *    - Array<(out) Foo>!
     */
    fun parseUserType(): Boolean {
        var isTypeArgumentList = false

        var userType = mark()

        var reference = mark()

        while (true) {
            recoverOnParenthesizedWordForPlatformTypes(0, "Mutable", consume = true)
            if (expect(
                    IDENTIFIER,
                    "Expecting type name",
                    TokenSet.orSet(
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
     * 解析类型引用
     */
    private fun parseTypeRefContents(extraRecoverySet: TokenSet): PsiBuilder.Marker {
        val typeRefMarker = mark()

        // 这里你可以继续完善类型引用的解析逻辑，目前是空实现

        return typeRefMarker
    }

    /**
     * 解析This类型
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
     * 解析多重声明名称 (SimpleName {","})
     */
    fun parseMultiDeclarationName(follow: TokenSet, recoverySet: TokenSet) {
        builder.disableNewlines()
        advance() // LPAR

        if (!atSet(follow)) {
            while (true) {
                when {
                    at(COMMA) -> errorAndAdvance("Expecting a name")
                    at(RPAR) -> { // For declaration similar to `val () = somethingCall()`
                        error("Expecting a name")
                        break
                    }

                    else -> {
                        val property = mark()

                        parseModifierList(COMMA_RPAR_COLON_EQ_SET)

                        expect(IDENTIFIER, "Expecting a name", recoverySet)

                        // 如果需要解析类型注解，可以取消注释
                        // if (at(COLON)) {
                        //     advance() // COLON
                        //     parseTypeRef(follow)
                        // }

                        property.done(DESTRUCTURING_DECLARATION_ENTRY)

                        if (!at(COMMA)) break
                        advance() // COMMA
                        if (at(RPAR)) break
                    }
                }
            }
        }

        expect(RPAR, "Expecting ')'", follow)
        builder.restoreNewlinesState()
    }

    /**
     * 解析类型参数列表
     */
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
            error("Expecting a '>'")
        } else {
            advance() // GT
        }
        builder.restoreNewlinesState()
        return atGT
    }



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
     * @param extraRecoverySet
     * @param isConstraint 是否为约束，约束没有问号,不解析userType
     */
    fun parseTypeRef(extraRecoverySet: TokenSet = TokenSet.EMPTY , isConstraint: Boolean = false) {
        val typeRefMarker = mark()

        if (!isConstraint) {
            parseTypeRefContents()
        } else {
            parseIdentifier()
        }

        typeRefMarker.done(TYPE_REFERENCE)
    }

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

    private fun parseTypeRefContents() {
        when {
            parseVArrayType() -> return
            parseThisType() -> return
            parseBasicType() -> return
            at(IDENTIFIER) -> parseUserType()
            at(LPAR) -> parseTupleOrFunctionType()
            at(QUEST) || at(SAFE_CALL) -> parseOptionType()
            else -> error("Expecting a type name, found '${builder.tokenText}'")
        }
    }

    enum class MacroType {
        MACRO_CALL,
        ANNOTATION,
    }

    enum class DeclarationParsingMode(
        val destructuringAllowed: Boolean,
        val accessorsAllowed: Boolean,
        val canBeEnumUsedAsSoftKeyword: Boolean
    ) {
        ALL(false, true, true),
        TOPLEVEL(false, true, true),
        MEMBER(false, true, true),
        MEMBER_OR_TOPLEVEL(false, true, true),
        LOCAL(true, false, false)
        // SCRIPT_TOPLEVEL(true, true, false) // 如需使用可以取消注释
    }

    enum class NameParsingMode {
        REQUIRED,
        ALLOWED,
        PROHIBITED
    }


}