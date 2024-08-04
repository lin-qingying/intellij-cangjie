package com.linqingying.cangjie.psi.stubs.impl;

import com.linqingying.cangjie.psi.CjElementImplStub;
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;


public class CangJiePlaceHolderStubImpl<T extends CjElementImplStub<? extends StubElement<?>>> extends CangJieStubBaseImpl<T>
        implements CangJiePlaceHolderStub<T> {
    public CangJiePlaceHolderStubImpl(StubElement parent, IStubElementType elementType) {
        //noinspection unchecked
        super(parent, elementType);
    }
}
