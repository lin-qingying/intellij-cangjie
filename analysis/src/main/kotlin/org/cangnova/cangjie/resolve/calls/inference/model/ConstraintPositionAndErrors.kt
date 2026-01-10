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

package org.cangnova.cangjie.resolve.calls.inference.model

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.EmptyIntersectionTypeKind
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeVariableMarker

/**
 * 仅输入类型约束位置标记接口
 *
 * 标记那些只能从输入类型（非输出类型）生成约束的位置。
 * 这类位置包括函数参数、接收者、显式类型参数等。
 */
interface OnlyInputTypeConstraintPosition

/**
 * 为保持兼容性而降低优先级的错误
 *
 * 当候选函数可以解析但需要保持兼容性时使用。
 *
 * @property needToReportWarning 是否需要报告警告
 */
class LowerPriorityToPreserveCompatibility(val needToReportWarning: Boolean) :
    ConstraintSystemError(CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY)

/**
 * 固定类型变量的约束位置
 *
 * 表示在解析过程中需要固定某个类型变量为具体类型的位置。
 *
 * @param T 解析原子的类型
 * @property variable 需要固定的类型变量
 * @property resolvedAtom 关联的已解析原子
 */
abstract class FixVariableConstraintPosition<T>(val variable: TypeVariableMarker, val resolvedAtom: T) :
    ConstraintPosition() {
    override fun toString(): String = "Fix variable $variable"
}

/**
 * 推断出的空交集类型接口
 *
 * 当类型推断系统推断出一组类型的交集为空时产生，
 * 表示这些类型之间没有公共子类型（即类型不兼容）。
 *
 * @property incompatibleTypes 不兼容的类型列表
 * @property causingTypes 导致空交集的原因类型列表
 * @property typeVariable 相关的类型变量
 * @property kind 空交集的种类（如上界冲突、下界冲突等）
 */
sealed interface InferredEmptyIntersection {
    val incompatibleTypes: List<CangJieTypeMarker>
    val causingTypes: List<CangJieTypeMarker>
    val typeVariable: TypeVariableMarker
    val kind: EmptyIntersectionTypeKind
}

/**
 * 已知类型参数约束位置的实现类
 *
 * 当显式指定类型参数时使用，例如 `func<Int>()`。
 *
 * @property typeArgument 显式指定的类型参数
 */
class KnownTypeParameterConstraintPositionImpl(typeArgument: CangJieType) :
    KnownTypeParameterConstraintPosition<CangJieType>(typeArgument)

/**
 * 已知类型参数约束位置基类
 *
 * 表示约束来自于显式指定的类型参数。
 *
 * @param T 类型参数的类型标记
 * @property typeArgument 显式指定的类型参数
 */
abstract class KnownTypeParameterConstraintPosition<T : CangJieTypeMarker>(val typeArgument: T) : ConstraintPosition() {
    override fun toString(): String = "TypeArgument $typeArgument"
}

/**
 * 推断出空交集的警告
 *
 * 当推断出的类型交集为空但不影响候选可用性时产生警告。
 * 这种情况下函数调用仍然被标记为 RESOLVED，但会报告潜在问题。
 *
 * @property incompatibleTypes 不兼容的类型列表
 * @property causingTypes 导致空交集的原因类型列表
 * @property typeVariable 相关的类型变量
 * @property kind 空交集的种类
 */
class InferredEmptyIntersectionWarning(
    override val incompatibleTypes: List<CangJieTypeMarker>,
    override val causingTypes: List<CangJieTypeMarker>,
    override val typeVariable: TypeVariableMarker,
    override val kind: EmptyIntersectionTypeKind,
) : ConstraintSystemError(CandidateApplicability.RESOLVED), InferredEmptyIntersection

/**
 * 推断出空交集的错误
 *
 * 当推断出的类型交集为空且导致候选不可用时产生错误。
 * 这种情况下函数调用被标记为 INAPPLICABLE，无法继续。
 *
 * @property incompatibleTypes 不兼容的类型列表
 * @property causingTypes 导致空交集的原因类型列表
 * @property typeVariable 相关的类型变量
 * @property kind 空交集的种类
 */
