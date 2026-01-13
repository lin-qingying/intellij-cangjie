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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintInjector
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.model.NewTypeVariable
import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.*
import org.cangnova.cangjie.types.model.*

/**
 * 约束系统的经典类型系统上下文 (Classic Type System Context For Constraint System)
 *
 * 这个类为约束系统(Constraint System)提供了经典类型系统的实现上下文。
 * 它整合了多个类型系统相关的接口,提供了类型推导、类型替换、捕获类型创建等核心功能。
 *
 * ## 核心职责
 * 1. **类型变量管理**: 处理类型变量的默认类型和构造器
 * 2. **捕获类型创建**: 创建泛型通配符捕获类型
 * 3. **类型替换**: 提供类型替换器的创建和应用
 * 4. **存根类型**: 为构建器推导和子类型检查创建存根类型
 * 5. **类型检查器状态**: 创建类型检查器状态以进行类型兼容性判断
 *
 * ## 多接口整合
 * 此类实现了三个核心接口:
 * - [TypeSystemInferenceExtensionContextDelegate]: 类型推导扩展上下文
 * - [ClassicTypeSystemContext]: 经典类型系统上下文
 * - [BuiltInsProvider]: 内置类型提供者
 *
 * ## 使用场景
 * ```kotlin
 * val context = ClassicTypeSystemContextForCS(builtIns, typeRefiner)
 *
 * // 创建捕获类型
 * val capturedType = context.createCapturedType(
 *     argument, supertypes, lowerType, CaptureStatus.FOR_SUBTYPING
 * )
 *
 * // 创建类型替换器
 * val substitutor = context.typeSubstitutorByTypeConstructor(
 *     mapOf(typeConstructor to concreteType)
 * )
 * ```
 *
 * @property builtIns 仓颉内置类型定义
 * @property cangjieTypeRefiner 类型精炼器,用于处理智能类型转换和类型细化
 *
 * @see TypeSystemInferenceExtensionContextDelegate 类型推导扩展上下文委托
 * @see ClassicTypeSystemContext 经典类型系统上下文
 * @see ConstraintSystemImpl 使用此上下文的约束系统实现
 */
