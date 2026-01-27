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

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.resolve.calls.components.CreateFreshVariablesSubstitutor.shouldBeFlexible
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeVariableMarker
import org.cangnova.cangjie.types.unCapture as unCaptureCangJieType

/**
 * 经典约束系统工具上下文类
 *
 * 该类实现了约束系统工具上下文接口，用于在类型推断过程中处理各种约束。
 * 主要负责创建类型变量、处理Lambda表达式和可调用引用的类型信息。
 *
 * @property cangjieTypeRefiner 仓颉类型精化器，用于类型精化操作
 * @property builtIns 仓颉内置类型集合，提供基础类型定义
 */
class ClassicConstraintSystemUtilContext(
    val cangjieTypeRefiner: CangJieTypeRefiner,
    val builtIns: CangJieBuiltIns,
) : ConstraintSystemUtilContext {

    // 以下是已注释掉的代码，保留用于未来可能的扩展
    //    override fun TypeVariableMarker.shouldBeFlexible(): Boolean {
    //        return this is TypeVariableFromCallableDescriptor && this.originalTypeParameter.shouldBeFlexible()
    //    }
    //

    /**
     * 判断类型变量是否只具有输入类型属性
     *
     * @receiver TypeVariableMarker 类型变量标记
     * @return Boolean 如果类型变量只有输入类型注解则返回true
     */
    override fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean {
        require(this is NewTypeVariable)
        return hasOnlyInputTypesAnnotation()
    }

    /**
     * 移除类型的捕获标记，返回未捕获的类型
     *
     * @receiver CangJieTypeMarker 仓颉类型标记
     * @return CangJieTypeMarker 未捕获的仓颉类型标记
     */
    override fun CangJieTypeMarker.unCapture(): CangJieTypeMarker {
        require(this is CangJieType)
        return unCaptureCangJieType().unwrap()
    }

    /**
     * 为Lambda表达式的返回类型创建类型变量
     *
     * @return TypeVariableMarker 代表Lambda返回类型的类型变量
     */
    override fun createTypeVariableForLambdaReturnType(): TypeVariableMarker {
        return TypeVariableForLambdaReturnType(
            builtIns,
            TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE
        )
    }

    /**
     * 为可调用引用的返回类型创建类型变量
     *
     * @return TypeVariableMarker 代表可调用引用返回类型的类型变量
     */
    override fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker {
        return TypeVariableForCallableReferenceReturnType(
            builtIns,
            TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE
        )
    }

    /**
     * 判断类型变量是否应该是灵活的（可变的）
     *
     * @receiver TypeVariableMarker 类型变量标记
     * @return Boolean 如果类型变量来自可调用描述符且其原始类型参数应该灵活则返回true
     */
    override fun TypeVariableMarker.shouldBeFlexible(): Boolean {
        return this is TypeVariableFromCallableDescriptor && this.originalTypeParameter.shouldBeFlexible()
    }

    /**
     * 从声明中提取Lambda表达式的参数类型列表
     *
     * @param declaration 延迟处理的、具有可修订预期类型的原子
     * @return List<CangJieTypeMarker?>? 参数类型列表，对于函数表达式会包含接收者类型（如果存在）
     *                                    如果无法提取则返回null
     */
    override fun extractLambdaParameterTypesFromDeclaration(declaration: PostponedAtomWithRevisableExpectedType): List<CangJieTypeMarker?>? {
        require(declaration is ResolvedAtom)
        return when (val atom = declaration.atom) {
            // 对于函数表达式，如果有接收者类型，将其添加到参数类型列表的开头
            is FunctionExpression -> {
                val receiverType = atom.receiverType
                if (receiverType != null) listOf(receiverType) + atom.parametersTypes else atom.parametersTypes.toList()
            }
            // 对于Lambda调用参数，直接返回参数类型列表
            is LambdaCangJieCallArgument -> atom.parametersTypes?.toList()
            else -> null
        }
    }

    /**
     * 为参数创建约束位置
     *
     * @param argument 延迟处理的、具有可修订预期类型的原子
     * @return ArgumentConstraintPosition<*> 参数约束位置对象
     */
    override fun createArgumentConstraintPosition(argument: PostponedAtomWithRevisableExpectedType): ArgumentConstraintPosition<*> {
        require(argument is ResolvedAtom)
        return ArgumentConstraintPositionImpl(argument.atom as CangJieCallArgument)
    }

    /**
     * 为Lambda表达式的参数类型创建类型变量
     *
     * @param argument 延迟处理的、具有可修订预期类型的原子
     * @param index 参数的索引位置
     * @return TypeVariableMarker 代表Lambda参数类型的类型变量
     */
    override fun createTypeVariableForLambdaParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker {
        require(argument is ResolvedAtom)
        val atom = argument.atom as PostponableCangJieCallArgument
        return TypeVariableForLambdaParameterType(
            atom,
            index,
            builtIns,
            TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + (index + 1)
        )
    }

    /**
     * 为可调用引用的参数类型创建类型变量
     *
     * @param argument 延迟处理的、具有可修订预期类型的原子
     * @param index 参数的索引位置
     * @return TypeVariableMarker 代表可调用引用参数类型的类型变量
     */
    override fun createTypeVariableForCallableReferenceParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker {
        return TypeVariableForCallableReferenceParameterType(
            builtIns,
            TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE + (index + 1)
        )
    }

    /**
     * 判断延迟原子是否为函数表达式
     *
     * @receiver PostponedAtomWithRevisableExpectedType 延迟处理的原子
     * @return Boolean 如果是函数表达式则返回true
     */
    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpression(): Boolean {
        require(this is ResolvedAtom)
        return this.atom is FunctionExpression
    }

    /**
     * 判断延迟原子是否为带接收者的函数表达式
     *
     * @receiver PostponedAtomWithRevisableExpectedType 延迟处理的原子
     * @return Boolean 如果是带接收者的函数表达式则返回true
     */
    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpressionWithReceiver(): Boolean {
        require(this is ResolvedAtom)
        val atom = this.atom
        return atom is FunctionExpression && atom.receiverType != null
    }

    /**
     * 判断延迟原子是否为Lambda表达式（非函数表达式）
     *
     * @receiver PostponedAtomWithRevisableExpectedType 延迟处理的原子
     * @return Boolean 如果是Lambda调用参数但不是函数表达式则返回true
     */
    override fun PostponedAtomWithRevisableExpectedType.isLambda(): Boolean {
        require(this is ResolvedAtom)
        val atom = this.atom
        return atom is LambdaCangJieCallArgument && atom !is FunctionExpression
    }

    /**
     * 创建固定变量约束位置
     *
     * @param T 原子的泛型类型
     * @param variable 需要固定的类型变量
     * @param atom 相关的原子对象
     * @return FixVariableConstraintPosition<T> 固定变量约束位置对象
     */
    override fun <T> createFixVariableConstraintPosition(
        variable: TypeVariableMarker,
        atom: T
    ): FixVariableConstraintPosition<T> {
        require(atom is ResolvedAtom)
        @Suppress("UNCHECKED_CAST")
        return FixVariableConstraintPositionImpl(variable, atom) as FixVariableConstraintPosition<T>
    }
}