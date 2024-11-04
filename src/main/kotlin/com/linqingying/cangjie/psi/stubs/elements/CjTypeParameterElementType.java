package com.linqingying.cangjie.psi.stubs.elements;

import com.linqingying.cangjie.psi.CjTypeParameter;
import com.linqingying.cangjie.psi.stubs.CangJieTypeParameterStub;
import com.linqingying.cangjie.psi.stubs.impl.CangJieTypeParameterStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class CjTypeParameterElementType extends CjStubElementType<CangJieTypeParameterStub, CjTypeParameter> {
    public CjTypeParameterElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjTypeParameter.class, CangJieTypeParameterStub.class);
    }

    @NotNull
    @Override
    public CangJieTypeParameterStub createStub(@NotNull CjTypeParameter psi, StubElement parentStub) {
        return new CangJieTypeParameterStubImpl(
                (StubElement<?>) parentStub, StringRef.fromString(psi.getName())
//                psi.getVariance() == Variance.IN_VARIANCE
        );
    }

    @Override
    public void serialize(@NotNull CangJieTypeParameterStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
//        dataStream.writeBoolean(stub.isInVariance());

    }

    @NotNull
    @Override
    public CangJieTypeParameterStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
//        bool isInVariance = dataStream.readBoolean();


        return new CangJieTypeParameterStubImpl((StubElement<?>) parentStub, name);
    }
}