class InferredEmptyIntersectionError(
    override val incompatibleTypes: List<CangJieTypeMarker>,
    override val causingTypes: List<CangJieTypeMarker>,
    override val typeVariable: TypeVariableMarker,
    override val kind: EmptyIntersectionTypeKind,
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE), InferredEmptyIntersection

/**
 * 约束类型是错误类型
 *
 * 当约束中使用的类型本身就是错误类型时产生。
 * 这通常发生在类型解析失败的情况下。
 *
 * @property typeVariable 相关的类型变量
 * @property constraintType 作为约束的错误类型
 * @property position 约束合并位置
 */
class ConstrainingTypeIsError(
    val typeVariable: TypeVariableMarker,
    val constraintType: CangJieTypeMarker,
    val position: IncorporationConstraintPosition
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

/**
 * 没有成功的分支
 *
 * 当约束系统尝试多个分支（例如联合类型）但所有分支都失败时产生。
 *
 * @property position 约束合并位置
 */
class NoSuccessfulFork(val position: IncorporationConstraintPosition) :
    ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

/**
 * 简单约束系统的约束位置
 *
 * 用于 SimpleConstraintSystemImpl 的内部实现。
 * TODO: 应该仅在 SimpleConstraintSystemImpl 中使用
 */
object SimpleConstraintSystemConstraintPosition : ConstraintPosition()

/**
 * 约束系统错误基类
 *
 * 所有约束系统中产生的错误都继承自此类。
 *
 * @property applicability 错误对应的候选可用性级别
 */
sealed class ConstraintSystemError(val applicability: CandidateApplicability)

/**
 * 约束位置基类
 *
 * 密封类用于表示约束系统中约束产生的源位置。
 * 通过密封类限制子类只能在本文件内定义，从而更好地控制约束位置的类型层次结构。
 * 这使得类型检查时能够精确地知道约束来源的上下文。
 *
 * 约束位置的主要类型包括：
 * - 参数位置（ArgumentConstraintPosition）
 * - 接收者位置（ReceiverConstraintPosition）
 * - 类型参数位置（ExplicitTypeParameterConstraintPosition）
 * - Lambda 位置（LambdaArgumentConstraintPosition）
 * - 期望类型位置（ExpectedTypeConstraintPosition）
 * - 约束合并位置（IncorporationConstraintPosition）
 */
sealed class ConstraintPosition

/**
 * 约束合并位置
 *
 * 表示约束在合并（incorporation）过程中产生的位置。
 * 约束合并是指将多个约束组合成更强的约束的过程。
 *
 * @property initialConstraint 初始约束，作为合并的起点
 * @property isFromDeclaredUpperBound 是否来自声明的上界，用于区分约束来源
 */
data class IncorporationConstraintPosition(
    val initialConstraint: InitialConstraint,
    var isFromDeclaredUpperBound: Boolean = false
) : ConstraintPosition() {
    /**
     * 获取初始约束的位置
     */
    val from: ConstraintPosition get() = initialConstraint.position

    override fun toString(): String = "Incorporate $initialConstraint from position $from"
}


/**
 * 参数约束位置基类
 *
 * 表示约束来自于函数调用的参数。
 *
 * @param T 参数的类型
 * @property argument 产生约束的参数
 */
abstract class ArgumentConstraintPosition<out T>(val argument: T) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Argument $argument"
}

/**
 * 可调用引用约束位置基类
 *
 * 表示约束来自于可调用引用（如函数引用、方法引用）。
 *
 * @param T 调用的类型
 * @property call 产生约束的可调用引用调用
 */
abstract class CallableReferenceConstraintPosition<out T>(val call: T) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Callable reference $call"
}

/**
 * 参数约束位置的实现类
 *
 * 封装具体的仓颉调用参数。
 *
 * @property argument 仓颉调用参数
 */
class ArgumentConstraintPositionImpl(argument: CangJieCallArgument) :
    ArgumentConstraintPosition<CangJieCallArgument>(argument)

/**
 * 可调用引用约束位置的实现类
 *
 * 封装具体的可调用引用仓颉调用。
 *
 * @property callableReferenceCall 可调用引用的仓颉调用
 */
class CallableReferenceConstraintPositionImpl(val callableReferenceCall: CallableReferenceCangJieCall) :
    CallableReferenceConstraintPosition<CallableReferenceResolutionAtom>(callableReferenceCall)

