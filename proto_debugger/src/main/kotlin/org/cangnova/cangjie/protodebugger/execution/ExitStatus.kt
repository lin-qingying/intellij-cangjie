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

package org.cangnova.cangjie.protodebugger.execution

import java.util.Objects

/**
 * 进程退出状态类
 *
 * 该类表示进程的退出状态信息，包括退出码和可选的描述信息。
 * 它区分了正常退出、信号终止和未知状态等情况，为调试器提供完整的进程终止信息。
 *
 * 使用场景：
 * - 调试器跟踪被调试程序的退出状态
 * - 区分不同类型的进程终止（正常退出、异常终止、信号终止）
 * - 提供详细的退出原因信息
 * - 调试器会话的结束状态管理
 *
 * 退出码说明：
 * - 0：程序正常退出
 * - 非0：程序异常退出（具体含义取决于程序）
 * - 128+n：程序因信号n而终止（Unix/Linux惯例）
 * - -1：未知退出状态
 *
 * @param code 进程的退出码
 * @param description 退出状态的描述信息，可选
 */
class ExitStatus(val code: Int, val description: String? = null) {
        companion object {
            /**
             * 未知退出状态
             *
             * 当无法获取进程的实际退出状态时使用，通常表示调试器与进程的连接中断
             * 或状态信息丢失。
             */
            val UNKNOWN = ExitStatus(-1)

            /**
             * 从信号编号创建退出状态
             *
             * 根据Unix/Linux惯例，当进程因信号而终止时，退出码为128加上信号编号。
             * 这种方式可以区分正常退出和信号终止。
             *
             * 使用场景：
             * - 处理SIGKILL、SIGTERM等信号导致的进程终止
             * - 区分不同信号类型的终止原因
             * - 提供标准的信号终止状态表示
             *
             * @param signalNumber 导致进程终止的信号编号
             * @return 对应的ExitStatus对象
             */
            fun fromSignal(signalNumber: Int): ExitStatus {
                val exitCode = 128 + signalNumber
                return ExitStatus(exitCode)
            }
        }

        /**
         * 返回退出状态的字符串表示
         *
         * 格式为"退出码"或"退出码 描述信息"，对于未知状态返回"UNKNOWN"。
         *
         * @return 格式化的退出状态字符串
         */
        override fun toString(): String {
            return if (this == UNKNOWN) "UNKNOWN" else "$code${description ?: ""}"
        }

        /**
         * 比较两个退出状态是否相等
         *
         * 两个退出状态相等需要满足以下条件：
         * 1. 都不是UNKNOWN状态
         * 2. 退出码相等
         * 3. 描述信息相等（都为null或内容相同）
         *
         * UNKNOWN状态不与任何其他状态相等，包括另一个UNKNOWN。
         *
         * @param other 要比较的对象
         * @return 如果退出状态相等返回true，否则返回false
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ExitStatus) return false

            return if (this != UNKNOWN && other != UNKNOWN) {
                this.code == other.code && Objects.equals(this.description, other.description)
            } else {
                false
            }
        }

        /**
         * 计算退出状态的哈希码
         *
         * 基于退出码和描述信息计算哈希码。
         *
         * @return 退出状态的哈希码
         */
        override fun hashCode(): Int {
            return Objects.hash(code, description)
        }
    }