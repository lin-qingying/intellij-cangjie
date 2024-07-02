package com.huawei.cangjie.idea.stubindex;


import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.psi.CangJiePsiHeuristics;
import com.huawei.cangjie.psi.CjFile;
import com.huawei.cangjie.psi.CjTypeReference;
import com.huawei.cangjie.psi.stubs.*;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.huawei.cangjie.psi.stubs.elements.StubIndexService;
import com.huawei.cangjie.psi.stubs.impl.CangJieFileStubImpl;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IdeStubIndexService extends StubIndexService {


    @Nullable
    private static CangJieModifierListStub getModifierListStub(@NotNull CangJieStubWithFqName<?> stub) {
        return stub.findChildStubByType(CjStubElementTypes.MODIFIER_LIST);
    }


    @NotNull
    @Override
    public CangJieFileStub createFileStub(@NotNull CjFile file) {
        String packageFqName = file.getPackageFqNameByTree().asString();


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
        } else {
            dataStream.writeInt(facadePartNames.size());
            for (String partName : facadePartNames) {
                dataStream.writeName(partName);
            }
        }
    }

    @Override
    public void indexFunction(@NotNull CangJieFunctionStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(CangJieFunctionShortNameIndex.Helper.getIndexKey(), name);

//            if (IndexUtilsKt.isDeclaredInObject(stub)) {
//                IndexUtilsKt.indexExtensionInObject(stub, sink);
//            }

            CjTypeReference typeReference = stub.getPsi().getTypeReference();
            if (typeReference != null && CangJiePsiHeuristics.isProbablyNothing(typeReference)) {
                sink.occurrence(CangJieProbablyNothingFunctionShortNameIndex.Helper.getIndexKey(), name);
            }
//
       /*     if (stub.mayHaveContract()) {
                sink.occurrence(CangJieProbablyContractedFunctionShortNameIndex.Helper.getIndexKey(), name);
            }*/

            indexPrime(stub, sink);
        }

        if (stub.isTopLevel()) {
            // can have special fq name in case of syntactically incorrect function with no name
            FqName fqName = stub.getFqName();
            if (fqName != null) {
                sink.occurrence(CangJieTopLevelFunctionFqnNameIndex.Helper.getIndexKey(), fqName.asString());
                sink.occurrence(CangJieTopLevelFunctionByPackageIndex.Helper.getIndexKey(), fqName.parent().asString());
                IndexUtilsKt.indexTopLevelExtension(stub, sink);
            }
        }

        IndexUtilsKt.indexInternals(stub, sink);

    }
    /**
     * Indexes non-private top-level symbols or members of top-level objects and companion objects subject to this object serving as namespaces.
     */
    private static void indexPrime(CangJieStubWithFqName<?> stub, IndexSink sink) {
        String name = stub.getName();
        if (name == null) return;

        CangJieModifierListStub modifierList = getModifierListStub(stub);
        if (modifierList != null && modifierList.hasModifier(CjTokens.PRIVATE_KEYWORD)) return;
        if (modifierList != null && modifierList.hasModifier(CjTokens.OVERRIDE_KEYWORD)) return;

        var parent = stub.getParentStub();
        boolean prime = false;
        if (parent instanceof CangJieFileStub) {
            prime = true;
        }
//        else if (parent instanceof CangJieStructStub) {
//            var grand = parent.getParentStub();
//            boolean primeGrand = grand instanceof CangJieClassStub && ((CangJieClassStub) grand).isTopLevel();
//
//            prime = ((CangJieStructStub) parent).isTopLevel() ||
//                    primeGrand && ((CangJieStructStub) parent).isCompanion();
//        }

        if (prime) {
            sink.occurrence(CangJiePrimeSymbolNameIndex.Helper.getIndexKey(), name);
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
