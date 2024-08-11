package com.huawei.cangjie.types

import com.huawei.cangjie.types.checker.ErrorTypesAreEqualToAnything

/**
 * This is temporary hack for type intersector.
 *
 * It is almost save, because:
 *  - it running only if general algorithm is failed
 *  - returned type is subtype of all [types].
 *
 * But it is hack, because it can give unstable result, but it better than exception.
 * See KT-11266.
 */
internal fun hackForTypeIntersector(types: Collection<CangJieType>): CangJieType? {
    if (types.size < 2) return types.firstOrNull()

    return types.firstOrNull { candidate ->
        types.all {
            ErrorTypesAreEqualToAnything.isSubtypeOf(candidate, it)
        }
    }
}
