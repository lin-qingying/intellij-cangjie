package com.huawei.cangjie1.psi.stubs.elements;

import com.huawei.cangjie1.lang.CangJieLanguage;
import com.huawei.cangjie1.psi.CjElementImplStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.*;
import com.intellij.util.ArrayFactory;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

public abstract    class CjStubElementType<StubT extends StubElement<?>, PsiT extends CjElementImplStub<?>> extends IStubElementType<StubT, PsiT> {

    @NotNull
    private final Constructor<PsiT> byNodeConstructor;
    @NotNull
    private final Constructor<PsiT> byStubConstructor;
    @NotNull
    private final PsiT[] emptyArray;
    @NotNull
    private final ArrayFactory<PsiT> arrayFactory;
    public CjStubElementType(@NotNull @NonNls String debugName, @NotNull Class<PsiT> psiClass, @NotNull Class<?> stubClass) {
        super(debugName, CangJieLanguage.INSTANCE);
        try {
            byNodeConstructor = psiClass.getConstructor(ASTNode.class);
            byStubConstructor = psiClass.getConstructor(stubClass);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException("Stub element type declaration for " + psiClass.getSimpleName() + " is missing required constructors",e);
        }
        emptyArray = (PsiT[]) Array.newInstance(psiClass, 0);
        arrayFactory = count -> {
            if (count == 0) {
                return emptyArray;
            }
            return (PsiT[]) Array.newInstance(psiClass, count);
        };
    }



    @NotNull
    public PsiT createPsiFromAst(@NotNull ASTNode node) {
        return ReflectionUtil.createInstance(byNodeConstructor, node);
    }

    @Override
    @NotNull
    public PsiT createPsi(@NotNull StubT stub) {
        return ReflectionUtil.createInstance(byStubConstructor, stub);
    }




    @NotNull
    @Override
    public String getExternalId() {
        return "cangjie." + getDebugName();
    }


    @NotNull
    public ArrayFactory<PsiT> getArrayFactory() {
        return arrayFactory;
    }
    @Override
    public void indexStub(@NotNull StubT stub, @NotNull IndexSink sink) {

    }
}
