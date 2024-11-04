package com.linqingying.cangjie.incremental.components

import com.linqingying.cangjie.container.DefaultImplementation
import java.io.Serializable


@DefaultImplementation(LookupTracker.DO_NOTHING::class)
interface LookupTracker {
    // used in tests for more accurate checks
    val requiresPosition: Boolean

    fun record(
        filePath: String,
        position: Position,
        scopeFqName: String,
        scopeKind: ScopeKind,
        name: String
    )

    fun clear()

    object DO_NOTHING : LookupTracker {
        override val requiresPosition: Boolean
            get() = false

        override fun record(filePath: String, position: Position, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        }

        override fun clear() {
        }
    }
}

enum class ScopeKind {
    PACKAGE,
    CLASSIFIER
}

data class LookupInfo(
    val filePath: String,
    val position: Position,
    val scopeFqName: String,
    val scopeKind: ScopeKind,
    val name: String
) : Serializable
