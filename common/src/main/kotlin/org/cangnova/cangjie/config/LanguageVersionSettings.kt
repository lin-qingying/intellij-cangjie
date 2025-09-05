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

package org.cangnova.cangjie.config


import org.cangnova.cangjie.config.CangJieVersion.Companion.MAX_COMPONENT_VALUE
import org.cangnova.cangjie.config.LanguageFeature.Kind.OTHER
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 描述感知接口
 */
interface DescriptionAware {
    /**
     * 描述信息
     */
    val description: String
}

/**
 * 语言特性枚举类
 *
 * @param sinceVersion 特性引入的语言版本
 * @param sinceApiVersion 特性引入的API版本
 * @param hintUrl 提示URL
 * @param isEnabledWithWarning 是否启用但有警告
 * @param kind 特性类型
 */
enum class LanguageFeature(
    val sinceVersion: LanguageVersion?,
    val sinceApiVersion: ApiVersion = ApiVersion.CANGJIE_0_53_4,
    val hintUrl: String? = null,
    internal val isEnabledWithWarning: Boolean = false,
    val kind: Kind = OTHER // 注意：默认值OTHER不会强制使用预发布版本(参见CDoc)
) {
    ;

    init {
        if (sinceVersion == null && isEnabledWithWarning) {
            error("$this: '${::isEnabledWithWarning.name}' 如果特性默认禁用，则没有效果")
        }
    }

    /**
     * 特性的可展示名称
     */
    val presentableName: String
        // 例如："DestructuringLambdaParameters" -> ["Destructuring", "Lambda", "Parameters"] -> "destructuring lambda parameters"
        get() = name.split("(?<!^)(?=[A-Z])".toRegex()).joinToString(separator = " ", transform = String::lowercase)

    /**
     * 语言特性状态枚举
     */
    enum class State(override val description: String) : DescriptionAware {
        /**
         * 已启用
         */
        ENABLED("Enabled"),

        /**
         * 启用但有警告
         */
        ENABLED_WITH_WARNING("Enabled with warning"),

        /**
         * 已禁用
         */
        DISABLED("Disabled");
    }

    /**
     * 语言特性类型
     *
     * @param forcesPreReleaseBinaries 是否强制使用预发布二进制文件
     */
    enum class Kind(val forcesPreReleaseBinaries: Boolean) {
        /**
         * 简单的bug修复，仅禁止某些语言结构。
         * 经验法则：它将"绿色代码"变为"红色"。
         *
         * 注意，一些实际的bug修复可能会影响重载解析/推断，悄悄地改变用户代码的语义
         * -- 不要为这些修复使用Kind.BUG_FIX！
         */
        BUG_FIX(false),

        /**
         * 语言中的新特性，对编译器的二进制输出没有影响，
         * 因此不会导致生成预发布二进制文件。
         * 经验法则：它将"红色"代码变为"绿色"，旧编译器可以正确使用
         * 新编译器生成的二进制文件。
         *
         * 注意：OTHER不是保守的后备选项，因为它不意味着生成预发布二进制文件
         */
        OTHER(false),
    }

    companion object {
        /**
         * 从字符串解析语言特性
         */
        @JvmStatic
        fun fromString(str: String) = entries.find { it.name == str }
    }
}

/**
 * 语言版本枚举
 *
 * @param major 主版本号
 * @param minor 次版本号
 * @param patch 补丁版本号
 */
enum class LanguageVersion(val major: Int, val minor: Int, val patch: Int) : DescriptionAware, LanguageOrApiVersion {
    CANGJIE_0_0_1(0, 0, 1),

    CANGJIE_0_53_4(0, 53, 4),
    CANGJIE_0_53_5(0, 53, 5),
    CANGJIE_0_53_18(0, 53, 18),

    CANGJIE_0_60_5(0, 60, 4),

    ;

    /**
     * 是否为稳定版本
     */
    override val isStable: Boolean
        get() = this <= LATEST_STABLE

    /**
     * 是否已废弃
     */
    override val isDeprecated: Boolean
        get() = FIRST_SUPPORTED <= this && this < FIRST_NON_DEPRECATED

    /**
     * 是否不受支持
     */
    override val isUnsupported: Boolean
        get() = this < FIRST_SUPPORTED

    /**
     * 版本字符串
     */
    override val versionString: String = "$major.$minor"

    override fun toString() = versionString

