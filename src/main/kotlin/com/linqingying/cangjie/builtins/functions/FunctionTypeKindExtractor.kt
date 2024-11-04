package com.linqingying.cangjie.builtins.functions

import com.linqingying.cangjie.name.FqName

@RequiresOptIn
annotation class AllowedToUsedOnlyInK1

class FunctionTypeKindExtractor(private val kinds: List<FunctionTypeKind>){
    fun getFunctionalClassKind(packageFqName: FqName, className: String): FunctionTypeKind? {
        return getFunctionalClassKindWithArity(packageFqName, className)?.kind
    }
    private val knownKindsByPackageFqName = kinds.groupBy { it.packageFqName }

    fun getFunctionalClassKindWithArity(packageFqName: FqName, className: String): KindWithArity? {
        val kinds = knownKindsByPackageFqName[packageFqName] ?: return null
        for (kind in kinds) {
            if (!className.startsWith(kind.classNamePrefix)) continue
            val arity = toInt(className.substring(kind.classNamePrefix.length)) ?: continue
            return KindWithArity(kind, arity)
        }
        return null
    }
    private fun toInt(s: String): Int? {
        if (s.isEmpty()) return null

        var result = 0
        for (c in s) {
            val d = c - '0'
            if (d !in 0..9) return null
            result = result * 10 + d
        }
        return result
    }
    data class KindWithArity(val kind: FunctionTypeKind, val arity: Int)

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
                FunctionTypeKind.Function,
//                FunctionTypeKind.SuspendFunction,
//                FunctionTypeKind.CFunction,
//                FunctionTypeKind.CSuspendFunction,
            )
        )
    }
}
