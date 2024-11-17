package com.linqingying.cangjie.psi.stubs.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.psi.CjMacroExpression
import com.linqingying.cangjie.psi.stubs.CangJieMacroExpressionStub
import com.linqingying.cangjie.psi.stubs.impl.CangJieAnnotationEntryStubImpl
import com.linqingying.cangjie.psi.stubs.impl.CangJieMacroExpressionStubImpl

class CjMacroExpressionElementType(debugName:String) :  CjStubElementType<CangJieMacroExpressionStub , CjMacroExpression>(
    debugName,
    CjMacroExpression::class.java,
    CangJieMacroExpressionStub::class.java
) {
    override fun serialize(stub: CangJieMacroExpressionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getShortName())
        dataStream.writeBoolean(stub.hasValueArguments())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieMacroExpressionStub {
        val text = dataStream.readName()
        val hasValueArguments = dataStream.readBoolean()
        //        int valueArgCount = dataStream.readInt();
//        Map<Name, ConstantValue<?>> args = new LinkedHashMap<>();
//        for (int i = 0; i < valueArgCount; i++) {
//            args.put(Name.identifier(Objects.requireNonNull(dataStream.readNameString())),
//                    CangJieConstantValueKt.createConstantValue(dataStream));
//        }
        return CangJieMacroExpressionStubImpl(parentStub, text, hasValueArguments)
    }

    override fun createStub(
        psi: CjMacroExpression,
        parentStub: StubElement<out PsiElement>?
    ): CangJieMacroExpressionStub {
        val shortName = psi.shortName
        val resultName = shortName?.asString()
        val valueArgumentList = psi.valueArgumentList
        val hasValueArguments = valueArgumentList != null && valueArgumentList.arguments.isNotEmpty()
        return CangJieMacroExpressionStubImpl(parentStub, StringRef.fromString(resultName), hasValueArguments)
    }


}
