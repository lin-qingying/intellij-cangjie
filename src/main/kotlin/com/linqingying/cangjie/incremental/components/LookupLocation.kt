package com.linqingying.cangjie.incremental.components

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
    FROM_PACKAGE,
                                             FROM_LIBRARY,
    FROM_IDE,
    FROM_BACKEND,
    FROM_TEST,
    FROM_BUILTINS,
    MATCH_CHECK_DECLARATION_CONFLICTS,
    MATCH_CHECK_OVERRIDES,

    FROM_REFLECTION,
    MATCH_RESOLVE_DECLARATION,
    MATCH_GET_DECLARATION_SCOPE,
    MATCH_RESOLVING_DEFAULT_TYPE_ARGUMENTS,
    FOR_ALREADY_TRACKED,
    // TODO replace with real location (e.g. FROM_IDE) where it possible
    MATCH_GET_ALL_DESCRIPTORS,
    MATCH_TYPING,
    MATCH_GET_SUPER_MEMBERS,
    FOR_NON_TRACKED_SCOPE,
    FROM_SYNTHETIC_SCOPE,
    FROM_DESERIALIZATION,

    MATCH_GET_LOCAL_VARIABLE,
    MATCH_FIND_BY_FQNAME,
    MATCH_GET_COMPANION_OBJECT,
    FOR_DEFAULT_IMPORTS;

    override val location: LocationInfo? get() = null
}
