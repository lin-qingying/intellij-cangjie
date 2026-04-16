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

package org.cangnova.cangjie.debugger.protobuf.data

import com.intellij.util.containers.ContainerUtil
import lldbprotobuf.Model
import org.cangnova.cangjie.debugger.protobuf.breakpoint.AddBreakpointResult
import org.cangnova.cangjie.debugger.protobuf.location.SourceLocation
import org.cangnova.cangjie.debugger.protobuf.memory.Address


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
data class LLDBBreakpointLocation(
    val id: String,
    val address: Address,
    val fileLocation: SourceLocation?
)

fun createBreakpointLocation(location: Model.BreakpointLocation): LLDBBreakpointLocation {
    val sourceLocation = if (location.hasLocation() && location.location.filePath.isNotEmpty()) {
        SourceLocation(
            location.location.filePath,
            location.location.line - 1
        )
    } else {
        null
    }

    return LLDBBreakpointLocation(
        location.id.toString(),
        Address.Companion.Factory.fromLong(location.address),
        sourceLocation
    )
}


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
 * val breakpoint = LLDBBreakpoint(1, "/path/to/file.cj", 10, "x > 0")
 * println("断点: $breakpoint")
 * // 输出: Breakpoint-1@/path/to/file.cj:10:condition:x > 0
 * ```
 */
class LLDBBreakpoint(val id: Long, val origFile: String, private val line: Int, val origCondition: String?) {


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

        val that = other as LLDBBreakpoint

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

private fun makeBreakpointLocation(breakpointId: Model.Id, loc: Model.BreakpointLocation): LLDBBreakpointLocation? {
    return if (!loc.isResolved) {
        null
    } else {
        val id: String = makeBreakpointLocationCanonicalName(breakpointId, loc.id)
        val address = Address.Companion.Factory.fromLong(loc.address)
        val location = SourceLocation(loc.location.filePath, loc.location.line - 1)
        LLDBBreakpointLocation(id, address, location)
    }
}

private fun makeBreakpointLocationCanonicalName(breakpointId: Model.Id, locationId: Model.Id): String {

    return "$breakpointId.${locationId.id}"
}

fun makeBreakpoint(
    breakpoint: Model.Breakpoint,
    breakpointLocations: List<Model.BreakpointLocation>
): AddBreakpointResult {

    val origFilePath =
        if (breakpoint.hasOriginalLocation()) breakpoint.originalLocation.filePath else "<address>"
    val origLine = if (breakpoint.hasOriginalLocation()) breakpoint.originalLocation.line else 0
    val condition: String? = breakpoint.getCondition()
    val lldbBreakpoint = LLDBBreakpoint(breakpoint.id.id, origFilePath, origLine - 1, condition)
    val locationList: List<LLDBBreakpointLocation> = ContainerUtil.mapNotNull(breakpointLocations) { loc ->
        makeBreakpointLocation(
            breakpoint.id,
            loc
        )
    }
    return AddBreakpointResult(lldbBreakpoint, locationList)
}