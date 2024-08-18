package com.huawei.cangjie.psi.stubs.elements;

import com.huawei.cangjie.descriptors.DescriptorVisibilities;
import com.huawei.cangjie.descriptors.DescriptorVisibility;
import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.psi.CjImportDirective;
//import com.huawei.cangjie.psi.CjImportDirectiveItem;
import com.huawei.cangjie.psi.stubs.CangJieImportDirectiveStub;
import com.huawei.cangjie.psi.stubs.impl.CangJieImportDirectiveStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class CjImportDirectiveElementType extends CjStubElementType<CangJieImportDirectiveStub, CjImportDirective> {
    public CjImportDirectiveElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjImportDirective.class, CangJieImportDirectiveStub.class);
    }

    @NotNull
    @Override
    public CangJieImportDirectiveStub createStub(@NotNull CjImportDirective psi, StubElement parentStub) {
        FqName importedFqName = psi.getImportedFqName();
        StringRef fqName = StringRef.fromString(importedFqName == null ? null : importedFqName.asString());
        return new CangJieImportDirectiveStubImpl((StubElement<?>) parentStub, psi.isAllUnder(), fqName, psi.isValidImport(),psi.getModifierVisibility());
    }

    @Override
    public void serialize(@NotNull CangJieImportDirectiveStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeBoolean(stub.isAllUnder());
        FqName importedFqName = stub.getImportedFqName();
        dataStream.writeName(importedFqName != null ? importedFqName.asString() : null);
        dataStream.writeBoolean(stub.isValid());
        dataStream.writeName(stub.getModifierVisibility().getName());
    }

    @NotNull
    @Override
    public CangJieImportDirectiveStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        boolean isAllUnder = dataStream.readBoolean();
        StringRef importedName = dataStream.readName();
        boolean isValid = dataStream.readBoolean();
        StringRef modifierVisibility = dataStream.readName();
        DescriptorVisibility visibility  = null;
        if (modifierVisibility != null) {
            visibility = DescriptorVisibilities.formName(modifierVisibility.getString());
        }else {
            visibility = DescriptorVisibilities.PRIVATE;
        }
        return new CangJieImportDirectiveStubImpl((StubElement<?>) parentStub, isAllUnder, importedName,isValid,visibility);
    }
}
