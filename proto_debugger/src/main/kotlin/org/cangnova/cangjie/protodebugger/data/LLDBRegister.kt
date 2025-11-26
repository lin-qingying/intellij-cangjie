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

import lldbprotobuf.Model

/**
 * 寄存器组信息数据类
 *
 * 表示寄存器组的元数据信息，基于 proto 定义中的 RegisterGroup 消息。
 * 用于描述寄存器的组织结构，不包含具体的寄存器值。
 *
 * @param name 寄存器组名称（例如："General Purpose Registers", "Floating Point Registers"）
 * @param registerCount 该组中的寄存器数量
 */
data class LLDBRegisterGroup(
    val name: String,
    val registerCount: Int
)

/**
 * 寄存器数据类
 *
 * 表示调试器中的一个寄存器，包含寄存器的基本信息和值。
 * 基于 proto 定义中的 Register 消息。
 *
 * @param name 寄存器名称（例如："rax", "rbx", "xmm0"）
 * @param value 寄存器值（十六进制字符串）
 * @param valueUnsigned 无符号整数值
 * @param size 寄存器大小（字节）
 * @param typeName 类型名称
 * @param summary 值摘要
 * @param groupName 所属组名
 * @param hasChildren 是否有子元素
 * @param changed 值是否发生变化
 * @param children 子元素寄存器列表（对于向量寄存器等）
 */
data class LLDBRegister(
    val name: String,
    val value: String,
    val valueUnsigned: Long = 0L,
    val size: Int,
    val typeName: String = "",
    val summary: String = "",
    val groupName: String = "",
    val hasChildren: Boolean = false,
    val changed: Boolean = false,
    val children: List<LLDBRegister> = emptyList()
) {
    /**
     * 检查寄存器是否有子元素
     */
    fun hasChildren(): Boolean = hasChildren || children.isNotEmpty()

    /**
     * 返回寄存器的字符串表示
     */
    override fun toString(): String {
        return if (summary.isNotEmpty()) {
            "$name = $summary"
        } else {
            "$name = $value"
        }
    }
}

/**
 * 从 protobuf Register 消息创建 LLDBRegister 对象
 *
 * @param register protobuf 中的 Register 消息
 * @return 转换后的 LLDBRegister 对象
 */
fun createLLDBRegister(register: Model.Register): LLDBRegister {
    return LLDBRegister(
        name = register.name,
        value = register.value,
        valueUnsigned = register.valueUnsigned,
        size = register.size,
        typeName = register.typeName,
        summary = register.summary,
        groupName = register.groupName,
        hasChildren = register.hasChildren,
        changed = register.changed,
        children = register.childrenList.map { createLLDBRegister(it) }
    )
}