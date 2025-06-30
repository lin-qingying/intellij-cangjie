/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.diagnostics

import cn.cangnova.cangjie.diagnostics.rendering.DiagnosticRenderer

/**
 * 诊断工厂抽象基类
 * 
 * 用于创建和管理诊断信息，提供诊断名称、严重程度和渲染器的管理
 *
 * @param D 未绑定诊断类型
 * @param _name 诊断名称，可为空
 * @param severity 诊断严重程度
 */
abstract class DiagnosticFactory<D : UnboundDiagnostic> protected constructor(
    private var _name: String?,
    open val severity: Severity
){
    /**
     * 获取诊断名称
     * 
     * 如果名称未设置，则返回"<unnamed>"
     */
    open val name: String
        get() = _name ?: "<unnamed>"
    
    /**
     * 初始化诊断名称
     *
     * @param name 要设置的名称
     */
    fun initializeName(name: String) {
        _name = name
    }
    
    /**
     * 默认诊断渲染器
     */
    open var defaultRenderer: DiagnosticRenderer<D>? = null

    /**
     * 仅使用严重程度的构造函数
     *
     * @param severity 诊断严重程度
     */
    protected constructor(severity: Severity) : this(null, severity)

    /**
     * 初始化默认渲染器
     *
     * @param defaultRenderer 默认渲染器
     */
    @Suppress("UNCHECKED_CAST")
    fun initDefaultRenderer(defaultRenderer: DiagnosticRenderer<*>?) {
        this.defaultRenderer = defaultRenderer as DiagnosticRenderer<D>?
    }
    
    /**
     * 将通用诊断转换为特定类型
     *
     * @param diagnostic 要转换的诊断
     * @return 转换后的特定类型诊断
     * @throws IllegalArgumentException 如果工厂不匹配
     */
    fun cast(diagnostic: UnboundDiagnostic): D {
        require(diagnostic.factory === this) { "Factory mismatch: expected " + this + " but was " + diagnostic.factory }
        @Suppress("UNCHECKED_CAST")
        return diagnostic as D
    }
    
    /**
     * 返回诊断工厂的字符串表示
     *
     * @return 诊断工厂名称或"<Anonymous DiagnosticFactory>"
     */
    override fun toString(): String {
        return _name ?: "<Anonymous DiagnosticFactory>"
    }
    
    companion object {
        /**
         * 将诊断转换为特定类型（使用多个工厂）
         *
         * @param D 目标诊断类型
         * @param diagnostic 要转换的诊断
         * @param factories 可能的工厂列表
         * @return 转换后的特定类型诊断
         * @throws IllegalArgumentException 如果没有匹配的工厂
         */
        @SafeVarargs
        fun <D : UnboundDiagnostic> cast(diagnostic: UnboundDiagnostic, vararg factories: DiagnosticFactory<out D>): D {
            return cast(diagnostic, listOf(*factories))
        }

        /**
         * 将诊断转换为特定类型（使用工厂集合）
         *
         * @param D 目标诊断类型
         * @param diagnostic 要转换的诊断
         * @param factories 可能的工厂集合
         * @return 转换后的特定类型诊断
         * @throws IllegalArgumentException 如果没有匹配的工厂
         */
        fun <D : UnboundDiagnostic> cast(diagnostic: UnboundDiagnostic, factories: Collection<DiagnosticFactory<out D>>): D {
            for (factory in factories) {
                if (diagnostic.factory === factory) return factory.cast(diagnostic)
            }
            throw IllegalArgumentException("Factory mismatch: expected one of " + factories + " but was " + diagnostic.factory)
        }
    }
}
