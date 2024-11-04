package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeConstructor

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
