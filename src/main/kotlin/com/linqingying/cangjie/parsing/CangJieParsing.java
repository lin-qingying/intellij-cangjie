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

package com.linqingying.cangjie.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.SyntaxTreeBuilder;
import com.intellij.lang.WhitespacesBinders;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.linqingying.cangjie.CjNodeTypes.*;
import static com.linqingying.cangjie.lexer.CjTokens.*;
import static com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes.CONSTRUCTOR_CALLEE;
import static com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes.END_SECONDARY_CONSTRUCTOR;

public class CangJieParsing extends AbstractCangJieParsing {
    public static final TokenSet PARAMETER_NAME_RECOVERY_SET = TokenSet.create(COLON, EQ, COMMA, RPAR);
    private static final TokenSet GT_COMMA_COLON_SET = TokenSet.create(GT, COMMA, COLON);
    private static final Logger LOG = Logger.getInstance(CangJieParsing.class);
    private static final TokenSet TOP_LEVEL_DECLARATION_FIRST = TokenSet.create(INTERFACE_KEYWORD, CLASS_KEYWORD, FUNC_KEYWORD, LET_KEYWORD, VAR_KEYWORD, CONST_KEYWORD, PACKAGE_KEYWORD);
    private static final TokenSet TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET = TokenSet.orSet(CangJieParsing.TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(SEMICOLON));
    private static final TokenSet LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET = TokenSet.orSet(TokenSet.create(LT, EQ, SEMICOLON), CangJieParsing.TOP_LEVEL_DECLARATION_FIRST);