/**
 * 接收者约束位置基类
 *
 * 表示约束来自于调用的接收者（如 `obj.method()` 中的 `obj`）。
 *
 * @param T 参数的类型
 * @property argument 作为接收者的参数
 */
abstract class ReceiverConstraintPosition<T>(val argument: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Receiver $argument"
}

/**
 * 约束不匹配接口
 *
 * 表示在约束合并过程中发现的类型不匹配。
 *
 * @property lowerType 下界类型（子类型）
 * @property upperType 上界类型（超类型）
 * @property position 约束合并位置
 */
sealed interface ConstraintMismatch {
    val lowerType: CangJieTypeMarker
    val upperType: CangJieTypeMarker
    val position: IncorporationConstraintPosition
}


/**
 * 约束错误
 *
 * 当合并的约束违反子类型关系时产生错误。
 * 如果约束来自接收者位置，则标记为 INAPPLICABLE_WRONG_RECEIVER；
 * 否则标记为 INAPPLICABLE。
 *
 * @property lowerType 下界类型
 * @property upperType 上界类型
 * @property position 约束合并位置
 */
class ConstraintError(
    override val lowerType: CangJieTypeMarker,
    override val upperType: CangJieTypeMarker,
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(if (position.from is ReceiverConstraintPosition<*>) CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER else CandidateApplicability.INAPPLICABLE),
    ConstraintMismatch {
    override fun toString(): String {
        return "$lowerType <: $upperType"
    }
}

/**
 * 显式类型参数约束位置的实现类
 *
 * 封装简单类型参数。
 *
 * @property typeArgument 简单类型参数
 */
class ExplicitTypeParameterConstraintPositionImpl(
    typeArgument: SimpleTypeArgument
) : ExplicitTypeParameterConstraintPosition<SimpleTypeArgument>(typeArgument)

/**
 * 声明的上界约束位置的实现类
 *
 * 表示约束来自于类型参数的声明上界（如 `<T : Number>` 中的 `Number`）。
 *
 * @property typeParameter 类型参数描述符
 * @property cangjieCall 相关的仓颉调用
 */
class DeclaredUpperBoundConstraintPositionImpl(
    typeParameter: TypeParameterDescriptor,
    val cangjieCall: CangJieCall
) : DeclaredUpperBoundConstraintPosition<TypeParameterDescriptor>(typeParameter) {
    override fun toString() = "DeclaredUpperBound ${typeParameter.name} from ${typeParameter.containingDeclaration}"
}

/**
 * 接收者约束位置的实现类
 *
 * 封装具体的接收者参数和选择器调用。
 *
 * @property argument 接收者参数
 * @property selectorCall 选择器调用（可能为 null）
 */
class ReceiverConstraintPositionImpl(
    argument: CangJieCallArgument,
    val selectorCall: CangJieCall?
) : ReceiverConstraintPosition<CangJieCallArgument>(argument)

/**
 * 约束警告
 *
 * 当合并的约束存在潜在问题但不影响候选可用性时产生警告。
 *
 * @property lowerType 下界类型
 * @property upperType 上界类型
 * @property position 约束合并位置
 */
class ConstraintWarning(
    override val lowerType: CangJieTypeMarker,
    override val upperType: CangJieTypeMarker,
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(CandidateApplicability.RESOLVED), ConstraintMismatch

/**
 * 将约束错误转换为警告
 *
 * 用于降低错误级别，允许调用继续但带有警告信息。
 */
fun ConstraintError.transformToWarning() = ConstraintWarning(lowerType, upperType, position)

/**
 * 声明的上界约束位置基类
 *
 * 表示约束来自于类型参数的声明上界。
 *
 * @param T 类型参数的类型
 * @property typeParameter 类型参数
 */
abstract class DeclaredUpperBoundConstraintPosition<T>(val typeParameter: T) : ConstraintPosition() {
    override fun toString(): String = "DeclaredUpperBound $typeParameter"
}

/**
 * 固定类型变量约束位置的实现类
 *
 * 封装需要固定的类型变量和已解析的原子。
 *
 * @property variable 类型变量
 * @property resolvedAtom 已解析的原子（可能为 null）
 */
class FixVariableConstraintPositionImpl(
    variable: TypeVariableMarker,
    resolvedAtom: ResolvedAtom?
) : FixVariableConstraintPosition<ResolvedAtom?>(variable, resolvedAtom)

/**
 * 类型参数信息不足的实现类
 *
 * 封装具体的类型参数信息不足错误。
 *
 * @property typeVariable 类型变量
 * @property resolvedAtom 已解析的原子
 * @property couldBeResolvedWithUnrestrictedBuilderInference 是否可以通过无限制的构建器推断解决
 */
class NotEnoughInformationForTypeParameterImpl(
    typeVariable: TypeVariableMarker,
    resolvedAtom: ResolvedAtom,
    couldBeResolvedWithUnrestrictedBuilderInference: Boolean
) : NotEnoughInformationForTypeParameter<ResolvedAtom>(
    typeVariable,
    resolvedAtom,
    couldBeResolvedWithUnrestrictedBuilderInference
)

/**
 * 仅输入类型诊断
 *
 * 当类型变量只有输入类型约束而没有输出类型约束时产生。
 * 这意味着类型推断系统无法确定该类型变量的具体类型。
 *
 * @property typeVariable 相关的类型变量
 */
class OnlyInputTypesDiagnostic(val typeVariable: TypeVariableMarker) :
    ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

/**
 * 从子类型化捕获的类型
 *
 * 当从子类型关系中捕获到不允许的类型时产生错误。
 * 这通常发生在泛型通配符或捕获类型的场景中。
 *
 * @property typeVariable 相关的类型变量
 * @property constraintType 被捕获的约束类型
 * @property position 约束位置
 */
class CapturedTypeFromSubtyping(
    val typeVariable: TypeVariableMarker,
    val constraintType: CangJieTypeMarker,
    val position: ConstraintPosition
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

/**
 * 类型参数信息不足的基类
 *
 * 当类型推断系统无法为类型参数推断出足够的信息时产生。
 *
 * @param T 已解析原子的类型
 * @property typeVariable 信息不足的类型变量
 * @property resolvedAtom 相关的已解析原子
 * @property couldBeResolvedWithUnrestrictedBuilderInference 如果使用无限制的构建器推断是否可以解决
 */
open class NotEnoughInformationForTypeParameter<T>(
    val typeVariable: TypeVariableMarker,
    val resolvedAtom: T,
    val couldBeResolvedWithUnrestrictedBuilderInference: Boolean
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

/**
 * 多个最小公共超类型
 *
 * 当类型推断找到多个候选的最小公共超类型且无法确定使用哪一个时产生。
 *
 * @property typeVariable 相关的类型变量
 * @property candidates 候选的最小公共超类型列表
 */
class MultipleMinimalCommonSupertypes(
    val typeVariable: TypeVariableMarker,
    val candidates: List<CangJieType>
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE) {

    override fun toString(): String {
        return "Multiple minimal common supertypes found for $typeVariable: ${candidates.joinToString(", ")}"
    }
}

/**
 * 推断到声明的上界
 *
 * 当类型推断结果恰好是类型参数声明的上界时产生。
 * 这种情况下调用仍然是有效的，但可能提示类型推断不够精确。
 *
 * @property typeVariable 相关的类型变量
 */
class InferredIntoDeclaredUpperBounds(val typeVariable: TypeVariableMarker) : ConstraintSystemError(
    CandidateApplicability.RESOLVED
)

/**
 * 构建器推断替换约束位置基类
 *
 * 表示约束来自于构建器推断过程中的类型替换。
 * 构建器推断是一种延迟类型推断机制，常用于DSL和构建器模式。
 *
 * @param L 构建器推断 Lambda 的类型
 * @property builderInferenceLambda 产生约束的构建器推断 Lambda
 * @property initialConstraint 初始约束
 * @property isFromNotSubstitutedDeclaredUpperBound 是否来自未替换的声明上界
 */
abstract class BuilderInferenceSubstitutionConstraintPosition<L>(
    private val builderInferenceLambda: L,
    val initialConstraint: InitialConstraint,
    val isFromNotSubstitutedDeclaredUpperBound: Boolean = false
) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Incorporated builder inference constraint $initialConstraint " +
            "into $builderInferenceLambda call"
}

/**
 * 显式类型参数约束位置基类
 *
 * 表示约束来自于显式指定的类型参数。
 *
 * @param T 类型参数的类型
 * @property typeArgument 显式指定的类型参数
 */
abstract class ExplicitTypeParameterConstraintPosition<T>(val typeArgument: T) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "TypeParameter $typeArgument"
}

/**
 * 期望类型约束位置的实现类
 *
 * 封装具体的顶层调用。
 *
 * @property topLevelCall 顶层仓颉调用
 */
class ExpectedTypeConstraintPositionImpl(topLevelCall: CangJieCall) :
    ExpectedTypeConstraintPosition<CangJieCall>(topLevelCall)

/**
 * 期望类型约束位置基类
 *
 * 表示约束来自于调用的期望返回类型（上下文类型）。
 * 例如，`val x: Int = foo()` 中的 `Int` 就是期望类型。
 *
 * @param T 顶层调用的类型
 * @property topLevelCall 产生约束的顶层调用
 */
abstract class ExpectedTypeConstraintPosition<T>(val topLevelCall: T) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "ExpectedType for call $topLevelCall"
}

/**
 * Lambda 参数约束位置基类
 *
 * 表示约束来自于 Lambda 表达式的参数或返回类型。
 *
 * @param T Lambda 的类型
 * @property lambda 产生约束的 Lambda 表达式
 */
abstract class LambdaArgumentConstraintPosition<T>(val lambda: T) : ConstraintPosition() {
    override fun toString(): String {
        return "LambdaArgument $lambda"
    }
}

/**
 * Lambda 参数约束位置的实现类
 *
 * 封装具体的已解析 Lambda 原子。
 *
 * @property lambda 已解析的 Lambda 原子
 */
class LambdaArgumentConstraintPositionImpl(lambda: ResolvedLambdaAtom) :
    LambdaArgumentConstraintPosition<ResolvedLambdaAtom>(lambda)

/**
 * 构建器推断位置对象
 *
 * 表示约束来自于构建器推断调用的特殊位置。
 */
object BuilderInferencePosition : ConstraintPosition() {
    override fun toString(): String = "For builder inference call"
}

/**
 * 构建器推断替换约束位置的实现类
 *
 * 封装具体的构建器推断 Lambda 调用参数。
 *
 * @property builderInferenceLambda Lambda 仓颉调用参数
 * @property initialConstraint 初始约束
 * @property isFromNotSubstitutedDeclaredUpperBound 是否来自未替换的声明上界
 */
class BuilderInferenceSubstitutionConstraintPositionImpl(
    builderInferenceLambda: LambdaCangJieCallArgument,
    initialConstraint: InitialConstraint,
    isFromNotSubstitutedDeclaredUpperBound: Boolean = false
) : BuilderInferenceSubstitutionConstraintPosition<LambdaCangJieCallArgument>(
    builderInferenceLambda, initialConstraint, isFromNotSubstitutedDeclaredUpperBound
)

/**
 * 注入的另一个存根类型约束位置基类
 *
 * 表示约束来自于从另一个构建器推断 Lambda 中注入的存根类型。
 * 这用于处理嵌套的构建器推断场景。
 *
 * @param T 构建器推断 Lambda 的类型
 * @property builderInferenceLambdaOfInjectedStubType 注入存根类型的构建器推断 Lambda
 */
abstract class InjectedAnotherStubTypeConstraintPosition<T>(private val builderInferenceLambdaOfInjectedStubType: T) :
    ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Injected from $builderInferenceLambdaOfInjectedStubType builder inference call"
}

/**
 * 注入的另一个存根类型约束位置的实现类
 *
 * 封装具体的构建器推断 Lambda 调用参数。
 *
 * @property builderInferenceLambdaOfInjectedStubType Lambda 仓颉调用参数
 */
class InjectedAnotherStubTypeConstraintPositionImpl(builderInferenceLambdaOfInjectedStubType: LambdaCangJieCallArgument) :
    InjectedAnotherStubTypeConstraintPosition<LambdaCangJieCallArgument>(builderInferenceLambdaOfInjectedStubType)
