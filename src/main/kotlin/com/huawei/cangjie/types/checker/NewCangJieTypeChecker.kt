package com.huawei.cangjie.types.checker

import com.huawei.cangjie.resolve.OverridingUtil
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.AbstractNullabilityChecker.hasNotNullSupertype

object SimpleClassicTypeSystemContext : ClassicTypeSystemContext

fun UnwrappedType.hasSupertypeWithGivenTypeConstructor(typeConstructor: TypeConstructor) =
    createClassicTypeCheckerState(isErrorTypeEqualsToAnything = false).anySupertype(lowerIfFlexible(), {
        require(it is SimpleType)
        it.constructor == typeConstructor
    }, { TypeCheckerState.SupertypesPolicy.LowerIfFlexible })

interface NewCangJieTypeChecker : CangJieTypeChecker {
    val cangjieTypeRefiner: CangJieTypeRefiner
    val cangjieTypePreparator: CangJieTypePreparator
    val overridingUtil: OverridingUtil

    companion object {
        val Default = NewCangJieTypeCheckerImpl(CangJieTypeRefiner.Default)
    }
}
object StrictEqualityTypeChecker {

    /**
     * String! != String & A<String!> != A<String>, also A<in Nothing> != A<out Any?>
     * also A<*> != A<out Any?>
     * different error types non-equals even errorTypeEqualToAnything
     */
    fun strictEqualTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractStrictEqualityTypeChecker.strictEqualTypes(SimpleClassicTypeSystemContext, a, b)
    }

    fun strictEqualTypes(a: SimpleType, b: SimpleType): Boolean {
        return AbstractStrictEqualityTypeChecker.strictEqualTypes(SimpleClassicTypeSystemContext, a, b)
    }

}
object ErrorTypesAreEqualToAnything : CangJieTypeChecker {
    override fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean =
        NewCangJieTypeChecker.Default.run {
            createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true).equalsIgnoringGenerics(
                a.unwrap(),
                b.unwrap()
            )
        }

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
    override fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean =
        createClassicTypeCheckerState(
            false, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
        ).equalsIgnoringGenerics(a.unwrap(), b.unwrap())

    override fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean =
        createClassicTypeCheckerState(
            true, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
        ).isSubtypeOf(subtype.unwrap(), supertype.unwrap()) // todo fix flag errorTypeEqualsToAnything

    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean =
        createClassicTypeCheckerState(
            false, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
        ).equalTypes(a.unwrap(), b.unwrap())

    fun TypeCheckerState.equalTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractTypeChecker.equalTypes(this, a, b)
    }

    fun TypeCheckerState.equalsIgnoringGenerics(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractTypeChecker.equalsIgnoringGenerics(this, a, b)
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
