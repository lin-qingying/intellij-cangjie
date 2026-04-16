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


/**
 * 表示调试器中的监视点（Watchpoint）
 *
 * 监视点是一种特殊的断点，用于监视特定内存地址或表达式的访问情况。
 * 当被监视的内存位置被读取、写入或访问时，调试器会暂停程序执行。
 *
 * @param id 监视点的唯一标识符
 * @param expression 监视的表达式或内存地址
 *
 * 使用场景：
 * - 变量监控：监视全局变量或成员变量的变化
 * - 内存调试：检测内存访问异常和缓冲区溢出
 * - 性能分析：统计特定内存位置的访问频率
 * - 数据流分析：跟踪数据的修改和访问路径
 *
 * 示例用法：
 * ```
 * val watchpoint = LLWatchpoint(1, "globalVariable")
 * println("监视点: $watchpoint")
 *
 * // 设置监视点
 * debuggerDriver.addWatchpoint(watchpoint, AccessType.WRITE)
 * ```
 *

 */
class LLDBWatchpoint(val id: Long, private val expression: String) {


    /**
     * 获取监视点对应的表达式
     *
     * @return 监视的表达式字符串
     */
    fun getExpression(): String {
        return expression
    }

    override fun toString(): String {
        return "Watchpoint-$id@$expression"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LLDBWatchpoint || !super.equals(other)) return false

        return expression == other.expression
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + expression.hashCode()
        return result
    }

    /**
     * 监视点的生命周期类型
     *
     * 定义了监视点的存活时间和作用范围。
     */
    enum class Lifetime(private val displayText: String) {
        /** 栈帧级别：仅在当前栈帧有效，离开函数时自动删除 */
        STACK_FRAME("Stack Frame"),

        /** 持久化：在整个调试会话期间有效，需要手动删除 */
        PERSISTENT("Persistent");

        override fun toString(): String {
            return displayText
        }
    }

    /**
     * 监视点的访问类型
     *
     * 定义了触发监视点时需要满足的内存访问条件。
     */
    enum class AccessType(private val param: String, private val displayText: String, private val tupleKey: String) {
        /** 读访问：当内存被读取时触发 */
        READ("-r", "Read", "hw-rwpt"),

        /** 写访问：当内存被写入时触发（默认） */
        WRITE("", "Write", "wpt"),

        /** 任意访问：读或写时都会触发 */
        ANY("-a", "Any", "hw-awpt");

        /**
         * 获取调试器命令参数字符串
         *
         * @return 用于LLDB命令的参数字符串
         */
        fun getParamString(): String {
            return param
        }

        override fun toString(): String {
            return displayText
        }

        /**
         * 获取元组键名
         *
         * @return 在协议通信中使用的键名
         */
        fun getTupleKey(): String {
            return tupleKey
        }
    }
}
