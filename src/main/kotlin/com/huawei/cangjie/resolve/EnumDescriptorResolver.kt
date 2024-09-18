package com.huawei.cangjie.resolve

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.lazy.LazyClassContext
import com.huawei.cangjie.resolve.lazy.data.CjTypeStatementInfo
import com.huawei.cangjie.resolve.lazy.descriptors.LazyEnumEntryDescriptor
import com.huawei.cangjie.storage.StorageManager

class EnumDescriptorResolver (
    private val typeResolver: TypeResolver,
    private val builtIns: CangJieBuiltIns,
    private val storageManager: StorageManager

    ){
    fun resolveEnumEntryDescriptor(
        c: LazyClassContext,
        thisDescriptor: DeclarationDescriptor,
        name: Name,
        it: CjTypeStatementInfo<*>,
        external: Boolean
    ): LazyEnumEntryDescriptor {
//        TODO 在这里校验还是在 LazyEnumEntryDescriptor的构造函数中校验？
        return LazyEnumEntryDescriptor(c, thisDescriptor, name, it, external)
    }
}
