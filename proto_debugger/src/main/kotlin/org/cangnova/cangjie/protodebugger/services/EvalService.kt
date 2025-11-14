package org.cangnova.cangjie.protodebugger.services

import org.cangnova.cangjie.protodebugger.data.LLFrame
import org.cangnova.cangjie.protodebugger.data.LLThread
import org.cangnova.cangjie.protodebugger.data.LLValue
import org.cangnova.cangjie.protodebugger.data.LLValueData
import org.cangnova.cangjie.protodebugger.output.ResultList

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
        thread: LLThread,
        frame: LLFrame,
        expression: String
    ): LLValue

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
    ): LLValue

    /**
     * 获取局部变量
     *
     * @param thread 目标线程
     * @param frame 目标栈帧
     * @return 变量列表
     */
    suspend fun getVariables(
        thread: LLThread,
        frame: LLFrame
    ): List<LLValue>

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
        statics: Boolean = false,
        globals: Boolean = false
    ): List<LLValue>

    /**
     * 获取变量的子元素
     *
     * @param value 父变量
     * @param from 起始索引
     * @param count 数量
     * @return 子元素列表
     */
    suspend fun getVariableChildren(
        value: LLValue,
        from: Int,
        count: Int
    ): ResultList<LLValue>

    /**
     * 获取子元素数量
     *
     * @param value 变量
     * @return 子元素数量
     */
    suspend fun getChildrenCount(value: LLValue): Int

    /**
     * 获取变量数据
     *
     * @param value 变量
     * @return 变量数据
     */
    suspend fun getData(value: LLValue): LLValueData

    /**
     * 获取变量描述
     *
     * @param value 变量
     * @param maxLength 最大长度
     * @return 描述字符串
     */
    suspend fun getDescription(value: LLValue, maxLength: Int): String

    /**
     * 获取值的内存地址
     *
     * @param value 变量
     * @return 内存地址
     */
    fun getValueAddress(value: LLValue): Long

    /**
     * 设置值过滤是否启用
     *
     * @param enabled 是否启用
     */
    suspend fun setValuesFilteringEnabled(enabled: Boolean)
}
