package com.huawei.cangjie.psi.stubs.elements

import com.huawei.cangjie.psi.CjAnnotationEntry
import com.huawei.cangjie.psi.stubs.CangJieAnnotationEntryStub
import com.huawei.cangjie.psi.stubs.impl.CangJieAnnotationEntryStubImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjAnnotationEntryElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieAnnotationEntryStub , CjAnnotationEntry >(
        debugName,
        CjAnnotationEntry::class.java,
        CangJieAnnotationEntryStub::class.java
    ) {
    override fun createStub(
        psi: CjAnnotationEntry,
        parentStub: StubElement<out PsiElement?>
    ): CangJieAnnotationEntryStub {
        val shortName = psi.shortName
        val resultName = shortName?.asString()
        val valueArgumentList = psi.valueArgumentList
        val hasValueArguments = valueArgumentList != null && !valueArgumentList.arguments.isEmpty()
        return CangJieAnnotationEntryStubImpl(parentStub, StringRef.fromString(resultName), hasValueArguments)
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieAnnotationEntryStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getShortName())
        dataStream.writeBoolean(stub.hasValueArguments())
        if (stub is CangJieAnnotationEntryStubImpl) {
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

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieAnnotationEntryStub {
        val text = dataStream.readName()
        val hasValueArguments = dataStream.readBoolean()
        //        int valueArgCount = dataStream.readInt();
//        Map<Name, ConstantValue<?>> args = new LinkedHashMap<>();
//        for (int i = 0; i < valueArgCount; i++) {
//            args.put(Name.identifier(Objects.requireNonNull(dataStream.readNameString())),
//                    CangJieConstantValueKt.createConstantValue(dataStream));
//        }
        return CangJieAnnotationEntryStubImpl(parentStub, text, hasValueArguments)
    }
}
