package com.linqingying.cangjie.contracts

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.TypeTable
import com.linqingying.cangjie.serialization.deserialization.DeserializationConfiguration
import com.linqingying.cangjie.serialization.deserialization.TypeDeserializer
import com.linqingying.cangjie.storage.StorageManager


class ContractDeserializerImpl(
    private val configuration: DeserializationConfiguration,
    private val storageManager: StorageManager
) : ContractDeserializer {
    override fun deserializeContractFromFunction(
        proto: ProtoBuf.Function,
        ownerFunction: FunctionDescriptor,
        typeTable: TypeTable,
        typeDeserializer: TypeDeserializer
    ): Pair<CallableDescriptor.UserDataKey<*>, ContractProvider>? {
        if (!proto.hasContract()) return null
        if (!configuration.readDeserializedContracts) return null
//        val worker = ContractDeserializationWorker(typeTable, typeDeserializer, ownerFunction, storageManager)
//        val contract = worker.deserializeContract(proto.contract) ?: return null
//        return ContractProviderKey to ContractProviderImpl(contract)
        return ContractProviderKey to ContractProvider.Companion.Default

    }
}
object ContractProviderKey : CallableDescriptor.UserDataKey<AbstractContractProvider?>
