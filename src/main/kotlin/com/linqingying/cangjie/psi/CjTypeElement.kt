package com.linqingying.cangjie.psi

import com.intellij.util.ArrayFactory

interface CjTypeElement : CjElement {
    val typeArgumentsAsTypes: List<CjTypeReference > get() = emptyList()

    companion object {
        @JvmStatic
        val EMPTY_ARRAY: Array<CjTypeElement?> = arrayOfNulls(0)

        val ARRAY_FACTORY: ArrayFactory<CjTypeElement ?> =
            ArrayFactory { count: Int -> if (count == 0) EMPTY_ARRAY else arrayOfNulls(count) }
    }
}
