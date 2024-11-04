package com.linqingying.cangjie.parsing;


public interface TokenStreamPredicate {
    boolean matching(boolean topLevel);

    TokenStreamPredicate or(TokenStreamPredicate other);
}
