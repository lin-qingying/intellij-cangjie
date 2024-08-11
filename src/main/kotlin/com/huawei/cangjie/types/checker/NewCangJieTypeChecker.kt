package com.huawei.cangjie.types.checker

import com.huawei.cangjie.resolve.OverridingUtil
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.AbstractNullabilityChecker.hasNotNullSupertype

object SimpleClassicTypeSystemContext : ClassicTypeSystemContext

interface NewCangJieTypeChecker : CangJieTypeChecker {
    val cangjieTypeRefiner: CangJieTypeRefiner
    val cangjieTypePreparator: CangJieTypePreparator
    val overridingUtil: OverridingUtil

    companion object {
        val Default = NewCangJieTypeCheckerImpl(CangJieTypeRefiner.Default)
    }
}

object ErrorTypesAreEqualToAnything : CangJieTypeChecker {
    override fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean =
        NewCangJieTypeChecker.Default.run {
            createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true).isSubtypeOf(
                subtype.unwrap(),
                supertype.unwrap()
            )
        }

    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean =
        NewCangJieTypeChecker.Default.run {
            createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true).equalTypes(a.unwrap(), b.unwrap())
        }
}

class NewCangJieTypeCheckerImpl(
    override val cangjieTypeRefiner: CangJieTypeRefiner,
    override val cangjieTypePreparator: CangJieTypePreparator = CangJieTypePreparator.Default
) : NewCangJieTypeChecker {
    override val overridingUtil: OverridingUtil = OverridingUtil.createWithTypeRefiner(cangjieTypeRefiner)

    override fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean = true
//        createClassicTypeCheckerState(
//            true, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
//        ).isSubtypeOf(subtype.unwrap(), supertype.unwrap()) // todo fix flag errorTypeEqualsToAnything

    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean =
        createClassicTypeCheckerState(
            false, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
        ).equalTypes(a.unwrap(), b.unwrap())

    fun TypeCheckerState.equalTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractTypeChecker.equalTypes(this, a, b)
    }

    fun TypeCheckerState.isSubtypeOf(subType: UnwrappedType, superType: UnwrappedType): Boolean {
        return AbstractTypeChecker.isSubtypeOf(this, subType, superType)
    }
}

object NullabilityChecker {
    fun isSubtypeOfAny(type: UnwrappedType): Boolean =
        SimpleClassicTypeSystemContext
            .newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
            .hasNotNullSupertype(type.lowerIfFlexible(), TypeCheckerState.SupertypesPolicy.LowerIfFlexible)
}
