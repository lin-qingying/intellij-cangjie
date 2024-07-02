package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.contracts.description.CjContractDescriptionElement
import com.huawei.cangjie.contracts.description.CjContractDescriptionVisitor
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
//enum class CangJieContractEffectType {
////    CALLS {
////        override fun deserialize(dataStream: StubInputStream): CjCallsEffectDeclaration<CangJieTypeBean, Nothing?> {
////            val declaration = PARAMETER_REFERENCE.deserialize(dataStream)
////            val range = EventOccurrencesRange.values()[dataStream.readInt()]
////            return CjCallsEffectDeclaration(declaration as CjValueParameterReference, range)
////        }
////    }
////    ,
////    RETURNS {
////        override fun deserialize(dataStream: StubInputStream): CjContractDescriptionElement<CangJieTypeBean, Nothing?> {
////            return CjReturnsEffectDeclaration(CONSTANT.deserialize(dataStream) as CjConstantReference)
////        }
////    },
////    CONDITIONAL {
////        override fun deserialize(dataStream: StubInputStream): CjContractDescriptionElement<CangJieTypeBean, Nothing?> {
////            val descriptionElement = values()[dataStream.readInt()].deserialize(dataStream)
////            val condition = values()[dataStream.readInt()].deserialize(dataStream)
////            return CjConditionalEffectDeclaration(
////                descriptionElement as CjEffectDeclaration,
////                condition as CjBooleanExpression
////            )
////        }
////    },
////    IS_NULL {
////        override fun deserialize(dataStream: StubInputStream): CjContractDescriptionElement<CangJieTypeBean, Nothing?> {
////            return CjIsNullPredicate(
////                PARAMETER_REFERENCE.deserialize(dataStream) as CjValueParameterReference,
////                dataStream.readBoolean()
////            )
////        }
////    },
////    IS_INSTANCE {
////        override fun deserialize(dataStream: StubInputStream): CjContractDescriptionElement<CangJieTypeBean, Nothing?> {
////            return CjIsInstancePredicate(
////                PARAMETER_REFERENCE.deserialize(dataStream) as CjValueParameterReference,
////                deserializeType(dataStream)!!,
////                dataStream.readBoolean()
////            )
////        }
////    },
////    NOT {
////        override fun deserialize(dataStream: StubInputStream): CjContractDescriptionElement<CangJieTypeBean, Nothing?> {
////            return CjLogicalNot(values()[dataStream.readInt()].deserialize(dataStream) as CjBooleanExpression)
////        }
////    },
////    BOOLEAN_LOGIC {
////        override fun deserialize(dataStream: StubInputStream): CjContractDescriptionElement<CangJieTypeBean, Nothing?> {
////            val kind = if (dataStream.readBoolean()) LogicOperationKind.AND else LogicOperationKind.OR
////            val left = values()[dataStream.readInt()].deserialize(dataStream) as CjBooleanExpression
////            val right = values()[dataStream.readInt()].deserialize(dataStream) as CjBooleanExpression
////            return CjBinaryLogicExpression(left, right, kind)
////        }
////    },
////    PARAMETER_REFERENCE {
////        override fun deserialize(dataStream: StubInputStream): CjValueParameterReference<CangJieTypeBean, Nothing?> {
////            return CjValueParameterReference(dataStream.readInt(), IGNORE_REFERENCE_PARAMETER_NAME)
////        }
////    };
////    BOOLEAN_PARAMETER_REFERENCE {
////        override fun deserialize(dataStream: StubInputStream): CjValueParameterReference<CangJieTypeBean, Nothing?> {
////            return CjBooleanValueParameterReference(dataStream.readInt(), IGNORE_REFERENCE_PARAMETER_NAME)
////        }
////    },
////    CONSTANT {
////        override fun deserialize(dataStream: StubInputStream): CjContractDescriptionElement<CangJieTypeBean, Nothing?> {
////            return when (val str = dataStream.readNameString()!!) {
////                "TRUE" -> CangJieContractConstantValues.TRUE
////                "FALSE" -> CangJieContractConstantValues.FALSE
////                "NULL" -> CangJieContractConstantValues.NULL
////                "NOT_NULL" -> CangJieContractConstantValues.NOT_NULL
////                "WILDCARD" -> CangJieContractConstantValues.WILDCARD
////                else -> error("Unexpected $str")
////            }
////        }
////    };
//
//    abstract fun deserialize(dataStream: StubInputStream): CjContractDescriptionElement<CangJieTypeBean, Nothing?>
//
//    companion object {
//        const val IGNORE_REFERENCE_PARAMETER_NAME = "<ignore>"
//    }
//}
class CangJieContractEffectStubImpl {
}

