package com.linqingying.cangjie.psi.stubs.elements;

import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.psi.CjFile;
import com.linqingying.cangjie.psi.CjMainFunction;
import com.linqingying.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.linqingying.cangjie.psi.stubs.CangJieFunctionStub;

import com.linqingying.cangjie.psi.stubs.impl.CangJieFunctionStubImpl;
import com.linqingying.cangjie.psi.stubs.impl.CangJieStubOrigin;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.linqingying.cangjie.builtins.StandardNames.MAIN;


public class CjMainFunctionElementType extends CjStubElementType<CangJieFunctionStub, CjMainFunction> {
    private static final String NAME = "cangjie.MAIN_FUNCTION";

    public CjMainFunctionElementType(@NotNull String debugName, @NotNull Class<CjMainFunction> psiClass, @NotNull Class<?> stubClass) {
        super(debugName, psiClass, stubClass);
    }


    public CjMainFunctionElementType(@NotNull String debugName) {
        super(debugName, CjMainFunction.class, CangJieFunctionStub.class);
    }


    @Override
    public @NotNull CangJieFunctionStubImpl createStub(@NotNull CjMainFunction psi, StubElement<? extends PsiElement> parentStub) {
        boolean isTopLevel = psi.getParent() instanceof CjFile;
        boolean isExtension = psi.getReceiverTypeReference() != null;
        FqName fqName = CjPsiUtilKt.safeFqNameForLazyResolve(psi);
        if (fqName != null) {
            fqName = new FqName(fqName.getModuleName().asString()).child(MAIN);
        }
        boolean hasBlockBody = psi.hasBlockBody();
        boolean hasBody = psi.hasBody();
        return new CangJieFunctionStubImpl(
                parentStub,CjStubElementTypes.MAIN_FUNC, StringRef.fromString(psi.getName()), isTopLevel, fqName,
                isExtension, hasBlockBody, hasBody, psi.hasTypeParameterListBeforeFunctionName(),

null,
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


            CangJieStubOrigin.serialize(stubImpl.getOrigin(), dataStream);
        }
    }
    @Override
    public void indexStub(@NotNull CangJieFunctionStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexMainFunction(stub, sink);
    }
    @Override
    public @NotNull CangJieFunctionStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
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
                (StubElement<?>) parentStub,CjStubElementTypes.MAIN_FUNC, name, isTopLevel, fqName, isExtension, hasBlockBody, hasBody,
                hasTypeParameterListBeforeFunctionName,
null,
                CangJieStubOrigin.deserialize(dataStream)
        );
    }
}