    private static final TokenSet CLASS_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(LT, LPAR, COLON, LBRACE), CangJieParsing.TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(WHERE_KEYWORD, LPAR, COLON, LBRACE, GT);
    private static final TokenSet PACKAGE_NAME_RECOVERY_SET = TokenSet.create(DOT, EOL_OR_SEMICOLON);
    private static final TokenSet IMPORT_RECOVERY_SET = TokenSet.create(AS_KEYWORD, DOT, EOL_OR_SEMICOLON);
    private static final TokenSet TYPE_REF_FIRST = TokenSet.create(LBRACKET, IDENTIFIER, LPAR, HASH);
    private static final TokenSet LBRACE_RBRACE_TYPE_REF_FIRST_SET = TokenSet.orSet(TokenSet.create(LBRACE, RBRACE), CangJieParsing.TYPE_REF_FIRST);
    private static final TokenSet LTCOLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET = TokenSet.orSet(TokenSet.create(LTCOLON, COMMA, LBRACE, RBRACE), CangJieParsing.TYPE_REF_FIRST);
    private static final TokenSet RECEIVER_TYPE_TERMINATORS = TokenSet.create(DOT);
    private static final TokenSet VALUE_PARAMETER_FIRST = TokenSet.orSet(TokenSet.create(IDENTIFIER, LBRACKET, LET_KEYWORD, CONST_KEYWORD, VAR_KEYWORD), TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUNC_KEYWORD)));
    private static final TokenSet LAMBDA_VALUE_PARAMETER_FIRST = TokenSet.orSet(TokenSet.create(IDENTIFIER, LBRACKET), TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUNC_KEYWORD)));

    private static final TokenSet BLOCK_DOC_COMMENT_SET = TokenSet.create(BLOCK_COMMENT, DOC_COMMENT);
    private static final TokenSet SEMICOLON_SET = TokenSet.create(SEMICOLON);
    private static final TokenSet COMMA_COLON_GT_SET = TokenSet.create(COMMA, COLON, GT);
    private static final TokenSet IDENTIFIER_RBRACKET_LBRACKET_SET = TokenSet.create(IDENTIFIER, RBRACKET, LBRACKET);
    private static final TokenSet LBRACE_RBRACE_SET = TokenSet.create(LBRACE, RBRACE);
    private static final TokenSet COMMA_SEMICOLON_RBRACE_SET = TokenSet.create(COMMA, SEMICOLON, RBRACE);
    private static final TokenSet VALUE_ARGS_RECOVERY_SET = TokenSet.create(LBRACE, SEMICOLON, RPAR, EOL_OR_SEMICOLON, RBRACE);
    private static final TokenSet PROPERTY_NAME_FOLLOW_SET = TokenSet.create(COLON, EQ, LBRACE, RBRACE, SEMICOLON, LET_KEYWORD, VAR_KEYWORD, FUNC_KEYWORD, CLASS_KEYWORD);
    private static final TokenSet PROPERTY_NAME_FOLLOW_MULTI_DECLARATION_RECOVERY_SET = TokenSet.orSet(CangJieParsing.PROPERTY_NAME_FOLLOW_SET, CangJieParsing.PARAMETER_NAME_RECOVERY_SET);
    private static final TokenSet PROPERTY_NAME_FOLLOW_FUNCTION_OR_PROPERTY_RECOVERY_SET = TokenSet.orSet(CangJieParsing.PROPERTY_NAME_FOLLOW_SET, CangJieParsing.LBRACE_RBRACE_SET, CangJieParsing.TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet IDENTIFIER_EQ_COLON_SEMICOLON_SET = TokenSet.create(IDENTIFIER, EQ, COLON, SEMICOLON);
    private static final TokenSet COMMA_RPAR_COLON_EQ_SET = TokenSet.create(COMMA, RPAR, COLON, EQ);
    private static final TokenSet ACCESSOR_FIRST_OR_PROPERTY_END = TokenSet.orSet(MODIFIER_KEYWORDS, TokenSet.create(GET_KEYWORD, SET_KEYWORD, EOL_OR_SEMICOLON, RBRACE));
    private static final TokenSet RPAR_IDENTIFIER_COLON_LBRACE_EQ_SET = TokenSet.create(RPAR, IDENTIFIER, COLON, LBRACE, EQ);
    private static final TokenSet COMMA_COLON_RPAR_SET = TokenSet.create(COMMA, COLON, RPAR);
    private static final TokenSet RPAR_COLON_LBRACE_EQ_SET = TokenSet.create(RPAR, COLON, LBRACE, EQ);
    private static final TokenSet LBRACKET_LBRACE_RBRACE_LPAR_SET = TokenSet.create(LBRACKET, LBRACE, RBRACE, LPAR);
    private static final TokenSet FUNCTION_NAME_FOLLOW_SET = TokenSet.create(LT, LPAR, RPAR, COLON, EQ);
    private static final TokenSet FUNCTION_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(LT, LPAR, RPAR, COLON, EQ), CangJieParsing.LBRACE_RBRACE_SET, CangJieParsing.TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet VALUE_PARAMETERS_FOLLOW_SET = TokenSet.create(EQ, LBRACE, RBRACE, SEMICOLON, RPAR);
    private static final TokenSet LPAR_VALUE_PARAMETERS_FOLLOW_SET = TokenSet.orSet(TokenSet.create(LPAR), CangJieParsing.VALUE_PARAMETERS_FOLLOW_SET);
    private static final TokenSet LPAR_LBRACE_COLON_CONSTRUCTOR_KEYWORD_SET = TokenSet.create(LPAR, LBRACE, COLON, INIT_KEYWORD);
    private static final TokenSet definitelyOutOfReceiverSet = TokenSet.orSet(TokenSet.create(EQ, COLON, LBRACE, RBRACE), CangJieParsing.TOP_LEVEL_DECLARATION_FIRST);
    private final static TokenSet EOL_OR_SEMICOLON_RBRACE_SET = TokenSet.create(EOL_OR_SEMICOLON, RBRACE);
    private final static TokenSet CLASS_INTERFACE_SET = TokenSet.create(CLASS_KEYWORD, INTERFACE_KEYWORD);
    private final static TokenSet CLASS_INTERFACE_STRUCT_ENUM_EXTEND_SET = TokenSet.create(CLASS_KEYWORD, INTERFACE_KEYWORD, STRUCT_KEYWORD, ENUM_KEYWORD, EXTEND_KEYWORD);

    private final static TokenSet IDENTIFIER_DOT_MUL_SET = TokenSet.create(IDENTIFIER, DOT, MUL);


    //    包的访问修饰符
    private final static TokenSet PACKAGE_ACCESS_MODIFIER_SET = TokenSet.create(PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD);
    //导入语句访问修饰符
    private final static TokenSet IMPORT_ACCESS_MODIFIER_SET = TokenSet.create(PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD);


    private final static TokenSet DOT_MUL_SET = TokenSet.create(DOT, MUL);
    private static final TokenSet DECLARATION_FIRST =
            TokenSet.orSet(CangJieParsing.TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(INIT_KEYWORD, GET_KEYWORD, SET_KEYWORD));

    private final static TokenSet MUT_PROP_SET = TokenSet.create(MUT_KEYWORD, PROP_KEYWORD);
    private final CangJieExpressionParsing myExpressionParsing;
    private final LastBefore lastDotAfterReceiverNotLParPattern =
            new LastBefore(new AtSet(CangJieParsing.RECEIVER_TYPE_TERMINATORS), new AbstractTokenStreamPredicate() {
                @Override
                public boolean matching(final boolean topLevel) {
                    if (topLevel && (CangJieParsing.this.atSet(CangJieParsing.definitelyOutOfReceiverSet) || CangJieParsing.this.at(LPAR))) return true;
                    if (topLevel && CangJieParsing.this.at(IDENTIFIER)) {
                        final IElementType lookahead = CangJieParsing.this.lookahead(1);
                        return lookahead != LT && lookahead != DOT && lookahead != QUEST;
                    }
                    return false;
                }
            });
    private final FirstBefore lastDotAfterReceiverLParPattern =
            new FirstBefore(new AtSet(CangJieParsing.RECEIVER_TYPE_TERMINATORS), new AbstractTokenStreamPredicate() {
                @Override
                public boolean matching(final boolean topLevel) {
                    if (topLevel && CangJieParsing.this.atSet(CangJieParsing.definitelyOutOfReceiverSet)) {
                        return true;
                    }
                    return topLevel && !CangJieParsing.this.at(QUEST) && !CangJieParsing.this.at(LPAR) && !CangJieParsing.this.at(RPAR);
                }
            });

    private CangJieParsing(final SemanticWhitespaceAwarePsiBuilder builder, final boolean isTopLevel, final boolean isLazy) {
        super(builder, isLazy);

        this.myExpressionParsing =
                isTopLevel ? new CangJieExpressionParsing(builder, this, isLazy) : new CangJieExpressionParsing(builder, this, isLazy) {


                    @Override
                    protected @NotNull CangJieParsing create(final SemanticWhitespaceAwarePsiBuilder builder) {
                        return CangJieParsing.createForByClause(builder, this.isLazy);
                    }
                };


    }

    //    如果是声明文件，则不需要函数体

    private static CangJieParsing createForByClause(final SemanticWhitespaceAwarePsiBuilder builder, final boolean isLazy) {
        return new CangJieParsing(new SemanticWhitespaceAwarePsiBuilderForByClause(builder), false, isLazy);
    }

    static CangJieParsing createForTopLevel(final SemanticWhitespaceAwarePsiBuilder builder) {
        return new CangJieParsing(builder, true, true);
    }

    static CangJieParsing createForTopLevelNonLazy(final SemanticWhitespaceAwarePsiBuilder builder) {
        return new CangJieParsing(builder, true, false);
    }

    public CangJieExpressionParsing getExpressionParsing() {
        return this.myExpressionParsing;
    }

    void parseTypeRef() {
        this.parseTypeRef(TokenSet.EMPTY, false);
    }

    void parseTypeRefWithoutIntersections() {
        this.parseTypeRef(TokenSet.EMPTY);
    }

    void parseTest() {
//        SyntaxTreeBuilder.Marker a = mark();
        this.advance();
//        a.done(IMPORT_LIST);
    }

    @Override
    protected CangJieParsing create(final SemanticWhitespaceAwarePsiBuilder builder) {
        return CangJieParsing.createForTopLevel(builder);
    }

    public void advanceBalancedBlock() {

        int braceCount = 1;
        while (!this.eof()) {
            if (this._at(LBRACE)) {
                braceCount++;
            } else if (this._at(RBRACE)) {
                braceCount--;
            }

            this.advance();

            if (0 == braceCount) {
                break;
            }
        }
    }

    private void parseThisOrSuper() {
        assert this._at(THIS_KEYWORD) || this._at(SUPER_KEYWORD);
        final PsiBuilder.Marker mark = this.mark();

        this.advance(); // THIS_KEYWORD | SUPER_KEYWORD

        mark.done(CONSTRUCTOR_DELEGATION_REFERENCE);
    }

    private void parseInitFunctionBlock() {
        final PsiBuilder.Marker lazyBlock = this.mark();

        this.myBuilder.enableNewlines();

//        恢复  init() xxxxxxx {}

        this.expect(LBRACE, "Expecting '{'  ");

        final PsiBuilder.Marker delegationCall = this.mark();
        if ((this.at(THIS_KEYWORD) || this.at(SUPER_KEYWORD)) && this.rawLookup(1) == LPAR) {
            this.parseThisOrSuper();
            this.myExpressionParsing.parseValueArgumentList();
            delegationCall.done(CONSTRUCTOR_DELEGATION_CALL);

        } else {
            this.mark().done(CONSTRUCTOR_DELEGATION_REFERENCE);
            delegationCall.done(CONSTRUCTOR_DELEGATION_CALL);
        }


        this.myExpressionParsing.parseStatements();
        this.expect(RBRACE, "Expecting '}'");


        this.myBuilder.restoreNewlinesState();


        lazyBlock.done(INIT_BLOCK);

    }

    private void parseBlock(final boolean collapse) {
        final PsiBuilder.Marker lazyBlock = this.mark();

        this.myBuilder.enableNewlines();

        final boolean hasOpeningBrace = this.expect(LBRACE, "Expecting '{'  ");
        final boolean canCollapse = collapse && hasOpeningBrace && this.isLazy;


        if (canCollapse) {
            this.advanceBalancedBlock();
        } else {
            this.myExpressionParsing.parseStatements();
            this.expect(RBRACE, "Expecting '}'");
        }

        this.myBuilder.restoreNewlinesState();

        if (canCollapse) {
            lazyBlock.collapse(BLOCK);
        } else {
            lazyBlock.done(BLOCK);
        }
    }


    /*
     * block
     *   : "{" (expressions)* "}"
     *   ;
     */
    void parseBlock() {
        this.parseBlock(true);
    }

    void parseBlockExpression() {
        this.parseBlock(false);
    }

    /*
     *preamble
     *  : fileAnnotationList? packageDirective?
     *  ;
     */
    private void parsePreamble() {
        final PsiBuilder.Marker firstEntry = this.mark();

        /*
         * TODO fileAnnotationList  Ko
         *
         *
         *
         * 文档注释
         *   : fileAnnotations*
         */

        /*
         * packageDirective  包声明
         *   : modifiers "package" SimpleName{"."} SEMI?
         *   ;
         */
        PsiBuilder.Marker packageDirective = this.mark();


//        是否有修饰符
        boolean istPackageAccessModifier = false;
        if (this.atSet(CangJieParsing.PACKAGE_ACCESS_MODIFIER_SET) && (this.lookahead(1) == MACRO_KEYWORD || this.lookahead(1) == PACKAGE_KEYWORD)) {
            this.advance(); //修饰符
            istPackageAccessModifier = true;
        }
        if (this.at(MACRO_KEYWORD)) {
            this.advance(); // MARCO_KEYWORD 宏声明
            istPackageAccessModifier = true;

        }

        if (this.at(PACKAGE_KEYWORD)) {


            if (this.at(PACKAGE_KEYWORD)) {
                this.advance(); // PACKAGE_KEYWORD
            } else if (istPackageAccessModifier) {
                this.error("Expecting package keyword");
            }


            //TODO 处理包名
            this.parsePackageName();

            firstEntry.drop();

            this.consumeIf(SEMICOLON);

            packageDirective.done(PACKAGE_DIRECTIVE);
        } else {
            //当忽略Package指令时，我们不应该在文件开头报告非文件批注的错误。
            //因此，我们回滚解析位置，重新解析文件注释列表，非文件注释没有上报错误。
            firstEntry.rollbackTo();

            //TODO 解析文件注解列表
//            parseFileAnnotationList(FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED);
            packageDirective = this.mark();
            packageDirective.done(PACKAGE_DIRECTIVE);
            //需要跳过除Shebang注释之外的所有内容，以允许将文件开头的注释绑定到第一个声明。
            packageDirective.setCustomEdgeTokenBinders(BindFirstShebangWithWhitespaceOnly.INSTANCE, null);


//          TODO   仓颉0.53.4 更新 位于包中的文件必须要包名
//            但是单文件可以没有

        }

        this.parseImportDirectives();
    }

    private void parseImportDirectives() {
        final PsiBuilder.Marker importList = this.mark();

        if (!(this.at(IMPORT_KEYWORD) || this.atSet(CangJieParsing.IMPORT_ACCESS_MODIFIER_SET) && (this.lookahead(1) == IMPORT_KEYWORD))) {
            // this is necessary to allow comments at the start of the file to be bound to the first declaration
            importList.setCustomEdgeTokenBinders(DoNotBindAnything.INSTANCE, null);
        }
        while (this.at(IMPORT_KEYWORD) || this.atSet(CangJieParsing.IMPORT_ACCESS_MODIFIER_SET) && (this.lookahead(1) == IMPORT_KEYWORD)) {
            this.parseImportDirective();
        }
        importList.done(IMPORT_LIST);
    }

    private boolean closeImportWithErrorIfNewline(
            @Nullable final PsiBuilder.Marker importDirective, @Nullable final PsiBuilder.Marker importAlias, final String errorMessage
    ) {
        if (this.myBuilder.newlineBeforeCurrentToken()) {
            if (null != importAlias) {
                importAlias.done(IMPORT_ALIAS);
            }
            this.error(errorMessage);
            if (null != importDirective) {
                importDirective.done(IMPORT_DIRECTIVE);
            }
            return true;
        }
        return false;
    }


    /**
     * 处理Import关键字后的单个导入项
     * : "import"
     * : SimpleName{"."} ("." "*" )? | ("as" SimpleName{"."} ("." "*"))? SEMI?
     */
    private IElementType parseImportDirectiveItem(final boolean isTopLevel, final boolean isCreateMark) {


        PsiBuilder.Marker importDirectiveItem = null;
        if (isCreateMark) {
            importDirectiveItem = this.mark();
        }


        if (!this.at(IDENTIFIER)) {

            this.error("expected a package name after '.' in qualified name, found '" + this.myBuilder.getTokenText() + "'");

            if (null != importDirectiveItem) {
                importDirectiveItem.done(IMPORT_DIRECTIVE);
            }

            this.consumeIf(SEMICOLON);
            return IMPORT_DIRECTIVE;
        }

        PsiBuilder.Marker qualifiedName = this.mark();
        PsiBuilder.Marker reference = this.mark();
        this.advance(); // IDENTIFIER
        reference.done(REFERENCE_EXPRESSION);

        boolean isMulitImport = false;
        boolean isParseDot = false;

        while (this.at(DOT) && this.lookahead(1) != MUL) {
            this.advance(); // DOT

//            同一个包多个导入项
            if (this.at(LBRACE) && isTopLevel) {
                isMulitImport = true;
                this.advance();
                do {
                    this.expect(COMMA);
                    this.parseImportDirectiveItem(false, true);

                } while (this.at(COMMA));

                this.expect(RBRACE, "Expecting '}'");


            } else {


                isParseDot = true;
                if (this.closeImportWithErrorIfNewline(importDirectiveItem, null, "Import must be placed on a single line")) {
                    qualifiedName.drop();
                    return IMPORT_DIRECTIVE;
                }

                reference = this.mark();
                if (this.expect(IDENTIFIER, "Qualified name must be a '.'-separated identifier list", CangJieParsing.IMPORT_RECOVERY_SET)) {
                    reference.done(REFERENCE_EXPRESSION);
                } else {
                    reference.drop();
                }

                final PsiBuilder.Marker precede = qualifiedName.precede();
                qualifiedName.done(DOT_QUALIFIED_EXPRESSION);
                qualifiedName = precede;
            }


        }
        qualifiedName.drop();

//        if (isTopLevel || !isParseDot) {

        if (this.at(DOT)) {
            this.advance(); // DOT
            assert this._at(MUL);
            this.advance(); // MUL
            if (this.at(AS_KEYWORD)) {
                this.errorAndAdvance("Aliases are not allowed for all imports");
//                    PsiBuilder.Marker as = mark();
//                    advance(); // AS_KEYWORD
//                    if (closeImportWithErrorIfNewline(importDirectiveItem, null, "Expecting identifier")) {
//                        as.drop();
//                        return;
//                    }
//                    consumeIf(IDENTIFIER);
////                as.done(IMPORT_ALIAS);
//
//                    if (!match(DOT, MUL)) {
////                    as.precede().error("The alias name should contain '.*' suffix after import-all");
//
//
////                    TODO 如果使用了LSP psi会重复报错一次
//                        error("The alias name should contain '.*' suffix after import-all");
//                    }
////                else {
//                    as.done(IMPORT_ALIAS);
////                }
//
            }
        } else if (this.at(AS_KEYWORD)) {
            final PsiBuilder.Marker alias = this.mark();
            this.advance(); // AS_KEYWORD
            if (this.closeImportWithErrorIfNewline(importDirectiveItem, alias, "Expecting identifier")) {
                return IMPORT_DIRECTIVE;
            }
            this.expect(IDENTIFIER, "Expecting identifier", CangJieParsing.SEMICOLON_SET);
            alias.done(IMPORT_ALIAS);
        }

        if (null != importDirectiveItem) {
            importDirectiveItem.done(IMPORT_DIRECTIVE);
        }

        if (isMulitImport) {
            return MULIT_IMPORT_DIRECTIVE;
        }

        return IMPORT_DIRECTIVE;
    }

    /*
     * import
     *   ;  "from" SimpleName
     *   : "import" SimpleName{"."} ("." "*" | "as" SimpleName)? SEMI?
     *   ;
     */
    private void parseImportDirective() {


        assert this._at(IMPORT_KEYWORD) || this._atSet(CangJieParsing.IMPORT_ACCESS_MODIFIER_SET);

        IElementType doneType = IMPORT_DIRECTIVE;

        final PsiBuilder.Marker importDirective = this.mark();


        if (this._atSet(CangJieParsing.IMPORT_ACCESS_MODIFIER_SET)) {
            this.advance(); //PUBLIC_KEYWORD
        }


        if (!this.at(IMPORT_KEYWORD)) {

            this.error("Expecting 'import' keyword");
            importDirective.done(doneType);
            return;
        }


        this.advance(); // IMPORT_KEYWORD

        if (this.closeImportWithErrorIfNewline(importDirective, null, "Expecting qualified name")) {
            return;
        }

//        if (at(LBRACE)) {
//            advance();
//            parseImportDirectiveItem(false, true);
//
////                多个导入语句
//
//
//            while (at(COMMA)) {
//                advance();
//                parseImportDirectiveItem(false, true);
//            }
//
//            expect(RBRACE, "Expecting '}'");
//            doneType = MULIT_IMPORT_DIRECTIVE1;
//
//        } else {
        doneType = this.parseImportDirectiveItem(true, false);
//        }


        this.consumeIf(SEMICOLON);
        importDirective.done(doneType);
        importDirective.setCustomEdgeTokenBinders(null, TrailingCommentsBinder.INSTANCE);

    }

    /* SimpleName{"."} */
    private void parsePackageName() {

        PsiBuilder.Marker qualifiedExpression = this.mark();
        boolean simpleName = true;
        while (true) {
            if (this.myBuilder.newlineBeforeCurrentToken()) {
                this.errorWithRecovery("Package name must be a '.'-separated identifier list placed on a single line",
                        CangJieParsing.PACKAGE_NAME_RECOVERY_SET);
                break;
            }

            if (this.at(DOT)) {
                this.advance(); // DOT
                qualifiedExpression.error("Package name must be a '.'-separated identifier list");
                qualifiedExpression = this.mark();
                continue;
            }

            final PsiBuilder.Marker nsName = this.mark();
            final boolean simpleNameFound = this.expect(IDENTIFIER, "Package name must be a '.'-separated identifier list", CangJieParsing.PACKAGE_NAME_RECOVERY_SET);
            if (simpleNameFound) {
                nsName.done(REFERENCE_EXPRESSION);
            } else {
                nsName.drop();
            }

            if (!simpleName) {
                final PsiBuilder.Marker precedingMarker = qualifiedExpression.precede();
                qualifiedExpression.done(DOT_QUALIFIED_EXPRESSION);
                qualifiedExpression = precedingMarker;
            }

            if (this.at(DOT)) {
                this.advance(); // DOT

                if (simpleName && !simpleNameFound) {
                    qualifiedExpression.drop();
                    qualifiedExpression = this.mark();
                } else {
                    simpleName = false;
                }
            } else {
                break;
            }
        }
        qualifiedExpression.drop();
    }

    void parseScript() {
        final PsiBuilder.Marker fileMarker = this.mark();
        fileMarker.done(CJ_FILE);

    }

//    public void parseDeclarationsFile() {
//        isDeclarationsFile = true;
//        parseFile();
//    }
//
//
//    void parseCjFile() {
//        isDeclarationsFile = false;
//        parseFile();
//    }

    //入口
    void parseFile() {
        final PsiBuilder.Marker fileMarker = this.mark();

        //处理开头  package
        this.parsePreamble();

//        处理声明式语句
        while (!this.eof()) {
            this.parseTopLevelDeclaration();
        }
        this.checkUnclosedBlockComment();

        fileMarker.done(CJ_FILE);
    }

    void parseLspFile() {
        final PsiBuilder.Marker fileMarker = this.mark();
//        将所有节点全都读取
        while (!this.eof()) {
            this.advance();
        }

        fileMarker.done(CJ_FILE);


    }

    private void checkUnclosedBlockComment() {
        if (CangJieParsing.BLOCK_DOC_COMMENT_SET.contains(this.myBuilder.rawLookup(-1))) {
            final int startOffset = this.myBuilder.rawTokenTypeStart(-1);
            final int endOffset = this.myBuilder.rawTokenTypeStart(0);
            final CharSequence tokenChars = this.myBuilder.getOriginalText().subSequence(startOffset, endOffset);
            if (!(2 < tokenChars.length() && "*/".equals(tokenChars.subSequence(tokenChars.length() - 2, tokenChars.length()).toString()))) {
                final PsiBuilder.Marker marker = this.myBuilder.mark();
                marker.error("Unclosed comment");
                marker.setCustomEdgeTokenBinders(WhitespacesBinders.GREEDY_RIGHT_BINDER, null);
            }
        }
    }

    private void parseTopLevelDeclaration() {
        this.parseTopLevelDeclaration(false);
    }

    /*
     * 顶层声明语句
     *   : function
     *   : class enum interface struct
     */
    private void parseTopLevelDeclaration(final boolean parseMacro) {
        if (this.at(SEMICOLON)) {
            this.advance(); // SEMICOLON
            return;
        }


        final PsiBuilder.Marker decl = this.mark();

//如果有导入语句

        if ((this.at(PUBLIC_KEYWORD) && this.lookahead(1) == IMPORT_KEYWORD)) {
//            error("imports are only allowed in the beginning of file");
            this.parseImportDirectives();
            decl.drop();

            return;
        }


        final ModifierDetector detector = new ModifierDetector();

        this.parseModifierList(detector, TokenSet.EMPTY, parseMacro);
        final IElementType declType = this.parseCommonDeclaration(detector, NameParsingMode.REQUIRED, DeclarationParsingMode.TOPLEVEL);

 /*       if (declType == ANNOTATION_ENTRY) {

            decl.rollbackTo(); //返回给文档流并重新解析
//            decl.drop();

//           应该为注解，加入到修饰符中并重新解析声明
            parseTopLevelDeclaration(true);

        } else*/
        if (null == declType) {

            this.errorAndAdvance("Expecting a top level declaration"); //期待一个顶层声明语句
//            decl.error("Expecting a top level declaration");
            decl.drop();
        } else {
            AbstractCangJieParsing.closeDeclarationWithCommentBinders(decl, declType, true);
        }


    }

    private boolean tryParseModifier(
            @Nullable final Consumer<IElementType> tokenConsumer, @NotNull final TokenSet noModifiersBefore, @NotNull final TokenSet modifierKeywords
    ) {
        final PsiBuilder.Marker marker = this.mark();

        if (this.atSet(modifierKeywords)) {
            final IElementType lookahead = this.lookahead(1);

            if (this.at(FUNC_KEYWORD) && lookahead != INTERFACE_KEYWORD) {
                marker.rollbackTo();
                return false;
            }

            if (null != lookahead && !noModifiersBefore.contains(lookahead)) {
                final IElementType tt = this.tt();
                if (null != tokenConsumer) {
                    tokenConsumer.consume(tt);
                }
                this.advance(); // MODIFIER
                marker.collapse(tt);
                return true;
            }
        } else if (this.at(CONST_KEYWORD) && /* lookahead(1) != IDENTIFIER && */this.lookahead(2) != EQ && this.lookahead(2) != COLON /* && (lookahead(2) == FUNC_KEYWORD || lookahead(3) == FUNC_KEYWORD || lookahead(4) == FUNC_KEYWORD)*/) {


//            处理特殊的const修饰的
            this.advance(); // MODIFIER
            if (null != tokenConsumer) {
                tokenConsumer.consume(CONST_KEYWORD);
            }
            marker.collapse(CONST_KEYWORD);
            return true;

        } else if (this.at(UNSAFE_KEYWORD) && this.lookahead(1) != LBRACE) {
            this.advance();

            if (null != tokenConsumer) {
                tokenConsumer.consume(UNSAFE_KEYWORD);
            }
            marker.collapse(UNSAFE_KEYWORD);

            return true;

        } else if (this.at(FOREIGN_KEYWORD) && this.lookahead(1) != LBRACE) {
            this.advance();
            if (null != tokenConsumer) {
                tokenConsumer.consume(FOREIGN_KEYWORD);
            }
            marker.collapse(FOREIGN_KEYWORD);

            return true;

        }
        marker.rollbackTo();
        return false;
    }

    private boolean doParseModifierListBody(
            @Nullable final Consumer<IElementType> tokenConsumer,
            @NotNull final TokenSet modifierKeywords,

            @NotNull final TokenSet noModifiersBefore

    ) {
        return this.doParseModifierListBody(tokenConsumer, modifierKeywords, noModifiersBefore, false);
    }

    private boolean doParseModifierListBody(
            @Nullable final Consumer<IElementType> tokenConsumer,
            @NotNull final TokenSet modifierKeywords,

            @NotNull final TokenSet noModifiersBefore,
            final boolean isParseMacro
    ) {

        boolean empty = true;
        PsiBuilder.Marker beforeAnnotationMarker;

        while (!this.eof()) {

     /*       if (at(AT) && isParseMacro) {

                beforeAnnotationMarker = mark();
                IElementType type = parseAnnotation(null);

                if (type == null || type == MACRO_EXPRESSION) {
//                    beforeAnnotationMarker.drop();
                    beforeAnnotationMarker.rollbackTo();
                    break;
                } else {
                    tokenConsumer.consume(type);
                    beforeAnnotationMarker.done(type);
                }


            } else */
            if (!this.tryParseModifier(tokenConsumer, noModifiersBefore, modifierKeywords)) {
                // modifier advanced
                break;
            }

            empty = false;
        }

        return empty;
    }


//    public IElementType parseAnnotation(ModifierDetector detector) {
//        return parseAnnotation(detector, MacroType.MACRO_CALL);
//    }

    void parseLambdaExpression() {
        this.myExpressionParsing.parseFunctionLiteral(/* preferBlock = */ false, /* collapse = */false, false);
    }


    /*TODO 注解与宏
     *  注解与宏
     * annotation
     *   : "@" (annotationUseSiteTarget ":")? unescapedAnnotation
     *   ;
     *
     * unescapedAnnotation
     *   : SimpleName{"."} typeArguments? valueArguments?
     *   ;
     */
    public IElementType parseAnnotation(final ModifierDetector detector) {
//        detector们没有修饰符
//        该宏调用没有括号     解析为注解

//        如果没有修饰符,根据是否有括号判断

        assert this._at(AT);
        final IElementType nextRawToken = this.lookahead(1);


        int modifierSize = 0;
        if (null != detector) {
            modifierSize = detector.getSize();
        }
//        PsiBuilder.Marker annotation = mark();
        if (nextRawToken == IDENTIFIER) {
            this.advance(); // AT
            final PsiBuilder.Marker reference = this.mark();
            final PsiBuilder.Marker typeReference = this.mark();
            this.parseUserType();
            typeReference.done(TYPE_REFERENCE);
            reference.done(CONSTRUCTOR_CALLEE);
//            宏属性
            if (this.at(LBRACKET)) {
                this.myExpressionParsing.parseValueArgumentList(LBRACKET, RBRACKET);
            }
        } else {
            this.errorAndAdvance("Expected annotation identifier after '@'", 1); // AT @
//            annotation.drop();
            return null;
        }
//        if (modifierSize > 0) {
////            处理宏调用
//            return MACRO_EXPRESSION;
//        }else {
//            return ANNOTATION_ENTRY;
//        }
        if (this.at(LPAR)) {
            //            TODO 处理宏调用
            this.advance();
//          该语句应该是宏调用表达式，而非注解
            if (this.at(RPAR)) {
//                直接返回
                this.advance();
            } else {
                while (!this.eof()) {
                    if (this.at(COMMA)) {
                        this.advance();
                    }
//                    parseTopLevelDeclaration();
                    this.myExpressionParsing.parseStatementByScope(DeclarationParsingMode.ALL);
                    if (this.at(RPAR)) {
                        break;
                    }
//                if (at(AT)) {
//                    parseTopLevelDeclaration();
//                } else {
////                    TODO 其他token令牌
//                    advance();
//                }
//                if (at(RPAR)) {
//                    if (lookahead(1) == COMMA) {
//                        advance();
//                    } else {
//                        break;
//                    }
//                }
                }
                this.expect(RPAR, "expected ')'");
            }
//            parseMacroInputExprWithParens();
            return MACRO_EXPRESSION;
        } else {
//            return ANNOTATION_ENTRY;
            if (0 < modifierSize) {
                this.error("Should call (..) for macros");

                return MACRO_EXPRESSION;
            } else {
//                error("expected declaration, found '" + myBuilder.getTokenText() + "'");
                return ANNOTATION_ENTRY;
            }
        }
//        annotation.done(ANNOTATION_ENTRY);
//        return ANNOTATION_ENTRY;
//        return true;
    }


    boolean parseModifierList(@Nullable final Consumer<IElementType> tokenConsumer, @NotNull final TokenSet noModifiersBefore) {
        return this.parseModifierList(tokenConsumer, noModifiersBefore, false);

    }

    /**
     * (modifier )*
     * <p>
     * 如果不为空，则将修饰符(非批注)馈送到传递的使用者
     *
     * @param noModifiersBefore 是一个令牌集，其中包含指示何时满足这些元素的元素。
     *                          必须将前一个令牌解析为标识符，而不是修饰符
     */
    boolean parseModifierList(@Nullable final Consumer<IElementType> tokenConsumer, @NotNull final TokenSet noModifiersBefore, final boolean isParseMacro) {


        return this.doParseModifierList(tokenConsumer, MODIFIER_KEYWORDS, noModifiersBefore, isParseMacro);
    }

    private boolean doParseModifierList(
            @Nullable final Consumer<IElementType> tokenConsumer,
            @NotNull final TokenSet modifierKeywords,

            @NotNull final TokenSet noModifiersBefore
    ) {
        return this.doParseModifierList(tokenConsumer, modifierKeywords, noModifiersBefore, false);
    }

    private boolean doParseModifierList(
            @Nullable final Consumer<IElementType> tokenConsumer,
            @NotNull final TokenSet modifierKeywords,

            @NotNull final TokenSet noModifiersBefore,
            final boolean isParseMacro
    ) {
        final PsiBuilder.Marker list = this.mark();

        final boolean empty = this.doParseModifierListBody(tokenConsumer, modifierKeywords, noModifiersBefore, isParseMacro);

        if (empty) {
            list.drop();
        } else {
            list.done(MODIFIER_LIST);
        }
        return !empty;
    }

    private IElementType parseClassCommonDeclaration(final Integer tokenId, final ModifierDetector classdetector, final ModifierDetector detector) {
        //init func let|var prop


        return switch (this.getTokenId()) {
            case AT_Id -> this.myExpressionParsing.parseMacroExpression(true);
//                    parseAnnotation(null);
            case FUNC_KEYWORD_Id ->
                    null != tokenId ? INTERFACE_KEYWORD_Id == tokenId ? this.parseFunction(true, classdetector, detector, tokenId) : this.parseFunction(classdetector, detector, tokenId) : this.parseFunction(detector, tokenId);


//                    tokenId != null && (tokenId == INTERFACE_KEYWORD_Id || tokenId == EXTEND_KEYWORD_Id) ?  parseFunction(true,classdetector, detector):parseFunction( ) ;
            case PROP_KEYWORD_Id ->
                    null != tokenId && (INTERFACE_KEYWORD_Id == tokenId /*|| tokenId == EXTEND_KEYWORD_Id*/) ? this.parseProperty(true, classdetector, detector) : this.parseProperty(classdetector, detector);
            case LET_KEYWORD_Id, VAR_KEYWORD_Id, CONST_KEYWORD_Id -> this.parseVariable(classdetector, DeclarationParsingMode.MEMBER);
            default -> null;
        };
    }

    private IElementType parseClassInitializer() {

        return null;
    }

    private boolean parsePropertyDelegateOrAssignment() {
        if (this.at(EQ)) {
            this.advance(); // EQ
            this.myExpressionParsing.parseExpression();
            return true;
        }

        return false;
    }

    private int lastDotAfterReceiver() {
        final AbstractTokenStreamPattern pattern = this.at(LPAR) ? this.lastDotAfterReceiverLParPattern : this.lastDotAfterReceiverNotLParPattern;
        pattern.reset();
        return this.matchTokenStreamPredicate(pattern);
    }

    private boolean parseReceiverType(final String title, final TokenSet nameFollow) {

        final int lastDot = this.lastDotAfterReceiver();
        final boolean receiverPresent = -1 != lastDot;


        if (!receiverPresent) return false;

        this.createTruncatedBuilder(lastDot).parseTypeRef();

        if (this.atSet(CangJieParsing.RECEIVER_TYPE_TERMINATORS)) {
            this.advance(); // expectation
        } else {
            this.errorWithRecovery("Expecting '.' before a " + title + " name", nameFollow);
        }
        return true;
    }

    public IElementType parseVariable(final ModifierDetector classdetector) {
        return this.parseVariable(classdetector, null);
    }

    /*
     * variableDeclarationEntry
     *   : SimpleName (":" ('?')?type)?
     *   ;
     *
     * property
     *   : modifiers ("let" | "var" | "const")
     *   ;
     */
    public IElementType parseVariable(final ModifierDetector classdetector, @Nullable final DeclarationParsingMode declarationParsingMode) {
        assert (this.at(LET_KEYWORD) || this.at(VAR_KEYWORD) || this.at(CONST_KEYWORD));
        this.advance();


//        if (at(LPAR)) {
//            //(标识符 ',' 标识符 {',' 标识符})
////            PsiBuilder.Marker tuple = mark();
//            advance();
//
//            expect(IDENTIFIER, "Expecting identifier");
//
//            expect(COMMA, "1-element tuple pattern is not allowed,Expecting ','");
//
//            expect(IDENTIFIER, "Expecting identifier");
//            while (at(COMMA)) {
//                advance(); // COMMA
//                expect(IDENTIFIER, "Expecting identifier");
//            }
//
//
//            expect(RPAR, "Expecting ')'");
//
//        } else {
//            parseIdentifierByTitle("property", PROPERTY_NAME_FOLLOW_SET, true);
//
//        }
        if (/*declarationParsingMode == DeclarationParsingMode.TOPLEVEL ||*/ DeclarationParsingMode.MEMBER == declarationParsingMode) {
            this.parseIdentifierByTitle("variable", CangJieParsing.PROPERTY_NAME_FOLLOW_SET, true);
        } else {
            this.myExpressionParsing.parsePattern(new PatternConfig(true), Pattern.Wildcard.INSTANCE, Pattern.Binding.INSTANCE, Pattern.Tuple.INSTANCE, Pattern.Enum.INSTANCE);

        }

        boolean noTypeReference = true;

        //类型 (:type)可以没有，但是默认值必须有

        if (this.at(COLON)) {
            this.advance(); // COLON
            noTypeReference = false;

            this.parseTypeRef();

        }


        if (this.at(EQ)) {
            this.advance(); // COLON

            //处理表达式
//myExpressionParsing.test();
            this.myExpressionParsing.parseExpression();
        } else if (noTypeReference) {
            this.errorAndAdvance("variable in top-level scope must be initialized");
        }

//        if (!parsePropertyDelegateOrAssignment() && isNameOnTheNextLine && noTypeReference && !receiverTypeDeclared) {
//
//            beforeName.rollbackTo();
//            error("Expecting variable name or receiver type");
//            return VARIABLE;
//        }

//        beforeName.drop();
//        consumeIf(SEMICOLON);
        return VARIABLE;

    }

    void parseExpressionCodeFragment() {
        final PsiBuilder.Marker marker = this.mark();
        this.myExpressionParsing.parseExpression();

        this.checkForUnexpectedSymbols();

        marker.done(EXPRESSION_CODE_FRAGMENT);
    }

    void parseBlockCodeFragment() {
        final PsiBuilder.Marker marker = this.mark();
        final PsiBuilder.Marker blockMarker = this.mark();

        if (this.at(PACKAGE_KEYWORD) || this.at(IMPORT_KEYWORD)) {
            final PsiBuilder.Marker err = this.mark();
            this.parsePreamble();
            err.error("Package directive and imports are forbidden in code fragments");
        }

        this.myExpressionParsing.parseStatements();

        this.checkForUnexpectedSymbols();

        blockMarker.done(BLOCK);
        marker.done(BLOCK_CODE_FRAGMENT);
    }

    public IElementType parseCommonDeclaration(
            @NotNull final ModifierDetector detector,
            @NotNull final NameParsingMode nameParsingMode,
            @NotNull final DeclarationParsingMode declarationParsingMode
    ) {

//       TODO 声明作用域判断

        return switch (this.getTokenId()) {

            case TYPE_KEYWORD_Id ->
//                if (declarationParsingMode == DeclarationParsingMode.LOCAL) {
//                    yield null;
//                } else {
//                    yield parseTypeAlias();
//                }

                    switch (declarationParsingMode) {
                        case LOCAL, MEMBER, MEMBER_OR_TOPLEVEL -> {
                            this.parseTypeAlias();
                            yield INVALID_DECLARATION;
                        }
                        case ALL, TOPLEVEL -> this.parseTypeAlias();


                    };


            case AT_Id -> this.myExpressionParsing.parseMacroExpression(true);
//                    parseAnnotation(detector);


            case FOREIGN_KEYWORD_Id ->
//                if (declarationParsingMode == DeclarationParsingMode.LOCAL) {
//                    yield null;
//                } else {
//                    yield parseForeign();
//                }

                    switch (declarationParsingMode) {

                        case ALL, TOPLEVEL -> this.parseForeign();

                        case MEMBER, MEMBER_OR_TOPLEVEL, LOCAL -> {
                            this.parseForeign();
                            yield INVALID_DECLARATION;
                        }

                    };

            case MACRO_KEYWORD_Id -> this.parseMacro();

//            case ABC_KEYWORD_Id:
//                return parseAbc();
            case FUNC_KEYWORD_Id -> this.parseFunction(detector);
            case MAIN_KEYWORD_Id ->
//                if (declarationParsingMode == DeclarationParsingMode.LOCAL) {
//                    yield null;
//                } else {
//                    yield parseMainFunc();
//                }

                    switch (declarationParsingMode) {

                        case ALL, TOPLEVEL -> this.parseMainFunc();

                        case MEMBER, MEMBER_OR_TOPLEVEL, LOCAL -> {
                            this.parseMainFunc();
                            yield INVALID_DECLARATION;
                        }

                    };


//                return parseEnum();

//            case EXTEND_KEYWORD_Id -> switch (declarationParsingMode) {
//
//                case ALL, TOPLEVEL -> parseExtend(detector);
//
//                case MEMBER, MEMBER_OR_TOPLEVEL, LOCAL -> {
//                    parseExtend(detector);
//                    yield INVALID_DECLARATION;
//                }
//
//            };
            case EXTEND_KEYWORD_Id, ENUM_KEYWORD_Id, STRUCT_KEYWORD_Id, INTERFACE_KEYWORD_Id, CLASS_KEYWORD_Id ->
                    switch (declarationParsingMode) {

                        case ALL, TOPLEVEL -> this.parseClass(detector);

                        case MEMBER, MEMBER_OR_TOPLEVEL, LOCAL -> {
                            this.parseClass(detector);
                            yield INVALID_DECLARATION;
                        }

                    };


            case LET_KEYWORD_Id, VAR_KEYWORD_Id, CONST_KEYWORD_Id -> this.parseVariable(detector, declarationParsingMode);
//            case UNSAFE_KEYWORD_Id -> parseUnsafeExpression();
            default -> null;
        };

    }

    /*
     * typeAlias
     *   : modifiers "typealias" SimpleName typeParameters? "=" type
     *   ;
     */
    private IElementType parseTypeAlias() {
        assert this._at(TYPE_KEYWORD);

        this.advance(); // TYPE_KEYWORD

        this.expect(IDENTIFIER, "Type name expected", CangJieParsing.LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET);

        this.parseTypeParameterList(CangJieParsing.TYPE_PARAMETER_GT_RECOVERY_SET);

        if (this.at(WHERE_KEYWORD)) {
            final PsiBuilder.Marker error = this.mark();
            this.parseTypeConstraints();
            error.error("Type alias parameters can't have bounds");
        }

        this.expect(EQ, "Expecting '='", CangJieParsing.TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET);

        this.parseTypeRef();

        this.consumeIf(SEMICOLON);

        return TYPEALIAS;
    }

    IElementType parseProperty(final boolean isInterface) {
        return this.parseProperty(isInterface, null, null);
    }

    private IElementType parseProperty() {
        return this.parseProperty(false);
    }


//    private IElementType parseAbc() {
//        assert _at(ABC_KEYWORD);
//        advance();
//        return null;
//    }

    IElementType parseProperty(final ModifierDetector classdetector, final ModifierDetector detector) {
        return this.parseProperty(false, classdetector, detector);
    }

    /*
     * prop
     *   :  "mnt"? prop Identifier :Type propBody
     *   ;
     */
    private IElementType parseProperty(final boolean isInterface, final ModifierDetector classdetector, final ModifierDetector detector) {
        assert this._at(PROP_KEYWORD);


//        bool isMut = false;
//        if (at(MUT_KEYWORD)) {
//            isMut = true;
//            advance();
//        }
//        if (!at(PROP_KEYWORD)) {
//            errorAndAdvance("Expecting 'prop'");
//
//        }
        this.advance();

        this.parseIdentifierByTitle(" prop ");

//        if(at(COLON)){
        this.parseByType();
//        }else {
//            error("Expecting ':'");
//        }

        if (this.isDeclarationsFile) {

            if (this.at(LBRACE)) {
                final PsiBuilder.Marker body = this.mark();

                final TokenSet tokenSet = TokenSet.orSet(
                        KEYWORDS, TokenSet.create(OPEN_KEYWORD,
                                ABSTRACT_KEYWORD,
                                SEALED_KEYWORD)
                );
                while (!this.atSet(tokenSet) && !this.eof()) {
                    this.advance();
                }
                body.error("Property body is not allowed in declarations file");

            }
            return PROPERTY;
        }

        if (this.at(LBRACE)) {
            this.parsePropertyBody(detector);
        } else if (!isInterface) {
            if (null != classdetector && !classdetector.isAbstractDetected()) {
                this.error("unimplemented abstract property");
                this.error("Missing prop body Expecting '{'");
            }

        }


        return PROPERTY;


    }

    /**
     * ":" type
     */
    private void parseByType() {
//        assert  _at(COLON);
        if (this.at(COLON)) {
            this.advance(); // COLON
            this.parseTypeRef();
        } else {
            this.error("Missing type Expecting ':' type");
        }
    }

    /**
     * prop get
     * :  "get" "(" ")" block
     */
    private void parsePropertyGet() {
        assert this._at(GET_KEYWORD);

        final PsiBuilder.Marker get = this.mark();
        this.advance(); // GET_KEYWORD

        if (this.expect(LPAR, "Expecting '('")) {

            if (this.expect(RPAR, "Expecting ')'")) {
                if (this.at(LBRACE)) {
                    this.parseBlock();
                } else {
                    this.error("Expecting '{'");
                }
            }
        }

        get.done(PROPERTY_ACCESSOR);

    }

    /**
     * prop set
     * :  "set" "(" Identifier ")" block
     */
    private void parsePropertySet(final ModifierDetector detector) {
        assert this._at(SET_KEYWORD);
        final PsiBuilder.Marker set = this.mark();

//        if(detector.isMutDetected()){
        this.advance(); // SET_KEYWORD


        if (this.expect(LPAR, "Expecting '('")) {
            final SyntaxTreeBuilder.Marker plist = this.mark();
            final SyntaxTreeBuilder.Marker value = this.mark();

            this.expect(IDENTIFIER, "Expecting identifier");
            value.done(VALUE_PARAMETER);
            plist.done(VALUE_PARAMETER_LIST);
            if (this.expect(RPAR, "Expecting ')'")) {
                if (this.at(LBRACE)) {
                    this.parseBlock();
                } else {
                    this.error("Expecting '{'");
                }
            }
        }
//        }else{
//            error("immutable property cannot have setter");
//            set.drop();
//            return;
//        }

//        if (!detector.isMutDetected()) {
//            set.error("immutable property cannot have setter");
//            return;
//        }
//        set.done(PROPERTY_SET);
        set.done(PROPERTY_ACCESSOR);

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
    private void parsePropertyBody(final ModifierDetector detector) {
        assert this._at(LBRACE);


        final PsiBuilder.Marker body = this.mark();
        this.advance(); // LBRACE
//        boolean isGet = false;
//        boolean isSet = false;
//        while (at(GET_KEYWORD) || at(SET_KEYWORD)) {
//            if (at(GET_KEYWORD) && !isGet) {
//                isGet = true;
//                parsePropertyGet();
//            }
//
//            if (at(SET_KEYWORD) && !isSet) {
//                isSet = true;
//                parsePropertySet(detector);
//            }
//
//        }
//
//        if (!isGet) {
//            error("Get accessor should be implemented");
//        }
//
//        if (!isSet && detector != null && detector.isMutDetected()) {
//            error("Set accessor should be implemented");
//        }
        while (this.at(GET_KEYWORD) || this.at(SET_KEYWORD)) {
            this.parsePropertyAccessor();

        }

        this.expect(RBRACE, "Expecting '}'");

        body.done(PROPERTY_BODY);

    }

    private void parsePropertyAccessor() {

        if (this.at(GET_KEYWORD)) {

            this.parsePropertyGet();
        }

        if (this.at(SET_KEYWORD)) {

            this.parsePropertySet(null);
        }


    }

    /*
     * enum
     *   : "enum" SimpleName ("{" enumEntry((Type)?){"|"}  "}")
     *   ;
     */
    private IElementType parseEnum() {
        assert this._at(ENUM_KEYWORD);
        this.advance();

        this.parseIdentifierByTitle("enum", CangJieParsing.IDENTIFIER_RBRACKET_LBRACKET_SET, false);


        this.parseEnumBody();

        return ENUM;
    }

    private void parseEnumBody() {
        final PsiBuilder.Marker body = this.mark();
        if (this.at(LBRACE)) {
            this.advance(); // LBRACE

            this.expect(OR);

            if (this.at(IDENTIFIER)) {
                this.parseEnumList();
            } else {
                this.error("Expecting enum entry");
            }

            this.parseMembers(null, null);


            this.expect(RBRACE, "Expecting '}'");
        } else {
            this.error("Expecting '{'");
        }
        body.done(ENUM_BODY);
    }

    private void parseEnumList() {


        while (true) {

            this.parseEnumEntry();
            if (this.at(RBRACE)) {
                break;
            }
            if (this.at(OR)) {
                this.advance();
            } else {
                break;
            }

        }


    }

    void parseTypeCodeFragment() {
        final PsiBuilder.Marker marker = this.mark();
        this.parseTypeRef();

        this.checkForUnexpectedSymbols();

        marker.done(TYPE_CODE_FRAGMENT);
    }

    private void checkForUnexpectedSymbols() {
        while (!this.eof()) {
            this.errorAndAdvance("Unexpected symbol");
        }
    }

    boolean parseEnumEntry() {
        return this.parseEnumEntry(true);
    }

    boolean parseEnumEntry(final boolean isCreateMark) {
        PsiBuilder.Marker entry = null;
        if (isCreateMark) {
            entry = this.mark();

        }

        if (!this.expect(IDENTIFIER, "Expecting enum entry name")) {
            if (isCreateMark) {
                entry.drop();
            }
            return false;
        }


//        parseIdentifierByTitle("enum entry", IDENTIFIER_RBRACKET_LBRACKET_SET);


//        处理泛型
//        parseTypeArgumentList();


        if (this.at(LPAR)) {
            this.advance(); // LPAR
            this.parseTypeList();

            this.expect(RPAR, "Expecting ')'");
        }


        if (isCreateMark) {
            entry.done(ENUM_ENTRY);
        }
        return true;
    }

    /**
     * typelist
     * : type{","}
     */
    void parseTypeList() {
        final PsiBuilder.Marker list = this.mark();

        while (true) {
            this.parseTypeRef();
            if (!this.at(COMMA)) break;
            this.advance(); // COMMA
        }

        list.done(TYPE_LIST);
    }

    /*
     * typeParameters
     *   : ("<" typeParameter{","} ">"
     *   ;
     */
    private boolean parseTypeParameterList(final TokenSet recoverySet) {

        boolean result = false;
        if (this.at(LT)) {
            final PsiBuilder.Marker list = this.mark();

            this.myBuilder.disableNewlines();
            this.advance(); // LT

            while (true) {
                if (this.at(COMMA)) this.errorAndAdvance("Expecting type parameter declaration");
                this.parseTypeParameter();
//                parseTypeRef(true);

                if (!this.at(COMMA)) break;
                this.advance(); // COMMA
                if (this.at(GT)) {
                    break;
                }
            }

            this.expect(GT, "Missing '>'", recoverySet);
            this.myBuilder.restoreNewlinesState();
            result = true;

            list.done(TYPE_PARAMETER_LIST);
        }
        return result;
    }

    /*
     * typeParameter
     *   : modifiers SimpleName (":" userType)?
     *   ;
     */
    private void parseTypeParameter() {
        if (this.atSet(CangJieParsing.TYPE_PARAMETER_GT_RECOVERY_SET)) {
            this.error("Type parameter declaration expected");
            return;
        }

        final PsiBuilder.Marker mark = this.mark();

//        parseModifierList(GT_COMMA_COLON_SET);

        this.expect(IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY);

//        if (at(COLON)) {
//            advance(); // COLON
//            parseTypeRef();
//        }

        mark.done(TYPE_PARAMETER);
    }

    private void parseDelegationSpecifier() {
        final PsiBuilder.Marker delegator = this.mark();
        final PsiBuilder.Marker reference = this.mark();
        this.parseTypeRef();


        reference.drop();
        delegator.done(SUPER_TYPE_ENTRY);

    }

    /*
     * delegationSpecifier{"&"}
     */
    private void parseDelegationSpecifierList() {
        final PsiBuilder.Marker list = this.mark();

        while (true) {
            if (this.at(AND)) {
                this.errorAndAdvance("Expecting a delegation specifier");
                continue;
            }
            this.parseDelegationSpecifier();
            if (!this.at(AND)) break;
            this.advance(); // COMMA
        }

        list.done(SUPER_TYPE_LIST);
    }

    /*
     * (modifier)*
     */
    boolean parseModifierList(@NotNull final TokenSet noModifiersBefore) {
        return this.parseModifierList(null, noModifiersBefore);
    }


    /*
     * class
     *   : "class" SimpleName (<: delegationSpecifier{"&"}) classBody
     *   ;
     */
    IElementType parseClass(@NotNull final ModifierDetector detector) {

        final int tokenid = this.getTokenId();

        final IElementType token = this.myBuilder.getTokenType();


//        assert _at(CLASS_KEYWORD);
        assert this._atSet(CangJieParsing.CLASS_INTERFACE_STRUCT_ENUM_EXTEND_SET);
        this.advance();


        boolean typeParametersDeclared = false;

        if (token == EXTEND_KEYWORD) {

            if (this.at(LT)) {
                this.parseTypeParameterList(CangJieParsing.TYPE_PARAMETER_GT_RECOVERY_SET);
                typeParametersDeclared = true;
            }


//            if (atSet(BASICTYPES) || at(IDENTIFIER)) {
            this.parseTypeRef();

//            }  else {
//
//                error("Expecting a type");
//            }
        } else {
            this.parseIdentifier(); //类名
            typeParametersDeclared = this.parseTypeParameterList(CangJieParsing.TYPE_PARAMETER_GT_RECOVERY_SET);
        }


        // TODO 继承
        if (this.at(LTCOLON)) {
            this.advance(); // COLON
            this.parseDelegationSpecifierList();
        }


        final OptionalMarker whereMarker = new OptionalMarker(false);
        this.parseTypeConstraintsGuarded(typeParametersDeclared);
        whereMarker.error("Where clause is not allowed");


        if (this.at(LBRACE)) {
            switch (tokenid) {
                case ENUM_KEYWORD_Id:
                    this.parseEnumBody();
                    break;
                case EXTEND_KEYWORD_Id:
                case STRUCT_KEYWORD_Id:
                case INTERFACE_KEYWORD_Id:
                case CLASS_KEYWORD_Id:
                    this.parseClassBody(tokenid, detector);
                    break;

                default:
                    this.parseClassBody(tokenid, detector);


            }

        } else {
            this.error("Expecting '{' or Inherit");  //应该为'{' 或者继承
        }


        return switch (tokenid) {
            case INTERFACE_KEYWORD_Id -> INTERFACE;
            case STRUCT_KEYWORD_Id -> STRUCT;
            case CLASS_KEYWORD_Id -> CLASS;
            case ENUM_KEYWORD_Id -> ENUM;
            case EXTEND_KEYWORD_Id -> EXTEND;
            default -> CLASS;
        };


    }

    /*
     * typeConstraints
     *   : ("where" typeConstraint{","})?
     *   ;
     */
    private void parseTypeConstraintsGuarded(final boolean typeParameterListOccurred) {
        final PsiBuilder.Marker error = this.mark();
        final boolean constraints = this.parseTypeConstraints();
        AbstractCangJieParsing.errorIf(error, constraints && !typeParameterListOccurred, "Type constraints are not allowed when no type parameters declared");
    }

    private boolean parseTypeConstraints() {
        if (this.at(WHERE_KEYWORD)) {
            this.parseTypeConstraintList();
            return true;
        }
        return false;
    }

    /*
     * typeConstraint{","}
     */
    private void parseTypeConstraintList() {
        assert this._at(WHERE_KEYWORD);

        this.advance(); // WHERE_KEYWORD

        final PsiBuilder.Marker list = this.mark();

        while (true) {
            if (this.at(COMMA)) this.errorAndAdvance("Type constraint expected");
            this.parseTypeConstraint();
            if (!this.at(COMMA)) break;
            this.advance(); // COMMA
        }

        list.done(TYPE_CONSTRAINT_LIST);
    }

    /*
     * typeConstraint
     *   :   SimpleName "<:" type
     *   ;
     */
    private void parseTypeConstraint() {
        final PsiBuilder.Marker constraint = this.mark();


        final PsiBuilder.Marker reference = this.mark();
        if (this.expect(IDENTIFIER, "Expecting type parameter name", CangJieParsing.LTCOLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET)) {
            reference.done(REFERENCE_EXPRESSION);
        } else {
            reference.drop();
        }

        this.expect(LTCOLON, "Expecting '<:' before the upper bound", CangJieParsing.LBRACE_RBRACE_TYPE_REF_FIRST_SET);


        do {
            if (this.at(AND)) this.advance();
            this.parseTypeRef();

        } while (this.at(AND));


        constraint.done(TYPE_CONSTRAINT);
    }

    private void parseMemberDeclaration(final Integer tokenId, final ModifierDetector classdetector) {
        this.parseMemberDeclaration(tokenId, classdetector, false);
    }

    private void parseMemberDeclaration(final Integer tokenId, final ModifierDetector classdetector, final boolean rollbackMacro) {
        if (this.at(SEMICOLON)) {
            this.advance(); // SEMICOLON
            return;
        }
        final PsiBuilder.Marker decl = this.mark();


        final ModifierDetector detector = new ModifierDetector();
        this.parseModifierList(detector, TokenSet.EMPTY, rollbackMacro);


        final IElementType declType = this.parseMemberDeclarationRest(tokenId, classdetector, detector);

      /*  if (declType == ANNOTATION_ENTRY) {
            decl.rollbackTo();
            parseMemberDeclaration(tokenId, classdetector, true);
        } else*/
        if (null == declType) {
            this.errorWithRecovery("Expecting member declaration", TokenSet.EMPTY);
            decl.drop();
        } else {
            AbstractCangJieParsing.closeDeclarationWithCommentBinders(decl, declType, true);
        }
    }

    private IElementType parseMemberDeclarationRest(final Integer tokenId, final ModifierDetector classdetector, final ModifierDetector detector) {
        IElementType declType = this.parseClassCommonDeclaration(tokenId, classdetector, detector);

        if (null != declType) return declType;

        if (null != tokenId && INTERFACE_KEYWORD_Id != tokenId) {

            this.parseModifierList(TokenSet.EMPTY);

            if (this.at(INIT_KEYWORD)) {
                this.parseInitFunc();
                declType = SECONDARY_CONSTRUCTOR;
            } else if (this.at(LBRACE)) {
                this.error("Expecting member declaration");
                this.parseBlock();
                declType = FUNC;
            } else if (this.at(IDENTIFIER) && this.lookahead(1) == LPAR) {
//                主构造函数
                this.parseMainInitFunc();
                declType = PRIMARY_CONSTRUCTOR;
            } else if (this.at(TILDE) && this.lookahead(1) == INIT_KEYWORD) {
                this.advance(); // TILDE ~
                this.parseInitFunc();
//                析构函数
                declType = END_SECONDARY_CONSTRUCTOR;

            }

        }


        return declType;
    }

    void parseMainInitFunc() {
        assert this._at(IDENTIFIER);
        this.advance(); // IDENTIFIER


        if (this.at(RBRACE)) {
            this.error("Function body expected");  //应该为函数体
            return;
        }

        this.myBuilder.disableJoiningComplexTokens();
        //类型参数
        if (this.at(LPAR)) {
            this.parseInitFuncValueParameterList();
        } else {
//            error("Expecting '(' ");  //应该为'('
            this.errorAndAdvance("Expecting '('  but available" + this.myBuilder.getTokenText());

        }


//        if (at(LBRACE)) {
        this.parseInitFunctionBody();
//        } else {
//            error("Expecting '{' ");  //应该为'{'
//        }
    }

    void parseInitFunc() {
        assert this._at(INIT_KEYWORD);
        this.advance(); // INIT_KEYWORD


        if (this.at(RBRACE)) {
            this.error("Function body expected");  //应该为函数体
            return;
        }

        this.myBuilder.disableJoiningComplexTokens();
        //类型参数
        if (this.at(LPAR)) {
            this.parseInitFuncValueParameterList();
        } else {
//            error("Expecting '(' ");  //应该为'('
            this.errorAndAdvance("Expecting '('  but available" + this.myBuilder.getTokenText());
        }

        if (this.isDeclarationsFile) {
            if (this.at(LBRACE)) {
                final PsiBuilder.Marker body = this.mark();
                while (!this.atSet(KEYWORDALL) && !this.eof()) {
                    this.advance();
                }

//                parseFunctionBody();

                body.error("Method bodies are not allowed in declaration files");


            }
            return;
        }


//        if (at(LBRACE)) {
        this.parseInitFunctionBody();
//        } else {
//            error("Expecting '{' ");  //应该为'{'
//        }
    }

    /**
     * members
     * : memberDeclaration*
     * ;
     */
    private void parseMembers(final Integer tokenId, final ModifierDetector detector) {
        while (!this.eof() && !this.at(RBRACE)) {
            this.parseMemberDeclaration(tokenId, detector);
        }
    }

    private void parseClassBody(final Integer tokenId, @NotNull final ModifierDetector detector) {
        final PsiBuilder.Marker body = this.mark();

        this.myBuilder.enableNewlines();

        if (this.expect(LBRACE, "Expecting a class body")) {
            this.parseMembers(tokenId, detector);
            this.expect(RBRACE, "Missing '}");
        }

        this.myBuilder.restoreNewlinesState();

        body.done(CLASS_BODY);
    }

    private IElementType parseMainFunc() {
        assert this._at(MAIN_KEYWORD);
        this.advance();
        if (this.at(RBRACE)) {
            this.error("Function body expected");  //应该为函数体
            return MAIN_FUNC;
        }
        this.myBuilder.disableJoiningComplexTokens();
        //类型参数
        if (this.at(LPAR)) {
            this.parseValueParameterList(false, false, CangJieParsing.VALUE_PARAMETERS_FOLLOW_SET);

        } else {
            this.error("Expecting '(' ");  //应该为'('
        }

        //返回值类型
        if (this.at(COLON)) {
            this.advance(); // COLON
            this.parseTypeRef();
        }

        //函数体
//        if (at(SEMICOLON)) {
//            advance(); // SEMICOLON
//        } else
        if (this.at(LBRACE)) {
            this.parseFunctionBody();
        } else {
            this.error("Expecting '{' ");  //应该为'{'
        }
        return MAIN_FUNC;
    }

    @NotNull
    IElementType parseFunction() {
        return this.parseFunction(false, null, null, false, 0);
    }

    @NotNull
    IElementType parseFunction(final boolean isInterfaceMethod, final ModifierDetector classdetector, final ModifierDetector detector, final Integer topTokenId) {
        return this.parseFunction(isInterfaceMethod, classdetector, detector, false, topTokenId);
    }

    @NotNull
    IElementType parseFunction(final boolean isInterfaceMethod, final Integer topTokenId) {
        return this.parseFunction(isInterfaceMethod, null, null, false, topTokenId);
    }

    @NotNull
    IElementType parseFunction(final ModifierDetector detector
    ) {
        return this.parseFunction(detector, 0);
    }

    @NotNull
    IElementType parseFunction(final ModifierDetector detector, final Integer topTokenId
    ) {
        return this.parseFunction(false, null, detector, false, topTokenId);
    }

    @NotNull
    IElementType parseForeignFunction(final ModifierDetector detector) {
        return this.parseForeignFunction(detector, 0);
    }

    @NotNull
    IElementType parseForeignFunction(final ModifierDetector detector, final Integer topTokenId) {
        return this.parseFunction(false, null, detector, true, topTokenId);
    }

    @NotNull
    IElementType parseFunction(final ModifierDetector classdetector, final ModifierDetector detector, final Integer topTokenId) {
        return this.parseFunction(false, classdetector, detector, false, topTokenId);
    }

    /*
     * IDENTIFIER 标识符
     */
    private void parseIdentifier() {
        if (this.expect(IDENTIFIER)) return;

        if (this.atSet(KEYWORDS)) {
            this.error("Keywords cannot be used"); //关键字不能使用
        }

        if (!this.at(LPAR)) {
            this.errorAndAdvance("Expecting an CangJie identifier");
            return;
        }

        this.error("Expecting an CangJie identifier"); //应该为标识符

    }

    /*
     * IDENTIFIER
     */
    private void parseIdentifierByTitle(
            final String title, final TokenSet recoverySet, final boolean isUnderline
    ) {

        if (isUnderline && this.expect(CangJieExpressionParsing.IDENTIFIER_RECOVERY_SET)) {
            return;
        }

        if (this.expect(IDENTIFIER)) {
            return;
        }

        this.errorWithRecovery("Expecting " + title + " name", recoverySet);
    }

    private void parseIdentifierByTitle(final String title) {
        this.parseIdentifierByTitle(title, TokenSet.EMPTY, false);
    }

    /*
     * function
     *   :   "func"
     *       SimpleName
     *       typeParameters? functionParameters (":" type)?
     *       functionBody?
     *   ;
     */
    @Contract("false -> !null")
    IElementType parseFunction(final boolean isInterfaceMethod, final ModifierDetector classdetector, final ModifierDetector detector, final boolean isForeign, final Integer topTokenId) {


        assert this._at(FUNC_KEYWORD);
        this.advance();
        IElementType type = FUNC;
        if (null != topTokenId && EXTEND_KEYWORD_Id == topTokenId) {
            type = FUNC_EXTEND;
        }

        if (this.at(RBRACE)) {
            this.error("Function body expected");  //应该为函数体
            return type;
        }


        this.myBuilder.disableJoiningComplexTokens();


        if (null != detector && detector.isOperatorDetected()) {


            //运算符重载

            final IElementType operatorToken = this.getOperationTokenType();

            if (OPERATIONS_CAN_BE_OVERLOADED.contains(operatorToken)) {
                final PsiBuilder.Marker operator = this.mark();


                this.advanceOperationToken(operatorToken);

                operator.done(OPERATION_NAME);
            } else {

                final PsiBuilder.Marker mark = this.mark();
                this.advance();
                mark.error("Should be an overloaded operator");


            }
        } else {
            //函数名
            this.myBuilder.getTokenType();
            this.parseIdentifier();
        }


        boolean typeParameterListOccurred = false;
        if (this.at(LT)) {
            this.parseTypeParameterList(CangJieParsing.LBRACKET_LBRACE_RBRACE_LPAR_SET);
            typeParameterListOccurred = true;
        }


        //类型参数
        if (this.at(LPAR)) {
            this.parseValueParameterList(false, false, CangJieParsing.VALUE_PARAMETERS_FOLLOW_SET);

        } else {
            this.error("Expecting '(' ");  //应该为'('
        }

        //返回值类型
        if (this.at(COLON)) {
            this.advance(); // COLON
            this.parseTypeRef();
        }
        this.parseTypeConstraintsGuarded(typeParameterListOccurred);
        //函数体

        if (this.isDeclarationsFile) {

            if (this.at(LBRACE)) {
                final PsiBuilder.Marker body = this.mark();
                while (!this.atSet(KEYWORDALL) && !this.eof()) {
                    this.advance();
                }


                body.error("Method bodies are not allowed in declaration files");

            }


            return type;
        }
      /*  if (at(EQ)) {
            advance();
            myExpressionParsing.parseExpression();

        } else*/
        if (this.at(LBRACE)) {

            this.parseFunctionBody();
            if (isForeign) {
                this.error("foreign function can not have body");  //应该为'{'
            }

        } else if (!(isInterfaceMethod || (null != classdetector && classdetector.isAbstractDetected())) && (null != detector && !detector.isForeignDetected())) {
            this.error("Expecting '{' ");  //应该为'{'
        }

        return type;
    }

    public void parseSynchronizedExpression() {
        assert this._at(SYNCHRONIZED_KEYWORD);
        final SyntaxTreeBuilder.Marker synchronizedMarker = this.mark();
        this.advance();


        if (this.at(LPAR)) {
            this.advance();
            this.myExpressionParsing.parseExpression();

            this.expect(RPAR, "Expecting '('");


        } else {
            this.error("Expecting '(' ");
        }

        if (this.at(LBRACE)) {
            this.parseBlock();
        } else {
            this.error("Expecting '{' ");  //应该为'{'
        }

        synchronizedMarker.done(SYNCHRONIZED_EXPRESSION);
    }

//    public void parseSpawnExpression() {
//        assert _at(SPAWN_KEYWORD);
//        SyntaxTreeBuilder.Marker spawn = mark();
//        advance();
//
//
//        if (at(LBRACE)) {
//            parseBlock();
//        } else {
//            error("Expecting '{' ");  //应该为'{'
//        }
//
//
//        spawn.done(SPAWN_EXPRESSION);
//    }


    /**
     * 外部函数声明块
     */
    private IElementType parseForeign() {
        assert this._at(FOREIGN_KEYWORD);
        this.advance();

//        处理声明块
        if (this.at(LBRACE)) {
            this.parseForeignBody();

        } else {
            this.error("Expecting '{' ");

        }


        return FOREIGN;
    }

    /**
     * 外部函数声明块
     */
    private void parseForeignBody() {
        assert this._at(LBRACE);

        final PsiBuilder.Marker mark = this.mark();
        this.advance(); // LBRACE

        while (!this.at(RBRACE) && !this.eof()) {
            final ModifierDetector detector = new ModifierDetector();

            this.parseModifierList(detector, TokenSet.EMPTY);


            if (this.at(FUNC_KEYWORD)) {
                detector.consume(FOREIGN_KEYWORD);

                this.parseForeignFunction(detector);
            } else {
                this.errorWithRecovery("Expecting function declaration", TokenSet.create(FUNC_KEYWORD));
            }


//            advance();

        }
        this.expect(RBRACE, "Missing '}");
//        if (at(RBRACE)) {
//            advance();
//
//        } else {
//            error("Expecting '}' ");
//        }
        mark.done(FOREIGN_BODY);

    }

    /**
     * 宏定义  与方法定义相同
     *
     * @return
     */
    private IElementType parseMacro() {
        assert this._at(MACRO_KEYWORD);
        this.advance();

        if (this.at(RBRACE)) {
            this.error("Function body expected");  //应该为函数体
            return MACRO;
        }


        this.myBuilder.disableJoiningComplexTokens();


        //函数名
        this.parseIdentifier();


//        expect(EXCL);

        boolean typeParameterListOccurred = false;
        if (this.at(LT)) {
            this.parseTypeParameterList(CangJieParsing.LBRACKET_LBRACE_RBRACE_LPAR_SET);
            typeParameterListOccurred = true;
        }


        //类型参数
        if (this.at(LPAR)) {
            this.parseValueParameterList(false, false, CangJieParsing.VALUE_PARAMETERS_FOLLOW_SET);

        } else {
            this.error("Expecting '(' ");  //应该为'('
        }

        //返回值类型
        if (this.at(COLON)) {
            this.advance(); // COLON
            this.parseTypeRef();
        }
        this.parseTypeConstraintsGuarded(typeParameterListOccurred);


        if (this.isDeclarationsFile) {

            if (this.at(LBRACE)) {
                final PsiBuilder.Marker body = this.mark();
                while (!this.atSet(KEYWORDALL) && !this.eof()) {
                    this.advance();
                }

                body.error("Method bodies are not allowed in declaration files");

            }

            return MACRO;
        }

        //函数体
//        if (at(SEMICOLON)) {
//            advance(); // SEMICOLON
//        } else
        if (this.at(LBRACE)) {
            this.parseFunctionBody();
        } else {
            this.error("Expecting '{' ");  //应该为'{'
        }
        return MACRO;
    }

    /*
     * functionBody
     *   : block

     *   ;
     */
    void parseInitFunctionBody() {
        //        恢复  init() : xxxx {}
        if (this.at(COLON)) {
            final PsiBuilder.Marker error = this.mark();
            while (!this.at(LBRACE)) this.advance();

            error.error("Expecting '{' ");
        }
        if (this.isDeclarationsFile) {

            if (this.at(LBRACE)) {
                final PsiBuilder.Marker body = this.mark();
                while (!this.atSet(KEYWORDALL) && !this.eof()) {
                    this.advance();
                }

                body.error("Method bodies are not allowed in declaration files");

            }

            return;
        }
        if (this.at(LBRACE)) {
            this.parseInitFunctionBlock();
        } else {
            this.error("Expecting function body"); //应该为函数体
        }
    }

    /*
     * functionBody
     *   : block
     *   : "=" element
     *   ;
     */
    void parseFunctionBody() {
        if (this.at(LBRACE)) {
            this.parseBlock();
        } else {
            this.error("Expecting function body"); //应该为函数体
        }
    }


    /*
     * functionParameter
     *   : modifiers ("val" | "var")? parameter ("=" element)?
     *   ;
     */
    private boolean tryParseValueParameter(final boolean typeRequired) {
        return this.parseValueParameter(true, typeRequired);
    }

    //    private void parseFunctionTypeValueParameterModifierList() {
//        doParseModifierList(null, RESERVED_VALUE_PARAMETER_MODIFIER_KEYWORDS, NO_ANNOTATIONS, NO_MODIFIER_BEFORE_FOR_VALUE_PARAMETER);
//    }


    void parseInitFuncValueParameterList() {
        this.parseValueParameterList(false, false, CangJieParsing.VALUE_PARAMETERS_FOLLOW_SET, true);
    }


    private void parseValueParameterList(final boolean isFunctionTypeContents, final boolean typeRequired, final TokenSet recoverySet) {
        this.parseValueParameterList(isFunctionTypeContents, typeRequired, recoverySet, false);

    }

    /*
     * functionParameters
     *   : "(" functionParameter{","}? ")"
     *   ;
     *
     * functionParameter
     *   : modifiers functionParameterRest
     *   ;
     *
     * functionParameterRest
     *   : parameter ("=" element)?
     *   ;
     */
    private void parseValueParameterList(final boolean isFunctionTypeContents, final boolean typeRequired, final TokenSet recoverySet, final boolean isInitFunc) {
        assert this.at(LPAR);
        final PsiBuilder.Marker parameters = this.mark();


        this.myBuilder.disableNewlines();
        this.advance(); // (


//        用于报告错误   要么全为命名参数，要么全不为命名参数
//        bool isNamedParameter = false;
        final List<Boolean> isNamedParameters = new ArrayList<>();

        while (!this.at(RPAR) && !this.atSet(recoverySet) && !this.eof()) {
            if (this.at(COMMA)) {
                this.errorAndAdvance("Expecting a parameter declaration");  //应该为参数声明
            }
            if (isFunctionTypeContents) {
                if (!this.tryParseValueParameter(typeRequired)) {
                    final PsiBuilder.Marker valueParameter = this.mark();
//                    parseFunctionTypeValueParameterModifierList();
                    this.parseTypeRef();
                    AbstractCangJieParsing.closeDeclarationWithCommentBinders(valueParameter, VALUE_PARAMETER, false);
                    isNamedParameters.add(false);

                } else {
                    isNamedParameters.add(true);

                }
            } else {
                this.parseValueParameter(false, typeRequired, isInitFunc);
            }
//            parseValueParameter(typeRequired);
            if (this.at(COMMA)) {
                this.advance(); // COMMA

                if (this.at(RPAR)) {
                    this.error("Expecting a parameter declaration");  //应该为参数声明
                }

            } else {
                if (!this.at(RPAR)) {
                    this.errorAndAdvance("Expecting ',' or ')',found '" + this.myBuilder.getTokenText() + "'");  //应该为参数声明
                }
//                if (!atSet(isFunctionTypeContents ? LAMBDA_VALUE_PARAMETER_FIRST : VALUE_PARAMETER_FIRST)) break;

            }
        }


//        if (!at(RPAR) && !atSet(recoverySet) && false) {
//            while (true) {
//                //第一个不能为,
//                if (at(COMMA)) {
//                    errorAndAdvance("Expecting a parameter declaration");  //应该为参数声明
//
//                } else if (at(RPAR)) {  //如果为)则跳出循环
//                    break;
//                }
//
//
////                if (isFunctionTypeContents) {
////
////                }
////                else {
//                parseValueParameter(typeRequired);
////                }
//
//
//                if (at(COMMA)) {
//                    advance(); // COMMA
//                } else if (at(COLON)) {
//                    continue;
//                } else {
//                    expect(RPAR, "Expecting Parameter list or ')' but found '" + myBuilder + "'");
////                    if (!at(RPAR)) errorAndAdvance("Expecting Parameter list or ')' but found '" + myBuilder + "'");
//                    if (!atSet(isFunctionTypeContents ? LAMBDA_VALUE_PARAMETER_FIRST : VALUE_PARAMETER_FIRST)) break;
//                }
//
//            }
//        }
        this.expect(RPAR, "Expecting ')'", recoverySet);
        this.myBuilder.restoreNewlinesState();


        if (isNamedParameters.contains(true) && isNamedParameters.contains(false)) {
//            要么全为true 要么全为false
            parameters.error("In a parameter type list, either all parameters must be named, or none of them; mixed is not allowed");
        } else {
            parameters.done(VALUE_PARAMETER_LIST);

        }

    }

    public void parseValueParameter(final boolean typeRequired) {
        this.parseValueParameter(false, typeRequired);
    }

    private boolean parseValueParameter(final boolean rollbackOnFailure, final boolean typeRequired) {
        return this.parseValueParameter(rollbackOnFailure, typeRequired, false);

    }

    private boolean parseValueParameter(final boolean rollbackOnFailure, final boolean typeRequired, final boolean isInitFunc) {
        final PsiBuilder.Marker parameter = this.mark();

//
//        if (at(VAR_KEYWORD) || at(LET_KEYWORD)) {
//            advance(); // VAR_KEYWORD | LET_KEYWORD
////            return false;
////            error("Expecting parameter declaration");  //应该为参数声明
//        }
        if (isInitFunc) {


            final ModifierDetector detector = new ModifierDetector();

            this.parseModifierList(detector, TokenSet.EMPTY, true);
            if ((this.at(LET_KEYWORD) || this.at(VAR_KEYWORD))) {
                this.advance();
            } else if (0 < detector.getSize()) {

                this.error("Missing variable declaration symbol let or var after modifier");  //应该为参数声明
            }
        }


        if (!this.parseFunctionParameterRest(typeRequired) && rollbackOnFailure) {
            parameter.rollbackTo();
            return false;
        }

        AbstractCangJieParsing.closeDeclarationWithCommentBinders(parameter, VALUE_PARAMETER, false);
        return true;
    }


    /*
     * functionParameterRest  函数参数
     *   : parameter
     *   : identifier('!') ':' type ("=" element)  ! 和 = 必须同时出现
     *   ; identifier ':' ('?')?type   可以为Option.Nono
     *   ;
     */
    private boolean parseFunctionParameterRest(final boolean typeRequired) {
        boolean noErrors = true;
        boolean isDefault = false;


        // 恢复 'func foo(Array<String>) {}'
        // 恢复 'func foo(: Int) {}'
        if ((this.at(IDENTIFIER) && this.lookahead(1) == LT) || this.at(COLON)) {
            this.error("Missing parameter name");  //缺少参数名称
            if (this.at(COLON)) {
                // 保留noErrors==true，这样在函数类型的解析过程中不会回滚以“：”开头的未命名参数
                this.advance(); // :


            } else {
                noErrors = false;
            }
            this.parseTypeRef();
        } else {
            this.expect(CangJieExpressionParsing.IDENTIFIER_RECOVERY_SET, "Missing parameter name", CangJieParsing.PARAMETER_NAME_RECOVERY_SET);

            if (this.expect(EXCL)) {
//              可以有默认值
                isDefault = true;
            }

            if (this.at(COLON)) {
                this.advance(); // :

                if (this.at(IDENTIFIER) && this.lookahead(1) == COLON) {
                    // 恢复 "func foo(x: y: Int)" 处理 'y:' 时，可能是下一个参数的名称
                    this.error("Type reference expected");
                    return false;
                }

                this.parseTypeRef();
            } else if (typeRequired) {
                this.errorWithRecovery("Parameters must have type annotation", CangJieParsing.PARAMETER_NAME_RECOVERY_SET);
                noErrors = false;
            } else {
                this.errorWithoutAdvancing("Expecting ':' Missing type declaration");  //应该为':'
                noErrors = false;
            }


        }
        if (this.at(EQ)) {
            if (isDefault) {
                this.advance();
            } else {
                this.error("The default value cannot be set for non-named parameters");
                this.errorAndAdvance("Expecting ',' or ')', found '='");
                noErrors = false;

            }


            this.myExpressionParsing.parseExpression();
        }
        return noErrors;
    }

    private boolean recoverOnParenthesizedWordForPlatformTypes(final int offset, final String word, final boolean consume) {
        // Array<(out) Foo>! or (Mutable)List<Bar>!
        if (this.lookahead(offset) == LPAR &&
                this.lookahead(offset + 1) == IDENTIFIER &&
                this.lookahead(offset + 2) == RPAR &&
                this.lookahead(offset + 3) == IDENTIFIER) {
            final PsiBuilder.Marker error = this.mark();

            this.advance(offset);

            this.advance(); // LPAR
            if (!word.equals(this.myBuilder.getTokenText())) {
                // something other than "out" / "Mutable"
                error.rollbackTo();
                return false;
            } else {
                this.advance(); // IDENTIFIER('out')
                this.advance(); // RPAR

                if (consume) {
                    error.error("Unexpected tokens");
                } else {
                    error.rollbackTo();
                }

                return true;
            }
        }
        return false;
    }

    /*
     *  (optionalProjection type){","}
     */
    private boolean parseTypeArgumentList() {
        if (!this.at(LT)) return false;

        final PsiBuilder.Marker list = this.mark();

        this.tryParseTypeArgumentList(TokenSet.EMPTY);

        list.done(TYPE_ARGUMENT_LIST);

        return true;
    }

    private void recoverOnPlatformTypeSuffix() {
        // 平台类型的恢复
        if (this.at(EXCL)) {
            final PsiBuilder.Marker error = this.mark();
            this.advance(); // EXCL
            error.error("Unexpected token");
        }
    }

    /*
     * functionType
     *   : (type ".")? "(" parameter{","}? ")" "->" type?
     *   ;
     */
    private void parseFunctionType(final PsiBuilder.Marker functionType) {
        this.parseFunctionTypeContents(functionType).done(FUNCTION_TYPE);
    }

    private PsiBuilder.Marker parseFunctionTypeContents(final PsiBuilder.Marker functionType) {
        assert this._at(LPAR) : this.tt();

        this.parseValueParameterList(true, /* typeRequired  = */ true, TokenSet.EMPTY);

        this.expect(ARROW, "Expecting '->' to specify return type of a function type", CangJieParsing.TYPE_REF_FIRST);
        this.parseTypeRef();

        return functionType;
    }

    //返回元组的类型数量
    private int parseTupleType() {
        assert this._at(LPAR);
        int count = 0;


        this.advance(); // LPAR

        if (!this.at(RPAR)) {
            while (true) {

                this.parseTypeRef();
                count++;
                if (!this.at(COMMA)) break;
                this.advance(); // COMMA

            }
        } else {
            this.error("Expecting type");
        }

        this.expect(RPAR, "Expecting ')'");

        return count;
    }

    /**
     * 解析VArray类型
     */
    private boolean parseVArrayType() {


        if (this.at(VARRAY_KEYWORD)) {
            final PsiBuilder.Marker typeRefMarker = this.mark();
            this.advance();

            if (this.at(LT)) {
                this.advance();
                final PsiBuilder.Marker list = this.mark();
                final PsiBuilder.Marker projection = this.mark();

                this.parseTypeRef(TokenSet.EMPTY);

                projection.done(TYPE_PROJECTION);
                list.done(TYPE_ARGUMENT_LIST);

                this.expect(COMMA, "Should be ','");
                this.expect(DOLLAR, "Should be '$'");


                this.expect(INTEGER_LITERAL, "Should be integer literal");

                this.expect(GT, "Should be '>");

            } else {
                this.error("expected type parameters after 'VArray' keyword");
            }

            typeRefMarker.done(VARRAY_TYPE);
            return true;
        }


        return false;


    }

    /*
     * userType
     *   : simpleUserType{"."}
     *   ;
     *
     *   recovers on platform types:
     *    - Foo!
     *    - (Mutable)List<Foo>!
     *    - Array<(out) Foo>!
     */
    public boolean parseUserType() {
//        是否具有泛型
        boolean isTypeArgumentList = false;

        PsiBuilder.Marker userType = this.mark();


        PsiBuilder.Marker reference = this.mark();

        while (true) {
            this.recoverOnParenthesizedWordForPlatformTypes(0, "Mutable", true);
            if (this.expect(IDENTIFIER, "Expecting type name",
                    TokenSet.orSet(CangJieExpressionParsing.Companion.getEXPRESSION_FIRST(), CangJieExpressionParsing.Companion.getEXPRESSION_FOLLOW(),
                            CangJieParsing.DECLARATION_FIRST))) {
                reference.done(REFERENCE_EXPRESSION);
            } else {
                reference.drop();
                break;
            }

            isTypeArgumentList = this.parseTypeArgumentList();

//            recoverOnPlatformTypeSuffix();

            if (!this.at(DOT)) {
                break;
            }

            final PsiBuilder.Marker precede = userType.precede();
            userType.done(USER_TYPE);
            userType = precede;

            this.advance(); // DOT
            reference = this.mark();
        }

        userType.done(USER_TYPE);
        return isTypeArgumentList;

    }

    /**
     * 解析类型引用
     *
     * @return
     */
    private PsiBuilder.Marker parseTypeRefContents(final TokenSet extraRecoverySet) {
        final PsiBuilder.Marker typeRefMarker = this.mark();

        return typeRefMarker;
    }


    /**
     * 解析This类型
     */
    private boolean parseThisType() {


        if (this.at(THIS_KEYWORD_UPPER)) {
            final PsiBuilder.Marker typeRefMarker = this.mark();
            this.advance();
            typeRefMarker.done(THIS_TYPE);
            return true;
        }


        return false;


    }

    /**
     * 解析基本类型
     */
    private boolean parseBasicType() {


        if (this.atSet(BASICTYPES)) {
            final PsiBuilder.Marker typeRefMarker = this.mark();
            this.advance();
            typeRefMarker.done(BASIC_TYPE);
            return true;
        }


        return false;


    }

    /*
     * (SimpleName  {","})
     */
    public void parseMultiDeclarationName(final TokenSet follow, final TokenSet recoverySet) {


        // Parsing multi-name, e.g.
        //   val (a, b) = foo()
        this.myBuilder.disableNewlines();
        this.advance(); // LPAR

        if (!this.atSet(follow)) {
            while (true) {
                if (this.at(COMMA)) {
                    this.errorAndAdvance("Expecting a name");
                } else if (this.at(RPAR)) { // For declaration similar to `val () = somethingCall()`
                    this.error("Expecting a name");
                    break;
                }
                final PsiBuilder.Marker property = this.mark();

                this.parseModifierList(CangJieParsing.COMMA_RPAR_COLON_EQ_SET);

                this.expect(IDENTIFIER, "Expecting a name", recoverySet);

//                if (at(COLON)) {
//
//                    advance(); // COLON
//                    parseTypeRef(follow);
//                }
                property.done(DESTRUCTURING_DECLARATION_ENTRY);

                if (!this.at(COMMA)) break;
                this.advance(); // COMMA
                if (this.at(RPAR)) break;
            }
        }

        this.expect(RPAR, "Expecting ')'", follow);
        this.myBuilder.restoreNewlinesState();
    }

    /**
     * 解析类型参数列表
     */
    boolean tryParseTypeArgumentList(final TokenSet extraRecoverySet) {

        this.myBuilder.disableNewlines();
        this.advance(); // LT


        do {
            if (this.at(COMMA)) {
                this.advance();
            }

            final PsiBuilder.Marker projection = this.mark();

            this.parseTypeRef(extraRecoverySet);

            projection.done(TYPE_PROJECTION);
            if (this.at(GT)) {
                break;
            }
        } while (this.at(COMMA));
//        while (true) {
//            PsiBuilder.Marker projection = mark();
//
//
////            if (at(MUL)) {
////                advance(); // MUL
////            } else {
//            parseTypeRef(extraRecoverySet);
////            }
//            projection.done(TYPE_PROJECTION);
////            if (!at(COMMA)) break;
////            advance(); // COMMA
//
//            if (at(COMMA)) {
//                advance();
//                parseTypeRef(extraRecoverySet);
//            } else {
//                break;
//            }
//            if (at(GT)) {
//                break;
//            }
//        }

        final boolean atGT = this.at(GT);
        if (!atGT) {
            this.error("Expecting a '>'");
        } else {
            this.advance(); // GT
        }
        this.myBuilder.restoreNewlinesState();
        return atGT;

    }
//    private bool parseUserType() {
//        PsiBuilder.Marker usertype = mark();
//
//        if (at(IDENTIFIER)) {
//            advance();
//            usertype.done(USER_TYPE);
//            return true;
//        }
//
//        usertype.drop();
//
//
//        return false;
//    }

    void parseTypeRef(final boolean isConstraint) {
        this.parseTypeRef(TokenSet.EMPTY, isConstraint);
    }

    void parseTypeRef(final TokenSet extraRecoverySet) {
        this.parseTypeRef(extraRecoverySet, false);
    }


    void parseOptionType() {
        assert this._at(QUEST);


        final PsiBuilder.Marker optionTypeMarker = this.mark();

//        if (at(SAFE_CALL)) {
//            // 重新映射为QUEST
//
//
////            if (!isConstraint) {
////                myBuilder.remapCurrentToken(LPAR);
////
////                parseTupleOrFunctionType();
////            } else {
////                error("Expecting a generic type name after '<' in generic, found '?'");
////            }
//
//            myBuilder.remapCurrentToken(LPAR);
//
//            parseTupleOrFunctionType();
//
//            optionTypeMarker.done(OPTIONAL_TYPE);
//
//
//            return;
//        }

        this.advance();

        this.parseTypeRefContents();


        optionTypeMarker.done(OPTIONAL_TYPE);

    }

    /**
     * @param extraRecoverySet
     * @param isConstraint     是否为约束，约束没有问号,不解析userType
     */
    void parseTypeRef(final TokenSet extraRecoverySet, final boolean isConstraint) {

        final PsiBuilder.Marker typeRefMarker = this.mark();
        //先解析基本类型，如果不是基本类型，则解析类型引用


        if (!isConstraint) {
            this.parseTypeRefContents();
        } else {
            this.parseIdentifier();
        }


        typeRefMarker.done(TYPE_REFERENCE);

    }

    private void parseTupleOrFunctionType() {
        PsiBuilder.Marker oType = this.mark();


        final int count = this.parseTupleType();


        if (this.at(ARROW) || this.at(COLON)) {

            oType.rollbackTo();
            oType = this.mark();
            this.parseFunctionType(oType);


        } else {
            if (1 >= count) {
                oType.done(PARENTHESIZED_TYPE);

            } else {
                oType.done(TUPLE_TYPE);

            }

        }
    }

    private void parseTypeRefContents() {
        if (this.parseVArrayType()) return;
        if (this.parseThisType()) return;
        if (this.parseBasicType()) return;
        if (this.at(IDENTIFIER)) {
            this.parseUserType();
        } else if (this.at(LPAR)) {
//            元组，方法或括号类型
            this.parseTupleOrFunctionType();

        } else if (this.at(QUEST) || this.at(SAFE_CALL)) {
//            OPTION类型可嵌套
            this.parseOptionType();

        } else {
            this.error("Expecting a type name, found '" + this.myBuilder.getTokenText() + "'");
        }

    }


    enum MacroType {
        MACRO_CALL,
        //        注解
        ANNOTATION,

    }

    public enum DeclarationParsingMode {
        ALL(false, true, true),
        TOPLEVEL(false, true, true),
        MEMBER(false, true, true),
        MEMBER_OR_TOPLEVEL(false, true, true),
        LOCAL(true, false, false);
//        SCRIPT_TOPLEVEL(true, true, false);

        public final boolean destructuringAllowed;
        public final boolean accessorsAllowed;
        public final boolean canBeEnumUsedAsSoftKeyword;

        DeclarationParsingMode(final boolean destructuringAllowed, final boolean accessorsAllowed, final boolean canBeEnumUsedAsSoftKeyword) {
            this.destructuringAllowed = destructuringAllowed;
            this.accessorsAllowed = accessorsAllowed;
            this.canBeEnumUsedAsSoftKeyword = canBeEnumUsedAsSoftKeyword;
        }
    }

    public enum NameParsingMode {
        REQUIRED, ALLOWED, PROHIBITED
    }

    static class ModifierDetector implements Consumer<IElementType> {

        private boolean abstractDetected;
        private boolean mutDetected;
        private boolean publicDetected;
        private boolean privateDetected;
        private boolean protectedDetected;
        private boolean operatorDetected;
        private boolean foreignDetected;
        private boolean constDetected;
        private boolean unsafeDetected;
        private boolean sealedDetected;
        private boolean redefDetected;
        private boolean openDetected;
        private boolean staticDetected;
        //注解数量
//        private int annotationCount = 0;

        ModifierDetector() {

        }


        /**
         * 返回修饰符的数量
         *
         * @return size
         */
        public int getSize() {
//            遍历该类所有属性
            int size = 0;


//            this.getClass().getDeclaredFields()
            for (final Field field : getClass().getDeclaredFields()) {
                try {
                    if (field.get(this).equals(true)) {
                        size++;
                    }
                } catch (final IllegalAccessException e) {
//                    throw new RuntimeException(e);
                }
            }

            return size;
        }

        @Override
        public void consume(final IElementType item) {
//            if (item == ABSTRACT_KEYWORD) {
//                abstractDetected = true;
//            } else if (item == MUT_KEYWORD) {
//                mutDetected = true;
//            }

            if (item.equals(PUBLIC_KEYWORD)) {
                this.publicDetected = true;
            } else if (item.equals(PRIVATE_KEYWORD)) {
                this.privateDetected = true;
            } else if (item.equals(PROTECTED_KEYWORD)) {
                this.protectedDetected = true;
            } else if (item.equals(ABSTRACT_KEYWORD)) {
                this.abstractDetected = true;
            } else if (item.equals(MUT_KEYWORD)) {
                this.mutDetected = true;
            } else if (item.equals(OPERATOR_KEYWORD)) {
                this.operatorDetected = true;
//
            } else if (item.equals(FOREIGN_KEYWORD)) {
                this.foreignDetected = true;
            } else if (item.equals(CONST_KEYWORD)) {
                this.constDetected = true;
            } else if (item.equals(UNSAFE_KEYWORD)) {
                this.unsafeDetected = true;
            } else if (item.equals(OPEN_KEYWORD)) {
                this.openDetected = true;
            } else if (item.equals(STATIC_KEYWORD)) {
                this.staticDetected = true;
            } else if (item.equals(SEALED_KEYWORD)) {
                this.sealedDetected = true;
            } else if (item.equals(REDEF_KEYWORD)) {
                this.redefDetected = true;
            }/* else if (item.equals(ANNOTATION_ENTRY)) {
                annotationCount++;
            }*/
        }

        public boolean isOperatorDetected() {
            return this.operatorDetected;
        }

        public boolean isAbstractDetected() {
            return this.abstractDetected;
        }

        public boolean isMutDetected() {
            return this.mutDetected;
        }

        public boolean isPublicDetected() {
            return this.publicDetected;
        }

        public boolean isPrivateDetected() {
            return this.privateDetected;
        }

        public boolean isProtectedDetected() {
            return this.protectedDetected;
        }

        public boolean isVisibilityDetected() {
            return this.publicDetected || this.privateDetected || this.protectedDetected;
        }

        public boolean isPublicOrProtectedDetected() {
            return this.publicDetected || this.protectedDetected;
        }

        public boolean isForeignDetected() {
            return this.foreignDetected;
        }

        public boolean isStaticDetected() {
            return this.staticDetected;
        }

        public boolean isUnsafeDetected() {
            return this.unsafeDetected;
        }

        public boolean isSealedDetected() {
            return this.sealedDetected;
        }

        public boolean isRedefDetected() {
            return this.redefDetected;
        }

        public boolean isOpenDetected() {
            return this.openDetected;
        }

    }
}
