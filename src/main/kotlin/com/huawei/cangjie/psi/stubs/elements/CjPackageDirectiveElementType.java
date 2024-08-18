package com.huawei.cangjie.psi.stubs.elements;

import com.huawei.cangjie.descriptors.DescriptorVisibilities;
import com.huawei.cangjie.descriptors.DescriptorVisibility;
import com.huawei.cangjie.psi.CjPackageDirective;
import com.huawei.cangjie.psi.stubs.CangJiePackageDirectiveStub;
import com.huawei.cangjie.psi.stubs.impl.CangJiePackageDirectiveStubImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CjPackageDirectiveElementType extends CjStubElementType<CangJiePackageDirectiveStub, CjPackageDirective> {


    public CjPackageDirectiveElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjPackageDirective.class, CangJiePackageDirectiveStub.class);
    }

    @Override
    public @NotNull CangJiePackageDirectiveStub createStub(@NotNull CjPackageDirective psi, StubElement<? extends PsiElement> parentStub) {
        return new CangJiePackageDirectiveStubImpl(parentStub, psi.getModifierVisibility());

    }

    @Override
    public void serialize(@NotNull CangJiePackageDirectiveStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getModifierVisibility().getName());

    }

    @Override
    public @NotNull CangJiePackageDirectiveStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef modifierVisibility = dataStream.readName();
        DescriptorVisibility visibility = null;
        if (modifierVisibility != null) {
            visibility = DescriptorVisibilities.formName(modifierVisibility.getString());
        } else {
            visibility = DescriptorVisibilities.PRIVATE;
        }
        return new CangJiePackageDirectiveStubImpl((StubElement<?>) parentStub, visibility);

    }
}
