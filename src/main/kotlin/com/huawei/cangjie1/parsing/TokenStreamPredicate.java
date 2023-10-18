package com.huawei.cangjie1.parsing;


public interface TokenStreamPredicate {
    boolean matching(boolean topLevel);

    TokenStreamPredicate or(TokenStreamPredicate other);
}
