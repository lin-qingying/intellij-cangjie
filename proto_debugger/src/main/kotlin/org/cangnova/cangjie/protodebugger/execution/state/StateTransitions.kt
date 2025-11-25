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

package org.cangnova.cangjie.protodebugger.execution.state

/**
 * 目标状态转换规则
 *
 * 定义所有有效的状态转换路径，确保调试流程的一致性。
 */
internal object StateTransitions {
    /**
     * 有效的状态转换映射
     *
     * Key: 当前状态
     * Value: 允许转换到的状态集合
     */
    private val validTransitions = mapOf(
        TargetState.Idle to setOf(
            TargetState.Running,
            TargetState.Terminated  // 初始化失败的情况
        ),
        TargetState.Running to setOf(
            TargetState.Paused,      // 命中断点或暂停
            TargetState.Exiting,     // 即将退出
            TargetState.Terminated   // 异常终止
        ),
        TargetState.Paused to setOf(
            TargetState.Running,     // 继续执行
            TargetState.Exiting,     // 用户终止
            TargetState.Terminated   // 调试会话结束
        ),
        TargetState.Exiting to setOf(
            TargetState.Terminated   // 完成退出
        ),
        TargetState.Terminated to emptySet()  // 终止状态，无法转换
    )

    /**
     * 检查状态转换是否有效
     *
     * @param from 当前状态
     * @param to 目标状态
     * @return true 如果转换有效，false 否则
     */
    fun isValidTransition(from: TargetState, to: TargetState): Boolean {
        if (from == to) return true  // 允许保持当前状态
        return validTransitions[from]?.contains(to) == true
    }

    /**
     * 获取从指定状态可以转换到的所有状态
     *
     * @param from 当前状态
     * @return 可转换到的状态集合
     */
    fun getValidNextStates(from: TargetState): Set<TargetState> {
        return validTransitions[from] ?: emptySet()
    }
}