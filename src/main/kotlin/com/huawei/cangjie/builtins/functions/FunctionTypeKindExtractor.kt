package com.huawei.cangjie.builtins.functions

import com.huawei.cangjie.name.FqName

@RequiresOptIn
annotation class AllowedToUsedOnlyInK1

class FunctionTypeKindExtractor(private val kinds: List<FunctionTypeKind>){
//    fun getFunctionalClassKind(packageFqName: FqName, className: String): FunctionTypeKind? {
//        return getFunctionalClassKindWithArity(packageFqName, className)?.kind
//    }
//
//    fun getFunctionalClassKindWithArity(packageFqName: FqName, className: String): KindWithArity? {
//        val kinds = knownKindsByPackageFqName[packageFqName] ?: return null
//        for (kind in kinds) {
//            if (!className.startsWith(kind.classNamePrefix)) continue
//            val arity = toInt(className.substring(kind.classNamePrefix.length)) ?: continue
//            return KindWithArity(kind, arity)
//        }
//        return null
//    }
    companion object {
        /**
         * This instance should be used only in:
         *  - FE 1.0, since it does not support custom functional kinds from plugins
         *  - places in FIR where session is not accessible by design (like in renderer of FIR elements)
         */
        @JvmStatic
        @AllowedToUsedOnlyInK1
        val Default = FunctionTypeKindExtractor(
            listOf(
//                FunctionTypeKind.Function,
//                FunctionTypeKind.SuspendFunction,
//                FunctionTypeKind.CFunction,
//                FunctionTypeKind.CSuspendFunction,
            )
        )
    }
}