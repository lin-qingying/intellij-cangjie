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

package com.linqingying.cangjie.metadata.decompiler

import com.linqingying.cangjie.metadata.deserialization.BinaryVersion

private const val FILE_ABI_VERSION_MARKER: String = "FILE_ABI"
private const val CURRENT_ABI_VERSION_MARKER: String = "CURRENT_ABI"

const val INCOMPATIBLE_ABI_VERSION_GENERAL_COMMENT: String =
    "// This class file was compiled with different version of CangJie compiler and can't be decompiled."

private const val INCOMPATIBLE_ABI_VERSION_COMMENT: String = "$INCOMPATIBLE_ABI_VERSION_GENERAL_COMMENT\n" +
        "//\n" +
        "// Current compiler ABI version is $CURRENT_ABI_VERSION_MARKER\n" +
        "// File ABI version is $FILE_ABI_VERSION_MARKER"

fun <V : BinaryVersion> createIncompatibleAbiVersionDecompiledText(expectedVersion: V, actualVersion: V): DecompiledText = DecompiledText(
    INCOMPATIBLE_ABI_VERSION_COMMENT.replace(CURRENT_ABI_VERSION_MARKER, expectedVersion.toString())
        .replace(FILE_ABI_VERSION_MARKER, actualVersion.toString())
)
