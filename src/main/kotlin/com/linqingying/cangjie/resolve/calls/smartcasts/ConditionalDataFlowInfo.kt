package com.linqingying.cangjie.resolve.calls.smartcasts


class ConditionalDataFlowInfo(val thenInfo: DataFlowInfo, val elseInfo: DataFlowInfo = thenInfo) {
    fun and(other: ConditionalDataFlowInfo): ConditionalDataFlowInfo = when {
        this == EMPTY -> other
        other == EMPTY -> this
        else -> ConditionalDataFlowInfo(this.thenInfo.and(other.thenInfo), this.elseInfo.and(other.elseInfo))
    }

    fun or(other: ConditionalDataFlowInfo): ConditionalDataFlowInfo = when {
        this == EMPTY -> other
        other == EMPTY -> this
        else -> ConditionalDataFlowInfo(this.thenInfo.or(other.thenInfo), this.elseInfo.or(other.elseInfo))
    }

    companion object {
        val EMPTY = ConditionalDataFlowInfo(DataFlowInfo.EMPTY)
    }
}