    companion object {
        /**
         * 从版本字符串解析语言版本
         */
        @JvmStatic
        fun fromVersionString(str: String?) = entries.find { it.versionString == str }

        /**
         * 从完整版本字符串解析语言版本
         */
        @JvmStatic
        fun fromFullVersionString(str: String) =
            str.split(".", "-").let { if (it.size >= 2) fromVersionString("${it[0]}.${it[1]}") else null }

        // 版本状态
        //            1.0..1.3        1.4..1.6           1.7..2.0    2.1
        // 语言:      UNSUPPORTED --> DEPRECATED ------> STABLE ---> EXPERIMENTAL
        // API:       UNSUPPORTED --> DEPRECATED ------> STABLE ---> EXPERIMENTAL

        /**
         * 第一个支持的API版本
         */
        @JvmField
        val FIRST_API_SUPPORTED = CANGJIE_0_0_1

        /**
         * 第一个支持的版本
         */
        @JvmField
        val FIRST_SUPPORTED = CANGJIE_0_0_1

        /**
         * 第一个非废弃版本
         */
        @JvmField
        val FIRST_NON_DEPRECATED = CANGJIE_0_0_1

        /**
         * 最新稳定版本
         */
        @JvmField
        val LATEST_STABLE = CANGJIE_0_60_5
    }
}

/**
 * 语言或API版本接口
 */
interface LanguageOrApiVersion : DescriptionAware {
    /**
     * 版本字符串
     */
    val versionString: String

    /**
     * 是否为稳定版本
     */
    val isStable: Boolean

    /**
     * 是否已废弃
     */
    val isDeprecated: Boolean

    /**
     * 是否不受支持
     */
    val isUnsupported: Boolean

    /**
     * 描述信息
     */
    override val description: String
        get() = when {
            !isStable -> "$versionString (experimental)"
            isDeprecated -> "$versionString (deprecated)"
            isUnsupported -> "$versionString (unsupported)"
            else -> versionString
        }
}

/**
 * 将语言版本转换为仓颉版本
 */
fun LanguageVersion.toCangJieVersion() = CangJieVersion(major, minor)

/**
 * 语言版本设置接口
 */
interface LanguageVersionSettings {
    /**
     * 获取特性支持状态
     */
    fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State

    /**
     * 是否支持特定特性
     */
    fun supportsFeature(feature: LanguageFeature): Boolean =
        getFeatureSupport(feature).let {
            it == LanguageFeature.State.ENABLED ||
                    it == LanguageFeature.State.ENABLED_WITH_WARNING
        }


    /**
     * 获取分析标志值
     */
    fun <T> getFlag(flag: AnalysisFlag<T>): T

    /**
     * API版本
     */
    val apiVersion: ApiVersion

    /**
     * 语言版本
     * 请不要使用此属性来启用/禁用特定功能/检查。请添加新的LanguageFeature条目并调用supportsFeature
     */
    val languageVersion: LanguageVersion

    companion object {
        /**
         * 允许从环境中读取配置的资源名称
         */
        const val RESOURCE_NAME_TO_ALLOW_READING_FROM_ENVIRONMENT = "META-INF/allow-configuring-from-environment"
    }
}

/**
 * 语言版本设置实现类
 *
 * @param languageVersion 语言版本
 * @param apiVersion API版本
 * @param analysisFlags 分析标志映射
 * @param specificFeatures 特定特性状态映射
 */
class LanguageVersionSettingsImpl @JvmOverloads constructor(
    override val languageVersion: LanguageVersion,
    override val apiVersion: ApiVersion,
    analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap(),
    specificFeatures: Map<LanguageFeature, LanguageFeature.State> = emptyMap()
) : LanguageVersionSettings {
    private val analysisFlags: Map<AnalysisFlag<*>, *> = Collections.unmodifiableMap(analysisFlags)
    private val specificFeatures: Map<LanguageFeature, LanguageFeature.State> =
        Collections.unmodifiableMap(specificFeatures)

    /**
     * 获取分析标志值
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> getFlag(flag: AnalysisFlag<T>): T = analysisFlags[flag] as T? ?: flag.defaultValue

    /**
     * 获取特性支持状态
     */
    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State {
        specificFeatures[feature]?.let { return it }

        val since = feature.sinceVersion
        if (since != null && languageVersion >= since && apiVersion >= feature.sinceApiVersion) {
            return if (feature.isEnabledWithWarning) LanguageFeature.State.ENABLED_WITH_WARNING else LanguageFeature.State.ENABLED
        }

        return LanguageFeature.State.DISABLED
    }

    override fun toString() = buildString {
        append("Language = $languageVersion, API = $apiVersion")
        specificFeatures.entries.sortedBy { (feature, _) -> feature.ordinal }.forEach { (feature, state) ->
            val char = when (state) {
                LanguageFeature.State.ENABLED -> '+'
                LanguageFeature.State.ENABLED_WITH_WARNING -> '~'
                LanguageFeature.State.DISABLED -> '-'
            }
            append(" $char$feature")
        }
        analysisFlags.entries.sortedBy { (flag, _) -> flag.toString() }.forEach { (flag, value) ->
            append(" $flag:$value")
        }
    }


    companion object {
        /**
         * 默认语言版本设置
         */
        @JvmField
        val DEFAULT = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
    }
}

