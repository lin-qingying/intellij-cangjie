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

import com.intellij.analysis.AnalysisBundle;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.lang.*;
import com.intellij.lang.impl.PsiBuilderAdapter;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringHash;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.Function;
import com.intellij.util.PairProcessor;
import com.intellij.util.containers.LimitedPool;
import com.intellij.util.containers.Stack;
import com.linqingying.cangjie.lexer.CjKeywordToken;
import com.linqingying.cangjie.lexer.CjToken;
import com.linqingying.cangjie.utils.StringsKt;
import org.jetbrains.annotations.*;

import java.util.*;

import static com.intellij.openapi.util.text.StringUtil.*;
import static com.linqingying.cangjie.lexer.CjTokens.*;


public abstract class AbstractCangJieParsing {
    // here's the new section API for compact parsers & less IntelliJ platform API exposure
    public static final int _NONE_ = 0x0;
    public static final int _COLLAPSE_ = 0x1;
    public static final int _LEFT_ = 0x2;
    public static final int _LEFT_INNER_ = 0x4;
    public static final int _AND_ = 0x8;
    public static final int _NOT_ = 0x10;
    public static final int _UPPER_ = 0x20;
    public static final Parser TOKEN_ADVANCER = (builder, level) -> {
        if (builder.eof()) return false;
        builder.advanceLexer();
        return true;
    };
    public static final Key<CompletionState> COMPLETION_STATE_KEY = Key.create("COMPLETION_STATE_KEY");
    private static final Map<String, CjKeywordToken> SOFT_KEYWORD_TEXTS = new HashMap<>();
    private static final int MAX_VARIANTS_SIZE = 10000;
    private static final int MAX_VARIANTS_TO_DISPLAY = 50;
    private static final int MAX_ERROR_TOKEN_TEXT = 20;
    private static final int INITIAL_VARIANTS_SIZE = 1000;
    private static final int VARIANTS_POOL_SIZE = 10000;
    private static final int FRAMES_POOL_SIZE = 500;

    static {
        for (IElementType type : SOFT_KEYWORDS.getTypes()) {
            CjKeywordToken keywordToken = (CjKeywordToken) type;
            assert keywordToken.isSoft();
            SOFT_KEYWORD_TEXTS.put(keywordToken.getValue(), keywordToken);
        }
    }

    static {
        for (IElementType token : KEYWORDS.getTypes()) {
            assert token instanceof CjKeywordToken : "Must be CjKeywordToken: " + token;
            assert !((CjKeywordToken) token).isSoft() : "Must not be soft: " + token;
        }
    }

    public final ErrorState state = new ErrorState();
    protected final SemanticWhitespaceAwarePsiBuilder myBuilder;
    protected final boolean isLazy;
    protected boolean isDeclarationsFile;

    protected AbstractCangJieParsing(SemanticWhitespaceAwarePsiBuilder builder) {
        this(builder, true);
    }

