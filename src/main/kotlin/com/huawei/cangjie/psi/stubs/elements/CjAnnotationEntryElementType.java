package com.huawei.cangjie.psi.stubs.elements;

import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.psi.CjAnnotationEntry;
import com.huawei.cangjie.psi.CjValueArgumentList;
import com.huawei.cangjie.psi.stubs.CangJieAnnotationEntryStub;
import com.huawei.cangjie.psi.stubs.impl.CangJieAnnotationEntryStubImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class CjAnnotationEntryElementType extends CjStubElementType<CangJieAnnotationEntryStub, CjAnnotationEntry>{

    public CjAnnotationEntryElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjAnnotationEntry.class, CangJieAnnotationEntryStub.class);
    }

    @Override
    public @NotNull CangJieAnnotationEntryStub createStub(@NotNull CjAnnotationEntry psi, StubElement<? extends PsiElement> parentStub) {
        Name shortName = psi.getShortName();
        String resultName = shortName != null ? shortName.asString() : null;
        CjValueArgumentList valueArgumentList = psi.getValueArgumentList();
        boolean hasValueArguments = valueArgumentList != null && !valueArgumentList.getArguments().isEmpty();
        return new CangJieAnnotationEntryStubImpl((StubElement<?>) parentStub, StringRef.fromString(resultName), hasValueArguments );

    }

    @Override
    public void serialize(@NotNull CangJieAnnotationEntryStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getShortName());
        dataStream.writeBoolean(stub.hasValueArguments());
        if (stub instanceof CangJieAnnotationEntryStubImpl) {
//            Map<Name, ConstantValue<?>> arguments = ((CangJieAnnotationEntryStubImpl) stub).getValueArguments();
//            dataStream.writeInt(arguments != null ? arguments.size() : 0);
//            if (arguments != null) {
//                for (Map.Entry<Name, ConstantValue<?>> valueEntry : arguments.entrySet()) {
//                    dataStream.writeName(valueEntry.getKey().asString());
//                    ConstantValue<?> value = valueEntry.getValue();
//                    CangJieConstantValueKt.serialize(value, dataStream);
//                }
//            }
        }
    }

    @Override
    public @NotNull CangJieAnnotationEntryStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef text = dataStream.readName();
        boolean hasValueArguments = dataStream.readBoolean();
//        int valueArgCount = dataStream.readInt();
//        Map<Name, ConstantValue<?>> args = new LinkedHashMap<>();
//        for (int i = 0; i < valueArgCount; i++) {
//            args.put(Name.identifier(Objects.requireNonNull(dataStream.readNameString())),
//                    KotlinConstantValueKt.createConstantValue(dataStream));
//        }
        return new CangJieAnnotationEntryStubImpl((StubElement<?>) parentStub, text, hasValueArguments );

    }
}
