/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.project.model

/**
 * 版本要求
 *
 * 支持多种版本约束格式：
 * - 精确版本: =1.0.0
 * - Caret 版本: ^1.0.0 (语义化版本兼容)
 * - Tilde 版本: ~1.0.0 (补丁级别兼容)
 * - 范围版本: >=1.0.0, <2.0.0
 * - 通配符: 1.* 或 1.2.*
 */
sealed class VersionRequirement {
    /**
     * 精确版本：=1.0.0
     * 只匹配指定的确切版本
     */
    data class Exact(val version: CjVersion) : VersionRequirement() {
        override fun toString(): String {
            return super.toString()
        }
    }

    /**
     * Caret 版本：^1.0.0
     * 最常用，符合语义化版本规范
     * - ^1.2.3 → >=1.2.3, <2.0.0
     * - ^0.2.3 → >=0.2.3, <0.3.0
     * - ^0.0.3 → >=0.0.3, <0.0.4
     */
    data class Caret(val version: CjVersion) : VersionRequirement() {
        override fun toString(): String {
            return super.toString()
        }
    }

    /**
     * Tilde 版本：~1.0.0
     * 补丁级别兼容
     * - ~1.2.3 → >=1.2.3, <1.3.0
     */
    data class Tilde(val version: CjVersion) : VersionRequirement() {
        override fun toString(): String {
            return super.toString()
        }
    }

    /**
     * 范围版本：>=1.0.0, <2.0.0
     * 支持灵活的版本范围定义
     */
    data class Range(
        val min: CjVersion?,
        val minInclusive: Boolean = true,
        val max: CjVersion?,
        val maxInclusive: Boolean = false
    ) : VersionRequirement() {
        override fun toString(): String {
            return super.toString()
        }

        init {
            require(min != null || max != null) {
                "Range must have at least one bound (min or max)"
            }
        }
    }

    /**
     * 通配符：1.* 或 1.2.*
     * 匹配指定前缀的所有版本
     */
    data class Wildcard(val major: Int, val minor: Int? = null) : VersionRequirement() {
        init {
            require(major >= 0) { "Major version must be non-negative" }
            require(minor == null || minor >= 0) { "Minor version must be non-negative" }
        }

        override fun toString(): String {
            return super.toString()
        }
    }

    /**
     * 转换为字符串表示
     */
    override fun toString(): String {
        return when (this) {
            is Exact -> version.parsedVersion
            is Caret -> "^${version.parsedVersion}"
            is Tilde -> "~${version.parsedVersion}"
            is Range -> buildRangeString()
            is Wildcard -> if (minor == null) "$major.*" else "$major.$minor.*"
        }
    }

    /**
     * 版本匹配算法
     * @param version 待匹配的版本
     * @return true 如果版本满足要求
     */
    fun matches(version: CjVersion): Boolean = when (this) {
        is Exact -> version.versionString == this.version.versionString
        is Caret -> matchesCaret(version)
        is Tilde -> matchesTilde(version)
        is Range -> matchesRange(version)
        is Wildcard -> matchesWildcard(version)
    }

