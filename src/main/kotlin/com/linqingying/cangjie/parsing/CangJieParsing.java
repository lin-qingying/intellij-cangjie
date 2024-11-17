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
    private static final TokenSet TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET = TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(SEMICOLON));
    private static final TokenSet LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET = TokenSet.orSet(TokenSet.create(LT, EQ, SEMICOLON), TOP_LEVEL_DECLARATION_FIRST);

    private static final TokenSet CLASS_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(LT, LPAR, COLON, LBRACE), TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(WHERE_KEYWORD, LPAR, COLON, LBRACE, GT);
    private static final TokenSet PACKAGE_NAME_RECOVERY_SET = TokenSet.create(DOT, EOL_OR_SEMICOLON);
    private static final TokenSet IMPORT_RECOVERY_SET = TokenSet.create(AS_KEYWORD, DOT, EOL_OR_SEMICOLON);
    private static final TokenSet TYPE_REF_FIRST = TokenSet.create(LBRACKET, IDENTIFIER, LPAR, HASH);
    private static final TokenSet LBRACE_RBRACE_TYPE_REF_FIRST_SET = TokenSet.orSet(TokenSet.create(LBRACE, RBRACE), TYPE_REF_FIRST);
    private static final TokenSet LTCOLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET = TokenSet.orSet(TokenSet.create(LTCOLON, COMMA, LBRACE, RBRACE), TYPE_REF_FIRST);
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
    private static final TokenSet PROPERTY_NAME_FOLLOW_MULTI_DECLARATION_RECOVERY_SET = TokenSet.orSet(PROPERTY_NAME_FOLLOW_SET, PARAMETER_NAME_RECOVERY_SET);
    private static final TokenSet PROPERTY_NAME_FOLLOW_FUNCTION_OR_PROPERTY_RECOVERY_SET = TokenSet.orSet(PROPERTY_NAME_FOLLOW_SET, LBRACE_RBRACE_SET, TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet IDENTIFIER_EQ_COLON_SEMICOLON_SET = TokenSet.create(IDENTIFIER, EQ, COLON, SEMICOLON);
    private static final TokenSet COMMA_RPAR_COLON_EQ_SET = TokenSet.create(COMMA, RPAR, COLON, EQ);
    private static final TokenSet ACCESSOR_FIRST_OR_PROPERTY_END = TokenSet.orSet(MODIFIER_KEYWORDS, TokenSet.create(GET_KEYWORD, SET_KEYWORD, EOL_OR_SEMICOLON, RBRACE));
    private static final TokenSet RPAR_IDENTIFIER_COLON_LBRACE_EQ_SET = TokenSet.create(RPAR, IDENTIFIER, COLON, LBRACE, EQ);
    private static final TokenSet COMMA_COLON_RPAR_SET = TokenSet.create(COMMA, COLON, RPAR);
    private static final TokenSet RPAR_COLON_LBRACE_EQ_SET = TokenSet.create(RPAR, COLON, LBRACE, EQ);
    private static final TokenSet LBRACKET_LBRACE_RBRACE_LPAR_SET = TokenSet.create(LBRACKET, LBRACE, RBRACE, LPAR);
    private static final TokenSet FUNCTION_NAME_FOLLOW_SET = TokenSet.create(LT, LPAR, RPAR, COLON, EQ);
    private static final TokenSet FUNCTION_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(LT, LPAR, RPAR, COLON, EQ), LBRACE_RBRACE_SET, TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet VALUE_PARAMETERS_FOLLOW_SET = TokenSet.create(EQ, LBRACE, RBRACE, SEMICOLON, RPAR);
    private static final TokenSet LPAR_VALUE_PARAMETERS_FOLLOW_SET = TokenSet.orSet(TokenSet.create(LPAR), VALUE_PARAMETERS_FOLLOW_SET);
    private static final TokenSet LPAR_LBRACE_COLON_CONSTRUCTOR_KEYWORD_SET = TokenSet.create(LPAR, LBRACE, COLON, INIT_KEYWORD);
    private static final TokenSet definitelyOutOfReceiverSet = TokenSet.orSet(TokenSet.create(EQ, COLON, LBRACE, RBRACE), TOP_LEVEL_DECLARATION_FIRST);
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
            TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(INIT_KEYWORD, GET_KEYWORD, SET_KEYWORD));

    private final static TokenSet MUT_PROP_SET = TokenSet.create(MUT_KEYWORD, PROP_KEYWORD);
    private final CangJieExpressionParsing myExpressionParsing;
    private final LastBefore lastDotAfterReceiverNotLParPattern =
            new LastBefore(new AtSet(RECEIVER_TYPE_TERMINATORS), new AbstractTokenStreamPredicate() {
                @Override
                public boolean matching(boolean topLevel) {
                    if (topLevel && (atSet(definitelyOutOfReceiverSet) || at(LPAR))) return true;
                    if (topLevel && at(IDENTIFIER)) {
                        IElementType lookahead = lookahead(1);
                        return lookahead != LT && lookahead != DOT && lookahead != QUEST;
                    }
                    return false;
                }
            });
    private final FirstBefore lastDotAfterReceiverLParPattern =
            new FirstBefore(new AtSet(RECEIVER_TYPE_TERMINATORS), new AbstractTokenStreamPredicate() {
                @Override
                public boolean matching(boolean topLevel) {
                    if (topLevel && atSet(definitelyOutOfReceiverSet)) {
                        return true;
                    }
                    return topLevel && !at(QUEST) && !at(LPAR) && !at(RPAR);
                }
            });

    private CangJieParsing(SemanticWhitespaceAwarePsiBuilder builder, boolean isTopLevel, boolean isLazy) {
        super(builder, isLazy);

        myExpressionParsing =
                isTopLevel ? new CangJieExpressionParsing(builder, this, isLazy) : new CangJieExpressionParsing(builder, this, isLazy) {


                    @Override
                    protected @NotNull CangJieParsing create(SemanticWhitespaceAwarePsiBuilder builder) {
                        return createForByClause(builder, isLazy);
                    }
                };


    }

    //    如果是声明文件，则不需要函数体

    private static CangJieParsing createForByClause(SemanticWhitespaceAwarePsiBuilder builder, boolean isLazy) {
        return new CangJieParsing(new SemanticWhitespaceAwarePsiBuilderForByClause(builder), false, isLazy);
    }

    static CangJieParsing createForTopLevel(SemanticWhitespaceAwarePsiBuilder builder) {
        return new CangJieParsing(builder, true, true);
    }

    static CangJieParsing createForTopLevelNonLazy(SemanticWhitespaceAwarePsiBuilder builder) {
        return new CangJieParsing(builder, true, false);
    }

    public CangJieExpressionParsing getExpressionParsing() {
        return myExpressionParsing;
    }

    void parseTypeRef() {
        parseTypeRef(TokenSet.EMPTY, false);
    }

    void parseTypeRefWithoutIntersections() {
        parseTypeRef(TokenSet.EMPTY);
    }

    void parseTest() {
//        SyntaxTreeBuilder.Marker a = mark();
        advance();
//        a.done(IMPORT_LIST);
    }

    @Override
    protected CangJieParsing create(SemanticWhitespaceAwarePsiBuilder builder) {
        return createForTopLevel(builder);
    }

    public void advanceBalancedBlock() {

        int braceCount = 1;
        while (!eof()) {
            if (_at(LBRACE)) {
                braceCount++;
            } else if (_at(RBRACE)) {
                braceCount--;
            }

            advance();

            if (braceCount == 0) {
                break;
            }
        }
    }

    private void parseThisOrSuper() {
        assert _at(THIS_KEYWORD) || _at(SUPER_KEYWORD);
        PsiBuilder.Marker mark = mark();

        advance(); // THIS_KEYWORD | SUPER_KEYWORD

        mark.done(CONSTRUCTOR_DELEGATION_REFERENCE);
    }

    private void parseInitFunctionBlock() {
        PsiBuilder.Marker lazyBlock = mark();

        myBuilder.enableNewlines();

//        恢复  init() xxxxxxx {}

        expect(LBRACE, "Expecting '{'  ");

        PsiBuilder.Marker delegationCall = mark();
        if ((at(THIS_KEYWORD) || at(SUPER_KEYWORD)) && rawLookup(1) == LPAR) {
            parseThisOrSuper();
            myExpressionParsing.parseValueArgumentList();
            delegationCall.done(CONSTRUCTOR_DELEGATION_CALL);

        } else {
            mark().done(CONSTRUCTOR_DELEGATION_REFERENCE);
            delegationCall.done(CONSTRUCTOR_DELEGATION_CALL);
        }


        myExpressionParsing.parseStatements();
        expect(RBRACE, "Expecting '}'");


        myBuilder.restoreNewlinesState();


        lazyBlock.done(INIT_BLOCK);

    }

    private void parseBlock(boolean collapse) {
        PsiBuilder.Marker lazyBlock = mark();

        myBuilder.enableNewlines();

        boolean hasOpeningBrace = expect(LBRACE, "Expecting '{'  ");
        boolean canCollapse = collapse && hasOpeningBrace && isLazy;


        if (canCollapse) {
            advanceBalancedBlock();
        } else {
            myExpressionParsing.parseStatements();
            expect(RBRACE, "Expecting '}'");
        }

        myBuilder.restoreNewlinesState();

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
        parseBlock(true);
    }

    void parseBlockExpression() {
        parseBlock(false);
    }

    /*
     *preamble
     *  : fileAnnotationList? packageDirective?
     *  ;
     */
    private void parsePreamble() {
        PsiBuilder.Marker firstEntry = mark();

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
        PsiBuilder.Marker packageDirective = mark();


//        是否有修饰符
        boolean istPackageAccessModifier = false;
        if (atSet(PACKAGE_ACCESS_MODIFIER_SET) && (lookahead(1) == MACRO_KEYWORD || lookahead(1) == PACKAGE_KEYWORD)) {
            advance(); //修饰符
            istPackageAccessModifier = true;
        }
        if (at(MACRO_KEYWORD)) {
            advance(); // MARCO_KEYWORD 宏声明
            istPackageAccessModifier = true;

        }

        if (at(PACKAGE_KEYWORD)) {


            if (at(PACKAGE_KEYWORD)) {
                advance(); // PACKAGE_KEYWORD
            } else if (istPackageAccessModifier) {
                error("Expecting package keyword");
            }


            //TODO 处理包名
            parsePackageName();

            firstEntry.drop();

            consumeIf(SEMICOLON);

            packageDirective.done(PACKAGE_DIRECTIVE);
        } else {
            //当忽略Package指令时，我们不应该在文件开头报告非文件批注的错误。
            //因此，我们回滚解析位置，重新解析文件注释列表，非文件注释没有上报错误。
            firstEntry.rollbackTo();

            //TODO 解析文件注解列表
//            parseFileAnnotationList(FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED);
            packageDirective = mark();
            packageDirective.done(PACKAGE_DIRECTIVE);
            //需要跳过除Shebang注释之外的所有内容，以允许将文件开头的注释绑定到第一个声明。
            packageDirective.setCustomEdgeTokenBinders(BindFirstShebangWithWhitespaceOnly.INSTANCE, null);


//          TODO   仓颉0.53.4 更新 位于包中的文件必须要包名
//            但是单文件可以没有

        }

        parseImportDirectives();
    }

    private void parseImportDirectives() {
        PsiBuilder.Marker importList = mark();

        if (!(at(IMPORT_KEYWORD) || atSet(IMPORT_ACCESS_MODIFIER_SET) && (lookahead(1) == IMPORT_KEYWORD))) {
            // this is necessary to allow comments at the start of the file to be bound to the first declaration
            importList.setCustomEdgeTokenBinders(DoNotBindAnything.INSTANCE, null);
        }
        while (at(IMPORT_KEYWORD) || atSet(IMPORT_ACCESS_MODIFIER_SET) && (lookahead(1) == IMPORT_KEYWORD)) {
            parseImportDirective();
        }
        importList.done(IMPORT_LIST);
    }

    private boolean closeImportWithErrorIfNewline(
            @Nullable PsiBuilder.Marker importDirective, @Nullable PsiBuilder.Marker importAlias, String errorMessage
    ) {
        if (myBuilder.newlineBeforeCurrentToken()) {
            if (importAlias != null) {
                importAlias.done(IMPORT_ALIAS);
            }
            error(errorMessage);
            if (importDirective != null) {
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
    private IElementType parseImportDirectiveItem(boolean isTopLevel, boolean isCreateMark) {


        PsiBuilder.Marker importDirectiveItem = null;
        if (isCreateMark) {
            importDirectiveItem = mark();
        }


        if (!at(IDENTIFIER)) {

            error("expected a package name after '.' in qualified name, found '" + myBuilder.getTokenText() + "'");

            if (importDirectiveItem != null) {
                importDirectiveItem.done(IMPORT_DIRECTIVE);
            }

            consumeIf(SEMICOLON);
            return IMPORT_DIRECTIVE;
        }

        PsiBuilder.Marker qualifiedName = mark();
        PsiBuilder.Marker reference = mark();
        advance(); // IDENTIFIER
        reference.done(REFERENCE_EXPRESSION);

        boolean isMulitImport = false;
        boolean isParseDot = false;

        while (at(DOT) && lookahead(1) != MUL) {
            advance(); // DOT

//            同一个包多个导入项
            if (at(LBRACE) && isTopLevel) {
                isMulitImport = true;
                advance();
                do {
                    expect(COMMA);
                    parseImportDirectiveItem(false, true);

                } while (at(COMMA));

                expect(RBRACE, "Expecting '}'");


            } else {


                isParseDot = true;
                if (closeImportWithErrorIfNewline(importDirectiveItem, null, "Import must be placed on a single line")) {
                    qualifiedName.drop();
                    return IMPORT_DIRECTIVE;
                }

                reference = mark();
                if (expect(IDENTIFIER, "Qualified name must be a '.'-separated identifier list", IMPORT_RECOVERY_SET)) {
                    reference.done(REFERENCE_EXPRESSION);
                } else {
                    reference.drop();
                }

                PsiBuilder.Marker precede = qualifiedName.precede();
                qualifiedName.done(DOT_QUALIFIED_EXPRESSION);
                qualifiedName = precede;
            }


        }
        qualifiedName.drop();

//        if (isTopLevel || !isParseDot) {

        if (at(DOT)) {
            advance(); // DOT
            assert _at(MUL);
            advance(); // MUL
            if (at(AS_KEYWORD)) {
                errorAndAdvance("Aliases are not allowed for all imports");
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
        } else if (at(AS_KEYWORD)) {
            PsiBuilder.Marker alias = mark();
            advance(); // AS_KEYWORD
            if (closeImportWithErrorIfNewline(importDirectiveItem, alias, "Expecting identifier")) {
                return IMPORT_DIRECTIVE;
            }
            expect(IDENTIFIER, "Expecting identifier", SEMICOLON_SET);
            alias.done(IMPORT_ALIAS);
        }

        if (importDirectiveItem != null) {
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


        assert _at(IMPORT_KEYWORD) || _atSet(IMPORT_ACCESS_MODIFIER_SET);

        IElementType doneType = IMPORT_DIRECTIVE;

        PsiBuilder.Marker importDirective = mark();


        if (_atSet(IMPORT_ACCESS_MODIFIER_SET)) {
            advance(); //PUBLIC_KEYWORD
        }


        if (!at(IMPORT_KEYWORD)) {

            error("Expecting 'import' keyword");
            importDirective.done(doneType);
            return;
        }


        advance(); // IMPORT_KEYWORD

        if (closeImportWithErrorIfNewline(importDirective, null, "Expecting qualified name")) {
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
        doneType = parseImportDirectiveItem(true, false);
//        }


        consumeIf(SEMICOLON);
        importDirective.done(doneType);
        importDirective.setCustomEdgeTokenBinders(null, TrailingCommentsBinder.INSTANCE);

    }

    /* SimpleName{"."} */
    private void parsePackageName() {

        PsiBuilder.Marker qualifiedExpression = mark();
        boolean simpleName = true;
        while (true) {
            if (myBuilder.newlineBeforeCurrentToken()) {
                errorWithRecovery("Package name must be a '.'-separated identifier list placed on a single line",
                        PACKAGE_NAME_RECOVERY_SET);
                break;
            }

            if (at(DOT)) {
                advance(); // DOT
                qualifiedExpression.error("Package name must be a '.'-separated identifier list");
                qualifiedExpression = mark();
                continue;
            }

            PsiBuilder.Marker nsName = mark();
            boolean simpleNameFound = expect(IDENTIFIER, "Package name must be a '.'-separated identifier list", PACKAGE_NAME_RECOVERY_SET);
            if (simpleNameFound) {
                nsName.done(REFERENCE_EXPRESSION);
            } else {
                nsName.drop();
            }

            if (!simpleName) {
                PsiBuilder.Marker precedingMarker = qualifiedExpression.precede();
                qualifiedExpression.done(DOT_QUALIFIED_EXPRESSION);
                qualifiedExpression = precedingMarker;
            }

            if (at(DOT)) {
                advance(); // DOT

                if (simpleName && !simpleNameFound) {
                    qualifiedExpression.drop();
                    qualifiedExpression = mark();
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
        PsiBuilder.Marker fileMarker = mark();
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
        PsiBuilder.Marker fileMarker = mark();

        //处理开头  package
        parsePreamble();

//        处理声明式语句
        while (!eof()) {
            parseTopLevelDeclaration();
        }
        checkUnclosedBlockComment();

        fileMarker.done(CJ_FILE);
    }

    void parseLspFile() {
        PsiBuilder.Marker fileMarker = mark();
//        将所有节点全都读取
        while (!eof()) {
            advance();
        }

        fileMarker.done(CJ_FILE);


    }

    private void checkUnclosedBlockComment() {
        if (BLOCK_DOC_COMMENT_SET.contains(myBuilder.rawLookup(-1))) {
            int startOffset = myBuilder.rawTokenTypeStart(-1);
            int endOffset = myBuilder.rawTokenTypeStart(0);
            CharSequence tokenChars = myBuilder.getOriginalText().subSequence(startOffset, endOffset);
            if (!(tokenChars.length() > 2 && tokenChars.subSequence(tokenChars.length() - 2, tokenChars.length()).toString().equals("*/"))) {
                PsiBuilder.Marker marker = myBuilder.mark();
                marker.error("Unclosed comment");
                marker.setCustomEdgeTokenBinders(WhitespacesBinders.GREEDY_RIGHT_BINDER, null);
            }
        }
    }

    private void parseTopLevelDeclaration() {
        parseTopLevelDeclaration(false);
    }

    /*
     * 顶层声明语句
     *   : function
     *   : class enum interface struct
     */
    private void parseTopLevelDeclaration(boolean parseMacro) {
        if (at(SEMICOLON)) {
            advance(); // SEMICOLON
            return;
        }


        PsiBuilder.Marker decl = mark();

//如果有导入语句

        if ((at(PUBLIC_KEYWORD) && lookahead(1) == IMPORT_KEYWORD)) {
//            error("imports are only allowed in the beginning of file");
            parseImportDirectives();
            decl.drop();

            return;
        }


        ModifierDetector detector = new ModifierDetector();

        parseModifierList(detector, TokenSet.EMPTY, parseMacro);
        IElementType declType = parseCommonDeclaration(detector, NameParsingMode.REQUIRED, DeclarationParsingMode.TOPLEVEL);

 /*       if (declType == ANNOTATION_ENTRY) {

            decl.rollbackTo(); //返回给文档流并重新解析
//            decl.drop();

//           应该为注解，加入到修饰符中并重新解析声明
            parseTopLevelDeclaration(true);

        } else*/
        if (declType == null) {

            errorAndAdvance("Expecting a top level declaration"); //期待一个顶层声明语句
//            decl.error("Expecting a top level declaration");
            decl.drop();
        } else {
            closeDeclarationWithCommentBinders(decl, declType, true);
        }


    }

    private boolean tryParseModifier(
            @Nullable Consumer<IElementType> tokenConsumer, @NotNull TokenSet noModifiersBefore, @NotNull TokenSet modifierKeywords
    ) {
        PsiBuilder.Marker marker = mark();

        if (atSet(modifierKeywords)) {
            IElementType lookahead = lookahead(1);

            if (at(FUNC_KEYWORD) && lookahead != INTERFACE_KEYWORD) {
                marker.rollbackTo();
                return false;
            }

            if (lookahead != null && !noModifiersBefore.contains(lookahead)) {
                IElementType tt = tt();
                if (tokenConsumer != null) {
                    tokenConsumer.consume(tt);
                }
                advance(); // MODIFIER
                marker.collapse(tt);
                return true;
            }
        } else if (at(CONST_KEYWORD) && /* lookahead(1) != IDENTIFIER && */lookahead(2) != EQ && lookahead(2) != COLON /* && (lookahead(2) == FUNC_KEYWORD || lookahead(3) == FUNC_KEYWORD || lookahead(4) == FUNC_KEYWORD)*/) {


//            处理特殊的const修饰的
            advance(); // MODIFIER
            if (tokenConsumer != null) {
                tokenConsumer.consume(CONST_KEYWORD);
            }
            marker.collapse(CONST_KEYWORD);
            return true;

        } else if (at(UNSAFE_KEYWORD) && lookahead(1) != LBRACE) {
            advance();

            if (tokenConsumer != null) {
                tokenConsumer.consume(UNSAFE_KEYWORD);
            }
            marker.collapse(UNSAFE_KEYWORD);

            return true;

        } else if (at(FOREIGN_KEYWORD) && lookahead(1) != LBRACE) {
            advance();
            if (tokenConsumer != null) {
                tokenConsumer.consume(FOREIGN_KEYWORD);
            }
            marker.collapse(FOREIGN_KEYWORD);

            return true;

        }
        marker.rollbackTo();
        return false;
    }

    private boolean doParseModifierListBody(
            @Nullable Consumer<IElementType> tokenConsumer,
            @NotNull TokenSet modifierKeywords,

            @NotNull TokenSet noModifiersBefore

    ) {
        return doParseModifierListBody(tokenConsumer, modifierKeywords, noModifiersBefore, false);
    }

    private boolean doParseModifierListBody(
            @Nullable Consumer<IElementType> tokenConsumer,
            @NotNull TokenSet modifierKeywords,

            @NotNull TokenSet noModifiersBefore,
            boolean isParseMacro
    ) {

        boolean empty = true;
        PsiBuilder.Marker beforeAnnotationMarker;

        while (!eof()) {

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
            if (!tryParseModifier(tokenConsumer, noModifiersBefore, modifierKeywords)) {
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
        myExpressionParsing.parseFunctionLiteral(/* preferBlock = */ false, /* collapse = */false, false);
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
    public IElementType parseAnnotation(ModifierDetector detector) {
//        detector们没有修饰符
//        该宏调用没有括号     解析为注解

//        如果没有修饰符,根据是否有括号判断

        assert _at(AT);
        IElementType nextRawToken = lookahead(1);


        int modifierSize = 0;
        if (detector != null) {
            modifierSize = detector.getSize();
        }
//        PsiBuilder.Marker annotation = mark();
        if (nextRawToken == IDENTIFIER) {
            advance(); // AT
            PsiBuilder.Marker reference = mark();
            PsiBuilder.Marker typeReference = mark();
            parseUserType();
            typeReference.done(TYPE_REFERENCE);
            reference.done(CONSTRUCTOR_CALLEE);
//            宏属性
            if (at(LBRACKET)) {
                myExpressionParsing.parseValueArgumentList(LBRACKET, RBRACKET);
            }
        } else {
            errorAndAdvance("Expected annotation identifier after '@'", 1); // AT @
//            annotation.drop();
            return null;
        }
//        if (modifierSize > 0) {
////            处理宏调用
//            return MACRO_EXPRESSION;
//        }else {
//            return ANNOTATION_ENTRY;
//        }
        if (at(LPAR)) {
            //            TODO 处理宏调用
            advance();
//          该语句应该是宏调用表达式，而非注解
            if (at(RPAR)) {
//                直接返回
                advance();
            } else {
                while (!eof()) {
                    if (at(COMMA)) {
                        advance();
                    }
//                    parseTopLevelDeclaration();
                    myExpressionParsing.parseStatementByScope(DeclarationParsingMode.ALL);
                    if (at(RPAR)) {
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
                expect(RPAR, "expected ')'");
            }
//            parseMacroInputExprWithParens();
            return MACRO_EXPRESSION;
        } else {
//            return ANNOTATION_ENTRY;
            if (modifierSize > 0) {
                error("Should call (..) for macros");

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


    boolean parseModifierList(@Nullable Consumer<IElementType> tokenConsumer, @NotNull TokenSet noModifiersBefore) {
        return parseModifierList(tokenConsumer, noModifiersBefore, false);

    }

    /**
     * (modifier )*
     * <p>
     * 如果不为空，则将修饰符(非批注)馈送到传递的使用者
     *
     * @param noModifiersBefore 是一个令牌集，其中包含指示何时满足这些元素的元素。
     *                          必须将前一个令牌解析为标识符，而不是修饰符
     */
    boolean parseModifierList(@Nullable Consumer<IElementType> tokenConsumer, @NotNull TokenSet noModifiersBefore, boolean isParseMacro) {


        return doParseModifierList(tokenConsumer, MODIFIER_KEYWORDS, noModifiersBefore, isParseMacro);
    }

    private boolean doParseModifierList(
            @Nullable Consumer<IElementType> tokenConsumer,
            @NotNull TokenSet modifierKeywords,

            @NotNull TokenSet noModifiersBefore
    ) {
        return doParseModifierList(tokenConsumer, modifierKeywords, noModifiersBefore, false);
    }

    private boolean doParseModifierList(
            @Nullable Consumer<IElementType> tokenConsumer,
            @NotNull TokenSet modifierKeywords,

            @NotNull TokenSet noModifiersBefore,
            boolean isParseMacro
    ) {
        PsiBuilder.Marker list = mark();

        boolean empty = doParseModifierListBody(tokenConsumer, modifierKeywords, noModifiersBefore, isParseMacro);

        if (empty) {
            list.drop();
        } else {
            list.done(MODIFIER_LIST);
        }
        return !empty;
    }

    private IElementType parseClassCommonDeclaration(Integer tokenId, ModifierDetector classdetector, ModifierDetector detector) {
        //init func let|var prop


        return switch (getTokenId()) {
            case AT_Id -> myExpressionParsing.parseMacroExpression(true);
//                    parseAnnotation(null);
            case FUNC_KEYWORD_Id ->
                    tokenId != null ? tokenId == INTERFACE_KEYWORD_Id ? parseFunction(true, classdetector, detector, tokenId) : parseFunction(classdetector, detector, tokenId) : parseFunction(detector, tokenId);


//                    tokenId != null && (tokenId == INTERFACE_KEYWORD_Id || tokenId == EXTEND_KEYWORD_Id) ?  parseFunction(true,classdetector, detector):parseFunction( ) ;
            case PROP_KEYWORD_Id ->
                    tokenId != null && (tokenId == INTERFACE_KEYWORD_Id /*|| tokenId == EXTEND_KEYWORD_Id*/) ? parseProperty(true, classdetector, detector) : parseProperty(classdetector, detector);
            case LET_KEYWORD_Id, VAR_KEYWORD_Id, CONST_KEYWORD_Id -> parseVariable(classdetector,DeclarationParsingMode.MEMBER);
            default -> null;
        };
    }

    private IElementType parseClassInitializer() {

        return null;
    }

    private boolean parsePropertyDelegateOrAssignment() {
        if (at(EQ)) {
            advance(); // EQ
            myExpressionParsing.parseExpression();
            return true;
        }

        return false;
    }

    private int lastDotAfterReceiver() {
        AbstractTokenStreamPattern pattern = at(LPAR) ? lastDotAfterReceiverLParPattern : lastDotAfterReceiverNotLParPattern;
        pattern.reset();
        return matchTokenStreamPredicate(pattern);
    }

    private boolean parseReceiverType(String title, TokenSet nameFollow) {

        int lastDot = lastDotAfterReceiver();
        boolean receiverPresent = lastDot != -1;


        if (!receiverPresent) return false;

        createTruncatedBuilder(lastDot).parseTypeRef();

        if (atSet(RECEIVER_TYPE_TERMINATORS)) {
            advance(); // expectation
        } else {
            errorWithRecovery("Expecting '.' before a " + title + " name", nameFollow);
        }
        return true;
    }

    public IElementType parseVariable(ModifierDetector classdetector) {
        return parseVariable(classdetector, null);
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
    public IElementType parseVariable(ModifierDetector classdetector, @Nullable DeclarationParsingMode declarationParsingMode) {
        assert (at(LET_KEYWORD) || at(VAR_KEYWORD) || at(CONST_KEYWORD));
        advance();


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
        if (/*declarationParsingMode == DeclarationParsingMode.TOPLEVEL ||*/ declarationParsingMode == DeclarationParsingMode.MEMBER) {
            parseIdentifierByTitle("variable", PROPERTY_NAME_FOLLOW_SET, true);
        } else {
            myExpressionParsing.parsePattern(new PatternConfig(true), Pattern.Wildcard.INSTANCE, Pattern.Binding.INSTANCE, Pattern.Tuple.INSTANCE, Pattern.Enum.INSTANCE);

        }

        boolean noTypeReference = true;

        //类型 (:type)可以没有，但是默认值必须有

        if (at(COLON)) {
            advance(); // COLON
            noTypeReference = false;

            parseTypeRef();

        }


        if (at(EQ)) {
            advance(); // COLON

            //处理表达式
//myExpressionParsing.test();
            myExpressionParsing.parseExpression();
        } else if (noTypeReference) {
            errorAndAdvance("variable in top-level scope must be initialized");
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
        PsiBuilder.Marker marker = mark();
        myExpressionParsing.parseExpression();

        checkForUnexpectedSymbols();

        marker.done(EXPRESSION_CODE_FRAGMENT);
    }

    void parseBlockCodeFragment() {
        PsiBuilder.Marker marker = mark();
        PsiBuilder.Marker blockMarker = mark();

        if (at(PACKAGE_KEYWORD) || at(IMPORT_KEYWORD)) {
            PsiBuilder.Marker err = mark();
            parsePreamble();
            err.error("Package directive and imports are forbidden in code fragments");
        }

        myExpressionParsing.parseStatements();

        checkForUnexpectedSymbols();

        blockMarker.done(BLOCK);
        marker.done(BLOCK_CODE_FRAGMENT);
    }

    public IElementType parseCommonDeclaration(
            @NotNull ModifierDetector detector,
            @NotNull NameParsingMode nameParsingMode,
            @NotNull DeclarationParsingMode declarationParsingMode
    ) {

//       TODO 声明作用域判断

        return switch (getTokenId()) {

            case TYPE_KEYWORD_Id ->
//                if (declarationParsingMode == DeclarationParsingMode.LOCAL) {
//                    yield null;
//                } else {
//                    yield parseTypeAlias();
//                }

                    switch (declarationParsingMode) {
                        case LOCAL, MEMBER, MEMBER_OR_TOPLEVEL -> {
                            parseTypeAlias();
                            yield INVALID_DECLARATION;
                        }
                        case ALL, TOPLEVEL -> parseTypeAlias();


                    };


            case AT_Id -> myExpressionParsing.parseMacroExpression(true);
//                    parseAnnotation(detector);


            case FOREIGN_KEYWORD_Id ->
//                if (declarationParsingMode == DeclarationParsingMode.LOCAL) {
//                    yield null;
//                } else {
//                    yield parseForeign();
//                }

                    switch (declarationParsingMode) {

                        case ALL, TOPLEVEL -> parseForeign();

                        case MEMBER, MEMBER_OR_TOPLEVEL, LOCAL -> {
                            parseForeign();
                            yield INVALID_DECLARATION;
                        }

                    };

            case MACRO_KEYWORD_Id -> parseMacro();

//            case ABC_KEYWORD_Id:
//                return parseAbc();
            case FUNC_KEYWORD_Id -> parseFunction(detector);
            case MAIN_KEYWORD_Id ->
//                if (declarationParsingMode == DeclarationParsingMode.LOCAL) {
//                    yield null;
//                } else {
//                    yield parseMainFunc();
//                }

                    switch (declarationParsingMode) {

                        case ALL, TOPLEVEL -> parseMainFunc();

                        case MEMBER, MEMBER_OR_TOPLEVEL, LOCAL -> {
                            parseMainFunc();
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

                        case ALL, TOPLEVEL -> parseClass(detector);

                        case MEMBER, MEMBER_OR_TOPLEVEL, LOCAL -> {
                            parseClass(detector);
                            yield INVALID_DECLARATION;
                        }

                    };


            case LET_KEYWORD_Id, VAR_KEYWORD_Id, CONST_KEYWORD_Id -> parseVariable(detector, declarationParsingMode);
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
        assert _at(TYPE_KEYWORD);

        advance(); // TYPE_KEYWORD

        expect(IDENTIFIER, "Type name expected", LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET);

        parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET);

        if (at(WHERE_KEYWORD)) {
            PsiBuilder.Marker error = mark();
            parseTypeConstraints();
            error.error("Type alias parameters can't have bounds");
        }

        expect(EQ, "Expecting '='", TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET);

        parseTypeRef();

        consumeIf(SEMICOLON);

        return TYPEALIAS;
    }

    IElementType parseProperty(boolean isInterface) {
        return parseProperty(isInterface, null, null);
    }

    private IElementType parseProperty() {
        return parseProperty(false);
    }


//    private IElementType parseAbc() {
//        assert _at(ABC_KEYWORD);
//        advance();
//        return null;
//    }

    IElementType parseProperty(ModifierDetector classdetector, ModifierDetector detector) {
        return parseProperty(false, classdetector, detector);
    }

    /*
     * prop
     *   :  "mnt"? prop Identifier :Type propBody
     *   ;
     */
    private IElementType parseProperty(boolean isInterface, ModifierDetector classdetector, ModifierDetector detector) {
        assert _at(PROP_KEYWORD);


//        bool isMut = false;
//        if (at(MUT_KEYWORD)) {
//            isMut = true;
//            advance();
//        }
//        if (!at(PROP_KEYWORD)) {
//            errorAndAdvance("Expecting 'prop'");
//
//        }
        advance();

        parseIdentifierByTitle(" prop ");

//        if(at(COLON)){
        parseByType();
//        }else {
//            error("Expecting ':'");
//        }

        if (isDeclarationsFile) {

            if (at(LBRACE)) {
                PsiBuilder.Marker body = mark();

                TokenSet tokenSet = TokenSet.orSet(
                        KEYWORDS, TokenSet.create(OPEN_KEYWORD,
                                ABSTRACT_KEYWORD,
                                SEALED_KEYWORD)
                );
                while (!atSet(tokenSet) && !eof()) {
                    advance();
                }
                body.error("Property body is not allowed in declarations file");

            }
            return PROPERTY;
        }

        if (at(LBRACE)) {
            parsePropertyBody(detector);
        } else if (!isInterface) {
            if (classdetector != null && !classdetector.isAbstractDetected()) {
                error("unimplemented abstract property");
                error("Missing prop body Expecting '{'");
            }

        }


        return PROPERTY;


    }

    /**
     * ":" type
     */
    private void parseByType() {
//        assert  _at(COLON);
        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        } else {
            error("Missing type Expecting ':' type");
        }
    }

    /**
     * prop get
     * :  "get" "(" ")" block
     */
    private void parsePropertyGet() {
        assert _at(GET_KEYWORD);

        PsiBuilder.Marker get = mark();
        advance(); // GET_KEYWORD

        if (expect(LPAR, "Expecting '('")) {

            if (expect(RPAR, "Expecting ')'")) {
                if (at(LBRACE)) {
                    parseBlock();
                } else {
                    error("Expecting '{'");
                }
            }
        }

        get.done(PROPERTY_ACCESSOR);

    }

    /**
     * prop set
     * :  "set" "(" Identifier ")" block
     */
    private void parsePropertySet(ModifierDetector detector) {
        assert _at(SET_KEYWORD);
        PsiBuilder.Marker set = mark();

//        if(detector.isMutDetected()){
        advance(); // SET_KEYWORD


        if (expect(LPAR, "Expecting '('")) {
            SyntaxTreeBuilder.Marker plist = mark();
            SyntaxTreeBuilder.Marker value = mark();

            expect(IDENTIFIER, "Expecting identifier");
            value.done(VALUE_PARAMETER);
            plist.done(VALUE_PARAMETER_LIST);
            if (expect(RPAR, "Expecting ')'")) {
                if (at(LBRACE)) {
                    parseBlock();
                } else {
                    error("Expecting '{'");
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
    private void parsePropertyBody(ModifierDetector detector) {
        assert _at(LBRACE);


        PsiBuilder.Marker body = mark();
        advance(); // LBRACE
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
        while (at(GET_KEYWORD) || at(SET_KEYWORD)) {
            parsePropertyAccessor();

        }

        expect(RBRACE, "Expecting '}'");

        body.done(PROPERTY_BODY);

    }

    private void parsePropertyAccessor() {

        if (at(GET_KEYWORD)) {

            parsePropertyGet();
        }

        if (at(SET_KEYWORD)) {

            parsePropertySet(null);
        }


    }

    /*
     * enum
     *   : "enum" SimpleName ("{" enumEntry((Type)?){"|"}  "}")
     *   ;
     */
    private IElementType parseEnum() {
        assert _at(ENUM_KEYWORD);
        advance();

        parseIdentifierByTitle("enum", IDENTIFIER_RBRACKET_LBRACKET_SET, false);


        parseEnumBody();

        return ENUM;
    }

    private void parseEnumBody() {
        PsiBuilder.Marker body = mark();
        if (at(LBRACE)) {
            advance(); // LBRACE

            expect(OR);

            if (at(IDENTIFIER)) {
                parseEnumList();
            } else {
                error("Expecting enum entry");
            }

            parseMembers(null, null);


            expect(RBRACE, "Expecting '}'");
        } else {
            error("Expecting '{'");
        }
        body.done(ENUM_BODY);
    }

    private void parseEnumList() {


        while (true) {

            parseEnumEntry();
            if (at(RBRACE)) {
                break;
            }
            if (at(OR)) {
                advance();
            } else {
                break;
            }

        }


    }

    void parseTypeCodeFragment() {
        PsiBuilder.Marker marker = mark();
        parseTypeRef();

        checkForUnexpectedSymbols();

        marker.done(TYPE_CODE_FRAGMENT);
    }

    private void checkForUnexpectedSymbols() {
        while (!eof()) {
            errorAndAdvance("Unexpected symbol");
        }
    }

    boolean parseEnumEntry() {
        return parseEnumEntry(true);
    }

    boolean parseEnumEntry(boolean isCreateMark) {
        PsiBuilder.Marker entry = null;
        if (isCreateMark) {
            entry = mark();

        }

        if (!expect(IDENTIFIER, "Expecting enum entry name")) {
            if (isCreateMark) {
                entry.drop();
            }
            return false;
        }


//        parseIdentifierByTitle("enum entry", IDENTIFIER_RBRACKET_LBRACKET_SET);


//        处理泛型
//        parseTypeArgumentList();


        if (at(LPAR)) {
            advance(); // LPAR
            parseTypeList();

            expect(RPAR, "Expecting ')'");
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
        PsiBuilder.Marker list = mark();

        while (true) {
            parseTypeRef();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        list.done(TYPE_LIST);
    }

    /*
     * typeParameters
     *   : ("<" typeParameter{","} ">"
     *   ;
     */
    private boolean parseTypeParameterList(TokenSet recoverySet) {

        boolean result = false;
        if (at(LT)) {
            PsiBuilder.Marker list = mark();

            myBuilder.disableNewlines();
            advance(); // LT

            while (true) {
                if (at(COMMA)) errorAndAdvance("Expecting type parameter declaration");
                parseTypeParameter();
//                parseTypeRef(true);

                if (!at(COMMA)) break;
                advance(); // COMMA
                if (at(GT)) {
                    break;
                }
            }

            expect(GT, "Missing '>'", recoverySet);
            myBuilder.restoreNewlinesState();
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
        if (atSet(TYPE_PARAMETER_GT_RECOVERY_SET)) {
            error("Type parameter declaration expected");
            return;
        }

        PsiBuilder.Marker mark = mark();

//        parseModifierList(GT_COMMA_COLON_SET);

        expect(IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY);

//        if (at(COLON)) {
//            advance(); // COLON
//            parseTypeRef();
//        }

        mark.done(TYPE_PARAMETER);
    }

    private void parseDelegationSpecifier() {
        PsiBuilder.Marker delegator = mark();
        PsiBuilder.Marker reference = mark();
        parseTypeRef();


        reference.drop();
        delegator.done(SUPER_TYPE_ENTRY);

    }

    /*
     * delegationSpecifier{"&"}
     */
    private void parseDelegationSpecifierList() {
        PsiBuilder.Marker list = mark();

        while (true) {
            if (at(AND)) {
                errorAndAdvance("Expecting a delegation specifier");
                continue;
            }
            parseDelegationSpecifier();
            if (!at(AND)) break;
            advance(); // COMMA
        }

        list.done(SUPER_TYPE_LIST);
    }

    /*
     * (modifier)*
     */
    boolean parseModifierList(@NotNull TokenSet noModifiersBefore) {
        return parseModifierList(null, noModifiersBefore);
    }


    /*
     * class
     *   : "class" SimpleName (<: delegationSpecifier{"&"}) classBody
     *   ;
     */
    IElementType parseClass(@NotNull ModifierDetector detector) {

        int tokenid = getTokenId();

        IElementType token = myBuilder.getTokenType();


//        assert _at(CLASS_KEYWORD);
        assert _atSet(CLASS_INTERFACE_STRUCT_ENUM_EXTEND_SET);
        advance();


        boolean typeParametersDeclared = false;

        if (token == EXTEND_KEYWORD) {

            if (at(LT)) {
                parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET);
                typeParametersDeclared = true;
            }


//            if (atSet(BASICTYPES) || at(IDENTIFIER)) {
            parseTypeRef();

//            }  else {
//
//                error("Expecting a type");
//            }
        } else {
            parseIdentifier(); //类名
            typeParametersDeclared = parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET);
        }


        // TODO 继承
        if (at(LTCOLON)) {
            advance(); // COLON
            parseDelegationSpecifierList();
        }


        OptionalMarker whereMarker = new OptionalMarker(false);
        parseTypeConstraintsGuarded(typeParametersDeclared);
        whereMarker.error("Where clause is not allowed");


        if (at(LBRACE)) {
            switch (tokenid) {
                case ENUM_KEYWORD_Id:
                    parseEnumBody();
                    break;
                case EXTEND_KEYWORD_Id:
                case STRUCT_KEYWORD_Id:
                case INTERFACE_KEYWORD_Id:
                case CLASS_KEYWORD_Id:
                    parseClassBody(tokenid, detector);
                    break;

                default:
                    parseClassBody(tokenid, detector);


            }

        } else {
            error("Expecting '{' or Inherit");  //应该为'{' 或者继承
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
    private void parseTypeConstraintsGuarded(boolean typeParameterListOccurred) {
        PsiBuilder.Marker error = mark();
        boolean constraints = parseTypeConstraints();
        errorIf(error, constraints && !typeParameterListOccurred, "Type constraints are not allowed when no type parameters declared");
    }

    private boolean parseTypeConstraints() {
        if (at(WHERE_KEYWORD)) {
            parseTypeConstraintList();
            return true;
        }
        return false;
    }

    /*
     * typeConstraint{","}
     */
    private void parseTypeConstraintList() {
        assert _at(WHERE_KEYWORD);

        advance(); // WHERE_KEYWORD

        PsiBuilder.Marker list = mark();

        while (true) {
            if (at(COMMA)) errorAndAdvance("Type constraint expected");
            parseTypeConstraint();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        list.done(TYPE_CONSTRAINT_LIST);
    }

    /*
     * typeConstraint
     *   :   SimpleName "<:" type
     *   ;
     */
    private void parseTypeConstraint() {
        PsiBuilder.Marker constraint = mark();


        PsiBuilder.Marker reference = mark();
        if (expect(IDENTIFIER, "Expecting type parameter name", LTCOLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET)) {
            reference.done(REFERENCE_EXPRESSION);
        } else {
            reference.drop();
        }

        expect(LTCOLON, "Expecting '<:' before the upper bound", LBRACE_RBRACE_TYPE_REF_FIRST_SET);


        do {
            if (at(AND)) advance();
            parseTypeRef();

        } while (at(AND));


        constraint.done(TYPE_CONSTRAINT);
    }

    private void parseMemberDeclaration(Integer tokenId, ModifierDetector classdetector) {
        parseMemberDeclaration(tokenId, classdetector, false);
    }

    private void parseMemberDeclaration(Integer tokenId, ModifierDetector classdetector, boolean rollbackMacro) {
        if (at(SEMICOLON)) {
            advance(); // SEMICOLON
            return;
        }
        PsiBuilder.Marker decl = mark();


        ModifierDetector detector = new ModifierDetector();
        parseModifierList(detector, TokenSet.EMPTY, rollbackMacro);


        IElementType declType = parseMemberDeclarationRest(tokenId, classdetector, detector);

      /*  if (declType == ANNOTATION_ENTRY) {
            decl.rollbackTo();
            parseMemberDeclaration(tokenId, classdetector, true);
        } else*/
        if (declType == null) {
            errorWithRecovery("Expecting member declaration", TokenSet.EMPTY);
            decl.drop();
        } else {
            closeDeclarationWithCommentBinders(decl, declType, true);
        }
    }

    private IElementType parseMemberDeclarationRest(Integer tokenId, ModifierDetector classdetector, ModifierDetector detector) {
        IElementType declType = parseClassCommonDeclaration(tokenId, classdetector, detector);

        if (declType != null) return declType;

        if (tokenId != null && tokenId != INTERFACE_KEYWORD_Id) {

            parseModifierList(TokenSet.EMPTY);

            if (at(INIT_KEYWORD)) {
                parseInitFunc();
                declType = SECONDARY_CONSTRUCTOR;
            } else if (at(LBRACE)) {
                error("Expecting member declaration");
                parseBlock();
                declType = FUNC;
            } else if (at(IDENTIFIER) && lookahead(1) == LPAR) {
//                主构造函数
                parseMainInitFunc();
                declType = PRIMARY_CONSTRUCTOR;
            } else if (at(TILDE) && lookahead(1) == INIT_KEYWORD) {
                advance(); // TILDE ~
                parseInitFunc();
//                析构函数
                declType = END_SECONDARY_CONSTRUCTOR;

            }

        }


        return declType;
    }

    void parseMainInitFunc() {
        assert _at(IDENTIFIER);
        advance(); // IDENTIFIER


        if (at(RBRACE)) {
            error("Function body expected");  //应该为函数体
            return;
        }

        myBuilder.disableJoiningComplexTokens();
        //类型参数
        if (at(LPAR)) {
            parseInitFuncValueParameterList();
        } else {
//            error("Expecting '(' ");  //应该为'('
            errorAndAdvance("Expecting '('  but available" + myBuilder.getTokenText());

        }


//        if (at(LBRACE)) {
        parseInitFunctionBody();
//        } else {
//            error("Expecting '{' ");  //应该为'{'
//        }
    }

    void parseInitFunc() {
        assert _at(INIT_KEYWORD);
        advance(); // INIT_KEYWORD


        if (at(RBRACE)) {
            error("Function body expected");  //应该为函数体
            return;
        }

        myBuilder.disableJoiningComplexTokens();
        //类型参数
        if (at(LPAR)) {
            parseInitFuncValueParameterList();
        } else {
//            error("Expecting '(' ");  //应该为'('
            errorAndAdvance("Expecting '('  but available" + myBuilder.getTokenText());
        }

        if (isDeclarationsFile) {
            if (at(LBRACE)) {
                PsiBuilder.Marker body = mark();
                while (!atSet(KEYWORDALL) && !eof()) {
                    advance();
                }

//                parseFunctionBody();

                body.error("Method bodies are not allowed in declaration files");


            }
            return;
        }


//        if (at(LBRACE)) {
        parseInitFunctionBody();
//        } else {
//            error("Expecting '{' ");  //应该为'{'
//        }
    }

    /**
     * members
     * : memberDeclaration*
     * ;
     */
    private void parseMembers(Integer tokenId, ModifierDetector detector) {
        while (!eof() && !at(RBRACE)) {
            parseMemberDeclaration(tokenId, detector);
        }
    }

    private void parseClassBody(Integer tokenId, @NotNull ModifierDetector detector) {
        PsiBuilder.Marker body = mark();

        myBuilder.enableNewlines();

        if (expect(LBRACE, "Expecting a class body")) {
            parseMembers(tokenId, detector);
            expect(RBRACE, "Missing '}");
        }

        myBuilder.restoreNewlinesState();

        body.done(CLASS_BODY);
    }

    private IElementType parseMainFunc() {
        assert _at(MAIN_KEYWORD);
        advance();
        if (at(RBRACE)) {
            error("Function body expected");  //应该为函数体
            return MAIN_FUNC;
        }
        myBuilder.disableJoiningComplexTokens();
        //类型参数
        if (at(LPAR)) {
            parseValueParameterList(false, false, VALUE_PARAMETERS_FOLLOW_SET);

        } else {
            error("Expecting '(' ");  //应该为'('
        }

        //返回值类型
        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        }

        //函数体
//        if (at(SEMICOLON)) {
//            advance(); // SEMICOLON
//        } else
        if (at(LBRACE)) {
            parseFunctionBody();
        } else {
            error("Expecting '{' ");  //应该为'{'
        }
        return MAIN_FUNC;
    }

    @NotNull
    IElementType parseFunction() {
        return parseFunction(false, null, null, false, 0);
    }

    @NotNull
    IElementType parseFunction(boolean isInterfaceMethod, ModifierDetector classdetector, ModifierDetector detector, Integer topTokenId) {
        return parseFunction(isInterfaceMethod, classdetector, detector, false, topTokenId);
    }

    @NotNull
    IElementType parseFunction(boolean isInterfaceMethod, Integer topTokenId) {
        return parseFunction(isInterfaceMethod, null, null, false, topTokenId);
    }

    @NotNull
    IElementType parseFunction(ModifierDetector detector
    ) {
        return parseFunction(detector, 0);
    }

    @NotNull
    IElementType parseFunction(ModifierDetector detector, Integer topTokenId
    ) {
        return parseFunction(false, null, detector, false, topTokenId);
    }

    @NotNull
    IElementType parseForeignFunction(ModifierDetector detector) {
        return parseForeignFunction(detector, 0);
    }

    @NotNull
    IElementType parseForeignFunction(ModifierDetector detector, Integer topTokenId) {
        return parseFunction(false, null, detector, true, topTokenId);
    }

    @NotNull
    IElementType parseFunction(ModifierDetector classdetector, ModifierDetector detector, Integer topTokenId) {
        return parseFunction(false, classdetector, detector, false, topTokenId);
    }

    /*
     * IDENTIFIER 标识符
     */
    private void parseIdentifier() {
        if (expect(IDENTIFIER)) return;

        if (atSet(KEYWORDS)) {
            error("Keywords cannot be used"); //关键字不能使用
        }

        if (!at(LPAR)) {
            errorAndAdvance("Expecting an CangJie identifier");
            return;
        }

        error("Expecting an CangJie identifier"); //应该为标识符

    }

    /*
     * IDENTIFIER
     */
    private void parseIdentifierByTitle(
            String title, TokenSet recoverySet, boolean isUnderline
    ) {

        if (isUnderline && expect(CangJieExpressionParsing.IDENTIFIER_RECOVERY_SET)) {
            return;
        }

        if (expect(IDENTIFIER)) {
            return;
        }

        errorWithRecovery("Expecting " + title + " name", recoverySet);
    }

    private void parseIdentifierByTitle(String title) {
        parseIdentifierByTitle(title, TokenSet.EMPTY, false);
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
    IElementType parseFunction(boolean isInterfaceMethod, ModifierDetector classdetector, ModifierDetector detector, boolean isForeign, Integer topTokenId) {


//        if (detector != null) {
//            if (detector.isOperatorDetected() && classdetector == null) {
////              detector.OPERATOR_MARK.error("unexpected modifier 'operator' on function declaration in 'top-level' scope");
//
//            }
//        }
        assert _at(FUNC_KEYWORD);
        advance();
        IElementType type = FUNC;
        if (topTokenId != null && EXTEND_KEYWORD_Id == topTokenId) {
            type = FUNC_EXTEND;
        }

        if (at(RBRACE)) {
            error("Function body expected");  //应该为函数体
            return type;
        }


        myBuilder.disableJoiningComplexTokens();


        if (detector != null && detector.isOperatorDetected()) {

//            if (classdetector == null) {
//                error("unexpected modifier 'operator' on function declaration in 'top-level' scope");
//            } else

            //运算符重载

            IElementType operatorToken = getOperationTokenType();

            if (OPERATIONS_CAN_BE_OVERLOADED.contains(operatorToken)) {
                PsiBuilder.Marker operator = mark();


                advanceOperationToken(operatorToken);

                operator.done(OPERATION_NAME);
            } else {

                PsiBuilder.Marker mark = mark();
                advance();
                mark.error("Should be an overloaded operator");

//              return FUNC;

            }
        } /*else if (atSet(OPERATIONS_CAN_BE_OVERLOADED) && detector != null && !detector.isOperatorDetected()) {

//            PsiBuilder.Marker mark = mark();
//            mark.error("Missing modifier as 'operator'");
//            if(at(LPAR) || at(LBRACKET)){
//                error("Missing modifier as 'operator'");
//            }
            if (at(LPAR) || at(LBRACKET)) {
                errorAndAdvance("Missing modifier as 'operator'", 2);
            } else {
                errorAndAdvance("Missing modifier as 'operator'");

            }


//            return FUNC;
        } */ else {
            //函数名
            myBuilder.getTokenType();
            parseIdentifier();
        }


//        expect(EXCL);

        boolean typeParameterListOccurred = false;
        if (at(LT)) {
            parseTypeParameterList(LBRACKET_LBRACE_RBRACE_LPAR_SET);
            typeParameterListOccurred = true;
        }


        //类型参数
        if (at(LPAR)) {
            parseValueParameterList(false, false, VALUE_PARAMETERS_FOLLOW_SET);

        } else {
            error("Expecting '(' ");  //应该为'('
        }

        //返回值类型
        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        }
        parseTypeConstraintsGuarded(typeParameterListOccurred);
        //函数体
//        if (at(SEMICOLON)) {
//            advance(); // SEMICOLON
//        } else

        if (isDeclarationsFile) {

            if (at(LBRACE)) {
                PsiBuilder.Marker body = mark();
                while (!atSet(KEYWORDALL) && !eof()) {
                    advance();
                }

//                if (!(MODIFIER_KEYWORDS.contains(lookahead(1)) || lookahead(1) == FUNC)) {
//                    parseFunctionBody();
//
//                }else {
//                    advance();
//                }


                body.error("Method bodies are not allowed in declaration files");

            }
//            else body.drop();

            return type;
        }

        if (at(LBRACE)) {

            parseFunctionBody();
            if (isForeign) {
                error("foreign function can not have body");  //应该为'{'
            }

        } else if (!(isInterfaceMethod || (classdetector != null && classdetector.isAbstractDetected())) && (detector != null && !detector.isForeignDetected())) {
            error("Expecting '{' ");  //应该为'{'
        }

        return type;
    }

    public void parseSynchronizedExpression() {
        assert _at(SYNCHRONIZED_KEYWORD);
        SyntaxTreeBuilder.Marker synchronizedMarker = mark();
        advance();


        if (at(LPAR)) {
            advance();
            myExpressionParsing.parseExpression();

            expect(RPAR, "Expecting '('");


        } else {
            error("Expecting '(' ");
        }

        if (at(LBRACE)) {
            parseBlock();
        } else {
            error("Expecting '{' ");  //应该为'{'
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
        assert _at(FOREIGN_KEYWORD);
        advance();

//        处理声明块
        if (at(LBRACE)) {
            parseForeignBody();

        } else {
            error("Expecting '{' ");

        }


        return FOREIGN;
    }

    /**
     * 外部函数声明块
     */
    private void parseForeignBody() {
        assert _at(LBRACE);

        PsiBuilder.Marker mark = mark();
        advance(); // LBRACE

        while (!at(RBRACE) && !eof()) {
            ModifierDetector detector = new ModifierDetector();

            parseModifierList(detector, TokenSet.EMPTY);


            if (at(FUNC_KEYWORD)) {
                detector.consume(FOREIGN_KEYWORD);

                parseForeignFunction(detector);
            } else {
                errorWithRecovery("Expecting function declaration", TokenSet.create(FUNC_KEYWORD));
            }


//            advance();

        }
        expect(RBRACE, "Missing '}");
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
        assert _at(MACRO_KEYWORD);
        advance();

        if (at(RBRACE)) {
            error("Function body expected");  //应该为函数体
            return MACRO;
        }


        myBuilder.disableJoiningComplexTokens();


        //函数名
        parseIdentifier();


//        expect(EXCL);

        boolean typeParameterListOccurred = false;
        if (at(LT)) {
            parseTypeParameterList(LBRACKET_LBRACE_RBRACE_LPAR_SET);
            typeParameterListOccurred = true;
        }


        //类型参数
        if (at(LPAR)) {
            parseValueParameterList(false, false, VALUE_PARAMETERS_FOLLOW_SET);

        } else {
            error("Expecting '(' ");  //应该为'('
        }

        //返回值类型
        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        }
        parseTypeConstraintsGuarded(typeParameterListOccurred);


        if (isDeclarationsFile) {

            if (at(LBRACE)) {
                PsiBuilder.Marker body = mark();
                while (!atSet(KEYWORDALL) && !eof()) {
                    advance();
                }

                body.error("Method bodies are not allowed in declaration files");

            }

            return MACRO;
        }

        //函数体
//        if (at(SEMICOLON)) {
//            advance(); // SEMICOLON
//        } else
        if (at(LBRACE)) {
            parseFunctionBody();
        } else {
            error("Expecting '{' ");  //应该为'{'
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
        if (at(COLON)) {
            PsiBuilder.Marker error = mark();
            while (!at(LBRACE)) advance();

            error.error("Expecting '{' ");
        }
        if (isDeclarationsFile) {

            if (at(LBRACE)) {
                PsiBuilder.Marker body = mark();
                while (!atSet(KEYWORDALL) && !eof()) {
                    advance();
                }

                body.error("Method bodies are not allowed in declaration files");

            }

            return;
        }
        if (at(LBRACE)) {
            parseInitFunctionBlock();
        } else {
            error("Expecting function body"); //应该为函数体
        }
    }

    /*
     * functionBody
     *   : block
     *   : "=" element
     *   ;
     */
    void parseFunctionBody() {
        if (at(LBRACE)) {
            parseBlock();
        } else {
            error("Expecting function body"); //应该为函数体
        }
    }


    /*
     * functionParameter
     *   : modifiers ("val" | "var")? parameter ("=" element)?
     *   ;
     */
    private boolean tryParseValueParameter(boolean typeRequired) {
        return parseValueParameter(true, typeRequired);
    }

    //    private void parseFunctionTypeValueParameterModifierList() {
//        doParseModifierList(null, RESERVED_VALUE_PARAMETER_MODIFIER_KEYWORDS, NO_ANNOTATIONS, NO_MODIFIER_BEFORE_FOR_VALUE_PARAMETER);
//    }


    void parseInitFuncValueParameterList() {
        parseValueParameterList(false, false, VALUE_PARAMETERS_FOLLOW_SET, true);
    }


    private void parseValueParameterList(boolean isFunctionTypeContents, boolean typeRequired, TokenSet recoverySet) {
        parseValueParameterList(isFunctionTypeContents, typeRequired, recoverySet, false);

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
    private void parseValueParameterList(boolean isFunctionTypeContents, boolean typeRequired, TokenSet recoverySet, boolean isInitFunc) {
        assert at(LPAR);
        PsiBuilder.Marker parameters = mark();


        myBuilder.disableNewlines();
        advance(); // (


//        用于报告错误   要么全为命名参数，要么全不为命名参数
//        bool isNamedParameter = false;
        List<Boolean> isNamedParameters = new ArrayList<>();

        while (!at(RPAR) && !atSet(recoverySet) && !eof()) {
            if (at(COMMA)) {
                errorAndAdvance("Expecting a parameter declaration");  //应该为参数声明
            }
            if (isFunctionTypeContents) {
                if (!tryParseValueParameter(typeRequired)) {
                    PsiBuilder.Marker valueParameter = mark();
//                    parseFunctionTypeValueParameterModifierList();
                    parseTypeRef();
                    closeDeclarationWithCommentBinders(valueParameter, VALUE_PARAMETER, false);
                    isNamedParameters.add(false);

                } else {
                    isNamedParameters.add(true);

                }
            } else {
                parseValueParameter(false, typeRequired, isInitFunc);
            }
//            parseValueParameter(typeRequired);
            if (at(COMMA)) {
                advance(); // COMMA

                if (at(RPAR)) {
                    error("Expecting a parameter declaration");  //应该为参数声明
                }

            } else {
                if (!at(RPAR)) {
                    errorAndAdvance("Expecting ',' or ')',found '" + myBuilder.getTokenText() + "'");  //应该为参数声明
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
        expect(RPAR, "Expecting ')'", recoverySet);
        myBuilder.restoreNewlinesState();


        if (isNamedParameters.contains(true) && isNamedParameters.contains(false)) {
//            要么全为true 要么全为false
            parameters.error("In a parameter type list, either all parameters must be named, or none of them; mixed is not allowed");
        } else {
            parameters.done(VALUE_PARAMETER_LIST);

        }

    }

    public void parseValueParameter(boolean typeRequired) {
        parseValueParameter(false, typeRequired);
    }

    private boolean parseValueParameter(boolean rollbackOnFailure, boolean typeRequired) {
        return parseValueParameter(rollbackOnFailure, typeRequired, false);

    }

    private boolean parseValueParameter(boolean rollbackOnFailure, boolean typeRequired, boolean isInitFunc) {
        PsiBuilder.Marker parameter = mark();

//
//        if (at(VAR_KEYWORD) || at(LET_KEYWORD)) {
//            advance(); // VAR_KEYWORD | LET_KEYWORD
////            return false;
////            error("Expecting parameter declaration");  //应该为参数声明
//        }
        if (isInitFunc) {


            ModifierDetector detector = new ModifierDetector();

            parseModifierList(detector, TokenSet.EMPTY, true);
            if ((at(LET_KEYWORD) || at(VAR_KEYWORD))) {
                advance();
            } else if (detector.getSize() > 0) {

                error("Missing variable declaration symbol let or var after modifier");  //应该为参数声明
            }
        }


        if (!parseFunctionParameterRest(typeRequired) && rollbackOnFailure) {
            parameter.rollbackTo();
            return false;
        }

        closeDeclarationWithCommentBinders(parameter, VALUE_PARAMETER, false);
        return true;
    }


    /*
     * functionParameterRest  函数参数
     *   : parameter
     *   : identifier('!') ':' type ("=" element)  ! 和 = 必须同时出现
     *   ; identifier ':' ('?')?type   可以为Option.Nono
     *   ;
     */
    private boolean parseFunctionParameterRest(boolean typeRequired) {
        boolean noErrors = true;
        boolean isDefault = false;


        // 恢复 'func foo(Array<String>) {}'
        // 恢复 'func foo(: Int) {}'
        if ((at(IDENTIFIER) && lookahead(1) == LT) || at(COLON)) {
            error("Missing parameter name");  //缺少参数名称
            if (at(COLON)) {
                // 保留noErrors==true，这样在函数类型的解析过程中不会回滚以“：”开头的未命名参数
                advance(); // :


            } else {
                noErrors = false;
            }
            parseTypeRef();
        } else {
            expect(CangJieExpressionParsing.IDENTIFIER_RECOVERY_SET, "Missing parameter name", PARAMETER_NAME_RECOVERY_SET);

            if (expect(EXCL)) {
//              可以有默认值
                isDefault = true;
            }

            if (at(COLON)) {
                advance(); // :

                if (at(IDENTIFIER) && lookahead(1) == COLON) {
                    // 恢复 "func foo(x: y: Int)" 处理 'y:' 时，可能是下一个参数的名称
                    error("Type reference expected");
                    return false;
                }

                parseTypeRef();
            } else if (typeRequired) {
                errorWithRecovery("Parameters must have type annotation", PARAMETER_NAME_RECOVERY_SET);
                noErrors = false;
            } else {
                errorWithoutAdvancing("Expecting ':' Missing type declaration");  //应该为':'
                noErrors = false;
            }


        }
        if (at(EQ)) {
            if (isDefault) {
                advance();
            } else {
                error("The default value cannot be set for non-named parameters");
                errorAndAdvance("Expecting ',' or ')', found '='");
                noErrors = false;

            }


            myExpressionParsing.parseExpression();
        }
        return noErrors;
    }

    private boolean recoverOnParenthesizedWordForPlatformTypes(int offset, String word, boolean consume) {
        // Array<(out) Foo>! or (Mutable)List<Bar>!
        if (lookahead(offset) == LPAR &&
                lookahead(offset + 1) == IDENTIFIER &&
                lookahead(offset + 2) == RPAR &&
                lookahead(offset + 3) == IDENTIFIER) {
            PsiBuilder.Marker error = mark();

            advance(offset);

            advance(); // LPAR
            if (!word.equals(myBuilder.getTokenText())) {
                // something other than "out" / "Mutable"
                error.rollbackTo();
                return false;
            } else {
                advance(); // IDENTIFIER('out')
                advance(); // RPAR

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
        if (!at(LT)) return false;

        PsiBuilder.Marker list = mark();

        tryParseTypeArgumentList(TokenSet.EMPTY);

        list.done(TYPE_ARGUMENT_LIST);

        return true;
    }

    private void recoverOnPlatformTypeSuffix() {
        // 平台类型的恢复
        if (at(EXCL)) {
            PsiBuilder.Marker error = mark();
            advance(); // EXCL
            error.error("Unexpected token");
        }
    }

    /*
     * functionType
     *   : (type ".")? "(" parameter{","}? ")" "->" type?
     *   ;
     */
    private void parseFunctionType(PsiBuilder.Marker functionType) {
        parseFunctionTypeContents(functionType).done(FUNCTION_TYPE);
    }

    private PsiBuilder.Marker parseFunctionTypeContents(PsiBuilder.Marker functionType) {
        assert _at(LPAR) : tt();

        parseValueParameterList(true, /* typeRequired  = */ true, TokenSet.EMPTY);

        expect(ARROW, "Expecting '->' to specify return type of a function type", TYPE_REF_FIRST);
        parseTypeRef();

        return functionType;
    }

    //返回元组的类型数量
    private int parseTupleType() {
        assert _at(LPAR);
        int count = 0;


        advance(); // LPAR

        if (!at(RPAR)) {
            while (true) {

                parseTypeRef();
                count++;
                if (!at(COMMA)) break;
                advance(); // COMMA

            }
        } else {
            error("Expecting type");
        }

        expect(RPAR, "Expecting ')'");

        return count;
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

        PsiBuilder.Marker userType = mark();


        PsiBuilder.Marker reference = mark();

        while (true) {
            recoverOnParenthesizedWordForPlatformTypes(0, "Mutable", true);
            if (expect(IDENTIFIER, "Expecting type name",
                    TokenSet.orSet(CangJieExpressionParsing.Companion.getEXPRESSION_FIRST(), CangJieExpressionParsing.Companion.getEXPRESSION_FOLLOW(),
                            DECLARATION_FIRST))) {
                reference.done(REFERENCE_EXPRESSION);
            } else {
                reference.drop();
                break;
            }

            isTypeArgumentList = parseTypeArgumentList();

//            recoverOnPlatformTypeSuffix();

            if (!at(DOT)) {
                break;
            }

            PsiBuilder.Marker precede = userType.precede();
            userType.done(USER_TYPE);
            userType = precede;

            advance(); // DOT
            reference = mark();
        }

        userType.done(USER_TYPE);
        return isTypeArgumentList;

    }

    /**
     * 解析类型引用
     *
     * @return
     */
    private PsiBuilder.Marker parseTypeRefContents(TokenSet extraRecoverySet) {
        PsiBuilder.Marker typeRefMarker = mark();

        return typeRefMarker;
    }

    /**
     * 解析This类型
     */
    private boolean parseThisType() {


        if (at(THIS_KEYWORD_UPPER)) {
            PsiBuilder.Marker typeRefMarker = mark();
            advance();
            typeRefMarker.done(THIS_TYPE);
            return true;
        }


        return false;


    }

    /**
     * 解析基本类型
     */
    private boolean parseBasicType() {


        if (atSet(BASICTYPES)) {
            PsiBuilder.Marker typeRefMarker = mark();
            advance();
            typeRefMarker.done(BASIC_TYPE);
            return true;
        }


        return false;


    }

    /*
     * (SimpleName  {","})
     */
    public void parseMultiDeclarationName(TokenSet follow, TokenSet recoverySet) {


        // Parsing multi-name, e.g.
        //   val (a, b) = foo()
        myBuilder.disableNewlines();
        advance(); // LPAR

        if (!atSet(follow)) {
            while (true) {
                if (at(COMMA)) {
                    errorAndAdvance("Expecting a name");
                } else if (at(RPAR)) { // For declaration similar to `val () = somethingCall()`
                    error("Expecting a name");
                    break;
                }
                PsiBuilder.Marker property = mark();

                parseModifierList(COMMA_RPAR_COLON_EQ_SET);

                expect(IDENTIFIER, "Expecting a name", recoverySet);

//                if (at(COLON)) {
//
//                    advance(); // COLON
//                    parseTypeRef(follow);
//                }
                property.done(DESTRUCTURING_DECLARATION_ENTRY);

                if (!at(COMMA)) break;
                advance(); // COMMA
                if (at(RPAR)) break;
            }
        }

        expect(RPAR, "Expecting ')'", follow);
        myBuilder.restoreNewlinesState();
    }

    /**
     * 解析类型参数列表
     */
    boolean tryParseTypeArgumentList(TokenSet extraRecoverySet) {

        myBuilder.disableNewlines();
        advance(); // LT


        do {
            if (at(COMMA)) {
                advance();
            }

            PsiBuilder.Marker projection = mark();

            parseTypeRef(extraRecoverySet);

            projection.done(TYPE_PROJECTION);
            if (at(GT)) {
                break;
            }
        } while (at(COMMA));
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

        boolean atGT = at(GT);
        if (!atGT) {
            error("Expecting a '>'");
        } else {
            advance(); // GT
        }
        myBuilder.restoreNewlinesState();
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

    void parseTypeRef(boolean isConstraint) {
        parseTypeRef(TokenSet.EMPTY, isConstraint);
    }

    void parseTypeRef(TokenSet extraRecoverySet) {
        parseTypeRef(extraRecoverySet, false);
    }


    void parseOptionType() {
        assert _at(QUEST);


        PsiBuilder.Marker optionTypeMarker = mark();

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

        advance();

        parseTypeRefContents();


        optionTypeMarker.done(OPTIONAL_TYPE);

    }

    /**
     * @param extraRecoverySet
     * @param isConstraint     是否为约束，约束没有问号,不解析userType
     */
    void parseTypeRef(TokenSet extraRecoverySet, boolean isConstraint) {

        PsiBuilder.Marker typeRefMarker = mark();
        //先解析基本类型，如果不是基本类型，则解析类型引用


        if (!isConstraint) {
            parseTypeRefContents();
        } else {
            parseIdentifier();
        }


        typeRefMarker.done(TYPE_REFERENCE);

    }

    private void parseTupleOrFunctionType() {
        PsiBuilder.Marker oType = mark();


        int count = parseTupleType();


        if (at(ARROW) || at(COLON)) {

            oType.rollbackTo();
            oType = mark();
            parseFunctionType(oType);


        } else {
            if (count <= 1) {
                oType.done(PARENTHESIZED_TYPE);

            } else {
                oType.done(TUPLE_TYPE);

            }

        }
    }

    private void parseTypeRefContents() {
        if (parseThisType()) return;
        if (parseBasicType()) return;
        if (at(IDENTIFIER)) {
            parseUserType();
        } else if (at(LPAR)) {
//            元组，方法或括号类型
            parseTupleOrFunctionType();

        } else if (at(QUEST) || at(SAFE_CALL)) {
//            OPTION类型可嵌套
            parseOptionType();

        } else {
            error("Expecting a type name, found '" + myBuilder.getTokenText() + "'");
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

        DeclarationParsingMode(boolean destructuringAllowed, boolean accessorsAllowed, boolean canBeEnumUsedAsSoftKeyword) {
            this.destructuringAllowed = destructuringAllowed;
            this.accessorsAllowed = accessorsAllowed;
            this.canBeEnumUsedAsSoftKeyword = canBeEnumUsedAsSoftKeyword;
        }
    }

    public enum NameParsingMode {
        REQUIRED, ALLOWED, PROHIBITED
    }

    static class ModifierDetector implements Consumer<IElementType> {

        private boolean abstractDetected = false;
        private boolean mutDetected = false;
        private boolean publicDetected = false;
        private boolean privateDetected = false;
        private boolean protectedDetected = false;
        private boolean operatorDetected = false;
        private boolean foreignDetected = false;
        private boolean constDetected = false;
        private boolean unsafeDetected = false;
        private boolean sealedDetected = false;
        private boolean redefDetected = false;
        private boolean openDetected = false;
        private boolean staticDetected = false;
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
            for (Field field : this.getClass().getDeclaredFields()) {
                try {
                    if (field.get(this).equals(true)) {
                        size++;
                    }
                } catch (IllegalAccessException e) {
//                    throw new RuntimeException(e);
                }
            }

            return size;
        }

        @Override
        public void consume(IElementType item) {
//            if (item == ABSTRACT_KEYWORD) {
//                abstractDetected = true;
//            } else if (item == MUT_KEYWORD) {
//                mutDetected = true;
//            }

            if (item.equals(PUBLIC_KEYWORD)) {
                publicDetected = true;
            } else if (item.equals(PRIVATE_KEYWORD)) {
                privateDetected = true;
            } else if (item.equals(PROTECTED_KEYWORD)) {
                protectedDetected = true;
            } else if (item.equals(ABSTRACT_KEYWORD)) {
                abstractDetected = true;
            } else if (item.equals(MUT_KEYWORD)) {
                mutDetected = true;
            } else if (item.equals(OPERATOR_KEYWORD)) {
                operatorDetected = true;
//
            } else if (item.equals(FOREIGN_KEYWORD)) {
                foreignDetected = true;
            } else if (item.equals(CONST_KEYWORD)) {
                constDetected = true;
            } else if (item.equals(UNSAFE_KEYWORD)) {
                unsafeDetected = true;
            } else if (item.equals(OPEN_KEYWORD)) {
                openDetected = true;
            } else if (item.equals(STATIC_KEYWORD)) {
                staticDetected = true;
            } else if (item.equals(SEALED_KEYWORD)) {
                sealedDetected = true;
            } else if (item.equals(REDEF_KEYWORD)) {
                redefDetected = true;
            }/* else if (item.equals(ANNOTATION_ENTRY)) {
                annotationCount++;
            }*/
        }

        public boolean isOperatorDetected() {
            return operatorDetected;
        }

        public boolean isAbstractDetected() {
            return abstractDetected;
        }

        public boolean isMutDetected() {
            return mutDetected;
        }

        public boolean isPublicDetected() {
            return publicDetected;
        }

        public boolean isPrivateDetected() {
            return privateDetected;
        }

        public boolean isProtectedDetected() {
            return protectedDetected;
        }

        public boolean isVisibilityDetected() {
            return publicDetected || privateDetected || protectedDetected;
        }

        public boolean isPublicOrProtectedDetected() {
            return publicDetected || protectedDetected;
        }

        public boolean isForeignDetected() {
            return foreignDetected;
        }

        public boolean isStaticDetected() {
            return staticDetected;
        }

        public boolean isUnsafeDetected() {
            return unsafeDetected;
        }

        public boolean isSealedDetected() {
            return sealedDetected;
        }

        public boolean isRedefDetected() {
            return redefDetected;
        }

        public boolean isOpenDetected() {
            return openDetected;
        }

    }
}
