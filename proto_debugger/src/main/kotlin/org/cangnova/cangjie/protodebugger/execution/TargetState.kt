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

/**
 * 目标状态枚举
 *
 * 该枚举定义了调试器中被调试程序（目标程序）的各种执行状态。
 * 调试器通过跟踪这些状态来管理程序的执行流程，并提供相应的调试功能。
 *
 * 使用场景：
 * - 调试器UI中显示程序执行状态
 * - 控制调试操作的可执行性（如只有在暂停时才能单步执行）
 * - 状态转换管理和验证
 * - 调试器内部逻辑的状态机控制
 *
 * 状态转换流程：
 * NOT_READY -> RUNNING -> SUSPENDED -> RUNNING -> FINISHING -> FINISHED
 */
enum class TargetState {
    /**
     * 未就绪状态
     *
     * 表示目标程序尚未准备好执行，可能处于以下情况：
     * - 调试器正在初始化连接
     * - 程序正在加载中
     * - 等待调试器附加到进程
     * - 等待用户启动调试会话
     *
     * 在此状态下，无法执行调试操作。
     */
    NOT_READY,

    /**
     * 运行状态
     *
     * 表示目标程序正在正常执行中。
     *
     * 使用场景：
     * - 程序自由执行，不受调试器控制
     * - 等待命中断点或异常
     * - 用户点击"继续执行"后的状态
     *
     * 在此状态下，大多数调试操作（如单步执行、变量查看）不可用，
     * 只有暂停和停止操作可以执行。
     */
    RUNNING,

    /**
     * 暂停状态
     *
     * 表示目标程序已暂停执行，等待调试器指令。
     *
     * 使用场景：
     * - 程序命中断点后暂停
     * - 用户手动暂停程序执行
     * - 单步执行后的暂停状态
     * - 遇到异常或信号时的暂停
     *
     * 在此状态下，可以进行完整的调试操作：
     * - 查看和修改变量值
     * - 单步执行（步入、步过、步出）
     * - 查看调用栈
     * - 查看内存和寄存器
     */
    SUSPENDED,

    /**
     * 正在完成状态
     *
     * 表示目标程序正在执行清理工作并即将退出。
     * 这是一个短暂的过渡状态。
     *
     * 使用场景：
     * - 程序执行完main函数后的清理阶段
     * - 调用atexit函数或析构函数
     * - 线程和资源的释放过程
     *
     * 在此状态下，调试功能受限，主要用于观察程序的退出过程。
     */
    FINISHING,

    /**
     * 已完成状态
     *
     * 表示目标程序已经完全执行完毕并退出。
     * 这是调试会话的最终状态。
     *
     * 使用场景：
     * - 程序正常执行完毕
     * - 程序因异常或错误而终止
     * - 用户手动终止程序
     * - 调试器与程序连接断开
     *
     * 在此状态下，调试会话结束，无法再执行调试操作。
     * 用户可以选择重新启动程序或关闭调试会话。
     */
    FINISHED
}
