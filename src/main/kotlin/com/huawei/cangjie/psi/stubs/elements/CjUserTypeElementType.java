package com.huawei.cangjie.psi.stubs.elements;

import com.huawei.cangjie.name.ClassId;
import com.huawei.cangjie.psi.CjProjectionKind;
import com.huawei.cangjie.psi.CjUserType;
import com.huawei.cangjie.psi.psiUtil.StubUtils;
import com.huawei.cangjie.psi.stubs.CangJieUserTypeStub;
import com.huawei.cangjie.psi.stubs.impl.*;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;



public class CjUserTypeElementType extends CjStubElementType<CangJieUserTypeStub, CjUserType> {
    public CjUserTypeElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjUserType.class, CangJieUserTypeStub.class);
    }

    @NotNull
    @Override
    public CangJieUserTypeStub createStub(@NotNull CjUserType psi, StubElement parentStub) {
        return new CangJieUserTypeStubImpl((StubElement<?>) parentStub );
    }

    @Override
    public void serialize(@NotNull CangJieUserTypeStub stub, @NotNull StubOutputStream dataStream) throws IOException {

    }



    @NotNull
    @Override
    public CangJieUserTypeStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new CangJieUserTypeStubImpl((StubElement<?>) parentStub );
    }

}
