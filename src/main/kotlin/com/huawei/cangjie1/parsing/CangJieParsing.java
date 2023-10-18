package com.huawei.cangjie1.parsing;

import com.huawei.cangjie1.lexer.CjTokens;
import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


import static com.huawei.cangjie1.lexer.CjTokens.*;

import static com.huawei.cangjie1.CjNodeTypes.*;
import static com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes.TYPE_REFERENCE;
import static com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes.VALUE_PARAMETER;


public class CangJieParsing extends AbstractCangJieParsing {
    private static final TokenSet GT_COMMA_COLON_SET = TokenSet.create(GT, COMMA, COLON);
    private static final Logger LOG = Logger.getInstance(CangJieParsing.class);

    private static final TokenSet TOP_LEVEL_DECLARATION_FIRST = TokenSet.create( INTERFACE_KEYWORD, CLASS_KEYWORD, FUNC_KEYWORD, LET_KEYWORD, PACKAGE_KEYWORD);
    private static final TokenSet TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET = TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(SEMICOLON));
    private static final TokenSet LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET = TokenSet.orSet(TokenSet.create(LT, EQ, SEMICOLON), TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet DECLARATION_FIRST = TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(INIT_KEYWORD, GET_KEYWORD, SET_KEYWORD, CONSTRUCTOR_KEYWORD));

    private static final TokenSet CLASS_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(LT, LPAR, COLON, LBRACE), TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(WHERE_KEYWORD, LPAR, COLON, LBRACE, GT);
    public static final TokenSet PARAMETER_NAME_RECOVERY_SET = TokenSet.create(COLON, EQ, COMMA, RPAR, LET_KEYWORD, VAR_KEYWORD);
    private static final TokenSet PACKAGE_NAME_RECOVERY_SET = TokenSet.create(DOT, EOL_OR_SEMICOLON);
    private static final TokenSet IMPORT_RECOVERY_SET = TokenSet.create(AS_KEYWORD, DOT, EOL_OR_SEMICOLON);
    private static final TokenSet TYPE_REF_FIRST = TokenSet.create(LBRACKET, IDENTIFIER, LPAR, HASH);
    private static final TokenSet LBRACE_RBRACE_TYPE_REF_FIRST_SET = TokenSet.orSet(TokenSet.create(LBRACE, RBRACE), TYPE_REF_FIRST);
    private static final TokenSet COLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET = TokenSet.orSet(TokenSet.create(COLON, COMMA, LBRACE, RBRACE), TYPE_REF_FIRST);
    private static final TokenSet RECEIVER_TYPE_TERMINATORS = TokenSet.create(DOT);
    private static final TokenSet VALUE_PARAMETER_FIRST = TokenSet.orSet(TokenSet.create(IDENTIFIER, LBRACKET, LET_KEYWORD, VAR_KEYWORD), TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUNC_KEYWORD)));
    private static final TokenSet LAMBDA_VALUE_PARAMETER_FIRST = TokenSet.orSet(TokenSet.create(IDENTIFIER, LBRACKET), TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUNC_KEYWORD)));
    private static final TokenSet SOFT_KEYWORDS_AT_MEMBER_START = TokenSet.create(CONSTRUCTOR_KEYWORD, INIT_KEYWORD);
    private static final TokenSet ANNOTATION_TARGETS = TokenSet.create(FILE_KEYWORD, FIELD_KEYWORD, GET_KEYWORD, SET_KEYWORD, PROPERTY_KEYWORD, RECEIVER_KEYWORD, PARAM_KEYWORD, SETPARAM_KEYWORD, DELEGATE_KEYWORD);
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
    private static final TokenSet ACCESSOR_FIRST_OR_PROPERTY_END = TokenSet.orSet(MODIFIER_KEYWORDS, TokenSet.create(GET_KEYWORD, SET_KEYWORD, FIELD_KEYWORD, EOL_OR_SEMICOLON, RBRACE));
    private static final TokenSet RPAR_IDENTIFIER_COLON_LBRACE_EQ_SET = TokenSet.create(RPAR, IDENTIFIER, COLON, LBRACE, EQ);
    private static final TokenSet COMMA_COLON_RPAR_SET = TokenSet.create(COMMA, COLON, RPAR);
    private static final TokenSet RPAR_COLON_LBRACE_EQ_SET = TokenSet.create(RPAR, COLON, LBRACE, EQ);
    private static final TokenSet LBRACKET_LBRACE_RBRACE_LPAR_SET = TokenSet.create(LBRACKET, LBRACE, RBRACE, LPAR);
    private static final TokenSet FUNCTION_NAME_FOLLOW_SET = TokenSet.create(LT, LPAR, RPAR, COLON, EQ);
    private static final TokenSet FUNCTION_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(LT, LPAR, RPAR, COLON, EQ), LBRACE_RBRACE_SET, TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet VALUE_PARAMETERS_FOLLOW_SET = TokenSet.create(EQ, LBRACE, RBRACE, SEMICOLON, RPAR);
    private static final TokenSet LPAR_VALUE_PARAMETERS_FOLLOW_SET = TokenSet.orSet(TokenSet.create(LPAR), VALUE_PARAMETERS_FOLLOW_SET);
    private static final TokenSet LPAR_LBRACE_COLON_CONSTRUCTOR_KEYWORD_SET = TokenSet.create(LPAR, LBRACE, COLON, CONSTRUCTOR_KEYWORD);
    private static final TokenSet definitelyOutOfReceiverSet = TokenSet.orSet(TokenSet.create(EQ, COLON, LBRACE, RBRACE, BY_KEYWORD), TOP_LEVEL_DECLARATION_FIRST);
    private final static TokenSet EOL_OR_SEMICOLON_RBRACE_SET = TokenSet.create(EOL_OR_SEMICOLON, RBRACE);
    private final static TokenSet CLASS_INTERFACE_SET = TokenSet.create(CLASS_KEYWORD, INTERFACE_KEYWORD);

    private CangJieParsing(SemanticWhitespaceAwarePsiBuilder builder, boolean isTopLevel, boolean isLazy) {
        super(builder, isLazy);


    }

    private static CangJieParsing createForByClause(SemanticWhitespaceAwarePsiBuilder builder, boolean isLazy) {
        return new CangJieParsing(new SemanticWhitespaceAwarePsiBuilderForByClause(builder), false, isLazy);
    }

    @Override
    protected CangJieParsing create(SemanticWhitespaceAwarePsiBuilder builder) {
        return createForTopLevel(builder);
    }


    private void parseBlock(boolean collapse) {
        PsiBuilder.Marker lazyBlock = mark();

        myBuilder.enableNewlines();

        boolean hasOpeningBrace = expect(LBRACE, "Expecting '{'  ");
        boolean canCollapse = collapse && hasOpeningBrace && isLazy;


        expect(RBRACE, "Expecting '}'");


        myBuilder.restoreNewlinesState();

        if (canCollapse) {
            lazyBlock.collapse(BLOCK);
        } else {
            lazyBlock.done(BLOCK);
        }
    }

    void parseBlock() {
        parseBlock(/*collapse*/ true);
    }

    void parseBlockExpression() {
        parseBlock(/* collapse = */ false);
    }

    static CangJieParsing createForTopLevel(SemanticWhitespaceAwarePsiBuilder builder) {
        return new CangJieParsing(builder, true, true);
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
//            parsePackageName();

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

    }

    //入口
    void parseFile() {
        PsiBuilder.Marker fileMarker = mark();

        //处理开头  package
        parsePreamble();

        //处理声明式语句
        while (!eof()) {
            parseTopLevelDeclaration();
        }


        fileMarker.done(CJ_FILE);
    }

    static class ModifierDetector implements Consumer<IElementType> {
        private boolean enumDetected = false;
        private final boolean companionDetected = false;

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

    public enum NameParsingMode {
        REQUIRED, ALLOWED, PROHIBITED
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

        ModifierDetector modifierDetector = new ModifierDetector();

        IElementType declType = parseCommonDeclaration();


        if (declType == null) {

            errorAndAdvance("Expecting a top level declaration"); //期待一个顶层声明语句
            decl.drop();
        } else {
            closeDeclarationWithCommentBinders(decl, declType, true);
        }


    }

    public IElementType parseCommonDeclaration(

    ) {


        switch (getTokenId()) {
            case FUNC_KEYWORD_Id:
                return parseFunction();
            case MAIN_KEYWORD_Id:
                return parseMainFunc();
        }

        return null;
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
            parseValueParameterList(false, /* typeRequired  = */ false, VALUE_PARAMETERS_FOLLOW_SET);

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
    private IElementType parseFunction() {
        return parseFunction(false);
    }

    /*
     * IDENTIFIER 标识符
     */
    private void parseIdentifier() {
        if (expect(IDENTIFIER)) return;

        error("Expecting an CangJie identifier"); //应该为标识符

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
    private IElementType parseFunction(boolean failIfIdentifierExists) {
        assert _at(FUNC_KEYWORD);
        advance();

        if (at(RBRACE)) {
            error("Function body expected");  //应该为函数体
            return FUNC;
        }


        myBuilder.disableJoiningComplexTokens();


        //函数名
        parseIdentifier();


        //类型参数
        if (at(LPAR)) {
            parseValueParameterList(false, /* typeRequired  = */ false, VALUE_PARAMETERS_FOLLOW_SET);

        } else {
            error("Expecting '(' ");  //应该为'('
        }

        //返回值类型
        if (at(COLON)) {
            advance(); // COLON
//            parseTypeRef();
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


        if (at(VAR_KEYWORD) || at(LET_KEYWORD)) {
            advance(); // VAR_KEYWORD | VAL_KEYWORD
        }
//
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
            }


        }

        return noErrors;
    }

    private PsiBuilder.Marker parseTypeRefContents(TokenSet extraRecoverySet, boolean allowSimpleIntersectionTypes) {
        PsiBuilder.Marker typeRefMarker = mark();

        return typeRefMarker;
    }

    private void parseTypeRef(TokenSet extraRecoverySet, boolean allowSimpleIntersectionTypes) {
        PsiBuilder.Marker typeRefMarker = parseTypeRefContents(extraRecoverySet, allowSimpleIntersectionTypes);
        typeRefMarker.done(TYPE_REFERENCE);
    }

    void parseTypeRef(TokenSet extraRecoverySet) {
        parseTypeRef(extraRecoverySet, /* allowSimpleIntersectionTypes */ true);
    }

    void parseTypeRef() {
        parseTypeRef(TokenSet.EMPTY);
    }
}

