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

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.VariableWithConstraints
import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtom
import org.cangnova.cangjie.types.AbstractTypeChecker
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.model.*
import org.cangnova.cangjie.utils.newLinkedHashMapWithExpectedSize
import java.util.*

/**
 * 约束系统完成器上下文的类型别名
 */
typealias CsCompleterContext = ConstraintSystemCompletionContext

/**
 * 完成模式计算器
 *
 * 该类负责计算类型推断过程中的约束系统完成模式,决定是执行完整的类型推断还是部分推断。
 * 完成模式的选择影响类型推断的精确度和性能。
 */
class CompletionModeCalculator {
    companion object {
        /**
         * 计算约束系统的完成模式
         *
         * @param candidate 解析候选项,包含待解析的调用信息
         * @param expectedType 期望的类型,通常来自外部上下文
         * @param returnType 调用的返回类型
         * @param trivialConstraintTypeInferenceOracle 简单约束类型推断预言器,用于判断类型是否合适
         * @param inferenceSession 推断会话,管理推断状态
         * @return 计算得到的约束系统完成模式(完整或部分)
         */
        fun computeCompletionMode(
            candidate: ResolutionCandidate,
            expectedType: UnwrappedType?,
            returnType: UnwrappedType?,
            trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
            inferenceSession: InferenceSession
        ): ConstraintSystemCompletionMode = with(candidate) {
            // 如果推断会话已经计算过完成模式,直接返回
            inferenceSession.computeCompletionMode(candidate)?.let { return it }

            // 将约束系统转换为完成器上下文
            val csCompleterContext = getSystem().asConstraintSystemCompleterContext()

            // 如果候选项是错误的,返回完整模式以获取更多错误信息
            if (candidate.isErrorCandidate()) return ConstraintSystemCompletionMode.FULL

            // 如果存在期望类型,说明正在完成最外层调用,应该使用完整模式
            if (expectedType != null) return ConstraintSystemCompletionMode.FULL

            // 如果返回类型为空,这通常只发生在错误调用中,返回部分模式
            if (returnType == null) return ConstraintSystemCompletionMode.PARTIAL

            // 如果返回类型是固有类型(不包含类型变量),使用完整模式
            if (getSystem().getBuilder().isProperType(returnType)) return ConstraintSystemCompletionMode.FULL

            // 对于返回类型中包含变量的嵌套调用,检查是否可以完整完成
            return CalculatorForNestedCall(
                candidate, returnType, csCompleterContext, trivialConstraintTypeInferenceOracle
            ).computeCompletionMode()
        }
    }

