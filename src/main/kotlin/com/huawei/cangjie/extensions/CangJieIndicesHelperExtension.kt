package com.huawei.cangjie.extensions

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.types.CangJieType


interface CangJieIndicesHelperExtension {
    companion object : ProjectExtensionDescriptor<CangJieIndicesHelperExtension>(
        "com.huawei.cangjie.cangjieIndicesHelperExtension", CangJieIndicesHelperExtension::class.java
    )

    fun appendExtensionCallables(
        consumer: MutableList<in CallableDescriptor>,
        moduleDescriptor: ModuleDescriptor,
        receiverTypes: Collection<CangJieType>,
        nameFilter: (String) -> Boolean,
        lookupLocation: LookupLocation,
    )
}
