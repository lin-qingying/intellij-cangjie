package com.linqingying.cangjie.debugger.breakpoint.properties

import com.intellij.xdebugger.breakpoints.XBreakpointProperties

import com.huawei.bitfun.utils.CodeCheckByPassUtils
import com.intellij.util.xmlb.annotations.OptionTag

import java.util.Objects

open class CangjieBreakpointFiltersProperties<T : CangjieBreakpointFiltersProperties<T>> : XBreakpointProperties<T>() {
    var isHitCountEnabled: Boolean = false
    var hitCount: String? = null

    override fun getState(): T {
        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    override fun loadState(state: T) {
        this.isHitCountEnabled = state.isHitCountEnabled
        this.hitCount = state.hitCount
    }

    @OptionTag("hit-count-filter-enabled")
    fun setHitCountEnabled(isHitCountEnabled: Boolean): Boolean {
        val isChanged = this.isHitCountEnabled != isHitCountEnabled
        this.isHitCountEnabled = isHitCountEnabled
        return isChanged
    }

    @OptionTag("hit-count-filter")
    fun setHitCount(hitCount: String?): Boolean {
        val isChanged = this.hitCount != hitCount
        this.hitCount = hitCount
        return isChanged
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CangjieBreakpointFiltersProperties<*>) return false
        if (!super.equals(other)) return false

        return isHitCountEnabled == other.isHitCountEnabled && hitCount == other.hitCount
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), isHitCountEnabled, hitCount)
    }

    override fun toString(): String {
        return "CangjieBreakpointFiltersProperties(isHitCountEnabled=$isHitCountEnabled, hitCount=$hitCount)"
    }
}
