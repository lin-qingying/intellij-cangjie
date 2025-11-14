package org.cangnova.cangjie.protodebugger.util

import com.intellij.openapi.util.Version
import com.intellij.util.VersionUtil
import java.io.Serializable


import java.util.Objects
import java.util.regex.Pattern
import org.jetbrains.annotations.NotNull

/**
 * 工具版本类
 *
 * 该类用于表示和管理工具软件的版本信息，包括版本字符串和结构化的版本号。
 * 它支持版本比较、格式化和解析等功能，是调试器工具链版本管理的基础类。
 *
 * 使用场景：
 * - 调试器工具版本检查和兼容性验证
 * - 版本依赖关系管理
 * - 版本字符串的解析和格式化
 * - 工具链版本冲突检测
 *
 * 主要功能：
 * - 存储版本字符串和结构化版本号
 * - 支持多种构造方式（字符串、数字、Version对象）
 * - 提供版本解析和格式化功能
 * - 支持未知版本的处理
 *
 * @param versionString 版本的字符串表示
 * @param versionNumber 结构化的版本号对象
 */
class ToolVersion(versionString: String, versionNumber: Version) : Serializable {
    /**
     * 版本的字符串表示
     *
     * 保存原始的版本字符串，用于显示和日志记录。
     */
    val versionString: String = versionString

    /**
     * 结构化的版本号
     *
     * 用于版本比较和数值计算，包含主版本号、次版本号和修订号。
     */
    val versionNumber: Version = versionNumber

    /**
     * 从数字构造版本对象
     *
     * 使用主版本号、次版本号和修订号创建版本对象，
     * 自动生成对应的版本字符串。
     *
     * @param major 主版本号
     * @param minor 次版本号
     * @param bugfix 修订号
     */
    constructor(major: Int, minor: Int, bugfix: Int) : this(Version.toCompactString(major, minor, bugfix), Version(major, minor, bugfix))

    /**
     * 从自定义字符串和数字构造版本对象
     *
     * 使用自定义的版本字符串和指定的版本号创建版本对象。
     * 适用于需要特殊版本字符串格式的情况。
     *
     * @param versionString 自定义的版本字符串
     * @param major 主版本号
     * @param minor 次版本号
     * @param bugfix 修订号
     */
    constructor(versionString: String, major: Int, minor: Int, bugfix: Int) : this(versionString, Version(major, minor, bugfix))

    /**
     * 从Version对象构造版本对象
     *
     * 使用IntelliJ平台的Version对象创建ToolVersion对象，
     * 自动生成紧凑格式的版本字符串。
     *
     * @param version IntelliJ平台的Version对象
     */
    constructor(version: Version) : this(version.toCompactString(), version)

    companion object {
        /**
         * 空版本号
         *
         * 表示未知或无效的版本，用于初始化和错误处理。
         */
        private val NULL_VERSION = Version(0, 0, 0)

        /**
         * 创建未知版本对象
         *
         * 当版本字符串无法解析或版本信息不可用时，
         * 创建一个标记为未知版本的ToolVersion对象。
         *
         * 使用场景：
         * - 处理版本解析失败的情况
         * - 表示版本信息缺失的工具
         * - 错误处理和日志记录
         *
         * @param versionString 原始版本字符串（可能无法解析）
         * @return 标记为未知版本的ToolVersion对象
         */
        @JvmStatic
        fun createUnknown(versionString: String): ToolVersion {
            return ToolVersion(versionString, NULL_VERSION)
        }

        /**
         * 解析版本字符串
         *
         * 使用正则表达式模式解析版本字符串，提取版本号信息。
         * 支持多种版本格式的解析，提高兼容性。
         *
         * 使用场景：
         * - 从工具输出中提取版本信息
         * - 解析不同格式的版本字符串
         * - 处理非标准版本格式
         *
         * @param versionString 要解析的版本字符串
         * @param patterns 用于解析的正则表达式模式，按优先级顺序尝试
         * @return 解析后的ToolVersion对象，解析失败时返回未知版本
         */
        @JvmStatic
        fun parse(versionString: String, vararg patterns: Pattern): ToolVersion {
            val version = VersionUtil.parseVersion(versionString, *patterns)
            return ToolVersion(versionString, version ?: NULL_VERSION)
        }
    }

    /**
     * 返回版本的字符串表示
     *
     * @return 原始的版本字符串
     */
    override fun toString(): String {
        return versionString
    }

    /**
     * 返回紧凑格式的版本字符串
     *
     * 如果版本未知则返回"unknown"，否则返回标准格式的版本字符串。
     * 紧凑格式通常用于显示和日志记录。
     *
     * @return 紧凑格式的版本字符串
     */
    @NotNull
    fun toCompactString(): String {
        return if (isUnknown()) "unknown" else versionNumber.toCompactString()
    }

    /**
     * 检查是否为未知版本
     *
     * 判断当前版本对象是否表示未知或无效的版本。
     *
     * @return 如果是未知版本返回true，否则返回false
     */
    fun isUnknown(): Boolean {
        return versionNumber == NULL_VERSION
    }

    /**
     * 比较两个版本对象是否相等
     *
     * 比较版本字符串、版本号和未知状态，只有所有字段都相等时才认为版本相等。
     *
     * @param other 要比较的对象
     * @return 如果版本相等返回true，否则返回false
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ToolVersion) return false
        return isUnknown() == other.isUnknown() && versionString == other.versionString && versionNumber == other.versionNumber
    }

    /**
     * 计算版本对象的哈希码
     *
     * 基于未知状态、版本字符串和版本号计算哈希码。
     *
     * @return 版本对象的哈希码
     */
    override fun hashCode(): Int {
        return Objects.hash(isUnknown(), versionString, versionNumber)
    }
}