class ClassicTypeSystemContextForCS(
    override val builtIns: CangJieBuiltIns,
    val cangjieTypeRefiner: CangJieTypeRefiner
) : TypeSystemInferenceExtensionContextDelegate,
    ClassicTypeSystemContext,
    BuiltInsProvider {

    /**
     * 获取类型变量的默认类型
     *
     * 返回类型变量的默认类型,通常用于当类型推导无法确定具体类型时的回退。
     * 默认类型通常是类型变量的上界(如果只有一个上界)或 Any。
     *
     * @receiver 类型变量标记,必须是 [NewTypeVariable] 实例
     * @return 类型变量的默认简单类型
     * @throws IllegalArgumentException 如果接收者不是 NewTypeVariable
     */
    override fun TypeVariableMarker.defaultType(): SimpleTypeMarker {
        require(this is NewTypeVariable, this::errorMessage)
        return this.defaultType
    }

    /**
     * 获取类型变量的新鲜构造器
     *
     * 返回类型变量的新鲜类型构造器(Fresh Type Constructor)。
     * 新鲜构造器用于在类型推导过程中创建唯一的类型变量实例,
     * 避免不同推导上下文之间的类型变量混淆。
     *
     * ## 新鲜构造器的作用
     * 在嵌套的类型推导中,可能需要为同一个泛型参数创建多个独立的类型变量实例。
     * 新鲜构造器确保每个实例都有唯一的标识,不会互相干扰。
     *
     * @receiver 类型变量标记,必须是 [NewTypeVariable] 实例
     * @return 类型变量的新鲜类型构造器
     * @throws IllegalArgumentException 如果接收者不是 NewTypeVariable
     */
    override fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker {
        require(this is NewTypeVariable, this::errorMessage)
        return this.freshTypeConstructor
    }

    /**
     * 创建捕获类型 (Captured Type)
     *
     * 创建一个捕获类型,用于表示泛型通配符或类型投影的捕获。
     * 捕获类型在处理协变、逆变类型参数时特别重要。
     *
     * ## 捕获类型的概念
     * 当遇到通配符类型(如 `List<out T>`)时,需要创建一个捕获类型来表示这个未知但固定的类型。
     * 捕获类型有上界(通过 constructorSupertypes 指定)和可选的下界(通过 lowerType 指定)。
     *
     * ## 示例场景
     * ```kotlin
     * fun foo(list: List<out Number>) {
     *     // 这里 List 的元素类型被捕获为一个上界为 Number 的类型
     *     val first = list.first() // first 的类型是捕获的 Number 子类型
     * }
     * ```
     *
     * @param constructorProjection 构造器投影,描述类型参数的变型
     * @param constructorSupertypes 捕获类型的上界列表
     * @param lowerType 捕获类型的下界(可选)
     * @param captureStatus 捕获状态,指示捕获的目的(子类型检查、推导等)
     * @return 创建的捕获类型标记
     * @throws IllegalArgumentException 如果参数类型不正确
     */
    override fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<CangJieTypeMarker>,
        lowerType: CangJieTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker {
        require(lowerType is UnwrappedType?, lowerType::errorMessage)
        require(constructorProjection is TypeArgument, constructorProjection::errorMessage)

        @Suppress("UNCHECKED_CAST")
        val capturedTypeConstructor = CapturedTypeConstructorImpl(
            constructorProjection as TypeArgument,
            constructorSupertypes as List<UnwrappedType>
        )
        return CapturedType(
            captureStatus,
            capturedTypeConstructor,
            lowerType = lowerType
        )
    }

    /**
     * 通过类型构造器映射创建类型替换器
     *
     * 根据提供的类型构造器到具体类型的映射,创建一个类型替换器。
     * 类型替换器用于将泛型类型参数替换为具体类型。
     *
     * ## 工作原理
     * 例如,如果有映射 `{ T -> Int, U -> String }`,
     * 那么类型 `List<T>` 会被替换为 `List<Int>`,
     * 类型 `Map<T, U>` 会被替换为 `Map<Int, String>`。
     *
     * @param map 类型构造器到具体类型的映射
     * @return 类型替换器,如果映射为空则返回空替换器
     */
    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker {
        if (map.isEmpty()) return createEmptySubstitutor()
        @Suppress("UNCHECKED_CAST")
        val unwrappedMap = (map as Map<TypeConstructor, CangJieType>).mapValues { (_, type) -> type.unwrap() }
        return ComposableTypeSubstitutor.create(unwrappedMap)
    }

    /**
     * 创建空的类型替换器
     *
     * 返回一个不进行任何替换的空替换器。
     * 空替换器在应用时会返回原始类型,不做任何修改。
     *
     * @return 空类型替换器实例
     */
    override fun createEmptySubstitutor(): TypeSubstitutorMarker {
        return ComposableTypeSubstitutor.EMPTY
    }

    /**
     * 安全地替换类型
     *
     * 使用此替换器安全地替换给定类型中的类型参数。
     * "安全"意味着即使替换失败也不会抛出异常,而是返回错误类型或原类型。
     *
     * ## 支持的替换器类型
     * - [ComposableTypeSubstitutor]: 组合式类型替换器
     *
     * @receiver 类型替换器标记
     * @param type 要替换的类型
     * @return 替换后的类型
     * @throws IllegalStateException 如果替换器类型不被支持
     */
    override fun TypeSubstitutorMarker.safeSubstitute(type: CangJieTypeMarker): CangJieTypeMarker {
        require(type is CangJieType, type::errorMessage)
        val unwrappedType = type.unwrap()
        return when (this) {
            is ComposableTypeSubstitutor -> safeSubstitute(unwrappedType)
            else -> error(this.errorMessage())
        }
    }

    /**
     * 为构建器推导创建存根类型
     *
     * 创建一个存根类型(Stub Type),用于构建器推导(Builder Inference)场景。
     * 存根类型是一个占位符,表示"这里有一个类型但我们还不知道它是什么"。
     *
     * ## 构建器推导
     * 构建器推导是一种高级类型推导技术,用于推导构建器 lambda 的类型参数。
     * 在推导过程中,某些类型变量可能暂时无法确定,需要使用存根类型作为占位符。
     *
     * @param typeVariable 类型变量标记
     * @return 为该类型变量创建的存根类型
     */
    override fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker {
        return StubTypeForBuilderInference(
            typeVariable.freshTypeConstructor() as TypeVariableConstructor,
            typeVariable.defaultType().isMarkedOption()
        )
    }

    /**
     * 为子类型检查中的类型变量创建存根类型
     *
     * 创建一个存根类型,专门用于子类型检查场景中的类型变量。
     * 这种存根类型在判断类型兼容性时有特殊的处理规则。
     *
     * ## 使用场景
     * 在检查 `A <: B` 时,如果 A 或 B 包含未确定的类型变量,
     * 需要使用存根类型来延迟判断,直到类型变量被固定。
     *
     * @param typeVariable 类型变量标记
     * @return 为该类型变量创建的存根类型
     */
    override fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker {
        return StubTypeForTypeVariablesInSubtyping(
            typeVariable.freshTypeConstructor() as TypeVariableConstructor,
            typeVariable.defaultType().isMarkedOption()
        )
    }

    /**
     * 判断类型构造器是否为类型变量
     *
     * 检查给定的类型构造器是否代表一个类型变量。
     * 类型变量是泛型类型参数的占位符,如 `<T>` 中的 T。
     *
     * @receiver 类型构造器标记
     * @return 如果是类型变量则返回 true
     */
    override fun TypeConstructorMarker.isTypeVariable(): Boolean {
        return this is TypeVariableTypeConstructor
    }

    /**
     * 判断类型变量是否包含在不变或逆变位置
     *
     * 检查类型变量在其使用位置上的变型特性。
     * 如果类型变量出现在不变(invariant)或逆变(contravariant)位置,
     * 则对类型推导有特殊的约束要求。
     *
     * ## 变型位置
     * - **协变位置**: 只读位置,如函数返回类型
     * - **逆变位置**: 只写位置,如函数参数类型
     * - **不变位置**: 既读又写,如可变属性类型
     *
     * @receiver 类型变量构造器标记
     * @return 如果包含在不变或逆变位置则返回 true
     * @throws IllegalArgumentException 如果接收者不是 TypeVariableTypeConstructor
     */
    override fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean {
        require(this is TypeVariableTypeConstructor)
        return isContainedInInvariantOrContravariantPositions
    }

    /**
     * 创建新的类型检查器状态
     *
     * 创建一个类型检查器状态,用于执行类型兼容性检查和子类型判断。
     * 类型检查器状态包含了类型检查所需的所有上下文信息。
     *
     * ## 配置选项
     * - **errorTypesEqualToAnything**: 如果为 true,错误类型与任何类型都兼容(用于错误恢复)
     * - **stubTypesEqualToAnything**: 如果为 true,存根类型与任何类型都兼容(用于部分推导)
     *
     * @param errorTypesEqualToAnything 错误类型是否与任何类型相等
     * @param stubTypesEqualToAnything 存根类型是否与任何类型相等
     * @return 配置好的类型检查器状态
     */
    override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): TypeCheckerState {
        return createClassicTypeCheckerState(
            errorTypesEqualToAnything,
            stubTypesEqualToAnything,
            typeSystemContext = this,
            cangjieTypeRefiner = cangjieTypeRefiner
        )
    }




}