    /**
     * Caret 版本匹配规则
     */
    private fun matchesCaret(version: CjVersion): Boolean {
        val caretVersion = (this as Caret).version
        return when {
            caretVersion.major == 0 -> {
                when {
                    caretVersion.minor == 0 -> {
                        // ^0.0.3 → >= 0.0.3, < 0.0.4
                        version.major == 0 &&
                                version.minor == 0 &&
                                version.patch == caretVersion.patch
                    }
                    else -> {
                        // ^0.2.3 → >= 0.2.3, < 0.3.0
                        version.major == 0 &&
                                version.minor == caretVersion.minor &&
                                version.patch >= caretVersion.patch
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

    /**
     * Tilde 版本匹配规则
     */
    private fun matchesTilde(version: CjVersion): Boolean {
        val tildeVersion = (this as Tilde).version
        // ~1.2.3 → >= 1.2.3, < 1.3.0
        return version.major == tildeVersion.major &&
                version.minor == tildeVersion.minor &&
                version.patch >= tildeVersion.patch
    }

    /**
     * 范围版本匹配规则
     */
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

    /**
     * 通配符版本匹配规则
     */
    private fun matchesWildcard(version: CjVersion): Boolean {
        val wildcard = this as Wildcard
        return version.major == wildcard.major &&
                (wildcard.minor == null || version.minor == wildcard.minor)
    }


    /**
     * 构建范围版本的字符串表示
     */
    private fun Range.buildRangeString(): String {
        val parts = mutableListOf<String>()

        min?.let {
            parts.add(if (minInclusive) ">=${it.parsedVersion}" else ">${it.parsedVersion}")
        }

        max?.let {
            parts.add(if (maxInclusive) "<=${it.parsedVersion}" else "<${it.parsedVersion}")
        }

        return parts.joinToString(", ")
    }

    companion object {
        /**
         * 从字符串解析版本要求
         *
         * 支持格式：
         * - ^1.0.0 (Caret)
         * - ~1.0.0 (Tilde)
         * - =1.0.0 (Exact)
         * - >=1.0.0, <2.0.0 (Range)
         * - 1.* 或 1.2.* (Wildcard)
         * - 1.0.0 (默认为 Caret)
         *
         * @param requirement 版本要求字符串
         * @return 解析后的版本要求对象
         * @throws IllegalArgumentException 如果格式无效
         */
        fun parse(requirement: String): VersionRequirement {
            val trimmed = requirement.trim()
            require(trimmed.isNotEmpty()) { "Version requirement cannot be empty" }

            return when {
                trimmed.startsWith("^") -> {
                    Caret(CjVersion.parse(trimmed.substring(1)))
                }

                trimmed.startsWith("~") -> {
                    Tilde(CjVersion.parse(trimmed.substring(1)))
                }

                trimmed.startsWith("=") -> {
                    Exact(CjVersion.parse(trimmed.substring(1)))
                }

                trimmed.contains(">=") || trimmed.contains("<=") ||
                        trimmed.contains(">") || trimmed.contains("<") -> {
                    parseRange(trimmed)
                }

                trimmed.contains("*") -> {
                    parseWildcard(trimmed)
                }

                else -> {
                    // 默认使用 Caret 语义
                    Caret(CjVersion.parse(trimmed))
                }
            }
        }

        /**
         * 解析范围版本
         * 支持格式：>=1.0.0, <2.0.0 或 >1.0.0, <=2.0.0
         */
        private fun parseRange(requirement: String): Range {
            val parts = requirement.split(",").map { it.trim() }
            var min: CjVersion? = null
            var minInclusive = true
            var max: CjVersion? = null
            var maxInclusive = false

            for (part in parts) {
                when {
                    part.startsWith(">=") -> {
                        min = CjVersion.parse(part.substring(2).trim())
                        minInclusive = true
                    }

                    part.startsWith(">") -> {
                        min = CjVersion.parse(part.substring(1).trim())
                        minInclusive = false
                    }

                    part.startsWith("<=") -> {
                        max = CjVersion.parse(part.substring(2).trim())
                        maxInclusive = true
                    }

                    part.startsWith("<") -> {
                        max = CjVersion.parse(part.substring(1).trim())
                        maxInclusive = false
                    }

                    else -> {
                        throw IllegalArgumentException("Invalid range part: $part")
                    }
                }
            }

            return Range(min, minInclusive, max, maxInclusive)
        }

        /**
         * 解析通配符版本
         * 支持格式：1.* 或 1.2.*
         */
        private fun parseWildcard(requirement: String): Wildcard {
            val parts = requirement.split(".")

            return when (parts.size) {
                2 -> {
                    require(parts[1] == "*") { "Invalid wildcard format: $requirement" }
                    Wildcard(parts[0].toInt(), null)
                }

                3 -> {
                    require(parts[2] == "*") { "Invalid wildcard format: $requirement" }
                    Wildcard(parts[0].toInt(), parts[1].toInt())
                }

                else -> {
                    throw IllegalArgumentException("Invalid wildcard version: $requirement")
                }
            }
        }
    }
}

/**
 * 版本模型
 *
 * 表示符合语义化版本规范 (Semantic Versioning 2.0.0) 的版本号
 * 格式：major.minor.patch[-preRelease][+buildMetadata]
 */
data class CjVersion(
    /**
     * 原始版本字符串
     */
    val versionString: String? = null
) : Comparable<CjVersion> {

    /**
     * 主版本号（不兼容的 API 修改）
     */
    val major: Int

    /**
     * 次版本号（向下兼容的功能性新增）
     */
    val minor: Int

    /**
     * 修订版本号（向下兼容的问题修正）
     */
    val patch: Int

    /**
     * 预发布标识 (如 alpha, beta, rc.1)
     */
    val preRelease: String?

    /**
     * 构建元数据 (如 build.123, 20130313144700)
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

    /**
     * 获取格式化的版本字符串（不含预发布和构建元数据）
     */
    val parsedVersion: String
        get() = "$major.$minor.$patch"

    /**
     * 获取完整的版本字符串（含预发布和构建元数据）
     */
    val fullVersion: String
        get() = buildString {
            append(parsedVersion)
            preRelease?.let { append("-$it") }
            buildMetadata?.let { append("+$it") }
        }

    /**
     * 是否为预发布版本
     */
    val isPreRelease: Boolean
        get() = preRelease != null

    /**
     * 是否为稳定版本
     */
    val isStable: Boolean
        get() = major > 0 && !isPreRelease

    /**
     * 比较版本大小
     * 按照语义化版本规范进行比较
     */
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

    override fun toString(): String = versionString ?: fullVersion

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CjVersion) return false
        return compareTo(other) == 0
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + (preRelease?.hashCode() ?: 0)
        return result
    }

    companion object {
        val EMPTY = CjVersion("")
        val ZERO = CjVersion("0.0.0")

        /**
         * 从字符串解析版本
         * @param versionString 版本字符串
         * @return 解析后的版本对象
         * @throws IllegalArgumentException 如果格式无效
         */
        fun parse(versionString: String): CjVersion {
            return CjVersion(versionString)
        }

        /**
         * 尝试解析版本字符串，失败返回 null
         */
        fun parseOrNull(versionString: String?): CjVersion? {
            return try {
                versionString?.let { parse(it) }
            } catch (e: Exception) {
                null
            }
        }

        /**
         * 解析版本字符串
         * 支持格式：major.minor.patch[-preRelease][+buildMetadata]
         */
        private fun parseVersion(version: String): VersionParts {
            // 语义化版本正则表达式
            val regex = Regex(
                """^(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-([0-9A-Za-z\-\.]+))?(?:\+([0-9A-Za-z\-\.]+))?$"""
            )

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
         * 根据语义化版本规范：
         * 1. 正式版本优先级高于预发布版本
         * 2. 预发布版本按字典序比较
         */
        private fun comparePreRelease(a: String?, b: String?): Int {
            return when {
                a == null && b == null -> 0
                a == null -> 1  // 正式版本大于预发布版本
                b == null -> -1 // 预发布版本小于正式版本
                else -> comparePreReleaseIdentifiers(a, b)
            }
        }

        /**
         * 比较预发布标识符
         * 按照语义化版本规范的详细规则
         */
        private fun comparePreReleaseIdentifiers(a: String, b: String): Int {
            val aParts = a.split(".")
            val bParts = b.split(".")
            val minLength = minOf(aParts.size, bParts.size)

            for (i in 0 until minLength) {
                val aNum = aParts[i].toIntOrNull()
                val bNum = bParts[i].toIntOrNull()

                val result = when {
                    aNum != null && bNum != null -> aNum.compareTo(bNum)
                    aNum != null -> -1 // 数字标识符优先级低于文本
                    bNum != null -> 1
                    else -> aParts[i].compareTo(bParts[i])
                }

                if (result != 0) return result
            }

            return aParts.size.compareTo(bParts.size)
        }
    }

    /**
     * 版本解析结果
     */
    private data class VersionParts(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preRelease: String?,
        val buildMetadata: String?
    )
}