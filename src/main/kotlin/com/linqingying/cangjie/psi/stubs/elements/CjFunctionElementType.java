package com.linqingying.cangjie.psi.stubs.elements;

import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.psi.CjFile;
import com.linqingying.cangjie.psi.CjNamedFunction;
import com.linqingying.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.linqingying.cangjie.psi.stubs.CangJieFunctionStub;
import com.linqingying.cangjie.psi.stubs.impl.CangJieFunctionStubImpl;
import com.linqingying.cangjie.psi.stubs.impl.CangJieStubOrigin;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class CjFunctionElementType extends CjStubElementType<CangJieFunctionStub, CjNamedFunction> {

    private static final String NAME = "cangjie.FUNCTION";

    public CjFunctionElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjNamedFunction.class, CangJieFunctionStub.class);
    }

    @NotNull
    @Override
    public CangJieFunctionStub createStub(@NotNull CjNamedFunction psi, @NotNull StubElement parentStub) {
        boolean isTopLevel = psi.getParent() instanceof CjFile;
        boolean isExtension = psi.getReceiverTypeReference() != null;
        FqName fqName = CjPsiUtilKt.safeFqNameForLazyResolve(psi);
        boolean hasBlockBody = psi.hasBlockBody();
        boolean hasBody = psi.hasBody();
        return new CangJieFunctionStubImpl(
                (StubElement<?>) parentStub, CjStubElementTypes.FUNCTION, StringRef.fromString(psi.getName()), isTopLevel, fqName,
                isExtension, hasBlockBody, hasBody, psi.hasTypeParameterListBeforeFunctionName(),
//                psi.mayHaveContract(),

                null
        );
    }

    @Override
    public void serialize(@NotNull CangJieFunctionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isTopLevel());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.asString() : null);

        dataStream.writeBoolean(stub.isExtension());
        dataStream.writeBoolean(stub.hasBlockBody());
        dataStream.writeBoolean(stub.hasBody());
        dataStream.writeBoolean(stub.hasTypeParameterListBeforeFunctionName());
//        bool haveContract = stub.mayHaveContract();
//        dataStream.writeBoolean(haveContract);

        if (stub instanceof CangJieFunctionStubImpl stubImpl) {
//            if (haveContract) {
//                stubImpl.serializeContract(dataStream);
//            }

            CangJieStubOrigin.serialize(stubImpl.getOrigin(), dataStream);
        }
    }

    @NotNull
    @Override
    public CangJieFunctionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isTopLevel = dataStream.readBoolean();

        StringRef fqNameAsString = dataStream.readName();
        FqName fqName = fqNameAsString != null ? new FqName(fqNameAsString.toString()) : null;

        boolean isExtension = dataStream.readBoolean();
        boolean hasBlockBody = dataStream.readBoolean();
        boolean hasBody = dataStream.readBoolean();
        boolean hasTypeParameterListBeforeFunctionName = dataStream.readBoolean();
//        bool mayHaveContract = dataStream.readBoolean();
        return new CangJieFunctionStubImpl(
                (StubElement<?>) parentStub, CjStubElementTypes.FUNCTION, name, isTopLevel, fqName, isExtension, hasBlockBody, hasBody,
                hasTypeParameterListBeforeFunctionName,
//                mayHaveContract,
//                mayHaveContract ? CangJieFunctionStubImpl.Companion.deserializeContract(dataStream) :



                CangJieStubOrigin.deserialize(dataStream)
        );
    }

    @Override
    public void indexStub(@NotNull CangJieFunctionStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexFunction(stub, sink);
    }

    @NotNull
    @Override
    public String getExternalId() {
        return NAME;
    }
}
