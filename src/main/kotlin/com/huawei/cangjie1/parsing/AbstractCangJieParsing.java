package com.huawei.cangjie1.parsing;

import com.huawei.cangjie1.lexer.CjKeywordToken;
import com.huawei.cangjie1.lexer.CjToken;
import com.huawei.cangjie1.lexer.CjTokens;
import com.huawei.cangjie1.utils.StringsKt;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.HashMap;
import java.util.Map;

import static com.huawei.cangjie1.lexer.CjTokens.*;


abstract class AbstractCangJieParsing {
    private static final Map<String, CjKeywordToken> SOFT_KEYWORD_TEXTS = new HashMap<>();

    static {
        for (IElementType type : CjTokens.SOFT_KEYWORDS.getTypes()) {
            CjKeywordToken keywordToken = (CjKeywordToken) type;
            assert keywordToken.isSoft();
            SOFT_KEYWORD_TEXTS.put(keywordToken.getValue(), keywordToken);
        }
    }

    static {
        for (IElementType token : CjTokens.KEYWORDS.getTypes()) {
            assert token instanceof CjKeywordToken : "Must be CjKeywordToken: " + token;
            assert !((CjKeywordToken) token).isSoft() : "Must not be soft: " + token;
        }
    }

    protected final SemanticWhitespaceAwarePsiBuilder myBuilder;
    protected final boolean isLazy;

    public AbstractCangJieParsing(SemanticWhitespaceAwarePsiBuilder builder) {
        this(builder, true);
    }

    public AbstractCangJieParsing(SemanticWhitespaceAwarePsiBuilder builder, boolean isLazy) {
        this.myBuilder = builder;
        this.isLazy = isLazy;
    }

    /**
     * 获取标记流中的最后一个标记类型
     *
     * @return
     */
    protected IElementType getLastToken() {
        int i = 1;
        int currentOffset = myBuilder.getCurrentOffset();
        while (i <= currentOffset && WHITE_SPACE_OR_COMMENT_BIT_SET.contains(myBuilder.rawLookup(-i))) {
            i++;
        }
        return myBuilder.rawLookup(-i);
    }

    /**
     * 检查当前标记是否为指定的 CjToken 类型，并在标记不匹配时报告错误
     *
     * @param expectation
     * @param message
     * @return
     */
    protected boolean expect(CjToken expectation, String message) {
        return expect(expectation, message, null);
    }

    /**
     * 在标记流中创建一个标记，并返回该标记的 PsiBuilder.Marker 对象
     *
     * @return
     */
    protected PsiBuilder.Marker mark() {
        return myBuilder.mark();
    }

    /**
     * 报告解析错误并停止解析过程
     *
     * @param message
     */
    protected void error(String message) {
        myBuilder.error(message);
    }

    /**
     * 检查当前标记是否为指定的复杂标记类型，并在标记不匹配时报告错误
     *
     * @param expectation
     * @param message
     * @param recoverySet
     * @return
     */
    protected boolean expect(CjToken expectation, String message, TokenSet recoverySet) {
        if (expect(expectation)) {
            return true;
        }

        errorWithRecovery(message, recoverySet);

        return false;
    }

    /**
     * 检查当前标记是否为指定的 CjToken 类型，并在标记不匹配时报告错误
     *
     * @param expectation
     * @return
     */
    protected boolean expect(CjToken expectation) {
        if (at(expectation)) {
            advance(); // 预期
            return true;
        }

        if (expectation == CjTokens.IDENTIFIER && "`".equals(myBuilder.getTokenText())) {
            advance();
        }

        return false;
    }

    /**
     * 检查当前标记是否为指定的 CjToken 类型，但不会消耗标记流中的标记
     *
     * @param expectation
     * @param message
     */
    protected void expectNoAdvance(CjToken expectation, String message) {
        if (at(expectation)) {
            advance(); // 预期
            return;
        }

        error(message);
    }

