package com.huawei.cangjie.types.checker

import com.huawei.cangjie.resolve.OverridingUtil
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.model.SimpleTypeMarker

object SimpleClassicTypeSystemContext : ClassicTypeSystemContext

interface NewCangJieTypeChecker : CangJieTypeChecker {
    val cangjieTypeRefiner: CangJieTypeRefiner
    val cangjieTypePreparator: CangJieTypePreparator
    val overridingUtil: OverridingUtil

    companion object {
        val Default = NewCangJieTypeCheckerImpl(CangJieTypeRefiner.Default)
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

    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean = true
//        createClassicTypeCheckerState(
//            false, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
//        ).equalTypes(a.unwrap(), b.unwrap())

//    fun TypeCheckerState.equalTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
//        return AbstractTypeChecker.equalTypes(this, a, b)
//    }
//
//    fun TypeCheckerState.isSubtypeOf(subType: UnwrappedType, superType: UnwrappedType): Boolean {
//        return AbstractTypeChecker.isSubtypeOf(this, subType, superType)
//    }
}