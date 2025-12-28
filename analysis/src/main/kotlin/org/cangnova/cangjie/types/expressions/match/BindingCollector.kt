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
 */

package org.cangnova.cangjie.types.expressions.match

import org.cangnova.cangjie.descriptors.DescriptorVisibility
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjCasePatternElement
import org.cangnova.cangjie.psi.CjTypePattern
import org.cangnova.cangjie.resolve.LocalVariableResolver
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.ExpressionTypingComponents

/**
 * 绑定收集器（BindingCollector）
 *
 * 负责从模式中收集所有变量绑定，并在作用域中声明它们。
 * 统一处理绑定模式和类型模式中的变量声明逻辑。
 *
 * ## 核心职责
 *
 * 1. **收集绑定**: 从 Pattern 中提取所有 PatternBinding
 * 2. **声明变量**: 在适当的作用域中创建变量描述符
 * 3. **类型推导**: 为绑定变量确定正确的类型
 *
 * ## 使用场景
 *
 * ```cangjie
 * // match 表达式中的绑定
 * match (opt) {
 *     case Some(x) => ...  // 收集绑定 x
 *     case None => ...
 * }
 *
 * // let 声明中的解构
 * let (a, b) = tuple  // 收集绑定 a, b
 *
 * // for-in 循环中的解构
 * for ((k, v) in map) { ... }  // 收集绑定 k, v
 * ```
 *
 * ## 与 PatternResolver 的关系
 *
 * ```
 * PSI Element → PatternResolver → Pattern → BindingCollector → PatternBinding[]
 *                                                    ↓
 *                                             LexicalScope.addVariable()
 * ```
 *
 * @see PatternBinding
 * @see PatternResolver
 * @see PatternAnalyzer
 */
class BindingCollector(private val components: ExpressionTypingComponents) {

    /**
     * 从模式中收集所有变量绑定
     *
     * 递归遍历模式树，收集所有绑定模式和类型模式中的变量。
     *
     * @param pattern 要分析的模式
     * @param context 模式分析上下文
     * @return 收集到的绑定列表
     */
    fun collectBindings(pattern: Pattern, context: ExtendedPatternContext): List<PatternBinding> {
        return collectBindingsFromPattern(pattern, context)
    }

    /**
     * 从 PSI 模式元素中收集绑定
     *
     * 直接从 PSI 元素分析，不需要先转换为 Pattern。
     * 用于兼容现有代码。
     *
     * @param element PSI 模式元素
     * @param subjectType 被匹配对象的类型
     * @param context 模式分析上下文
     * @return 收集到的绑定列表
     */
    fun collectBindingsFromPsi(
        element: CjCasePatternElement,
        subjectType: CangJieType,
        context: ExtendedPatternContext
    ): List<PatternBinding> {
        val bindings = mutableListOf<PatternBinding>()
        collectFromPsiElement(element, subjectType, context, bindings)
        return bindings
    }

    /**
     * 在作用域中声明绑定的变量
     *
     * @param bindings 要声明的绑定列表
     * @param scope 目标作用域
     * @param trace 绑定跟踪器
     * @param localVariableResolver 局部变量解析器
     * @param isLocal 是否为局部变量
     */
    fun declareBindings(
        bindings: List<PatternBinding>,
        scope: LexicalWritableScope,
        trace: BindingTrace,
        localVariableResolver: LocalVariableResolver,
        isLocal: Boolean = true
    ) {
        for (binding in bindings) {
            if (!binding.isValid) continue

            val descriptor = createVariableDescriptor(
                binding, scope, trace, localVariableResolver, isLocal
            )
            if (descriptor != null) {
                scope.addVariableDescriptor(descriptor)
            }
        }
    }

    /**
     * 收集并声明变量（组合操作）
     *
     * 便捷方法，同时完成收集和声明。
     *
     * @param pattern 要分析的模式
     * @param context 模式分析上下文
     * @param scope 目标作用域
     * @param trace 绑定跟踪器
     * @param localVariableResolver 局部变量解析器
     * @param isLocal 是否为局部变量
     * @return 收集到的绑定列表
     */
    fun collectAndDeclare(
        pattern: Pattern,
        context: ExtendedPatternContext,
        scope: LexicalWritableScope,
        trace: BindingTrace,
        localVariableResolver: LocalVariableResolver,
        isLocal: Boolean = true
    ): List<PatternBinding> {
        val bindings = collectBindings(pattern, context)
        declareBindings(bindings, scope, trace, localVariableResolver, isLocal)
        return bindings
    }

    // ===== 私有辅助方法 =====

