package com.huawei.cangjie.psi.stubs.elements;

import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.psi.CjVariable;
import com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.huawei.cangjie.psi.stubs.CangJieVariableStub;
import com.huawei.cangjie.psi.stubs.impl.CangJieVariableStubImpl;
import com.huawei.cangjie.psi.stubs.impl.CangJieStubOrigin;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;



public class CjVariableElementType extends CjStubElementType<CangJieVariableStub, CjVariable> {
    public CjVariableElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjVariable.class, CangJieVariableStub.class);
    }

    @NotNull
    @Override
    public CangJieVariableStub createStub(@NotNull CjVariable psi, StubElement parentStub) {
//        assert !psi.isLocal() :
//                String.format("Should not store local property: %s, parent %s",
//                        psi.getText(), psi.getParent() != null ? psi.getParent().getText() : "<no parent>");

        return new CangJieVariableStubImpl(
                (StubElement<?>) parentStub, StringRef.fromString(psi.getName()),
                psi.isVar(), psi.isTopLevel(),
              psi.hasInitializer(),
                psi.getReceiverTypeReference() != null, psi.getTypeReference() != null,
                CjPsiUtilKt.safeFqNameForLazyResolve(psi),

              null
        );
    }

    @Override
    public void serialize(@NotNull CangJieVariableStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isVar());
        dataStream.writeBoolean(stub.isTopLevel());

        dataStream.writeBoolean(stub.hasInitializer());
        dataStream.writeBoolean(stub.isExtension());
        dataStream.writeBoolean(stub.hasReturnTypeRef());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.asString() : null);

        if (stub instanceof CangJieVariableStubImpl) {
            CangJieVariableStubImpl stubImpl = (CangJieVariableStubImpl) stub;



            CangJieStubOrigin.serialize(stubImpl.getOrigin(), dataStream);
        }
    }

    @NotNull
    @Override
    public CangJieVariableStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isVar = dataStream.readBoolean();
        boolean isTopLevel = dataStream.readBoolean();
        boolean hasInitializer = dataStream.readBoolean();
        boolean hasReceiverTypeRef = dataStream.readBoolean();
        boolean hasReturnTypeRef = dataStream.readBoolean();

        StringRef fqNameAsString = dataStream.readName();
        FqName fqName = fqNameAsString != null ? new FqName(fqNameAsString.toString()) : null;

        return new CangJieVariableStubImpl(
                (StubElement<?>) parentStub, name, isVar, isTopLevel,   hasInitializer,
                hasReceiverTypeRef, hasReturnTypeRef, fqName,
                CangJieStubOrigin.deserialize(dataStream)
        );
    }

    @Override
    public void indexStub(@NotNull CangJieVariableStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexVariable(stub, sink);
    }
}
