package com.huawei.cangjie.psi.stubs.elements;

import com.huawei.cangjie.psi.CjImportDirectiveItem;
import com.huawei.cangjie.psi.stubs.CangJieImportDirectiveItemStub;
import com.huawei.cangjie.psi.stubs.impl.CangJieImportDirectiveItemStubImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CjImportDirectiveItemElementType extends CjStubElementType<CangJieImportDirectiveItemStub, CjImportDirectiveItem> {

    public CjImportDirectiveItemElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjImportDirectiveItem.class, CangJieImportDirectiveItemStub.class);
    }

    @Override
    public @NotNull CangJieImportDirectiveItemStub createStub(@NotNull CjImportDirectiveItem cjImportDirectiveItem, StubElement<? extends PsiElement> stubElement) {
        return new CangJieImportDirectiveItemStubImpl(stubElement);

    }

    @Override
    public void serialize(@NotNull CangJieImportDirectiveItemStub cangJieImportDirectiveItemStub, @NotNull StubOutputStream stubOutputStream) throws IOException {

    }

    @Override
    public @NotNull CangJieImportDirectiveItemStub deserialize(@NotNull StubInputStream stubInputStream, StubElement stubElement) throws IOException {
        return new CangJieImportDirectiveItemStubImpl((StubElement<?>) stubElement);

    }
}
