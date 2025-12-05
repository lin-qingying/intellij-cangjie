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

package org.cangnova.cangjie.model

/**
 * 版本要求
 */
sealed class VersionRequirement {
    /**
     * 精确版本：=1.0.0
     */
    data class Exact(val version: CjVersion) : VersionRequirement()

    /**
     * Caret 版本：^1.0.0
     * 最常用，符合语义化版本
     */
    data class Caret(val version: CjVersion) : VersionRequirement()

    /**
     * Tilde 版本：~1.0.0
     * 补丁级别兼容
     */
    data class Tilde(val version: CjVersion) : VersionRequirement()

    /**
     * 范围版本：>=1.0, <2.0
     */
    data class Range(
        val min: CjVersion?,
        val minInclusive: Boolean = true,
        val max: CjVersion?,
        val maxInclusive: Boolean = false
    ) : VersionRequirement()

    /**
     * 通配符：1.*
     */
    data class Wildcard(val major: Int, val minor: Int?) : VersionRequirement()

    /**
     * 版本匹配算法
     */
    fun matches(version: CjVersion): Boolean = when (this) {
        is Exact -> version.versionString == this.version.versionString
        is Caret -> matchesCaret(version)
        is Tilde -> matchesTilde(version)
        is Range -> matchesRange(version)
        is Wildcard -> matchesWildcard(version)
    }

    private fun matchesCaret(version: CjVersion): Boolean {
        val caretVersion = (this as Caret).version
        return when {
            caretVersion.major == 0 -> {
                when {
                    caretVersion.minor == 0 -> {
                        // ^0.0.3 → >= 0.0.3, < 0.0.4
                        version.major == 0 && version.minor == 0 && version.patch == caretVersion.patch
                    }
                    else -> {
                        // ^0.2.3 → >= 0.2.3, < 0.3.0
                        version.major == 0 && version.minor == caretVersion.minor && version.patch >= caretVersion.patch
                    }
                }
            }
            else -> {
                // ^1.2.3 → >= 1.2.3, < 2.0.0
                version.major == caretVersion.major &&
                        (version.minor > caretVersion.minor ||
                                (version.minor == caretVersion.minor && version.patch >= caretVersion.patch))
            }
        }
    }

    private fun matchesTilde(version: CjVersion): Boolean {
        val tildeVersion = (this as Tilde).version
        // ~1.2.3 → >= 1.2.3, < 1.3.0
        return version.major == tildeVersion.major &&
                version.minor == tildeVersion.minor &&
                version.patch >= tildeVersion.patch
    }

    private fun matchesRange(version: CjVersion): Boolean {
        val range = this as Range
        val minMatch = range.min?.let {
            if (range.minInclusive) {
                version.compareTo(it) >= 0
            } else {
                version.compareTo(it) > 0
            }
        } ?: true

        val maxMatch = range.max?.let {
            if (range.maxInclusive) {
                version.compareTo(it) <= 0
            } else {
                version.compareTo(it) < 0
            }
        } ?: true

        return minMatch && maxMatch
    }

    private fun matchesWildcard(version: CjVersion): Boolean {
        val wildcard = this as Wildcard
        return version.major == wildcard.major &&
                (wildcard.minor == null || version.minor == wildcard.minor)
    }

    companion object {
        /**
         * 从字符串解析版本要求
         * 支持：^1.0.0, ~1.0.0, =1.0.0, >=1.0,<2.0, 1.*
         */
        fun parse(requirement: String): VersionRequirement {
            return when {
                requirement.startsWith("^") -> Caret(CjVersion.parse(requirement.substring(1)))
                requirement.startsWith("~") -> Tilde(CjVersion.parse(requirement.substring(1)))
                requirement.startsWith("=") -> Exact(CjVersion.parse(requirement.substring(1)))
                requirement.contains(">=") || requirement.contains("<") -> parseRange(requirement)
                requirement.contains("*") -> parseWildcard(requirement)
                else -> Caret(CjVersion.parse(requirement))  // 默认 Caret
            }
        }

        private fun parseRange(requirement: String): Range {
            // 简化实现，实际应该使用正则表达式
            // TODO: 完整实现范围解析
            val parts = requirement.split(",")
            var min: CjVersion? = null
            var minInclusive = true
            var max: CjVersion? = null
            var maxInclusive = false

            for (part in parts) {
                val trimmed = part.trim()
                when {
                    trimmed.startsWith(">=") -> {
                        min = CjVersion.parse(trimmed.substring(2).trim())
                        minInclusive = true
                    }
                    trimmed.startsWith(">") -> {
                        min = CjVersion.parse(trimmed.substring(1).trim())
                        minInclusive = false
                    }
                    trimmed.startsWith("<=") -> {
                        max = CjVersion.parse(trimmed.substring(2).trim())
                        maxInclusive = true
                    }
                    trimmed.startsWith("<") -> {
                        max = CjVersion.parse(trimmed.substring(1).trim())
                        maxInclusive = false
                    }
                }
            }

            return Range(min, minInclusive, max, maxInclusive)
        }

        private fun parseWildcard(requirement: String): Wildcard {
            // 简化实现：1.* 或 1.2.*
            val parts = requirement.split(".")
            return when (parts.size) {
                2 -> Wildcard(parts[0].toInt(), null)
                3 -> Wildcard(parts[0].toInt(), parts[1].toInt())
                else -> throw IllegalArgumentException("Invalid wildcard version: $requirement")
            }
        }
    }
}
/**
 * 版本模型
 *
 * 表示依赖包的版本信息
 */
data class CjVersion(
    /**
     * 版本字符串
     */
    val versionString: String? = null
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
        val parts = parseVersion(versionString ?: "0.0.0")
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

    override fun toString(): String = versionString ?: ""

    companion object {
        /**
         * 从字符串解析版本
         */
        fun parse(versionString: String): CjVersion {
            return CjVersion(versionString)
        }

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