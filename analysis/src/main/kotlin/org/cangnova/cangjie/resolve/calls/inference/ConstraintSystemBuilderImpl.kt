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

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations


import org.cangnova.cangjie.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariable
import org.cangnova.cangjie.resolve.calls.results.SimpleConstraintSystem
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.TypeUtils.DONT_CARE
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.checker.ConstraintCheckContext
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.createCapturedType
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeParameterMarker
import org.cangnova.cangjie.types.model.TypeSystemInferenceExtensionContext
import org.cangnova.cangjie.types.model.requireOrDescribe
import java.util.*

/**
 * 约束系统构建器实现
 *
 * 用于在类型推导过程中构建和管理类型约束系统。主要职责包括:
 * - 注册类型变量及其边界约束
 * - 添加子类型/相等类型约束
 * - 执行约束简化和传播
 * - 固定类型变量的最终类型
 *
 * ## 工作模式
 * - **INFERENCE**: 类型推导模式,用于推导泛型实参类型
 * - **SPECIFICITY**: 特异性比较模式,用于重载解析中判断候选函数的特异性
 *
 * ## 核心数据结构
 * - [allTypeParameterBounds]: 所有类型变量的边界约束映射
 * - [initialConstraints]: 初始约束列表,用于错误报告
 * - [usedInBounds]: 记录哪些类型变量被用于其他变量的边界
 * - [typeVariableSubstitutors]: 每个函数调用的类型变量替换器
 *
 * @property mode 工作模式,默认为类型推导模式
 */
