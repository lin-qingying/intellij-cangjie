package com.huawei.cangjie1.parsing;

public abstract class AbstractTokenStreamPredicate implements TokenStreamPredicate {

    @Override
    public TokenStreamPredicate or(TokenStreamPredicate other) {
        return new AbstractTokenStreamPredicate() {
            @Override
            public boolean matching(boolean topLevel) {
                if (AbstractTokenStreamPredicate.this.matching(topLevel)) return true;
                return other.matching(topLevel);
            }
        };
    }
}
