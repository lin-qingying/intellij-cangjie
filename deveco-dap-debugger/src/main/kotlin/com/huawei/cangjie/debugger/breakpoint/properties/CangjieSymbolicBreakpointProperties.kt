package com.huawei.cangjie.debugger.breakpoint.properties

import java.util.*

data class CangjieSymbolicBreakpointProperties(
    var symbolName: String? = null
) : CangjieBreakpointFiltersProperties<CangjieSymbolicBreakpointProperties>() {

    override fun getState(): CangjieSymbolicBreakpointProperties = this

    override fun loadState(state: CangjieSymbolicBreakpointProperties) {
        super.loadState(state)
        this.symbolName = state.symbolName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CangjieSymbolicBreakpointProperties) return false
        if (!super.equals(other)) return false

        return symbolName == other.symbolName
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), symbolName)

    }
}