    protected AbstractCangJieParsing(SemanticWhitespaceAwarePsiBuilder builder, boolean isLazy) {
        myBuilder = builder;
        this.isLazy = isLazy;


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
     * 关闭声明并绑定注释
     *
     * @param marker
     * @param elementType
     * @param precedingNonDocComments
     */
    protected static void closeDeclarationWithCommentBinders(@NotNull PsiBuilder.Marker marker, @NotNull IElementType elementType, boolean precedingNonDocComments) {
        marker.done(elementType);
        marker.setCustomEdgeTokenBinders(precedingNonDocComments ? PrecedingCommentsBinder.INSTANCE : PrecedingDocCommentsBinder.INSTANCE,
                TrailingCommentsBinder.INSTANCE);
    }

    public static boolean isWhitespaceOrComment(@NotNull PsiBuilder builder, @Nullable IElementType type) {
        return ((PsiBuilderImpl) builder).whitespaceOrComment(type);
    }

    protected List<PsiBuilderImpl.ProductionMarker> getProductions() {

        return ((PsiBuilderImpl) ((PsiBuilderAdapter) myBuilder).getDelegate()).getProductions();
    }

    public void setDeclarationsFile(boolean isDeclarationsFile) {
        this.isDeclarationsFile = isDeclarationsFile;


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

    protected IElementType getSafeTokenType() {
        IElementType tokenType = tt();
        if (tokenType != QUEST) return tokenType;
        if (rawLookup(1) == QUEST) {

            tokenType = COALESCING;

        } else if (rawLookup(1) == LPAR) {
            tokenType = SAFE_CALL;
        }
        return tokenType;
    }

    protected void advanceSafeToken(IElementType type) {
        PsiBuilder.Marker safeToken = mark();
        if (type == COALESCING || type == SAFE_CALL) {
            PsiBuilderUtil.advance(myBuilder, 2);
        } else {
            safeToken.drop();
            advance();
            return;
        }
        safeToken.collapse(type);
    }

    protected IElementType getGtTokenType() {
        IElementType tokenType = tt();

        if (tokenType == QUEST && rawLookup(1) == QUEST) return COALESCING;

        if (tokenType != GT) return tokenType;
        if (rawLookup(1) == GT) {
            if (rawLookup(2) == EQ) {
                tokenType = GTGTEQ;
            } else {
                tokenType = GTGT;
            }
        } else if (rawLookup(1) == EQ) {
            tokenType = GTEQ;
        }
        return tokenType;
    }

    //    获取可以呗重载的操作符标记 OPERATIONS
    protected IElementType getOperationTokenType() {
        IElementType tokenType = getGtTokenType();

//        invoke
        if (tokenType == LPAR) {
            if (rawLookup(1) == RPAR) {
                tokenType = OPERATION_INVOKE;
            }

        }
        if (tokenType == LBRACKET) {
            if (rawLookup(1) == RBRACKET) {
                tokenType = OPERATION_GET;
            }
        }
        if (tokenType == EXCL) {
            tokenType = OPERATION_NOT;
        }
        if (tokenType == EXCLEQ) {
            tokenType = OPERATION_NOT_EQUALS;
        }
        if (tokenType == MULMUL) {
            tokenType = OPERATION_EXPONENTIATION;
        }
        if (tokenType == EQEQ) {
            tokenType = OPERATION_EQUALS;
        }
        if (tokenType == MUL) {
            tokenType = OPERATION_TIMES;
        }
        if (tokenType == DIV) {
            tokenType = OPERATION_DIV;
        }
        if (tokenType == PERC) {
            tokenType = OPERATION_REM;
        }

        if (tokenType == MINUS) {
            tokenType = OPERATION_MINUS;
        }
        if (tokenType == PLUS) {
            tokenType = OPERATION_PLUS;
        }
        if (tokenType == LTLT) {
            tokenType = OPERATION_LEFT_SHIFT;
        }
        if (tokenType == GTGT) {
            tokenType = OPERATION_RIGHT_SHIFT;
        }
        if (tokenType == GT) {
            tokenType = OPERATION_COMPARE_GT;
        }
        if (tokenType == LT) {
            tokenType = OPERATION_COMPARE_LT;
        }
        if (tokenType == LTEQ) {
            tokenType = OPERATION_COMPARE_LTEQ;
        }
        if (tokenType == GTEQ) {
            tokenType = OPERATION_COMPARE_GTEQ;
        }

        if (tokenType == AND) {
            tokenType = OPERATION_AND;
        }
        if (tokenType == XOR) {
            tokenType = OPERATION_XOR;
        }
        if (tokenType == OR) {
            tokenType = OPERATION_OR;
        }
        return tokenType;
    }

    protected void advanceOperationToken(IElementType type) {
        PsiBuilder.Marker gtToken = mark();

        if (type == OPERATION_INVOKE || type == OPERATION_GET || OPERATION_COMPARE_GTEQ == type || OPERATION_RIGHT_SHIFT == type) {
            PsiBuilderUtil.advance(myBuilder, 2);
        } else {
            gtToken.drop();
            advance();
            return;
        }
        gtToken.collapse(type);

    }

    protected void advanceGtToken(IElementType type) {
        PsiBuilder.Marker gtToken = mark();

        if (type == COALESCING) {
            PsiBuilderUtil.advance(myBuilder, 2);

        } else if (type == GTGTEQ) {
            PsiBuilderUtil.advance(myBuilder, 3);
        } else if (type == GTGT || type == GTEQ) {
            PsiBuilderUtil.advance(myBuilder, 2);
        } else {
            gtToken.drop();
            advance();
            return;
        }
        gtToken.collapse(type);
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

    //    获取上一个标记类型
    protected @Nullable LighterASTNode getLatestMarker() {
        return myBuilder.getLatestDoneMarker();
    }

    //    上一个已解析的标记是否以分号结尾
    protected boolean expectSemicolon() {
        return expect(SEMICOLON, "Expected semicolon");
    }

    /**
     * 报告解析错误并停止解析过程
     *
     * @param message
     */
    protected void error(String message) {
        myBuilder.error(message);
    }

    protected void errorBefore(String message, PsiBuilder.Marker marker) {
        PsiBuilder.Marker err = marker.precede();
//
//        marker.error(message);
//
////        err.done(ERROR_ELEMENT);


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

    protected boolean expectSafeCall(TokenSet expectationSet, String message, TokenSet recoverySet) {

        IElementType tokenType = getSafeTokenType();
        if (expectationSet.contains(tokenType)) {
            advanceSafeToken(tokenType);
            return true;
        }

        errorWithRecovery(message, recoverySet);

        return false;
    }

    protected boolean expect(TokenSet expectationSet, String message, TokenSet recoverySet) {
        if (expect(expectationSet)) {
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
            advance();
            return true;
        }

        if (expectation == IDENTIFIER && "`".equals(myBuilder.getTokenText())) {
            advance();
        }

        return false;
    }

    protected boolean expect(TokenSet expectationSet) {
        if (atSet(expectationSet)) {
            advance();
            return true;
        }
//
//        if (expectationSet == CjTokens.IDENTIFIER && "`".equals(myBuilder.getTokenText())) {
//            advance();
//        }

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
            advance();
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
        if (null == recoverySet ||
                recoverySet.contains(tt) ||
//                tt == LBRACE || tt == RBRACE ||
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
     * 报告错误，但不消耗标记
     */
    protected void errorWithoutAdvancing(String message) {
        mark().error(message);
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

    protected IElementType rawLookup(int steps) {
        return myBuilder.rawLookup(steps);
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
     * 如果当前标记与指定的标记匹配，则该方法将消耗该标记并返回true。否则，该方法将不会消耗标记并返回false
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
     * 如果当前标记与复杂标记匹配，则该方法将消耗该标记并返回true。否则，该方法将不会消耗标记并返回false
     */
    protected boolean consumeIfSet(TokenSet tokenSet) {
        if (atSet(tokenSet)) {
            advance(); // token
            return true;
        }
        return false;
    }

    /**
     * 根据传入的复杂类型顺序匹配标记流中的标记
     */
    protected boolean match(IElementType... types) {
        int i = 0;
        while (i < types.length && !eof()) {
            if (!at(types[i])) return false;
            advance();
            i++;
        }
        return i == types.length;
    }

    /**
     * 跳过标记流中的标记，直到找到指定的标记集合中的任何一个标记
     *
     * @param tokenSet
     */
    protected void skipUntil(TokenSet tokenSet) {
        boolean stopAtEolOrSemi = tokenSet.contains(EOL_OR_SEMICOLON);
        while (!eof() && !tokenSet.contains(tt()) && !(stopAtEolOrSemi && at(EOL_OR_SEMICOLON))) {
            advance();
        }
    }

    protected void errorUntil(String message, TokenSet tokenSet) {
        assert tokenSet.contains(LBRACE) : "Cannot include LBRACE into error element!";
        assert tokenSet.contains(RBRACE) : "Cannot include RBRACE into error element!";
        PsiBuilder.Marker error = mark();
        skipUntil(tokenSet);
        error.error(message);

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

            advance();
        }

        currentPosition.rollbackTo();

        return pattern.result();
    }

    /**
     * 检查当前标记是否位于行末
     *
     * @return
     */
    protected boolean eol() {
        return myBuilder.newlineBeforeCurrentToken() || eof();
    }

    protected abstract CangJieParsing create(SemanticWhitespaceAwarePsiBuilder builder);

    protected CangJieParsing createTruncatedBuilder(int eofPosition) {
        return create(new TruncatedSemanticWhitespaceAwarePsiBuilder(myBuilder, eofPosition));
    }

    /**
     * 获取当前解析上下文的字符串表示
     *
     * @return
     */
    @SuppressWarnings("UnusedDeclaration")
    @TestOnly
    public String currentContext() {
        return StringsKt.substringWithContext(myBuilder.getOriginalText(), myBuilder.getCurrentOffset(), myBuilder.getCurrentOffset(), 20);
    }

    public interface Parser {
        boolean parse(PsiBuilder builder, int level);
    }

    public interface Hook<T> {

        @Contract("_,null,_->null")
        PsiBuilder.Marker run(PsiBuilder builder, PsiBuilder.Marker marker, T param);

    }

    private static class MyList<E> extends ArrayList<E> {
        MyList(int initialCapacity) {
            super(initialCapacity);
        }

        protected void setSize(int fromIndex) {
            removeRange(fromIndex, size());
        }

        @Override
        public boolean add(E e) {
            int size = size();
            if (MAX_VARIANTS_SIZE <= size) {
                removeRange(MAX_VARIANTS_SIZE / 4, size - MAX_VARIANTS_SIZE / 4);
            }
            return super.add(e);
        }
    }

    private static class Variant {
        int position;
        Object object;

        public Variant init(int pos, Object o) {
            position = pos;
            object = o;
            return this;
        }

        @Override
        public String toString() {
            return "<" + position + ", " + object + ">";
        }
    }

    public static class CompletionState implements Function<Object, String> {
        public final int offset;
        public final Collection<String> items = new HashSet<>();

        public CompletionState(int offset_) {
            offset = offset_;
        }

        public @Nullable String convertItem(Object o) {
            return o instanceof Object[] ? join((Object[]) o, this, " ") : o.toString();
        }

        @Override
        public String fun(Object o) {
            return convertItem(o);
        }

        public void addItem(@NotNull PsiBuilder builder, @NotNull String text) {
            items.add(text);
        }

        public boolean prefixMatches(@NotNull PsiBuilder builder, @NotNull String text) {
            int builderOffset = builder.getCurrentOffset();
            int diff = offset - builderOffset;
            int length = text.length();
            if (0 == diff) {
                return true;
            } else if (0 < diff && diff <= length) {
                CharSequence fragment = builder.getOriginalText().subSequence(builderOffset, offset);
                return prefixMatches(fragment.toString(), text);
            } else if (0 > diff) {
                for (int i = -1; ; i--) {
                    IElementType type = builder.rawLookup(i);
                    int tokenStart = builder.rawTokenTypeStart(i);
                    if (isWhitespaceOrComment(builder, type)) {
                        diff = offset - tokenStart;
                    } else if (null != type && tokenStart < offset) {
                        CharSequence fragment = builder.getOriginalText().subSequence(tokenStart, offset);
                        if (prefixMatches(fragment.toString(), text)) {
                            diff = offset - tokenStart;
                        }
                        break;
                    } else break;
                }
                return 0 <= diff && diff < length;
            }
            return false;
        }

        public boolean prefixMatches(@NotNull String prefix, @NotNull String variant) {
            boolean matches = new CamelHumpMatcher(prefix, false).prefixMatches(variant.replace(' ', '_'));
            if (matches && isWhiteSpace(prefix.charAt(prefix.length() - 1))) {
                return startsWithIgnoreCase(variant, prefix);
            }
            return matches;
        }
    }

    public static class Frame {
        public Frame parentFrame;
        public IElementType elementType;

        public int offset;
        public int position;
        public int level;
        public int modifiers;
        public @NonNls String name;
        public int variantCount;
        public int errorReportedAt;
        public int lastVariantAt;
        public PsiBuilder.Marker leftMarker;

        public Frame() {
        }

        public Frame init(PsiBuilder builder,
                          ErrorState state,
                          int level_,
                          int modifiers_,
                          IElementType elementType_,
                          String name_) {
            parentFrame = state.currentFrame;
            elementType = elementType_;

            offset = builder.getCurrentOffset();
            position = builder.rawTokenIndex();
            level = level_;
            modifiers = modifiers_;
            name = name_;
            variantCount = state.variants.size();
            errorReportedAt = -1;
            lastVariantAt = -1;

            leftMarker = null;
            return this;
        }

        @Override
        public @NonNls String toString() {
            String mod = _NONE_ == modifiers ? "_NONE_, " :
                    (0 != (modifiers & _COLLAPSE_) ? "_CAN_COLLAPSE_, " : "") +
                            (0 != (modifiers & _LEFT_) ? "_LEFT_, " : "") +
                            (0 != (modifiers & _LEFT_INNER_) ? "_LEFT_INNER_, " : "") +
                            (0 != (modifiers & _AND_) ? "_AND_, " : "") +
                            (0 != (modifiers & _NOT_) ? "_NOT_, " : "") +
                            (0 != (modifiers & _UPPER_) ? "_UPPER_, " : "");
            return String.format("{%s:%s:%d, %d, %s%s, %s}", offset, position, level, errorReportedAt, mod, elementType, name);
        }
    }

    private record Hooks<T>(Hook<T> hook, T param, int level, Hooks next) {
        static <E> Hooks<E> concat(Hook<E> hook, E param, int level, Hooks<?> hooks) {
            return new Hooks<>(hook, param, level, hooks);
        }
    }

    public static class ErrorState {

        final MyList<Variant> variants = new MyList<>(INITIAL_VARIANTS_SIZE);
        final MyList<Variant> unexpected = new MyList<>(INITIAL_VARIANTS_SIZE / 10);
        final LimitedPool<Variant> VARIANTS = new LimitedPool<>(VARIANTS_POOL_SIZE, Variant::new);
        final LimitedPool<Frame> FRAMES = new LimitedPool<>(FRAMES_POOL_SIZE, Frame::new);
        public Frame currentFrame;
        public CompletionState completionState;
        public PairProcessor<IElementType, IElementType> altExtendsChecker;
        public BracePair[] braces;
        public Parser tokenAdvancer = TOKEN_ADVANCER;
        public boolean altMode;
        int predicateCount;
        int level;
        boolean predicateSign = true;
        boolean suppressErrors;
        Hooks<?> hooks;
        TokenSet[] extendsSets;
        private boolean caseSensitive;


        public static void initState(ErrorState state, PsiBuilder builder, IElementType root, TokenSet[] extendsSets) {
            state.extendsSets = extendsSets;
            PsiFile file = builder.getUserData(FileContextUtil.CONTAINING_FILE_KEY);
            state.completionState = null == file ? null : file.getUserData(COMPLETION_STATE_KEY);
            Language language = null == file ? root.getLanguage() : file.getLanguage();
            state.caseSensitive = language.isCaseSensitive();
            PairedBraceMatcher matcher = LanguageBraceMatching.INSTANCE.forLanguage(language);
            state.braces = null == matcher ? null : matcher.getPairs();
            if (null != state.braces && 0 == state.braces.length) state.braces = null;
        }

        public @NotNull String getExpected(int position, boolean expected) {
            StringBuilder sb = new StringBuilder();
            MyList<Variant> list = expected ? variants : unexpected;
            String[] strings = new String[list.size()];
            long[] hashes = new long[strings.length];
            Arrays.fill(strings, "");
            int count = 0;
            loop:
            for (Variant variant : list) {
                if (position == variant.position) {
                    String text = String.valueOf(variant.object);
                    long hash = StringHash.calc(text);
                    for (int i = 0; i < count; i++) {
                        if (hashes[i] == hash) continue loop;
                    }
                    hashes[count] = hash;
                    strings[count] = text;
                    count++;
                }
            }
            Arrays.sort(strings);
            count = 0;
            for (String s : strings) {
                if (0 == s.length()) continue;
                if (0 < count) {
                    if (MAX_VARIANTS_TO_DISPLAY < count) {
                        sb.append(" ").append(AnalysisBundle.message("parsing.error.and.ellipsis"));
                        break;
                    } else {
                        sb.append(", ");
                    }
                }
                count++;
                char c = s.charAt(0);
                String displayText = '<' == c || isJavaIdentifierStart(c) ? s : '\'' + s + '\'';
                sb.append(displayText);
            }
            if (1 < count && MAX_VARIANTS_TO_DISPLAY > count) {
                int idx = sb.lastIndexOf(", ");
                sb.replace(idx, idx + 1, " " + AnalysisBundle.message("parsing.error.or"));
            }
            return sb.toString();
        }

        public void clearVariants(Frame frame) {
            clearVariants(true, null == frame ? 0 : frame.variantCount);
            if (null != frame) frame.lastVariantAt = -1;
        }

        void clearVariants(boolean expected, int start) {
            MyList<Variant> list = expected ? variants : unexpected;
            if (0 > start || start >= list.size()) return;
            for (int i = start, len = list.size(); i < len; i++) {
                VARIANTS.recycle(list.get(i));
            }
            list.setSize(start);
        }

        public boolean typeExtends(IElementType child, IElementType parent) {
            if (child == parent) return true;
            if (null != extendsSets) {
                for (TokenSet set : extendsSets) {
                    if (set.contains(child) && set.contains(parent)) return true;
                }
            }
            return null != altExtendsChecker && altExtendsChecker.process(child, parent);
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
            if (null == marker) return;
            marker.done(elementType);
        }

        /**
         * 报告解析错误
         *
         * @param message
         */
        public void error(String message) {
            if (null == marker) return;
            if (offset == myBuilder.getCurrentOffset()) {
                marker.drop(); // 没有空错误
            } else {
                marker.error(message);
            }
        }

        /**
         * 用于删除当前位置的标记
         */
        public void drop() {
            if (null == marker) return;
            marker.drop();
        }
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

}
