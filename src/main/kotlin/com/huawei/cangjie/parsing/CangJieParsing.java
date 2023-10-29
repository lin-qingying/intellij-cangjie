package com.huawei.cangjie.parsing;

import com.huawei.cangjie.lexer.CjTokens;
import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.huawei.cangjie.CjNodeTypes.*;
import static com.huawei.cangjie.lexer.CjTokens.*;


public class CangJieParsing extends AbstractCangJieParsing {
    public static final TokenSet PARAMETER_NAME_RECOVERY_SET = TokenSet.create(COLON, EQ, COMMA, RPAR);
    private static final TokenSet GT_COMMA_COLON_SET = TokenSet.create(GT, COMMA, COLON);
    private static final Logger LOG = Logger.getInstance(CangJieParsing.class);
    private static final TokenSet TOP_LEVEL_DECLARATION_FIRST = TokenSet.create(INTERFACE_KEYWORD, CLASS_KEYWORD, FUNC_KEYWORD, LET_KEYWORD, PACKAGE_KEYWORD);
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
    private static final TokenSet VALUE_PARAMETER_FIRST = TokenSet.orSet(TokenSet.create(IDENTIFIER, LBRACKET, LET_KEYWORD, VAR_KEYWORD), TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUNC_KEYWORD)));
    private static final TokenSet LAMBDA_VALUE_PARAMETER_FIRST = TokenSet.orSet(TokenSet.create(IDENTIFIER, LBRACKET), TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUNC_KEYWORD)));

