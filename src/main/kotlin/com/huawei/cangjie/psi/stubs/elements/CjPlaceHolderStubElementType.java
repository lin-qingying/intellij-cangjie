package com.huawei.cangjie.psi.stubs.elements;


import com.huawei.cangjie.psi.CjElementImplStub;
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie.psi.stubs.impl.CangJiePlaceHolderStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CjPlaceHolderStubElementType<T extends CjElementImplStub<? extends StubElement<?>>> extends
        CjStubElementType<CangJiePlaceHolderStub<T>, T> {
    public CjPlaceHolderStubElementType(@NotNull @NonNls String debugName, @NotNull Class<T> psiClass) {
        super(debugName, psiClass, CangJiePlaceHolderStub.class);
    }


    @Override
    public @NotNull CangJiePlaceHolderStub<T> createStub(@NotNull T psi, StubElement<?> parentStub) {
        return new CangJiePlaceHolderStubImpl<>(parentStub, this);
    }



    @Override
    public void serialize(@NotNull CangJiePlaceHolderStub<T> stub, @NotNull StubOutputStream dataStream) throws IOException {
        //do nothing
    }

    @NotNull
    @Override
    public CangJiePlaceHolderStub<T> deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new CangJiePlaceHolderStubImpl<>(parentStub, this);
    }


}
