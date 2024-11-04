package com.linqingying.cangjie.psi.stubs.elements;

import com.linqingying.cangjie.psi.CjPropertyAccessor;
import com.linqingying.cangjie.psi.stubs.CangJiePropertyAccessorStub;
import com.linqingying.cangjie.psi.stubs.impl.CangJiePropertyAccessorStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class CjPropertyAccessorElementType extends CjStubElementType<CangJiePropertyAccessorStub, CjPropertyAccessor> {
    public CjPropertyAccessorElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjPropertyAccessor.class, CangJiePropertyAccessorStub.class);
    }

    @Override
    public CangJiePropertyAccessorStub createStub(@NotNull CjPropertyAccessor psi, StubElement parentStub) {
        return new CangJiePropertyAccessorStubImpl(parentStub, psi.isGetter(), psi.hasBody(), psi.hasBlockBody());
    }

    @Override
    public void serialize(@NotNull CangJiePropertyAccessorStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeBoolean(stub.isGetter());
        dataStream.writeBoolean(stub.hasBody());
        dataStream.writeBoolean(stub.hasBlockBody());
    }

    @NotNull
    @Override
    public CangJiePropertyAccessorStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        boolean isGetter = dataStream.readBoolean();
        boolean hasBody = dataStream.readBoolean();
        boolean hasBlockBody = dataStream.readBoolean();
        return new CangJiePropertyAccessorStubImpl(parentStub, isGetter, hasBody, hasBlockBody);
    }
}
