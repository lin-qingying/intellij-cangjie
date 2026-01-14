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

package org.cangnova.cangjie.resolve.calls.inference.components


import com.intellij.util.SmartList
import org.cangnova.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.types.model.*
import org.cangnova.cangjie.utils.SmartSet

/**
 * 约束合并器
 *
 * 该类负责在类型推断过程中合并和传播约束条件。
 * 约束合并是类型推断的核心机制之一，通过传递性规则将不同类型变量之间的约束关系联系起来。
 *
 * 主要功能：
 * 1. 处理类型变量之间的传递性约束（如果 A <: α 且 α <: B，则 A <: B）
 * 2. 生成新的合并约束
 * 3. 处理捕获类型和类型近似
 *
 *
 * @property typeApproximator 类型近似器，用于将复杂类型近似为更简单的上界或下界
 * @property trivialConstraintTypeInferenceOracle 简单约束类型推断预言器，用于判断约束是否平凡
 * @property utilContext 约束系统工具上下文
 */
class ConstraintIncorporator(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    val utilContext: ConstraintSystemUtilContext
) {
    /**
     * 处理与类型变量直接相关的约束传递
     *
     * 传递性规则：A <:(=) α <:(=) B => A <: B
     * 其中 α 是类型变量，A 和 B 是其他类型
     *
     * @param typeVariable 中间的类型变量 α
     * @param constraint 新添加的约束
     */
    private fun Context.directWithVariable(
        typeVariable: TypeVariableMarker,
        constraint: Constraint
    ) {
        // 确定类型变量是否应该是灵活的（flexible）
        // 灵活类型允许在上下界之间有一定的变化空间
        val shouldBeTypeVariableFlexible =
            if (useRefinedBoundsForTypeVariableInFlexiblePosition())
                false
            else
                with(utilContext) { typeVariable.shouldBeFlexible() }

        // 情况1: α <: constraint.type
        // 如果新约束不是下界约束（即是上界或相等约束）
        if (constraint.kind != ConstraintKind.LOWER) {
            // 遍历该类型变量的所有现有约束
            forEachConstraint(typeVariable) {
                // 如果现有约束不是上界约束（即是下界或相等约束）
                if (it.kind != ConstraintKind.UPPER) {
                    // 添加传递约束: it.type <: constraint.type
                    addNewIncorporatedConstraint(
                        it.type,
                        constraint.type,
                        shouldBeTypeVariableFlexible,

                    )
                }
            }
        }

        // 情况2: constraint.type <: α
        // 如果新约束不是上界约束（即是下界或相等约束）
        if (constraint.kind != ConstraintKind.UPPER) {
            // 遍历该类型变量的所有现有约束
            forEachConstraint(typeVariable) {
                // 如果现有约束不是下界约束（即是上界或相等约束）
                if (it.kind != ConstraintKind.LOWER) {
                    // 检查约束是否来自声明的上界，且类型不是类型变量
                    val isFromDeclaredUpperBound =
                        it.position.from is DeclaredUpperBoundConstraintPosition<*> && !it.type.typeConstructor()
                            .isTypeVariable()

                    // 添加传递约束: constraint.type <: it.type
                    addNewIncorporatedConstraint(
                        constraint.type,
                        it.type,
                        shouldBeTypeVariableFlexible,
                        isFromDeclaredUpperBound = isFromDeclaredUpperBound
                    )
                }
            }
        }
    }

    /**
     * 检查是否存在递归约束
     *
     * 递归约束是指约束类型中包含被约束的类型变量本身。
     * 例如：α <: List<α> 就是一个递归约束。
     *
     * @param typeVariable 被约束的类型变量
     * @param constraint 要检查的约束
     * @return 如果存在递归约束则返回true
     */
    private fun Context.areThereRecursiveConstraints(typeVariable: TypeVariableMarker, constraint: Constraint) =
        constraint.type.contains {
            it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable.freshTypeConstructor()
        }

    /**
     * 合并约束
     *
     * 这是约束合并器的主要入口方法。当向类型变量添加新约束时调用此方法。
     *
     * @param c 约束系统上下文
     * @param typeVariable 要添加约束的类型变量 α
     * @param constraint 新添加的约束
     */
    fun incorporate(c: Context, typeVariable: TypeVariableMarker, constraint: Constraint) {
        // 检查编译是否被取消
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        // 不应该合并递归约束 -- 这样做太危险
        // 递归约束可能导致无限循环或不可解的类型系统
        if (c.areThereRecursiveConstraints(typeVariable, constraint)) return

        // 处理与该类型变量直接相关的约束传递
        c.directWithVariable(typeVariable, constraint)
        // 处理该类型变量在其他约束中的出现
        c.insideOtherConstraint(typeVariable, constraint)
    }

    /**
     * 处理类型变量在其他约束中的出现
     *
     * 当类型变量 β 在另一个类型变量 α 的约束中出现时，
     * β 的约束变化会影响 α 的约束。
     *
     * 例如：如果有 α <: List<β> 和新约束 β <: Number，
     * 则应该生成 α <: List<Number>
     *
     * @param typeVariable 被更新的类型变量 β
     * @param constraint β 的新约束
     */
    private fun Context.insideOtherConstraint(
        typeVariable: TypeVariableMarker,
        constraint: Constraint
    ) {
        val freshTypeConstructor = typeVariable.freshTypeConstructor()
        // 遍历所有其他类型变量
        for (typeVariableWithConstraint in this@insideOtherConstraint.allTypeVariablesWithConstraints) {
            // 找出包含当前类型变量的所有约束
            val constraintsWhichConstraintMyVariable = typeVariableWithConstraint.constraints.filter {
                containsTypeVariable(it.type, freshTypeConstructor)
            }
            // 为每个这样的约束生成新的合并约束
            constraintsWhichConstraintMyVariable.forEach {
                generateNewConstraint(typeVariableWithConstraint.typeVariable, it, typeVariable, constraint)
            }
        }
    }

    /**
     * 生成新的合并约束
     *
     * 当一个类型变量的约束包含另一个类型变量时，根据两个约束生成新的约束。
     *
     * @param targetVariable 目标类型变量（约束的主体）
     * @param baseConstraint 基础约束（包含otherVariable的约束）
     * @param otherVariable 另一个类型变量（在baseConstraint中出现）
     * @param otherConstraint otherVariable的约束
     */
    private fun Context.generateNewConstraint(
        targetVariable: TypeVariableMarker,
        baseConstraint: Constraint,
        otherVariable: TypeVariableMarker,
        otherConstraint: Constraint
    ) {
        val isBaseGenericType = baseConstraint.type.argumentsCount() != 0
        val isBaseOrOtherCapturedType = baseConstraint.type.isCapturedType() || otherConstraint.type.isCapturedType()

        // 根据otherConstraint的种类确定要使用的类型和是否需要近似
        val (type, needApproximation) = when (otherConstraint.kind) {
            // 相等约束：直接使用类型，不需要近似
            ConstraintKind.EQUALITY -> {
                otherConstraint.type to false
            }

            // 上界约束：需要创建捕获类型或使用特殊处理
            ConstraintKind.UPPER -> {
                /*
                 * 在某些情况下，不需要创建捕获类型，因为它会被近似为 Nothing 或其本身
                 * 示例：
                 *      targetVariable = TypeVariable(A)
                 *      baseConstraint = LOWER(TypeVariable(B))
                 *      otherConstraint = UPPER(Number)
                 *      incorporatedConstraint = Approx(CapturedType(out Number)) <: TypeVariable(A)
                 *                            => Nothing <: TypeVariable(A)
                 * 待办：为泛型和捕获类型实现此优化
                 */
                if (baseConstraint.kind == ConstraintKind.LOWER && !isBaseGenericType && !isBaseOrOtherCapturedType) {
                    nothingType() to false
                } else if (baseConstraint.kind == ConstraintKind.UPPER && !isBaseGenericType && !isBaseOrOtherCapturedType) {
                    otherConstraint.type to false
                } else {
                    // 创建协变捕获类型 (out T)
                    createCapturedType(
                        createTypeArgument(otherConstraint.type),
                        listOf(otherConstraint.type),
                        null,
                        CaptureStatus.FOR_INCORPORATION
                    ) to true
                }
            }

            // 下界约束：需要创建捕获类型或使用特殊处理
            ConstraintKind.LOWER -> {
                /*
                 * 在某些情况下，不需要创建捕获类型，因为它会被近似为 Any? 或其本身
                 * 示例：
                 *      targetVariable = TypeVariable(A)
                 *      baseConstraint = UPPER(TypeVariable(B))
                 *      otherConstraint = LOWER(Number)
                 *      incorporatedConstraint = TypeVariable(A) <: Approx(CapturedType(in Number))
                 *                            => TypeVariable(A) <: Any?
                 * 待办：为泛型和捕获类型实现此优化
                 */
                if (baseConstraint.kind == ConstraintKind.UPPER && !isBaseGenericType && !isBaseOrOtherCapturedType) {
                    anyType() to false
                } else if (baseConstraint.kind == ConstraintKind.LOWER && !isBaseGenericType && !isBaseOrOtherCapturedType) {
                    otherConstraint.type to false
                } else {
                    // 创建逆变捕获类型 (in T)
                    createCapturedType(
                        createTypeArgument(otherConstraint.type),
                        emptyList(),
                        otherConstraint.type,
                        CaptureStatus.FOR_INCORPORATION
                    ) to true
                }
            }
        }

        // 如果需要的话进行近似，并添加新的约束
        approximateIfNeededAndAddNewConstraint(
            baseConstraint,
            type,
            targetVariable,
            otherVariable,
            otherConstraint,
            needApproximation
        )
    }

    /**
     * 在类型中替换类型变量
     *
     * @param c 上下文
     * @param typeVariable 要被替换的类型变量
     * @param value 替换后的值
     * @return 替换后的类型
     */
    private fun CangJieTypeMarker.substitute(
        c: Context,
        typeVariable: TypeVariableMarker,
        value: CangJieTypeMarker
    ): CangJieTypeMarker {
        val substitutor = c.typeSubstitutorByTypeConstructor(mapOf(typeVariable.freshTypeConstructor(c) to value))
        return substitutor.safeSubstitute(c, this)
    }

    /**
     * 获取类型中嵌套的所有类型变量
     *
     * 递归遍历类型参数，收集所有出现的类型变量。
     *
     * @param type 要分析的类型
     * @return 嵌套的类型变量列表
     */
    private fun Context.getNestedTypeVariables(type: CangJieTypeMarker): List<TypeVariableMarker> =
        getNestedArguments(type).mapNotNullTo(SmartList()) {
            getTypeVariable(it.getType().typeConstructor().unwrapStubTypeVariableConstructor())
        }

    /**
     * 判断约束是否对可空性推断有潜在用处
     *
     * 某些约束虽然看起来平凡，但对于确定类型的可空性很重要。
     *
     * @param newConstraint 新生成的约束类型
     * @param otherConstraint 另一个约束类型
     * @param kind 约束种类
     * @return 如果对可空性推断有用则返回true
     */
    private fun Context.isPotentialUsefulNullabilityConstraint(
        newConstraint: CangJieTypeMarker,
        otherConstraint: CangJieTypeMarker,
        kind: ConstraintKind
    ): Boolean {
        // 如果新约束已经是合适的结果类型，不需要额外的可空性约束
        if (trivialConstraintTypeInferenceOracle.isSuitableResultedType(newConstraint)) return false

        // otherConstraint 可以为 newConstraint 添加可空性
        // 例如：newConstraint 是 T，otherConstraint 是 T?，kind 是 LOWER
        val otherConstraintCanAddNullabilityToNewOne =
            !newConstraint.isOptionType() && otherConstraint.isOptionType() && kind == ConstraintKind.LOWER

        // newConstraint 可以为 otherConstraint 添加可空性
        // 例如：newConstraint 是 T?，otherConstraint 是 T，kind 是 UPPER
        val newConstraintCanAddNullabilityToOtherOne =
            newConstraint.isOptionType() && !otherConstraint.isOptionType() && kind == ConstraintKind.UPPER

        return otherConstraintCanAddNullabilityToNewOne || newConstraintCanAddNullabilityToOtherOne
    }

    /**
     * 检查新约束是否包含没有型变（projection）的约束类型
     *
     * 在仓颉语言中，所有类型参数都是不变的（invariant），因此简化了检查逻辑。
     *
     * @param newConstraint 新约束类型
     * @param otherConstraint 另一个约束
     * @return 如果包含则返回true
     */
    private fun Context.containsConstrainingTypeWithoutProjection(
        newConstraint: CangJieTypeMarker,
        otherConstraint: Constraint
    ): Boolean {
        // 在仓颉语言中，所有类型参数都是 invariant 的，因此简化检查逻辑
        return getNestedArguments(newConstraint).any {
            it.getType().typeConstructor() == otherConstraint.type.typeConstructor()
        }
    }

    /**
     * 添加新的合并约束
     *
     * 在添加约束之前进行多项检查，以避免添加无用的或平凡的约束。
     *
     * @param targetVariable 目标类型变量
     * @param baseConstraint 基础约束
     * @param otherVariable 另一个类型变量
     * @param otherConstraint 另一个约束
     * @param newConstraint 新生成的约束类型
     * @param isSubtype 是否是子类型约束（true表示下界，false表示上界）
     */
    private fun Context.addNewConstraint(
        targetVariable: TypeVariableMarker,
        baseConstraint: Constraint,
        otherVariable: TypeVariableMarker,
        otherConstraint: Constraint,
        newConstraint: CangJieTypeMarker,
        isSubtype: Boolean
    ) {
        // 如果目标变量出现在新约束的嵌套类型变量中，避免循环约束
        if (targetVariable in getNestedTypeVariables(newConstraint)) return

        // 检查约束是否对可空性推断有用
        val isUsefulForNullabilityConstraint =
            isPotentialUsefulNullabilityConstraint(newConstraint, otherConstraint.type, otherConstraint.kind)

        // 检查约束是否来自变量固定
        val isFromVariableFixation = baseConstraint.position.from is FixVariableConstraintPosition<*>
                || otherConstraint.position.from is FixVariableConstraintPosition<*>

        // 如果约束不满足以下任何条件，则跳过：
        // 1. 不是相等约束
        // 2. 对可空性推断无用
        // 3. 不是来自变量固定
        // 4. 不包含约束类型
        if (!otherConstraint.kind.isEqual() &&
            !isUsefulForNullabilityConstraint &&
            !isFromVariableFixation &&
            !containsConstrainingTypeWithoutProjection(newConstraint, otherConstraint)
        ) return

        // 检查生成的约束是否是平凡的（总是满足的）
        if (trivialConstraintTypeInferenceOracle.isGeneratedConstraintTrivial(
                baseConstraint, otherConstraint, newConstraint, isSubtype
            )
        ) return

        // 合并派生来源，避免循环引用
        val derivedFrom = SmartSet.create(baseConstraint.derivedFrom).also { it.addAll(otherConstraint.derivedFrom) }
        if (otherVariable in derivedFrom) return

        derivedFrom.add(otherVariable)

        // 确定约束种类
        val kind = if (isSubtype) ConstraintKind.LOWER else ConstraintKind.UPPER

        // 获取输入类型位置信息
        val inputTypePosition =
            baseConstraint.position.from as? OnlyInputTypeConstraintPosition
                ?: baseConstraint.inputTypePositionBeforeIncorporation


        // 创建约束上下文
        val constraintContext = ConstraintContext(kind, derivedFrom, inputTypePosition)

        // 添加新的合并约束
        addNewIncorporatedConstraint(targetVariable, newConstraint, constraintContext)
    }

    /**
     * 如果需要的话进行类型近似，并添加新约束
     *
     * @param baseConstraint 基础约束
     * @param type 要替换的类型
     * @param targetVariable 目标类型变量
     * @param otherVariable 另一个类型变量
     * @param otherConstraint 另一个约束
     * @param needApproximation 是否需要近似
     */
    private fun Context.approximateIfNeededAndAddNewConstraint(
        baseConstraint: Constraint,
        type: CangJieTypeMarker,
        targetVariable: TypeVariableMarker,
        otherVariable: TypeVariableMarker,
        otherConstraint: Constraint,
        needApproximation: Boolean = true
    ) {
        // 在基础约束类型中用给定类型替换 otherVariable
        val typeWithSubstitution = baseConstraint.type.substitute(this, otherVariable, type)

        // 定义类型准备函数：根据需要进行近似
        val prepareType = { toSuper: Boolean ->
            if (needApproximation) approximateCapturedTypes(typeWithSubstitution, toSuper) else typeWithSubstitution
        }

        // 如果基础约束不是下界约束，添加上界约束
        if (baseConstraint.kind != ConstraintKind.LOWER) {
            addNewConstraint(
                targetVariable,
                baseConstraint,
                otherVariable,
                otherConstraint,
                prepareType(true),  // 近似到超类型
                isSubtype = false
            )
        }

        // 如果基础约束不是上界约束，添加下界约束
        if (baseConstraint.kind != ConstraintKind.UPPER) {
            addNewConstraint(
                targetVariable,
                baseConstraint,
                otherVariable,
                otherConstraint,
                prepareType(false),  // 近似到子类型
                isSubtype = true
            )
        }
    }

    /**
     * 近似捕获类型
     *
     * 将捕获类型近似为更简单的上界或下界类型。
     *
     * @param type 要近似的类型
     * @param toSuper 是否近似到超类型（true）或子类型（false）
     * @return 近似后的类型
     */
    private fun approximateCapturedTypes(type: CangJieTypeMarker, toSuper: Boolean): CangJieTypeMarker =
        if (toSuper) typeApproximator.approximateToSuperType(
            type,
            TypeApproximatorConfiguration.IncorporationConfiguration
        ) ?: type
        else typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.IncorporationConfiguration)
            ?: type

    /**
     * 遍历指定类型变量的所有约束，并对每个约束执行给定的操作
     *
     * 此函数设计为内联函数，以避免额外的函数调用栈创建，从而提高性能，
     * 尤其是在循环或频繁调用的情况下。
     *
     * @param typeVariable 需要遍历约束的类型变量标记
     * @param action 对每个约束执行的操作
     */
    private inline fun Context.forEachConstraint(typeVariable: TypeVariableMarker, action: (Constraint) -> Unit) {
        // 使用索引循环是因为在迭代过程中集合可能会被修改
        // 然而，唯一可能的修改是追加元素，因此这样做应该是安全的
        val constraints = getConstraintsForVariable(typeVariable)
        var i = 0
        while (i < constraints.size) {
            action(constraints[i++])
        }
    }


    /**
     * 约束合并上下文接口
     *
     * 定义了约束合并过程中需要的所有操作。
     */
    interface Context : TypeSystemInferenceExtensionContext {
        /** 所有带约束的类型变量集合 */
        val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>

        /**
         * 根据类型构造器获取类型变量
         *
         * @param typeConstructor 类型构造器
         * @return 对应的类型变量，如果类型变量已固定则返回null（这是错误情况）
         */
        fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker?

        /**
         * 获取类型变量的所有约束
         *
         * @param typeVariable 类型变量
         * @return 约束列表
         */
        fun getConstraintsForVariable(typeVariable: TypeVariableMarker): List<Constraint>

        /**
         * 添加新的合并约束（类型之间的子类型关系）
         *
         * @param lowerType 下界类型（子类型）
         * @param upperType 上界类型（超类型）
         * @param shouldTryUseDifferentFlexibilityForUpperType 是否尝试为上界类型使用不同的灵活性
         * @param isFromDeclaredUpperBound 是否来自声明的上界
         */
        fun addNewIncorporatedConstraint(
            lowerType: CangJieTypeMarker,
            upperType: CangJieTypeMarker,
            shouldTryUseDifferentFlexibilityForUpperType: Boolean,
            isFromDeclaredUpperBound: Boolean = false
        )

        /**
         * 添加新的合并约束（类型变量与类型之间的约束）
         *
         * @param typeVariable 类型变量
         * @param type 约束类型
         * @param constraintContext 约束上下文，包含约束种类、来源等信息
         */
        fun addNewIncorporatedConstraint(
            typeVariable: TypeVariableMarker,
            type: CangJieTypeMarker,
            constraintContext: ConstraintContext
        )
    }
}

