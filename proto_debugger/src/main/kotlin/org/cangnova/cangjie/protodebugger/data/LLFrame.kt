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

import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.util.DebuggerSourceFileHash
import proto.Model

/**
 * 低级别栈帧（LLFrame）类
 *
 * 该类表示调试器调用栈中的一个栈帧，包含了函数调用的详细信息。
 * 栈帧是调试器显示调用栈和进行函数级别调试的基础数据结构。
 *
 * 使用场景：
 * - 在调试器中显示函数调用栈
 * - 提供当前执行上下文信息
 * - 支持函数级别的调试操作
 * - 显示源代码位置和程序计数器信息
 * - 区分内联函数和优化代码
 *
 * 主要功能：
 * - 存储函数名、模块名、文件名等位置信息
 * - 记录源代码行号和程序计数器地址
 * - 标识优化状态和内联函数
 * - 提供符号信息和调试信息的检查方法
 *
 * @param index 栈帧在调用栈中的索引，0表示当前栈帧
 * @param function 函数名称，可能为null（无符号信息时）
 * @param module 模块名称，可能为null
 * @param file 源代码文件名，可能为null（无调试信息时）
 * @param hash 源文件哈希值，用于验证文件版本，可能为null
 * @param line 源代码行号，从1开始计数
 * @param programCounter 程序计数器，指向当前执行的指令地址
 * @param optimized 是否为优化代码，优化代码的调试可能受限
 * @param inlined 是否为内联函数，内联函数在调用栈中可能不可见
 */
class LLFrame(
    val index: Int,
    private val function: String?,
    val module: String?,
    val file: String?,
    val hash: DebuggerSourceFileHash?,
    val line: Int,
    val programCounter: Address,

    private val optimized: Boolean,
    val inlined: Boolean
) {
    constructor(
        index: Int,
        function: String?,
        file: String?,
        hash: DebuggerSourceFileHash?,
        line: Int,
        pc: Long,

        optimized: Boolean,
        inlined: Boolean,
        module: String?
    ) : this(index, function, module, file, hash, line, Address.fromUnsignedLong(pc),  optimized, inlined)



    /**
     * 获取函数名称
     *
     * 返回当前栈帧对应的函数名称。如果函数名为null（通常发生在
     * 没有符号信息的情况下），则返回"<unknown>"作为默认值。
     *
     * 使用场景：
     * - 在调用栈视图中显示函数名
     * - 生成调试信息时的函数标识
     * - 日志记录和错误报告
     *
     * @return 函数名称，如果未知则返回"<unknown>"
     */
    @NlsSafe
    fun getFunction(): String {
        return StringUtil.notNullize(function, "<unknown>")
    }

    /**
     * 检查是否具有符号信息
     *
     * 判断当前栈帧是否包含函数名等符号信息。
     * 符号信息通常来自编译时生成的调试符号。
     *
     * 使用场景：
     * - 判断调试信息的完整性
     * - 决定是否可以显示函数级别的调试信息
     * - 优化调试器UI的显示逻辑
     *
     * @return 如果有函数名信息返回true，否则返回false
     */
    fun hasSymbolInfo(): Boolean {
        return this.function != null
    }

    /**
     * 检查是否具有调试信息
     *
     * 判断当前栈帧是否包含源代码文件等调试信息。
     * 调试信息通常来自编译时生成的调试元数据。
     *
     * 使用场景：
     * - 判断是否可以显示源代码级别的调试
     * - 决定是否可以进行源代码级别的单步执行
     * - 检查调试信息的可用性
     *
     * @return 如果有源代码文件信息返回true，否则返回false
     */
    fun hasDebugInfo(): Boolean {
        return this.file != null
    }
    override fun toString(): String {
        return String.format(
            "%d: %s %s@%s(%s):%d (%s)%s",
            index,
            programCounter,
            function,
            file,
            hash,
            line,

            if (optimized) " [opt]" else ""
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LLFrame) return false

        if (line != other.line) return false
        if (index != other.index) return false
        if (function != other.function) return false
        if (file != other.file) return false
        if (hash != other.hash) return false
        if (programCounter != other.programCounter) return false

        if (optimized != other.optimized) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + (function?.hashCode() ?: 0)
        result = 31 * result + (file?.hashCode() ?: 0)
        result = 31 * result + (hash?.hashCode() ?: 0)
        result = 31 * result + line
        result = 31 * result + programCounter.hashCode()

        result = 31 * result + optimized.hashCode()
        return result
    }
}

private fun createSourceFileHash(type: Model.HashAlgorithm?, hash: String?): DebuggerSourceFileHash? {
    return if (type != null && hash != null) {
        val t: DebuggerSourceFileHash.Type = when (type) {
            Model.HashAlgorithm.HASH_ALGORITHM_MD5 -> DebuggerSourceFileHash.Type.MD5
            Model.HashAlgorithm.HASH_ALGORITHM_SHA1 -> DebuggerSourceFileHash.Type.SHA1
            Model.HashAlgorithm.HASH_ALGORITHM_SHA256 -> DebuggerSourceFileHash.Type.SHA256
            else -> return null
        }
        DebuggerSourceFileHash(t, hash)
    } else {
        null
    }
}
  fun newLLFrame(frame: proto.Model.StackFrame): LLFrame {
    return LLFrame(
        frame.index,
        frame.functionName,
        frame.location.filePath,
        createSourceFileHash(frame.location.hashAlgorithm, frame.location.hashValue),
        frame.location.line - 1,
        frame.programCounter,
        frame.isOptimized,
        frame.isInlined,
        frame.moduleName
    )
}