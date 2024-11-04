package com.linqingying.cangjie.resolve.calls.smartcasts

import com.linqingying.cangjie.psi.Call
import com.linqingying.cangjie.types.CangJieType

interface ExplicitSmartCasts {
    fun type(call: Call?): CangJieType?

    val defaultType: CangJieType?

    operator fun plus(smartCast: SingleSmartCast): ExplicitSmartCasts
}
data class SingleSmartCast(val call: Call?, val type: CangJieType) : ExplicitSmartCasts {
    override fun type(call: Call?) = if (call == this.call) type else null

    override val defaultType: CangJieType get() = type

    override fun plus(smartCast: SingleSmartCast) =
        if (this == smartCast) this
        else MultipleSmartCasts(mapOf(call to type, smartCast.call to smartCast.type))
}
data class MultipleSmartCasts internal constructor(val map: Map<Call?, CangJieType>) : ExplicitSmartCasts {
    override fun type(call: Call?) = map[call]

    override val defaultType: CangJieType? get() = null

    override fun plus(smartCast: SingleSmartCast) = MultipleSmartCasts(map + mapOf(smartCast.call to smartCast.type))
}
