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
import com.intellij.openapi.util.UserDataHolderBase
import lldbprotobuf.Model

import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException

class LLDBType(
    val typeName: String,
    val displayTypeName: String,
    val typeKind: TypeKind?,

    ) {
    override fun toString(): String {

        return typeName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LLDBType) return false

        if (typeName != other.typeName) return false
        if (displayTypeName != other.displayTypeName) return false



        return typeKind == other.typeKind
    }

    override fun hashCode(): Int {
        var result = typeName.hashCode()
        result = 31 * result + displayTypeName.hashCode()
        result = 31 * result + typeKind.hashCode()

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
    enum class TypeKind {
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

/**
 * 低级别值（LLDBValue）类
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
class LLDBValue(
    val name: String,
    val type: LLDBType,
val valueKind: ValueKind,
    val address: Long?,
    val referenceExpression: String
) : UserDataHolderBase() {
    /**
     * 调试器值的种类枚举
     *
     * 用于标识调试器中不同类型的值，帮助区分变量的作用域和来源。
     * 每种值类型在调试过程中可能有不同的显示方式、访问权限或处理逻辑。
     *
     * 使用场景：
     * - 在调试器UI中区分不同来源的变量（全局变量、局部变量、参数等）
     * - 根据值类型提供不同的操作选项
     * - 过滤和分类显示变量列表
     * - 确定变量的作用域和生命周期
     */
    enum class ValueKind {
        /**
         * 无效值
         *
         * 表示一个无效或未定义的值。通常在以下情况出现：
         * - protobuf 枚举值无法识别时
         * - 值对象已失效或过期时
         * - 初始化失败时的默认状态
         */
        VALUE_INVALID,

        /**
         * 全局变量
         *
         * 在程序全局作用域中定义的变量，整个程序都可以访问。
         * 全局变量的生命周期从程序启动到程序结束。
         */
        VALUE_GLOBAL,

        /**
         * 静态变量
         *
         * 在函数或类中使用 static 关键字定义的变量。
         * 静态变量在程序的整个执行期间都存在，但作用域可能受限。
         */
        VALUE_STATIC,

        /**
         * 函数参数
         *
         * 函数或方法的参数变量，在函数调用时由调用者传递。
         * 参数的生命周期限于函数执行期间。
         */
        VALUE_ARGUMENT,

        /**
         * 本地变量
         *
         * 在函数或代码块内部定义的局部变量。
         * 本地变量的作用域限于其定义的代码块内。
         */
        VALUE_LOCAL,

        /**
         * 寄存器值
         *
         * 存储在CPU寄存器中的值，通常是编译器优化的结果。
         * 寄存器值访问速度快，但数量有限。
         */
        VALUE_REGISTER,

        /**
         * 寄存器集合
         *
         * 一组相关的寄存器值，通常表示某个时刻的所有寄存器状态。
         * 用于查看完整的CPU寄存器快照。
         */
        VALUE_REGISTER_SET,

        /**
         * 常量结果
         *
         * 表达式求值产生的常量结果值。
         * 这种值通常在编译时就可以确定，或者在表达式计算中产生。
         */
        VALUE_CONST_RESULT,

        /**
         * 线程局部变量
         *
         * 使用线程局部存储（Thread Local Storage, TLS）的变量。
         * 每个线程都有自己独立的副本，线程之间互不影响。
         */
        VALUE_THREAD_LOCAL;

        companion object {
            fun fromProto(proto: lldbprotobuf.Model.ValueKind): ValueKind {
                return when (proto) {
                    lldbprotobuf.Model.ValueKind.VALUE_GLOBAL -> VALUE_GLOBAL
                    lldbprotobuf.Model.ValueKind.VALUE_STATIC -> VALUE_STATIC
                    lldbprotobuf.Model.ValueKind.VALUE_ARGUMENT -> VALUE_ARGUMENT
                    lldbprotobuf.Model.ValueKind.VALUE_LOCAL -> VALUE_LOCAL
                    lldbprotobuf.Model.ValueKind.VALUE_REGISTER -> VALUE_REGISTER
                    lldbprotobuf.Model.ValueKind.VALUE_REGISTER_SET -> VALUE_REGISTER_SET
                    lldbprotobuf.Model.ValueKind.VALUE_CONST_RESULT -> VALUE_CONST_RESULT
                    lldbprotobuf.Model.ValueKind.VALUE_THREAD_LOCAL -> VALUE_THREAD_LOCAL
                    else -> VALUE_INVALID
                }
            }
        }
    }

    /**
     * 值是否有效
     *
     * 当值对应的对象在调试过程中发生变化或失效时，该标志会被设置为false。
     * 这有助于避免在值已经过期的情况下继续使用它。
     */
    var valid: Boolean = true


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
        if (other !is LLDBValue) return false

        if (valid != other.valid) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (type != other.type) return false
        if (address != other.address) return false

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
        result = 31 * result + type.hashCode()
        result = 31 * result + (address?.hashCode() ?: 0)

        result = 31 * result + referenceExpression.hashCode()
        result = 31 * result + valid.hashCode()
        return result
    }


}

