package com.huawei.cangjie1.psi.stubs.elements;

import com.huawei.cangjie1.name.ClassId;
import com.huawei.cangjie1.psi.CjBasicType;
import com.huawei.cangjie1.psi.CjProjectionKind;
import com.huawei.cangjie1.psi.CjUserType;
import com.huawei.cangjie1.psi.psiUtil.StubUtils;
import com.huawei.cangjie1.psi.stubs.CangJieBasicTypeStub;
import com.huawei.cangjie1.psi.stubs.CangJieUserTypeStub;
import com.huawei.cangjie1.psi.stubs.impl.*;
import com.intellij.psi.PsiElement;
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

public class CjBasicTypeElementType extends CjStubElementType<CangJieBasicTypeStub, CjBasicType> {

    public CjBasicTypeElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjBasicType.class, CangJieBasicTypeStub.class);
    }


    @NotNull
    @Override
    public  CangJieBasicTypeStub createStub(@NotNull CjBasicType psi, StubElement<? extends PsiElement> parentStub) {
        return new CangJieBasicTypeStubImpl((StubElement<?>) parentStub);
    }

    @Override
    public void serialize(@NotNull CangJieBasicTypeStub stub, @NotNull StubOutputStream dataStream) throws IOException {
dataStream.writeName(getDebugName());

    }

    @Override
    public @NotNull CangJieBasicTypeStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new CangJieBasicTypeStubImpl(parentStub);
    }
}
