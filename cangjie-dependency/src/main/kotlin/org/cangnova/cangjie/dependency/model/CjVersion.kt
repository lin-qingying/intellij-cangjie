/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.dependency.model

/**
 * 版本模型
 *
 * 表示依赖包的版本信息
 */
data class CjVersion(
    /**
     * 版本字符串
     */
    val versionString: String
) : Comparable<CjVersion> {

    /**
     * 主版本号
     */
    val major: Int

    /**
     * 次版本号
     */
    val minor: Int

    /**
     * 修订版本号
     */
    val patch: Int

    /**
     * 预发布标识 (如 alpha, beta, rc)
     */
    val preRelease: String?

    /**
     * 构建元数据
     */
    val buildMetadata: String?

    init {
        val parts = parseVersion(versionString)
        major = parts.major
        minor = parts.minor
        patch = parts.patch
        preRelease = parts.preRelease
        buildMetadata = parts.buildMetadata
    }

    override fun compareTo(other: CjVersion): Int {
        // 比较主版本号
        var result = major.compareTo(other.major)
        if (result != 0) return result

        // 比较次版本号
        result = minor.compareTo(other.minor)
        if (result != 0) return result

        // 比较修订版本号
        result = patch.compareTo(other.patch)
        if (result != 0) return result

        // 比较预发布版本
        return comparePreRelease(preRelease, other.preRelease)
    }

    override fun toString(): String = versionString

    companion object {
        /**
         * 解析版本字符串
         */
        private fun parseVersion(version: String): VersionParts {
            // 简化的版本解析，支持 major.minor.patch[-preRelease][+buildMetadata]
            val regex = Regex("""^(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-([^+]+))?(?:\+(.+))?${'$'}""")
            val match = regex.matchEntire(version)
                ?: return VersionParts(0, 0, 0, null, null)

            val (majorStr, minorStr, patchStr, preReleaseStr, buildMetadataStr) = match.destructured

            return VersionParts(
                major = majorStr.toIntOrNull() ?: 0,
                minor = minorStr.toIntOrNull() ?: 0,
                patch = patchStr.toIntOrNull() ?: 0,
                preRelease = preReleaseStr.takeIf { it.isNotEmpty() },
                buildMetadata = buildMetadataStr.takeIf { it.isNotEmpty() }
            )
        }

        /**
         * 比较预发布版本
         */
        private fun comparePreRelease(a: String?, b: String?): Int {
            return when {
                a == null && b == null -> 0
                a == null -> 1  // 正式版本大于预发布版本
                b == null -> -1 // 预发布版本小于正式版本
                else -> a.compareTo(b)
            }
        }
    }

    private data class VersionParts(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preRelease: String?,
        val buildMetadata: String?
    )
}