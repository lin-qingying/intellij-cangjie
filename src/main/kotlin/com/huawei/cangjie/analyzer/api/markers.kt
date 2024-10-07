package com.huawei.cangjie.analyzer.api

import com.huawei.cangjie.name.Name

interface CjSymbolWithTypeParameters : CjSymbol {
    val typeParameters: List<CjTypeParameterSymbol>
}

interface CjPossiblyNamedSymbol : CjSymbol {
    val name: Name?
}

interface CjNamedSymbol : CjPossiblyNamedSymbol {
    override val name: Name
}

/**
 * A marker interface for symbols which could potentially be `expect` or `actual`. For more details about `expect` and `actual`
 * declarations, see [documentation](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html).
 */
interface CjPossibleMultiplatformSymbol : CjSymbol {
    /**
     * Returns true if the declaration is a platform-specific implementation in a multiplatform project.
     */
    val isActual: Boolean

    /**
     * Returns true if the declaration is platform-specific declaration in a multiplatform project. An implementation
     * in platform modules is expected. Note, that in the following example:
     * ```
     * expect class A {
     *     class Nested
     * }
     * ```
     * `isExpect` returns `true` for both `A` and `A.Nested`.
     */
    val isExpect: Boolean
}
