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

import com.intellij.execution.ExecutionException
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.UserDataHolderBase
import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
import org.cangnova.cangjie.protodebugger.protocol.ProtobufMessageFactory
import proto.Model
import proto.ProtocolResponses

/**
 * 低级别值（LLValue）类
 *
 * 该类表示调试器中的一个值对象，包含了值的基本信息如名称、类型、地址等。
 * 它继承自UserDataHolderBase，可以存储额外的用户数据。
 *
 * 使用场景：
 * - 表示调试过程中的变量值
 * - 存储对象或数据结构的引用
 * - 在调试器UI中显示变量信息
 * - 作为表达式求值的结果容器
 * - 缓存调试过程中的数据
 *
 * @param name 值的名称，通常为变量名或表达式
 * @param type 值的数据类型名称
 * @param displayType 用于显示的类型名称，可能与type不同
 * @param address 值在内存中的地址，可能为null
 * @param typeClass 值的类型分类，用于区分不同类型的值
 * @param referenceExpression 引用表达式，用于在调试器中重新获取该值
 */
class LLValue(
    val name: String,
    val type: String,
    val displayType: String,
    val address: Long?,
    val typeClass: TypeClass?,
    val referenceExpression: String
) : UserDataHolderBase() {

    /**
     * 值是否有效
     *
     * 当值对应的对象在调试过程中发生变化或失效时，该标志会被设置为false。
     * 这有助于避免在值已经过期的情况下继续使用它。
     */
    var valid: Boolean = true

    /**
     * 简化构造函数，使用type作为displayType
     *
     * @param name 值的名称
     * @param type 值的数据类型
     * @param address 内存地址
     * @param typeClass 类型分类
     * @param referenceExpression 引用表达式
     */
    constructor(
        name: String,
        type: String,
        address: Long?,
        typeClass: TypeClass?,
        referenceExpression: String
    ) : this(name, type, type, address, typeClass, referenceExpression)


    /**
     * 返回值的字符串表示
     *
     * 提供一个简洁的字符串表示，格式为"name:type"。
     * 适用于日志记录、调试输出等场景。
     *
     * @return 格式为"name:type"的字符串
     */
    override fun toString(): String {
        return "$name:$type"
    }

    /**
     * 比较两个LLValue对象是否相等
     *
     * 逐一比较所有字段，包括名称、类型、显示类型、地址、
     * 类型分类、引用表达式和有效性标志。
     *
     * @param other 要比较的对象
     * @return 如果所有字段都相等返回true，否则返回false
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LLValue) return false

        if (valid != other.valid) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (displayType != other.displayType) return false
        if (address != other.address) return false
        if (typeClass != other.typeClass) return false
        return referenceExpression == other.referenceExpression
    }

    /**
     * 计算LLValue对象的哈希码
     *
     * 基于所有字段的值计算哈希码，确保equals方法返回true的对象
     * 具有相同的哈希码。
     *
     * @return 对象的哈希码
     */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + displayType.hashCode()
        result = 31 * result + (address?.hashCode() ?: 0)
        result = 31 * result + (typeClass?.hashCode() ?: 0)
        result = 31 * result + referenceExpression.hashCode()
        result = 31 * result + valid.hashCode()
        return result
    }

    /**
     * 值的类型分类枚举
     *
     * 用于对调试器中的值进行分类，以便在UI显示和处理时进行区分。
     * 不同的类型分类可能有不同的显示方式或操作选项。
     *
     * 使用场景：
     * - 在调试器UI中区分不同类型的值
     * - 根据类型分类提供不同的操作选项
     * - 优化值的显示和处理逻辑
     */
    enum class TypeClass {
        /**
         * 类或结构体类型
         *
         * 表示用户定义的类、结构体等复合数据类型。
         * 这些类型通常包含成员变量和方法，可能需要特殊处理。
         */
        CLASS_STRUCT,

        /**
         * Objective-C指针类型
         *
         * 表示Objective-C对象的指针，通常是id类型或具体的类指针。
         * 这些值支持消息发送等Objective-C特有的操作。
         */
        OBJC_POINTER,

        /**
         * 函数类型
         *
         * 表示函数或方法指针，可能包含函数签名信息。
         * 用于调试函数调用和函数指针相关的问题。
         */
        FUNCTION,

        /**
         * 内置类型
         *
         * 表示语言内置的基本数据类型，如int、float、bool等。
         * 这些类型通常有固定的内存布局和操作方式。
         */
        BUILTIN,

        /**
         * 指针类型
         *
         * 表示指向内存地址的指针，可能指向其他类型的数据。
         * 需要特殊的显示和操作处理，如解引用操作。
         */
        POINTER
    }
}