internal val LLVALUE_ID: Key<Long> = Key.create("CangJie.Debugger.LLDBVALUE_ID")
internal val LLVALUE_DATA_LOADER: Key<(LLDBValue) -> LLDBValueData> =
    Key.create("CangJie.Debugger..LLDBVALUE_DATA_LOADER")
internal val LLVALUE_DATA: Key<LLDBValueData> = Key.create("CangJie.Debugger.LLDBVALUE_DATA")
internal val CHILDREN_COUNT_CACHE: Key<Int> = Key.create("CangJie.Debugger.CHILDREN_COUNT_CACHE")
fun createLLDBType(
    type: Model.Type
): LLDBType {
    val typeKind: LLDBType.TypeKind? = when (type.typeKind) {
        Model.TypeKind.TYPE_FUNCTION -> LLDBType.TypeKind.FUNCTION
        Model.TypeKind.TYPE_BUILTIN -> LLDBType.TypeKind.BUILTIN
        Model.TypeKind.TYPE_CLASS,
        Model.TypeKind.TYPE_STRUCT -> LLDBType.TypeKind.CLASS_STRUCT

        Model.TypeKind.TYPE_BLOCK_POINTER -> LLDBType.TypeKind.POINTER
        else -> null
    }

    return LLDBType(
        type.typeName,
        type.displayType,
        typeKind
    )
}

/**
 * Creates an LLDBValue from a protobuf Model.Value
 *
 * @param lldbValue The protobuf value from LLDB
 * @param expression Optional expression string, defaults to value name
 * @param dataLoader Function to load value data lazily
 * @return Configured LLDBValue instance
 */
fun createLLDBValue(
    lldbValue: Model.Variable,
    expression: String?,
    dataLoader: (LLDBValue) -> LLDBValueData
): LLDBValue {


    val referenceExpression: String = lldbValue.getName()
    val result = LLDBValue(
        expression ?: lldbValue.getName(),
        createLLDBType(lldbValue.getType()),
LLDBValue.ValueKind.fromProto(lldbValue.valueKind),
        lldbValue.address,
        referenceExpression
    )

    result.putUserData(LLVALUE_ID, lldbValue.id.id)
    result.putUserData(LLVALUE_DATA_LOADER, dataLoader)

    return result
}

/**
 * Gets the LLDB value ID from an LLDBValue
 *
 * @param value The LLDBValue to extract ID from
 * @return The LLDB value ID
 * @throws ExecutionException if the ID is not found
 */
@Throws(ExecutionException::class)
fun getValueId(value: LLDBValue): Long {
    return value.getUserData(LLVALUE_ID)
        ?: throw ExecutionException("Value ID not found for $value")
}

/**
 * Loads value data, using cached data if available
 *
 * @param value The LLDBValue to load data for
 * @return The loaded or cached LLValueData
 * @throws ExecutionException if data cannot be loaded
 */
@Throws(ExecutionException::class, DebuggerCommandException::class)
fun loadValueData(value: LLDBValue): LLDBValueData {
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
