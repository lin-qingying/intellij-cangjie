package com.linqingying.cangjie.extensions

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.types.CangJieType


interface CangJieIndicesHelperExtension {
    companion object : ProjectExtensionDescriptor<CangJieIndicesHelperExtension>(
        "com.linqingying.cangjie.cangjieIndicesHelperExtension", CangJieIndicesHelperExtension::class.java
    )

    fun appendExtensionCallables(
        consumer: MutableList<in CallableDescriptor>,
        moduleDescriptor: ModuleDescriptor,
        receiverTypes: Collection<CangJieType>,
        nameFilter: (String) -> Boolean,
        lookupLocation: LookupLocation,
    )
}
