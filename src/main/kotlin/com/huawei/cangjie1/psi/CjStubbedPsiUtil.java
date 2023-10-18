package com.huawei.cangjie1.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class CjStubbedPsiUtil {
    @Nullable
    public static CjDeclaration getContainingDeclaration(@NotNull PsiElement element) {
        return getPsiOrStubParent(element, CjDeclaration.class, true);
    }

    @Nullable
    public static <T extends CjDeclaration> T getContainingDeclaration(@NotNull PsiElement element, @NotNull Class<T> declarationClass) {
        return getPsiOrStubParent(element, declarationClass, true);
    }

    //TODO: contribute to idea PsiTreeUtil#getPsiOrStubParent
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends CjElement> T getPsiOrStubParent(
            @NotNull PsiElement element,
            @NotNull Class<T> declarationClass,
            boolean strict
    ) {
        if (!strict && declarationClass.isInstance(element)) {
            return (T) element;
        }
        if (element instanceof CjElementImplStub) {
            StubElement<?> stub = ((CjElementImplStub) element).getStub();
            if (stub != null) {
                return stub.getParentStubOfType(declarationClass);
            }
        }
        return PsiTreeUtil.getParentOfType(element, declarationClass, strict);
    }

    @Nullable
    public static <T extends CjElement> T getStubOrPsiChild(
            @NotNull CjElementImplStub<?> element,
            @NotNull TokenSet types,
            @NotNull ArrayFactory<T> factory
    ) {
        T[] typeElements = element.getStubOrPsiChildren(types, factory);
        if (typeElements.length == 0) {
            return null;
        }
        return typeElements[0];
    }

    private CjStubbedPsiUtil() {
    }
}