//    private static final TokenSet ANNOTATION_TARGETS = TokenSet.create(FILE_KEYWORD, FIELD_KEYWORD, GET_KEYWORD, SET_KEYWORD, PROPERTY_KEYWORD, RECEIVER_KEYWORD, PARAM_KEYWORD, SETPARAM_KEYWORD, DELEGATE_KEYWORD);
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
    private final static TokenSet CLASS_INTERFACE_STRUCT_SET = TokenSet.create(CLASS_KEYWORD, INTERFACE_KEYWORD, STRUCT_KEYWORD);

    private final static TokenSet IDENTIFIER_DOT_MUL_SET = TokenSet.create(IDENTIFIER, DOT, MUL);

    private final static TokenSet DOT_MUL_SET = TokenSet.create(DOT, MUL);
    private final static TokenSet FROM_IMPORT_SET = TokenSet.create(FROM_KEYWORD, IMPORT_KEYWORD);

    private final static TokenSet MUT_PROP_SET = TokenSet.create(MUT_KEYWORD, PROP_KEYWORD);

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
    void parseTypeRef() {
        parseTypeRef(TokenSet.EMPTY);
    }
    void parseTypeRefWithoutIntersections() {
        parseTypeRef(TokenSet.EMPTY  );
    }
    private final CangJieExpressionParsing myExpressionParsing;

    private static CangJieParsing createForByClause(SemanticWhitespaceAwarePsiBuilder builder, boolean isLazy) {
        return new CangJieParsing(new SemanticWhitespaceAwarePsiBuilderForByClause(builder), false, isLazy);
    }

    static CangJieParsing createForTopLevel(SemanticWhitespaceAwarePsiBuilder builder) {
        return new CangJieParsing(builder, true, true);
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
            }
            else if (_at(RBRACE)) {
                braceCount--;
            }

            advance();

            if (braceCount == 0) {
                break;
            }
        }
    }

    private void parseBlock(boolean collapse) {
        PsiBuilder.Marker lazyBlock = mark();

        myBuilder.enableNewlines();

        boolean hasOpeningBrace = expect(LBRACE, "Expecting '{'  ");
        boolean canCollapse = collapse && hasOpeningBrace && isLazy;


//        expect(RBRACE, "Expecting '}'");


        if (canCollapse) {
            advanceBalancedBlock();
        }
        else {
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
         * TODO fileAnnotationList  文档注释
         *   : fileAnnotations*
         */

        /*
         * packageDirective  包声明
         *   : modifiers "package" SimpleName{"."} SEMI?
         *   ;
         */
        PsiBuilder.Marker packageDirective = mark();


        if (at(PACKAGE_KEYWORD)) {
            advance(); // PACKAGE_KEYWORD


            //TODO 处理包名
            parsePackageName();

            firstEntry.drop();

            consumeIf(SEMICOLON);

            packageDirective.done(PACKAGE_DIRECTIVE);
        } else {
            //当忽略Package指令时，我们不应该在文件开头报告非文件批注的错误。
            //因此，我们回滚解析位置，重新解析文件注释列表，非文件注释没有上报错误。
            firstEntry.rollbackTo();

            //TODO 解析文件注释列表
//            parseFileAnnotationList(FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED);
            packageDirective = mark();
            packageDirective.done(PACKAGE_DIRECTIVE);
            //需要跳过除Shebang注释之外的所有内容，以允许将文件开头的注释绑定到第一个声明。
            packageDirective.setCustomEdgeTokenBinders(BindFirstShebangWithWhitespaceOnly.INSTANCE, null);

        }
        parseImportDirectives();
    }

    private void parseImportDirectives() {
        PsiBuilder.Marker importList = mark();

        while (atSet(FROM_IMPORT_SET)) {
            parseImportDirective();
        }
        importList.done(IMPORT_LIST);
    }

    private boolean closeImportWithErrorIfNewline(
            PsiBuilder.Marker importDirective, @Nullable PsiBuilder.Marker importAlias, String errorMessage
    ) {
        if (myBuilder.newlineBeforeCurrentToken()) {
            if (importAlias != null) {
                importAlias.done(IMPORT_ALIAS);
            }
            error(errorMessage);
            importDirective.done(IMPORT_DIRECTIVE);
            return true;
        }
        return false;
    }

    /*
     * import
     *   ;  "from" SimpleName
     *   : "import" SimpleName{"."} ("." "*" | "as" SimpleName)? SEMI?
     *   ;
     */
    private void parseImportDirective() {
        assert _atSet(FROM_IMPORT_SET);


        boolean isFrom = false;
        PsiBuilder.Marker importDirective = mark();
        if (at(FROM_KEYWORD)) {
            isFrom = true;
            advance();

            //处理软件包名
            if (!at(IDENTIFIER)) {
                error("Software package name is required");
                importDirective.done(IMPORT_DIRECTIVE);
                return;
            } else {
                advance();
            }


        }
        if (!at(IMPORT_KEYWORD) && isFrom) {

            error("Expecting 'import' keyword");
            importDirective.done(IMPORT_DIRECTIVE);
            return;
        }


        advance(); // IMPORT_KEYWORD

        if (closeImportWithErrorIfNewline(importDirective, null, "Expecting qualified name")) {
            return;
        }

        if (!at(IDENTIFIER)) {
            PsiBuilder.Marker error = mark();
            skipUntil(TokenSet.create(EOL_OR_SEMICOLON));
            error.error("Expecting qualified name");
            importDirective.done(IMPORT_DIRECTIVE);
            consumeIf(SEMICOLON);
            return;
        }

        PsiBuilder.Marker qualifiedName = mark();
        PsiBuilder.Marker reference = mark();
        advance(); // IDENTIFIER
        reference.done(REFERENCE_EXPRESSION);

        while (at(DOT) && lookahead(1) != MUL) {
            advance(); // DOT

            if (closeImportWithErrorIfNewline(importDirective, null, "Import must be placed on a single line")) {
                qualifiedName.drop();
                return;
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
        qualifiedName.drop();

        if (at(DOT)) {
            advance(); // DOT
            assert _at(MUL);
            advance(); // MUL
            if (at(AS_KEYWORD)) {
                PsiBuilder.Marker as = mark();
                advance(); // AS_KEYWORD
                if (closeImportWithErrorIfNewline(importDirective, null, "Expecting identifier")) {
                    as.drop();
                    return;
                }
                consumeIf(IDENTIFIER);
//                as.done(IMPORT_ALIAS);

                if (!match(DOT, MUL)) {
//                    as.precede().error("The alias name should contain '.*' suffix after import-all");
                    error("The alias name should contain '.*' suffix after import-all");
                } else {
                    as.done(IMPORT_ALIAS);
                }

            }
        }
        if (at(AS_KEYWORD)) {
            PsiBuilder.Marker alias = mark();
            advance(); // AS_KEYWORD
            if (closeImportWithErrorIfNewline(importDirective, alias, "Expecting identifier")) {
                return;
            }
            expect(IDENTIFIER, "Expecting identifier", SEMICOLON_SET);
            alias.done(IMPORT_ALIAS);
        }
        consumeIf(SEMICOLON);
        importDirective.done(IMPORT_DIRECTIVE);
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

    //入口
    void parseFile() {
        PsiBuilder.Marker fileMarker = mark();

        //处理开头  package
        parsePreamble();

//        处理声明式语句
        while (!eof()) {
            parseTopLevelDeclaration();
        }


        fileMarker.done(CJ_FILE);
    }

    /*
     * 顶层声明语句
     *   : function
     */
    private void parseTopLevelDeclaration() {
        if (at(SEMICOLON)) {
            advance(); // SEMICOLON
            return;
        }
        PsiBuilder.Marker decl = mark();
        ModifierDetector detector = new ModifierDetector();

        parseModifierList(detector, TokenSet.EMPTY);
        IElementType declType = parseCommonDeclaration(detector, NameParsingMode.REQUIRED, DeclarationParsingMode.MEMBER_OR_TOPLEVEL);


        if (declType == null) {

            errorAndAdvance("Expecting a top level declaration"); //期待一个顶层声明语句
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
        }

        marker.rollbackTo();
        return false;
    }

    private boolean doParseModifierListBody(
            @Nullable Consumer<IElementType> tokenConsumer,
            @NotNull TokenSet modifierKeywords,

            @NotNull TokenSet noModifiersBefore
    ) {

        boolean empty = true;

        while (!eof()) {
            if (!tryParseModifier(tokenConsumer, noModifiersBefore, modifierKeywords)) {
                // modifier advanced
                break;
            }

            empty = false;
        }

        return empty;
    }

    /**
     * (modifier )*
     * <p>
     * 如果不为空，则将修饰符(非批注)馈送到传递的使用者
     *
     * @param noModifiersBefore 是一个令牌集，其中包含指示何时满足这些元素的元素。
     *                          必须将前一个令牌解析为标识符，而不是修饰符
     */
    boolean parseModifierList(@Nullable Consumer<IElementType> tokenConsumer, @NotNull TokenSet noModifiersBefore) {
        return doParseModifierList(tokenConsumer, MODIFIER_KEYWORDS, noModifiersBefore);
    }

    private boolean doParseModifierList(
            @Nullable Consumer<IElementType> tokenConsumer,
            @NotNull TokenSet modifierKeywords,

            @NotNull TokenSet noModifiersBefore
    ) {
        PsiBuilder.Marker list = mark();

        boolean empty = doParseModifierListBody(tokenConsumer, modifierKeywords, noModifiersBefore);

        if (empty) {
            list.drop();
        } else {
            list.done(MODIFIER_LIST);
        }
        return !empty;
    }

    private IElementType parseClassCommonDeclaration() {
        //init func let|var prop
        return switch (getTokenId()) {
            case FUNC_KEYWORD_Id -> parseFunction();
            case PROP_KEYWORD_Id -> parseProperty();
            case LET_KEYWORD_Id, VAR_KEYWORD_Id -> parseVariable();
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
    private final LastBefore lastDotAfterReceiverNotLParPattern =
            new LastBefore(new AtSet(RECEIVER_TYPE_TERMINATORS), new AbstractTokenStreamPredicate() {
                @Override
                public boolean matching(boolean topLevel) {
                    if (topLevel && (atSet(definitelyOutOfReceiverSet) || at(LPAR))) return true;
                    if (topLevel && at(IDENTIFIER)) {
                        IElementType lookahead = lookahead(1);
                        return lookahead != LT && lookahead != DOT   && lookahead != QUEST;
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
    private int lastDotAfterReceiver() {
        AbstractTokenStreamPattern pattern = at(LPAR) ? lastDotAfterReceiverLParPattern : lastDotAfterReceiverNotLParPattern;
        pattern.reset();
        return matchTokenStreamPredicate(pattern);
    }
    private boolean parseReceiverType(String title, TokenSet nameFollow){

        int lastDot = lastDotAfterReceiver();
        boolean receiverPresent = lastDot != -1;


        if (!receiverPresent) return false;

        createTruncatedBuilder(lastDot).parseTypeRef();

        if (atSet(RECEIVER_TYPE_TERMINATORS)) {
            advance(); // expectation
        }
        else {
            errorWithRecovery("Expecting '.' before a " + title + " name", nameFollow);
        }
        return true;
    }

    /*
     * variableDeclarationEntry
     *   : SimpleName (":" ('?')?type)?
     *   ;
     *
     * property
     *   : modifiers ("let" | "var")
     *   ;
     */
    public IElementType parseVariable() {
        assert (at(LET_KEYWORD) || at(VAR_KEYWORD));
        advance();



//        myBuilder.disableJoiningComplexTokens();

        boolean receiverTypeDeclared = parseReceiverType("property", PROPERTY_NAME_FOLLOW_SET);

//        boolean isNameOnTheNextLine = eol();
//        PsiBuilder.Marker beforeName = mark();

        parseIdentifierByTitle("property", PROPERTY_NAME_FOLLOW_SET);
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
        } else {
            errorAndAdvance("variable in top-level scope must be initialized");
        }

//        if (!parsePropertyDelegateOrAssignment() && isNameOnTheNextLine && noTypeReference && !receiverTypeDeclared) {
//
//            beforeName.rollbackTo();
//            error("Expecting variable name or receiver type");
//            return VARIABLE;
//        }

//        beforeName.drop();
        return VARIABLE;

    }
    enum DeclarationParsingMode {
        MEMBER_OR_TOPLEVEL(false, true, true), LOCAL(true, false, false), SCRIPT_TOPLEVEL(true, true, false);

        public final boolean destructuringAllowed;
        public final boolean accessorsAllowed;
        public final boolean canBeEnumUsedAsSoftKeyword;

        DeclarationParsingMode(boolean destructuringAllowed, boolean accessorsAllowed, boolean canBeEnumUsedAsSoftKeyword) {
            this.destructuringAllowed = destructuringAllowed;
            this.accessorsAllowed = accessorsAllowed;
            this.canBeEnumUsedAsSoftKeyword = canBeEnumUsedAsSoftKeyword;
        }
    }

    public IElementType parseCommonDeclaration(
            @NotNull ModifierDetector detector,
            @NotNull NameParsingMode nameParsingMode,
            @NotNull DeclarationParsingMode declarationParsingMode
    ) {


        switch (getTokenId()) {
            case FUNC_KEYWORD_Id:
                return parseFunction();
            case MAIN_KEYWORD_Id:
                return parseMainFunc();


            case ENUM_KEYWORD_Id:
                return parseEnum();


            case STRUCT_KEYWORD_Id:
            case INTERFACE_KEYWORD_Id:
            case CLASS_KEYWORD_Id:
                return parseClass();

            case LET_KEYWORD_Id:
            case VAR_KEYWORD_Id:
                return parseVariable();
        }

        return null;
    }

    /*
     * prop
     *   :  "mnt"? prop Identifier :Type propBody
     *   ;
     */
    private IElementType parseProperty() {
        assert _at(PROP_KEYWORD);


//        boolean isMut = false;
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


        if (at(LBRACE)) {
            parsePropertyBody();
        } else {
            error("Missing prop body Expecting '{'");
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
    private void parsePropertyBody() {
        assert _at(LBRACE);
        advance(); // LBRACE

        PsiBuilder.Marker body = mark();


        if (at(RBRACE)) {
            body.done(PROPERTY_BODY);
            advance(); // RBRACE
        } else {
            body.drop();
            error("Expecting '}'");
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

        parseIdentifierByTitle("enum", IDENTIFIER_RBRACKET_LBRACKET_SET);


        parseEnumBody();

        return ENUM;
    }

    private void parseEnumBody() {
        PsiBuilder.Marker body = mark();
        if (at(LBRACE)) {
            advance(); // LBRACE


            if (at(IDENTIFIER)) {
                parseEnumList();
            } else {
                error("Expecting enum entry");
            }

            parseMembers();


            expect(RBRACE, "Expecting '}'");
        } else {
            error("Expecting '{'");
        }
        body.done(ENUM_BODY);
    }


    private void parseEnumList() {


        while (true) {
            if (at(RBRACE)) {
                break;
            }
            parseEnumEntry();


            if (!at(OR) ) break;
            advance(); // OR

        }


    }

    private void parseEnumEntry() {
        PsiBuilder.Marker entry = mark();

        parseIdentifierByTitle("enum entry", IDENTIFIER_RBRACKET_LBRACKET_SET);

        if (at(LPAR)) {
            advance(); // LPAR
            parseTypeRef();
            expect(RPAR, "Expecting ')'");
        }

        entry.done(ENUM_ENTRY);
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
//                parseTypeParameter();
                parseTypeRef();

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

        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        }

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
    private IElementType parseClass() {

        int tokenid = getTokenId();

//        assert _at(CLASS_KEYWORD);
        assert _atSet(CLASS_INTERFACE_STRUCT_SET);
        advance();

        //类名
        parseIdentifier();

        boolean typeParametersDeclared = parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET);


        // TODO 继承
        if (at(LTCOLON)) {
            advance(); // COLON
            parseDelegationSpecifierList();
        }


        OptionalMarker whereMarker = new OptionalMarker(false);
        parseTypeConstraintsGuarded(typeParametersDeclared);
        whereMarker.error("Where clause is not allowed");


        if (at(LBRACE)) {
            parseClassBody();
        } else {
            error("Expecting '{' or Inherit");  //应该为'{' 或者继承
        }


        return switch (tokenid) {
            case INTERFACE_KEYWORD_Id -> INTERFACE;
            case STRUCT_KEYWORD_Id -> STRUCT;
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

        parseTypeRef();

        constraint.done(TYPE_CONSTRAINT);
    }

    private void parseMemberDeclaration() {
        if (at(SEMICOLON)) {
            advance(); // SEMICOLON
            return;
        }
        PsiBuilder.Marker decl = mark();


        ModifierDetector detector = new ModifierDetector();
        parseModifierList(detector, TokenSet.EMPTY);


        IElementType declType = parseMemberDeclarationRest();

        if (declType == null) {
            errorWithRecovery("Expecting member declaration", TokenSet.EMPTY);
            decl.drop();
        } else {
            closeDeclarationWithCommentBinders(decl, declType, true);
        }
    }

    private IElementType parseMemberDeclarationRest() {
        IElementType declType = parseClassCommonDeclaration();

        if (declType != null) return declType;

        if (at(INIT_KEYWORD)) {
            advance(); // init
            if (at(LBRACE)) {
                parseBlock();
            } else {
                mark().error("Expecting '{' after 'init'");
            }
            declType = CLASS_INITIALIZER;
        } else if (at(LBRACE)) {
            error("Expecting member declaration");
            parseBlock();
            declType = FUNC;
        }
        return declType;
    }

    /**
     * members
     * : memberDeclaration*
     * ;
     */
    private void parseMembers() {
        while (!eof() && !at(RBRACE)) {
            parseMemberDeclaration();
        }
    }

    private void parseClassBody() {
        PsiBuilder.Marker body = mark();

        myBuilder.enableNewlines();

        if (expect(LBRACE, "Expecting a class body")) {
            parseMembers();
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
            return FUNC;
        }
        myBuilder.disableJoiningComplexTokens();
        //类型参数
        if (at(LPAR)) {
            parseValueParameterList(false, false, VALUE_PARAMETERS_FOLLOW_SET);

        } else {
            error("Expecting '(' ");  //应该为'('
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
        return parseFunction(false);
    }

    /*
     * IDENTIFIER 标识符
     */
    private void parseIdentifier() {
        if (expect(IDENTIFIER)) return;

        if (atSet(KEYWORDS)) {
            error("Keywords cannot be used"); //关键字不能使用
        }

        error("Expecting an CangJie identifier"); //应该为标识符

    }
    public enum NameParsingMode {
        REQUIRED, ALLOWED, PROHIBITED
    }
    /*
     * IDENTIFIER
     */
    private void parseIdentifierByTitle(
            String title, TokenSet recoverySet
    ) {


        if (expect(IDENTIFIER)) {
            return;
        }

        errorWithRecovery("Expecting " + title + " name", recoverySet);
    }

    private void parseIdentifierByTitle(String title) {
        parseIdentifierByTitle(title, TokenSet.EMPTY);
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
      IElementType parseFunction(boolean failIfIdentifierExists) {
        assert _at(FUNC_KEYWORD);
        advance();

        if (at(RBRACE)) {
            error("Function body expected");  //应该为函数体
            return FUNC;
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
        //函数体
//        if (at(SEMICOLON)) {
//            advance(); // SEMICOLON
//        } else
        if (at(LBRACE)) {
            parseFunctionBody();
        } else {
            error("Expecting '{' ");  //应该为'{'
        }

        return FUNC;
    }

    /*
     * functionBody
     *   : block
     *   : "=" element
     *   ;
     */
    private void parseFunctionBody() {
        if (at(LBRACE)) {
            parseBlock();
        } else {
            error("Expecting function body"); //应该为函数体
        }
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
    private void parseValueParameterList(boolean isFunctionTypeContents, boolean typeRequired, TokenSet recoverySet) {
        assert at(LPAR);
        PsiBuilder.Marker parameters = mark();


        myBuilder.disableNewlines();
        advance(); // (


        if (!at(RPAR) && !atSet(recoverySet)) {
            while (true) {
                //第一个不能为,
                if (at(COMMA)) {
                    errorAndAdvance("Expecting a parameter declaration");  //应该为参数声明

                } else if (at(RPAR)) {  //如果为)则跳出循环
                    break;
                }


//                if (isFunctionTypeContents) {
//
//                }
//                else {
                parseValueParameter(typeRequired);
//                }


                if (at(COMMA)) {
                    advance(); // COMMA
                } else if (at(COLON)) {
                    continue;
                } else {
                    if (!at(RPAR)) error("Expecting Parameter list or ')'");
                    if (!atSet(isFunctionTypeContents ? LAMBDA_VALUE_PARAMETER_FIRST : VALUE_PARAMETER_FIRST)) break;
                }

            }


        }
        expect(RPAR, "Expecting ')'", recoverySet);
        myBuilder.restoreNewlinesState();

        parameters.done(VALUE_PARAMETER_LIST);
    }


    public void parseValueParameter(boolean typeRequired) {
        parseValueParameter(false, typeRequired);
    }


    private boolean parseValueParameter(boolean rollbackOnFailure, boolean typeRequired) {
        PsiBuilder.Marker parameter = mark();

//
//        if (at(VAR_KEYWORD) || at(LET_KEYWORD)) {
//            advance(); // VAR_KEYWORD | LET_KEYWORD
////            return false;
////            error("Expecting parameter declaration");  //应该为参数声明
//        }


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
            expect(IDENTIFIER, "Missing parameter name", PARAMETER_NAME_RECOVERY_SET);

            expect(EXCL);

            if (at(COLON)) {
                advance(); // :

                if (at(IDENTIFIER) && lookahead(1) == COLON) {
                    // 恢复 "func foo(x: y: Int)" 处理 'y:' 时，可能是下一个参数的名称
                    error("Type reference expected");
                    return false;
                }

                parseTypeRef();
            } else {
                errorWithoutAdvancing("Expecting ':' Missing type declaration");  //应该为':'
                noErrors = false;
            }


        }

        return noErrors;
    }

    /**
     * 解析类型引用
     *
     * @return
     */
//    private PsiBuilder.Marker parseTypeRefContents(TokenSet extraRecoverySet) {
//        PsiBuilder.Marker typeRefMarker = mark();
//
//        return typeRefMarker;
//    }
    private boolean parseUserType() {
        PsiBuilder.Marker usertype = mark();

        if (at(IDENTIFIER)) {
            advance();
            usertype.done(USER_TYPE);
            return true;
        }

        usertype.drop();


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
                }
                else if (at(RPAR)) { // For declaration similar to `val () = somethingCall()`
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
     *
     */
    boolean tryParseTypeArgumentList(TokenSet extraRecoverySet) {

        myBuilder.disableNewlines();
        advance(); // LT

        while (true) {
            PsiBuilder.Marker projection = mark();



            if (at(MUL)) {
                advance(); // MUL
            }
            else {
                parseTypeRef(extraRecoverySet);
            }
            projection.done(TYPE_PROJECTION);
            if (!at(COMMA)) break;
            advance(); // COMMA
            if (at(GT)) {
                break;
            }
        }

        boolean atGT = at(GT);
        if (!atGT) {
            error("Expecting a '>'");
        }
        else {
            advance(); // GT
        }
        myBuilder.restoreNewlinesState();
        return atGT;

    }
    void parseTypeRef(TokenSet extraRecoverySet) {

        PsiBuilder.Marker typeRefMarker = mark();
        //先解析基本类型，如果不是基本类型，则解析类型引用

        expect(QUEST);

        if (parseBasicType() || parseUserType()) {
            typeRefMarker.done(TYPE_REFERENCE);
//            return;
        } else {
            error("Expecting a type reference ");
            typeRefMarker.drop();
//            return;
        }


    }




    static class ModifierDetector implements Consumer<IElementType> {
        private final boolean companionDetected = false;
        private boolean enumDetected = false;

        @Override
        public void consume(IElementType item) {
            if (item == CjTokens.ENUM_KEYWORD) {
                enumDetected = true;
            }
        }

        public boolean isEnumDetected() {
            return enumDetected;
        }

        public boolean isCompanionDetected() {
            return companionDetected;
        }
    }
}

