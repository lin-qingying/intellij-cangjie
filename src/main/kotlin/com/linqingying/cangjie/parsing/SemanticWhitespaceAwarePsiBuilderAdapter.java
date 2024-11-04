package com.linqingying.cangjie.parsing;



import com.intellij.lang.impl.PsiBuilderAdapter;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class SemanticWhitespaceAwarePsiBuilderAdapter extends PsiBuilderAdapter implements SemanticWhitespaceAwarePsiBuilder {

    private final SemanticWhitespaceAwarePsiBuilder myBuilder;

    public SemanticWhitespaceAwarePsiBuilderAdapter(SemanticWhitespaceAwarePsiBuilder builder) {
        super(builder);
        this.myBuilder = builder;
    }

    @Override
    public boolean newlineBeforeCurrentToken() {
        return myBuilder.newlineBeforeCurrentToken();
    }

    @Override
    public void disableNewlines() {
        myBuilder.disableNewlines();
    }

    @Override
    public void enableNewlines() {
        myBuilder.enableNewlines();
    }

    @Override
    public void restoreNewlinesState() {
        myBuilder.restoreNewlinesState();
    }

    @Override
    public void restoreJoiningComplexTokensState() {
        myBuilder.restoreJoiningComplexTokensState();
    }

    @Override
    public void enableJoiningComplexTokens() {
        myBuilder.enableJoiningComplexTokens();
    }

    @Override
    public void disableJoiningComplexTokens() {
        myBuilder.disableJoiningComplexTokens();
    }

    @Override
    public boolean isWhitespaceOrComment(@NotNull IElementType elementType) {
        return myBuilder.isWhitespaceOrComment(elementType);
    }
}
