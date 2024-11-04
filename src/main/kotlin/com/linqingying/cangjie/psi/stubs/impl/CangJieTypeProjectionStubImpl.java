package com.linqingying.cangjie.psi.stubs.impl;

import com.linqingying.cangjie.psi.CjProjectionKind;
import com.linqingying.cangjie.psi.CjTypeProjection;
import com.linqingying.cangjie.psi.stubs.CangJieTypeProjectionStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;


public class CangJieTypeProjectionStubImpl extends CangJieStubBaseImpl<CjTypeProjection> implements CangJieTypeProjectionStub {

    private final int projectionKindOrdinal;

    public CangJieTypeProjectionStubImpl(StubElement parent, int projectionKindOrdinal) {
        super(parent, CjStubElementTypes.TYPE_PROJECTION);
        this.projectionKindOrdinal = projectionKindOrdinal;
    }

    @NotNull
    @Override
    public CjProjectionKind getProjectionKind() {
        return CjProjectionKind.getEntries().get(projectionKindOrdinal);
    }
}
