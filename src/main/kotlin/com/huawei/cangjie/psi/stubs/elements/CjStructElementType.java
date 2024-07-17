package com.huawei.cangjie.psi.stubs.elements;

import com.huawei.cangjie.name.ClassId;
import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.psi.CjClass;
import com.huawei.cangjie.psi.CjInterface;
import com.huawei.cangjie.psi.CjStruct;
import com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.huawei.cangjie.psi.psiUtil.StubUtils;
import com.huawei.cangjie.psi.stubs.CangJieClassStub;
import com.huawei.cangjie.psi.stubs.CangJieInterfaceStub;
import com.huawei.cangjie.psi.stubs.CangJieStructStub;
import com.huawei.cangjie.psi.stubs.impl.CangJieClassStubImpl;
import com.huawei.cangjie.psi.stubs.impl.CangJieInterfaceStubImpl;
import com.huawei.cangjie.psi.stubs.impl.CangJieStructStubImpl;
import com.huawei.cangjie.psi.stubs.impl.Utils;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class CjStructElementType extends CjStubElementType<CangJieStructStub, CjStruct> {

    public CjStructElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjStruct.class, CangJieStructStub.class);
    }

    public void indexStub(@NotNull CangJieStructStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexStruct(stub, sink);
    }

    @NotNull
    @Override
    public CjStruct createPsi(@NotNull CangJieStructStub stub) {
        return new CjStruct(stub);
    }


    @NotNull
    @Override
    public CjStruct createPsiFromAst(@NotNull ASTNode node) {
        return new CjStruct(node);
    }

    @Override
    public @NotNull CangJieStructStub createStub(@NotNull CjStruct psi, StubElement<? extends PsiElement> parentStub) {
        FqName fqName = CjPsiUtilKt.safeFqNameForLazyResolve(psi);

        List<String> superNames = CjPsiUtilKt.getSuperNames(psi);
        ClassId classId = StubUtils.createNestedClassId(parentStub, psi);
        return new CangJieStructStubImpl(
                CjStubElementTypes.STRUCT, parentStub,
                StringRef.fromString(fqName != null ? fqName.asString() : null), classId,
                StringRef.fromString(psi.getName()),
                Utils.INSTANCE.wrapStrings(superNames),
                psi.isLocal()
        )  ;
    }

    @Override
    public void serialize(@NotNull CangJieStructStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName == null ? null : fqName.asString());

        StubUtils.serializeClassId(dataStream, stub.getClassId());


        dataStream.writeBoolean(stub.isLocal());
//        dataStream.writeBoolean(stub.isTopLevel());

        List<String> superNames = stub.getSuperNames();
        dataStream.writeVarInt(superNames.size());
        for (String name : superNames) {
            dataStream.writeName(name);
        }
    }

    @Override
    public @NotNull CangJieStructStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        StringRef qualifiedName = dataStream.readName();

        ClassId classId = StubUtils.deserializeClassId(dataStream);


        boolean isLocal = dataStream.readBoolean();
//        bool isTopLevel = dataStream.readBoolean();

        int superCount = dataStream.readVarInt();
        StringRef[] superNames = StringRef.createArray(superCount);
        for (int i = 0; i < superCount; i++) {
            superNames[i] = dataStream.readName();
        }

        return new CangJieStructStubImpl(
                CjStubElementTypes.STRUCT, (StubElement<?>) parentStub, qualifiedName,classId, name, superNames,
                isLocal
        );
    }
}
