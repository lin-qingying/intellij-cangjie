package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.lexer.CjTokens;
import com.huawei.cangjie1.name.Name;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;


abstract class CjNamedDeclarationNotStubbed extends CjDeclarationImpl implements CjNamedDeclaration {
    public CjNamedDeclarationNotStubbed(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        PsiElement identifier = getNameIdentifier();
        if (identifier != null) {
            String text = identifier.getText();
            return text != null ? CjPsiUtil.unquoteIdentifier(text) : null;
        }
        else {
            return null;
        }
    }

    @Override
    public Name getNameAsName() {
        String name = getName();
        return name != null ? Name.identifier(name) : null;
    }

    @Override
    public  Name getNameAsSafeName() {
        return CjPsiUtil.safeName(getName());
    }

    @Override
    public PsiElement getNameIdentifier() {
        return findChildByType(CjTokens.IDENTIFIER);
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        PsiElement identifier = getNameIdentifier();
        if (identifier == null) throw new IncorrectOperationException();

        return identifier.replace(new CjPsiFactory(getProject()).createNameIdentifier(name));
    }

    @Override
    public int getTextOffset() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getTextRange().getStartOffset() : getTextRange().getStartOffset();
    }
}
