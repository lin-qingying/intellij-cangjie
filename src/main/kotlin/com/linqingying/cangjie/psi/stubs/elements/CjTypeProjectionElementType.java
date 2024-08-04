package com.linqingying.cangjie.psi.stubs.elements;

import com.linqingying.cangjie.psi.CjTypeProjection;
import com.linqingying.cangjie.psi.stubs.CangJieTypeProjectionStub;
import com.linqingying.cangjie.psi.stubs.impl.CangJieTypeProjectionStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;



public class CjTypeProjectionElementType extends CjStubElementType<CangJieTypeProjectionStub, CjTypeProjection> {
    public CjTypeProjectionElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjTypeProjection.class, CangJieTypeProjectionStub.class);
    }

    @Override
    public CangJieTypeProjectionStub createStub(@NotNull CjTypeProjection psi, StubElement parentStub) {
        return new CangJieTypeProjectionStubImpl(parentStub, psi.getProjectionKind().ordinal());
    }

    @Override
    public void serialize(@NotNull CangJieTypeProjectionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeVarInt(stub.getProjectionKind().ordinal());
    }

    @NotNull
    @Override
    public CangJieTypeProjectionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        int projectionKindOrdinal = dataStream.readVarInt();
        return new CangJieTypeProjectionStubImpl(parentStub, projectionKindOrdinal);
    }
}
