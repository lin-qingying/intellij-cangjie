package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.name.FqName

abstract class Visibility protected constructor(
    val name: String,
    val isPublicAPI: Boolean
) {

    open val internalDisplayName: String
        get() = name

    open val externalDisplayName: String
        get() = internalDisplayName

    abstract fun mustCheckInImports(): Boolean

    open fun compareTo(visibility: Visibility): Int? {
        return Visibilities.compareLocal(this, visibility)
    }

    final override fun toString() = internalDisplayName

    open fun normalize(): Visibility = this

    // Should be overloaded in Java visibilities
    open fun customEffectiveVisibility(): EffectiveVisibility? = null

    open fun visibleFromPackage(fromPackage: FqName, myPackage: FqName): Boolean = true
}
