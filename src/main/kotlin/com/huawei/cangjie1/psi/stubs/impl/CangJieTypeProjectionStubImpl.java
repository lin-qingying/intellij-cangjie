package com.huawei.cangjie1.psi.stubs.impl;

import com.huawei.cangjie1.psi.CjProjectionKind;
import com.huawei.cangjie1.psi.CjTypeProjection;
import com.huawei.cangjie1.psi.stubs.CangJieTypeProjectionStub;
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes;
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
        return CjProjectionKind.values()[projectionKindOrdinal];
    }
}
