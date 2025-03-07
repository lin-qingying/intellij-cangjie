/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.incremental.components

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
