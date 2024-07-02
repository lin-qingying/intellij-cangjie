package com.huawei.cangjie.incremental.components

import java.io.Serializable

interface LookupLocation {
    val location: LocationInfo?
}

interface LocationInfo {
    val filePath: String

    // only for tests
    val position: Position
}


data class Position(val line: Int, val column: Int) : Serializable {
    companion object {
        val NO_POSITION = Position(-1, -1)
    }
}


enum class NoLookupLocation : LookupLocation {
    FROM_IDE,
    FROM_BACKEND,
    FROM_TEST,
    FROM_BUILTINS,
    WHEN_CHECK_DECLARATION_CONFLICTS,
    WHEN_CHECK_OVERRIDES,
    FOR_SCRIPT,
    FROM_REFLECTION,
    WHEN_RESOLVE_DECLARATION,
    WHEN_GET_DECLARATION_SCOPE,
    WHEN_RESOLVING_DEFAULT_TYPE_ARGUMENTS,
    FOR_ALREADY_TRACKED,
    // TODO replace with real location (e.g. FROM_IDE) where it possible
    WHEN_GET_ALL_DESCRIPTORS,
    WHEN_TYPING,
    WHEN_GET_SUPER_MEMBERS,
    FOR_NON_TRACKED_SCOPE,
    FROM_SYNTHETIC_SCOPE,
    FROM_DESERIALIZATION,
    FROM_JAVA_LOADER,
    WHEN_GET_LOCAL_VARIABLE,
    WHEN_FIND_BY_FQNAME,
    WHEN_GET_COMPANION_OBJECT,
    FOR_DEFAULT_IMPORTS;

    override val location: LocationInfo? get() = null
}
