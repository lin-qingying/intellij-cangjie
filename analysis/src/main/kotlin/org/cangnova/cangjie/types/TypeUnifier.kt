/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.utils.newHashMapWithExpectedSize
import org.cangnova.cangjie.utils.newHashSetWithExpectedSize
import java.util.function.Predicate

/**
 * 类型统一器（Type Unifier）
 *
 * 用于在类型推导过程中进行类型统一（Unification）操作。
 * 类型统一是指找到一个类型替换（substitution），使得两个类型表达式相等。
 */
object TypeUnifier {

    /**
     * 统一结果接口
     *
     * 表示类型统一操作的结果，包含统一是否成功以及生成的类型替换映射。
     */
    interface UnificationResult {
        /** 统一是否成功 */
        val isSuccess: Boolean

        /** 类型替换映射：将类型构造器映射到具体的类型参数 */
        val substitution: Map<TypeConstructor, TypeArgument>
    }

    /**
     * 统一结果的内部实现类
     */
    private
    class UnificationResultImpl : UnificationResult {
        /** 统一是否成功，初始值为 true */
        override var isSuccess: Boolean = true
            private set

        /** 类型替换映射表，用于存储变量到类型的映射关系 */
        override val substitution: MutableMap<TypeConstructor, TypeArgument> =
            newHashMapWithExpectedSize(1)

        /** 失败的变量集合，记录那些无法统一的类型变量 */
        private val failedVariables: MutableSet<TypeConstructor> =
            newHashSetWithExpectedSize(0)

        /**
         * 标记统一失败
         */
        fun fail() {
            isSuccess = false
        }

        /**
         * 添加类型替换映射
         *
         * @param key 类型构造器（类型变量）
         * @param value 要替换成的类型参数
         *
         * 如果同一个变量被映射到不同的类型，则统一失败。
         */
        fun put(key: TypeConstructor, value: TypeArgument) {
            // 如果这个变量已经失败过，直接返回
            if (failedVariables.contains(key)) return

            // 尝试添加映射
            val oldValue: TypeArgument? = substitution.put(key, value)

            // 如果之前已有映射且与新值不同，则统一失败
            if (oldValue != null && oldValue != value) {
                substitution.remove(key)
                failedVariables.add(key)
                fail()
            }
        }
    }

    /**
     * 类型统一函数
     *
     * 寻找一个替换 S，使得 `projectWithVariables` 变成 `knownProjection`。
     *
     * 示例：
     * known = List<String>
     * withVariables = List<X>
     * variables = {X}
     *
     * 结果 = X -> String
     *
     * @param knownProjection 已知的具体类型
     * @param projectWithVariables 包含类型变量的类型
     * @param isVariable 判断一个类型构造器是否为类型变量的谓词
     * @return 统一结果，包含是否成功以及类型替换映射
     *
     * 只有被 `isVariable` 接受的类型才会被视为变量。
     */
    fun unify(
        knownProjection: TypeArgument,
        projectWithVariables: TypeArgument,
        isVariable: Predicate<TypeConstructor>
    ): UnificationResult {
        val result: UnificationResultImpl =
            UnificationResultImpl()
        doUnify(knownProjection, projectWithVariables, isVariable, result)
        return result
    }

    /**
     * 执行类型统一的内部递归函数
     *
     * @param knownProjection 已知的具体类型
     * @param projectWithVariables 包含类型变量的类型
     * @param isVariable 判断类型变量的谓词
     * @param result 累积的统一结果
     */
    private fun doUnify(
        knownProjection: TypeArgument,
        projectWithVariables: TypeArgument,
        isVariable: Predicate<TypeConstructor>,
        result: UnificationResultImpl
    ) {
        val known: CangJieType = knownProjection.type
        val withVariables: CangJieType = projectWithVariables.type

        // 规则1: Foo? ~ X?  =>  Foo ~ X
        // 如果两边都是可选类型，去掉可选标记后继续统一
        if (known.isOption && withVariables.isOption) {
            doUnify(
                TypeArgumentImpl(known.makeNonOption()),
                TypeArgumentImpl(withVariables.makeNonOption()),
                isVariable,
                result
            )
            return
        }

        // 规则2: Foo ~ X? => 失败
        // 如果已知类型不是可选的，但变量类型是可选的，则统一失败
        if (!known.isOption && withVariables.isOption) {
            result.fail()
            return
        }

        // 规则3: Foo ~ X  =>  X |-> Foo
        //       * ~ X   =>  X |-> *
        // 如果右边是类型变量，建立映射关系
        val maybeVariable: TypeConstructor = withVariables.constructor
        if (isVariable.test(maybeVariable)) {
            result.put(maybeVariable, knownProjection)
            return
        }

        // 规则4: Foo? ~ Foo 或 Foo ~ Bar => 失败
        // 检查结构不匹配：可选性不同或类型构造器不同
        val structuralMismatch =
            known.isOption != withVariables.isOption || known.constructor != withVariables.constructor
        if (structuralMismatch) {
            result.fail()
            return
        }

        // 规则5: Foo<A> ~ Foo<B, C> => 失败
        // 类型参数数量不匹配
        if (known.arguments.size != withVariables.arguments.size) {
            result.fail()
            return
        }

        // 规则6: Foo ~ Foo => 成功
        // 如果没有类型参数，直接成功
        if (known.arguments.isEmpty()) {
            return
        }

        // 规则7: Foo<...> ~ Foo<...>
        // 递归统一所有类型参数
        val knownArguments: List<TypeArgument> = known.arguments
        val withVariablesArguments: List<TypeArgument> = withVariables.arguments
        for (i in knownArguments.indices) {
            val knownArg: TypeArgument = knownArguments[i]
            val withVariablesArg: TypeArgument = withVariablesArguments[i]

            // 递归统一每一对类型参数
            doUnify(knownArg, withVariablesArg, isVariable, result)
        }
    }
}