package com.huawei.cangjie.psi.stubs.elements;

import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.psi.CjParameter;
import com.huawei.cangjie.psi.stubs.CangJieParameterStub;
import com.huawei.cangjie.psi.stubs.impl.CangJieParameterStubImpl;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class CjParameterElementType extends CjStubElementType<CangJieParameterStub, CjParameter> {
    public CjParameterElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjParameter.class, CangJieParameterStub.class);
    }

    @NotNull
    @Override
    public CangJieParameterStub createStub(@NotNull CjParameter psi, StubElement parentStub) {
        FqName fqName = psi.getFqName();
        StringRef fqNameRef = StringRef.fromString(fqName != null ? fqName.asString() : null);
        return new CangJieParameterStubImpl(
                (StubElement<?>) parentStub, fqNameRef, StringRef.fromString(psi.getName()),
                psi.isMutable(), psi.hasLetOrVar(), psi.hasDefaultValue(), null
        );
    }

    @Override
    public void serialize(@NotNull CangJieParameterStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isMutable());
        dataStream.writeBoolean(stub.hasValOrVar());
        dataStream.writeBoolean(stub.hasDefaultValue());
        FqName name = stub.getFqName();
        dataStream.writeName(name != null ? name.asString() : null);
        dataStream.writeName(stub instanceof CangJieParameterStubImpl ? ((CangJieParameterStubImpl) stub).getFunctionTypeParameterName() : null);
    }

    @NotNull
    @Override
    public CangJieParameterStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isMutable = dataStream.readBoolean();
        boolean hasValOrValNode = dataStream.readBoolean();
        boolean hasDefaultValue = dataStream.readBoolean();
        StringRef fqName = dataStream.readName();

        return new CangJieParameterStubImpl((StubElement<?>) parentStub, fqName, name, isMutable, hasValOrValNode, hasDefaultValue,
                dataStream.readNameString());
    }

    @Override
    public void indexStub(@NotNull CangJieParameterStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexParameter(stub, sink);
    }
}
