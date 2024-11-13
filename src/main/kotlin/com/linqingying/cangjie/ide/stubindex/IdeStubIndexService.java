package com.linqingying.cangjie.ide.stubindex;


import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.psi.CangJiePsiHeuristics;
import com.linqingying.cangjie.psi.CjFile;
import com.linqingying.cangjie.psi.CjTypeReference;
import com.linqingying.cangjie.psi.CjTypeStatement;
import com.linqingying.cangjie.psi.stubs.*;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.linqingying.cangjie.psi.stubs.elements.StubIndexService;
import com.linqingying.cangjie.psi.stubs.impl.CangJieFileStubImpl;
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

    private static void indexSuperNames(CangJieTypeStatementStub<? extends CjTypeStatement> stub, IndexSink sink) {
        for (String superName : stub.getSuperNames()) {
            sink.occurrence(CangJieSuperClassIndex.Helper.getIndexKey(), superName);
        }

//        if (!(stub instanceof CangJieClassStub)) {
//        }

//        CangJieModifierListStub modifierListStub = getModifierListStub(stub);

//        if (modifierListStub.hasModifier(CjTokens.ENUM_KEYWORD)) {
//            sink.occurrence(CangJieSuperClassIndex.Helper.getIndexKey(), Enum.class.getSimpleName());
//        }
//        if (modifierListStub.hasModifier(CjTokens.ANNOTATION_KEYWORD)) {
//            sink.occurrence(CangJieSuperClassIndex.Helper.getIndexKey(), Annotation.class.getSimpleName());
//        }
    }

    private static void processNames(
            @NotNull IndexSink sink,
            String shortName,
            FqName fqName,
            boolean level) {
        if (shortName != null) {
            sink.occurrence(CangJieClassShortNameIndex.Helper.getIndexKey(), shortName);
        }

        if (fqName != null) {
            sink.occurrence(CangJieFullClassNameIndex.Helper.getIndexKey(), fqName.asString());

            if (level) {
                sink.occurrence(CangJieTopLevelClassByPackageIndex.Helper.getIndexKey(), fqName.parent().asString());
            }
        }
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
        boolean prime = parent instanceof CangJieFileStub;
        //        else if (parent instanceof CangJieStructStub) {
//            var grand = parent.getParentStub();
//            bool primeGrand = grand instanceof CangJieClassStub && ((CangJieClassStub) grand).isTopLevel();
//
//            prime = ((CangJieStructStub) parent).isTopLevel() ||
//                    primeGrand && ((CangJieStructStub) parent).isCompanion();
//        }

        if (prime) {
            sink.occurrence(CangJiePrimeSymbolNameIndex.Helper.getIndexKey(), name);
        }
    }

    @Override
    public void indexVariable(@NotNull CangJieVariableStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(CangJieVariableShortNameIndex.Helper.getIndexKey(), name);

//            if (IndexUtilsKt.isDeclaredInObject(stub)) {
//                IndexUtilsKt.indexExtensionInObject(stub, sink);
//            }

            CjTypeReference typeReference = stub.getPsi().getTypeReference();
            if (typeReference != null && CangJiePsiHeuristics.isProbablyNothing(typeReference)) {
                sink.occurrence(CangJieVariableNothingVariableShortNameIndex.Helper.getIndexKey(), name);
            }
            indexPrime(stub, sink);
        }

        if (stub.isTopLevel()) {
            FqName fqName = stub.getFqName();
            // can have special fq name in case of syntactically incorrect property with no name
            if (fqName != null) {
                sink.occurrence(CangJieTopLevelVariableFqnNameIndex.Helper.getIndexKey(), fqName.asString());
                sink.occurrence(CangJieTopLevelVariableByPackageIndex.Helper.getIndexKey(), fqName.parent().asString());
                IndexUtilsKt.indexTopLevelExtension(stub, sink);
            }
        }

        IndexUtilsKt.indexInternals(stub, sink);
    }

    @Override
    public void indexProperty(@NotNull CangJiePropertyStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(CangJiePropertyShortNameIndex.Helper.getIndexKey(), name);

//            if (IndexUtilsKt.isDeclaredInObject(stub)) {
//                IndexUtilsKt.indexExtensionInObject(stub, sink);
//            }

            CjTypeReference typeReference = stub.getPsi().getTypeReference();
            if (typeReference != null && CangJiePsiHeuristics.isProbablyNothing(typeReference)) {
                sink.occurrence(CangJieProbablyNothingPropertyShortNameIndex.Helper.getIndexKey(), name);
            }
            indexPrime(stub, sink);
        }

        if (stub.isExtension()) {
            FqName fqName = stub.getFqName();
            // can have special fq name in case of syntactically incorrect property with no name
            if (fqName != null) {
                sink.occurrence(CangJieTopLevelPropertyFqnNameIndex.Helper.getIndexKey(), fqName.asString());
                sink.occurrence(CangJieTopLevelPropertyByPackageIndex.Helper.getIndexKey(), fqName.parent().asString());
                IndexUtilsKt.indexTopLevelExtension(stub, sink);
            }
        }

        IndexUtilsKt.indexInternals(stub, sink);
    }

    @Override
    public void indexFile(@NotNull CangJieFileStub stub, @NotNull IndexSink sink) {
        FqName packageFqName = stub.getPackageFqName();

        sink.occurrence(CangJieExactPackagesIndex.NAME, packageFqName.asString());


//
//        FqName facadeFqName = ((CangJieFileStubImpl) stub).getFacadeFqName();
//        if (facadeFqName != null) {
//            sink.occurrence(CangJieFileFacadeFqNameIndex.Helper.getIndexKey(), facadeFqName.asString());
//            sink.occurrence(CangJieFileFacadeShortNameIndex.Helper.getIndexKey(), facadeFqName.shortName().asString());
//            sink.occurrence(CangJieFileFacadeClassByPackageIndex.Helper.getIndexKey(), packageFqName.asString());
//        }
//
//        FqName partFqName = ((CangJieFileStubImpl) stub).getPartFqName();
//        if (partFqName != null) {
//            sink.occurrence(CangJieFilePartClassIndex.Helper.getIndexKey(), partFqName.asString());
//        }
//
//        List<String> partNames = ((CangJieFileStubImpl) stub).getFacadePartSimpleNames();
//        if (partNames != null) {
//            for (String partName : partNames) {
//                if (partName == null) {
//                    continue;
//                }
//                FqName multiFileClassPartFqName = packageFqName.child(Name.identifier(partName));
//                sink.occurrence(CangJieMultiFileClassPartIndex.Helper.getIndexKey(), multiFileClassPartFqName.asString());
//            }
//        }
    }

    @Override
    public void indexTypeAlias(@NotNull CangJieTypeAliasStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(CangJieTypeAliasShortNameIndex.Helper.getIndexKey(), name);
            indexPrime(stub, sink);
        }

        IndexUtilsKt.indexTypeAliasExpansion(stub, sink);

        FqName fqName = stub.getFqName();
        if (fqName != null) {

            sink.occurrence(CangJieTopLevelTypeAliasFqNameIndex.Helper.getIndexKey(), fqName.asString());
            sink.occurrence(CangJieTopLevelTypeAliasByPackageIndex.Helper.getIndexKey(), fqName.parent().asString());

        }

