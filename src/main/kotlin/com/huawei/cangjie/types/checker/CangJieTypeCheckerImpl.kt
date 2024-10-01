package com.huawei.cangjie.types.checker

import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructor

class CangJieTypeCheckerImpl protected constructor(procedure: TypeCheckingProcedure) :
    CangJieTypeChecker {
    private val procedure: TypeCheckingProcedure = procedure

    override fun isSubtypeOf(
        subtype:  CangJieType,
        supertype:  CangJieType
    ): Boolean {
        return procedure.isSubtypeOf(subtype, supertype)
    }

    override fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean {

        return procedure.equalsIgnoringGenerics(a, b)

    }
    override fun equalTypes(
        a:  CangJieType,
        b: CangJieType
    ): Boolean {
        return procedure.equalTypes(a, b)
    }

    companion object {
        fun withAxioms(equalityAxioms: CangJieTypeChecker.TypeConstructorEquality): CangJieTypeChecker {
            return CangJieTypeCheckerImpl(  TypeCheckingProcedure(object :
                TypeCheckerProcedureCallbacksImpl() {
                override fun assertEqualTypeConstructors(
                    constructor1: TypeConstructor,
                    constructor2:  TypeConstructor
                ): Boolean {
                    return constructor1 == constructor2 || equalityAxioms.equals(constructor1, constructor2)
                }
            }))
        }
    }
}