/**
 * 如果启用，特性是否强制使用预发布二进制文件
 */
fun LanguageFeature.forcesPreReleaseBinariesIfEnabled(): Boolean {
    val isFeatureNotReleasedYet = sinceVersion?.isStable != true
    return isFeatureNotReleasedYet && kind.forcesPreReleaseBinaries
}


/**
 * API版本类
 *
 * @param version 版本
 * @param versionString 版本字符串
 */
class ApiVersion private constructor(
    val version: String,
    override val versionString: String
) : Comparable<ApiVersion>, DescriptionAware, LanguageOrApiVersion {

    /**
     * 是否为稳定版本
     */
    override val isStable: Boolean
        get() = this <= LATEST_STABLE

    /**
     * 是否已废弃
     */
    override val isDeprecated: Boolean
        get() = FIRST_SUPPORTED <= this && this < FIRST_NON_DEPRECATED

    /**
     * 是否不受支持
     */
    override val isUnsupported: Boolean
        get() = this < FIRST_SUPPORTED

    override fun compareTo(other: ApiVersion): Int =
        version.compareTo(other.version)

    override fun equals(other: Any?) =
        (other as? ApiVersion)?.version == version

    override fun hashCode() =
        version.hashCode()

    override fun toString() = versionString

    companion object {

        /**
         * CANGJIE_0_53_4 API版本
         */
        @JvmField
        val CANGJIE_0_53_4 = createByLanguageVersion(LanguageVersion.CANGJIE_0_53_4)

        /**
         * 最新API版本
         */
        @JvmField
        val LATEST: ApiVersion = createByLanguageVersion(LanguageVersion.entries.last())

        /**
         * 最新稳定API版本
         */
        @JvmField
        val LATEST_STABLE: ApiVersion = createByLanguageVersion(LanguageVersion.LATEST_STABLE)

        /**
         * 第一个支持的API版本
         */
        @JvmField
        val FIRST_SUPPORTED: ApiVersion = createByLanguageVersion(LanguageVersion.FIRST_API_SUPPORTED)

        /**
         * 第一个非废弃API版本
         */
        @JvmField
        val FIRST_NON_DEPRECATED: ApiVersion = createByLanguageVersion(LanguageVersion.FIRST_NON_DEPRECATED)

        /**
         * 根据语言版本创建API版本
         */
        @JvmStatic
        fun createByLanguageVersion(version: LanguageVersion): ApiVersion = parse(version.versionString)!!

        /**
         * 解析API版本字符串
         */
        fun parse(versionString: String): ApiVersion? = try {
            ApiVersion(versionString, versionString)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 分析标志类
 *
 * @param name 标志名称
 * @param defaultValue 默认值
 */
class AnalysisFlag<out T> internal constructor(
    private val name: String,
    val defaultValue: T
) {
    override fun equals(other: Any?): Boolean = other is AnalysisFlag<*> && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    /**
     * 分析标志委托类
     */
    class Delegate<out T>(name: String, defaultValue: T) : ReadOnlyProperty<Any?, AnalysisFlag<T>> {
        private val flag = AnalysisFlag(name, defaultValue)

        override fun getValue(thisRef: Any?, property: KProperty<*>): AnalysisFlag<T> = flag
    }

    /**
     * 分析标志委托对象
     */
    object Delegates {
        /**
         * 布尔值委托
         */
        object Boolean {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Delegate(property.name, false)
        }

        /**
         * API模式默认禁用委托
         */
        object ApiModeDisabledByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) =
                Delegate(property.name, ExplicitApiMode.DISABLED)
        }

        /**
         * 字符串列表委托
         */
        object ListOfStrings {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) =
                Delegate(property.name, emptyList<String>())
        }
    }
}

/**
 * 显式API模式枚举
 *
 * @param state 状态字符串
 */
enum class ExplicitApiMode(val state: String) {
    /**
     * 已禁用
     */
    DISABLED("disable"),

    /**
     * 严格模式
     */
    STRICT("strict"),

    /**
     * 警告模式
     */
    WARNING("warning");

