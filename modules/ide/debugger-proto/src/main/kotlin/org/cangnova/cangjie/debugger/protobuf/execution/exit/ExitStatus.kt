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

package org.cangnova.cangjie.debugger.protobuf.execution.exit

/**
 * 进程退出状态
 *
 * 使用sealed class确保类型安全，明确区分不同的退出场景。
 */
sealed class ExitStatus {
    /**
     * 正常退出
     *
     * @param code 退出码（通常为0表示成功，非0表示程序定义的错误）
     * @param description 可选的退出描述
     */
    data class Normal(
        val code: Int,
        val description: String? = null
    ) : ExitStatus() {
        /**
         * 检查是否成功退出（退出码为0）
         */
        fun isSuccess(): Boolean = code == 0

        override fun toString(): String = when {
            description != null -> "退出码: $code ($description)"
            else -> "退出码: $code"
        }
    }

    /**
     * 信号终止
     *
     * 进程因接收信号而终止（Unix/Linux）
     *
     * @param signalNumber 信号编号（如SIGTERM=15, SIGKILL=9）
     * @param signalName 信号名称（如 "SIGTERM", "SIGKILL"）
     */
    data class Signal(
        val signalNumber: Int,
        val signalName: String? = null
    ) : ExitStatus() {
        /**
         * 根据Unix惯例计算退出码（128 + 信号编号）
         */
        val exitCode: Int get() = 128 + signalNumber

        override fun toString(): String = when {
            signalName != null -> "信号终止: $signalName ($signalNumber)"
            else -> "信号终止: $signalNumber"
        }
    }

    /**
     * 未知退出状态
     *
     * 无法获取进程的实际退出状态，通常是连接中断或状态丢失
     */
    object Unknown : ExitStatus() {
        override fun toString(): String = "未知退出状态"
    }

    companion object {
        /**
         * 创建正常退出状态
         */
        @JvmStatic
        fun normal(code: Int, description: String? = null): ExitStatus {
            return Normal(code, description)
        }

        /**
         * 创建信号终止状态
         */
        @JvmStatic
        fun fromSignal(signalNumber: Int, signalName: String? = null): ExitStatus {
            return Signal(signalNumber, signalName)
        }

        /**
         * 获取未知状态
         */
        @JvmStatic
        fun unknown(): ExitStatus = Unknown
    }
}