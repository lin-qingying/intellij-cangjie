package org.cangnova.cangjie.protodebugger.services

import lldbprotobuf.Model.DynamicValueType
import org.cangnova.cangjie.protodebugger.data.LLDBFrame
import org.cangnova.cangjie.protodebugger.data.LLDBThread
import org.cangnova.cangjie.protodebugger.data.LLDBVariable
import org.cangnova.cangjie.protodebugger.data.LLDBValue
import org.cangnova.cangjie.protodebugger.result.PagedResult

/**
 * 表达式求值服务接口
 *
 * 负责表达式求值、变量读取和数据获取
 */
interface EvalService {
    /**
     * 在指定上下文中求值表达式
     *
     * @param thread 目标线程
     * @param frame 目标栈帧
     * @param expression 表达式字符串
     * @return 求值结果
     */
    suspend fun evaluate(
        thread: LLDBThread,
        frame: LLDBFrame,
        expression: String
    ): LLDBVariable

    /**
     * 通过值ID和索引求值
     *
     * @param valueId 值ID
     * @param index 索引
     * @param expression 表达式字符串
     * @return 求值结果
     */
    suspend fun evaluate(
        valueId: Long,
        index: Int,
        expression: String
    ): LLDBVariable

    /**
     * 获取变量列表
     *
     * @param thread 目标线程
     * @param frame 目标栈帧
     * @return 变量列表
     */
    suspend fun getVariables(
        thread: LLDBThread,
        frame: LLDBFrame,
        includeArgument: Boolean = true,
        includeStatics: Boolean = true,
        includeLocals: Boolean = true,
        inScopeOnly: Boolean = true,
        includeRuntimeSupportValues: Boolean = true,
        useDynamic: DynamicValueType = DynamicValueType.DYNAMIC_VALUE_NONE,
        includeRecognizedArguments: Boolean = true,
    ): List<LLDBVariable>

    /**
     * 获取变量（包含静态和全局变量选项）
     *
     * @param threadId 线程ID
     * @param frameIndex 栈帧索引
     * @param statics 是否包含静态变量
     * @param globals 是否包含全局变量
     * @return 变量列表
     */
    suspend fun getVariables(
        threadId: Long,
        frameIndex: Int,
        includeArgument: Boolean = true,
        includeStatics: Boolean = true,
        includeLocals: Boolean = true,
        inScopeOnly: Boolean = true,
        includeRuntimeSupportValues: Boolean = true,
        useDynamic: DynamicValueType = DynamicValueType.DYNAMIC_VALUE_NONE,
        includeRecognizedArguments: Boolean = true,
    ): List<LLDBVariable>

    /**
     * 获取变量的子元素
     *
     * @param value 父变量
     * @param from 起始索引
     * @param count 数量
     * @return 子元素列表
     */
    suspend fun getVariableChildren(
        value: LLDBVariable,
        from: Int = 0,
        count: Int = 1000
    ): PagedResult<LLDBVariable>



    /**
     * 获取子元素数量
     *
     * @param value 变量
     * @return 子元素数量
     */
    suspend fun getChildrenCount(value: LLDBVariable): Int

    


    /**
     * 获取值的详细信息
     *
     * 获取用于UI展示的值数据，包括字符串表示、描述、类型信息等。
     * 支持缓存机制，提高重复访问性能。
     *
     * @param value 变量
     * @param useCache 是否使用缓存，默认为true
     * @return 值的详细信息
     */
    suspend fun getValue(
        variable: LLDBVariable,

    ): LLDBValue


    /**
     * 修改变量值
     *
     * @param value 要修改的变量
     * @param newValue 新的值（字符串表示）
     * @return 修改是否成功
     */
    suspend fun setValue(
        value: LLDBVariable,
        newValue: String
    ): Boolean

    /**
     * 检查变量是否可修改
     *
     * @param value 变量
     * @return 如果变量可修改返回true，否则返回false
     */
    suspend fun isMutable(value: LLDBVariable): Boolean

    /**
     * 设置值过滤是否启用
     *
     * @param enabled 是否启用
     */
    suspend fun setValuesFilteringEnabled(enabled: Boolean)
}
