package com.huawei.cangjie.resolve.lazy.descriptors

import com.huawei.cangjie.psi.CjParameter
import com.huawei.cangjie.psi.CjPureTypeStatement
import com.huawei.cangjie.resolve.lazy.data.CjClassLikeInfo
import com.huawei.cangjie.resolve.lazy.declarations.DeclarationProvider

interface ClassMemberDeclarationProvider : DeclarationProvider {
    val ownerInfo: CjClassLikeInfo? // is null for synthetic classes/object that don't present in the source code

    val correspondingClassOrObject: CjPureTypeStatement? get() = ownerInfo?.correspondingClass
    val primaryConstructorParameters: List<CjParameter> get() = ownerInfo?.primaryConstructorParameters ?: emptyList()

}
