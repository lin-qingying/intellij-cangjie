package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.SimpleType

interface FlexibleTypeDeserializer {
    fun create(proto: ProtoBuf.Type, flexibleId: String, lowerBound: SimpleType, upperBound: SimpleType): CangJieType

    object ThrowException : FlexibleTypeDeserializer {
        override fun create(proto: ProtoBuf.Type, flexibleId: String, lowerBound: SimpleType, upperBound: SimpleType): CangJieType =
            throw IllegalArgumentException("This method should not be used.")
    }
}
