package com.huawei.cangjie.analyzer.api.types

import com.huawei.cangjie.analyzer.lifetime.CjLifetimeOwner
import com.huawei.cangjie.psi.CjAnnotated


public sealed interface CjType : CjLifetimeOwner, CjAnnotated {
    public val nullability: CjTypeNullability
    public fun asStringForDebugging(): String
}
public enum class CjTypeNullability(public val isNullable: Boolean) {
    NULLABLE(true),
    NON_NULLABLE(false),
    UNKNOWN(false);

    public companion object {
        public fun create(isNullable: Boolean): CjTypeNullability = if (isNullable) NULLABLE else NON_NULLABLE
    }
}
