package com.linqingying.cangjie.psi.stubs.elements;

import com.linqingying.cangjie.psi.CjNameReferenceExpression;
import com.linqingying.cangjie.psi.stubs.CangJieNameReferenceExpressionStub;
import com.linqingying.cangjie.psi.stubs.impl.CangJieNameReferenceExpressionStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;



public class CjNameReferenceExpressionElementType extends CjStubElementType<CangJieNameReferenceExpressionStub, CjNameReferenceExpression> {
    public CjNameReferenceExpressionElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjNameReferenceExpression.class, CangJieNameReferenceExpressionStub.class);
    }

    @Override
    public CangJieNameReferenceExpressionStub createStub(@NotNull CjNameReferenceExpression psi, StubElement parentStub) {
        return new CangJieNameReferenceExpressionStubImpl(parentStub, StringRef.fromString(psi.getReferencedName()));
    }

    @Override
    public void serialize(@NotNull CangJieNameReferenceExpressionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getReferencedName());
        dataStream.writeBoolean(
                stub instanceof CangJieNameReferenceExpressionStubImpl && ((CangJieNameReferenceExpressionStubImpl) stub).isClassRef());
    }

    @NotNull
    @Override
    public CangJieNameReferenceExpressionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef referencedName = dataStream.readName();
        boolean isClassRef = dataStream.readBoolean();
        return new CangJieNameReferenceExpressionStubImpl(parentStub, referencedName, isClassRef);
    }
}
