package com.huawei.cangjie1.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public interface CjDeclarationWithBody extends CjDeclaration {
    @Nullable
    CjExpression getBodyExpression();

    @Nullable
    PsiElement getEqualsToken();

    @Override
    @Nullable
    String getName();



    boolean hasBlockBody();

    boolean hasBody();

    boolean hasDeclaredReturnType();

    @NotNull
    List<CjParameter> getValueParameters();

    @Nullable
    default CjBlockExpression getBodyBlockExpression() {
        CjExpression bodyExpression = getBodyExpression();
        if (bodyExpression instanceof CjBlockExpression) {
            return (CjBlockExpression) bodyExpression;
        }

        return null;
    }
}

