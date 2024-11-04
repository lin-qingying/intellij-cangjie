package com.linqingying.cangjie.contracts

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.TypeTable
import com.linqingying.cangjie.serialization.deserialization.TypeDeserializer

interface ContractDeserializer {
    fun deserializeContractFromFunction(
        proto: ProtoBuf.Function,
        ownerFunction: FunctionDescriptor,
        typeTable: TypeTable,
        typeDeserializer: TypeDeserializer
    ): Pair<CallableDescriptor.UserDataKey<*>, ContractProvider>?

    companion object {
        val DEFAULT = object : ContractDeserializer {
            override fun deserializeContractFromFunction(
                proto: ProtoBuf.Function,
                ownerFunction: FunctionDescriptor,
                typeTable: TypeTable,
                typeDeserializer: TypeDeserializer
            ): Pair<CallableDescriptor.UserDataKey<*>, Nothing>? = null
        }
    }
}