class CangJieContractSerializationVisitor(val dataStream: StubOutputStream) :
    CjContractDescriptionVisitor<Unit, Nothing?, CangJieTypeBean, Nothing?>() {
//    override fun visitConditionalEffectDeclaration(
//        conditionalEffect: CjConditionalEffectDeclaration<CangJieTypeBean, Nothing?>,
//        data: Nothing?
//    ) {
//        dataStream.writeInt(CangJieContractEffectType.CONDITIONAL.ordinal)
//        conditionalEffect.effect.accept(this, data)
//        conditionalEffect.condition.accept(this, data)
//    }
//
//    override fun visitReturnsEffectDeclaration(returnsEffect: CjReturnsEffectDeclaration<CangJieTypeBean, Nothing?>, data: Nothing?) {
//        dataStream.writeInt(CangJieContractEffectType.RETURNS.ordinal)
//        dataStream.writeName(returnsEffect.value.name)
//    }
//
//    override fun visitCallsEffectDeclaration(callsEffect: CjCallsEffectDeclaration<CangJieTypeBean, Nothing?>, data: Nothing?) {
//        dataStream.writeInt(CangJieContractEffectType.CALLS.ordinal)
//        dataStream.writeInt(callsEffect.valueParameterReference.parameterIndex)
//        dataStream.writeInt(callsEffect.kind.ordinal)
//    }
//
//    override fun visitLogicalBinaryOperationContractExpression(
//        binaryLogicExpression: CjBinaryLogicExpression<CangJieTypeBean, Nothing?>,
//        data: Nothing?
//    ) {
//        dataStream.writeInt(CangJieContractEffectType.BOOLEAN_LOGIC.ordinal)
//        dataStream.writeBoolean(binaryLogicExpression.kind == LogicOperationKind.AND)
//        binaryLogicExpression.left.accept(this, data)
//        binaryLogicExpression.right.accept(this, data)
//    }
//
//    override fun visitLogicalNot(logicalNot: CjLogicalNot<CangJieTypeBean, Nothing?>, data: Nothing?) {
//        dataStream.writeInt(CangJieContractEffectType.NOT.ordinal)
//        logicalNot.arg.accept(this, data)
//    }
//
//    override fun visitIsInstancePredicate(isInstancePredicate: CjIsInstancePredicate<CangJieTypeBean, Nothing?>, data: Nothing?) {
//        dataStream.writeInt(CangJieContractEffectType.IS_INSTANCE.ordinal)
//        dataStream.writeInt(isInstancePredicate.arg.parameterIndex)
//        serializeType(dataStream, isInstancePredicate.type)
//        dataStream.writeBoolean(isInstancePredicate.isNegated)
//    }
//
//    override fun visitIsNullPredicate(isNullPredicate: CjIsNullPredicate<CangJieTypeBean, Nothing?>, data: Nothing?) {
//        dataStream.writeInt(CangJieContractEffectType.IS_NULL.ordinal)
//        dataStream.writeInt(isNullPredicate.arg.parameterIndex)
//        dataStream.writeBoolean(isNullPredicate.isNegated)
//    }
//
//
//    override fun visitConstantDescriptor(constantReference: CjConstantReference<CangJieTypeBean, Nothing?>, data: Nothing?) {
//        dataStream.writeInt(CangJieContractEffectType.CONSTANT.ordinal)
//        dataStream.writeName(constantReference.name)
//    }
//
//    override fun visitValueParameterReference(valueParameterReference: CjValueParameterReference<CangJieTypeBean, Nothing?>, data: Nothing?) {
//        dataStream.writeInt(CangJieContractEffectType.PARAMETER_REFERENCE.ordinal)
//        dataStream.writeInt(valueParameterReference.parameterIndex)
//    }
//
//    override fun visitBooleanValueParameterReference(
//        booleanValueParameterReference: CjBooleanValueParameterReference<CangJieTypeBean, Nothing?>,
//        data: Nothing?
//    ) {
//        dataStream.writeInt(CangJieContractEffectType.BOOLEAN_PARAMETER_REFERENCE.ordinal)
//        dataStream.writeInt(booleanValueParameterReference.parameterIndex)
//    }
}
