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

/**
 * 寄存器组信息数据类
 *
 * 表示寄存器组的元数据信息，基于 proto 定义中的 RegisterGroup 消息。
 * 用于描述寄存器的组织结构，不包含具体的寄存器值。
 *
 * @param name 寄存器组名称（例如："General Purpose Registers", "Floating Point Registers"）
 * @param registerCount 该组中的寄存器数量
 */
data class RegisterGroupInfo(
    val name: String,
    val registerCount: Int
)

/**
 * 寄存器集合数据类
 *
 * 表示一个完整的寄存器集合，包含组信息和具体的寄存器值。
 * 用于调试器UI中显示完整的寄存器信息。
 *
 * @param groupInfo 寄存器组的元数据信息
 * @param registers 该组中的具体寄存器列表
 */
data class RegisterSet(
    val groupInfo: RegisterGroupInfo,
    val registers: List<LLDBVariable>
) {
    /**
     * 便捷属性：寄存器组名称
     */
    val name: String get() = groupInfo.name

    /**
     * 便捷属性：寄存器数量
     */
    val registerCount: Int get() = groupInfo.registerCount
}

