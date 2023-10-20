package com.huawei.cangjie.parsing;


public interface TokenStreamPredicate {
    boolean matching(boolean topLevel);

    TokenStreamPredicate or(TokenStreamPredicate other);
}
