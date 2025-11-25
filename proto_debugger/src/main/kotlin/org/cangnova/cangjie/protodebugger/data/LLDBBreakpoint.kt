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

package org.cangnova.cangjie.protodebugger.data

import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtil
import org.cangnova.cangjie.protodebugger.breakpoint.AddBreakpointResult
import org.cangnova.cangjie.protodebugger.memory.Address
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.NonNls
import proto.Model
import java.util.regex.Matcher
import java.util.regex.Pattern

private val LOCATION_PATTERN: Pattern = Pattern.compile("^(.*):(\\d+)$")

/**
 * 表示调试目标模块
 *
 * 封装了动态链接库或可执行文件的路径信息，用于模块级别的断点和符号解析。
 *
 * @param path 模块的完整路径
 * @property name 模块文件名（不包含路径）
 *
 * 使用场景：
 * - 模块加载跟踪：监控动态库的加载和卸载
 * - 符号解析：在特定模块中查找函数和变量符号
 * - 模块断点：在模块入口或导出函数设置断点
 * - 调试信息：显示当前加载的模块列表
 */
data class LLModule(val path: String) {
    /** 获取模块文件名，去除路径部分 */
    val name: String = PathUtil.getFileName(path)


}

/**
 * 表示符号断点
 *
 * 符号断点基于函数名或符号名称设置，而不是特定的代码行。当函数被调用或符号被访问时触发。
 *
 * @param id 断点的唯一标识符
 * @param symbolPattern 符号模式（函数名、方法名等）
 * @param condition 断点条件表达式
 * @param enabled 断点是否启用
 *
 * 使用场景：
 * - 函数入口断点：在函数开始执行时暂停
 * - 系统调用跟踪：监控特定系统API的调用
 * - 库函数调试：在外部库函数中设置断点
 * - 动态调试：在没有源码的情况下调试二进制代码
 */
 data class LLSymbolicBreakpoint(
    override val id: Int,
    val symbolPattern: String = "",
    val condition: String? = null,
    val enabled: Boolean = true
) : LLCodepoint(id)


/**
 * 表示源码文件位置
 *
 * 封装了文件路径和行号信息，用于精确定位源码中的位置。
 * 行号从0开始计数，与调试器的内部表示一致。
 *
 * @property path 源码文件的完整路径
 * @property line 行号（从0开始）
 *
 * 使用场景：
 * - 断点设置：在源码的特定行设置断点
 * - 错误报告：指示编译错误或运行时错误的位置
 * - 调试导航：在调试过程中跳转到指定源码位置
 * - 代码覆盖：记录代码执行的行号信息
 *
 * 示例用法：
 * ```
 * val location = FileLocation("/path/to/file.cj", 10)
 * val parsed = FileLocation.tryParse("/path/to/file.cj:11")
 * ```
 */
data class FileLocation(
    @field:NonNls val path: String,
    val line: Int
) {
    companion object {
        /**
         * 从文件路径和行号创建FileLocation
         *
         * 行号会自动减1以转换为调试器内部表示（从0开始计数）。
         *
         * @param path 源码文件路径
         * @param lineNumber 行号（从1开始计数）
         * @return FileLocation实例
         */
        @JvmStatic
        fun fromFileLineNumber(
            @NonNls path: String,
            lineNumber: Int
        ): FileLocation = FileLocation(path, lineNumber - 1)

        /**
         * 尝试解析位置字符串为FileLocation
         *
         * 支持格式"path:line"，例如"/path/to/file.cj:10"。
         * 如果解析失败，返回默认值。
         *
         * @param locationString 位置字符串
         * @param defaultValue 解析失败时返回的默认值
         * @return 解析成功的FileLocation或默认值
         */
        @JvmStatic
        @JvmOverloads
        @Contract
        fun tryParse(
            @NonNls locationString: String,
            defaultValue: FileLocation? = null
        ): FileLocation? {
            val matcher: Matcher = LOCATION_PATTERN.matcher(locationString)
            return if (!matcher.matches()) {
                defaultValue
            } else {
                val path = matcher.group(1)
                val lineNumber = matcher.group(2).toInt()
                FileLocation(path!!, lineNumber)
            }
        }
    }


}

