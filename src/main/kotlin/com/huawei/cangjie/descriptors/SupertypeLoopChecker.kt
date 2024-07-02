package com.huawei.cangjie.descriptors

import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructor

interface SupertypeLoopChecker {
    fun findLoopsInSupertypesAndDisconnect(
        currentTypeConstructor: TypeConstructor,
        superTypes: Collection<CangJieType>,
        neighbors: (TypeConstructor) -> Iterable<CangJieType>,
        reportLoop: (CangJieType) -> Unit
    ): Collection<CangJieType>

    object EMPTY : SupertypeLoopChecker {
        override fun findLoopsInSupertypesAndDisconnect(
            currentTypeConstructor: TypeConstructor,
            superTypes: Collection<CangJieType>,
            neighbors: (TypeConstructor) -> Iterable<CangJieType>,
            reportLoop: (CangJieType) -> Unit
        ): Collection<CangJieType> = superTypes
    }
}