open class ConstraintSystemBuilderImpl(private val mode: Mode = Mode.INFERENCE) :
    ConstraintSystem.Builder {

    /**
     * 约束系统的工作模式
     */
    enum class Mode {
        /** 类型推导模式 - 用于推导泛型实参类型 */
        INFERENCE,

        /** 特异性比较模式 - 用于重载解析中判断候选函数的特异性 */
        SPECIFICITY
    }

    /**
     * 约束类型
     *
     * 表示类型之间的约束关系,每种约束类型对应一种类型边界
     *
     * @property bound 对应的类型边界类型
     */
    enum class ConstraintKind(val bound: TypeBounds.BoundKind) {
        /** 子类型约束 (A <: B) - 对应上界约束 */
        SUB_TYPE(TypeBounds.BoundKind.UPPER_BOUND),

        /** 相等类型约束 (A == B) - 对应精确边界约束 */
        EQUAL(TypeBounds.BoundKind.EXACT_BOUND)
    }

    /** 约束系统中收集的错误列表 */
    internal val errors = ArrayList<ConstraintError>()

    /** 所有类型变量的边界约束映射表 */
    internal val allTypeParameterBounds = LinkedHashMap<TypeVariable, TypeBoundsImpl>()

    /**
     * 类型约束数据类
     *
     * 表示两个类型之间的约束关系,用于后续的约束求解和错误报告
     *
     * @property kind 约束类型 (子类型或相等)
     * @property subtype 子类型
     * @property superType 父类型
     * @property position 约束位置,用于错误报告
     */
    internal data class Constraint(
        val kind: ConstraintKind, val subtype: CangJieType, val superType: CangJieType, val position: ConstraintPosition
    )

    /** 初始约束列表,保存所有添加的原始约束,用于错误报告和调试 */
    internal val initialConstraints = ArrayList<Constraint>()

    /**
     * 类型变量使用记录表
     *
     * 记录每个类型变量被用于哪些其他类型变量的边界约束中
     * 用于约束传播: 当一个类型变量的值被确定后,需要更新所有依赖它的约束
     */
    internal val usedInBounds = HashMap<TypeVariable, MutableList<TypeBounds.Bound>>()

    /**
     * 存储函数调用的类型变量替换器
     *
     * 为每个函数调用保存其类型变量替换器,同一个调用只能注册一次
     *
     * @param call 函数调用句柄
     * @param substitutor 类型替换器
     * @return 传入的替换器
     * @throws IllegalStateException 如果同一个调用的类型变量已经被注册过
     */
    private fun storeSubstitutor(call: CallHandle, substitutor: DefaultTypeSubstitutor): DefaultTypeSubstitutor {
        if (typeVariableSubstitutors.containsKey(call)) {
            throw IllegalStateException("Type variables for the same call can be registered only once: $call")
        }
        typeVariableSubstitutors[call] = substitutor
        return substitutor
    }

    /**
     * 获取类型变量的边界约束
     *
     * @param variable 类型变量
     * @return 该变量的边界约束对象
     * @throws IllegalArgumentException 如果该类型参数不是约束系统中的类型变量
     */
    internal fun getTypeBounds(variable: TypeVariable): TypeBoundsImpl {
        return allTypeParameterBounds[variable]
            ?: throw IllegalArgumentException("TypeParameterDescriptor is not a type variable for constraint system: $variable")
    }

    /**
     * 为类型参数生成类型边界约束
     *
     * 此方法处理类型参数和具体类型之间的约束关系,并进行必要的类型转换
     *
     * ## 处理可空类型的特殊逻辑
     * 当类型参数是可空类型 (T?) 时,约束会被拆分:
     * - `T? = Int?` 转换为: `T >: Int` 和 `T <: Int?`
     * - `T? = Int!` 转换为: `T >: Int` 和 `T <: Int!`
     * - `T? >: Int?` 转换为: `T >: Int`
     * - `T? <: Int?` 保持为: `T <: Int?`
     *
     * @param parameterType 类型参数类型
     * @param constrainingType 约束类型
     * @param boundKind 边界类型 (上界/下界/精确)
     * @param constraintContext 约束上下文
     */
    private fun generateTypeParameterBound(
        parameterType: CangJieType,
        constrainingType: CangJieType,
        boundKind: TypeBounds.BoundKind,
        constraintContext: ConstraintContext
    ) {
        val typeVariable = getMyTypeVariable(parameterType) ?: return

        var newConstrainingType = constrainingType

        // 处理灵活类型的情况,当 T! 获得边界 Foo 或 Foo? 时
        // 类型参数 T 应该获得边界 Foo!
        // 示例:
        // val c: Collection<Foo> = Collections.singleton(null : Foo?)
        // T 的约束为:
        //   Foo? <: T!
        //   Foo >: T!
        // Foo 和 Foo? 在这里都会转换为 Foo!
        if (parameterType.isFlexible()) {
            val customTypeParameter = parameterType.getCustomTypeParameter()
            if (customTypeParameter != null) {
                newConstrainingType = customTypeParameter.substitutionResult(constrainingType)
            }
        }

        if (!parameterType.isOption || !TypeUtils.isOptionType(newConstrainingType)) {
            addBound(typeVariable, newConstrainingType, boundKind, constraintContext)
            return
        }
        // 对于类型参数 T:
        // 约束 T? = Int? 应转换为 T >: Int 和 T <: Int?
        // 约束 T? = Int! 应转换为 T >: Int 和 T <: Int!

        // 约束 T? >: Int?; T? >: Int! 应转换为 T >: Int
        val notNullConstrainingType = TypeUtils.makeNonOption(newConstrainingType)
        if (boundKind == TypeBounds.BoundKind.EXACT_BOUND || boundKind == TypeBounds.BoundKind.LOWER_BOUND) {
            addBound(typeVariable, notNullConstrainingType, TypeBounds.BoundKind.LOWER_BOUND, constraintContext)
        }
        // 约束 T? <: Int?; T? <: Int! 应转换为 T <: Int?; T <: Int! (对应保持)
        if (boundKind == TypeBounds.BoundKind.EXACT_BOUND || boundKind == TypeBounds.BoundKind.UPPER_BOUND) {
            addBound(typeVariable, newConstrainingType, TypeBounds.BoundKind.UPPER_BOUND, constraintContext)
        }
    }

    /**
     * 检查类型是否为错误类型或特殊类型
     *
     * 特殊类型包括:
     * - DONT_CARE 占位符类型
     * - 未推导的类型变量
     * - 错误类型 (非函数占位符)
     *
     * @param type 待检查的类型
     * @param constraintPosition 约束位置,用于错误报告
     * @return 如果是错误或特殊类型返回 true
     */
    private fun isErrorOrSpecialType(type: CangJieType?, constraintPosition: ConstraintPosition): Boolean {
        if (TypeUtils.isDontCarePlaceholder(type) || ErrorUtils.isUninferredTypeVariable(type)) {
            return true
        }

        if (type == null || (type.isError && !type.isFunctionPlaceholder)) {
            errors.add(ErrorInConstrainingType(constraintPosition))
            return true
        }
        return false
    }

    /**
     * 添加约束的核心实现方法
     *
     * 此方法负责:
     * 1. 验证类型的有效性
     * 2. 处理函数占位符类型
     * 3. 简化约束并递归处理嵌套约束
     * 4. 记录初始约束用于错误报告
     *
     * ## 函数占位符处理
     * 当子类型是函数占位符时,会根据期望的父类型创建相应的函数类型
     *
     * @param constraintKind 约束类型 (子类型或相等)
     * @param subType 子类型
     * @param superType 父类型
     * @param constraintContext 约束上下文
     * @param constraintCheckContext 约束检查上下文,用于收集嵌套约束
     */
    private fun doAddConstraint(
        constraintKind: ConstraintKind,
        subType: CangJieType?,
        superType: CangJieType?,
        constraintContext: ConstraintContext,
        constraintCheckContext: ConstraintCheckContext
    ) {
        val constraintPosition = constraintContext.position
        if (isErrorOrSpecialType(subType, constraintPosition) || isErrorOrSpecialType(
                superType,
                constraintPosition
            )
        ) return
        if (subType == null || superType == null) return

//        if (constraintContext.initialReduction && (subType.hasExactAnnotation() || superType.hasExactAnnotation()) && (constraintKind != EQUAL)) {
//            return doAddConstraint(ConstraintKind.EQUAL, subType, superType, constraintContext, typeCheckingProcedure)
//        }

        assert(!superType.isFunctionPlaceholder) { "类型 $constraintPosition 不应该是函数类型占位符" }

        // 函数字面量 { x -> ... } 未声明接收器类型
        // 如果期望扩展函数则可以视为扩展函数
        val newSubType = if (constraintKind == ConstraintKind.SUB_TYPE && subType.isFunctionPlaceholder) {
            if (isMyTypeVariable(superType)) {
                // 该约束绑定了类型参数和函数类型
                // 在不知道它是函数类型还是扩展函数类型之前,我们不添加它
                return
            }
            createTypeForFunctionPlaceholder(subType, superType)
        } else {
            subType
        }

        fun simplifyConstraint(subType: CangJieType, superType: CangJieType) {
            if (isMyTypeVariable(subType)) {
                generateTypeParameterBound(subType, superType, constraintKind.bound, constraintContext)
                return
            }
            if (isMyTypeVariable(superType)) {
                generateTypeParameterBound(superType, subType, constraintKind.bound.reverse(), constraintContext)
                return
            }
            val subType2 = simplifyType(subType, constraintContext.initial)
            val superType2 = simplifyType(superType, constraintContext.initial)
            // 使用新的 CangJieTypeChecker 进行类型检查
            val result = if (constraintKind == ConstraintKind.EQUAL) {
                CangJieTypeChecker.DEFAULT.checkEqualityConstraint(subType2, superType2, constraintCheckContext)
            } else {
                CangJieTypeChecker.DEFAULT.checkSubtypeConstraint(subType2, superType, constraintCheckContext)
            }
            if (!result) errors.add(newTypeInferenceOrParameterConstraintError(constraintPosition))
        }
        if (constraintContext.initial) {
            storeInitialConstraint(constraintKind, subType, superType, constraintPosition)
        }
//        if (subType.hasNoInferAnnotation() || superType.hasNoInferAnnotation()) return

        simplifyConstraint(newSubType, superType)
    }

    /**
     * 存储初始约束
     *
     * 保存原始约束用于后续的错误报告和调试
     *
     * @param constraintKind 约束类型
     * @param subType 子类型
     * @param superType 父类型
     * @param position 约束位置
     */
    private fun storeInitialConstraint(
        constraintKind: ConstraintKind,
        subType: CangJieType,
        superType: CangJieType,
        position: ConstraintPosition
    ) {
        initialConstraints.add(Constraint(constraintKind, subType, superType, position))
    }

    /**
     * 简化类型
     *
     * 在类型推导模式下,对于初始约束会移除可空性,以避免因可空性导致的类型不匹配
     * 实际的可空性错误会在后续阶段报告
     *
     * @param type 待简化的类型
     * @param isInitialConstraint 是否为初始约束
     * @return 简化后的类型
     */
    private fun simplifyType(type: CangJieType, isInitialConstraint: Boolean): CangJieType =
        if (mode == Mode.SPECIFICITY || !isInitialConstraint)
            type
        else {
            // 如果子类型是可空的而父类型不可空,稍后会生成不安全调用或类型不匹配错误
            // 但约束系统仍然应该被求解
            TypeUtils.makeNonOption(type)
        }

    /**
     * 检查类型是否为当前约束系统中的类型变量
     *
     * @param type 待检查的类型
     * @return 如果是类型变量返回 true
     */
    internal fun isMyTypeVariable(type: CangJieType): Boolean =
        getMyTypeVariable(type) != null

    /**
     * 从类型中获取对应的类型变量
     *
     * @param type 待检查的类型
     * @return 如果该类型对应一个类型变量,返回该变量;否则返回 null
     */
    internal fun getMyTypeVariable(type: CangJieType): TypeVariable? {
        return getMyTypeVariable(type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return null)
    }

    /**
     * 从类型参数描述符获取对应的类型变量
     *
     * @param typeParameter 类型参数描述符
     * @return 如果该参数对应一个类型变量,返回该变量;否则返回 null
     */
    private fun getMyTypeVariable(typeParameter: TypeParameterDescriptor): TypeVariable? =
        allTypeParameterBounds.keys.find { it.freshTypeParameter == typeParameter }

    /**
     * 生成类型参数捕获约束
     *
     * 处理类型参数的捕获,将类型参数转换为捕获类型并添加为精确边界约束
     *
     * ## 仓颉语言特性
     * 仓颉语言不支持类型投影(in/out variance),所有类型参数都是不变的(invariant)
     * 因此捕获逻辑大大简化,不需要处理投影类型
     *
     * ## 错误检测
     * 如果类型变量有多个非默认上界,则报告捕获错误
     *
     * @param typeVariable 类型变量
     * @param constrainingTypeArgument 约束类型参数
     * @param constraintContext 约束上下文
     * @param isTypeMarkedNullable 类型是否标记为可空
     */
    private fun generateTypeParameterCaptureConstraint(
        typeVariable: TypeVariable,
        constrainingTypeArgument: TypeArgument,
        constraintContext: ConstraintContext,
        isTypeMarkedNullable: Boolean
    ) {
        // 仓颉语言中所有类型参数都是不变的,检查是否有多个非默认上界
        if (!typeVariable.originalTypeParameter.upperBounds.let { it.size == 1 && it.single().isDefaultBound() }) {
            errors.add(CannotCapture(constraintContext.position, typeVariable))
        }

        val typeArgument = if (isTypeMarkedNullable) {
            TypeArgumentImpl(TypeUtils.makeNonOption(constrainingTypeArgument.type))
        } else {
            constrainingTypeArgument
        }
        val capturedType = createCapturedType(typeArgument)
        addBound(typeVariable, capturedType, TypeBounds.BoundKind.EXACT_BOUND, constraintContext)
    }

    /**
     * 添加类型约束
     *
     * 这是添加约束的主要公开方法,创建约束检查上下文并委托给 [doAddConstraint]
     *
     * ## 约束检查上下文
     * 通过 [ConstraintCheckContext] 实现以下功能:
     * - 相等类型约束 - 递归添加相等约束
     * - 子类型约束 - 递归添加子类型约束
     * - 类型捕获 - 处理类型参数的捕获
     * - 错误报告 - 当类型检查失败时报告错误
     *
     * @param constraintKind 约束类型
     * @param subType 子类型
     * @param superType 父类型
     * @param constraintContext 约束上下文
     */
    fun addConstraint(
        constraintKind: ConstraintKind,
        subType: CangJieType?,
        superType: CangJieType?,
        constraintContext: ConstraintContext
    ) {
        val constraintPosition = constraintContext.position

        // 处理嵌套约束时,`derivedFrom` 信息应该被重置
        val newConstraintContext = ConstraintContext(
            constraintContext.position, derivedFrom = null, initial = false,
            initialReduction = constraintContext.initialReduction
        )

        // 创建约束检查上下文实现
        val constraintCheckContext = object : ConstraintCheckContext {
            private var depth = 0

            override fun addEqualityConstraint(type1: CangJieType, type2: CangJieType) {
                depth++
                doAddConstraint(ConstraintKind.EQUAL, type1, type2, newConstraintContext, this)
                depth--
            }

            override fun addSubtypeConstraint(subtype: CangJieType, supertype: CangJieType) {
                depth++
                doAddConstraint(
                    ConstraintKind.SUB_TYPE,
                    subtype,
                    supertype,
                    newConstraintContext,
                    this
                )
                depth--
            }

            override fun tryCaptureTypeArgument(type: CangJieType, typeArgument: TypeArgument): Boolean {
                if (isMyTypeVariable(typeArgument.type) || depth > 0) return false
                val myTypeVariable = getMyTypeVariable(type)

                if (myTypeVariable != null && constraintPosition.isParameter()) {
                    generateTypeParameterCaptureConstraint(
                        myTypeVariable,
                        typeArgument,
                        newConstraintContext,
                        type.isOption
                    )
                    return true
                }
                return false
            }

            override fun reportConstraintError() {
                errors.add(newTypeInferenceOrParameterConstraintError(constraintPosition))
            }
        }

        doAddConstraint(constraintKind, subType, superType, constraintContext, constraintCheckContext)
    }

    /**
     * 注册类型变量
     *
     * 为函数调用注册类型参数,创建对应的类型变量,并添加类型参数的上界约束
     *
     * ## 外部类型变量 vs 内部类型变量
     * - **外部变量**: 直接使用原始类型参数,不创建新鲜变量
     * - **内部变量**: 创建新鲜类型参数副本,避免污染原始类型参数
     *
     * ## 处理流程
     * 1. 为每个类型参数创建类型变量
     * 2. 为类型变量创建边界约束对象
     * 3. 添加类型参数的上界约束 (跳过默认上界 Any?)
     * 4. 创建并存储类型替换器
     *
     * @param call 函数调用句柄
     * @param typeParameters 类型参数列表
     * @param external 是否为外部类型变量
     * @return 类型替换器,用于将类型参数替换为类型变量
     */
    override fun registerTypeVariables(
        call: CallHandle,
        typeParameters: Collection<TypeParameterDescriptor>,
        external: Boolean
    ): DefaultTypeSubstitutor {
        if (typeParameters.isEmpty()) return storeSubstitutor(call, DefaultTypeSubstitutor.EMPTY)

        val typeVariables = if (external) {
            typeParameters.map {
                TypeVariable(call, it, it, true)
            }
        } else {
            val freshTypeParameters = ArrayList<TypeParameterDescriptor>(typeParameters.size)
            DescriptorSubstitutor.substituteTypeParameters(
                typeParameters.toList(),
                TypeSubstitution.EMPTY,
                typeParameters.first().containingDeclaration,
                freshTypeParameters
            )
            freshTypeParameters.zip(typeParameters).map {
                val (fresh, original) = it
                TypeVariable(call, fresh, original, external)
            }
        }

        for ((_, typeVariable) in typeParameters.zip(typeVariables)) {
            allTypeParameterBounds[typeVariable] = TypeBoundsImpl(typeVariable)
        }

        for ((typeVariable, _) in allTypeParameterBounds) {
            for (declaredUpperBound in typeVariable.freshTypeParameter.upperBounds) {
                if (declaredUpperBound.isDefaultBound()) continue // TODO 是否移除此行?
                val context =
                    ConstraintContext(ConstraintPositionKind.TYPE_BOUND_POSITION.position(typeVariable.originalTypeParameter.index))
                addBound(typeVariable, declaredUpperBound, TypeBounds.BoundKind.UPPER_BOUND, context)
            }
        }

        return storeSubstitutor(
            call, DefaultTypeSubstitutor.create(
                TypeConstructorSubstitution.createByParametersMap(
                    typeParameters.zip(typeVariables.map { it.type }.defaultProjections()).toMap()
                )
            )
        )
    }

    /**
     * 检查类型是否为正规类型 (Proper Type)
     *
     * 正规类型是指不包含任何约束系统中的类型变量的类型
     * 例如: `Int` 是正规类型, `List<T>` (其中 T 是类型变量) 不是正规类型
     *
     * @return 如果类型不包含类型变量返回 true
     */
    private fun CangJieType.isProper() = !TypeUtils.contains(this) { type ->
        type.constructor.declarationDescriptor.let { it is TypeParameterDescriptor && isMyTypeVariable(it) }
    }

    /**
     * 检查类型参数是否为约束系统中的类型变量
     *
     * @param typeParameter 类型参数描述符
     * @return 如果是类型变量返回 true
     */
    private fun isMyTypeVariable(typeParameter: TypeParameterDescriptor) =
        getMyTypeVariable(typeParameter) != null

    /**
     * 为类型变量添加类型边界约束
     *
     * ## 约束传播机制
     * 当添加的边界不是正规类型时:
     * 1. 提取边界中包含的所有类型变量
     * 2. 在 [usedInBounds] 中记录依赖关系
     * 3. 当依赖的类型变量被固定后,触发约束合并
     *
     * @param typeVariable 类型变量
     * @param constrainingType 约束类型
     * @param kind 边界类型 (上界/下界/精确)
     * @param constraintContext 约束上下文
     */
    internal fun addBound(
        typeVariable: TypeVariable,
        constrainingType: CangJieType,
        kind: TypeBounds.BoundKind,
        constraintContext: ConstraintContext
    ) {
        val bound = TypeBounds.Bound(
            typeVariable, constrainingType, kind, constraintContext.position,
            constrainingType.isProper(), constraintContext.derivedFrom ?: emptySet()
        )
        val typeBounds = getTypeBounds(typeVariable)
        if (typeBounds.bounds.contains(bound)) return

        typeBounds.addBound(bound)

        if (!bound.isProper) {
            for (dependentTypeVariable in getNestedTypeVariables(bound.constrainingType)) {
                val dependentBounds = usedInBounds.getOrPut(dependentTypeVariable) { arrayListOf() }
                dependentBounds.add(bound)
            }
        }

        incorporateBound(bound)
    }

    /**
     * 获取类型中嵌套的所有类型变量
     *
     * 遍历类型的所有类型参数,返回其中属于约束系统的类型变量
     *
     * @param type 待检查的类型
     * @return 嵌套的类型变量列表
     */
    internal fun getNestedTypeVariables(type: CangJieType): List<TypeVariable> =
        type.getNestedTypeParameters().mapNotNull { getMyTypeVariable(it) }

    /**
     * 添加子类型约束
     *
     * 这是 [ConstraintSystem.Builder] 接口的实现方法
     *
     * @param constrainingType 约束类型 (子类型)
     * @param subjectType 目标类型 (父类型)
     * @param constraintPosition 约束位置
     */
    override fun addSubtypeConstraint(
        constrainingType: CangJieType?,
        subjectType: CangJieType?,
        constraintPosition: ConstraintPosition
    ) {
        addConstraint(
            ConstraintKind.SUB_TYPE,
            constrainingType,
            subjectType,
            ConstraintContext(constraintPosition, initial = true, initialReduction = true)
        )
    }

    /** 类型变量替换器映射表,记录每个函数调用对应的类型替换器 */
    override val typeVariableSubstitutors = LinkedHashMap<CallHandle, DefaultTypeSubstitutor>()


    /**
     * 合并其他约束系统
     *
     * 将另一个约束系统的所有数据合并到当前系统中
     * 要求两个系统针对不同的函数调用且没有共同的类型变量
     *
     * @param other 待合并的约束系统构建器
     * @throws IllegalArgumentException 如果两个系统有共同的调用或类型变量
     */
    override fun add(other: ConstraintSystem.Builder) {
        if (other !is ConstraintSystemBuilderImpl) {
            throw IllegalArgumentException("Unknown constraint system builder implementation: $other")
        }
        if (!Collections.disjoint(typeVariableSubstitutors.keys, other.typeVariableSubstitutors.keys)) {
            throw IllegalArgumentException(
                "Combining two constraint systems only makes sense when they were created for different calls. " +
                        "Calls of the first system: ${typeVariableSubstitutors.keys}, second: ${other.typeVariableSubstitutors.keys}"
            )
        }
        if (!Collections.disjoint(other.allTypeParameterBounds.keys, allTypeParameterBounds.keys)) {
            throw IllegalArgumentException(
                "Combining two constraint systems only makes sense when they have no common variables. " +
                        "First system variables: ${allTypeParameterBounds.keys}, second: ${other.allTypeParameterBounds.keys}"
            )
        }

        allTypeParameterBounds.putAll(other.allTypeParameterBounds)
        usedInBounds.putAll(other.usedInBounds)
        errors.addAll(other.errors)
        initialConstraints.addAll(other.initialConstraints)
        typeVariableSubstitutors.putAll(other.typeVariableSubstitutors)
    }

    /**
     * 固定所有类型变量的类型
     *
     * 按照正确的顺序固定类型变量:
     * 1. 先固定外部类型变量
     * 2. 再固定内部类型变量 (函数类型参数)
     *
     * 固定过程会计算每个类型变量的最终类型并添加为精确边界约束
     */
    override fun fixVariables() {
        // TODO 变量应该按照正确的顺序固定
        val (external, functionTypeParameters) = allTypeParameterBounds.keys.partition { it.isExternal }
        external.forEach { fixVariable(it) }
        functionTypeParameters.forEach { fixVariable(it) }
    }

    /**
     * 固定单个类型变量
     *
     * ## 固定流程
     * 1. 标记变量为已固定状态
     * 2. 递归固定该变量边界中包含的嵌套类型变量
     * 3. 如果有推导出的值,添加为精确边界约束
     *
     * @param typeVariable 待固定的类型变量
     */
    private fun fixVariable(typeVariable: TypeVariable) {
        val typeBounds = getTypeBounds(typeVariable)
        if (typeBounds.isFixed) return
        typeBounds.setFixed()

        val nestedTypeVariables = typeBounds.bounds.flatMap { getNestedTypeVariables(it.constrainingType) }
        nestedTypeVariables.forEach { fixVariable(it) }

        val value = typeBounds.value ?: return

        addBound(
            typeVariable,
            value,
            TypeBounds.BoundKind.EXACT_BOUND,
            ConstraintContext(ConstraintPositionKind.FROM_COMPLETER.position())
        )
    }

    /**
     * 构建最终的约束系统
     *
     * 将构建器中收集的所有数据封装为不可变的约束系统对象
     *
     * @return 构建完成的约束系统
     */
    override fun build(): ConstraintSystem {
        return ConstraintSystemImpl(
            allTypeParameterBounds,
            usedInBounds,
            errors,
            initialConstraints,
            typeVariableSubstitutors
        )

    }

    companion object {
        /**
         * 创建用于特异性比较的约束系统
         *
         * 特异性比较用于重载解析,判断哪个候选函数更特化
         *
         * ## 与标准推导系统的差异
         * - 不进行可空性简化
         * - 约束编号递增计数
         * - 实现 [SimpleConstraintSystem] 接口
         *
         * @return 特异性比较约束系统
         */
        fun forSpecificity(): SimpleConstraintSystem =
            object : ConstraintSystemBuilderImpl(Mode.SPECIFICITY), SimpleConstraintSystem {
                override val context: TypeSystemInferenceExtensionContext
                    get() = SimpleClassicTypeSystemContext
                var counter = 0

                override fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): DefaultTypeSubstitutor {
                    @Suppress("UNCHECKED_CAST")
                    return registerTypeVariables(CallHandle.NONE, typeParameters as Collection<TypeParameterDescriptor>)
                }

                override fun addSubtypeConstraint(subType: CangJieTypeMarker, superType: CangJieTypeMarker) {
                    requireOrDescribe(subType is UnwrappedType, subType)
                    requireOrDescribe(superType is UnwrappedType, superType)
                    addSubtypeConstraint(
                        subType,
                        superType,
                        ConstraintPositionKind.VALUE_PARAMETER_POSITION.position(counter++)
                    )
                }

                override fun hasContradiction(): Boolean {
                    fixVariables()
                    return build().status.hasContradiction()
                }
            }
    }
}

