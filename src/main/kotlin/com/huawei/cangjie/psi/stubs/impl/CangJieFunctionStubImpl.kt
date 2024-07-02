package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.contracts.description.CjContractDescriptionElement
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjFunctionImpl
import com.huawei.cangjie.psi.stubs.CangJieFunctionStub
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import java.io.IOException


class CangJieFunctionStubImpl(

    parent: StubElement<out PsiElement>?,
    element: IStubElementType<*,*>,
    private val nameRef: StringRef?,
    private val isTopLevel: Boolean,
    private val fqName: FqName?,
    private val isExtension: Boolean,
    private val hasBlockBody: Boolean,
    private val hasBody: Boolean,
    private val hasTypeParameterListBeforeFunctionName: Boolean,
//    private val mayHaveContract: Boolean,
    val contract: List<CjContractDescriptionElement<CangJieTypeBean, Nothing?>>?,

    val origin: CangJieStubOrigin?
) : CangJieStubBaseImpl<CjFunctionImpl>(parent, element), CangJieFunctionStub {
    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level functions")
        }
    }
//    @Throws(IOException::class)
//    fun serializeContract(dataStream: StubOutputStream) {
//        val effects: List<CjContractDescriptionElement<CangJieTypeBean, Nothing?>>? = contract
//        dataStream.writeInt(effects?.size ?: 0)
//        val visitor = CangJieContractSerializationVisitor(dataStream)
//        effects?.forEach { it.accept(visitor, null) }
//    }

    override fun getFqName() = fqName

    override fun getName() = StringRef.toString(nameRef)
    override fun isTopLevel() = isTopLevel
    override fun isExtension() = isExtension
    override fun hasBlockBody() = hasBlockBody
    override fun hasBody() = hasBody
    override fun hasTypeParameterListBeforeFunctionName() = hasTypeParameterListBeforeFunctionName
//    override fun mayHaveContract(): Boolean = mayHaveContract
companion object {
//    fun deserializeContract(dataStream: StubInputStream): List<CjContractDescriptionElement<CangJieTypeBean, Nothing?>> {
//        val effects = mutableListOf<CjContractDescriptionElement<CangJieTypeBean, Nothing?>>()
//        val count: Int = dataStream.readInt()
////        for (i in 0 until count) {
////            val effectType: CangJieContractEffectType = CangJieContractEffectType.entries[dataStream.readInt()]
////            effects.add(effectType.deserialize(dataStream))
////        }
//        return effects
//    }
}

}