internal val LLVALUE_ID: Key<Int> = Key.create("DebuggerDriver.LLVALUE_ID")
internal val LLVALUE_DATA_LOADER: Key<(LLValue) -> LLValueData> =
    Key.create("DebuggerDriver.LLVALUE_DATA_LOADER")
internal val LLVALUE_DATA: Key<LLValueData> = Key.create("DebuggerDriver.LLVALUE_DATA")
internal val CHILDREN_COUNT_CACHE: Key<Int> = Key.create("DebuggerDriver.CHILDREN_COUNT_CACHE")

/**
 * Creates an LLValue from a protobuf Model.Value
 *
 * @param lldbValue The protobuf value from LLDB
 * @param expression Optional expression string, defaults to value name
 * @param dataLoader Function to load value data lazily
 * @return Configured LLValue instance
 */
fun createLLValue(
    lldbValue: Model.Value,
    expression: String?,
    dataLoader: (LLValue) -> LLValueData
): LLValue {
    val type: LLValue.TypeClass? = when (lldbValue.getTypeClass()) {
        Model.ValueTypeClass.VALUE_TYPE_FUNCTION -> LLValue.TypeClass.FUNCTION
        Model.ValueTypeClass.VALUE_TYPE_BUILTIN -> LLValue.TypeClass.BUILTIN
        Model.ValueTypeClass.VALUE_TYPE_CLASS,
        Model.ValueTypeClass.VALUE_TYPE_STRUCT -> LLValue.TypeClass.CLASS_STRUCT
        Model.ValueTypeClass.VALUE_TYPE_BLOCK_POINTER -> LLValue.TypeClass.POINTER
        else -> null
    }

    val referenceExpression: String = lldbValue.getName()
    val result = LLValue(
        expression ?: lldbValue.getName(),
        lldbValue.typeName,
        lldbValue.getDisplayType(),
        lldbValue.address,
        type,
        referenceExpression
    )

    result.putUserData(LLVALUE_ID, lldbValue.id)
    result.putUserData(LLVALUE_DATA_LOADER, dataLoader)

    return result
}

/**
 * Gets the LLDB value ID from an LLValue
 *
 * @param value The LLValue to extract ID from
 * @return The LLDB value ID
 * @throws ExecutionException if the ID is not found
 */
@Throws(ExecutionException::class)
fun getValueId(value: LLValue): Int {
    return value.getUserData(LLVALUE_ID)
        ?: throw ExecutionException("Value ID not found for $value")
}

/**
 * Loads value data, using cached data if available
 *
 * @param value The LLValue to load data for
 * @return The loaded or cached LLValueData
 * @throws ExecutionException if data cannot be loaded
 */
@Throws(ExecutionException::class, DebuggerCommandException::class)
fun loadValueData(value: LLValue): LLValueData {
    synchronized(value) {
        // Check if data is already loaded
        value.getUserData(LLVALUE_DATA)?.let { return it }

        // Get the loader function
        val loader = value.getUserData(LLVALUE_DATA_LOADER)
            ?: throw ExecutionException("Value data loader not found for $value")

        // Load the data outside synchronized block
        val data = loader(value)

        // Cache the data and remove the loader
        value.putUserData(LLVALUE_DATA, data)
        value.putUserData(LLVALUE_DATA_LOADER, null)

        return data
    }
}
