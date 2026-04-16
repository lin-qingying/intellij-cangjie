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

package org.cangnova.cangjie.debugger.protobuf.core

import com.intellij.execution.ExecutionException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.*
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.containers.FactoryMap
import org.cangnova.cangjie.debugger.protobuf.data.LLDBFrame
import org.cangnova.cangjie.debugger.protobuf.data.LLDBThread
import org.cangnova.cangjie.debugger.protobuf.data.LLDBValue
import org.cangnova.cangjie.debugger.protobuf.data.LLDBVariable
import org.cangnova.cangjie.debugger.protobuf.exception.DebuggerCommandExceptionException
import org.cangnova.cangjie.debugger.protobuf.result.PagedResult

/**
 * 调试器表达式求值上下文类
 *
 * 该类提供了在调试过程中执行表达式求值的环境和功能。它负责管理与调试驱动的连接，
 * 在指定的线程和栈帧中执行表达式，并缓存求值结果以提高性能。
 *
 * 使用场景：
 * - 在调试过程中求值变量的值
 * - 执行复杂的表达式计算
 * - 调用对象的方法
 * - 获取对象的属性和字段
 * - 类型转换和强制类型转换
 * - 缓存类型信息以避免重复计算
 *
 * @param facade 调试器驱动门面，负责与底层调试器通信
 * @param expirable 可过期对象引用，用于检测求值上下文是否仍然有效
 * @param thread 当前求值所在的线程
 * @param frame 当前求值所在的栈帧
 * @param cacheHolder 用户数据缓存容器，用于存储类型信息缓存
 */