    /**
     * 根据 Pattern 对象收集绑定
     *
     * 使用 Pattern 中的 psiElement 作为绑定的 element，确保正确的 PSI 引用。
     */
    private fun collectBindingsFromPattern(
        pattern: Pattern,
        context: ExtendedPatternContext
    ): List<PatternBinding> {
        val kind = pattern.kind
        val psiElement = pattern.psiElement

        return when (kind) {
            // 通配符和错误模式不引入绑定
            is PatternKind.Wild, is PatternKind.Error -> emptyList()

            // 常量模式不引入绑定
            is PatternKind.Const -> emptyList()

            // 绑定模式：引入一个变量
            is PatternKind.Binding -> {
                // 使用 Pattern 中保存的 PSI 元素，而不是 subject.element
                val element = psiElement ?: return emptyList()
                listOf(
                    PatternBinding(
                        name = kind.name,
                        type = kind.type,
                        element = element,
                        isMutable = false
                    )
                )
            }

            // 类型模式：引入一个类型标注的变量
            is PatternKind.Type -> {
                // 使用 Pattern 中保存的 PSI 元素
                val element = psiElement ?: return emptyList()
                listOf(
                    PatternBinding(
                        name = kind.name,
                        type = kind.type,
                        element = element,
                        isMutable = false
                    )
                )
            }

            // 元组模式：递归收集子模式的绑定
            is PatternKind.Tuple -> {
                kind.subPatterns.flatMap { subPattern ->
                    collectBindingsFromPattern(subPattern, context)
                }
            }

            // 枚举模式：递归收集子模式的绑定
            is PatternKind.Enum -> {
                kind.subPatterns.flatMap { subPattern ->
                    collectBindingsFromPattern(subPattern, context)
                }
            }
        }
    }

    /**
     * 从 PSI 元素中收集绑定
     */
    private fun collectFromPsiElement(
        element: CjCasePatternElement,
        subjectType: CangJieType,
        context: ExtendedPatternContext,
        bindings: MutableList<PatternBinding>
    ) {
        when (element) {
            is CjBindingPattern -> {
                val name = element.name ?: return
                bindings.add(
                    PatternBinding(
                        name = name,
                        type = subjectType,
                        element = element,
                        isMutable = false
                    )
                )
            }

            is CjTypePattern -> {
                val name = element.name ?: return
                val typeRef = element.typeReference
                val declaredType = typeRef?.let {
                    components.typeResolver.resolveType(
                        context.typingContext.scope,
                        it,
                        context.typingContext.trace,
                        false
                    )
                } ?: subjectType

                bindings.add(
                    PatternBinding(
                        name = name,
                        type = declaredType,
                        element = element,
                        isMutable = false
                    )
                )
            }

            // 其他模式类型可以根据需要扩展
        }
    }

    /**
     * 创建变量描述符
     */
    private fun createVariableDescriptor(
        binding: PatternBinding,
        scope: LexicalScope,
        trace: BindingTrace,
        localVariableResolver: LocalVariableResolver,
        isLocal: Boolean
    ): VariableDescriptor? {
        val element = binding.element

        return if (isLocal) {
            when (element) {
                is CjBindingPattern -> localVariableResolver.resolveLocalVariableDescriptorWithType(
                    scope, element, binding.type, trace, binding.isMutable
                )
                is CjTypePattern -> localVariableResolver.resolveLocalVariableDescriptorWithType(
                    scope, element, binding.type, trace, binding.isMutable
                )
                else -> null
            }
        } else {
            when (element) {
                is CjBindingPattern -> localVariableResolver.resolveVariableDescriptorWithType(
                    scope, element, binding.type, trace, binding.isMutable, binding.visibility
                )
                is CjTypePattern -> localVariableResolver.resolveVariableDescriptorWithType(
                    scope, element, binding.type, trace, binding.isMutable, binding.visibility
                )
                else -> null
            }
        }
    }

    companion object {
        /**
         * 检查模式是否引入任何变量绑定
         */
        fun hasBindings(pattern: Pattern): Boolean {
            return when (val kind = pattern.kind) {
                is PatternKind.Wild, is PatternKind.Error, is PatternKind.Const -> false
                is PatternKind.Binding, is PatternKind.Type -> true
                is PatternKind.Tuple -> kind.subPatterns.any { hasBindings(it) }
                is PatternKind.Enum -> kind.subPatterns.any { hasBindings(it) }
            }
        }

        /**
         * 获取模式中所有绑定的名称
         */
        fun getBindingNames(pattern: Pattern): Set<String> {
            val names = mutableSetOf<String>()
            collectBindingNames(pattern.kind, names)
            return names
        }

        private fun collectBindingNames(kind: PatternKind, names: MutableSet<String>) {
            when (kind) {
                is PatternKind.Binding -> names.add(kind.name)
                is PatternKind.Type -> names.add(kind.name)
                is PatternKind.Tuple -> kind.subPatterns.forEach { collectBindingNames(it.kind, names) }
                is PatternKind.Enum -> kind.subPatterns.forEach { collectBindingNames(it.kind, names) }
                else -> { /* no bindings */ }
            }
        }
    }
}
