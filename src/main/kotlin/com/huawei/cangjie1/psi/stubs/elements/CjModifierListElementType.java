package com.huawei.cangjie1.psi.stubs.elements;

import com.huawei.cangjie1.psi.CjModifierList;
import com.huawei.cangjie1.psi.stubs.CangJieModifierListStub;
import com.huawei.cangjie1.psi.stubs.impl.CangJieModifierListStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.DataInputOutputUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.huawei.cangjie1.psi.stubs.impl.ModifierMaskUtils.computeMaskFromModifierList;


public class CjModifierListElementType<T extends CjModifierList> extends CjStubElementType<CangJieModifierListStub, T> {
    public CjModifierListElementType(@NotNull @NonNls String debugName, @NotNull Class<T> psiClass) {
        super(debugName, psiClass, CangJieModifierListStub.class);
    }
    @NotNull
    @Override
    public  CangJieModifierListStub createStub(@NotNull T psi, StubElement<?> parentStub) {
        return new CangJieModifierListStubImpl(parentStub, computeMaskFromModifierList(psi), this);
    }

    @Override
    public void serialize(@NotNull CangJieModifierListStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        long mask = ((CangJieModifierListStubImpl) stub).getMask();
        DataInputOutputUtil.writeLONG(dataStream, mask);
    }

    @NotNull
    @Override
    public CangJieModifierListStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        long mask = DataInputOutputUtil.readLONG(dataStream);
        return new CangJieModifierListStubImpl(parentStub, mask, this);
    }
}
