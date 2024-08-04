package com.linqingying.cangjie.ide.stubindex;


import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.psi.CjFile;
import com.linqingying.cangjie.psi.stubs.*;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.linqingying.cangjie.psi.stubs.elements.StubIndexService;
import com.linqingying.cangjie.psi.stubs.impl.CangJieFileStubImpl;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IdeStubIndexService extends StubIndexService{











    @Nullable
    private static CangJieModifierListStub getModifierListStub(@NotNull CangJieStubWithFqName<?> stub) {
        return stub.findChildStubByType(CjStubElementTypes.MODIFIER_LIST);
    }











    @NotNull
    @Override
    public CangJieFileStub createFileStub(@NotNull CjFile file) {
        String packageFqName =file.getPackageFqNameByTree().asString();


        return new CangJieFileStubImpl(file, packageFqName, null, null, null);
    }

    @Override
    public void serializeFileStub(
            @NotNull CangJieFileStub stub, @NotNull StubOutputStream dataStream
    ) throws IOException {
        CangJieFileStubImpl fileStub = (CangJieFileStubImpl) stub;
        dataStream.writeName(fileStub.getPackageFqName().asString());

        FqName facadeFqName = fileStub.getFacadeFqName();
        dataStream.writeName(facadeFqName != null ? facadeFqName.asString() : null);
        dataStream.writeName(fileStub.getPartSimpleName());
        List<String> facadePartNames = fileStub.getFacadePartSimpleNames();
        if (facadePartNames == null) {
            dataStream.writeInt(0);
        }
        else {
            dataStream.writeInt(facadePartNames.size());
            for (String partName : facadePartNames) {
                dataStream.writeName(partName);
            }
        }
    }

    @NotNull
    @Override
    public CangJieFileStub deserializeFileStub(@NotNull StubInputStream dataStream) throws IOException {
        String packageFqNameAsString = dataStream.readNameString();
        if (packageFqNameAsString == null) {
            throw new IllegalStateException("Can't read package fqname from stream");
        }


        String facadeString = dataStream.readNameString();
        String partSimpleName = dataStream.readNameString();
        int numPartNames = dataStream.readInt();
        List<String> facadePartNames = new ArrayList<>();
        for (int i = 0; i < numPartNames; ++i) {
            String partNameRef = dataStream.readNameString();
            facadePartNames.add(partNameRef);
        }
        return new CangJieFileStubImpl(null, packageFqNameAsString, facadeString, partSimpleName, facadePartNames);
    }
}
