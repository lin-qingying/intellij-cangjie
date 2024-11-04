package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeCheckerState
import com.linqingying.cangjie.types.TypeConstructor
import com.linqingying.cangjie.types.checker.*
import com.linqingying.cangjie.types.model.*


class OverridingUtilTypeSystemContext(
    val matchingTypeConstructors: Map<TypeConstructor, TypeConstructor>?,
    private val equalityAxioms: CangJieTypeChecker.TypeConstructorEquality,
    private val cangjieTypeRefiner: CangJieTypeRefiner,
    private val cangjieTypePreparator: CangJieTypePreparator,
    private val customSubtype: ((CangJieType, CangJieType) -> Boolean)? = null,
) : ClassicTypeSystemContext {

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        require(c1 is TypeConstructor)
        require(c2 is TypeConstructor)
        return super.areEqualTypeConstructors(c1, c2) || areEqualTypeConstructorsByAxioms(c1, c2)
    }

    override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): TypeCheckerState {
        if (customSubtype == null) {
            return createClassicTypeCheckerState(
                errorTypesEqualToAnything,
                stubTypesEqualToAnything,
                typeSystemContext = this,
                cangjieTypeRefiner = cangjieTypeRefiner,
                cangjieTypePreparator = cangjieTypePreparator,
            )
        }

        return object : TypeCheckerState(
            errorTypesEqualToAnything, stubTypesEqualToAnything, allowedTypeVariable = true,
            typeSystemContext = this,
            cangjieTypePreparator, cangjieTypeRefiner,
        ) {
            override fun customIsSubtypeOf(subType: CangJieTypeMarker, superType: CangJieTypeMarker): Boolean {
                require(subType is CangJieType)
                require(superType is CangJieType)
                return customSubtype.invoke(subType, superType)
            }
        }
    }





    private fun areEqualTypeConstructorsByAxioms(a: TypeConstructor, b: TypeConstructor): Boolean {
        if (equalityAxioms.equals(a, b)) return true
        if (matchingTypeConstructors == null) return false
        val img1 = matchingTypeConstructors[a]
        val img2 = matchingTypeConstructors[b]
        return img1 != null && img1 == b || img2 != null && img2 == a
    }
}
