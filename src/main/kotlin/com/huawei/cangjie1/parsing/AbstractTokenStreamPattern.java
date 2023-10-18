package com.huawei.cangjie1.parsing;


import com.intellij.psi.tree.IElementType;

public abstract class AbstractTokenStreamPattern implements TokenStreamPattern {

    protected int lastOccurrence = -1;

    protected void fail() {
        lastOccurrence = -1;
    }

    @Override
    public int result() {
        return lastOccurrence;
    }

    @Override
    public boolean isTopLevel(int openAngleBrackets, int openBrackets, int openBraces, int openParentheses) {
        return openBraces == 0 && openBrackets == 0 && openParentheses == 0 && openAngleBrackets == 0;
    }

    @Override
    public boolean handleUnmatchedClosing(IElementType token) {
        return false;
    }

    public void reset() {
        lastOccurrence = -1;
    }
}

