package com.linqingying.cangjie.psi.stubs.impl;

import com.linqingying.cangjie.psi.CjNameReferenceExpression;
import com.linqingying.cangjie.psi.stubs.CangJieNameReferenceExpressionStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class CangJieNameReferenceExpressionStubImpl extends CangJieStubBaseImpl<CjNameReferenceExpression> implements
        CangJieNameReferenceExpressionStub {
    @NotNull
    private final StringRef referencedName;
    private final boolean myClassRef;

    public CangJieNameReferenceExpressionStubImpl(StubElement parent, @NotNull StringRef referencedName) {
        super(parent, CjStubElementTypes.REFERENCE_EXPRESSION);
        this.referencedName = referencedName;
        myClassRef = false;
    }

    public CangJieNameReferenceExpressionStubImpl(
            @Nullable StubElement<?> parent,
            @NotNull StringRef referencedName,
            boolean myClassRef
    ) {
        super(parent, CjStubElementTypes.REFERENCE_EXPRESSION);
        this.referencedName = referencedName;
        this.myClassRef = myClassRef;
    }

    public boolean isClassRef() {
        return myClassRef;
    }

    @NotNull
    @Override
    public String getReferencedName() {
        return referencedName.getString();
    }
}
