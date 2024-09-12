package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.diagnostics.rendering.DiagnosticRenderer

abstract class DiagnosticFactory<D : UnboundDiagnostic> protected constructor(
    private var _name: String?,
    open val severity: Severity
){
    open val name: String
        get() = _name ?: "<unnamed>"
    fun initializeName(name: String) {
        _name = name
    }
    open var defaultRenderer: DiagnosticRenderer<D>? = null

    protected constructor(severity: Severity) : this(null, severity)

    @Suppress("UNCHECKED_CAST")
    fun initDefaultRenderer(defaultRenderer: DiagnosticRenderer<*>?) {
        this.defaultRenderer = defaultRenderer as DiagnosticRenderer<D>?
    }
    fun cast(diagnostic: UnboundDiagnostic): D {
        require(diagnostic.factory === this) { "Factory mismatch: expected " + this + " but was " + diagnostic.factory }
        @Suppress("UNCHECKED_CAST")
        return diagnostic as D
    }
    override fun toString(): String {
        return _name ?: "<Anonymous DiagnosticFactory>"
    }
    companion object {
        @SafeVarargs
        fun <D : UnboundDiagnostic> cast(diagnostic: UnboundDiagnostic, vararg factories: DiagnosticFactory<out D>): D {
            return cast(diagnostic, listOf(*factories))
        }

        fun <D : UnboundDiagnostic> cast(diagnostic: UnboundDiagnostic, factories: Collection<DiagnosticFactory<out D>>): D {
            for (factory in factories) {
                if (diagnostic.factory === factory) return factory.cast(diagnostic)
            }
            throw IllegalArgumentException("Factory mismatch: expected one of " + factories + " but was " + diagnostic.factory)
        }
    }
}