    /**
     * 嵌套调用的计算器
     *
     * 用于处理嵌套函数调用的完成模式计算。嵌套调用的特殊性在于其返回类型
     * 可能包含类型变量,需要更仔细地分析约束条件来决定完成模式。
     */
    private class CalculatorForNestedCall(
        private val candidate: ResolutionCandidate,
        private val returnType: UnwrappedType?,
        private val csCompleterContext: CsCompleterContext,
        private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    ) {
        /**
         * 固定方向枚举
         * 定义类型变量的固定方向,用于确定类型推断的方向
         */
        private enum class FixationDirection {
            /** 向子类型方向固定 */
            TO_SUBTYPE,
            /** 相等方向固定(不变) */
            EQUALITY
        }

        /**
         * 类型变量的固定方向映射表
         * 记录每个类型变量应该朝哪个方向进行类型推断
         */
        private val fixationDirectionsForVariables: MutableMap<VariableWithConstraints, FixationDirection> =
            newLinkedHashMapWithExpectedSize(csCompleterContext.notFixedTypeVariables.size)

        /**
         * 已入队约束的变量集合
         * 用于避免重复处理同一个变量的约束
         */
        private val variablesWithQueuedConstraints = mutableSetOf<TypeVariableMarker>()

        /**
         * 待处理的类型队列
         * 使用广度优先的方式处理类型及其组成部分
         */
        private val typesToProcess: Queue<CangJieTypeMarker> = ArrayDeque()

        /**
         * 延迟的原子解析列表
         * 包含尚未完全分析的参数,按顺序排列
         */
        private val postponedAtoms: List<PostponedResolvedAtom> by lazy {
            CangJieConstraintSystemCompleter.getOrderedNotAnalyzedPostponedArguments(listOf(candidate.resolvedCall))
        }

        /**
         * 计算完成模式
         *
         * @return 根据类型变量的约束条件确定的完成模式
         */
        fun computeCompletionMode(): ConstraintSystemCompletionMode = with(csCompleterContext) {
            // 将返回类型加入待处理队列
            typesToProcess.add(returnType)
            // 计算所有类型变量的固定方向
            computeDirections()

            // 如果所有变量都有所需的固有约束,执行完整完成
            if (directionRequirementsForVariablesHold())
                return ConstraintSystemCompletionMode.FULL

            // 否则执行部分完成
            return ConstraintSystemCompletionMode.PARTIAL
        }

        /**
         * 计算类型变量的固定方向
         *
         * 通过分析类型结构和约束条件,确定每个类型变量应该朝哪个方向进行推断
         */
        private fun CsCompleterContext.computeDirections() {
            while (typesToProcess.isNotEmpty()) {
                val type = typesToProcess.poll() ?: break

                // 如果类型不包含未固定的类型变量,跳过
                if (!type.contains { it.typeConstructor() in notFixedTypeVariables })
                    continue

                val fixationDirectionsFromType = mutableSetOf<FixationDirectionForVariable>()
                // 仓颉语言的所有类型参数都是不变的(invariant),直接收集需要固定的变量
                collectRequiredDirectionsForVariables(type, fixationDirectionsFromType)

                // 更新每个变量的固定方向,并将相关约束中的类型加入处理队列
                for (directionForVariable in fixationDirectionsFromType) {
                    updateDirection(directionForVariable)
                    enqueueTypesFromConstraints(directionForVariable.variable)
                }
            }
        }

        /**
         * 将变量的约束中的类型加入处理队列
         *
         * @param variableWithConstraints 带约束的类型变量
         */
        private fun enqueueTypesFromConstraints(variableWithConstraints: VariableWithConstraints) {
            val variable = variableWithConstraints.typeVariable
            // 避免重复处理同一个变量
            if (variable !in variablesWithQueuedConstraints) {
                // 将该变量的所有约束类型加入待处理队列
                for (constraint in variableWithConstraints.constraints) {
                    typesToProcess.add(constraint.type)
                }

                variablesWithQueuedConstraints.add(variable)
            }
        }

        /**
         * 检查所有变量的方向要求是否满足
         *
         * @return 如果所有变量都有满足其固定方向的固有约束则返回true
         */
        private fun CsCompleterContext.directionRequirementsForVariablesHold(): Boolean {
            for ((variable, fixationDirection) in fixationDirectionsForVariables) {
                if (!hasProperConstraint(variable, fixationDirection))
                    return false
            }
            return true
        }

        /**
         * 更新变量的固定方向
         *
         * 如果一个变量从多个方向被约束,最终会被标记为EQUALITY(相等方向)
         *
         * @param directionForVariable 变量及其新的固定方向
         */
        private fun updateDirection(directionForVariable: FixationDirectionForVariable) {
            val (variable, newDirection) = directionForVariable
            fixationDirectionsForVariables[variable]?.let { oldDirection ->
                // 如果旧方向不是EQUALITY且与新方向不同,则更新为EQUALITY
                if (oldDirection != FixationDirection.EQUALITY && oldDirection != newDirection)
                    fixationDirectionsForVariables[variable] = FixationDirection.EQUALITY
            } ?: run {
                // 如果是首次设置,直接使用新方向
                fixationDirectionsForVariables[variable] = newDirection
            }
        }

        /**
         * 变量的固定方向数据类
         *
         * @property variable 带约束的类型变量
         * @property direction 该变量的固定方向
         */
        private data class FixationDirectionForVariable(
            val variable: VariableWithConstraints,
            val direction: FixationDirection
        )

        /**
         * 收集类型中所有变量需要的固定方向
         *
         * 递归分析类型结构,为每个类型变量确定其固定方向
         *
         * @param type 要分析的类型
         * @param fixationDirectionsCollector 用于收集固定方向的集合
         */
        private fun CsCompleterContext.collectRequiredDirectionsForVariables(
            type: CangJieTypeMarker,
            fixationDirectionsCollector: MutableSet<FixationDirectionForVariable>
        ) {
            val typeArgumentsCount = type.argumentsCount()
            val typeConstructor = type.typeConstructor()

            // 如果类型有类型参数
            if (typeArgumentsCount > 0 && typeArgumentsCount == typeConstructor.parametersCount()) {
                for (position in 0 until typeArgumentsCount) {
                    val argument = type.getArgument(position)

                    // 仓颉语言所有类型参数都是不变的,递归处理参数类型
                    collectRequiredDirectionsForVariables(
                        argument.getType(),
                        fixationDirectionsCollector
                    )
                }
            } else {
                // 处理没有类型参数的类型(如类型变量本身)
                processTypeWithoutParameters(type, fixationDirectionsCollector)
            }
        }

        /**
         * 处理没有类型参数的类型
         *
         * 对于类型变量,直接设置其固定方向为EQUALITY(因为仓颉语言类型参数是不变的)
         *
         * @param type 要处理的类型
         * @param newRequirementsCollector 用于收集新要求的集合
         */
        private fun CsCompleterContext.processTypeWithoutParameters(
            type: CangJieTypeMarker,
            newRequirementsCollector: MutableSet<FixationDirectionForVariable>
        ) {
            val variableWithConstraints = notFixedTypeVariables[type.typeConstructor()] ?: return
            // 仓颉语言所有类型参数都是不变的,固定方向为EQUALITY
            val direction = FixationDirection.EQUALITY
            val requirement = FixationDirectionForVariable(variableWithConstraints, direction)
            newRequirementsCollector.add(requirement)
        }

        /**
         * 检查变量是否有满足指定方向的固有约束
         *
         * 固有约束是指约束类型本身不包含未固定的类型变量
         *
         * @param variableWithConstraints 带约束的类型变量
         * @param direction 需要检查的固定方向
         * @return 如果存在满足条件的固有约束则返回true
         */
        private fun CsCompleterContext.hasProperConstraint(
            variableWithConstraints: VariableWithConstraints,
            direction: FixationDirection
        ): Boolean {
            val constraints = variableWithConstraints.constraints
            val variable = variableWithConstraints.typeVariable

            // 需要跟踪整数字面量类型(ILT)约束,以防止Nothing约束导致错误的完整完成
            // 考虑这种情况: ILT <: T; Nothing <: T,其中T需要下界约束
            // Nothing会触发完整完成,但结果类型会是Int
            // 来自外部调用的整数常量限制会被忽略

            var iltConstraintPresent = false  // 是否存在整数字面量类型约束
            var properConstraintPresent = false  // 是否存在固有约束
            var nonNothingProperConstraintPresent = false  // 是否存在非Nothing的固有约束

            for (constraint in constraints) {
                // 跳过不满足方向要求或不是固有类型的约束
                if (!constraint.hasRequiredKind(direction) || !isProperType(constraint.type))
                    continue

                if (constraint.type.typeConstructor().isIntegerLiteralTypeConstructor()) {
                    // 整数字面量类型约束
                    iltConstraintPresent = true
                } else if (trivialConstraintTypeInferenceOracle.isSuitableResultedType(constraint.type)) {
                    // 合适的结果类型(非Nothing)
                    properConstraintPresent = true
                    nonNothingProperConstraintPresent = true
                } else if (!isLowerConstraintForPartiallyAnalyzedVariable(constraint, variable)) {
                    // 其他固有约束
                    properConstraintPresent = true
                }
            }

            // 如果没有固有约束,返回false
            if (!properConstraintPresent) return false

            // 如果存在整数字面量类型约束,必须同时存在非Nothing的固有约束
            return !iltConstraintPresent || nonNothingProperConstraintPresent
        }

        /**
         * 检查约束是否满足指定方向的要求
         *
         * @param direction 固定方向
         * @return 如果约束种类满足方向要求则返回true
         */
        private fun Constraint.hasRequiredKind(direction: FixationDirection) = when (direction) {
            FixationDirection.TO_SUBTYPE -> kind.isLower() || kind.isEqual()  // 子类型方向需要下界或相等约束
            FixationDirection.EQUALITY -> kind.isEqual()  // 相等方向只需要相等约束
        }

        /**
         * 判断约束是否是部分分析变量的下界约束
         *
         * 部分分析变量是指在延迟原子中作为期望类型出现的变量
         *
         * @param constraint 要检查的约束
         * @param variable 类型变量
         * @return 如果是部分分析变量的下界约束则返回true
         */
        private fun CsCompleterContext.isLowerConstraintForPartiallyAnalyzedVariable(
            constraint: Constraint,
            variable: TypeVariableMarker
        ): Boolean {
            val defaultType = variable.defaultType()
            // 检查是否是下界约束,且该变量的默认类型出现在延迟原子的期望类型中
            return constraint.kind.isLower() && postponedAtoms.any { atom ->
                atom.expectedType?.contains { type -> defaultType == type } ?: false
            }
        }
    }
}