    /**
     * 报告解析错误并尝试恢复解析过程
     *
     * @param message
     * @param recoverySet
     */
    protected void errorWithRecovery(String message, TokenSet recoverySet) {
        IElementType tt = tt();
        if (recoverySet == null ||
                recoverySet.contains(tt) ||
                tt == LBRACE || tt == RBRACE ||
                (recoverySet.contains(EOL_OR_SEMICOLON) && (eof() || tt == SEMICOLON || myBuilder.newlineBeforeCurrentToken()))) {
            error(message);
        } else {
            errorAndAdvance(message);
        }
    }

    /**
     * 报告解析错误并消耗当前标记
     *
     * @param message
     */
    protected void errorAndAdvance(String message) {
        errorAndAdvance(message, 1);
    }

    /**
     * 报告解析错误并消耗指定数量的标记
     *
     * @param message
     * @param advanceTokenCount
     */
    protected void errorAndAdvance(String message, int advanceTokenCount) {
        PsiBuilder.Marker err = mark();
        advance(advanceTokenCount);
        err.error(message);
    }





    /**
     * 是否到文件结尾
     *
     * @return
     */
    protected boolean eof() {
        return myBuilder.eof();
    }

    /**
     * 推进标记流并返回下一个标记
     */
    protected void advance() {
        // TODO: 如何在错误字符上报告错误？(除突出显示外)
        myBuilder.advanceLexer();
    }

    /**
     * 推进标记流并返回下  advanceTokenCount 个标记
     *
     * @param advanceTokenCount
     */
    protected void advance(int advanceTokenCount) {
        for (int i = 0; i < advanceTokenCount; i++) {
            advance(); // 错误的令牌
        }
    }

    /**
     * 推进标记流并返回下一个指定类型的标记
     *
     * @param current
     */
    protected void advanceAt(IElementType current) {
        assert _at(current);
        myBuilder.advanceLexer();
    }

    /**
     * 获取当前标记的类型id
     *
     * @return
     */
    protected int getTokenId() {
        IElementType elementType = tt();
        return (elementType instanceof CjToken) ? ((CjToken) elementType).getTokenId() : INVALID_Id;
    }

    /**
     * 获取当前标记的类型
     *
     * @return
     */
    protected IElementType tt() {
        return myBuilder.getTokenType();
    }

    /**
     * 无副作用版本的at()
     */
    protected boolean _at(IElementType expectation) {
        IElementType token = tt();
        return tokenMatches(token, expectation);
    }

    /**
     * 检查当前标记是否与预期标记匹配
     *
     * @param token
     * @param expectation
     * @return
     */
    private boolean tokenMatches(IElementType token, IElementType expectation) {
        if (token == expectation) return true;
        if (expectation == EOL_OR_SEMICOLON) {
            if (eof()) return true;
            if (token == SEMICOLON) return true;
            return myBuilder.newlineBeforeCurrentToken();
        }
        return false;
    }

