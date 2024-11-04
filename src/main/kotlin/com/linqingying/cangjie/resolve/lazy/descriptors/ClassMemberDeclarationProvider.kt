package com.linqingying.cangjie.resolve.lazy.descriptors

import com.linqingying.cangjie.psi.CjParameter
import com.linqingying.cangjie.psi.CjPureTypeStatement
import com.linqingying.cangjie.resolve.lazy.data.CjClassLikeInfo
import com.linqingying.cangjie.resolve.lazy.declarations.DeclarationProvider

interface ClassMemberDeclarationProvider : DeclarationProvider {
    val ownerInfo: CjClassLikeInfo? // is null for synthetic classes/object that don't present in the source code

    val correspondingClassOrObject: CjPureTypeStatement? get() = ownerInfo?.correspondingClass
    val primaryConstructorParameters: List<CjParameter> get() = ownerInfo?.primaryConstructorParameters ?: emptyList()

}