/**
 * 错误消息生成器
 *
 * 内联辅助函数,用于生成类型不匹配的错误消息。
 * 当传递给 [ClassicTypeSystemContextForCS] 的参数类型不正确时,使用此函数生成描述性错误消息。
 *
 * @receiver 任何对象或 null
 * @return 包含对象值和类型信息的错误消息字符串
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun Any?.errorMessage(): String {
    return "ClassicTypeSystemContextForCS couldn't handle: $this, ${this?.let { it::class }}"
}

/**
 * 新约束系统工厂函数 (New Constraint System Factory)
 *
 * 便捷工厂函数,用于创建 [ConstraintSystemImpl] 实例。
 * 此函数自动创建配套的 [ClassicTypeSystemContextForCS] 上下文。
 *
 * ## 使用场景
 * 在需要创建新的约束系统进行类型推导时调用此函数:
 * ```kotlin
 * val constraintSystem = ConstraintSystemImpl(
 *     constraintInjector,
 *     builtIns,
 *     typeRefiner,
 *     languageVersionSettings
 * )
 *
 * // 添加约束
 * constraintSystem.addSubtypeConstraint(typeA, typeB, position)
 *
 * // 求解类型变量
 * val resultType = constraintSystem.getResultingType(typeVariable)
 * ```
 *
 * @param constraintInjector 约束注入器,用于生成类型约束
 * @param builtIns 仓颉内置类型定义
 * @param cangjieTypeRefiner 类型精炼器,处理智能类型转换
 * @param languageVersionSettings 语言版本设置,控制语言特性开关
 * @return 配置好的新约束系统实例
 *
 * @see ConstraintSystemImpl 约束系统实现
 * @see ClassicTypeSystemContextForCS 类型系统上下文
 * @see ConstraintInjector 约束注入器
 */
@Suppress("FunctionName")
fun ConstraintSystemImpl(
    constraintInjector: ConstraintInjector,
    builtIns: CangJieBuiltIns,
    cangjieTypeRefiner: CangJieTypeRefiner,
    languageVersionSettings: LanguageVersionSettings
): ConstraintSystemImpl {
    return ConstraintSystemImpl(
        constraintInjector,
        ClassicTypeSystemContextForCS(builtIns, cangjieTypeRefiner),
        languageVersionSettings
    )
}