/**
 * 为函数占位符创建具体的函数类型
 *
 * 当 lambda 表达式未声明参数类型时,会使用函数占位符类型
 * 此方法根据期望类型推导出具体的函数类型
 *
 * ## 处理逻辑
 * 1. 如果不是函数占位符,直接返回原类型
 * 2. 如果占位符未声明参数,根据期望类型推导参数数量
 * 3. 创建函数类型,参数类型为 DONT_CARE (待推导)
 *
 * ## 仓颉语言特性
 * - 仓颉没有扩展函数类型,接收器总是 null
 * - 参数数量 = 期望类型的类型参数数量 - 1 (最后一个是返回类型)
 *
 * @param functionPlaceholder 函数占位符类型
 * @param expectedType 期望的函数类型
 * @return 具体化的函数类型
 */
internal fun createTypeForFunctionPlaceholder(
    functionPlaceholder: CangJieType,
    expectedType: CangJieType
): CangJieType {
    if (!functionPlaceholder.isFunctionPlaceholder) return functionPlaceholder

    val functionPlaceholderTypeConstructor = functionPlaceholder.constructor as FunctionPlaceholderTypeConstructor

    // 仓颉没有扩展函数类型，函数参数大小计算不需要考虑接收器
    val newArgumentTypes = if (!functionPlaceholderTypeConstructor.hasDeclaredArguments) {
        val typeParamSize = expectedType.constructor.parameters.size
        // 类型参数：函数参数 + 返回类型
        val functionArgumentsSize = typeParamSize - 1
        val result = arrayListOf<CangJieType>()
        (1..functionArgumentsSize).forEach { result.add(DONT_CARE) }
        result
    } else {
        functionPlaceholderTypeConstructor.argumentTypes
    }

    // 仓颉没有扩展函数类型，接收器总是 null
    return createFunctionType(
        functionPlaceholder.builtIns,
        Annotations.EMPTY,
        null,
        newArgumentTypes,
        null,
        DONT_CARE,
//        suspendFunction = expectedType.isSuspendFunctionType
    )
}