//        ClassId classId = stub.getClassId();
//        if (classId != null && !stub.isTopLevel()) {
//            sink.occurrence(CangJieInnerTypeAliasClassIdIndex.Helper.getIndexKey(), classId.asString());
//        }
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
    public void indexMainFunction(@NotNull CangJieFunctionStub stub, @NotNull IndexSink sink) {
        if (stub.isTopLevel()) {
            // can have special fq name in case of syntactically incorrect function with no name
            FqName fqName = stub.getFqName();
            if (fqName != null) {
                sink.occurrence(CangJieMainFunctionFqnNameIndex.Helper.getIndexKey(), fqName.asString());
//                sink.occurrence(CangJieTopLevelFunctionByPackageIndex.Helper.getIndexKey(), fqName.parent().asString());
                IndexUtilsKt.indexTopLevelExtension(stub, sink);
            }
        }

        IndexUtilsKt.indexInternals(stub, sink);
    }

    @Override
    public void indexMacroFunction(@NotNull CangJieFunctionStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(CangJieMacroDeclarationShortNameIndex.Helper.getIndexKey(), name);

            indexPrime(stub, sink);
        }
        FqName fqName = stub.getFqName();
        if (fqName != null) {
            sink.occurrence(CangJieMacroDeclarationFqnNameIndex.Helper.getIndexKey(), fqName.asString());
            sink.occurrence(CangJieMacroDeclarationByPackageIndex.Helper.getIndexKey(), fqName.parent().asString());
            IndexUtilsKt.indexTopLevelExtension(stub, sink);
        }
    }

    @Override
    public void indexFunction(@NotNull CangJieFunctionStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(CangJieFunctionShortNameIndex.Helper.getIndexKey(), name);

//            if (IndexUtilsKt.isDeclaredInStruct(stub)) {
//                IndexUtilsKt.indexExtensionInStruct(stub, sink);
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
//如果该方法是顶层方法或有扩展接收器，则将其索引到顶层
        if (stub.isTopLevel() || stub.isExtension()) {
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

    @Override
    public void indexExtend(@NotNull CangJieExtendStub stub, @NotNull IndexSink sink) {
        FqName fqName = stub.getFqName();


        if (fqName != null) {
            sink.occurrence(CangJieExtendClassNameIndex.Helper.getIndexKey(), fqName.asString());
        }

        indexSuperNames(stub, sink);


    }

    @Override
    public void indexClass(@NotNull CangJieClassStub stub, @NotNull IndexSink sink) {

        indexTypeStatementStub(stub, sink);
    }

    @Override
    public void indexInterface(@NotNull CangJieInterfaceStub stub, @NotNull IndexSink sink) {
//        processNames(sink, stub.getName(), stub.getFqName()/*, stub.isTopLevel()*/);
//        sink.occurrence(CangJieClassShortNameIndex.Helper.getIndexKey(), "DefaultImpls");
//        indexSuperNames(stub, sink);
//        indexPrime(stub, sink);
        indexTypeStatementStub(stub, sink);

    }

    @Override
    public void indexImports(@NotNull CangJieImportDirectiveStub stub, @NotNull IndexSink sink) {

//        if (stub.getModifierVisibility() == DescriptorVisibilities.PRIVATE) {
//            return;
//        }

//        String name = stub.getName();
//        if (name != null) {
//            sink.occurrence(CangJieImportShortNameIndex.Helper.getIndexKey(), name);
//            indexPrime(stub, sink);
//        }
//
//        IndexUtilsKt.indexTypeAliasExpansion(stub, sink);

        FqName fqName = stub.getPackageFqName();
        if (fqName != null) {

            sink.occurrence(CangJieImportFqNameForPackageNameIndex.Helper.getIndexKey(), fqName.asString());
//            sink.occurrence(CangJieImportFqNameForPackageNameIndex.Helper.getIndexKey(), fqName.parent().asString());

        }

    }

    @Override
    public void indexEnumEntry(@NotNull CangJieEnumEntryStub stub, @NotNull IndexSink sink) {

        processNames(sink, stub.getName(), stub.getFqName()/*, stub.isTopLevel()*/);
        indexSuperNames(stub, sink);

        indexPrime(stub, sink);
        sink.occurrence(CangJieEnumEntryShortNameIndex.Helper.getIndexKey(), stub.getName());
    }

    @Override
    public void indexEnum(@NotNull CangJieEnumStub stub, @NotNull IndexSink sink) {
        indexTypeStatementStub(stub, sink);

    }

    private void indexTypeStatementStub(@NotNull CangJieTypeStatementStub<? extends CjTypeStatement> stub, @NotNull IndexSink sink) {

        processNames(sink, stub.getName(), stub.getFqName()/*, stub.isTopLevel()*/);

        if (stub instanceof CangJieInterfaceStub) {
            sink.occurrence(CangJieClassShortNameIndex.Helper.getIndexKey(), "DefaultImpls");
        }

        indexSuperNames(stub, sink);

        indexPrime(stub, sink);

    }

    @Override
    public void indexStruct(@NotNull CangJieStructStub stub, @NotNull IndexSink sink) {
        indexTypeStatementStub(stub, sink);

    }

    private void processNames(IndexSink sink, String name, FqName fqName) {

        processNames(sink, name, fqName, true);
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
