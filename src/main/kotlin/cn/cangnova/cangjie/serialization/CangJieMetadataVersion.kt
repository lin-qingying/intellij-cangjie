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

package cn.cangnova.cangjie.serialization

import cn.cangnova.cangjie.metadata.deserialization.BinaryVersion

class CangJieMetadataVersion(versionArray: IntArray, val isStrictSemantics: Boolean) : BinaryVersion(*versionArray) {
    constructor(vararg numbers: Int) : this(numbers, isStrictSemantics = false)

    companion object {
        @JvmField
        val INSTANCE = CangJieMetadataVersion(2, 1, 0)

        @JvmField
        val INSTANCE_NEXT = INSTANCE.next()

        @JvmField
        val INVALID_VERSION = CangJieMetadataVersion()
    }
    private fun newerThan(other: CangJieMetadataVersion): Boolean {
        return when {
            major > other.major -> true
            major < other.major -> false
            minor > other.minor -> true
            else -> false
        }
    }
    fun next(): CangJieMetadataVersion =
        if (major == 1 && minor == 9) CangJieMetadataVersion(2, 0, 0)
        else CangJieMetadataVersion(major, minor + 1, 0)

    override fun isCompatibleWithCurrentCompilerVersion(): Boolean {
        return isCompatibleInternal(if (isStrictSemantics) INSTANCE else INSTANCE_NEXT)
    }
    private fun isCompatibleInternal(limitVersion: CangJieMetadataVersion): Boolean {
        // NOTE: 1.0 is a pre-Kotlin-1.0 metadata version, with which the current compiler is incompatible
        if (major == 1 && minor == 0) return false
        // The same for 0.*
        if (major == 0) return false
        // Otherwise we just compare with the given limitVersion
        return !newerThan(limitVersion)
    }

}