class EvaluationContext(
    private val facade: DebuggerDriverFacade,
    private val expirable: Expirable?,
    private val thread: LLDBThread,
    private val frame: LLDBFrame,
    private val cacheHolder: UserDataHolderEx
) : Expirable {


    /**
     * 将值转换为右值（R-Value）表达式字符串
     *
     * 该方法用于将给定的LLValue对象转换为可用于表达式求值的右值字符串。
     * 右值是指可以出现在赋值运算符右侧的表达式。
     *
     * 使用场景：
     * - 在表达式中使用变量值时，需要将其转换为右值形式
     * - 处理空指针时返回"((id)0)"表示空对象指针
     * - 处理整型指针的特殊情况，返回"0"而不是指针表达式
     *
     * @param data 包含值类型信息的LLValueData对象
     * @param pair 包含LLValue对象和对应表达式字符串的Pair
     * @return 可用于表达式求值的右值字符串
     * @throws DebuggerCommandExceptionException 当调试命令执行失败时
     * @throws ExecutionException 当执行过程中出现错误时
     */
    @Throws(DebuggerCommandExceptionException::class, ExecutionException::class)
    fun convertToRValue(data: LLDBValue, pair: Pair<LLDBVariable, String>): String {
        val rValueData = getValue(pair.first)

        return if (rValueData.isNullPointer()) {
            "((id)0)"
        } else {
            if (data.isPointer() && pair.first.type.typeName == "int" && rValueData.intValue() == 0L) {
                "0"
            } else {
                pair.second ?: throw NullPointerException("rValuePair.second must not be null")
            }
        }
    }

    /**
     * 获取值的内存地址
     *
     * 该方法用于获取指定LLValue对象在内存中的地址。这对于理解数据的内存布局和
     * 进行底层调试操作非常有用。
     *
     * 使用场景：
     * - 检查变量在内存中的存储位置
     * - 进行内存相关的调试分析
     * - 验证指针的有效性
     * - 分析内存访问问题
     *
     * @param value 要获取地址的LLValue对象
     * @return 值在内存中的地址（以字节为单位）
     * @throws ExecutionException 当执行过程中出现错误时
     * @throws DebuggerCommandExceptionException 当调试命令执行失败时
     */
    @Throws(ExecutionException::class, DebuggerCommandExceptionException::class)
    fun getValueAddress(value: LLDBVariable): Long {
        TODO()
    }

    /**
     * 检查求值上下文是否已过期
     *
     * 求值上下文可能在以下情况下过期：
     * - 调试会话结束
     * - 线程状态改变
     * - 栈帧不再有效
     * - 调试器连接断开
     *
     * 使用场景：
     * - 在执行任何求值操作前检查上下文有效性
     * - 避免在无效的上下文中执行调试命令
     *
     * @return 如果上下文已过期返回true，否则返回false
     */
    override fun isExpired(): Boolean {
        return expirable?.isExpired ?: false
    }

    /**
     * 检查求值上下文是否过期，如果过期则抛出异常
     *
     * 该方法用于在执行求值操作前验证上下文的有效性。如果上下文已过期，
     * 将抛出EvaluationExpiredException异常来终止当前操作。
     *
     * 使用场景：
     * - 在执行任何调试操作前的安全检查
     * - 确保所有操作都在有效的上下文中进行
     *
     * @throws EvaluationExpiredException 当求值上下文已过期时
     */
    fun checkExpiration() {
        if (expirable != null && isExpired) {
            throw EvaluationExpiredException(this, expirable)
        }
    }

    /**
     * 在当前上下文中求值表达式
     *
     * 该方法在指定的线程和栈帧中执行给定的表达式，并返回求值结果。
     * 这是调试器中最基本和最常用的功能之一。
     *
     * 使用场景：
     * - 求值变量的值
     * - 执行算术或逻辑表达式
     * - 调用函数或方法
     * - 访问对象的属性
     * - 计算复杂的条件表达式
     *
     * @param expression 要求值的表达式字符串
     * @return 表达式的求值结果，以LLValue对象形式返回
     * @throws EvaluationExpiredException 当求值上下文已过期时
     */
    fun evaluate(expression: String): LLDBVariable {
        checkExpiration()
        return kotlinx.coroutines.runBlocking {
            facade.evalService.evaluate(thread, frame, expression)
        }
    }

    /**
     * 求值表达式并返回详细的数据信息
     *
     * 该方法是对evaluate方法的便捷封装，同时执行表达式求值和数据获取操作。
     * 返回的LLValueData对象包含更详细的信息，如类型、值、地址等。
     *
     * 使用场景：
     * - 需要同时获取值和详细类型信息时
     * - 进行变量检查和调试分析
     * - 获取对象或变量的完整描述
     *
     * @param expression 要求值的表达式字符串
     * @return 包含详细信息的LLValueData对象
     */
    fun evaluateData(expression: String): LLDBValue {
        return getValue(evaluate(expression))
    }

    /**
     * 将ID类型转换为指定类型的数值表达式
     *
     * 在Objective-C调试中，id类型是一种通用对象指针。该方法用于将id类型的
     * 表达式转换为特定类型的数值表达式，以便进行数值操作或比较。
     *
     * 使用场景：
     * - 将对象指针转换为数值进行分析
     * - 进行地址相关的计算
     * - 类型转换和强制类型转换
     *
     * @param expr 要转换的表达式
     * @param type 目标类型名称
     * @return 转换后的表达式字符串
     */
    fun castIDToNumber(expr: String, type: String): String {
        return cast(expr, type)
    }

    /**
     * 将名称转换为左值（L-Value）表达式
     *
     * 左值是指可以出现在赋值运算符左侧的表达式，通常表示一个内存位置。
     * 该方法的默认实现直接返回原始名称，子类可以重写以提供特殊的转换逻辑。
     *
     * 使用场景：
     * - 在赋值表达式中使用变量名
     * - 处理需要特殊转换的变量名
     * - 支持复杂的变量名解析
     *
     * @param name 要转换的变量名
     * @return 左值表达式字符串
     */
    fun convertToLValue(name: String): String {
        return name
    }


    companion object {
        /**
         * 执行类型转换表达式
         *
         * 创建一个C风格的类型转换表达式，将指定的表达式转换为指定的类型。
         * 这是调试器中常用的基础功能，用于处理不同类型之间的转换。
         *
         * 使用场景：
         * - 在表达式中进行显式类型转换
         * - 处理指针类型的转换
         * - 数值类型之间的转换
         * - 对象类型转换
         *
         * @param expr 要转换的表达式
         * @param type 目标类型名称
         * @return 转换表达式字符串，格式为"((type)(expr))"
         */
        fun cast(expr: String, type: String): String {
            return "(($type)($expr))"
        }

        /**
         * 类型信息缓存的键名
         * 用于在UserDataHolder中存储类型相关的缓存信息
         */
        private val TYPES_CACHE_KEY = Key.create<Map<String, UserDataHolder>>("TYPES_CACHE_KEY")

        /**
         * 创建Objective-C消息发送表达式
         *
         * 该方法用于生成Objective-C方法调用的表达式字符串。在调试Objective-C代码时，
         * 方法调用实际上是通过消息发送机制实现的。
         *
         * 使用场景：
         * - 在Objective-C调试中调用对象的方法
         * - 生成方法调用表达式用于求值
         * - 处理带有参数的方法调用
         *
         * @param expr 目标对象的表达式
         * @param selectorAndArgs 方法选择器和参数，如"methodName:param1:"
         * @param returnType 方法返回值类型
         * @return 完整的消息发送表达式字符串
         */
        fun messageSendExpr(expr: String, selectorAndArgs: String, returnType: String): String {
            return cast("[$expr $selectorAndArgs]", returnType)
        }

        /**
         * 创建从NSString对象转换为C字符串的表达式
         *
         * 该方法生成一个表达式，用于将NSString对象转换为UTF-8编码的C字符串。
         * 这是调试Objective-C代码时的常用操作，便于查看NSString的内容。
         *
         * 使用场景：
         * - 在调试器中查看NSString的内容
         * - 将NSString转换为可显示的字符串
         * - 处理字符串相关的调试操作
         *
         * @param expr NSString对象的表达式
         * @return 转换为C字符串的表达式，结果为char*类型
         */
        fun stringFromNSStringExpr(expr: String): String {
            val lengthExpr = messageSendExpr(expr, "length", "int")
            val substringExpr = messageSendExpr(
                expr,
                "substringToIndex:" + String.format("(((%s) > %d) ? %d : %s)", lengthExpr, 256, 256, lengthExpr),
                "id"
            )
            return messageSendExpr(substringExpr, "UTF8String", "char *")
        }
    }


    /**
     * 从NSString对象获取字符串内容
     *
     * 该方法用于将NSString对象转换为可读的字符串内容，便于在调试过程中查看和分析。
     * 处理了字符串的编码转换和引号移除。
     *
     * 使用场景：
     * - 在调试器中查看NSString变量的内容
     * - 分析字符串数据
     * - 验证字符串操作的结果
     * - 处理本地化字符串
     *
     * @param nsstring 要转换的NSString对象
     * @return 字符串内容，已移除引号
     */
    fun stringFromNSString(nsstring: LLDBVariable): String {
        return StringUtil.unquoteString(
            evaluateData(
                stringFromNSStringExpr(getValue(nsstring).getPointer()),

                ).getPresentableValue()
        )
    }

    /**
     * 获取值的详细数据信息
     *
     * 该方法从LLValue对象中获取详细的数据信息，包括类型、值、地址等。
     * 这是调试器中获取变量信息的基础方法。
     *
     * 使用场景：
     * - 获取变量的完整信息
     * - 分析复杂数据结构
     * - 检查对象的类型和状态
     * - 内存地址分析
     *
     * @param varValue 要获取数据的LLValue对象
     * @return 包含详细信息的LLValueData对象
     * @throws EvaluationExpiredException 当求值上下文已过期时
     */
    fun getValue(varValue: LLDBVariable): LLDBValue {
        checkExpiration()
        return kotlinx.coroutines.runBlocking {
            facade.evalService.getValue(varValue)
        }
    }


    /**
     * 获取变量的子项列表
     *
     * 该方法用于获取复合值的子项列表，支持分页获取以处理大型数据结构。
     * 通过offset和size参数可以控制获取的范围。
     *
     * 使用场景：
     * - 查看数组元素
     * - 浏览对象的属性
     * - 分析数据结构的内容
     * - 分页显示大型集合
     *
     * @param varValue 父值的LLValue对象
     * @param offset 起始偏移量
     * @param size 要获取的子项数量
     * @return 包含子项的PagedResult对象
     * @throws EvaluationExpiredException 当求值上下文已过期时
     */
    fun getVariableChildren(varValue: LLDBVariable, offset: Int, size: Int): PagedResult<LLDBVariable> {
        checkExpiration()
        return kotlinx.coroutines.runBlocking {
            facade.evalService.getVariableChildren(varValue, offset, size)
        }
    }

    /**
     * 获取缓存的类型信息
     *
     * 该方法用于从缓存中获取特定类型的附加信息，避免重复计算。
     * 这是优化调试性能的重要机制。
     *
     * 使用场景：
     * - 获取类型元数据
     * - 缓存复杂的类型分析结果
     * - 提高类型检查的性能
     * - 存储类型相关的调试信息
     *
     * @param type 类型名称
     * @param key 缓存数据的键
     * @return 缓存的类型信息，如果不存在则返回null
     * @param <T> 缓存数据的类型
     */
    fun <T> getCachedTypeInfo(type: String, key: Key<T>): T? {
        val holder = getTypeInfoHolder(type)
        return holder?.getUserData(key)
    }

    /**
     * 存储缓存的类型信息
     *
     * 该方法用于将类型相关的信息存储到缓存中，以便后续使用。
     * 这是提高调试器性能的重要机制，避免重复的昂贵操作。
     *
     * 使用场景：
     * - 缓存类型解析结果
     * - 存储类型元数据
     * - 保存类型分析方法的结果
     * - 优化重复的类型查询
     *
     * @param type 类型名称
     * @param key 缓存数据的键
     * @param value 要缓存的值，可以为null
     * @param <T> 缓存数据的类型
     */
    fun <T> putCachedTypeInfo(type: String, key: Key<T>, value: T?) {
        val holder = getTypeInfoHolder(type)
        holder?.putUserData(key, value)
    }

    /**
     * 获取类型信息缓存容器
     *
     * 该方法用于获取指定类型的缓存容器，用于存储该类型相关的所有缓存信息。
     * 每个类型都有独立的缓存空间，避免类型间的信息混淆。
     *
     * @param type 类型名称
     * @return 类型信息缓存容器，如果类型为"id"则返回null
     */
    private fun getTypeInfoHolder(type: String): UserDataHolder? {
        if (type == "id") {
            return null
        }

        val map = cacheHolder.getUserData(TYPES_CACHE_KEY)
            ?: cacheHolder.putUserDataIfAbsent(TYPES_CACHE_KEY, FactoryMap.create { UserDataHolderBase() })

        return map[type]
    }
}

/**
 * 求值上下文过期异常类
 *
 * 当尝试在已过期的求值上下文中执行操作时抛出此异常。
 * 求值上下文过期通常发生在调试状态发生变化时，如线程继续执行、
 * 栈帧改变或调试会话结束等情况。
 *
 * 使用场景：
 * - 调试器状态变化后阻止继续使用旧上下文
 * - 提醒开发者调试状态已经改变
 * - 保护调试器避免在无效状态下执行命令
 *
 * @param context 已过期的求值上下文
 * @param expirable 导致过期的可过期对象
 */
class EvaluationExpiredException(
    context: EvaluationContext,
    expirable: Expirable
) : ProcessCanceledException("expired: $expirable evaluation context: $context")