    /**
     * 检查当前标记是否与预期标记匹配
     *
     * @param expectation
     * @return
     */
    protected boolean at(IElementType expectation) {
        if (_at(expectation)) return true;
        IElementType token = tt();
        if (token == IDENTIFIER && expectation instanceof CjKeywordToken expectedKeyword) {
            if (expectedKeyword.isSoft() && expectedKeyword.getValue().equals(myBuilder.getTokenText())) {
                myBuilder.remapCurrentToken(expectation);
                return true;
            }
        }
        if (expectation == IDENTIFIER && token instanceof CjKeywordToken keywordToken) {
            if (keywordToken.isSoft()) {
                myBuilder.remapCurrentToken(IDENTIFIER);
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前标记是否与预期标记匹配
     *
     * @param set
     * @return
     */
    protected boolean _atSet(TokenSet set) {
        IElementType token = tt();
        if (set.contains(token)) return true;
        if (set.contains(EOL_OR_SEMICOLON)) {
            if (eof()) return true;
            if (token == SEMICOLON) return true;
            return myBuilder.newlineBeforeCurrentToken();
        }
        return false;
    }

    /**
     * 检查当前标记是否与预期标记匹配
     *
     * @param set
     * @return
     */
    protected boolean atSet(TokenSet set) {
        if (_atSet(set)) return true;
        IElementType token = tt();
        if (token == IDENTIFIER) {
            CjKeywordToken keywordToken = SOFT_KEYWORD_TEXTS.get(myBuilder.getTokenText());
            if (set.contains(keywordToken)) {
                myBuilder.remapCurrentToken(keywordToken);
                return true;
            }
        } else {
            // We know at this point that <code>set</code> does not contain <code>token</code>
            if (set.contains(IDENTIFIER) && token instanceof CjKeywordToken) {
                if (((CjKeywordToken) token).isSoft()) {
                    myBuilder.remapCurrentToken(IDENTIFIER);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 查看标记流中的下 k 个标记，而不会推进标记流
     *
     * @param k
     * @return
     */
    protected IElementType lookahead(int k) {

        return myBuilder.lookAhead(k);
    }

    /**
     * 检查当前标记是否为指定的复杂标记，并在标记匹配时推进标记流
     *
     * @param token
     * @return
     */
    protected boolean consumeIf(CjToken token) {
        if (at(token)) {
            advance(); // token
            return true;
        }
        return false;
    }

    /**
     * 跳过标记流中的标记，直到找到指定的标记集合中的任何一个标记
     * @param tokenSet
     */
    protected void skipUntil(TokenSet tokenSet) {
        boolean stopAtEolOrSemi = tokenSet.contains(EOL_OR_SEMICOLON);
        while (!eof() && !tokenSet.contains(tt()) && !(stopAtEolOrSemi && at(EOL_OR_SEMICOLON))) {
            advance();
        }
    }


    /**
     * 报告解析错误并跳过标记流中指定的标记集合，直到遇到指定的标记集合中的任意一个标记
     *
     * @param message
     * @param tokenSet
     */
    protected void errorUntil(String message, TokenSet tokenSet) {
        assert tokenSet.contains(LBRACE) : "Cannot include LBRACE into error element!";
        assert tokenSet.contains(RBRACE) : "Cannot include RBRACE into error element!";
        PsiBuilder.Marker error = mark();
        skipUntil(tokenSet);
        error.error(message);
    }

    /**
     * 在满足指定条件时报告解析错误
     *
     * @param marker
     * @param condition
     * @param message
     */
    protected static void errorIf(PsiBuilder.Marker marker, boolean condition, String message) {
        if (condition) {
            marker.error(message);
        } else {
            marker.drop();
        }
    }

    /**
     * 表示一个可选的标记
     */
    protected class OptionalMarker {
        private final PsiBuilder.Marker marker;
        private final int offset;

        /**
         * 创建一个可选的标记
         *
         * @param actuallyMark
         */
        public OptionalMarker(boolean actuallyMark) {
            marker = actuallyMark ? mark() : null;
            offset = myBuilder.getCurrentOffset();
        }

        /**
         * 标记可选的语法单元已经解析完成，并将其转换为指定类型的语法单元
         *
         * @param elementType
         */
        public void done(IElementType elementType) {
            if (marker == null) return;
            marker.done(elementType);
        }

        /**
         * 报告解析错误
         *
         * @param message
         */
        public void error(String message) {
            if (marker == null) return;
            if (offset == myBuilder.getCurrentOffset()) {
                marker.drop(); // no empty errors
            } else {
                marker.error(message);
            }
        }

        /**
         * 用于删除当前位置的标记
         */
        public void drop() {
            if (marker == null) return;
            marker.drop();
        }
    }

    /**
     * 匹配指定的标记流模式
     *
     * @param pattern
     * @return
     */
    protected int matchTokenStreamPredicate(TokenStreamPattern pattern) {
        PsiBuilder.Marker currentPosition = mark();
        Stack<IElementType> opens = new Stack<>();
        int openAngleBrackets = 0;
        int openBraces = 0;
        int openParentheses = 0;
        int openBrackets = 0;
        while (!eof()) {
            if (pattern.processToken(
                    myBuilder.getCurrentOffset(),
                    pattern.isTopLevel(openAngleBrackets, openBrackets, openBraces, openParentheses))) {
                break;
            }
            switch (getTokenId()) {
                case LPAR_Id:
                    openParentheses++;
                    opens.push(LPAR);
                    break;
                case LT_Id:
                    openAngleBrackets++;
                    opens.push(LT);
                    break;
                case LBRACE_Id:
                    openBraces++;
                    opens.push(LBRACE);
                    break;
                case LBRACKET_Id:
                    openBrackets++;
                    opens.push(LBRACKET);
                    break;
                case RPAR_Id:
                    openParentheses--;
                    if (opens.isEmpty() || opens.pop() != LPAR) {
                        if (pattern.handleUnmatchedClosing(RPAR)) {
                            break;
                        }
                    }
                    break;
                case GT_Id:
                    openAngleBrackets--;
                    break;
                case RBRACE_Id:
                    openBraces--;
                    break;
                case RBRACKET_Id:
                    openBrackets--;
                    break;
            }

            advance(); // skip token
        }

        currentPosition.rollbackTo();

        return pattern.result();
    }

    /**
     * 检查当前标记是否位于行末
     * @return
     */
    protected boolean eol() {
        return myBuilder.newlineBeforeCurrentToken() || eof();
    }

    /**
     * 关闭声明并绑定注释
     * @param marker
     * @param elementType
     * @param precedingNonDocComments
     */
    protected static void closeDeclarationWithCommentBinders(@NotNull PsiBuilder.Marker marker, @NotNull IElementType elementType, boolean precedingNonDocComments) {
        marker.done(elementType);
        marker.setCustomEdgeTokenBinders(precedingNonDocComments ? PrecedingCommentsBinder.INSTANCE : PrecedingDocCommentsBinder.INSTANCE,
                TrailingCommentsBinder.INSTANCE);
    }

    protected abstract CangJieParsing create(SemanticWhitespaceAwarePsiBuilder builder);

    protected CangJieParsing createTruncatedBuilder(int eofPosition) {
        return create(new TruncatedSemanticWhitespaceAwarePsiBuilder(myBuilder, eofPosition));
    }

    protected class At extends AbstractTokenStreamPredicate {

        private final IElementType lookFor;
        private final boolean topLevelOnly;

        public At(IElementType lookFor, boolean topLevelOnly) {
            this.lookFor = lookFor;
            this.topLevelOnly = topLevelOnly;
        }

        public At(IElementType lookFor) {
            this(lookFor, true);
        }

        @Override
        public boolean matching(boolean topLevel) {
            return (topLevel || !topLevelOnly) && at(lookFor);
        }

    }

    protected class AtSet extends AbstractTokenStreamPredicate {
        private final TokenSet lookFor;
        private final TokenSet topLevelOnly;

        public AtSet(TokenSet lookFor, TokenSet topLevelOnly) {
            this.lookFor = lookFor;
            this.topLevelOnly = topLevelOnly;
        }

        public AtSet(TokenSet lookFor) {
            this(lookFor, lookFor);
        }

        @Override
        public boolean matching(boolean topLevel) {
            return (topLevel || !atSet(topLevelOnly)) && atSet(lookFor);
        }
    }

    /**
     * 获取当前解析上下文的字符串表示
     * @return
     */
    @SuppressWarnings("UnusedDeclaration")
    @TestOnly
    public String currentContext() {
        return StringsKt.substringWithContext(myBuilder.getOriginalText(), myBuilder.getCurrentOffset(), myBuilder.getCurrentOffset(), 20);
    }
}