/**
 * 表示断点的实际位置信息
 *
 * 断点位置包含内存地址和对应的源码文件位置，用于建立二进制地址与源码的映射关系。
 *
 * @property id 断点位置的唯一标识符
 * @property address 断点在内存中的绝对地址
 * @property fileLocation 对应的源码文件位置（可能为null）
 *
 * 使用场景：
 * - 断点验证：确认断点已正确设置到指定地址
 * - 源码映射：在断点命中时跳转到对应的源码位置
 * - 调试信息：显示所有断点的详细位置信息
 * - 地址导航：在内存视图中显示断点位置
 */
data class LLBreakpointLocation(
    val id: String,
    val address: Address,
    val fileLocation: FileLocation?
)

fun convertBreakpointLocation(location: Model.BreakpointLocation): LLBreakpointLocation {
    val fileLocation = if (location.hasLocation() && location.location.filePath.isNotEmpty()) {
        FileLocation(
            location.location.filePath,
            location.location.line - 1
        )
    } else {
        null
    }

    return LLBreakpointLocation(
        location.id.toString(),
        Address(location.address),
        fileLocation
    )
}

/**
 * 代码点基类
 *
 * 表示调试器中各种代码点（断点、监视点等）的抽象基类。
 * 所有代码点都有唯一的标识符。
 *
 * @property id 代码点的唯一标识符
 */
open class LLCodepoint(open val id: Int)

/**
 * 表示源码行断点
 *
 * 源码断点是最常用的断点类型，基于文件路径和行号设置。
 * 可以包含条件表达式，只有当条件满足时才会触发断点。
 *
 * @param id 断点的唯一标识符
 * @param origFile 源码文件的完整路径
 * @param line 断点所在的行号（从0开始计数）
 * @param origCondition 断点条件表达式，可以为null
 *
 * 使用场景：
 * - 代码调试：在关键代码行设置断点，暂停程序执行
 * - 条件断点：设置触发条件，只在满足特定条件时暂停
 * - 变量监控：在变量修改前后设置断点观察其值变化
 * - 流程跟踪：在代码分支处设置断点，跟踪程序执行路径
 *
 * 示例用法：
 * ```
 * val breakpoint = LLBreakpoint(1, "/path/to/file.cj", 10, "x > 0")
 * println("断点: $breakpoint")
 * // 输出: Breakpoint-1@/path/to/file.cj:10:condition:x > 0
 * ```
 */
class LLBreakpoint(id: Int, val origFile: String, private val line: Int, val origCondition: String?) : LLCodepoint(id) {


    override fun toString(): String {
        val result = "Breakpoint-$id@$origFile:$line"
        return if (origCondition != null) {
            "$result:condition:$origCondition"
        } else {
            result
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass || !super.equals(other)) return false

        val that = other as LLBreakpoint

        if (line != that.line) return false
        if (origFile != that.origFile) return false
        return origCondition == that.origCondition
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + origFile.hashCode()
        result = 31 * result + line
        result = 31 * result + (origCondition?.hashCode() ?: 0)
        return result
    }
}

private fun makeLocation(breakpointId: Int, loc: Model.BreakpointLocation): LLBreakpointLocation? {

    return if (!loc.isResolved) {
        null
    } else {
        val id: String = makeBreakpointLocationCanonicalName(breakpointId, loc.id)
        val address = Address.fromUnsignedLong(loc.address)
        val location = FileLocation(loc.location.filePath, loc.location.line - 1)
        LLBreakpointLocation(id, address, location)
    }
}

private fun makeBreakpointLocationCanonicalName(breakpointId: Int, locationId: Int): String {

    return "$breakpointId.$locationId"
}

fun makeBreakpoint(
    breakpoint: Model.Breakpoint,
    breakpointLocations: List<Model.BreakpointLocation>
): AddBreakpointResult {

    val origFilePath =
        if (breakpoint.hasOriginalLocation()) breakpoint.originalLocation.filePath else "<address>"
    val origLine = if (breakpoint.hasOriginalLocation()) breakpoint.originalLocation.line else 0
    val condition: String? = breakpoint.getCondition()
    val llBreakpoint = LLBreakpoint(breakpoint.id, origFilePath, origLine - 1, condition)
    val locationList: List<LLBreakpointLocation> = ContainerUtil.mapNotNull(breakpointLocations) { loc ->
        makeLocation(
            breakpoint.id,
            loc
        )
    }
    return AddBreakpointResult(llBreakpoint, locationList)
}