    companion object {
        /**
         * 从字符串解析显式API模式
         */
        fun fromString(string: String): ExplicitApiMode? = entries.find { it.state == string }

        /**
         * 获取可用值列表
         */
        fun availableValues() = values().joinToString(prefix = "{", postfix = "}") { it.state }
    }
}

/**
 * 仓颉编译器版本对象
 */
object CangJieCompilerVersion {
    /**
     * 版本文件路径
     */
    const val VERSION_FILE_PATH: String = "/META-INF/compiler.version"

    /**
     * 版本
     */
    var VERSION: String? = null

    // 如果此编译器支持的最新稳定语言版本尚未发布，则为True。
    // 使用该语言版本（或任何未来语言版本）由此编译器生成的二进制文件将被标记为
    // "预发布"，并且不会被编译器的发布版本加载。
    // 在每次主要发布之前和之后更改此值
    private const val IS_PRE_RELEASE = true


    /**
     * 编译器版本
     * @return 此编译器的版本，如果未知（如果VERSION为"@snapshot@"）则返回`null`
     */
    val version: String?
        get() = if (VERSION == "@snapshot@") null else VERSION

    /**
     * 加载仓颉编译器版本
     */
    @Throws(IOException::class)
    private fun loadCangJieCompilerVersion(): String {
        val versionReader = BufferedReader(
            InputStreamReader(CangJieCompilerVersion::class.java.getResourceAsStream(VERSION_FILE_PATH))
        )
        try {
            return versionReader.readLine()
        } finally {
            versionReader.close()
        }
    }

    init {
        try {
            VERSION = loadCangJieCompilerVersion()
        } catch (e: IOException) {
            throw IllegalStateException("Failed to read compiler version from " + VERSION_FILE_PATH)
        }

        check(!(VERSION != "@snapshot@" && !VERSION!!.contains("-") && IS_PRE_RELEASE)) {
            """
                IS_PRE_RELEASE cannot be true for a compiler without '-' in its version.
                Please change IS_PRE_RELEASE to false, commit and push this change to master
                """.trimIndent()
        }
    }
}

/**
 * 表示仓颉标准库的版本。
 *
 * [major]、[minor]和[patch]是版本的整数组件，
 * 它们必须是非负的，且不大于255（[MAX_COMPONENT_VALUE]）。
 *
 * @constructor 从所有三个组件创建版本。
 */
class CangJieVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<CangJieVersion> {
    /**
     * 从[major]和[minor]组件创建版本，将[patch]组件设为零。
     */
    constructor(major: Int, minor: Int) : this(major, minor, 0)

    private val version = versionOf(major, minor, patch)

    private fun versionOf(major: Int, minor: Int, patch: Int): Int {
        require(major in 0..MAX_COMPONENT_VALUE && minor in 0..MAX_COMPONENT_VALUE && patch in 0..MAX_COMPONENT_VALUE) {
            "Version components are out of range: $major.$minor.$patch"
        }
        return major.shl(16) + minor.shl(8) + patch
    }

    /**
     * 返回此版本的字符串表示形式
     */
    override fun toString(): String = "$major.$minor.$patch"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherVersion = (other as? CangJieVersion) ?: return false
        return this.version == otherVersion.version
    }

    override fun hashCode(): Int = version

    override fun compareTo(other: CangJieVersion): Int = version - other.version

    /**
     * 如果此版本不低于由提供的[major]和[minor]组件指定的版本，则返回`true`。
     */
    fun isAtLeast(major: Int, minor: Int): Boolean = // this.version >= versionOf(major, minor, 0)
        this.major > major || (this.major == major &&
                this.minor >= minor)

    /**
     * 如果此版本不低于由提供的[major]、[minor]和[patch]组件指定的版本，则返回`true`。
     */
    fun isAtLeast(major: Int, minor: Int, patch: Int): Boolean =
        // this.version >= versionOf(major, minor, patch)
        this.major > major || (this.major == major &&
                (this.minor > minor || this.minor == minor &&
                        this.patch >= patch))

    companion object {
        /**
         * 版本组件可以具有的最大值，常量值255。
         */
        // 注意：必须放在CURRENT之前，因为在JS中初始化CURRENT需要先初始化此字段
        const val MAX_COMPONENT_VALUE = 255

        /**
         * 返回仓颉标准库的当前版本。
         */
        @JvmField
        val CURRENT: CangJieVersion = CangJieVersionCurrentValue.get()
    }
}

// 在考虑是否在仓颉构建中重新编译依赖项时，此类在类路径规范化期间被忽略
private object CangJieVersionCurrentValue {
    @JvmStatic
    fun get(): CangJieVersion = CangJieVersion(0, 0, 0) // 此值在构建过程中自动写入
}

