package com.huawei.cangjie.resolve.lazy.descriptors

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.lazy.LazyClassContext
import com.huawei.cangjie.resolve.lazy.data.CjTypeStatementInfo

class LazyEnumEntryDescriptor(
    c: LazyClassContext,
    thisDescriptor: DeclarationDescriptor,
    name: Name,
    cjTypeStatementInfo: CjTypeStatementInfo<*>,
    isExternal: Boolean
) : LazyClassDescriptor(c, thisDescriptor, name, cjTypeStatementInfo, isExternal) {

    init {
        checkEntrys()
    }

    fun checkEntrys() {

    }
}
