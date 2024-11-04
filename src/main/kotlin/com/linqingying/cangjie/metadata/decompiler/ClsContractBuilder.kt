package com.linqingying.cangjie.metadata.decompiler

import com.linqingying.cangjie.contracts.description.CjEffectDeclaration
import com.linqingying.cangjie.metadata.ProtoBuf

//
//class ClsContractBuilder(private val c: ClsStubBuilderContext, private val typeStubBuilder: TypeClsStubBuilder) :
//    ProtoBufContractDeserializer<CangJieTypeBean, Nothing?, ProtoBuf.Function>() {
//
//    fun loadContract(proto: ProtoBuf.Function): List<CjEffectDeclaration<CangJieTypeBean, Nothing?>>? {
//        return proto.contract.effectList.map { loadPossiblyConditionalEffect(it, proto) ?: return null }
//    }
//
//    override fun extractVariable(valueParameterIndex: Int, owner: ProtoBuf.Function): CjValueParameterReference<CangJieTypeBean, Nothing?> {
//        val type = if (valueParameterIndex < 0) {
//            owner.receiverType(c.typeTable)
//        } else owner.valueParameterList[valueParameterIndex].type(c.typeTable)
//        return if (type?.hasClassName() == true && c.nameResolver.getClassId(type.className) == StandardClassIds.Boolean) {
//            CjBooleanValueParameterReference(valueParameterIndex, name = IGNORE_REFERENCE_PARAMETER_NAME)
//        } else CjValueParameterReference(valueParameterIndex, name = IGNORE_REFERENCE_PARAMETER_NAME)
//    }
//
//    override fun extractType(proto: ProtoBuf.Expression): CangJieTypeBean? {
//        return typeStubBuilder.createCangJieTypeBean(proto.isInstanceType(c.typeTable))
//    }
//
//    override fun loadConstant(value: ProtoBuf.Expression.ConstantValue): CjConstantReference<CangJieTypeBean, Nothing?> {
//        return when (value) {
//            ProtoBuf.Expression.ConstantValue.TRUE -> CangJieContractConstantValues.TRUE
//            ProtoBuf.Expression.ConstantValue.FALSE -> CangJieContractConstantValues.FALSE
//            ProtoBuf.Expression.ConstantValue.NULL -> CangJieContractConstantValues.NULL
//        }
//    }
//
//    override fun getNotNull(): CjConstantReference<CangJieTypeBean, Nothing?> {
//        return CangJieContractConstantValues.NOT_NULL
//    }
//
//    override fun getWildcard(): CjConstantReference<CangJieTypeBean, Nothing?> {
//        return CangJieContractConstantValues.WILDCARD
//    }
//}
