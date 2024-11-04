package com.linqingying.cangjie.parsing;



import com.intellij.psi.tree.IElementType;

public interface TokenStreamPattern {

    boolean processToken(int offset, boolean topLevel);


    int result();


    boolean isTopLevel(int openAngleBrackets, int openBrackets, int openBraces, int openParentheses);


    boolean handleUnmatchedClosing(IElementType token);
}
