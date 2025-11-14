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

package org.cangnova.cangjie.protodebugger.breakpoint

import org.cangnova.cangjie.protodebugger.data.LLFrame
import org.cangnova.cangjie.protodebugger.data.LLThread
import org.cangnova.cangjie.protodebugger.data.LLValue

/**
 * 调试停止位置类
 *
 * 该数据类表示调试器在程序执行过程中停止的位置信息。当程序命中断点、单步执行或因其他原因暂停时，
 * 需要记录当前所在的线程、栈帧以及可能的返回值信息。这些信息对于显示调用栈、变量值和执行状态至关重要。
 *
 * @property thread 当前停止的线程信息
 * @property frame 当前停止的栈帧信息，包含函数调用上下文
 * @property returnValue 函数的返回值，如果当前不在函数返回点则为null
 */
data class StopPlace(
    /**
     * 当前停止的线程信息
     */
    val thread: LLThread,
    
    /**
     * 当前停止的栈帧信息
     */
    val frame: LLFrame,
    
    /**
     * 函数的返回值
     * 仅在函数执行完成并返回时有值，其他情况下为null
     */
    val returnValue: LLValue? = null
    ) {
    /**
     * 辅助构造函数
     * 
     * 创建一个没有返回值的停止位置对象。
     * 
     * @param thread 当前停止的线程信息
     * @param frame 当前停止的栈帧信息
     */
    constructor(thread: LLThread, frame: LLFrame) : this(thread, frame, null)

    /**
     * 将停止位置转换为字符串表示
     * 
     * @return 包含线程和栈帧信息的字符串
     */
    override fun toString(): String {
        return "$thread: $frame"
    }
}