/**
 * 获取类型的所有嵌套类型参数
 *
 * 递归遍历类型结构，收集所有嵌套的类型参数，包括灵活类型的上下界。
 *
 * @param type 要分析的类型
 * @return 所有嵌套的类型参数列表
 */
private fun TypeSystemInferenceExtensionContext.getNestedArguments(type: CangJieTypeMarker): List<TypeArgumentMarker> {
    val result = SmartList<TypeArgumentMarker>()
    val stack = ArrayDeque<TypeArgumentMarker>()

    // 处理灵活类型：同时添加上界和下界
    when (type) {
        is FlexibleTypeMarker -> {
            stack.add(createTypeArgument(type.lowerBound()))
            stack.add(createTypeArgument(type.upperBound()))
        }

        else -> stack.add(createTypeArgument(type))
    }

    // 添加类型本身
    stack.add(createTypeArgument(type))

    // 定义将类型参数添加到栈的辅助函数
    val addArgumentsToStack = { projectedType: CangJieTypeMarker ->
        for (argumentIndex in 0 until projectedType.argumentsCount()) {
            stack.add(projectedType.getArgument(argumentIndex))
        }
    }

    // 广度优先遍历类型树
    while (!stack.isEmpty()) {
        val typeProjection = stack.removeFirst()

        // 将当前类型参数添加到结果中
        result.add(typeProjection)

        // 处理类型参数的嵌套类型
        when (val projectedType = typeProjection.getType()) {
            is FlexibleTypeMarker -> {
                addArgumentsToStack(projectedType.lowerBound())
                addArgumentsToStack(projectedType.upperBound())
            }

            else -> addArgumentsToStack(projectedType)
        }
    }
    return result
}