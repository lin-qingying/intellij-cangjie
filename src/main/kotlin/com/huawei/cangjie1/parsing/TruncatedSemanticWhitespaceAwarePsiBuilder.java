package com.huawei.cangjie1.parsing;




import com.intellij.psi.tree.IElementType;

public class TruncatedSemanticWhitespaceAwarePsiBuilder extends SemanticWhitespaceAwarePsiBuilderAdapter {

    private final int myEOFPosition;

    public TruncatedSemanticWhitespaceAwarePsiBuilder(SemanticWhitespaceAwarePsiBuilder builder, int eofPosition) {
        super(builder);
        this.myEOFPosition = eofPosition;
    }

    @Override
    public boolean eof() {
        return super.eof() || isOffsetBeyondEof(getCurrentOffset());
    }

    @Override
    public String getTokenText() {
        if (eof()) return null;
        return super.getTokenText();
    }

    @Override
    public IElementType getTokenType() {
        if (eof()) return null;
        return super.getTokenType();
    }

    @Override
    public IElementType lookAhead(int steps) {
        if (eof()) return null;

        int rawLookAheadSteps = rawLookAhead(steps);
        if (isOffsetBeyondEof(rawTokenTypeStart(rawLookAheadSteps))) return null;

        return super.rawLookup(rawLookAheadSteps);
    }

    private int rawLookAhead(int steps) {
        // This code reproduces the behavior of PsiBuilderImpl.lookAhead(), but returns a number of raw steps instead of a token type
        // This is required for implementing truncated builder behavior
        int cur = 0;
        while (steps > 0) {
            cur++;

            IElementType rawTokenType = rawLookup(cur);
            while (rawTokenType != null && isWhitespaceOrComment(rawTokenType)) {
                cur++;
                rawTokenType = rawLookup(cur);
            }

            steps--;
        }
        return cur;
    }

    private boolean isOffsetBeyondEof(int offsetFromCurrent) {
        return myEOFPosition >= 0 && offsetFromCurrent >= myEOFPosition;
    }
}
